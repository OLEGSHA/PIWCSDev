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
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class Packages {
	
	private static File saveDir = null;
	
	private static final Set<Package> PACKAGES = new HashSet<>();
	
	public static void setSaveDirectory(File saveDir) {
		Packages.saveDir = saveDir;
	}
	
	public static File getSaveDirectory() {
		return saveDir;
	}

	public static Set<Package> getPackages() {
		return PACKAGES;
	}
	
	public static void load() throws IOException {
		for (File file : getSaveDirectory().listFiles((FileFilter)
				file -> file.isFile() && file.getName().endsWith(".package")
				)) {
			PACKAGES.add(Package.load(new DataInputStream(new FileInputStream(file))));
		}
	}

	public static void save() throws IOException {
		// TODO Auto-generated method stub
		
	}

}
