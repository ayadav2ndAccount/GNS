package edu.umass.cs.gns.nsdesign.replicaController;

import edu.umass.cs.gns.exceptions.FieldNotFoundException;
import edu.umass.cs.gns.exceptions.RecordNotFoundException;
import edu.umass.cs.gns.main.GNS;
import edu.umass.cs.gns.nameserver.replicacontroller.ReplicaControllerRecord;
import edu.umass.cs.gns.nsdesign.GNSMessagingTask;
import edu.umass.cs.gns.packet.RequestActivesPacket;
import org.json.JSONException;

/**
 * A replica controller will execute this code on a local name server's request to lookup
 * current set of active replicas for a name.
 * <p>
 * This class contains static methods that will be called by ReplicaController.
 * <p>
 * Created by abhigyan on 2/27/14.
 */
public class LookupActives {


  public static GNSMessagingTask executeLookupActives(RequestActivesPacket packet, ReplicaController replicaController)
          throws JSONException{

    GNSMessagingTask msgTask = null;

    GNS.getLogger().info("Received Request Active Packet Name = " + packet.getName());

    boolean isError = false;
    try {
      ReplicaControllerRecord rcRecord = ReplicaControllerRecord.getNameRecordPrimaryMultiField(
              replicaController.getReplicaControllerDB(), packet.getName(), ReplicaControllerRecord.MARKED_FOR_REMOVAL,
              ReplicaControllerRecord.ACTIVE_NAMESERVERS);
      if (rcRecord.isMarkedForRemoval()) {
        isError = true;
      } else { // send reply to client
        packet.setActiveNameServers(rcRecord.getActiveNameservers());
        msgTask = new GNSMessagingTask(packet.getLNSID(), packet.toJSONObject());

        GNS.getLogger().info("Sent actives for " + packet.getName() + " Actives = " + rcRecord.getActiveNameservers());
      }
    } catch (RecordNotFoundException e) {
      isError = true;
    } catch (FieldNotFoundException e) {
      GNS.getLogger().severe("Field not found exception. " + e.getMessage());
      e.printStackTrace();
    }

    if (isError) {
      packet.setActiveNameServers(null);
      msgTask = new GNSMessagingTask(packet.getLNSID(), packet.toJSONObject());
        GNS.getLogger().fine("Error: Record does not exist for " + packet.getName());
    }

    return msgTask;
  }
}
