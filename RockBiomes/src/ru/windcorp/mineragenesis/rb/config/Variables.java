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

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedSet;
import java.util.TreeSet;

class Variables {
	
	private final Map<String, Object> map = new HashMap<>();
	
	public Object get(String name) {
		return map.get(name);
	}
	
	public void put(String name, Object obj) {
		map.put(name, obj);
	}
	
	@Override
	public String toString() {
		if (map.isEmpty()) {
			return "No variables set";
		}
		
		StringBuilder sb = new StringBuilder();
		
		SortedSet<Entry<String, Object>> entries = new TreeSet<>(Comparator.comparing(Entry::getKey));
		entries.addAll(map.entrySet());
		
		for (Entry<String, Object> entry : entries) {
			if (entries.first() != entry) sb.append('\n');
			sb.append("  ").append(entry.getKey()).append(" = ").append(entry.getValue().getClass().getSimpleName()).append(" \"").append(entry.getValue()).append('"');
		}
		
		return sb.toString();
	}

}
