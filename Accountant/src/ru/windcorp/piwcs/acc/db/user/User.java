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
package ru.windcorp.piwcs.acc.db.user;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZonedDateTime;

import ru.windcorp.piwcs.acc.Agent;
import ru.windcorp.piwcs.acc.db.Field;
import ru.windcorp.piwcs.acc.db.FieldManager;

public class User implements Agent {
	
	private final FieldManager manager = new FieldManager();

	private final Field<String> username =
			manager.newField(String.class).initial(null).name("username");
	
	private final Field<String> realName =
			manager.newField(String.class).optional().name("real-name");
	
	private final Field<ZonedDateTime> registered =
			manager.newField(ZonedDateTime.class).initial(null).name("registered");
	private final Field<ZonedDateTime> joined =
			manager.newField(ZonedDateTime.class).optional().name("joined");
	private final Field<ZonedDateTime> lastSeen =
			manager.newField(ZonedDateTime.class).optional().name("last-seen");
	
	private final Field<AccessLevel> accessLevel =
			manager.newField(AccessLevel.class)
			.optional(AccessLevel.PLAYER)
			.ioReader(Agent.AccessLevel::valueOf, Agent.AccessLevel::name)
			.name("access-level");
	
	protected User() {
		
	}
	
	public Path getFile(Path directory) {
		return directory.resolve(getUsername() + ".user");
	}
	
	public static User load(Path file) throws IOException {
		try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
			User user = new User();
			user.manager.load(reader);
			UserDatabase.addUser(user);
			return user;
		}
	}
	
	public void save(Path file) throws IOException {
		try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
			this.manager.save(writer);
		}
	}
	
	public static User create(String username, ZonedDateTime registerDate) {
		User user = new User();
		user.username.set(username);
		user.registered.set(registerDate);
		UserDatabase.addUser(user);
		return user;
	}
	
	public String getUsername() {
		return this.username.get();
	}
	
	public String getRealName() {
		return this.realName.get();
	}
	
	public ZonedDateTime getRegisterDate() {
		return this.registered.get();
	}
	
	public ZonedDateTime getJoinDate() {
		return this.joined.get();
	}
	
	public void setJoinDate(ZonedDateTime joinDate) {
		this.joined.set(joinDate);
	}
	
	public ZonedDateTime getLastSeenDate() {
		return this.lastSeen.get();
	}
	
	public void setLastSeenDate(ZonedDateTime lastSeenDate) {
		this.lastSeen.set(lastSeenDate);
	}
	
	/**
	 * @see ru.windcorp.piwcs.acc.Agent#getAccessLevel()
	 */
	@Override
	public AccessLevel getAccessLevel() {
		return this.accessLevel.get();
	}
	
	public void setAccessLevel(AccessLevel lvl) {
		this.accessLevel.set(lvl);
	}
	
	/**
	 * @see ru.windcorp.piwcs.acc.Agent#sendMessage(java.lang.String)
	 */
	@Override
	public void sendMessage(String msg) {
		// TODO Auto-generated method stub
		// fail silently if offline
	}
	
	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return getAccessLevel() + " " + getUsername();
	}
	
}
