/*
 * MineraGenesis Rock Biomes Addon
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
package ru.windcorp.mineragenesis.rb.config;

import java.text.CharacterIterator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ru.windcorp.mineragenesis.MineraGenesis;

/**
 * @author Javapony
 *
 */
public class ConfigLoader {
	
	private final Variables variables = new Variables();
	private final Map<String, Verb<?>> verbs = new HashMap<>();
	
	private final ConfigReader reader;
	
	private final StackTrace stacktrace = new StackTrace();
	
	public ConfigLoader(ConfigReader reader, Verb<?>... verbs) {
		this.reader = reader;
		
		for (Verb<?> verb : verbs) {
			this.verbs.put(verb.getName(), verb);
		}
	}
	
	public void load() throws ConfigurationException {
		try {
			reader.toMeaningfulChar();
			while (!reader.isEnd()) {
				statement();
				reader.toMeaningfulChar();
			}
		} catch (Exception e) {
			ConfigurationException ce;
			
			if (e instanceof ConfigurationException) {
				ce = (ConfigurationException) e;
			} else {
				ce = new ConfigurationException("Unhandled exception while parsing configuration", e);
			}
			
			ce.setContext(reader, null, variables, stacktrace);
			throw ce;
		}
	}

	private void statement() throws ConfigurationException {
		start("statement");
		
		switch (reader.current()) {
		case '$':
			reader.next();
			variableAssignment();
			break;
		case ';':
			reader.next();
			end("statement");
			return;
		case CharacterIterator.DONE:
			end("statement");
			return;
		default:
			verb(false);
			break;
		}
		
		reader.toMeaningfulChar();
		if (!reader.match(';'))
			throw new ConfigurationException("';' not found after statement");
		
		end("statement");
	}
	
	private Object expression() throws ConfigurationException {
		start("expression");
		
		switch (reader.current()) {
		case '$':
			reader.next();
			return end(variable(), "expression");
		case '"':
			reader.next();
			return end(stringLiteral(), "expression");
		case '<':
			reader.next();
			return end(blockLiteral(), "expression");
		case '[':
			reader.next();
			return end(array(), "expression");
		default:
			if (isNumberBeginning(reader.current())) {
				return end(numberLiteral(), "expression");
			} else {
				return end(verb(true), "expression");
			}
		}
	}

	private void variableAssignment() throws ConfigurationException {
		start("variable-assign");
		
		String variable = new String(reader.readWord());
		reader.toMeaningfulChar();
		
		if (!reader.match('='))
			throw new ConfigurationException("Invalid variable assignment: '=' not found");
		
		reader.toMeaningfulChar();
		Object value = expression();
		variables.put(variable, value);
		
		end("variable-assign");
	}

	private Object variable() throws ConfigurationException {
		start("variable");
		String variable = new String(reader.readWord());
		Object result = variables.get(variable);
		
		if (result == null)
			throw new ConfigurationException("Variable \"$" + variable + "\" is not set");
		
		return end(result, "variable");
	}

	private Object verb(boolean requireResult) throws ConfigurationException {
		start("verb-declaration");
		String verbName = new String(reader.readWord());
		
		if (!reader.match('('))
			throw new ConfigurationException("Assumed verb invocation but '(' not found");
		reader.toMeaningfulChar();
		
		Verb<?> verb = verbs.get(verbName);
		
		if (verb == null)
			throw new ConfigurationException("Verb \"" + verbName + "()\" not found");
		
		if (requireResult && verb.getReturnType() == Void.TYPE)
			throw new ConfigurationException("Verb " + verbName + "() does not have a result");
		
		Arguments arguments = new Arguments();
		try {
			
			boolean commaPlaced = true;
			while (reader.current() != ')') {
				if (reader.isEnd())
					throw new ConfigurationException("Invalid verb invocation: ')' not found (EOF)");
				
				if (!commaPlaced) {
					throw new ConfigurationException("Invalid verb invocation: ',' not found");
				} else {
					commaPlaced = false;
				}
				
				Object value = expression();
				String label = null;
				
				reader.toMeaningfulChar();
				if (reader.match(':')) {
					label = new String(reader.readWord());
					reader.toMeaningfulChar();
				}
				
				if (reader.match(',')) {
					commaPlaced = true;
					reader.toMeaningfulChar();
				}
				
				arguments.data.add(new Argument(value, label));
			}
	
			reader.next();
			start("verb-invocation");
			
			return end(end(verb.run(arguments), "verb-invocation"), "verb-declaration");
		} catch (ConfigurationException e) {
			e.setContext(null, arguments, null, null);
			throw e;
		}
		
	}

