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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.stream.Collectors;

public class ConfigReader implements CharacterIterator {
	
	private final CharacterIterator iterator;
	private final String source;
	
	private final Deque<Integer> marks = new ArrayDeque<>();
	
	public ConfigReader(Path path) throws IOException {
		this.source = Files.lines(path, StandardCharsets.UTF_8)
				.collect(Collectors.joining("\n", "", "")); // Force \n as line delimiter while we're at it
		this.iterator = new StringCharacterIterator(source);
	}
	
	public int mark() {
		marks.push(getIndex());
		return getIndex();
	}
	
	public void reset() {
		setIndex(marks.pop());
	}
	
	public void rollBack(int rollBack) {
		setIndex(getIndex() - rollBack);
	}
	
	public void skipWhitespace() {
		while (Character.isWhitespace(current())) next();
	}
	
	public void toMeaningfulChar() {
		int commentaryLevel = 0;
		
		while (true) {
			if (isEnd()) {
				break;
			} else if (current() == '/') {
				next();
				if (commentaryLevel == 0 && current() == '/') {
					do next(); while (current() != '\n');
				} else if (current() == '*') {
					commentaryLevel++;
				} else {
					rollBack(1);
					break;
				}
			} else if (commentaryLevel > 0) {
				if (current() == '*') {
					if (next() == '/') {
						commentaryLevel--;
					} else {
						rollBack(1);
					}
				}
			} else if (commentaryLevel == 0 && !Character.isWhitespace(current())) {
				break;
			}
			
			next();
		}
	}
	
	public char[] readWord() {
		int length = 0;
		
		mark();
		while (isExtendedAlphanumeric(current())) {
			length++;
			next();
		}
		reset();
		
		char[] result = new char[length];
		
		for (int i = 0; i < length; ++i) {
			result[i] = current();
			next();
		}
		
		return result;
	}
	
	public static boolean isExtendedAlphanumeric(char c) {
		return c == '.' || c == '-' || c == '+' || isAlphanumeric(c);
	}
	
	public static boolean isAlphanumeric(char c) {
		return  (c >= '0' && c <= '9') ||
				(c >= 'a' && c <= 'z') ||
				(c >= 'A' && c <= 'Z') ||
				c == '_';
	}

	public char[] findWord() {
		skipWhitespace();
		return readWord();
	}
	
	public boolean match(char c) {
		toMeaningfulChar();
		if(current() == c) {
			next();
			return true;
		} else {
			return false;
		}
		
	}
	
	@Override
	public String toString() {
		int limit = mark() + 1;
		try {
			int lineNumber = 1, position = 1;
			int lineStartIndex = 0;
			
			first();
			while (true) {
				if (isEnd()) break;
				if (current() == '\n') {
					if (getIndex() >= limit) break;
					lineNumber++;
					lineStartIndex = getIndex() + 1;
					position = 1;
				} else {
					if (getIndex() < limit) position++;
				}
				next();
			}
			
			String line = source.substring(lineStartIndex, getIndex()).replace('\t', ' ');
			
			StringBuilder sb = new StringBuilder("Line: ").append(lineNumber)
					.append(", pos: ").append(position).append("\n\"")
					.append(line).append("\"\n");
			for (int i = 0; i < position - 1; ++i) sb.append(' ');
			return sb.append("^").toString();
		} finally {
			reset();
		}
	}

	@Override
	public char first() {
		return iterator.first();
	}

	@Override
	public char last() {
		return iterator.last();
	}

	@Override
	public char current() {
		return iterator.current();
	}
	
	public boolean isEnd() {
		return getIndex() >= getEndIndex();
	}

	@Override
	public char next() {
		return iterator.next();
	}

	@Override
	public char previous() {
		return iterator.previous();
	}

	@Override
	public char setIndex(int position) {
		return iterator.setIndex(position);
	}

	@Override
	public int getBeginIndex() {
		return iterator.getBeginIndex();
	}

	@Override
	public int getEndIndex() {
		return iterator.getEndIndex();
	}

	@Override
	public int getIndex() {
		return iterator.getIndex();
	}

	@Override
	public Object clone() {
		return iterator.clone();
	}

}
