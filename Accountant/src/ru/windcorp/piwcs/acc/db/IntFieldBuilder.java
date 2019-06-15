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

import java.util.function.IntSupplier;

public class IntFieldBuilder {
	
	private final FieldManager manager;
	
	private IntSupplier def = null;
	private Integer initial = null;
	private boolean isRequired = true;
	
	public IntFieldBuilder(FieldManager manager) {
		this.manager = manager;
	}
	
	public IntFieldBuilder initial(int value) {
		this.initial = value;
		return this;
	}
	
	public IntFieldBuilder def(IntSupplier supplier) {
		this.def = supplier;
		return this;
	}
	
	public IntFieldBuilder def(int value) {
		this.def = () -> value;
		return this;
	}
	
	public IntFieldBuilder optional() {
		this.isRequired = false;
		return this;
	}
	
	public IntFieldBuilder optional(IntSupplier defSupplier) {
		optional();
		def(defSupplier);
		return this;
	}
	
	public IntFieldBuilder optional(int defValue) {
		optional();
		def(defValue);
		return this;
	}
	
	public IntFieldBuilder required() {
		this.isRequired = true;
		return this;
	}

	public IntField name(String name) {
		if (initial == null) {
			if (def != null) initial = def.getAsInt();
			else throw new IllegalArgumentException("Field " + name + " has no initial value set and no default supplier set");
		}
		
		IntField field = new IntField(name, isRequired, initial, def);
		manager.addField(field);
		return field;
	}
	
}
