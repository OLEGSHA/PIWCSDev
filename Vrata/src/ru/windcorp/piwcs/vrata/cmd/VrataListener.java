/*
 * PIWCS Vrata Plugin
 * Copyright (C) 2019  PIWCS Team
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package ru.windcorp.piwcs.vrata.cmd;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;

import ru.windcorp.piwcs.vrata.VrataUserInterface;
import ru.windcorp.piwcs.vrata.users.VrataUsers;

@SuppressWarnings("static-method")
public class VrataListener implements Listener {
	
	public static interface VrataPlayerHandler {

		String onChat(String message);
		
		boolean onInventoryOpened(Inventory inventory);
		void onUnregistered();
		
	}

	private static final Map<Player, VrataPlayerHandler> HANDLERS = Collections.synchronizedMap(new WeakHashMap<>());
	
	@SuppressWarnings("deprecation")
	@EventHandler
	public void onPlayerChat(org.bukkit.event.player.PlayerChatEvent e) {
		if (e.isCancelled()) {
			return;
		}
		
		synchronized (HANDLERS) {
			VrataPlayerHandler handler = HANDLERS.get(e.getPlayer());
			if (handler != null) {
				String msg = handler.onChat(e.getMessage());
				if (msg == null) {
					e.setCancelled(true);
				} else if (msg != e.getMessage()) {
					e.setMessage(msg);
				}
			}
		}
	}
	
	@EventHandler
	public void onPlayerOpenInventory(InventoryOpenEvent e) {
		if (e.isCancelled()) {
			return;
		}
		
		synchronized (HANDLERS) {
			VrataPlayerHandler handler = HANDLERS.get(e.getPlayer());
			if (handler != null) {
				e.setCancelled(handler.onInventoryOpened(e.getInventory()));
			}
		}
	}
	
	@EventHandler
	public void onPlayerQuit(PlayerQuitEvent e) {
		VrataUserInterface.onPlayerQuitting(VrataUsers.getUser(e.getPlayer()));
	}
	
	@EventHandler
	public void onPlayerKicked(PlayerKickEvent e) {
		VrataUserInterface.onPlayerQuitting(VrataUsers.getUser(e.getPlayer()));
	}
	
	public static void onStopping() {
		synchronized (HANDLERS) {
			for (VrataPlayerHandler handler : HANDLERS.values()) {
				handler.onUnregistered();
			}
			HANDLERS.clear();
		}
	}
	
	public static boolean registerHandler(Player player, VrataPlayerHandler handler) {
		return HANDLERS.putIfAbsent(player, handler) == null;
	}
	
	public static boolean unregisterHandler(Player player) {
		VrataPlayerHandler handler = HANDLERS.remove(player);
		if (handler != null) handler.onUnregistered();
		return handler != null;
	}
}
