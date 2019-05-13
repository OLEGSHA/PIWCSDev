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

package ru.windcorp.piwcs.vrata.crates;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Packages {
	
	private static File saveDir = null;
	
	private static final Map<UUID, Package> PACKAGES = new HashMap<>();
	
	public static void setSaveDirectory(File saveDir) {
		Packages.saveDir = saveDir;
	}
	
	public static File getSaveDirectory() {
		return saveDir;
	}

	public static Collection<Package> getPackages() {
		return PACKAGES.values();
	}
	
	public static Package getPackage(UUID uuid) {
		return PACKAGES.get(uuid);
	}
	
	static void registerPackage(Package pkg) {
		PACKAGES.put(pkg.getUuid(), pkg);
	}
	
	public static boolean removePackage(Package pkg) {
		pkg.setCurrentUser(null);
		return !PACKAGES.remove(pkg.getUuid(), pkg);
	}
	
	public static void load() throws IOException {
		for (File file : getSaveDirectory().listFiles((FileFilter)
				file -> file.isFile() && file.getName().endsWith(".package")
				)) {
			
			Package pkg = Package.load(new DataInputStream(new FileInputStream(file)));
			PACKAGES.put(pkg.getUuid(), pkg);
		}
	}

	public static void save() throws IOException {
		for (Package pkg : PACKAGES.values()) {
			if (pkg.needsSaving()) {
				pkg.save(new DataOutputStream(new FileOutputStream(pkg.getFile())));
			}
			if (pkg.needsDescriptionRewrite()) {
				pkg.saveDescriptions(new OutputStreamWriter(new FileOutputStream(pkg.getFile()), StandardCharsets.UTF_8));
			}
		}
	}

}
