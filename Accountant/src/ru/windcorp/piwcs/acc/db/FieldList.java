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
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import ru.windcorp.jputil.chars.reader.CharReader;
import ru.windcorp.jputil.chars.reader.CharReaders;

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
		
		CharReader in = CharReaders.wrap(str);
		
		in.skipWhitespace();
		if (in.isEnd()) throw new IOException("Invalid List declaration: '{' expected, end of declaration encountered");
		if (in.current() != '{')
			throw new IOException("Invalid List declaration: '{' expected, '" + in.current() + "' encountered");
		
		int i = 0;
		while (true) {
			in.next();
			in.skipWhitespace();
			
			if (in.isEnd())
				throw new IOException("Invalid List declaration: '}' or value expected, end of declaration encountered");
			
			if (in.current() == '}') break;
			
			if (in.current() == ';')
				throw new IOException("Invalid List declaration: '}' or value expected, ';' encountered");
			
			T field = newEntry();
			String declar = FieldManager.readValue(in, field.getType() + " " + getName() + "[" + i + "]", ';');// TODO first character is dismissed
			field.load(declar);
			data.add(field);
		}
		
		in.skipWhitespace();
		if (in.next() != CharReader.DONE) {
			throw new IOException("Invalid List declaration: encountered excessive characters after '}'");
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
