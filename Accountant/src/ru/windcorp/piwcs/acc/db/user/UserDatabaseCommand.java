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
package ru.windcorp.piwcs.acc.db.user;

import java.time.ZonedDateTime;
import java.util.Collection;

import ru.windcorp.jputil.cmd.AutoCommand;
import ru.windcorp.jputil.cmd.Invocation;
import ru.windcorp.piwcs.acc.Accountant;
import ru.windcorp.piwcs.acc.db.DatabaseCommand;

/**
 * @author Javapony
 *
 */
public class UserDatabaseCommand extends DatabaseCommand<User> {

	/**
	 * @param name
	 * @param desc
	 * @param commands
	 */
	public UserDatabaseCommand() {
		super(Accountant.getUsers(), new String[] {"users", "u"}, "User database", new String[] {"username"});
		
		add(AutoCommand.forMethod(UserDatabaseCommand.class, "add").desc("Adds new user").parser("<word USERNAME>", null));
		
		// TODO create user DB handling interface
	}
	
	public void add(Invocation inv, String username) {
		User user = Accountant.getUsers().create(username, ZonedDateTime.now());
		
		Collection<User> sel = getSelection(inv.getRunner());
		sel.clear();
		sel.add(user);
		
		inv.getRunner().respond("User %s added and selected", user);
	}

}
