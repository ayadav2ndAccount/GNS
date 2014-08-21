package edu.umass.cs.gns.nsdesign.packet;

import edu.umass.cs.gns.nsdesign.packet.Packet.PacketType;
import java.net.InetSocketAddress;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * This packet is used to send commands from a Local Name Server to a Name Server
 * as well as send responses back to the LNS.
 * 
 * This is used by an new module that is not yet completed.
 */
public class LNSToNSCommandPacket extends BasicPacket {

  private final static String REQUESTID = "reqID";
  private final static String COMMAND = "command";
  private final static String RETURNVALUE = "returnValue";
  //private final static String LNSID = "lnsid";
  // these two replace LNSID
  private final static String LNSADDRESS = "lnsAddress";
  private final static String LNSPORT = "lnsPort";

  /**
   * Identifier of the request.
   */
  private final int requestId;
  /**
   * The JSON form of the command. Always includes a COMMANDNAME field.
   * Almost always has a GUID field or NAME (for HRN records) field.
   */
  private final JSONObject command;
  private String returnValue;
  //private final int lnsID; // the local name server handling this request
  /**
   * The address of the local name server handling this request.
   */
  private final InetSocketAddress lnsAddress;
  

  /**
   *
   * @param requestId
   * @param command
   */
  public LNSToNSCommandPacket(int requestId, InetSocketAddress lnsAddress, JSONObject command) {
    this.setType(PacketType.LNS_TO_NS_COMMAND);
    this.requestId = requestId;
    this.lnsAddress = lnsAddress;
    this.command = command;
    this.returnValue = null; // only set when the packet is ready to send back
  }

  public LNSToNSCommandPacket(JSONObject json) throws JSONException {
    this.type = Packet.getPacketType(json);
    this.requestId = json.getInt(REQUESTID);
    this.lnsAddress = new InetSocketAddress(json.getString(LNSADDRESS), json.getInt(LNSPORT));
    this.command = json.getJSONObject(COMMAND);
    this.returnValue = json.has(RETURNVALUE) ? json.optString(RETURNVALUE, null) : null;
  }

  /**
   * Converts the command object into a JSONObject.
   *
   * @return
   * @throws org.json.JSONException
   */
  @Override
  public JSONObject toJSONObject() throws JSONException {
    JSONObject json = new JSONObject();
    Packet.putPacketType(json, getType());
    json.put(REQUESTID, this.requestId);
    json.put(LNSADDRESS, lnsAddress.getHostString());
    json.put(LNSPORT, lnsAddress.getPort());
    json.put(COMMAND, this.command);
    if (returnValue != null) {
      json.put(RETURNVALUE, returnValue);
    }
    return json;
  }

  public int getRequestId() {
    return requestId;
  }

//  @Deprecated
//  public int getLnsID() {
//    return lnsID;
//  }

  public InetSocketAddress getLnsAddress() {
    return lnsAddress;
  }
  
  public JSONObject getCommand() {
    return command;
  }

  public String getReturnValue() {
    return returnValue;
  }

  public void setReturnValue(String returnValue) {
    this.returnValue = returnValue;
  }

}
