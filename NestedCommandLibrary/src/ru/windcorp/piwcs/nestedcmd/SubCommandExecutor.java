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

@FunctionalInterface
public interface SubCommandExecutor {
	
	void run(
			CommandSender sender,
			List<String> args,
			String fullCommand
			)
					
		throws
			NCPermissionException,
			NCExecutionException,
			NCSyntaxException,
			NCComplaintException;
	
	default SubCommandExecutor then(final SubCommandExecutor other) {
		return (sender, args, fullCommand) -> {
			SubCommandExecutor.this.run(sender, args, fullCommand);
			other.run(sender, args, fullCommand);
		};
	}

}
