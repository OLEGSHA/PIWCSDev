/**
 * Copyright (C) 2019 OLEGSHA
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
package ru.windcorp.piwcs.acc;

import ru.windcorp.jputil.Nameable;
import ru.windcorp.jputil.cmd.CommandRunner;

public abstract class Agent extends Nameable implements CommandRunner {

	public static enum AccessLevel {
		PLAYER,
		MODERATOR,
		ADMIN
	}
	
	private final AccessLevel accessLevel;
	
	public Agent(String name, AccessLevel accessLevel) {
		super(name);
		this.accessLevel = accessLevel;
	}

	public AccessLevel getAccessLevel() {
		return accessLevel;
	}
	
	public boolean hasAccessLevel(AccessLevel lvl) {
		return getAccessLevel().compareTo(lvl) > 0;
	}
	
	public void runCommand(String rawInput) {
		Accountant.getCommandSystem().runCommand(this, rawInput);
	}

}
