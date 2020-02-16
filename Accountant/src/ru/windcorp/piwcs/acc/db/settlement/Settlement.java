/*
 * PIWCS Accountant Plugin
 * Copyright (C) 2020  Javapony/OLEGSHA
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

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;

import ru.windcorp.piwcs.acc.Accountant;
import ru.windcorp.piwcs.acc.db.DatabaseEntry;
import ru.windcorp.piwcs.acc.db.Field;
import ru.windcorp.piwcs.acc.db.FieldManager;
import ru.windcorp.piwcs.acc.db.user.User;

/**
 * @author Javapony
 *
 */
public class Settlement extends DatabaseEntry {
	
	private final FieldManager manager = new FieldManager();
	
	private final Field<String> id =
			manager.newField(String.class).initial(null).name("id");
	private final Field<String> name =
			manager.newField(String.class).initial(null).name("name");
	private final Field<LocalDate> created =
			manager.newField(LocalDate.class).initial(null).name("created");
	
	private final Field<String> mayor =
			manager.newField(String.class).initial(null).name("mayor");
	private final Field<String> prefix =
			manager.newField(String.class).optionalNull().name("prefix");
	
	protected Settlement() {
		
	}

	@Override
	public String getDatabaseId() {
		return getId();
	}

	@Override
	public String getFileName() {
		return getId();
	}

	@Override
	public FieldManager getFieldManager() {
		return manager;
	}
	
	public static Settlement load(Path file) throws IOException {
		try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
			Settlement settlement = new Settlement();
			settlement.manager.load(reader, file.toString());
			return settlement;
		}
	}

	@Override
	public void save(Path file) throws IOException {
		try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
			this.manager.save(writer);
		}
	}
	
	public static Settlement create(String id, String name, LocalDate created, User mayor, String prefix) {
		Settlement settlement = new Settlement();
		
		settlement.id.set(id);
		settlement.name.set(name);
		settlement.setCreated(created);
		settlement.setMayor(mayor);
		settlement.setPrefix(prefix);
		
		return settlement;
	}
	
	public String getId() {
		return this.id.get();
	}
	
	public String getName() {
		return this.name.get();
	}
	
	public void setName(String name) {
		this.name.set(name);
	}
	
	public String getMayorName() {
		return this.mayor.get();
	}
	
	public User getMayor() {
		return Accountant.getUsers().get(getMayorName());
	}
	
	public void setMayor(User mayor) {
		this.mayor.set(mayor.getDatabaseId());
	}
	
	public String getPrefix() {
		String prefix = this.prefix.get();
		return prefix != null ? prefix : "";
	}
	
	public void setPrefix(String prefix) {
		this.prefix.set("".equals(prefix) ? null : prefix);
	}
	
	public LocalDate getCreated() {
		return this.created.get();
	}

	public void setCreated(LocalDate created) {
		this.created.set(created);
	}
	
	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return getName();
	}

}
