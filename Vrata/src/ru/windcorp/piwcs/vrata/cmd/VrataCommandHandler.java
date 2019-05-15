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
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import ru.windcorp.piwcs.nestedcmd.*;
import ru.windcorp.piwcs.vrata.VrataLogger;
import ru.windcorp.piwcs.vrata.VrataUserInterface;
import ru.windcorp.piwcs.vrata.exceptions.VrataPermissionException;
import ru.windcorp.piwcs.vrata.users.VrataUser;
import ru.windcorp.piwcs.vrata.users.VrataUsers;
import ru.windcorp.tge2.util.StringUtil;
import ru.windcorp.piwcs.vrata.crates.Package;
import ru.windcorp.piwcs.vrata.crates.Packages;

import static ru.windcorp.piwcs.vrata.VrataTemplates.*;
import static ru.windcorp.piwcs.vrata.users.VrataUsers.*;

public class VrataCommandHandler implements CommandExecutor {
	
	private final static Pattern UUID_REGEX = Pattern.compile("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");
	
	private final SubCommandRegistry rootCommand;
	
	public VrataCommandHandler() {
		String helpFormat = get("cmd.helpFormat");
		String helpHeader = get("cmd.helpHeader");
		
		CommandSenderFilter isPlayer =
				sender -> sender instanceof Player ? null : VrataPermissionException.create("problem.notPlayer", null);
		CommandSenderFilter canModerate =
				sender -> sender instanceof Player ? null : VrataPermissionException.checkModerator(getPlayerProfile(sender.getName()));
		CommandSenderFilter isAdmin =
				sender -> sender instanceof Player ? null : VrataPermissionException.checkAdmin(getPlayerProfile(sender.getName()));
				
		SubCommandExecutor checkPackageSelected = VrataCommandHandler::checkPackageSelection;
		
		this.rootCommand = new SubCommandRegistry("vrata", get("cmd.root.desc"),
				null,
				helpFormat,
				helpHeader,
				
				new SubCommand(
						"new",
						get("cmd.new.desc"), get("cmd.new.syntax"),
						VrataCommandHandler::cmdNew,
						null),
				
				new SubCommand(
						new String[] {"select", "sel"},
						get("cmd.select.desc"), get("cmd.select.syntax"),
						VrataCommandHandler::cmdSelect,
						null),
				
				new SubCommand(
						StringUtil.allCombinations(
								new String[] {"remove", "delete", "rem", "del"},
								new String[] {"Package", "Pkg"}),
						get("cmd.removePackage.desc"), "",
						checkPackageSelected.then(VrataCommandHandler::cmdRemovePackage),
						null),
				
				new SubCommand(
						StringUtil.allCombinations(
								new String[] {"list", "view"},
								new String[] {"Package", "Pkg"},
								new String[] {"s", ""}),
						get("cmd.listPackages.desc"), "",
						VrataCommandHandler::cmdListPackages,
						null),
				
				new SubCommand(
						"pack",
						get("cmd.pack.desc"), get("cmd.pack.syntax"),
						checkPackageSelected.then(VrataCommandHandler::cmdPack),
						isPlayer),
				
				new SubCommand(
						new String[] {"deploy", "unpack"},
						get("cmd.deploy.desc"), "",
						checkPackageSelected.then(VrataCommandHandler::cmdDeploy),
						isPlayer),
				
				new SubCommand("stop",
						get("cmd.stop.desc"), "",
						VrataCommandHandler::cmdStop,
						isPlayer)
				
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
	
	private static void cmdNew(CommandSender sender, List<String> args, String fullCommand)
			throws NCSyntaxException, NCComplaintException, VrataPermissionException {
		if (args.isEmpty()) {
			throw new NCSyntaxException(get("cmd.new.problem.noName"));
		}
		
		String name = args.remove(0).replace('_', ' ').trim();
		if (name.isEmpty()) throw new NCComplaintException(get("cmd.new.problem.nameEmpty"));
		if (isUUIDString(name)) throw new NCComplaintException(get("cmd.new.problem.newIsAUUID"));
			
		Set<String> owners = args.stream().map(String::toLowerCase).collect(Collectors.toCollection(HashSet::new));
		
		if (!owners.remove("~") && (sender instanceof Player)) {
			owners.add(sender.getName());
		}
		
		Package pkg = VrataUserInterface.createNewPackage(name);
		owners.forEach(owner -> VrataUserInterface.addPackageOwner(pkg, owner));
		VrataUserInterface.selectPackage(pkg, VrataUsers.getUser(sender));
		
		sender.sendMessage(getf("cmd.new.success", pkg, pkg.getUuid()));
	}
	
	private static void cmdSelect(CommandSender sender, List<String> args, String fullCommand)
			throws NCSyntaxException, NCComplaintException, VrataPermissionException {
		VrataUser user = VrataUsers.getUser(sender);
		
		if (args.isEmpty()) {
			Package pkg = user.getCurrentPackage();
			if (pkg == null) {
				throw new NCComplaintException(get("cmd.select.problem.nothingSelected"));
			} else {
				pkg.setCurrentUser(null);
				user.sendMessage(getf("cmd.select.success.deselect", pkg));
			}
			return;
		}
		
		String query = args.get(0);
		Package pkg = null;
		
		if (isUUIDString(query)) {
			UUID uuid = UUID.fromString(query);
			pkg = Packages.getPackage(uuid);
			if (pkg == null) throw new NCComplaintException(getf("cmd.select.problem.noMatch.forUUID", uuid));
		} else {
			List<Package> pkgs = new ArrayList<>();
			
			for (Package p : Packages.getPackages()) {
				if (query.equalsIgnoreCase(p.getName())) {
					pkg = p;
					break;
				} else if (p.getName().startsWith(query)) {
					pkgs.add(p);
				} else if (p.getUuid().toString().startsWith(query)) {
					pkgs.add(p);
				}
			}
			
			if (pkg == null) {
				if (pkgs.isEmpty()) {
					throw new NCComplaintException(getf("cmd.select.problem.noMatch.forName", query));
				} else if (pkgs.size() == 1) {
					pkg = pkgs.get(0);
				} else if (args.size() == 2) {
					String uuidSpec = args.get(1);
					pkgs.removeIf(p -> !p.getUuid().toString().startsWith(uuidSpec));
					
					if (pkgs.isEmpty()) {
						throw new NCComplaintException(getf("cmd.select.problem.noMatch.forNameAndUUID", query, uuidSpec));
					} else if (pkgs.size() == 1) {
						pkg = pkgs.get(0);
					} else {
						user.sendMessage(getf("cmd.select.problem.manyMatches.forNameAndUUID", query, uuidSpec));
						sendPackageList(pkgs, user::sendMessage);
						user.sendMessage(get("cmd.select.problem.manyMatches.advice"));
						return;
					}
				} else {
					user.sendMessage(getf("cmd.select.problem.manyMatches.forName", query));
					sendPackageList(pkgs, user::sendMessage);
					user.sendMessage(get("cmd.select.problem.manyMatches.advice"));
					return;
				}
			}
		}
		
		VrataUserInterface.selectPackage(pkg, user);
		user.sendMessage(get("cmd.select.success.select"));
	}
	
	private static void cmdRemovePackage(CommandSender sender, List<String> args, String fullCommand)
			throws NCComplaintException, VrataPermissionException, NCSyntaxException {
		Package pkg = VrataUsers.getUser(sender).getCurrentPackage();
		if (!args.isEmpty()) {
			throw new NCSyntaxException(getf("cmd.removePackage.problem.args", pkg));
		}
		VrataUserInterface.removePackage(pkg);
		sender.sendMessage(getf("cmd.removePackage.success"));
	}
	
	private static void cmdListPackages(CommandSender sender, List<String> args, String fullCommand)
			throws NCComplaintException, VrataPermissionException {
		
		VrataUser user = VrataUsers.getUser(sender);
		Collection<Package> pkgs = Packages.packages()
				.filter(p -> p.getOwners()
				.contains(user.getProfile().getName()))
				.collect(Collectors.toList());
		
		if (pkgs.isEmpty()) {
			user.sendMessage(get("cmd.listPackages.success.noPackages"));
			return;
		}
		
		if (pkgs.size() == 1) {
			user.sendMessage(get("cmd.listPackages.success.onePackage"));
			Package theOne = pkgs.iterator().next();
			user.sendMessage(getf("cmd.packageListFormat", theOne.getName(), theOne.getUuid(), 1));
			return;
		}
		
		user.sendMessage(getf("cmd.listPackages.success.manyPackages", pkgs.size()));
		sendPackageList(pkgs, user::sendMessage);
	}
	
	private static void cmdPack(CommandSender sender, List<String> args, String fullCommand) {
		String batch = null;
		if (!args.isEmpty()) {
			batch = args.get(0);
		}
		VrataPacker.startPacking(batch, VrataUsers.getUser(sender));
	}
	
	private static void cmdDeploy(CommandSender sender, List<String> args, String fullCommand) {
		// TODO
	}
	
	private static void cmdStop(CommandSender sender, List<String> args, String fullCommand) {
		if (!VrataListener.unregisterHandler(VrataUsers.getUser(sender).getPlayer())) {
			sender.sendMessage(get("cmd.stop.problem.nothingToStop"));
		}
	}
	
	public static void onPackageSelectionChanged(VrataUser user, Package oldPkg, Package newPkg) {
		if (user.isPlayer()) VrataListener.unregisterHandler(user.getPlayer());
		VrataUserInterface.onPackageSelectionChanged(user, oldPkg, newPkg);
	}
	
	private static void checkPackageSelection(CommandSender sender, List<String> args, String fullCommand)
			throws NCComplaintException {
		if (VrataUsers.getUser(sender).getCurrentPackage() == null) {
			throw new NCComplaintException(get("cmd.noPackageSelected"));
		}
	}
	
	private static boolean isUUIDString(String declar) {
		return UUID_REGEX.matcher(declar).matches();
	}
	
	private static void sendPackageList(Iterable<Package> packages, Consumer<String> sendMessage) {
		int i = 1;
		for (Package pkg : packages) {
			sendMessage.accept(getf("cmd.packageListFormat", pkg.getName(), pkg.getUuid(), i++));
		}
	}

}
