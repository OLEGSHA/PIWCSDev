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
import java.io.Reader;
import java.io.Writer;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.WeakHashMap;

import ru.windcorp.tge2.util.StringUtil;

public class FieldManager {
	
	public static interface FieldLoader<T> {
		T load(String str, T current) throws IOException;
	}
	
	public static interface FieldReader<T> {
		T read(String str) throws IOException;
		
		default FieldLoader<T> toLoader() {
			return (str, ignore) -> read(str);
		}
	}
	
	public static interface FieldWriter<T> {
		String write(T value);
	}
	
	public static interface FieldValue {
		String save();
		void load(String str) throws IOException;
	}
	
	private static final FieldLoader<?> SELF_LOADER = (str, value) -> {
		((FieldValue) value).load(str);
		return value;
	};
	
	private static final FieldWriter<?> SELF_WRITER = value -> ((FieldValue) value).save();
	
	@SuppressWarnings("unchecked")
	static final <T> FieldLoader<T> selfLoader() {
		return (FieldLoader<T>) SELF_LOADER;
	}
	
	@SuppressWarnings("unchecked")
	static final <T> FieldWriter<T> selfWriter() {
		return (FieldWriter<T>) SELF_WRITER;
	}
	
	private static final Map<String, FieldLoader<?>> DEFAULT_LOADERS = new WeakHashMap<>();
	private static final Map<String, FieldWriter<?>> DEFAULT_WRITERS = new WeakHashMap<>();
	
	public static <T> void registerDefaultType(String type, Class<T> clazz, FieldLoader<T> loader, FieldWriter<T> writer) {
		type = type.trim();
		if (type.isEmpty())
			throw new IllegalArgumentException("Type name is empty");
		if (type.chars().anyMatch(Character::isWhitespace))
			throw new IllegalArgumentException("Type name \"" + type + "\" contains whitespace");
		DEFAULT_LOADERS.put(type, loader);
		DEFAULT_WRITERS.put(type, writer);
	}
	
	public static <T> void registerDefaultTypeReader(String type, Class<T> clazz, FieldReader<T> reader, FieldWriter<T> writer) {
		registerDefaultType(type, clazz, reader.toLoader(), writer);
	}
	
	public static <T> void registerDefaultType(Class<T> clazz, FieldLoader<T> loader, FieldWriter<T> writer) {
		registerDefaultType(getDefaultTypeName(clazz), clazz, loader, writer);
	}
	
	public static <T> void registerDefaultTypeReader(Class<T> clazz, FieldReader<T> reader, FieldWriter<T> writer) {
		registerDefaultType(getDefaultTypeName(clazz), clazz, reader.toLoader(), writer);
	}
	
	@SuppressWarnings("unchecked")
	public static <T> FieldLoader<T> getDefaultLoader(String type, Class<T> clazz) {
		return (FieldLoader<T>) DEFAULT_LOADERS.get(type);
	}
	
	@SuppressWarnings("unchecked")
	public static <T> FieldWriter<T> getDefaultWriter(String type, Class<T> clazz) {
		return (FieldWriter<T>) DEFAULT_WRITERS.get(type);
	}
	
	static String getDefaultTypeName(Class<?> forClass) {
		if (forClass.isAnonymousClass())
			return  forClass.getName();
		return forClass.getSimpleName();
	}
	
	public static void registerStandardTypes() {
		registerDefaultTypeReader(String.class, s -> s, s -> s);
		registerDefaultTypeReader(UUID.class, UUID::fromString, UUID::toString);
		registerDefaultTypeReader(ZonedDateTime.class, ZonedDateTime::parse, ZonedDateTime::toString);
	}
	
	private final Map<String, AbstractField> fields = new TreeMap<>();
	
	public synchronized void load(Reader in) throws IOException {
		try {
			StringBuilder sb = new StringBuilder();
			while (true) {
				String type = readWord(in, sb);
				if (type == null) break;
				String name = readWord(in, sb);
				if (name == null)
					throw new IOException("No name for last field of type " + type);
				String declar = readValue(in, sb, type, name);
				
				AbstractField field = fields.get(name);
				if (field == null)
					throw new IOException("No field named " + name);
				
				if (!field.getType().equals(type))
					throw new IOException("Field " + field + " is annotated as having type " + type);
				
				if (field.getLoadFlag())
					throw new IOException("Field " + field + " has multiple annotations");
				
				try {
					field.load(declar);
					field.setLoadFlag(true);
				} catch (RuntimeException e) {
					throw new IOException("Field " + field + " has invalid value", e);
				}
			}
		} catch (Exception e) {
			for (AbstractField field : fields.values()) {
				field.setLoadFlag(false);
			}
			
			if (e instanceof IOException) {
				throw (IOException) e;
			}
			
			throw new IOException(e);
		}

		for (AbstractField field : fields.values()) {
			field.onAllFieldsLoaded();
		}
	}
	
	private static String readWord(Reader reader, StringBuilder sb) throws IOException {
		int c;
		
		while (true) {
			c = reader.read();
			if (c == -1) return null;
			if (!Character.isWhitespace(c)) break;
		}
		
		do {
			sb.append((char) c);
			c = reader.read();
		} while (c != -1 && !Character.isWhitespace(c));
		
		return StringUtil.resetStringBuilder(sb);
	}
	
