/*
 * PIWCS Vrata Plugin
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

package ru.windcorp.piwcs.vrata.users;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.WeakHashMap;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import ru.windcorp.piwcs.vrata.VrataPlugin;

public class VrataUsers {
	
	private static final Map<String, VrataUserProfile> PROFILES = new HashMap<>();
	private static final WeakHashMap<CommandSender, VrataUser> USERS = new WeakHashMap<>();
	private static boolean loadedSuccessfully = false;
	
	private static final File DATABASE_FILE = new File(VrataPlugin.getInst().getDataFolder(), "database.db");
	
	public static void load() throws IOException {
		try (Scanner scanner = new Scanner(new InputStreamReader(new FileInputStream(DATABASE_FILE), StandardCharsets.UTF_8))) {
			while (scanner.hasNext()) {
				VrataUserProfile profile = VrataUserProfile.load(scanner);
				PROFILES.put(profile.getName(), profile);
			}
			
			IOException problem = scanner.ioException();
			if (problem != null) {
				throw problem;
			}
			
			loadedSuccessfully = true;
		} catch (NoSuchElementException e) {
			throw new IOException("User profile database is corrupted", e);
		}
	}
	
	public static synchronized void save() throws IOException {
		if (!loadedSuccessfully) {
			return;
		}
		
		try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(DATABASE_FILE), StandardCharsets.UTF_8))) {
			for (Map.Entry<String, VrataUserProfile> entry : PROFILES.entrySet()) {
				entry.getValue().save(writer);
			}
		}
	}
	
	public static synchronized VrataUserProfile getPlayerProfile(String name) {
		name = name.toLowerCase();
		
		VrataUserProfile profile = PROFILES.get(name);
		
		if (profile == null) {
			profile = new VrataUserProfile(name, VrataUserProfile.Status.USER);
			PROFILES.put(name, profile);
		}
		
		return profile;
	}
	
	public static synchronized VrataUser getUser(CommandSender sender) {
		VrataUser user = USERS.get(sender);
		
		if (user == null) {
			if (sender instanceof Player) {
				user = new VrataUser(sender, getPlayerProfile(sender.getName()));
			} else {
				user = new VrataUser(sender, new VrataUserProfile(sender.getName(), VrataUserProfile.Status.NON_PLAYER));
			}
			USERS.put(sender, user);
		}
		
		return user;
	}

}
