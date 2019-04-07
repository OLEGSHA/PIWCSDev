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

import java.io.IOException;
import java.io.Writer;
import java.util.NoSuchElementException;
import java.util.Scanner;

public class VrataUserProfile {
	
	private final String name;
	private boolean isAdmin;
	private boolean isModerator;
	
	public VrataUserProfile(String name) {
		this(name, false, false);
	}
	
	protected VrataUserProfile(String name, boolean isAdmin, boolean isModerator) {
		this.name = name;
		this.isAdmin = isAdmin;
		this.isModerator = isModerator;
	}
	
	public String getName() {
		return name;
	}
	
	public boolean isAdmin() {
		return isAdmin;
	}
	
	public void setAdmin(boolean isAdmin) {
		this.isAdmin = isAdmin;
	}
	
	public boolean isModerator() {
		return isModerator;
	}
	
	public void setModerator(boolean isModerator) {
		this.isModerator = isModerator;
	}
	
	public boolean canModerate() {
		return isAdmin() || isModerator();
	}
	
	public static VrataUserProfile load(Scanner input) throws NoSuchElementException {
		return new VrataUserProfile(input.next(), input.nextBoolean(), input.nextBoolean());
	}
	
	public void save(Writer output) throws IOException {
		output.write(getName());
		output.write(" ");
		output.write(Boolean.toString(isAdmin()));
		output.write(" ");
		output.write(Boolean.toString(isModerator()));
		output.write("\n");
	}

}
