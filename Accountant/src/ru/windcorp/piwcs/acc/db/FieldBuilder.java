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

import java.util.function.Supplier;

import ru.windcorp.piwcs.acc.db.FieldManager.FieldLoader;
import ru.windcorp.piwcs.acc.db.FieldManager.FieldReader;
import ru.windcorp.piwcs.acc.db.FieldManager.FieldValue;
import ru.windcorp.piwcs.acc.db.FieldManager.FieldWriter;

public class FieldBuilder<T> {
	
	private final Class<T> clazz;
	private final FieldManager manager;
	
	private String type = null;
	private FieldLoader<T> loader = null;
	private FieldWriter<T> writer = null;
	
	private Supplier<T> def = null;
	private T initial = null;
	private boolean initialSet = false;
	
	private boolean isRequired = true;
	
	public FieldBuilder(Class<T> clazz, FieldManager manager) {
		this.clazz = clazz;
		this.manager = manager;
	}
	
	public FieldBuilder<T> io(FieldLoader<T> loader, FieldWriter<T> writer) {
		this.loader = loader;
		this.writer = writer;
		return this;
	}
	
	public FieldBuilder<T> ioReader(FieldReader<T> reader, FieldWriter<T> writer) {
		io(reader.toLoader(), writer);
		return this;
	}
	
	public FieldBuilder<T> mutable() {
		if (FieldValue.class.isAssignableFrom(clazz)) {
			throw new IllegalArgumentException();
		}
		
		io(FieldManager.selfLoader(), FieldManager.selfWriter());
		return this;
	}
	
	public FieldBuilder<T> immutable() {
		io(null, null);
		return this;
	}
	
	public FieldBuilder<T> type(String type) {
		this.type = type;
		return this;
	}
	
	public FieldBuilder<T> initial(T value) {
		this.initial = value;
		this.initialSet = true;
		return this;
	}
	
	public FieldBuilder<T> def(Supplier<T> supplier) {
		this.def = supplier;
		return this;
	}
	
	public FieldBuilder<T> def(T value) {
		this.def = () -> value;
		return this;
	}
	
	public FieldBuilder<T> optional() {
		this.isRequired = false;
		return this;
	}
	
	public FieldBuilder<T> optional(Supplier<T> defSupplier) {
		optional();
		def(defSupplier);
		return this;
	}
	
	public FieldBuilder<T> optional(T defValue) {
		optional();
		def(defValue);
		return this;
	}
	
	public FieldBuilder<T> required() {
		this.isRequired = true;
		return this;
	}

	public Field<T> name(String name) {
		if (type == null) type = FieldManager.getDefaultTypeName(clazz);
		
		if (loader == null || writer == null) {
			if (loader != null || writer != null) {
				throw new IllegalArgumentException("Field " + name + " has broken IO model: loader is "
						+ (loader == null ? "null" : "not null")
						+ ", but writer is "
						+ (writer == null ? "null" : "not null"));
			}
			
			loader = FieldManager.getDefaultLoader(type, clazz);
			writer = FieldManager.getDefaultWriter(type, clazz);
			
			if (loader == null || writer == null) {
				throw new IllegalArgumentException("Type " + type + " has no default IO model");
			}
		}
		
		if (!initialSet) {
			if (def != null) initial = def.get();
			else throw new IllegalArgumentException("Field " + name + " has no initial value set and no default supplier set");
		}
		
		Field<T> field = new Field<>(name, type, clazz, loader, writer, def, isRequired, initial);
		manager.addField(field);
		return field;
	}
	
}
