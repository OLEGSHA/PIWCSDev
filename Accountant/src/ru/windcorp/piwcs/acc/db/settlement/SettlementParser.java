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
package ru.windcorp.piwcs.acc.db.settlement;

import java.text.CharacterIterator;
import java.util.function.Consumer;
import java.util.function.Supplier;

import ru.windcorp.jputil.cmd.AutoCommand.AutoInvocation;
import ru.windcorp.jputil.cmd.CommandSyntaxException;
import ru.windcorp.jputil.cmd.parsers.Parser;
import ru.windcorp.piwcs.acc.Accountant;

/**
 * @author Javapony
 *
 */
public class SettlementParser extends Parser {

	/**
	 * @param id
	 */
	public SettlementParser(String id) {
		super(id, Settlement.class);
	}

	/**
	 * @see ru.windcorp.jputil.cmd.parsers.Parser#getProblem(java.text.CharacterIterator, ru.windcorp.jputil.cmd.Invocation)
	 */
	@Override
	public Supplier<Exception> getProblem(CharacterIterator data, AutoInvocation inv) {
		String id = new String(nextWord(data));
		if (id.isEmpty()) return argNotFound(inv); // TODO fix getProblem() for parsers: return null if no problem
		return () -> new CommandSyntaxException(inv, "No settlement has ID \"" + id + "\"");
	}

	/**
	 * @see ru.windcorp.jputil.cmd.parsers.Parser#matches(java.text.CharacterIterator)
	 */
	@Override
	public boolean matches(CharacterIterator data, AutoInvocation inv) {
		return Accountant.getSettlements().get(new String(nextWord(data))) != null;
	}

	/**
	 * @see ru.windcorp.jputil.cmd.parsers.Parser#parse(java.text.CharacterIterator, java.util.function.Consumer)
	 */
	@Override
	public void insertParsed(CharacterIterator data, AutoInvocation inv, Consumer<Object> output) {
		output.accept(Accountant.getSettlements().get(new String(nextWord(data))));
	}

}
