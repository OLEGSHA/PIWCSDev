/*
 * Bukkit Nested Command Library
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

package ru.windcorp.piwcs.nestedcmd;

import java.util.List;

import org.bukkit.command.CommandSender;

public class SubCommand {
	
	private final String[] names;
	private final String description;
	private final String syntax;
	
	private final SubCommandExecutor executor;
	private final CommandSenderFilter filter;
	
	public SubCommand(
			String[] names,
			String description, String syntax,
			SubCommandExecutor executor,
			CommandSenderFilter filter) {
		this.names = names;
		this.description = description;
		this.syntax = syntax;
		this.executor = executor;
		this.filter = filter;
	}
	
	public SubCommand(
			String name,
			String description, String syntax,
			SubCommandExecutor executor,
			CommandSenderFilter filter) {
		this(
				new String[] {name},
				description, syntax,
				executor, filter
				);
	}
	
	public String getName() {
		return getNames()[0];
	}

	public String[] getNames() {
		return names;
	}

	public String getDescription() {
		return description;
	}

	public String getSyntax() {
		return syntax;
	}

	public SubCommandExecutor getExecutor() {
		return executor;
	}

	public CommandSenderFilter getFilter() {
		return filter;
	}

	public void run(CommandSender sender, List<String> args, String fullCommand) {
		try {
			if (getFilter() != null) {
				getFilter().checkPermission(sender);
			}
			
			execute(sender, args, fullCommand);
		} catch (NCComplaintException e) {
			sender.sendMessage(e.getLocalizedMessage());
			return;
		} catch (NCPermissionException e) {
			e.report(sender);
			return;
		} catch (NCExecutionException e) {
			sender.sendMessage(e.getLocalizedMessage());
			e.printStackTrace();
			return;
		} catch (NCSyntaxException e) {
			sender.sendMessage(e.getLocalizedMessage());
			sender.sendMessage(getSyntax());
		}
	}
	
	protected void execute(CommandSender sender, List<String> args, String fullCommand) throws NCPermissionException, NCExecutionException, NCSyntaxException, NCComplaintException {
		getExecutor().run(sender, args, fullCommand);
	}

}
