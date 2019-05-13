/*
 * PIWCS Vrata Plugin
 * Copyright (C) 2019  PIWCS Team
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

package ru.windcorp.piwcs.vrata;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class VrataTemplates {
	
	private static final Map<String, String> MAP = new HashMap<>();
	
	public static void load() throws FileNotFoundException {
		try (Scanner scanner = new Scanner(new File(VrataPlugin.getInst().getDataFolder(), "templates.cfg"))) {
			while (scanner.hasNext()) {
				MAP.put(scanner.next(), translateAlternateColorCodes('&', scanner.nextLine().trim().replace('^', '\n')));
			}
		}
	}
	
	private static String translateAlternateColorCodes(char altColorChar, String textToTranslate) {
		char[] b = textToTranslate.toCharArray();
        for (int i = 0; i < b.length - 1; i++) {
            if (b[i] == altColorChar && "0123456789AaBbCcDdEeFfKkLlMmNnOoRr".indexOf(b[i+1]) > -1) {
                b[i] = '\u00a7';
                b[i+1] = Character.toLowerCase(b[i+1]);
            }
        }
        return new String(b);
	}
	
	public static String get(String key) {
		return MAP.get(key);
	}
	
	public static String getf(String key, Object... args) {
		return String.format(get(key), args);
	}

}
