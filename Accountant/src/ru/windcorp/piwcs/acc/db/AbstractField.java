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
package ru.windcorp.piwcs.acc.db;

import java.io.IOException;

public abstract class AbstractField {

	protected final String name;

	protected final String type;
	protected final boolean isRequired;
	private boolean loadFlag = false;

	protected AbstractField(String name, String type, boolean isRequired) {
		this.name = name;
		this.type = type;
		this.isRequired = isRequired;
	}

	public abstract void reset();

	public abstract void load(String str) throws IOException;

	public abstract String save();

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return the type
	 */
	public String getType() {
		return type;
	}

	/**
	 * @return {@code true} if this Field must be set explicitly
	 */
	public boolean isRequired() {
		return isRequired;
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return getType() + " " + getName();
	}

	protected void setLoadFlag(boolean flag) {
		loadFlag = flag;
	}

	protected boolean getLoadFlag() {
		return loadFlag;
	}

	protected void onAllFieldsLoaded() throws IOException {
		if (loadFlag) {
			loadFlag = false;
		} else if (isRequired()) {
			throw new IOException("Required field " + this + " is missing");
		} else {
			reset();
		}
	}

}