/*
 * PIWCS Accountant Plugin
 * Copyright (C) 2019  Javapony/OLEGSHA
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
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * @author Javapony
 *
 */
public class FieldList<T extends AbstractField> extends AbstractField {
	
	static final String TYPE_PREFIX = "List<";
	static final String TYPE_SUFFIX = ">";

	private final AbstractFieldBuilder<T> fieldBuilder;
	private final List<T> entries;
	
	private final Consumer<List<T>> def;
	
	public FieldList(
			String name,
			boolean isRequired,
			AbstractFieldBuilder<T> fieldBuilder,
			List<T> entries, Consumer<List<T>> def) {
		super(name, TYPE_PREFIX + fieldBuilder.getType() + TYPE_SUFFIX, isRequired);
		this.fieldBuilder = fieldBuilder;
		this.entries = entries == null ? new ArrayList<>() : entries;
		this.def = def;
	}

	@Override
	public String save() {
		List<T> data = getFields();
		
		if (data.isEmpty()) {
			return "{ }";
		}
		
		StringWriter sw = new StringWriter();
		sw.write("{ ");

		try {
			for (T entry : data) {
				FieldManager.writeValue(sw, entry.save());
				sw.write("; ");
			}
		} catch (IOException impossible) {}
		
		sw.write('}');
		
		return sw.toString();
	}
	
	@Override
	public void load(String str) throws IOException {
		List<T> data = getFields();
		data.clear();
		
		StringReader reader = new StringReader(str);
		int c;
		
		do {
			c = reader.read();
			if (c == -1)
				throw new IOException("Invalid List declaration: '{' expected, end of declaration encountered");
		} while (Character.isWhitespace(c));
		
		if (c != '{')
			throw new IOException("Invalid List declaration: '{' expected, '" + ((char) c) + "' encountered");
		
		int i = 0;
		while (true) {
			c = reader.read();
			
			if (c == -1)
				throw new IOException("Invalid List declaration: '}' or value expected, end of declaration encountered");
			
			if (c == '}') break;
			
			if (c == ';')
				throw new IOException("Invalid List declaration: '}' or value expected, ';' encountered");
			
			if (Character.isWhitespace(c)) continue;
			
			T field = newEntry();
			String declar = FieldManager.readValue(reader, field.getType(), getName() + "[" + i + "]");// FIXME first character is dismissed
			field.load(declar);
			data.add(field);
		}
	}
	
	public List<T> getFields() {
		return entries;
	}
	
	public T newEntry() {
		return fieldBuilder.name(null);
	}
	
	public Consumer<List<T>> getDefault() {
		return def;
	}
	
	@Override
	public void reset() {
		getFields().clear();
		if (getDefault() != null)
			getDefault().accept(getFields());
	}

}
