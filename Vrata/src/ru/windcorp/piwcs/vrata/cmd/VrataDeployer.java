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
import ru.windcorp.piwcs.vrata.users.VrataUser;

import static ru.windcorp.piwcs.vrata.VrataTemplates.*;

public class VrataDeployer implements VrataPlayerHandler {
	
	private final VrataUser user;
	
	protected VrataDeployer(VrataUser user) {
		this.user = user;
		// TODO: setup DeploymentQueue
	}
	
	public static boolean startPacking(String batch, VrataUser user) {
//		VrataDeployer packer = new VrataDeployer(batch, user);
//		if (!VrataListener.registerHandler(user.getPlayer(), packer)) {
//			return false;
//		}
//		user.sendMessage(getf("cmd.pack.intro", user.getCurrentPackage(), batch == null ? get("untitledBatch") : batch));
//		return true;
		// TODO
		return true;
	}
	
	@Override
	public void onInventoryOpened() {
		// TODO
	}
	
	@Override
	public void onUnregistered() {
		user.sendMessage(get("cmd.pack.stopped"));
	}

}
