package edu.umass.cs.gns.gigapaxos.paxosutil;

import edu.umass.cs.gns.nio.AbstractPacketDemultiplexer;
import edu.umass.cs.gns.nsdesign.packet.Packet;
import edu.umass.cs.gns.util.Stringifiable;
import edu.umass.cs.gns.gigapaxos.PaxosManager;
import edu.umass.cs.gns.gigapaxos.multipaxospacket.PaxosPacket;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * @author V. Arun
 */
/* Needed to get NIO to send paxos packets to PaxosManager */
public class PaxosPacketDemultiplexer<NodeIDType> extends
		AbstractPacketDemultiplexer {

	private final PaxosManager<NodeIDType> paxosManager;
	private final IntegerMap<NodeIDType> nodeMap;
	private final Stringifiable<NodeIDType> unstringer;

	public PaxosPacketDemultiplexer(PaxosManager<NodeIDType> pm,
			IntegerMap<NodeIDType> nodeMap, Stringifiable<NodeIDType> unstringer) {
		paxosManager = pm;
		this.nodeMap = nodeMap;
		this.unstringer = unstringer;
		this.register(Packet.PacketType.PAXOS_PACKET);
	}

	public boolean handleJSONObject(JSONObject jsonMsg) {
		boolean isPacketTypeFound = true;

		try {
			Packet.PacketType type = Packet.getPacketType(jsonMsg);
			switch (type) {
			case PAXOS_PACKET:
				/*
				 * FIXME: need to fix by mapping string node IDs in
				 * incoming packets to integers. The reverse
				 * operation will be done by Messenger using
				 * IntegerMap just before sending out packets.
				 */
				paxosManager.handleIncomingPacket(fixNodeStringToInt(jsonMsg));
				break;
			default:
				isPacketTypeFound = false;
				break;
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return isPacketTypeFound;
	}

	// convert string -> NodeIDType -> int (can *NOT* convert string directly to int)
	private JSONObject fixNodeStringToInt(JSONObject json) throws JSONException {
		// FailureDetectionPacket already has generic NodeIDType
		if (PaxosPacket.getPaxosPacketType(json) == PaxosPacket.PaxosPacketType.FAILURE_DETECT)
			return json;

		if (json.has(PaxosPacket.NodeIDKeys.BALLOT.toString())) {
			// fix ballot string
			String ballotString =
					json.getString(PaxosPacket.NodeIDKeys.BALLOT.toString());
			Integer coordInt = stringToInt(Ballot.getBallotCoordString(ballotString));
			assert (coordInt != null);
			Ballot ballot =
					new Ballot(Ballot.getBallotNumString(ballotString),
							coordInt);
			json.put(PaxosPacket.NodeIDKeys.BALLOT.toString(),
				ballot.toString());
		}
		else if (json.has(PaxosPacket.NodeIDKeys.GROUP.toString())) {
			// fix group string (JSONArray)
			JSONArray jsonArray =
					json.getJSONArray(PaxosPacket.NodeIDKeys.GROUP.toString());
			for (int i = 0; i < jsonArray.length(); i++) {
				String memberString = jsonArray.getString(i);
				int memberInt = stringToInt(memberString);
				jsonArray.put(i, memberInt);
			}
		}
		else for (PaxosPacket.NodeIDKeys key : PaxosPacket.NodeIDKeys.values()) {
			if (json.has(key.toString())) {
				// fix default node string
				String nodeString = json.getString(key.toString());
				int nodeInt = stringToInt(nodeString);
				json.put(key.toString(), nodeInt);
			}
		}
		return json;
	}

	// converts string to int by (*necessarily*) going through NodeIDType
	private int stringToInt(String nodeString) {
		NodeIDType node = this.unstringer.valueOf(nodeString);
		int nodeInt = this.nodeMap.put(node);
		return nodeInt;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}
}