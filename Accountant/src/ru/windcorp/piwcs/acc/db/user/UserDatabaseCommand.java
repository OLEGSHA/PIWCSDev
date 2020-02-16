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
import ru.windcorp.jputil.cmd.CommandRegistry;
import ru.windcorp.jputil.cmd.Invocation;
import ru.windcorp.jputil.textui.TUITable;
import ru.windcorp.piwcs.acc.Accountant;
import ru.windcorp.piwcs.acc.db.DatabaseCommand;
import ru.windcorp.piwcs.acc.db.settlement.Settlement;

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
		super(Accountant.getUsers(), new String[] {"users", "u"}, "User database", new String[] {"access-level", "username", "nickname", "settlement"});
		
		add(AutoCommand.forMethod(this, "add")
				.name("add")
				.desc("Adds a new user")
				.parser(
						"<word USERNAME> "
						+ "<<user INVITOR> | \"none\"> "
						+ "[\"-n\" <word NICKNAME>] "
						+ "[\"-r\" <word REAL_NAME>] "
						+ "[\"-s\" <settlement SETTLEMENT>] "
						+ "[\"-c\" <<word CONTACT_METHOD> <word CONTACT_DATA>>...]", null)
		);
		
		add(AutoCommand.forMethod(this, "setSettlement")
				.name("settlement")
				.desc("Sets a new settlement for the selected users")
				.parser("\"none\" | <settlement SETTLEMENT>", null)
				.setRunnerFilter(mustHaveSelection())
		);
		
		add(AutoCommand.forMethod(this, "setNickname")
				.name("nickname", "nick")
				.desc("Sets a new nickname for the selected user")
				.parser("\"none\" | <word NICKNAME>", null)
				.setRunnerFilter(mustHaveSingularSelection())
		);
		
		add(AutoCommand.forMethod(this, "setRealName")
				.name("realname")
				.desc("Sets a new real name for the selected user")
				.parser("\"none\" | <word REAL_NAME>", null)
				.setRunnerFilter(mustHaveSingularSelection())
		);
		
		add(new CommandRegistry("contacts", "Contacts of selected user",
				
				AutoCommand.forMethod(this, "addContact")
					.name("add")
					.desc("Adds contact data")
					.parser("<word METHOD> <word DATA>", null),
					
				AutoCommand.forMethod(this, "removeContact")
					.name("remove", "delete", "rem", "del")
					.desc("Removes contact data with provided METHOD")
					.parser("<word METHOD>", null),
					
				AutoCommand.forMethod(this, "listContacts")
					.name("list", "view")
					.desc("Lists all contact data")
					.parser("", null)
				
		).setRunnerFilter(mustHaveSingularSelection()));
		
		// TODO create user DB handling interface
	}
	
	public void add(Invocation inv,
			String username,
			boolean invitorProvided, User invitor, boolean noneProvided,
			boolean nicknameProvided, String nickname,
			boolean realNameProvided, String realName,
			boolean settlementProvided, Settlement settlement,
			boolean contactDataProvided, String[] contactMethods, String[] contactData
			) {
		
		User user = Accountant.getUsers().create(username, ZonedDateTime.now());
		
		Collection<User> sel = getSelection(inv.getRunner());
		sel.clear();
		sel.add(user);
		
		if (invitorProvided) user.setInvitor(invitor);
		if (nicknameProvided) user.setNickname(nickname);
		if (realNameProvided) user.setRealName(realName);
		if (settlementProvided) user.setSettlement(settlement);
		
		if (contactDataProvided) {
			for (int i = 0; i < contactMethods.length; ++i) {
				user.getContacts().put(contactMethods[i], contactData[i]);
			}
		}
		
		inv.getRunner().respond("User %s added and selected", user);
	}
	
	public void setSettlement(Invocation inv,
			boolean noneProvided,
			boolean settlementProvided, Settlement settlement) {
		
		for (User user : getSelection(inv.getRunner())) {
			user.setSettlement(settlement);
		}
		
		inv.getRunner().respond("Changed settlement to %s for selected users", settlement);
	}
	
	public void setNickname(Invocation inv,
			boolean noneProvided,
			boolean nicknameProvided, String nickname) {
		
		getSelected(inv.getRunner()).setNickname(nickname);
		
		if (nicknameProvided) {
			inv.getRunner().respond("Changed nickname to \"%s\" for selected user", nickname);
		} else {
			inv.getRunner().respond("Cleared nickname for selected user");
		}
	}
	
	public void setRealName(Invocation inv,
			boolean noneProvided,
			boolean realNameProvided, String realName) {
		
		getSelected(inv.getRunner()).setRealName(realName);
		
		if (realNameProvided) {
			inv.getRunner().respond("Changed real name to \"%s\" for selected user", realName);
		} else {
			inv.getRunner().respond("Cleared real name for selected user");
		}
	}
	
	public void addContact(Invocation inv, String method, String data) {
		getSelected(inv.getRunner()).getContacts().put(method, data);
		inv.getRunner().respond("Added contact record \"%s: %s\" for selected user", method, data);
	}
	
	public void removeContact(Invocation inv, String method) {
		String data = getSelected(inv.getRunner()).getContacts().remove(method);
		
		if (data != null) {
			inv.getRunner().respond("Removed contact record \"%s: %s\" from selected user", method, data);
		} else {
			inv.getRunner().complain("Selected user has no contact record with method \"%s\"", method);
		}
		
	}
	
	public void listContacts(Invocation inv) {
		User selected = getSelected(inv.getRunner());
		ContactRecordSet contacts = selected.getContacts();
		
		inv.getRunner().respond("User %s has %d contact records:", selected, contacts.size());
		
		TUITable table = new TUITable("Method", "Data");
		contacts.forEach(table::addRow);
		inv.getRunner().respond(table);
	}

}
