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

import ru.windcorp.piwcs.vrata.cmd.VrataListener.VrataPlayerHandler;
import ru.windcorp.piwcs.vrata.crates.Crate;
import ru.windcorp.piwcs.vrata.crates.Package;
import ru.windcorp.piwcs.vrata.crates.Vrata;
import ru.windcorp.piwcs.vrata.exceptions.VrataOperationException;
import ru.windcorp.piwcs.vrata.users.VrataUser;

import static ru.windcorp.piwcs.vrata.VrataTemplates.*;

import org.bukkit.inventory.Inventory;

import ru.windcorp.piwcs.vrata.VrataLogger;
import ru.windcorp.piwcs.vrata.VrataPlugin;

public class VrataPacker implements VrataPlayerHandler {
	
	private final String batch;
	private final VrataUser user;
	
	protected VrataPacker(String batch, VrataUser user) {
		this.batch = batch;
		this.user = user;
	}
	
	public static boolean startPacking(String batch, VrataUser user) {
		VrataPacker packer = new VrataPacker(batch, user);
		if (!VrataListener.registerHandler(user.getPlayer(), packer)) {
			return false;
		}
		user.sendMessage(getf("cmd.pack.intro", user.getCurrentPackage(), batch == null ? get("untitledBatch") : batch));
		return true;
	}
	
	@Override
	public void onInventoryOpened(Inventory inventory) {
		if (!Vrata.isAContainer(inventory)) return;
		
		Package pkg = user.getCurrentPackage();
		Crate crate;
		
		try {
			crate = Vrata.importContainer(user.getPlayer());
		} catch (VrataOperationException e) {
			user.sendMessage(getf("cmd.pack.problem.exception", e.getMessage(), String.valueOf(e.getCause())));
			String msg = String.format("User %s attempted to pack a crate from %s but caused an exception: %s",
					user, Vrata.describeInventory(inventory, user.getPlayer()), e);
			VrataLogger.write(msg);
			VrataPlugin.getInst().getLogger().severe(msg);
			e.printStackTrace();
			return;
		}

		crate.setBatch(batch);
		pkg.add(crate);
		
		VrataLogger.write("%s packed crate %s (UUID %s) into package %s (UUID %s) from %s",
				user,
				crate, crate.getUuid(),
				pkg, pkg.getUuid(),
				Vrata.describeInventory(inventory, user.getPlayer()));
		VrataLogger.writeCrate(crate);
		user.sendMessage(getf("cmd.pack.packed", crate));
		
	}
	
	@Override
	public void onUnregistered() {
		user.sendMessage(get("cmd.pack.stopped"));
	}

}
