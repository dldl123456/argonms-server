/*
 * ArgonMS MapleStory server emulator written in Java
 * Copyright (C) 2011-2013  GoldenKevin
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package argonms.shop.net.internal;

import argonms.common.net.internal.CenterRemoteOps;
import argonms.common.net.internal.CenterRemotePacketProcessor;
import argonms.common.net.internal.RemoteCenterInterface;
import argonms.common.util.input.LittleEndianReader;
import argonms.shop.ShopServer;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Processes packet sent from the center server and received at the shop
 * server.
 * @author GoldenKevin
 */
public class CenterShopPacketProcessor extends CenterRemotePacketProcessor {
	private static final Logger LOG = Logger.getLogger(CenterShopPacketProcessor.class.getName());

	private final ShopServer local;

	public CenterShopPacketProcessor(ShopServer ls) {
		local = ls;
	}

	@Override
	public void process(LittleEndianReader packet, RemoteCenterInterface r) {
		switch (packet.readByte()) {
			case CenterRemoteOps.AUTH_RESPONSE:
				processAuthResponse(packet, r.getLocalServer());
				break;
			case CenterRemoteOps.PING:
				r.getSession().send(pongMessage());
				break;
			case CenterRemoteOps.PONG:
				r.getSession().receivedPong();
				break;
			case CenterRemoteOps.GAME_CONNECTED:
				processGameConnected(packet);
				break;
			case CenterRemoteOps.GAME_DISCONNECTED:
				processGameDisconnected(packet);
				break;
			case CenterRemoteOps.CHANNEL_PORT_CHANGE:
				processChannelPortChange(packet);
				break;
			case CenterRemoteOps.SHOP_CHANNEL_SHOP_SYNCHRONIZATION:
				processChannelShopSynchronization(packet);
				break;
			case CenterRemoteOps.CENTER_SERVER_SYNCHRONIZATION:
				processCenterServerSynchronization(packet);
				break;
			default:
				LOG.log(Level.FINE, "Received unhandled interserver packet {0} bytes long:\n{1}", new Object[] { packet.available() + 2, packet });
				break;
		}
	}

	private void processGameConnected(LittleEndianReader packet) {
		byte serverId = packet.readByte();
		byte world = packet.readByte();
		String host = packet.readLengthPrefixedString();
		byte size = packet.readByte();
		Map<Byte, Integer> ports = new HashMap<Byte, Integer>(size);
		for (int i = 0; i < size; i++)
			ports.put(Byte.valueOf(packet.readByte()), Integer.valueOf(packet.readInt()));
		local.registerGame(serverId, world, host, ports);
	}

	private void processGameDisconnected(LittleEndianReader packet) {
		byte serverId = packet.readByte();
		byte world = packet.readByte();
		local.unregisterGame(serverId, world);
	}

	private void processChannelPortChange(LittleEndianReader packet) {
		byte world = packet.readByte();
		byte channel = packet.readByte();
		int newPort = packet.readInt();
		local.getWorld(Byte.valueOf(world)).setPort(channel, newPort);
		local.getCrossServerInterface().changeChannelPort(world, channel, newPort);
	}

	private void processChannelShopSynchronization(LittleEndianReader packet) {
		ShopServer.getInstance().getCrossServerInterface().receivedChannelShopSynchronizationPacket(packet);
	}

	private void processCenterServerSynchronization(LittleEndianReader packet) {
		ShopServer.getInstance().getCrossServerInterface().receivedCenterServerSynchronizationPacket(packet);
	}
}
