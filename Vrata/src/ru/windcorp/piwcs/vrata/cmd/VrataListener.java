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

@SuppressWarnings("static-method")
public class VrataListener implements Listener {
	
	public static interface VrataPlayerHandler {

		default boolean onChat(String message) {
			return false;
		}
		
		void onInventoryOpened();
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
				handler.onChat(e.getMessage());
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
				handler.onInventoryOpened();
			}
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
