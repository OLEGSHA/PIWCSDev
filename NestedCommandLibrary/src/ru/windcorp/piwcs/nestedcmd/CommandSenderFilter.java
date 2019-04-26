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

import java.util.function.Function;

import org.bukkit.command.CommandSender;

@FunctionalInterface
public interface CommandSenderFilter {
	
	NCPermissionException getException(CommandSender sender);
	
	default void checkPermission(CommandSender sender) throws NCPermissionException {
		NCPermissionException e = getException(sender);
		if (e != null) {
			throw e;
		}
	}
	
	public default CommandSenderFilter and(final CommandSenderFilter other) {
		return new CommandSenderFilter() {
			@Override
			public NCPermissionException getException(CommandSender sender) {
				NCPermissionException e = CommandSenderFilter.this.getException(sender);
				if (e == null) {
					e = other.getException(sender);
				}
				return e;
			}
		};
	}
	
	public default CommandSenderFilter or(final CommandSenderFilter other) {
		return new CommandSenderFilter() {
			@Override
			public NCPermissionException getException(CommandSender sender) {
				NCPermissionException e = CommandSenderFilter.this.getException(sender);
				if (e != null) {
					e = other.getException(sender);
				}
				return e;
			}
		};
	}
	
	public default CommandSenderFilter invert(Function<CommandSender, NCPermissionException> exceptionProvider) {
		return new CommandSenderFilter() {
			@Override
			public NCPermissionException getException(CommandSender sender) {
				if (CommandSenderFilter.this.getException(sender) == null) {
					return exceptionProvider.apply(sender);
				}
				return null;
			}
		};
	}

}
