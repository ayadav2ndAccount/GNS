package edu.umass.cs.gns.replicaCoordination.multipaxos;

import java.util.ArrayList;
import java.util.logging.Logger;

import edu.umass.cs.gns.replicaCoordination.multipaxos.multipaxospacket.AcceptPacket;
import edu.umass.cs.gns.replicaCoordination.multipaxos.multipaxospacket.AcceptReplyPacket;
import edu.umass.cs.gns.replicaCoordination.multipaxos.multipaxospacket.PValuePacket;
import edu.umass.cs.gns.replicaCoordination.multipaxos.multipaxospacket.PrepareReplyPacket;
import edu.umass.cs.gns.replicaCoordination.multipaxos.multipaxospacket.ProposalPacket;
import edu.umass.cs.gns.replicaCoordination.multipaxos.multipaxospacket.RequestPacket;
import edu.umass.cs.gns.replicaCoordination.multipaxos.paxosutil.Ballot;

/**
@author V. Arun
 */

/* This class is a paxos coordinator. It is really a shell to 
 * wrap PaxosCoordinator state and exists for the 
 * following reasons: (1) it allows multiple operations over
 * PaxosCoordinatorState to be atomically; (2) it hides from 
 * PaxosInstanceStateMachine any synchronization issues wrt 
 * PaxosCoordinatorState (just like with PaxosAcceptorState); 
 * (3) it keeps the code in PaxosCoordinator and 
 * PaxosCoordinatorState more readable and closer to Rensesse's 
 * Paxos Made Moderately Complex pseudocode; (4) it uses little
 * empty space for the coordinator when a paxos instance is not
 * a coordinator. The alternative would be to nullify 
 * PaxosCoordinatorState when it is not the coordinator (making
 * synchronization uglier without the shell) or let it use empty 
 * space. Space is only a minor reason as the PaxosCoordinatorState 
 * internally uses optimized structures because of which empty
 * space adds only a few tens of bytes and still keeps the *total*
 * space used by an inactive paxos instance under 250B. An active 
 * coordinator or acceptor on the other hand could easily use 
 * 1000B or more. 
 * 
 * Notes: (1) This class itself has no state other than the
 * PaxosCoordinatorState pointer. (2) Most methods require 
 * synchronization for atomicity even though PaxosCoordinatoState
 * is internally synchronized.
 */
public class PaxosCoordinator {
	public static final boolean DEBUG=PaxosManager.DEBUG;
	private PaxosCoordinatorState pcs=null; // This gets recreated for each ballot.
	
	private static Logger log = Logger.getLogger(PaxosCoordinator.class.getName()); // GNS.getLogger();
	
	/* Come to exist if nonexistent. Called by PaxosInstanceStateMachine
	 */
	protected synchronized Ballot makeCoordinator(int bnum, int coord, int[] members, int slot, boolean recovery) {
		boolean sendPrepare=false;
		if(pcs==null || (pcs.getBallot().compareTo(bnum, coord))<0) {
			pcs = new PaxosCoordinatorState(bnum, coord, slot, members);
			if(bnum==0 || recovery) pcs.setCoordinatorActive(); // Initial coordinator status assumed, not explicitly prepared.
			else sendPrepare=true;
		} else if(pcs!=null && (pcs.getBallot().compareTo(bnum, coord))==0 && !pcs.isActive()) sendPrepare = true; // resend prepare 
		return sendPrepare ? pcs.prepare(members) : null; // For ballotnum>0, must explicitly prepare
	}
	protected synchronized Ballot remakeCoordinator(int[] members) {
		return (this.isActive() ? pcs.prepare(members): null);
	}
	/* Cease to exist. Internally called when preempted. 
	 */
	private synchronized ArrayList<ProposalPacket> resignAsCoordinator() {
		ArrayList<ProposalPacket> preActiveProposals=null;
		if(this.exists() && !pcs.isActive()) {
			preActiveProposals = pcs.getPreActiveProposals();
			log.info("Node "+this.pcs.getBallot().coordinatorID + " resigning as coordinator, " +
					"preActive proposals = " + this.pcs.getPreActiveProposals());
		}
		
		pcs = null; // The main point of this method.
		
		return preActiveProposals; // Pass these to the coordinator to whom you deferred
	}
	/* Returns true if pcs!=null. Existence is different from 
	 * the coordinator being active (=pcs.active==true).
	 */
	protected synchronized boolean exists() {
		return pcs!=null;
	}
	protected synchronized boolean exists(Ballot b) {
		return pcs!=null && pcs.getBallot().compareTo(b)>=0;
	}
	/* Allows PaxosInstanceStateMachine to forcibly call resignAsCoordinator().
	 */
	protected synchronized void forceStop() {
		this.resignAsCoordinator();
	}

