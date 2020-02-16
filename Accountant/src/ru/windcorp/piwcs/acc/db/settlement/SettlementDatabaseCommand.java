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
package ru.windcorp.piwcs.acc.db.settlement;

import java.time.LocalDate;
import java.util.Collection;

import ru.windcorp.jputil.cmd.AutoCommand;
import ru.windcorp.jputil.cmd.Invocation;
import ru.windcorp.piwcs.acc.Accountant;
import ru.windcorp.piwcs.acc.db.DatabaseCommand;
import ru.windcorp.piwcs.acc.db.user.User;

/**
 * @author Javapony
 *
 */
public class SettlementDatabaseCommand extends DatabaseCommand<Settlement> {

	/**
	 * @param name
	 * @param desc
	 * @param commands
	 */
	public SettlementDatabaseCommand() {
		super(Accountant.getSettlements(), new String[] {"settlements", "s"}, "Settlement database", new String[] {"id", "name"});
		
		add(AutoCommand.forMethod(this, "add")
				.desc("Adds a new settlement, optionally setting its PREFIX. Unless CREATED is specified, creation date is set to today")
				.parser("<word ID> <word NAME> <user MAYOR> [\"-p\" <word PREFIX>] [\"-c\" <date CREATED>]", null));
		
		add(AutoCommand.forMethod(this, "setMayor")
				.name("mayor")
				.desc("Sets a new MAYOR for the selected settlement")
				.parser("<user MAYOR>", null)
				.setRunnerFilter(mustHaveSingularSelection()));
		
		add(AutoCommand.forMethod(this, "setPrefix")
				.name("prefix")
				.desc("Sets a new PREFIX for the selected settlement")
				.parser("<word PREFIX>", null)
				.setRunnerFilter(mustHaveSingularSelection()));
	}
	
	public void add(Invocation inv,
			String id, String name, User mayor,
			boolean prefixPresent, String prefix,
			boolean createdPresent, LocalDate created) {
		
		if (!createdPresent) created = LocalDate.now();
		
		Settlement settlement = Accountant.getSettlements().add(id, name, created, mayor, prefix);
		
		Collection<Settlement> sel = getSelection(inv.getRunner());
		sel.clear();
		sel.add(settlement);
		
		inv.getRunner().respond("Settlement %s added and selected", settlement);
	}
	
	public void setMayor(Invocation inv, User mayor) {
		Settlement settlement = getSelected(inv.getRunner());
		settlement.setMayor(mayor);
		inv.getRunner().respond("The mayor of settlement %s is now %s", settlement, mayor);
	}
	
	public void setPrefix(Invocation inv, String prefix) {
		Settlement settlement = getSelected(inv.getRunner());
		settlement.setPrefix(prefix);
		inv.getRunner().respond("The prefix of settlement %s is now \"%s\"", settlement, prefix);
	}

}
