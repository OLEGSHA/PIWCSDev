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

package ru.windcorp.piwcs.vrata.users;

import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import ru.windcorp.piwcs.vrata.cmd.VrataCommandHandler;
import ru.windcorp.piwcs.vrata.crates.Crate;
import ru.windcorp.piwcs.vrata.crates.Package;

public class VrataUser {
	
	private final CommandSender sender;
	private final VrataUserProfile profile;
	
	private Package currentPackage = null;
	private List<Crate> currentDeploymentQueue = null;
	
	public VrataUser(CommandSender sender, VrataUserProfile profile) {
		this.sender = sender;
		this.profile = profile;
	}
	
	public CommandSender getSender() {
		return sender;
	}
	
	public Player getPlayer() {
		return (Player) sender;
	}
	
	public void sendMessage(String message) {
		getSender().sendMessage(message);
	}
	
	public VrataUserProfile getProfile() {
		return profile;
	}
	
	public Package getCurrentPackage() {
		return currentPackage;
	}
	
	public void setCurrentPackage(Package currentPackage) {
		VrataCommandHandler.onPackageSelectionChanged(this, this.currentPackage, currentPackage);
		this.currentPackage = currentPackage;
		this.currentDeploymentQueue = null;
	}
	
	public List<Crate> getCurrentDeploymentQueue() {
		return currentDeploymentQueue;
	}
	
	public void setCurrentDeploymentQueue(List<Crate> currentDeploymentQueue) {
		this.currentDeploymentQueue = currentDeploymentQueue;
	}
	
	@Override
	public String toString() {
		return getProfile().toString();
	}

}
