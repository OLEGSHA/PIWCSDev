/*
 * PIWCS Accountant Plugin
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
package ru.windcorp.piwcs.acc.db.mods;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import ru.windcorp.jputil.Version;
import ru.windcorp.jputil.chars.IndentedStringBuilder;
import ru.windcorp.jputil.cmd.AutoCommand;
import ru.windcorp.jputil.cmd.CommandExceptions;
import ru.windcorp.jputil.cmd.CommandRegistry;
import ru.windcorp.jputil.cmd.CommandSyntaxException;
import ru.windcorp.jputil.cmd.Complaint;
import ru.windcorp.jputil.cmd.Invocation;
import ru.windcorp.piwcs.acc.Accountant;
import ru.windcorp.piwcs.acc.db.DatabaseCommand;
import ru.windcorp.piwcs.acc.db.mods.Modification.State;

/**
 * @author Javapony
 *
 */
public class ModDatabaseCommand extends DatabaseCommand<Modification> {

	/**
	 * @param name
	 * @param desc
	 * @param commands
	 */
	public ModDatabaseCommand() {
		super(Accountant.getMods(), new String[] {"mods", "m"}, "Modification database",
				new String[] {"id", "name"});
				
		add(AutoCommand.forMethod(this, "add").desc("Adds new modification").parser(
						"<word ID> <word NAME> <word SUGGESTER> <word URI>", null));
				
		add(new CommandRegistry("state", "Changes state of selected modifications",
				
				AutoCommand.forMethod(this, "setStateSuggested")
					.name("suggested", "suggest")
					.desc("Sets SUGGESTED state").parser("[comment...]", null),
					
				AutoCommand.forMethod(this, "setStateAccepted")
					.name("accepted", "accept")
					.desc("Sets ACCEPTED state").parser("[comment...]", null),
					
				AutoCommand.forMethod(this, "setStateRejected")
					.name("rejected", "reject")
					.desc("Sets REJECTED state").parser("[comment...]", null),
					
				AutoCommand.forMethod(this, "setStateDeleting")
					.name("deleting", "delete", "rejecting")
					.desc("Sets DELETING state").parser("<comment...>", null),
					
				AutoCommand.forMethod(this, "setStateInstalled")
					.name("installed", "install")
					.desc("Sets INSTALLED state").parser("<version VERSION> [\"lockVersion\"] [comment...]", null)
				
				).setRunnerFilter(mustHaveSelection()));
		
		add(new CommandRegistry(new String[] {"dependencies", "deps", "dependency", "dep"}, "Manages dependencies of selected modifications",
				
				AutoCommand.forMethod(this, "depAdd")
					.name("add")
					.desc("Adds DEPENDENCIES").parser("<mod DEPENDENCIES...>", null),
					
				AutoCommand.forMethod(this, "depRemove")
					.name("remove", "rem", "delete", "del")
					.desc("Removes any of DEPENDENCIES").parser("<mod DEPENDENCIES...>", null),
					
				AutoCommand.forMethod(this, "depTree")
					.name("tree", "dependencies", "list")
					.desc("Prints dependancy trees").parser("", null),
					
				AutoCommand.forMethod(this, "dependants")
					.name("dependants", "deps")
					.desc("Prints all modifications that depend on selected modifications").parser("", null)
				
				).setRunnerFilter(mustHaveSelection()));
	}
	
	public void add(Invocation inv, String id, String name, String suggester, String uri) throws CommandExceptions {
		Modification mod;
		try {
			mod = Accountant.getMods().add(id, name, LocalDate.now(), suggester, new URI(uri));
		} catch (URISyntaxException e) {
			throw new CommandSyntaxException(inv, "\"" + uri + "\" is not a valid URI", e);
		}
		
		Collection<Modification> sel = getSelection(inv.getRunner());
		sel.clear();
		sel.add(mod);
		
		inv.getRunner().respond("Modification %s added and selected", mod);
	}
	
	public void setStateSuggested(Invocation inv, boolean commentPresent, String comment) throws Complaint {
		commonSetState(State.SUGGESTED, inv, commentPresent, comment);
	}
	
	public void setStateAccepted(Invocation inv, boolean commentPresent, String comment) throws Complaint {
		commonSetState(State.SUGGESTED, inv, commentPresent, comment);
	}
	
	public void setStateRejected(Invocation inv, boolean commentPresent, String comment) throws Complaint {
		commonSetState(State.REJECTED, inv, commentPresent, comment);
	}
	
