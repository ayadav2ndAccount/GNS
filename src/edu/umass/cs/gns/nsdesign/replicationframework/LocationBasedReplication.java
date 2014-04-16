package edu.umass.cs.gns.nsdesign.replicationframework;

import edu.umass.cs.gns.exceptions.FieldNotFoundException;
import edu.umass.cs.gns.nsdesign.Config;
import edu.umass.cs.gns.nsdesign.recordmap.ReplicaControllerRecord;
import edu.umass.cs.gns.nsdesign.replicaController.ReplicaController;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

/*************************************************************
 * This class implements the ReplicationFramework interface
 * and is used to select active name servers based on location
 * of the demand.
 * 
 * @author Hardeep Uppal
 ************************************************************/
public class LocationBasedReplication implements ReplicationFrameworkInterface {

  //  private Random random = new Random(System.currentTimeMillis());
  /*************************************************************
   * Returns a new set of active name servers based on votes 
   * received for replica selection for this record. NameServers
   * with the highest votes are selected as the new active name
   * servers.
   * @param rcRecord NameRecord whose active name server set is
   * generated.
   * @param numReplica Size of the new active name server set.
   * @param count
   ************************************************************/
  @Override
  public Set<Integer> newActiveReplica(ReplicaController rc, ReplicaControllerRecord rcRecord, int numReplica, int count) throws FieldNotFoundException {


    if (numReplica >= rc.getGnsNodeConfig().getAllNameServerIDs().size()) {
      return new HashSet<Integer>(rc.getGnsNodeConfig().getAllNameServerIDs());
    }
    Set<Integer> newActiveNameServerSet;
    //		 Use top-K based on locality.
    if (numReplica > Config.nameServerVoteSize) {
      newActiveNameServerSet = rcRecord.getHighestVotedReplicaID(rc.getGnsNodeConfig(), Config.nameServerVoteSize);
    } else {
      newActiveNameServerSet = rcRecord.getHighestVotedReplicaID(rc.getGnsNodeConfig(), numReplica);
    }

    // Select based on votes as much as you can.

    if (newActiveNameServerSet.size() < numReplica) {
      int difference = numReplica - newActiveNameServerSet.size();
      //Randomly select the other active name servers
      for (int i = 1; i <= difference; i++) {
        if (newActiveNameServerSet.size() >= rc.getGnsNodeConfig().getAllNameServerIDs().size()) {
          break;
        }
        boolean added = false;
        // Ensures that random selection will still be deterministic for each name. 
        Random random = new Random(rcRecord.getName().hashCode());
        do {
          int nsIndex = random.nextInt(rc.getGnsNodeConfig().getAllNameServerIDs().size());
          int newActiveNameServerId = getSetIndex(rc.getGnsNodeConfig().getAllNameServerIDs(), nsIndex);
          if (rc.getGnsNodeConfig().getPingLatency(newActiveNameServerId) == -1) {
            continue;
          }
          added = newActiveNameServerSet.add(newActiveNameServerId);
        } while (!added);
      }
    }

    return newActiveNameServerSet;
  }

  private int getSetIndex(Set<Integer> nodeIds, int index) {
    int count = 0;
    for (int node: nodeIds) {
      if  (count == index) return node;
      count++;

    }
    return -1;
  }
}