	private static String readValue(Reader reader, StringBuilder sb, String readingType, String readingName) throws IOException {
		int c;
		
		while (true) {
			c = reader.read();
			if (c == -1)
				throw new IOException("Field " + readingType + " " + readingName + " has invalid syntax ('=' is missing)");;
			if (!Character.isWhitespace(c)) break;
		}
		
		if (c != '=')
			throw new IOException("Field " + readingType + " " + readingName + " has invalid syntax ('=' is missing)");
		
		while (true) {
			c = reader.read();
			if (c == -1)
				throw new IOException("Field " + readingType + " " + readingName + " has invalid syntax (value is missing)");;
			if (!Character.isWhitespace(c)) break;
		}
		
		if (c == '\"') {
			while (true) {
				c = reader.read();
				switch (c) {
				case -1:
					throw new IOException("Field " + readingType + " " + readingName + " has invalid syntax (quoted value not closed)");
				case '\"':
					if (reader.read() != ';')
						throw new IOException("Field " + readingType + " " + readingName + " has invalid syntax (';' not found)");
					return StringUtil.resetStringBuilder(sb);
				case '\\':
					c = reader.read();
					switch (c) {
					case -1:
						throw new IOException("Field " + readingType + " " + readingName
								+ " has invalid syntax (quoted value not closed and invalid escape sequence)");
					case '0':
						sb.append('\0');
						break;
					case 'n':
						sb.append('\n');
						break;
					case 'r':
						sb.append('\r');
						break;
					default:
						sb.append((char) c);
						break;
					}
					break;
				case '\n':
				case '\r':
					throw new IOException("Field " + readingType + " " + readingName + " has invalid syntax (quoted value contains newlines)");
				default:
					sb.append((char) c);
				}
			}
		}
		
		while (true) {
			switch (c) {
			case -1:
				throw new IOException("Field " + readingType + " " + readingName + " has invalid syntax (';' not found)");
			case ';':
				String result = StringUtil.resetStringBuilder(sb);
				if ("null".equals(result)) {
					return null;
				}
				return result;
			case '\n':
			case '\r':
				throw new IOException("Field " + readingType + " " + readingName + " has invalid syntax (value contains newlines)");
			default:
				sb.append((char) c);
			}
			c = reader.read();
		}
	}
	
	public synchronized void save(Writer out) throws IOException {
		int maxTypeLength = 0;
		int maxNameLength = 0;
		
		for (AbstractField field : fields.values()) {
			int typeLength = field.getType().length();
			int nameLength = field.getName().length();
			
			if (typeLength > maxTypeLength) maxTypeLength = typeLength;
			if (nameLength > maxNameLength) maxNameLength = nameLength;
		}
		
		char[] spaces = new char[Math.max(maxNameLength, maxTypeLength) + 1];
		Arrays.fill(spaces, ' ');
		
		for (AbstractField field : fields.values()) {
			out.write(field.getType());
			out.write(spaces, 0, maxTypeLength - field.getType().length() + 1);
			out.write(field.getName());
			out.write(spaces, 0, maxNameLength - field.getName().length() + 1);
			out.write("= ");
			writeValue(out, field.save());
			out.write(";\n");
		}
	}
	
	private static void writeValue(Writer out, String declar) throws IOException {
		if (declar == null) {
			out.write("null");
			return;
		} else if (declar.isEmpty()) {
			out.write("\"\"");
			return;
		} else if ("null".equals(declar)) {
			out.write("\"null\"");
			return;
		}
		
		char[] chars = declar.toCharArray();
		
		boolean quote = Character.isWhitespace(chars[0]);
		
		if (!quote) {
			boolean trailingIsWhitespace = false;
			
			search:
			for (char c : chars) {
				switch (c) {
				case '\\':
				case '\"':
				case '\n':
				case '\r':
				case '\0':
					quote = true;
					break search;
				default:
					trailingIsWhitespace = Character.isWhitespace(c);
					continue search;
				}
			}
			
			if (trailingIsWhitespace)
				quote = true;
		}
		
		if (quote) {
			out.write('\"');
			
			for (char c : chars) {
				switch (c) {
				case '\\':
				case '\"':
					out.write('\\');
					out.write(c);
					break;
				case '\n':
					out.write("\\n");
					break;
				case '\r':
					out.write("\\r");
					break;
				case '\0':
					out.write("\\0");
					break;
				default:
					out.write(c);
					break;
				}
			}
			
			out.write('\"');
		} else {
			out.write(chars);
		}
	}

	public <T> FieldBuilder<T> newField(Class<T> clazz) {
		return new FieldBuilder<T>(clazz, this);
	}
	
	public IntFieldBuilder newIntField() {
		return new IntFieldBuilder(this);
	}
	
	public LongFieldBuilder newLongField() {
		return new LongFieldBuilder(this);
	}
	
	public DoubleFieldBuilder newDoubleField() {
		return new DoubleFieldBuilder(this);
	}
	
	synchronized void addField(AbstractField field) {
		if (this.fields.putIfAbsent(field.getName(), field) != null) {
			throw new IllegalArgumentException("Field " + field + " has a duplicate name");
		}
	}

}
