/*
 * PIWCS Accountant Plugin
 * Copyright (C) 2020  Javapony/OLEGSHA
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
package ru.windcorp.piwcs.acc.db.user;

import java.io.IOException;
import java.util.AbstractMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import ru.windcorp.jputil.chars.EscapeException;
import ru.windcorp.jputil.chars.Escaper;
import ru.windcorp.jputil.chars.StringUtil;
import ru.windcorp.jputil.iterators.FunctionIterator;
import ru.windcorp.piwcs.acc.db.FieldManager.FieldValue;

/**
 * @author Javapony
 *
 */
public class ContactRecordSet extends AbstractMap<String, String> implements FieldValue {
	
	private static final Escaper ESCAPER = Escaper.create().strict(false).withChars(",:", "cC").build();
	
	private final Map<String, String> contents = new TreeMap<>();
	
	@Override
	public Set<Entry<String, String>> entrySet() {
		return contents.entrySet();
	}
	
	@Override
	public String put(String key, String value) {
		return contents.put(key, value);
	}

	@Override
	public String save() {
		return StringUtil.iteratorToString(
				new FunctionIterator<>(
						entrySet().iterator(),
						e -> ESCAPER.escape(e.getKey()) + ": " + ESCAPER.escape(e.getValue())
				),
				", "
		);
	}

	@Override
	public void load(String str) throws IOException {
		Map<String, String> buffer = new TreeMap<>();
		
		try {
			for (int startOfRecord = 0, endOfRecord; startOfRecord < str.length(); startOfRecord = endOfRecord + 1) {
				
				endOfRecord = str.indexOf(',', startOfRecord);
				if (endOfRecord < 0) endOfRecord = str.length();
				
				int separator = str.indexOf(':', startOfRecord);
				if (separator < 0 || separator >= endOfRecord) {
					throw new IOException("Contact record \"" + str.substring(startOfRecord, endOfRecord) + "\" does not contain a separator ':'");
				}
				
				String method = ESCAPER.unescape(str.substring(startOfRecord, separator).trim());
				String data = ESCAPER.unescape(str.substring(separator + 1, endOfRecord).trim());
				
				contents.put(method, data);
			}
		} catch (EscapeException e) {
			throw new IOException("Could not parse contact record", e);
		}
		
		contents.clear();
		contents.putAll(buffer);
	}

}
