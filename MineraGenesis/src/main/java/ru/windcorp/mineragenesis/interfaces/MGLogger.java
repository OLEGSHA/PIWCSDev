/* 
 * MineraGenesis Minecraft mod
 * Copyright (C) 2019  Javapony and contributors
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
 */
package ru.windcorp.mineragenesis.interfaces;

import ru.windcorp.mineragenesis.MineraGenesis;

@FunctionalInterface
public interface MGLogger {
	
	void log(Object msg);
	
	default void logf(String format, Object... args) {
		log(String.format(format, args));
	}
	
	default void debug(String format, Object... args) {
		if (MineraGenesis.isDebugging) {
			logf(format, args);
		}
	}

}
