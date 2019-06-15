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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class UserDatabase {
	
	private static final Map<String, User> USERS = new HashMap<>();
	private static Path directory;
	
	public synchronized static User getUser(String username) {
		return USERS.get(username);
	}
	
	static synchronized void addUser(User user) {
		User previous = USERS.putIfAbsent(user.getUsername(), user);
		if (previous != null)
			throw new IllegalArgumentException("Duplicate username: " + user + ", " + previous);
	}
	
	static synchronized void removeUser(User user) {
		USERS.remove(user.getUsername(), user);
	}
	
	public static synchronized int load(Path directory) throws IOException {
		USERS.clear();
		for (Iterator<Path> it = Files.list(directory).filter(Files::isRegularFile).iterator(); it.hasNext();) {
			Path path = it.next();
			if (!path.toString().endsWith(".user")) continue;
			User.load(path);
		}
		UserDatabase.directory = directory;
		return USERS.size();
	}
	
	public static synchronized void save() throws IOException {
		for (User user : USERS.values()) {
			user.save(user.getFile(directory));
		}
	}
	
}
