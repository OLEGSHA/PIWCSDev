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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class Arguments {
	
	final List<Argument> data = new LinkedList<>();
	
	public <E> E get(String label, Class<E> type) throws ConfigurationException {
		E result = get(label, type, null);
		
		if (result != null) return result;
		
		if (label == null)
			throw new ConfigurationException("No argument of type " + type.getSimpleName() + " found");
		else
			throw new ConfigurationException("No argument with label " + label + " found");
	}
	
	public <E> E get(String label, Class<E> type, E def) throws ConfigurationException {
		Iterator<Argument> it = data.iterator();
		while (it.hasNext()) {
			Argument arg = it.next();
			if (label != null) {
				if (label.equals(arg.label)) {
					it.remove();
					return arg.as(type);
				}
			} else if (arg.is(type)) {
				it.remove();
				return arg.as(type);
			}
		}
		
		return def;
	}
	
	public List<Object> get() {
		List<Object> result = new ArrayList<>();
		for (Argument arg : data) result.add(arg.object);
		data.clear();
		return result;
	}
	
	public short getBlock(String label) throws ConfigurationException {
		return get(label, MGID.class).toShort();
	}
	
	public short getBlock(String label, short def) throws ConfigurationException {
		MGID mgid = get(label, MGID.class, null);
		return mgid == null ? def : mgid.toShort();
	}
	
	public Short getBlockOrNull(String label) throws ConfigurationException {
		MGID mgid = get(label, MGID.class, null);
		return mgid == null ? null : mgid.toShort();
	}
	
	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		if (data.isEmpty()) return "Empty";
		
		StringBuilder sb = new StringBuilder("{");
		for (Argument arg : data) sb.append("\n  ").append(arg);
		sb.append("\n}");
		return sb.toString();
	}
}