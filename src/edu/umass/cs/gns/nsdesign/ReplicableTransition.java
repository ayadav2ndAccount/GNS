package edu.umass.cs.gns.nsdesign;

import edu.umass.cs.gns.main.GNS;
import edu.umass.cs.gns.nio.IntegerPacketType;
import edu.umass.cs.gns.nsdesign.packet.Packet;
import edu.umass.cs.gns.reconfiguration.InterfaceRequest;
import edu.umass.cs.gns.reconfiguration.RequestParseException;
import edu.umass.cs.gns.replicaCoordination.multipaxos.multipaxospacket.RequestPacket;
import java.util.Set;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Temporary class created for the testing with multipaxos package. This is the replicable app that we give to
 * multipaxos. This replicable app creates a wrapper around the actual replicable app (GNSReconfigurable or ReplicaController).
 *
 * Why this class? When multipaxos calls the method handleDecision, the parameter 'String value' in
 * that method is actually a RequestPacket represented in string form. The actual replicable app
 * does not recognize RequestPacket. but expects that 'value' is actually
 * field 'requestValue' of RequestPacket. The handleDecision method extracts 'requestValue' from RequestPacket and
 * calls the handleDecision of the actual replicable app.
 *
 * Created by abhigyan on 6/17/14.
 */
public class ReplicableTransition implements Replicable{

  private Replicable replicable;

  public ReplicableTransition(Replicable replicable) {
    this.replicable = replicable;
  }

  @Override
  public boolean handleDecision(String name, String value, boolean doNotReplyToClient) {
    boolean noop = false;
    try {

      JSONObject json = new JSONObject(value);
      if (Packet.getPacketType(json).equals(Packet.PacketType.PAXOS_PACKET)) {
        if (Config.debuggingEnabled) GNS.getLogger().info("$$$$$$$$$$ Received decision: " + value);
        RequestPacket requestPacket = new RequestPacket(json);
        value = requestPacket.requestValue;
        noop = value.equals(RequestPacket.NO_OP);
      }
    } catch (JSONException e) {
      GNS.getLogger().severe(" JSON Exception; " + value);
      e.printStackTrace();
    }
    if (noop) {
      return true;
    } else {
      return replicable.handleDecision(name, value, doNotReplyToClient);
    }
  }

  @Override
  public String getState(String name) {
    return replicable.getState(name);
  }

  @Override
  public boolean updateState(String name, String state) {
    return replicable.updateState(name, state);
  }

  // For InterfaceReplicable
  
  @Override
  public boolean handleRequest(InterfaceRequest request) {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }
  
  @Override
  public boolean handleRequest(InterfaceRequest request, boolean doNotReplyToClient) {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

//  @Override
//  public String getState(String name, int epoch) {
//    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
//  }

  

  @Override
  public InterfaceRequest getRequest(String stringified) throws RequestParseException {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public Set<IntegerPacketType> getRequestTypes() {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }
}