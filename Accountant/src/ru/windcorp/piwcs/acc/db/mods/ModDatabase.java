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
package ru.windcorp.piwcs.acc.db.mods;

import java.net.URI;
import java.nio.file.Path;
import java.time.LocalDate;

import ru.windcorp.piwcs.acc.db.Database;

/**
 * @author Javapony
 *
 */
public class ModDatabase extends Database<Modification> {

	public ModDatabase(Path dir) {
		super(Modification::load, dir, ".mod");
	}
	
	public Modification add(
			String id, String name,
			LocalDate today, String suggester,
			URI uri
			) {
		Modification mod = Modification.create(id, name, today, suggester, uri);
		add(mod);
		return mod;
	}

}
