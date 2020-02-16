/*
 * PIWCS Accountant Plugin
 * Copyright (C) 2019  Javapony/OLEGSHA
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

import java.nio.file.Path;
import java.time.LocalDate;

import ru.windcorp.piwcs.acc.db.Database;
import ru.windcorp.piwcs.acc.db.user.User;

/**
 * @author Javapony
 *
 */
public class SettlementDatabase extends Database<Settlement> {

	public SettlementDatabase(Path dir) {
		super(Settlement::load, dir, ".stlmnt.txt");
	}
	
	public Settlement add(
			String id, String name,
			LocalDate created, User mayor,
			String prefix
			) {
		Settlement settlement = Settlement.create(id, name, created, mayor, prefix);
		add(settlement);
		return settlement;
	}

}
