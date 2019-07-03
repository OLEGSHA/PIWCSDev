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
package ru.windcorp.piwcs.acc;

import java.text.CharacterIterator;
import java.util.function.Consumer;
import java.util.function.Supplier;

import ru.windcorp.jputil.Version;
import ru.windcorp.jputil.cmd.CommandSyntaxException;
import ru.windcorp.jputil.cmd.Invocation;
import ru.windcorp.jputil.cmd.parsers.Parser;

/**
 * @author Javapony
 *
 */
class VersionParser extends Parser {

	/**
	 * @param id
	 */
	public VersionParser(String id) {
		super(id);
	}

	/**
	 * @see ru.windcorp.jputil.cmd.parsers.Parser#getProblem(java.text.CharacterIterator, ru.windcorp.jputil.cmd.Invocation)
	 */
	@Override
	public Supplier<CommandSyntaxException> getProblem(CharacterIterator data, Invocation inv) {
		char[] declar = nextWord(data);
		if (declar.length == 0) return argNotFound(inv);
		return () -> new CommandSyntaxException(inv, String.valueOf(declar) + " is not a valid Version");
	}

	/**
	 * @see ru.windcorp.jputil.cmd.parsers.Parser#matches(java.text.CharacterIterator)
	 */
	@Override
	public boolean matches(CharacterIterator data) {
		char[] declar = nextWord(data);
		if (declar.length == 0) return false;
		for (char c : declar) {
			if (c != '.' && !(c >= '0' && c <= '9')) {
				return false;
			}
		}
		return true;
	}

	/**
	 * @see ru.windcorp.jputil.cmd.parsers.Parser#parse(java.text.CharacterIterator, java.util.function.Consumer)
	 */
	@Override
	public void parse(CharacterIterator data, Consumer<Object> output) {
		skipWhitespace(data);
		
		int index = data.getIndex();
		int elements = 1;
		
		while (true) {
			char c = data.current();
			if (c == CharacterIterator.DONE) break;
			else if (c == '.') elements++;
			else if (Character.isWhitespace(c)) break;
			data.next();
		}
		
		data.setIndex(index);
		
		int[] subversions = new int[elements];
		int nextArrayIndex = 0;
		int current = 0;
		
		while (true) {
			char c = data.current();
			if (c == CharacterIterator.DONE) break;
			else if (c == '.') {
				subversions[nextArrayIndex++] = current;
				current = 0;
			} else if (Character.isWhitespace(c)) break;
			else current = current * 10 + (c - '0');
			data.next();
		}
		
		subversions[nextArrayIndex] = current;
		
		output.accept(new Version(subversions));
	}

	/**
	 * @see ru.windcorp.jputil.cmd.parsers.Parser#insertArgumentClasses(java.util.function.Consumer)
	 */
	@Override
	public void insertArgumentClasses(Consumer<Class<?>> output) {
		output.accept(Version.class);
	}

}
