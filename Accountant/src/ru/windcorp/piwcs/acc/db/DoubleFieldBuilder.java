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

import java.util.function.DoubleSupplier;

public class DoubleFieldBuilder {
	
	private final FieldManager manager;
	
	private DoubleSupplier def = null;
	private Double initial = null;
	private boolean isRequired = true;
	
	public DoubleFieldBuilder(FieldManager manager) {
		this.manager = manager;
	}
	
	public DoubleFieldBuilder initial(double value) {
		this.initial = value;
		return this;
	}
	
	public DoubleFieldBuilder def(DoubleSupplier supplier) {
		this.def = supplier;
		return this;
	}
	
	public DoubleFieldBuilder def(double value) {
		this.def = () -> value;
		return this;
	}
	
	public DoubleFieldBuilder optional() {
		this.isRequired = false;
		return this;
	}
	
	public DoubleFieldBuilder optional(DoubleSupplier defSupplier) {
		optional();
		def(defSupplier);
		return this;
	}
	
	public DoubleFieldBuilder optional(double defValue) {
		optional();
		def(defValue);
		return this;
	}
	
	public DoubleFieldBuilder required() {
		this.isRequired = true;
		return this;
	}

	public DoubleField name(String name) {
		if (initial == null) {
			if (def != null) initial = def.getAsDouble();
			else throw new IllegalArgumentException("Field " + name + " has no initial value set and no default supplier set");
		}
		
		DoubleField field = new DoubleField(name, isRequired, initial, def);
		manager.addField(field);
		return field;
	}
	
}
