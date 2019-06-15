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

import java.util.function.LongSupplier;

public class LongFieldBuilder {
	
	private final FieldManager manager;
	
	private LongSupplier def = null;
	private Long initial = null;
	private boolean isRequired = true;
	
	public LongFieldBuilder(FieldManager manager) {
		this.manager = manager;
	}
	
	public LongFieldBuilder initial(long value) {
		this.initial = value;
		return this;
	}
	
	public LongFieldBuilder def(LongSupplier supplier) {
		this.def = supplier;
		return this;
	}
	
	public LongFieldBuilder def(long value) {
		this.def = () -> value;
		return this;
	}
	
	public LongFieldBuilder optional() {
		this.isRequired = false;
		return this;
	}
	
	public LongFieldBuilder optional(LongSupplier defSupplier) {
		optional();
		def(defSupplier);
		return this;
	}
	
	public LongFieldBuilder optional(long defValue) {
		optional();
		def(defValue);
		return this;
	}
	
	public LongFieldBuilder required() {
		this.isRequired = true;
		return this;
	}

	public LongField name(String name) {
		if (initial == null) {
			if (def != null) initial = def.getAsLong();
			else throw new IllegalArgumentException("Field " + name + " has no initial value set and no default supplier set");
		}
		
		LongField field = new LongField(name, isRequired, initial, def);
		manager.addField(field);
		return field;
	}
	
}