	/* Phase2a
	 */
	protected synchronized AcceptPacket propose(int[] groupMembers, RequestPacket req) {
		if(!this.exists()) return null;
		return this.pcs.propose(groupMembers, req);
	}

	/* Phase2b
	 */
	protected synchronized PValuePacket handleAcceptReply(int[] members, AcceptReplyPacket acceptReply) {
		if(!this.exists()) return null;
		
		PValuePacket committedPValue=null; PValuePacket preemptedPValue=null;
		if(this.pcs.isActive()) { 
			if(acceptReply.ballot.compareTo(this.pcs.getBallot()) > 0) {
				if((preemptedPValue = pcs.handleAcceptReplyHigherBallot(acceptReply))!=null) {
					/* Can ignore return value of preActiveProposals as handleAcceptReplyHigherBallot 
					 * returns true only if there are no proposals at this coordinator.
					 */
					if(DEBUG) log.info("PREEMPTED request#: " + acceptReply.slotNumber);
					if(pcs.preemptedFully()) {
						log.info("Node " + this.pcs.getBallot().coordinatorID + " preempted fully, about to resign");
						resignAsCoordinator();
					}
				}
			}
			else if(acceptReply.ballot.compareTo(pcs.getBallot()) == 0) {
				committedPValue = pcs.handleAcceptReplyMyBallot(members, acceptReply);
			}
		}
		return committedPValue!=null ? committedPValue : preemptedPValue; // both could be null too
	}
	
	/* Phase1a
	 * Propose ballot.
	 */
	protected synchronized Ballot prepare(int[] members) {
		return this.pcs.prepare(members);
	}
	/* Phase1b
	 * Event: Received a prepare reply message.
	 * Action: Resign if reply contains higher ballot as we are
	 * guaranteed to lose the election. Else if this prepare reply 
	 * constitutes a majority, combine received pvalues onto 
	 * existing proposals, spawn commanders for all proposals, 
	 * and set coordinator to active state.
	 * 
	 * Return: The set of accept messages (phase2a) to be sent out
	 * corresponding to spawned commanders.
	 */
	protected synchronized ArrayList<AcceptPacket> handlePrepareReply(PrepareReplyPacket prepareReply, int[] members) {
		if(!this.exists()) return null; 
		ArrayList<AcceptPacket> acceptPacketList = null;

		if(this.pcs.isPrepareAcceptedByMajority(prepareReply, members)) { // pcs still valid
			assert(!this.pcs.isActive());   // ******ensures this else block is called exactly once
			this.pcs.combinePValuesOntoProposals(members); // okay even for multiple threads to call in parallel
			acceptPacketList = this.pcs.spawnCommandersForProposals(); // should be called only once, o/w conflicting conflicts possible
			this.pcs.setCoordinatorActive(); // *****ensures this else block is called exactly once
			log.info("Node " + prepareReply.coordinatorID + " Acquired PREPARE MAJORITY: About to conduct view change.");
		} // "synchronized" in the method definition ensures that this else block is called atomically 
		
		return (acceptPacketList);
	}

	/* Phase1b
	 * Received prepare reply with higher ballot.
	 */
	protected synchronized ArrayList<ProposalPacket> getPreActivesIfPreempted(PrepareReplyPacket prepareReply, int[] members) {
		if(!this.exists()) return null;
				
		ArrayList<ProposalPacket> preActiveProposals = null;
		if(this.pcs.isPreemptable(prepareReply)) {
			preActiveProposals = resignAsCoordinator(); // pcs no longer valid
		} 
		return preActiveProposals;
	}
	protected synchronized boolean isOverloaded(int acceptorSlot) {
		if(!this.isActive()) return false;
		return this.pcs.isOverloaded(acceptorSlot);
	}
	protected synchronized boolean waitingTooLong() {
		if(this.isActive() && this.pcs.waitingTooLong()) return true;
		return false;
	}
	protected synchronized boolean isCommandering(int slot) {
		return (this.isActive() && this.pcs.isCommandering(slot)) ? true : false;
	}
	protected synchronized AcceptPacket reCommander(int slot) {
		if(!this.isActive()) return null;
		return this.pcs.reInitCommander(slot);
	}
		
	// Testing methods
	protected boolean isActive() {
		return this.exists() && this.pcs.isActive();
	}
	public String toString() {return "{Coordinator=" + (pcs!=null ? this.pcs.toString() : null);}
	protected void testingInitCoord(int load) {
		if(pcs!=null) pcs.testingInitCoord(load);
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		System.out.println("FAILURE: I am not tested, so I am useless and should drown myself in a puddle.");
		System.out.println("Try running PaxosManager's or PaxosAcceptorState's tests for now.");
	}
}
