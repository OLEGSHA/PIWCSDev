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

/**
 * @author Javapony
 *
 */
class StackTrace {
	
	public static final int MAX_DEPTH = 256;
	
	private final String[] elements = new String[MAX_DEPTH];
	private int depth = 0;
	
	public void push(String element) throws ConfigurationException {
		try {
			elements[depth] = element;
			depth++;
		} catch (ArrayIndexOutOfBoundsException e) {
			throw new ConfigurationException("Cannot enter " + element + ": too deep", e);
		}
	}
	
	public void pop(String element) throws ConfigurationException {
		if (elements[--depth] != element) {
			depth++;
			
			if (element.equals(elements[depth])) {
				throw new ConfigurationException("(Code fault) Could not pop stacktrace element " + element +
						": top of stack is different (element hash " + element.hashCode() + ", found hash " + elements[depth].hashCode() + ")");
			} else {
				throw new ConfigurationException("(Code fault) Could not pop stacktrace element " + element +
						": top of stack is different (\"" + elements[depth] + "\")");
			}
		}
	}
	
	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		if (depth == 0) {
			return "Empty";
		}
		
		StringBuilder sb = new StringBuilder("{");
		
		for (int i = depth - 1; i >= 0; --i) {
			sb.append("\n  ");
			sb.append(elements[i]);
			sb.append(';');
		}
		
		return sb.append("\n}").toString();
	}

}
