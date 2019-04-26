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

package ru.windcorp.piwcs.vrata.exceptions;

import java.util.Objects;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import ru.windcorp.piwcs.nestedcmd.NCPermissionException;
import ru.windcorp.piwcs.vrata.VrataTemplates;
import ru.windcorp.piwcs.vrata.crates.Package;
import ru.windcorp.piwcs.vrata.users.VrataUser;
import ru.windcorp.piwcs.vrata.users.VrataUserProfile;
import ru.windcorp.piwcs.vrata.users.VrataUsers;

public class VrataPermissionException extends NCPermissionException {
	
	private static final long serialVersionUID = -3181709120844515138L;
	
	private final String fullMessage;

	protected VrataPermissionException(String userMessage, String fullMessage) {
		super(userMessage);
		this.fullMessage = fullMessage;
	}
	
	public String getFullMessage() {
		return fullMessage;
	}
	
	private static final Object[] EMPTY_ARGS = new Object[0];

	public static VrataPermissionException create(String key, Object[] fullOnlyArgs, Object... args) {
		if (fullOnlyArgs == null) fullOnlyArgs = EMPTY_ARGS;
		Objects.requireNonNull(args, "args is null");
		
		Object[] fullArray = new Object[fullOnlyArgs.length + args.length];
		System.arraycopy(args, 0, fullArray, 0, args.length);
		System.arraycopy(fullOnlyArgs, 0, fullArray, args.length, fullOnlyArgs.length);
		
		return new VrataPermissionException(VrataTemplates.getf(key, args), VrataTemplates.getf(key + ".full", fullArray));
	}
	
	public static VrataPermissionException checkModerator(VrataUserProfile user) {
		if (!user.canModerate()) {
			String problem = VrataTemplates.get("problem.notMod");
			return new VrataPermissionException(problem, problem);
		}
		return null;
	}
	
	public static VrataPermissionException checkAdmin(VrataUserProfile user) {
		if (!user.isAdmin()) {
			String problem = VrataTemplates.get("problem.notAdmin");
			return new VrataPermissionException(problem, problem);
		}
		return null;
	}
	
	public static VrataPermissionException checkOwner(VrataUser user, Package pkg) {
		if (user.getProfile().canModerate()) return null;
		if (!pkg.isOwner(user.getPlayer())) {
			String problem = VrataTemplates.getf("problem.notOwner", pkg);
			return new VrataPermissionException(problem, problem);
		}
		return null;
	}
	
	@Override
	public void report(CommandSender sender) {
		if (!(sender instanceof Player) || VrataUsers.getProfile(sender.getName()).canModerate()) {
			sender.sendMessage(getFullMessage());
		} else {
			sender.sendMessage(getMessage());
		}
	}

}
