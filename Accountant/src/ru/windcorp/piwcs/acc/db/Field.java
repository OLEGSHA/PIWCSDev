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
import java.util.Objects;
import java.util.function.Supplier;

import ru.windcorp.piwcs.acc.db.FieldManager.FieldLoader;
import ru.windcorp.piwcs.acc.db.FieldManager.FieldWriter;

public class Field<T> extends AbstractField {
	
	private final Class<T> clazz;
	
	private final FieldLoader<T> loader;
	private final FieldWriter<T> writer;
	
	private final Supplier<T> def;
	private T value;
	
	protected Field(
			String name,
			String type, Class<T> clazz,
			FieldLoader<T> loader, FieldWriter<T> writer,
			Supplier<T> def, boolean isRequired,
			T value) {
		super(name, type, isRequired);
		this.clazz = Objects.requireNonNull(clazz, "clazz");
		this.loader = Objects.requireNonNull(loader, "loader");
		this.writer = Objects.requireNonNull(writer, "writer");
		this.def = def;
		this.value = value;
	}
	
	@Override
	public String save() {
		return getWriter().writeNullAware(get());
	}
	
	@Override
	public void load(String str) throws IOException {
		set(getLoader().loadNullAware(str, get()));
	}
	
	public T get() {
		return value;
	}
	
	public void set(T value) {
		this.value = value;
	}
	
	public Supplier<T> getDefault() {
		return def;
	}
	
	@Override
	public void reset() {
		set(getDefault() == null ? null : getDefault().get());
	}
	
	/**
	 * @return the type class
	 */
	public Class<T> getTypeClass() {
		return clazz;
	}
	
	/**
	 * @return the loader
	 */
	public FieldLoader<T> getLoader() {
		return loader;
	}
	
	/**
	 * @return the writer
	 */
	public FieldWriter<T> getWriter() {
		return writer;
	}
	
}
