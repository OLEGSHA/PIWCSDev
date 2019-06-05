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

import ru.windcorp.piwcs.nestedcmd.*;
import ru.windcorp.piwcs.vrata.cmd.VrataListener;
import ru.windcorp.piwcs.vrata.crates.*;
import ru.windcorp.piwcs.vrata.crates.Package;
import ru.windcorp.piwcs.vrata.exceptions.*;
import ru.windcorp.piwcs.vrata.users.*;

import static ru.windcorp.piwcs.vrata.VrataLogger.write;
import static ru.windcorp.piwcs.vrata.VrataTemplates.*;

import java.util.UUID;

public class VrataUserInterface {
	
	public static Package createNewPackage(String name) {
		Package pkg = new Package(UUID.randomUUID(), Package.getLocalUniverseId(), name);
		write("Created new package %s", pkg);
		return pkg;
	}

	public static void removePackage(Package pkg) throws NCComplaintException, VrataPermissionException {
		if (!pkg.isEmpty()) {
			throw new NCComplaintException(getf("cmd.removePackage.problem.notEmpty", pkg));
		}
		
		selectPackage(pkg, null);
		Packages.removePackage(pkg);
	}
	
	public static void addPackageOwner(Package pkg, String owner) {
		pkg.addOwner(owner);
		write("Added owner %s to package %s", owner, pkg);
	}
	
	public static void selectPackage(Package pkg, VrataUser user) throws VrataPermissionException {
		if (pkg == null) {
			pkg = user.getCurrentPackage();
			user = null;
		}
		
		if (pkg.getCurrentUser() == user) {
			return;
		}
		
		if (user != null && !user.getProfile().isModerator()) {
			if (pkg.getCurrentUser() != null) {
				throw VrataPermissionException.create("package.alreadyInUse", new Object[] {pkg.getCurrentUser()}, pkg);
			}
			if (!pkg.isOwner(user)) {
				throw VrataPermissionException.createNotPackageOwner(user, pkg);
			}
		}
		
		pkg.setCurrentUser(user);
	}

	public static void onPackageSelectionChanged(VrataUser user, Package oldPkg, Package newPkg) {
		if (oldPkg == null) {
			write("%s selected package %s", user, newPkg);
		} else if (newPkg == null) {
			write("%s deselected package %s", user, oldPkg);
		} else {
			write("%s changed selected package from %s to %s", user, oldPkg, newPkg);
		}

		Packages.attemptSave();
	}
	
	public static void onPlayerQuitting(VrataUser user) {
		VrataListener.unregisterHandler(user.getPlayer());
		
		try {
			selectPackage(null, user);
		} catch (VrataPermissionException impossible) {}
	}

}
