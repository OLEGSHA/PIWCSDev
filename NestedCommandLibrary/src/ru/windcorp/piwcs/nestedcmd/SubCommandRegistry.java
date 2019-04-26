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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.command.CommandSender;

public class SubCommandRegistry extends SubCommand {
	
	private final Map<String, SubCommand> subCommands = new HashMap<>();
	
	private final String helpHeader;
	private final String helpFormat;

	public SubCommandRegistry(String[] names, String description, CommandSenderFilter filter, String helpFormat, String helpHeader, SubCommand... commands) {
		super(names, description, "<subcommand> [args...]", null, filter);
		
		this.helpHeader = helpHeader;
		this.helpFormat = helpFormat;
		
		for (SubCommand command : commands) {
			for (String name : command.getNames()) {
				getSubCommands().put(name, command);
			}
		}
	}
	
	public SubCommandRegistry(String name, String description, CommandSenderFilter filter, String helpFormat, String helpHeader, SubCommand... commands) {
		this(new String[] {name}, description, filter, helpFormat, helpHeader, commands);
	}
	
	public Map<String, SubCommand> getSubCommands() {
		return subCommands;
	}

	@Override
	protected void execute(CommandSender sender, List<String> args, String fullCommand)
			throws NCPermissionException, NCExecutionException, NCSyntaxException {
		if (args.isEmpty()) {
			showHelp(sender);
			return;
		}
		
		String name = args.get(0);
		
		if (name.equalsIgnoreCase("help") || name.equalsIgnoreCase("?")) {
			showHelp(sender);
			return;
		}

		SubCommand command = getSubCommands().get(name);
		if (command == null) {
			showHelp(sender);
			return;
		}
		
		command.run(sender, args.subList(1, args.size()), fullCommand + " " + name);
	}

	public void showHelp(CommandSender sender) {
		sender.sendMessage(String.format(helpHeader, getName()));
		for (SubCommand command : getSubCommands().values()) {
			if (command.getFilter().getException(sender) == null) {
				sender.sendMessage(String.format(helpFormat, command.getName(), command.getSyntax(), command.getDescription()));
			}
		}
	}

}
