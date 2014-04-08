package edu.umass.cs.gns.replicaCoordination.multipaxos.multipaxospacket;

import org.json.JSONException;
import org.json.JSONObject;

import edu.umass.cs.gns.nsdesign.packet.PaxosPacket;
import edu.umass.cs.gns.replicaCoordination.multipaxos.paxosutil.Ballot;

/* A <slot, ballot, request> three-tuple. The slot and request are
 * within the proposal here. A pvalue is used internally by paxos
 * acceptors and coordinators. It also acts as a DECISION packet
 * as every committed request must be associated with some slot,
 * and ballot.
 */

public class PValuePacket extends ProposalPacket {
	protected static final String BALLOT = "ballot";
	private static final String MCSLOT = "slotCommitted@Majority";

	public final Ballot ballot;
	public final int majorityCommittedSlot; // for garbage collection, similar to that in AcceptPacket
	public final boolean recovery;
	
	public PValuePacket(Ballot b, ProposalPacket p) {
    	super(p);
		this.ballot = b;
		this.majorityCommittedSlot=-1;
		// packetType inherited, not assigned until DECISION or PREEMPTED
		this.recovery = false;
	}
	// Meant for super calling by inheritors
	protected PValuePacket(PValuePacket pvalue) {
		super(pvalue);
		this.ballot = pvalue.ballot;
		this.majorityCommittedSlot = pvalue.majorityCommittedSlot;
		this.recovery = pvalue.recovery;
	}

	// Private constructor called by getDecisionPacket
	private PValuePacket(PValuePacket pvalue, int mcSlot) {
		super(pvalue);
		this.ballot = pvalue.ballot;
		this.majorityCommittedSlot=mcSlot;
		// packetType inherited, not assigned
		this.recovery = pvalue.recovery;
	}
	public PValuePacket getDecisionPacket(int mcSlot) {
		PValuePacket decision = new PValuePacket(this, mcSlot);
		decision.packetType = PaxosPacketType.DECISION;
		return decision;
	}
	public PValuePacket preempt() {
		this.packetType = PaxosPacketType.PREEMPTED; // preemption does not change final fields, unlike getDecisionPacket
		return this;
	}
	/* A convenience method for when we really need a RequestPacket, not a deeper
	 * inherited PaxosPacket, e.g., when forwarding a preempted pvalue to the
	 * current, new coordinator to re-propose. Else, we would need to explicitly
	 * handle the PREEMPTED type in PaxosInstanceStateMachine.handlePaxosMessage.
	 */
	public RequestPacket getRequestPacket() {return new RequestPacket(this);}

	public PValuePacket(JSONObject json) throws JSONException{
		super(json);
		this.ballot = new Ballot(json.getString(BALLOT));
		this.majorityCommittedSlot = json.getInt(MCSLOT);
		this.recovery = json.getBoolean(RECOVERY);
		this.packetType = PaxosPacket.getPaxosPacketType(json);
	}

	@Override
	public JSONObject toJSONObjectImpl() throws JSONException {
		JSONObject json = super.toJSONObjectImpl();
		json.put(BALLOT, ballot.toString());
		json.put(MCSLOT, this.majorityCommittedSlot);
		json.put(RECOVERY, recovery);
		return json;
	}

}

