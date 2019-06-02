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
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import ru.windcorp.tge2.util.synch.SyncStreams;

public class Packages {
	
	private static File saveDir = null;
	
	private static final Map<UUID, Package> PACKAGES = Collections.synchronizedMap(new HashMap<>());
	
	public static void setSaveDirectory(File saveDir) {
		Packages.saveDir = saveDir;
	}
	
	public static File getSaveDirectory() {
		return saveDir;
	}

	public static Collection<Package> getPackages() {
		return PACKAGES.values();
	}
	
	public static Stream<Package> packages() {
		return SyncStreams.synchronizedStream(getPackages().stream(), getPackages());
	}
	
	public static Package getPackage(UUID uuid) {
		return PACKAGES.get(uuid);
	}
	
	static void registerPackage(Package pkg) {
		PACKAGES.put(pkg.getUuid(), pkg);
	}
	
	public static boolean removePackage(Package pkg) {
		pkg.setCurrentUser(null);// TODO delete files
		return !PACKAGES.remove(pkg.getUuid(), pkg);
	}
	
	public static void load() throws IOException {
		getSaveDirectory().mkdirs();
		
		for (File file : getSaveDirectory().listFiles((FileFilter)
				file -> file.isFile() && file.getName().endsWith(".package")
				)) {
			
			Package pkg = Package.load(new DataInputStream(new FileInputStream(file)));
			PACKAGES.put(pkg.getUuid(), pkg);
		}
	}

	public static void save() throws IOException {
		synchronized (PACKAGES) {
			for (Package pkg : PACKAGES.values()) {
				if (pkg.needsSaving()) {
					try (DataOutputStream output = new DataOutputStream(new FileOutputStream(pkg.getFile()))) {
						pkg.save(output);
					}
				}
				if (pkg.needsDescriptionRewrite()) {
					try (Writer jkRowling = new OutputStreamWriter(new FileOutputStream(pkg.getDescriptionFile()), StandardCharsets.UTF_8)) {
						pkg.saveDescriptions(jkRowling);
					}
				}
			}
		}
	}

}
