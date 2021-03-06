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
import java.util.function.IntSupplier;

public class IntField extends AbstractField {
	
	static final String TYPE = "int";
	
	private int value;
	private final IntSupplier def;

	protected IntField(
			String name,
			boolean isRequired,
			int value,
			IntSupplier def) {
		super(name, TYPE, isRequired);
		this.value = value;
		this.def = def;
	}
	
	public int get() {
		return value;
	}
	
	public void set(int value) {
		this.value = value;
	}
	
	public IntSupplier getDefault() {
		return def;
	}

	/**
	 * @see ru.windcorp.piwcs.acc.db.AbstractField#reset()
	 */
	@Override
	public void reset() {
		set(getDefault() == null ? 0 : getDefault().getAsInt());
	}

	/**
	 * @see ru.windcorp.piwcs.acc.db.AbstractField#load(java.lang.String)
	 */
	@Override
	public void load(String str) throws IOException {
		this.value = Integer.parseInt(str);
	}

	/**
	 * @see ru.windcorp.piwcs.acc.db.AbstractField#save()
	 */
	@Override
	public String save() {
		return Integer.toString(value);
	}

}
