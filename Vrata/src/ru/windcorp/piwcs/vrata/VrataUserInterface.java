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

package ru.windcorp.piwcs.vrata;

import ru.windcorp.piwcs.vrata.crates.*;
import ru.windcorp.piwcs.vrata.crates.Package;
import ru.windcorp.piwcs.vrata.users.*;

import static ru.windcorp.piwcs.vrata.users.VrataUsers.*;
import static ru.windcorp.piwcs.vrata.VrataLogger.write;

import java.util.UUID;

public class VrataUserInterface {
	
	public static Package createNewPackage(String name) {
		Package pkg = new Package(UUID.randomUUID(), name);
		write("Created new package %s", pkg);
		return pkg;
	}
	
	public static void addPackageOwner(Package pkg, String owner) {
		pkg.addOwner(owner);
		write("Added owner %s to package %s", owner, pkg);
	}
	
	public static void selectPackage(Package pkg, VrataUser user) {
		user.setCurrentPackage(pkg);
		// TODO: log and check everything
		//user.
	}

}
