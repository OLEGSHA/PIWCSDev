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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import ru.windcorp.piwcs.nestedcmd.*;
import ru.windcorp.piwcs.vrata.VrataLogger;
import ru.windcorp.piwcs.vrata.VrataUserInterface;
import ru.windcorp.piwcs.vrata.exceptions.VrataPermissionException;
import ru.windcorp.tge2.util.StringUtil;
import ru.windcorp.piwcs.vrata.crates.Package;

import static ru.windcorp.piwcs.vrata.VrataTemplates.*;
import static ru.windcorp.piwcs.vrata.users.VrataUsers.*;

public class VrataCommandHandler implements CommandExecutor {
	
	private final SubCommandRegistry rootCommand;
	
	public VrataCommandHandler() {
		String helpFormat = get("cmd.helpFormat");
		String helpHeader = get("cmd.helpHeader");
		
		CommandSenderFilter isPlayer =
				sender -> sender instanceof Player ? null : VrataPermissionException.create("problem.notPlayer", null);
		CommandSenderFilter canModerate =
				sender -> sender instanceof Player ? null : VrataPermissionException.checkModerator(getProfile(sender.getName()));
		CommandSenderFilter isAdmin =
				sender -> sender instanceof Player ? null : VrataPermissionException.checkAdmin(getProfile(sender.getName()));
		
		this.rootCommand = new SubCommandRegistry("vrata", get("cmd.root.desc"),
				null,
				helpFormat,
				helpHeader,
				
				new SubCommand(
						"new",
						get("cmd.new.desc"), get("cmd.new.syntax"),
						VrataCommandHandler::cmdNew,
						null)
				
				);
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] argsAsArray) {
		List<String> args = new LinkedList<>();
		for (String arg : argsAsArray) args.add(arg);
		
		VrataLogger.write("%s issued command %s", sender, StringUtil.arrayToString(argsAsArray, " "));
		
		rootCommand.run(sender, args, label);
		return true;
	}
	
	private static void cmdNew(CommandSender sender, List<String> args, String fullCommand) throws NCSyntaxException, NCComplaintException {
		if (args.isEmpty()) {
			throw new NCSyntaxException(get("cmd.new.problem.noName"));
		}
		
		String name = args.remove(0).replace('_', ' ').trim();
		if (name.isEmpty()) throw new NCComplaintException(get("cmd.new.problem.nameEmpty"));
			
		Set<String> owners = args.stream().map(String::toLowerCase).collect(Collectors.toCollection(HashSet::new));
		
		if (!owners.remove("~") && (sender instanceof Player)) {
			owners.add(sender.getName());
		}
		
		Package pkg = VrataUserInterface.createNewPackage(name);
		owners.forEach(owner -> VrataUserInterface.addPackageOwner(pkg, owner));
		if (sender instanceof Player) {
			//VrataUserInterface.selectPackage(pkg, VrataUser.);
			// TODO: select package
			// TODO: make an abstract VrataUser and VrataUserProfile for non-players and refactor everything to fit the new design
		}
	}

}
