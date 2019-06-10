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
import java.util.Scanner;

public class VrataUserProfile {
	
	public static enum Status {
		USER,
		MODERATOR,
		ADMIN,
		NON_PLAYER
	}
	
	private final String name;
	private Status status;
	
	protected VrataUserProfile(String name, Status status) {
		System.out.println("Created new profile obj");
		this.name = name;
		this.status = status;
	}
	
	public String getName() {
		return name;
	}
	
	public Status getStatus() {
		return status;
	}
	
	public void setStatus(Status status) {
		this.status = status;
	}
	
	public void promote(Status newStatus) {
		if (!isAtLeast(newStatus)) {
			setStatus(newStatus);
		}
	}
	
	public void demote(Status newStatus) {
		if (isAtLeast(newStatus)) {
			setStatus(newStatus);
		}
	}
	
	public boolean isAtLeast(Status status) {
		return status.compareTo(getStatus()) <= 0;
	}
	
	public boolean isAdmin() {
		return isAtLeast(Status.ADMIN);
	}
	
	public boolean isModerator() {
		return isAtLeast(Status.MODERATOR);
	}
	
	public static VrataUserProfile load(Scanner input) throws IOException {
		String name = null;
		try {
			name = input.next();
			Status status;
			try {
				status = Status.valueOf(input.next());
			} catch (IllegalArgumentException e) {
				status = Status.USER;
			}
			return new VrataUserProfile(name, status);
		} catch (Exception e) {
			throw new IOException("Could not read profile with name " + name, e);
		}
	}
	
	public void save(Writer output) throws IOException {
		output.write(getName());
		output.write(" ");
		output.write(getStatus().name());
		output.write("\n");
	}
	
	@Override
	public String toString() {
		return "[" + getStatus().toString() + "] " + getName();
	}

}