	public void setStateDeleting(Invocation inv, String comment) throws CommandExceptions {
		if (comment.isEmpty()) throw new CommandSyntaxException(inv, "Comment is required");
		commonSetState(State.DELETING, inv, true, comment);
	}
	
	public void setStateInstalled(
			Invocation inv,
			Version version,
			boolean lockVersion,
			boolean commentPresent,
			String comment) throws Complaint {

		commentPresent &= !comment.isEmpty();
		final State state = State.INSTALLED;
		LocalDate today = null;
		
		for (Modification mod : getSelectionAndCheckStateChange(inv, state)) {
			State prev = mod.getState();
			mod.setState(state);
			
			mod.setInstalledVersion(version);
			mod.setNewestVersion(version);
			
			if (lockVersion) {
				mod.setLastUpdateCheckDate(null);
			} else {
				if (today == null) today = LocalDate.now();
				mod.setLastUpdateCheckDate(today);
			}
			
			if (commentPresent) mod.setComment(comment);
			
			inv.getRunner().respond("Changed state of %s to %s from %s", mod, state, prev);
		}
	}
	
	private Collection<Modification> getSelectionAndCheckStateChange(Invocation inv, State target) throws Complaint {
		Collection<Modification> sel = getSelection(inv.getRunner());
		for (Modification mod : sel) {
			if (!State.canChangeTo(mod.getState(), target)) {
				throw new Complaint(inv, "Cannot change modification " + mod + " from state " + mod.getState() + " to " + target);
			}
		}
		return sel;
	}
	
	private void commonSetState(State state, Invocation inv, boolean commentPresent, String comment) throws Complaint {
		commentPresent &= !comment.isEmpty();
		for (Modification mod : getSelectionAndCheckStateChange(inv, state)) {
			State prev = mod.getState();
			mod.setState(state);
			if (commentPresent) mod.setComment(comment);
			inv.getRunner().respond("Changed state of %s to %s from %s", mod, state, prev);
		}
	}
	
	public void depAdd(Invocation inv, Modification[] deps) {
		Collection<Modification> dependencies = Arrays.asList(deps);
		for (Modification mod : getSelection(inv.getRunner())) {
			mod.getDependencies().addAll(dependencies);
			inv.getRunner().respond("Added %s dependencies to modification %s", deps.length, mod);
		}
	}
	
	public void depRemove(Invocation inv, Modification[] deps) {
		for (Modification mod : getSelection(inv.getRunner())) {
			int removed = 0;
			for (Modification dep : deps)
				if (mod.getDependencies().remove(dep)) removed++;
			inv.getRunner().respond("Removed %s dependencies from modification %s", removed, mod);
		}
	}
	
	public void depTree(Invocation inv) {
		for (Modification mod : getSelection(inv.getRunner())) {
			IndentedStringBuilder sb = new IndentedStringBuilder("- ");
			if (!appendDependenciesRecursive(mod, sb)) {
				inv.getRunner().respond("Modification %s has no dependencies", mod);
			} else {
				inv.getRunner().respond("Dependencies of modification %s:", mod);
				inv.getRunner().respond(sb);
			}
		}
	}
	
	public void dependants(Invocation inv) {
		Map<Modification, Collection<Modification>> dependantMap = new HashMap<>();
		for (Modification mod : getSelection(inv.getRunner())) {
			dependantMap.put(mod, new ArrayList<>());
		}
		
		for (Modification mod : Accountant.getMods().entries()) {
			for (Modification dep : mod.getDependencies()) {
				Collection<Modification> dependants = dependantMap.get(dep);
				if (dependants != null) dependants.add(mod);
			}
		}
		
		for (Entry<Modification, Collection<Modification>> entry : dependantMap.entrySet()) {
			inv.getRunner().respond("Modification %s: %s", entry.getKey(), entry.getValue());
		}
	}
	
	private static boolean appendDependenciesRecursive(Modification mod, IndentedStringBuilder sb) {
		Collection<Modification> deps = mod.getDependencies();
		
		if (deps.isEmpty()) {
			sb.newLine();
			return false;
		}
		
		sb.indent();
		for (Modification dep : mod.getDependencies()) {
			sb.append(dep.toString());
			appendDependenciesRecursive(dep, sb);
		}
		sb.unindent().newLine();
		
		return true;
	}

}
