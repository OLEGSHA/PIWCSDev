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

import ru.windcorp.piwcs.nestedcmd.NCComplaintException;
import ru.windcorp.piwcs.vrata.VrataLogger;
import ru.windcorp.piwcs.vrata.VrataPlugin;
import ru.windcorp.piwcs.vrata.cmd.VrataListener.VrataPlayerHandler;
import ru.windcorp.piwcs.vrata.crates.Crate;
import ru.windcorp.piwcs.vrata.crates.Package;
import ru.windcorp.piwcs.vrata.crates.Vrata;
import ru.windcorp.piwcs.vrata.exceptions.VrataOperationException;
import ru.windcorp.piwcs.vrata.users.VrataUser;

import static ru.windcorp.piwcs.vrata.VrataTemplates.*;

import org.bukkit.inventory.Inventory;

public class VrataDeployer implements VrataPlayerHandler {
	
	private final VrataUser user;
	private boolean displaySkips = false;
	
	protected VrataDeployer(VrataUser user, boolean displaySkips) {
		this.user = user;
		this.displaySkips = displaySkips;
	}
	
	public static boolean startDeploying(VrataUser user, String firstSorter, boolean displaySkips) {
		VrataDeployer deployer = new VrataDeployer(user, displaySkips);
		if (!VrataListener.registerHandler(user.getPlayer(), deployer)) {
			return false;
		}
		
		user.getCurrentPackage().sort(firstSorter);
		
		user.sendMessage(getf("cmd.deploy.intro", user.getCurrentPackage()));
		deployer.selectAndDisplayNextCrate();
		return true;
	}
	
	@Override
	public void onInventoryOpened(Inventory inventory) {
		if (!Vrata.isAContainer(inventory)) return;
		
		Package pkg = user.getCurrentPackage();
		Crate crate = pkg.getNext();
		
		try {
			Vrata.exportContainer(user.getPlayer(), crate);
			pkg.remove(crate);
		} catch (VrataOperationException e) {
			user.sendMessage(getf("cmd.deploy.problem.exception", e.getMessage(), String.valueOf(e.getCause())));
			String msg = String.format("User %s attempted to deploy a crate to %s but caused an exception: %s",
					user, e, Vrata.describeInventory(inventory, user.getPlayer()));
			VrataLogger.write(msg);
			VrataPlugin.getInst().getLogger().severe(msg);
			e.printStackTrace();
		} catch (NCComplaintException e) {
			user.sendMessage(e.getMessage());
			return;
		}

		user.sendMessage(getf("cmd.deploy.deployed"));
		VrataLogger.write("%s deployed crate %s (UUID %s) from package %s (UUID %s) to %s",
				user,
				crate, crate.getUuid(),
				pkg, pkg.getUuid(),
				Vrata.describeInventory(inventory, user.getPlayer()));
		
		selectAndDisplayNextCrate();
	}
	
	private void selectAndDisplayNextCrate() {
		StringBuilder skippedDescription = null;
		int skippedAmount = 0;
		
		Package pkg = user.getCurrentPackage();
		Crate crate = null;
		
		while (true) {
			crate = pkg.getNext();
			if (crate == null) break;
			if (crate.canDeploy(user)) break;
			
			skippedAmount++;
			if (skippedDescription == null) {
				skippedDescription = new StringBuilder(crate.toString());
			} else {
				skippedDescription.append(" ").append(crate.toString());
			}
			pkg.skip();
		}
		
		if (crate == null) {
			user.sendMessage(get("cmd.deploy.outOfCrates"));
			VrataListener.unregisterHandler(user.getPlayer());
			return;
		}
		
		user.sendMessage(getf("cmd.deploy.next", crate, crate.getSlots()));
		if (skippedDescription == null && displaySkips) {
			user.sendMessage(getf("cmd.deploy.skipped", skippedAmount, skippedDescription));
		}
	}
	
	@Override
	public boolean onChat(String message) {
		if (message.startsWith("!")) {
			return false;
		}
		
		if ("$info".equalsIgnoreCase(message)) {
			Crate crate = user.getCurrentPackage().getNext();
			user.sendMessage(getf("cmd.deploy.crateInfo",
					crate.toString(),
					crate.getSlots(),
					crate.getUuid(),
					crate.getPackage(),
					crate.getPackageUuid(),
					crate.getCreationTime()));
		} else if ("$skip".equalsIgnoreCase(message)) {
			user.getCurrentPackage().skip();
			selectAndDisplayNextCrate();
		} else if ("$displaySkips".equalsIgnoreCase(message)) {
			if (displaySkips) {
				displaySkips = false;
				user.sendMessage(getf("cmd.deploy.displaySkips.nowFalse"));
			} else {
				displaySkips = true;
				user.sendMessage(getf("cmd.deploy.displaySkips.nowTrue"));
			}
		} else {
			user.getCurrentPackage().sort(message);
			user.sendMessage(getf("cmd.deploy.sort", message));
			selectAndDisplayNextCrate();
		}
		
		return true;
	}

	@Override
	public void onUnregistered() {
		user.getCurrentPackage().sort(null);
		user.sendMessage(get("cmd.deploy.stopped"));
	}

}
