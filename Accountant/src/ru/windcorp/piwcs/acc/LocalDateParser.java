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
package ru.windcorp.piwcs.acc;

import java.text.CharacterIterator;
import java.time.LocalDate;
import java.util.function.Consumer;
import java.util.function.Supplier;

import ru.windcorp.jputil.cmd.AutoCommand.AutoInvocation;
import ru.windcorp.jputil.cmd.CommandSyntaxException;
import ru.windcorp.jputil.cmd.parsers.Parser;

/**
 * @author Javapony
 *
 */
public class LocalDateParser extends Parser {

	public LocalDateParser(String id) {
		super(id, LocalDate.class);
	}

	/**
	 * @see ru.windcorp.jputil.cmd.parsers.Parser#matches(java.text.CharacterIterator, ru.windcorp.jputil.cmd.AutoCommand.AutoInvocation)
	 */
	@Override
	public boolean matches(CharacterIterator data, AutoInvocation inv) {
		try {
			LocalDate.parse(new String(nextWord(data)));
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * @see ru.windcorp.jputil.cmd.parsers.Parser#getProblem(java.text.CharacterIterator, ru.windcorp.jputil.cmd.AutoCommand.AutoInvocation)
	 */
	@Override
	public Supplier<? extends Exception> getProblem(CharacterIterator data, AutoInvocation inv) {
		if (matchOrReset(data, inv)) return null;
		
		String text = new String(nextWord(data));
		return () -> new CommandSyntaxException(inv, "\"" + text + "\" is not a ISO-8601 date (YYYY-MM-DD, Ex.: \"2020-04-30\")");
	}

	/**
	 * @see ru.windcorp.jputil.cmd.parsers.Parser#insertParsed(java.text.CharacterIterator, ru.windcorp.jputil.cmd.AutoCommand.AutoInvocation, java.util.function.Consumer)
	 */
	@Override
	public void insertParsed(CharacterIterator data, AutoInvocation inv, Consumer<Object> output) {
		output.accept(LocalDate.parse(new String(nextWord(data))));
	}

}
