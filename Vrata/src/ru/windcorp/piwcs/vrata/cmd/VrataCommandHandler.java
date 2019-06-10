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
import ru.windcorp.piwcs.vrata.VrataPlugin;
import ru.windcorp.piwcs.vrata.VrataUserInterface;
import ru.windcorp.piwcs.vrata.exceptions.VrataPermissionException;
import ru.windcorp.piwcs.vrata.users.VrataUser;
import ru.windcorp.piwcs.vrata.users.VrataUserProfile;
import ru.windcorp.piwcs.vrata.users.VrataUserProfile.Status;
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
				sender -> sender instanceof Player ? VrataPermissionException.create("problem.notPlayer", null) : null;
		CommandSenderFilter canModerate =
				sender -> sender instanceof Player ? VrataPermissionException.checkModerator(getPlayerProfile(sender.getName())) : null;
		CommandSenderFilter isAdmin =
				sender -> sender instanceof Player ? VrataPermissionException.checkAdmin(getPlayerProfile(sender.getName())) : null;
				
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
						new String[] {"select", "sel"},// TODO: BUG ignores duplicates
						get("cmd.select.desc"), get("cmd.select.syntax"),
						VrataCommandHandler::cmdSelect,
						null),
				
				new SubCommand(
						new String[] {"deselect", "desel"},
						get("cmd.deselect.desc"), "",
						VrataCommandHandler::cmdDeselect,
						null),
				
				new SubCommand(
						StringUtil.allCombinations(
								new String[] {"remove", "delete", "rem", "del", "rm"},
								new String[] {"Package", "Pkg", ""}),
						get("cmd.removePackage.desc"), "",
						checkPackageSelected.then(VrataCommandHandler::cmdRemovePackage),
						null),
				
				new SubCommand(
						StringUtil.allCombinations(
								new String[] {"list", "view"},
								new String[] {"Package", "Pkg", ""},
								new String[] {"s", ""}),
						get("cmd.listPackages.desc"), "",//TODO: BUG does not display anything
						VrataCommandHandler::cmdListPackages,
						null),
				
				new SubCommand(
						"pack",
						get("cmd.pack.desc"), get("cmd.pack.syntax"),
						checkPackageSelected.then(VrataCommandHandler::cmdPack),
						isPlayer),
				
				new SubCommand(
						new String[] {"deploy", "unpack"},
						get("cmd.deploy.desc"), get("cmd.deploy.syntax"),
						checkPackageSelected.then(VrataCommandHandler::cmdDeploy),
						isPlayer),
				
				new SubCommand(
						"stop",
						get("cmd.stop.desc"), "",
						VrataCommandHandler::cmdStop,
						isPlayer),
				
				new SubCommand(
						new String[] {"info", "view"},
						get("cmd.info.desc"), "",
						checkPackageSelected.then(VrataCommandHandler::cmdInfo),
						null),
				
				new SubCommand(
						"save",
						get("cmd.save.desc"), "",
						VrataCommandHandler::cmdSave,
						null),
				
				new SubCommandRegistry(
						"users",
						get("cmd.users.desc"),
						isAdmin,
						helpFormat,
						helpHeader,
						
						new SubCommand(
								"info",
								get("cmd.users.info.desc"), get("cmd.users.info.syntax"),
								VrataCommandHandler::cmdUsersInfo,
								null),
						
						new SubCommand(
								"status",
								get("cmd.users.status.desc"), get("cmd.users.status.syntax"),
								VrataCommandHandler::cmdUsersStatus,
								null)
						
						)
				
				// TODO: CMD moderation
				
				);
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] argsAsArray) {
		List<String> args = new LinkedList<>();
		for (String arg : argsAsArray) args.add(arg);
		
		StringBuilder sb = new StringBuilder(label);
		for (String arg : argsAsArray) sb.append(" ").append(arg);
		
		VrataLogger.write("%s issued command /%s", sender.getName(), sb.toString());
		
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
			
		Set<String> owners = new HashSet<>(args);
		
		if (!owners.remove("~") && (sender instanceof Player)) {
			owners.add(sender.getName());
		}
		
		Package pkg = VrataUserInterface.createNewPackage(name);
		owners.forEach(owner -> VrataUserInterface.addPackageOwner(pkg, owner));
		VrataUserInterface.selectPackage(pkg, VrataUsers.getUser(sender));
		
		sender.sendMessage(getf("cmd.new.success", pkg));
	}
	
	private static void cmdSelect(CommandSender sender, List<String> args, String fullCommand)
			throws NCSyntaxException, NCComplaintException, VrataPermissionException {
		
		VrataUser user = VrataUsers.getUser(sender);
		boolean canSelectAny = user.getProfile().isModerator();
		
		if (args.isEmpty()) {
			cmdInfo(sender, args, fullCommand);
			return;
		}
		
		String selector = args.get(0);
		Package match = null;
		
		if (isUUIDString(selector)) {
			UUID uuid = UUID.fromString(selector);
			match = Packages.getPackage(uuid);
			if (match == null) throw new NCComplaintException(getf("cmd.select.problem.noMatch.forUUID", uuid));
		} else {
			List<Package> pkgs = new ArrayList<>();
			
			for (Package p : Packages.getPackages()) {
				if (p.getName().equalsIgnoreCase(selector)) {
					match = p;
					break;
				}
				
				if (!canSelectAny && !p.isOwner(user)) continue;
				
				if (
						   p.toString().startsWith(selector)
						|| p.getName().startsWith(selector)
						|| p.getUuid().toString().startsWith(selector)
						
						) pkgs.add(p);
			}
			
			if (match == null) switch (pkgs.size()) {
			case 0:
				throw new NCComplaintException(getf("cmd.select.problem.noMatch.forName", selector));
			case 1:
				match = pkgs.get(0);
				break;
			default:
				user.sendMessage(getf("cmd.select.problem.manyMatches.forName", selector));
				sendPackageList(pkgs, user::sendMessage);
				user.sendMessage(get("cmd.select.problem.manyMatches.advice"));
				return;
			}
		}
		
		if (user.getCurrentPackage() == match) {
			user.sendMessage(getf("cmd.select.problem.alreadySelected", match));
			return;
		}
		
		VrataUserInterface.selectPackage(match, user);
		user.sendMessage(getf("cmd.select.success.select", match));
	}
	
	private static void cmdDeselect(CommandSender sender, List<String> args, String fullCommand)
			throws NCComplaintException, VrataPermissionException {
		
		VrataUser user = getUser(sender);
		
		if (user.getCurrentPackage() == null) {
			user.sendMessage(get("cmd.deselect.success.nothingSelected"));
		} else {
			VrataUserInterface.selectPackage(null, user);
			user.sendMessage(get("cmd.deselect.success.deselected"));
		}
	}
	
	private static void cmdRemovePackage(CommandSender sender, List<String> args, String fullCommand)
			throws NCComplaintException, VrataPermissionException, NCSyntaxException {
		Package pkg = VrataUsers.getUser(sender).getCurrentPackage();
		if (!args.isEmpty()) {
			throw new NCSyntaxException(getf("cmd.removePackage.problem.args", pkg));
		}
		VrataUserInterface.removePackage(pkg);
		sender.sendMessage(getf("cmd.removePackage.success", pkg));
	}
	
	private static void cmdListPackages(CommandSender sender, List<String> args, String fullCommand)
			throws NCComplaintException, VrataPermissionException {
		
		VrataUser user = VrataUsers.getUser(sender);
		List<Package> pkgs = Packages.packages()
				.filter(p -> p.isOwner(user))
				.collect(Collectors.toList());
		
		if (pkgs.isEmpty()) {
			user.sendMessage(get("cmd.listPackages.success.noPackages"));
			return;
		}
		
		if (pkgs.size() == 1) {
			user.sendMessage(get("cmd.listPackages.success.onePackage"));
			Package theOne = pkgs.get(0);
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
		boolean displaySkips = args.remove("displaySkips");
		String firstSorter = args.isEmpty() ? null : args.get(0);
		VrataDeployer.startDeploying(VrataUsers.getUser(sender), firstSorter, displaySkips);
	}
	
	private static void cmdStop(CommandSender sender, List<String> args, String fullCommand) {
		if (!VrataListener.unregisterHandler(VrataUsers.getUser(sender).getPlayer())) {
			sender.sendMessage(get("cmd.stop.problem.nothingToStop"));
		}
	}
	
	private static void cmdInfo(CommandSender sender, List<String> args, String fullCommand) {
		VrataUser user = getUser(sender);
		Package pkg = user.getCurrentPackage();
		synchronized (pkg) {
			user.sendMessage(getf("cmd.info.header",
					pkg.getName(),
					pkg.toString(),
					pkg.getUuid().toString(),
					StringUtil.iterableToString(pkg.getOwners(), ", ")));
		
			int pkgSize = pkg.size();
			switch (pkg.size()) {
			case 0:
				user.sendMessage(get("cmd.info.empty"));
				return;
			case 1:
				user.sendMessage(get("cmd.info.single"));
				user.sendMessage(getf("cmd.info.crates", pkg.iterator().next()));
				return;
			default:
				user.sendMessage(getf("cmd.info.multiple", pkgSize));
				user.sendMessage(getf("cmd.info.crates", StringUtil.iterableToString(pkg, " ")));
				return;
			}
		}
	}
	
	private static void cmdSave(CommandSender sender, List<String> args, String fullCommand) {
		VrataPlugin.save();
		sender.sendMessage(get("cmd.save.success"));
	}
	
	private static void cmdUsersInfo(CommandSender sender, List<String> args, String fullCommand)
			throws NCSyntaxException, NCComplaintException {
		VrataUserProfile profile;
		VrataUser user = null;
		
		if (args.isEmpty()) {
			user = getUser(sender);
			profile = user.getProfile();
		} else {
			profile = getExistingPlayerProfile(args.get(0));
			
			if (profile == null) {
				throw new NCComplaintException(getf("cmd.users.problem.notFound", args.get(0)));
			}
		}
		
		String ownedPackages = StringUtil.iteratorToString(
				Packages.packages().filter(p -> p.isOwner(profile.getName())).iterator(),
				", ",
				get("cmd.users.info.success.noPackages"),
				"null",
				"{null}");
		
		String selectedPackage;
		
		{
			if (user == null) user = getOnlineUser(profile);
			if (user == null) {
				selectedPackage = get("cmd.users.info.success.selectedPackage.notOnline");
			} else if (user.getCurrentPackage() == null) {
				selectedPackage = get("cmd.users.info.success.selectedPackage.empty");
			} else {
				selectedPackage = user.getCurrentPackage().toString();
			}
		}
		
		sender.sendMessage(getf("cmd.users.info.success", profile.getName(), profile.getStatus(), selectedPackage, ownedPackages));
	}
	
	private static void cmdUsersStatus(CommandSender sender, List<String> args, String fullCommand)
			throws NCSyntaxException, NCComplaintException {
		VrataUserProfile profile;
		int statusArgIndex;
		
		if (args.isEmpty()) {
			if (!(sender instanceof Player)) {
				throw new NCSyntaxException(get("cmd.users.problem.notPlayer"));
			}
			
			profile = getUser(sender).getProfile();
			statusArgIndex = 0;
		} else {
			profile = getExistingPlayerProfile(args.get(0));
			
			if (profile == null) statusArgIndex = 0;
			else statusArgIndex = 1;
		}
		
		if (args.size() <= statusArgIndex) {
			throw new NCSyntaxException(get("cmd.users.status.problem.noStatus"));
		}
		
		if (profile.getStatus() == Status.NON_PLAYER) {
			throw new NCComplaintException(getf("cmd.users.status.problem.cannotChangeNonPlayer"));
		}
		
		String newStatusString = args.get(statusArgIndex);
		Status newStatus = null;
		
		try {
			newStatus = Status.values()[Integer.parseInt(newStatusString)];
		} catch (NumberFormatException e) {
			try {
				newStatus = Status.valueOf(newStatusString.toUpperCase());
			} catch (IllegalArgumentException e1) {
				throw new NCComplaintException(getf("cmd.users.status.problem.noSuchStatus", newStatusString));
			}
		} catch (IllegalArgumentException e) {
			throw new NCComplaintException(getf("cmd.users.status.problem.noSuchStatus", newStatusString));
		}
		
		if (newStatus == Status.NON_PLAYER) {
			throw new NCComplaintException(getf("cmd.users.status.problem.cannotSetNonPlayer"));
		}
		
		profile.setStatus(newStatus);
		sender.sendMessage(getf("cmd.users.status.success", profile.getName(), profile.getStatus()));
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
