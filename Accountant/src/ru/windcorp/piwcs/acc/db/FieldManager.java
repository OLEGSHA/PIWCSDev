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
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.WeakHashMap;

import ru.windcorp.jputil.chars.CharPredicate;
import ru.windcorp.jputil.chars.EscapeException;
import ru.windcorp.jputil.chars.Escaper;
import ru.windcorp.jputil.chars.reader.CharReader;
import ru.windcorp.jputil.chars.reader.CharReaders;

public class FieldManager {
	
	public static interface FieldLoader<T> {
		T load(String str, T current) throws Exception;
		
		default T loadNullAware(String str, T current) throws IOException {
			if (str == null) return null;
			try {
				return load(str, current);
			} catch (IOException e) {
				throw e;
			} catch (Exception e) {
				throw new IOException(e);
			}
		}
	}
	
	public static interface FieldReader<T> {
		T read(String str) throws Exception;
		
		default FieldLoader<T> toLoader() {
			return (str, ignore) -> read(str);
		}
	}
	
	public static interface FieldWriter<T> {
		String write(T value);
		
		default String writeNullAware(T value) {
			if (value == null) return null;
			return write(value);
		}
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
	
	private static final Escaper ESCAPER = Escaper.JAVA;
	
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
		FieldLoader<?> loader = DEFAULT_LOADERS.get(type);
		
		if (loader == null && clazz.isEnum()) loader = EnumFieldReadWrite.getForClass(clazz.asSubclass(Enum.class));
		
		if (loader == null && FieldValue.class.isAssignableFrom(clazz)) loader = selfLoader();
		
		return (FieldLoader<T>) loader;
	}
	
	@SuppressWarnings("unchecked")
	public static <T> FieldWriter<T> getDefaultWriter(String type, Class<T> clazz) {
		FieldWriter<?> writer = DEFAULT_WRITERS.get(type);
		
		if (writer == null && clazz.isEnum()) writer = EnumFieldReadWrite.getForClass(clazz.asSubclass(Enum.class));
		
		if (writer == null && FieldValue.class.isAssignableFrom(clazz)) writer = selfWriter();
		
		return (FieldWriter<T>) writer;
	}
	
	static String getDefaultTypeName(Class<?> forClass) {
		if (forClass.isAnonymousClass())
			return forClass.getName();
		return forClass.getSimpleName();
	}
	
	public static void registerStandardTypes() {
		final DateTimeFormatter dtFormatter = DateTimeFormatter.ofPattern("uuuu/MM/dd HH:mm:ss ZZZZZ");
		
		registerDefaultTypeReader(String.class, s -> s, s -> s);
		registerDefaultTypeReader(UUID.class, UUID::fromString, UUID::toString);
		registerDefaultTypeReader(ZonedDateTime.class,
				declar -> dtFormatter.parse(declar, ZonedDateTime::from),
				dtFormatter::format);
		registerDefaultTypeReader(LocalDate.class, LocalDate::parse, LocalDate::toString);
	}
	
	private final Map<String, AbstractField> fields = new TreeMap<>();
	
	public synchronized void load(Reader reader, String file) throws IOException {
		CharReader in = CharReaders.wrap(reader);
		
		try {
			while (in.has()) {
				String type = new String(in.readWord());
				if (type.length() == 0) break;
				String name = new String(in.readWord());
				if (name.length() == 0)
					throw new IOException("No name for last field of type " + type, in.getLastException());
				readNameValueSeparator(in, type + " " + name);
				String declar = readValue(in, type + " " + name, ';');
				
				AbstractField field = fields.get(name);
				if (field == null)
					throw new IOException("No field named " + name);
				
				if (!field.getType().equals(type))
					throw new IOException("Field " + field + " is annotated as having type " + type,
							in.getLastException());
				
				if (field.getLoadFlag())
					throw new IOException("Field " + field + " has multiple annotations", in.getLastException());
				
				try {
					field.load(declar);
					field.setLoadFlag(true);
				} catch (RuntimeException e) {
					throw new IOException("Field " + field + " has invalid value", e);
				}
			}
	
			for (AbstractField field : fields.values()) {
				field.onAllFieldsLoaded();
			}
		} catch (Exception e) {
			for (AbstractField field : fields.values()) {
				field.setLoadFlag(false);
			}
			
			throw new IOException("Could not load file " + file, e);
		}
		
		if (in.hasErrored()) throw in.getLastException();
	}
	
	private static void readNameValueSeparator(CharReader in, String desc) throws IOException {
		in.skipWhitespace();
		if (in.consume() != '=')
			throw invalidSyntax(desc, "'=' is missing", in.getLastException());
	}
	
	private static final CharPredicate ESCAPER_UNTIL = CharPredicate.forArray('\"', '\n', '\r');
	public static String readValue(CharReader in, String desc, char until) throws IOException {
		in.skipWhitespace();
		if (in.isEnd())
			throw invalidSyntax(desc, "value is missing", in.getLastException());
		
		if (in.current() == '\"') {
			try {
				in.next();
				String result = new String(ESCAPER.unescape(in, ESCAPER_UNTIL));
				if (in.consume() != '\"')
					throw invalidSyntax(desc, "quoted value not closed", in.getLastException());
				in.skipWhitespace();
				if (in.consume() != until)
					throw invalidSyntax(desc, "'" + until + "' not found", in.getLastException());
				return result;
			} catch (EscapeException e) {
				throw invalidSyntax(desc, "quoted value contains invalid escape sequence", e);
			}
		}
		
		int length = 0;
		in.mark();
		while (in.current() != until) {
			switch (in.current()) {
			case CharReader.DONE:
				throw invalidSyntax(desc, "'" + until + "' not found", in.getLastException());
			case '\n':
			case '\r':
				throw invalidSyntax(desc, "value contains newlines", in.getLastException());
			default:
				length++;
			}
			in.next();
		}
		in.reset();

		String result = in.getString(length);
		in.next();
		if ("null".equals(result)) {
			return null;
		}
		return result;
	}
	
	private static IOException invalidSyntax(String desc, String msg, Exception cause) {
		return new IOException("Field " + desc + " has invalid syntax (" + msg + ")", cause);
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
	
	public static void writeValue(Writer out, String declar) throws IOException {
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
				if (c == ';') {
					quote = true;
					break search;
				}
				for (char u : ESCAPER.getUnsafes()) {
					if (c == u) {
						quote = true;
						break search;
					}
				}
				trailingIsWhitespace = Character.isWhitespace(c);
				continue search;
			}
			
			if (trailingIsWhitespace)
				quote = true;
		}
		
		if (quote) {
			out.write('\"');
			out.write(ESCAPER.escape(CharReaders.wrap(declar)));
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
	
	public <T extends AbstractField> FieldListBuilder<T> newFieldList(AbstractFieldBuilder<T> fieldBuilder) {
		return new FieldListBuilder<>(fieldBuilder, this);
	}
	
	synchronized void addField(AbstractField field) {
		if (this.fields.putIfAbsent(field.getName(), field) != null) {
			throw new IllegalArgumentException("Field " + field + " has a duplicate name");
		}
	}
	
	public Collection<AbstractField> getFields() {
		return this.fields.values();
	}
	
	public AbstractField getField(String name) {
		return this.fields.get(name);
	}

}
