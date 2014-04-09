/*
 * Copyright (C) 2013
 * University of Massachusetts
 * All Rights Reserved 
 */
package edu.umass.cs.gns.nsdesign.packet;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/*************************************************************
 * This class implements a packet that contains a response
 * to a select statement.
 * 
 * @author Westy
 ************************************************************/
public class SelectResponsePacket extends BasicPacket {

  public enum ResponseCode {

    NOERROR,
    ERROR
  }
  //
  private final static String ID = "id";
  private final static String RECORDS = "records";
  private final static String GUIDS = "guids";
  private final static String LNSID = "lnsid";
  private final static String LNSQUERYID = "lnsQueryId";
  private final static String NSQUERYID = "nsQueryId";
  private final static String NAMESERVER = "ns";
  private final static String RESPONSECODE = "code";
  private final static String ERRORSTRING = "error";
  private final static String GUID = "guid"; // for auto group guid this is the guid to be maintained
  
  private int id;
  private int lnsID; // the local name server handling this request
  private int lnsQueryId;
  private int nsQueryId;
  private int nameServer;
  private JSONArray records;
  private JSONArray guids;
  private ResponseCode responseCode;
  private String errorMessage;

  /**
   * Constructs a new SelectResponsePacket
   * @param id
   * @param jsonObject 
   */
  private SelectResponsePacket(int id, int lns, int lnsQueryId, int nsQueryId, int nameServer, JSONArray records, JSONArray guids, ResponseCode responseCode,
          String errorMessage) {
    this.type = Packet.PacketType.SELECT_RESPONSE;
    this.id = id;
    this.lnsID = lns;
    this.lnsQueryId = lnsQueryId;
    this.nsQueryId = nsQueryId;
    this.nameServer = nameServer;
    this.records = records;
    this.guids = guids;
    this.responseCode = responseCode;
    this.errorMessage = errorMessage;
  }

  /**
   * Used by a NameServer to a send response with full records back to the collecting NameServer
   * 
   * @param id
   * @param lns
   * @param lnsQueryId
   * @param nsQueryId
   * @param nameServer
   * @param records
   * @return 
   */
  public static SelectResponsePacket makeSuccessPacketForRecordsOnly(int id, int lns, int lnsQueryId, int nsQueryId, int nameServer, JSONArray records) {
    return new SelectResponsePacket(id, lns, lnsQueryId, nsQueryId, nameServer, records, null, ResponseCode.NOERROR, null);
  }
  
  /**
   * Used by a NameServer to a send response with only a list of guids back to the Local NameServer
   * 
   * @param id
   * @param lns
   * @param lnsQueryId
   * @param nsQueryId
   * @param nameServer
   * @param guids
   * @return 
   */
  public static SelectResponsePacket makeSuccessPacketForGuidsOnly(int id, int lns, int lnsQueryId, int nsQueryId, int nameServer, JSONArray guids) {
    return new SelectResponsePacket(id, lns, lnsQueryId, nsQueryId, nameServer, null, guids, ResponseCode.NOERROR, null);
  }

  /**
   * Used by a NameServer to a failure response to a NameServer or Local NameServer
   * @param id
   * @param lns
   * @param lnsQueryId
   * @param nsQueryId
   * @param nameServer
   * @param errorMessage
   * @return 
   */
  public static SelectResponsePacket makeFailPacket(int id, int lns, int lnsQueryId, int nsQueryId, int nameServer, String errorMessage) {
    return new SelectResponsePacket(id, lns, lnsQueryId, nsQueryId, nameServer, null, null, ResponseCode.ERROR, errorMessage);
  }

  /**
   * Constructs new SelectResponsePacket from a JSONObject
   * @param json JSONObject representing this packet
   * @throws org.json.JSONException
   */
  public SelectResponsePacket(JSONObject json) throws JSONException {
    if (Packet.getPacketType(json) != Packet.PacketType.SELECT_RESPONSE) {
      Exception e = new Exception("StatusPacket: wrong packet type " + Packet.getPacketType(json));
      e.printStackTrace();
      return;
    }
    this.type = Packet.getPacketType(json);
    this.id = json.getInt(ID);
    this.lnsID = json.getInt(LNSID);
    this.lnsQueryId = json.getInt(LNSQUERYID);
    this.nsQueryId = json.getInt(NSQUERYID);
    this.nameServer = json.getInt(NAMESERVER);
    this.responseCode = ResponseCode.valueOf(json.getString(RESPONSECODE));
    // either of these could be null
    this.records = json.optJSONArray(RECORDS);
    this.guids = json.optJSONArray(GUIDS);
    this.errorMessage = json.optString(ERRORSTRING, null);
    
  }

  /**
   * Converts a SelectResponsePacket to a JSONObject.
   *
   * @return JSONObject representing this packet.
   * @throws org.json.JSONException
   */
  @Override
  public JSONObject toJSONObject() throws JSONException {
    JSONObject json = new JSONObject();
    Packet.putPacketType(json, getType());
    json.put(ID, id);
    json.put(LNSID, lnsID);
    json.put(LNSQUERYID, lnsQueryId);
    json.put(NSQUERYID, nsQueryId);
    json.put(NAMESERVER, nameServer);
    json.put(RESPONSECODE, responseCode.name());
    if (records != null) {
      json.put(RECORDS, records);
    }
    if (guids != null) {
      json.put(GUIDS, guids);
    }
    if (errorMessage != null) {
      json.put(ERRORSTRING, errorMessage);
    }
    return json;
  }

  public int getId() {
    return id;
  }

  public int getLnsID() {
    return lnsID;
  }

  public JSONArray getRecords() {
    return records;
  }
  
  public JSONArray getGuids() {
    return guids;
  }

  public int getLnsQueryId() {
    return lnsQueryId;
  }

  public int getNsQueryId() {
    return nsQueryId;
  }

  public int getNameServer() {
    return nameServer;
  }

  public ResponseCode getResponseCode() {
    return responseCode;
  }

  public String getErrorMessage() {
    return errorMessage;
  }

  
}