	private Object array() throws ConfigurationException {
		start("array");
		List<Object> contents = new ArrayList<>();
		
		reader.toMeaningfulChar();

		boolean commaPlaced = true;
		while (reader.current() != ']') {
			if (reader.isEnd())
				throw new ConfigurationException("Invalid array declaration: ']' not found (EOF)");
			
			if (!commaPlaced) {
				throw new ConfigurationException("Invalid array declaration: ',' not found");
			} else {
				commaPlaced = false;
			}
			
			contents.add(expression());
			
			if (reader.match(',')) {
				commaPlaced = true;
				reader.toMeaningfulChar();
			}
		}
		
		reader.next();
		return end(contents.toArray(), "array");
	}
	
	private Object numberLiteral() throws ConfigurationException {
		start("literal-number");
		String declar = new String(reader.readWord());
		
		if (declar.equals("+INF")) return end(Double.POSITIVE_INFINITY, "literal-number");
		else if (declar.equals("-INF")) return end(Double.NEGATIVE_INFINITY, "literal-number");
		
		try {
			return end(Double.parseDouble(declar), "literal-number");
		} catch (NumberFormatException e) {
			throw new ConfigurationException("\"" + declar + "\" is not a floating-point number", e);
		}
	}
	
	private Object stringLiteral() throws ConfigurationException {
		start("literal-string");
		int length = 0;
		
		reader.mark();
		while (reader.current() != '"') {
			if (reader.isEnd()) throw new ConfigurationException("Invalid string literal: '\"' not found (EOF)");
			if (reader.current() == '\\') reader.next();
			reader.next();
			length++;
		}
		reader.reset();
		
		char[] chars = new char[length];
		
		for (int i = 0; i < chars.length; ++i) {
			if (reader.current() == '\\') reader.next();
			chars[i] = reader.current();
			reader.next();
		}
		
		reader.next();
		return end(new String(chars), "literal-string");
	}

	private Object blockLiteral() throws ConfigurationException {
		start("block-literal");
		int id;
		char[] idDeclar = reader.readWord();
		
		if (idDeclar.length == 0)
			throw new ConfigurationException("ID cannot be empty");
		
		id = 0;
		for (char c : idDeclar) {
			if (c < '0' || c > '9') {
				id = -1;
				break;
			}
			id = 10*id + (c - '0');
		}
		
		if (id < 0) {
			if (!reader.match(':'))
				throw new ConfigurationException("ID is not numeric, expected ':'");
			
			String name; {
				char[] blockName = reader.readWord();
				char[] concat = new char[idDeclar.length + blockName.length + 1];
				System.arraycopy(idDeclar, 0, concat, 0, idDeclar.length);
				concat[idDeclar.length] = ':';
				System.arraycopy(blockName, 0, concat, idDeclar.length + 1, blockName.length);
				name = new String(concat);
			}

			id = MineraGenesis.getBlockIdFromName(name);
		}
		
		int meta;
		if (reader.match('>')) {
			meta = 0;
		} else if (reader.current() == ':') {
			char c = reader.next();
			if (c == '*') {
				reader.next();
				if (!reader.match('>')) throw whineAboutMeta();
				meta = MGID.META_WILDCARD;
			} else {
				if (c < '0' || c > '9') throw whineAboutMeta();
				meta = c - '0';
				reader.next();
				if (!reader.match('>')) {
					if (meta > 1) throw whineAboutMeta();
					c = reader.current();
					if (c < '0' || c > '9') throw whineAboutMeta();
					meta = 10 * meta + (c - '0');
					if (!reader.match('>')) throw whineAboutMeta();
				}
			}
		} else {
			throw whineAboutMeta();
		}
		
		return end(new MGID(id, meta), "block-literal");
	}
	
	private static ConfigurationException whineAboutMeta() {
		return new ConfigurationException("Meta can only be [0; 16) (unsigned) or '*' for wildcard");
	}
	
	private static boolean isNumberBeginning(char c) {
		return (c >= '0' && c <= '9') || c == '+' || c == '-';
	}
	
	private void start(String id) throws ConfigurationException {
		stacktrace.push(id);
	}
	
	private void end(String id) throws ConfigurationException {
		stacktrace.pop(id);
	}
	
	private <T> T end(T t, String id) throws ConfigurationException {
		stacktrace.pop(id);
		return t;
	}
}
