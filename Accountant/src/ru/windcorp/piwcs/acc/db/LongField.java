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
import java.util.function.LongSupplier;

public class LongField extends AbstractField {
	
	static final String TYPE = "long";
	
	private long value;
	private final LongSupplier def;

	protected LongField(
			String name,
			boolean isRequired,
			long value,
			LongSupplier def) {
		super(name, TYPE, isRequired);
		this.value = value;
		this.def = def;
	}
	
	public long get() {
		return value;
	}
	
	public void set(long value) {
		this.value = value;
	}
	
	public LongSupplier getDefault() {
		return def;
	}

	/**
	 * @see ru.windcorp.piwcs.acc.db.AbstractField#reset()
	 */
	@Override
	public void reset() {
		set(getDefault() == null ? 0 : getDefault().getAsLong());
	}

	/**
	 * @see ru.windcorp.piwcs.acc.db.AbstractField#load(java.lang.String)
	 */
	@Override
	public void load(String str) throws IOException {
		this.value = Long.parseLong(str);
	}

	/**
	 * @see ru.windcorp.piwcs.acc.db.AbstractField#save()
	 */
	@Override
	public String save() {
		return Long.toString(value);
	}

}
