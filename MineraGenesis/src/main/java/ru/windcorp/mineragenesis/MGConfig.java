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
package ru.windcorp.mineragenesis;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static ru.windcorp.mineragenesis.MineraGenesis.logger;

public class MGConfig {
	
	private static List<String> addonLoadOrder = null;
	private static Set<String> disabledAddons = null;
	
	private static Boolean earlyInitForced = null;
	
	private static Integer threads = null;
	private static Long terminationWaitTimeout = null;
	
	private static final int NOT_SET = -1;
	private static long maxQueueLogJunk = NOT_SET;
	private static int importsPerTick = NOT_SET;
	private static int exportsPerTick = NOT_SET;
	
	private static boolean[] dimensionListNatural = null;
	private static boolean[] dimensionListNegative = null;
	private static boolean dimensionListIsWhitelist = false;
	private static boolean dimensionListIsWhitelistIsSet = false;
	
	private static boolean isDebugSet = false;
	
	public static void load() {
		Path file = MineraGenesis.getHelper().getGlobalConfigurationDirectory().resolve("config.cfg");
		
		if (!Files.exists(file)) {
			logger.logf("Configuration file %s not found, exporting default", file);
			
			try {
				OutputStream output = Files.newOutputStream(file, StandardOpenOption.CREATE);
				InputStream input = MineraGenesis.class.getClassLoader().getResourceAsStream("configDefault.cfg");
				
				byte[] buffer = new byte[1024 * 4];
				int read;
				while ((read = input.read(buffer)) != -1) {
					output.write(buffer, 0, read);
				}
				
			} catch (IOException e) {
				logger.logf("Could not write default configuration file %s: %s", file, e);
				throw new RuntimeException("Could not write default configuration file", e);
			}
		}
		
		logger.logf("Reading configuration from %s", file);
		try {
			Files.lines(file).forEach(line -> {
				
				line = line.trim();
				if (line.isEmpty() || line.startsWith("#")) {
					return;
				}
				
				String[] parts = line.split("=", 2);
				if (parts.length < 2 || parts[1] == null) {
					logger.logf("Could not parse line \"%s\": missing separator \'=\'. Skipping", line);
					return;
				}
				parseLine(parts[0].trim().toLowerCase(), parts[1].trim());
				
			});
		} catch (IOException e) {
			logger.logf("Could not read configuration file %s: %s", file, e);
			throw new RuntimeException("Could not read configuration file", e);
		}
		
		checkCompleteness();
	}

	private static void parseLine(String name, String value) {
		switch (name) {
		case "addonloadorder":
			addonLoadOrder = Arrays.asList(value.split(";"));
			break;
		case "disabledaddons":
			String[] ids = value.split(";");
			disabledAddons = new HashSet<>(ids.length);
			for (String id : ids) {
				disabledAddons.add(id);
			}
			break;
		case "earlyinitforced":
			earlyInitForced = Boolean.getBoolean(value.toLowerCase());
			break;
		case "threads":
			try {
				threads = Integer.valueOf(value);
				if (threads <= 0) {
					threads = Runtime.getRuntime().availableProcessors() - (-threads);
					if (threads <= 0) {
						threads = 1;
					}
				}
			} catch (NumberFormatException e) {
				logger.logf("Could not parse value for \"threads\": \"%s\" in not an integer. Skipping", value);
			}
			break;
		case "terminationwaittimeout":
			try {
				terminationWaitTimeout = (long) (Double.valueOf(value) * 60 * 1000);
			} catch (NumberFormatException e) {
				logger.logf("Could not parse value for \"terminationWaitTimeout\": \"%s\" in not an integer. Skipping", value);
			}
			break;
		case "maxqueuelogjunk":
			try {
				maxQueueLogJunk = (long) (Double.parseDouble(value) * 1024 * 1024);
			} catch (NumberFormatException e) {
				logger.logf("Could not parse value for \"maxqueuelogjunk\": \"%s\" in not an integer. Skipping", value);
			}
			break;
		case "importspertick":
			try {
				importsPerTick = Integer.valueOf(value);
				if (importsPerTick <= 0) {
					importsPerTick = Integer.MAX_VALUE;
				}
			} catch (NumberFormatException e) {
				logger.logf("Could not parse value for \"importsPerTick\": \"%s\" in not an integer. Skipping", value);
			}
			break;
		case "exportspertick":
			try {
				exportsPerTick = Integer.valueOf(value);
				if (exportsPerTick <= 0) {
					exportsPerTick = Integer.MAX_VALUE;
				}
			} catch (NumberFormatException e) {
				logger.logf("Could not parse value for \"exportsPerTick\": \"%s\" in not an integer. Skipping", value);
			}
			break;
		case "dimensionlist":
			parseDimensionList(value);
			break;
		case "dimensionlistiswhitelist":
			dimensionListIsWhitelist = "true".equals(value.toLowerCase());
			dimensionListIsWhitelistIsSet = true;
			break;
		case "debug":
			MineraGenesis.isDebugging = "true".equals(value.toLowerCase());
			isDebugSet = true;
			break;
			
		default:
			logger.logf("Encountered unknown key \"%s\". Skipping", name);
		}
	}

	private static void parseDimensionList(String value) {
		String[] parts = value.split(";");
		
		ArrayList<Integer> intermediateList = new ArrayList<>(256);
		int naturalLength = 0, negativeLength = 0;
		
		for (String str : parts) {
			try {
				int id = Integer.parseInt(str);
				intermediateList.add(id);
				
				if (+id >= naturalLength) {
					naturalLength = +id + 1;
				} else if (-id < negativeLength) {
					negativeLength = -id;
				}
			} catch (NumberFormatException e) {
				logger.logf("Could not parse dimension ID \"%s\" in value for \"dimensionList\": not an integer. Skipping", str);
			}
		}
		
		dimensionListNatural = new boolean[naturalLength];
		dimensionListNegative = new boolean[negativeLength];
		
		for (int id : intermediateList) {
			if (id >= 0) {
				dimensionListNatural[+id] = true;
			} else {
				dimensionListNegative[-id - 1] = true;
			}
		}
	}

	private static void checkCompleteness() {
		if (addonLoadOrder == null) failCompletenessCheck("addonLoadOrder");
		if (disabledAddons == null) failCompletenessCheck("disabledAddons");
		if (earlyInitForced == null) failCompletenessCheck("earlyInitForced");
		if (threads == null) failCompletenessCheck("theads");
		if (terminationWaitTimeout == null) failCompletenessCheck("terminationWaitTimeout");
		if (maxQueueLogJunk == NOT_SET) failCompletenessCheck("maxQueueLogJunk");
		if (importsPerTick == NOT_SET) failCompletenessCheck("importsPerTick");
		if (exportsPerTick == NOT_SET) failCompletenessCheck("exportsPerTick");
		if (dimensionListNatural == null) failCompletenessCheck("dimensionList");
		if (!dimensionListIsWhitelistIsSet) failCompletenessCheck("dimensionListIsWhitelist");
		if (!isDebugSet) failCompletenessCheck("debug");
	}
	
	private static void failCompletenessCheck(String missingKey) {
		logger.logf("Key %s has not been set", missingKey);
		throw new RuntimeException("Key " + missingKey + " has not been set");
	}

	public static List<String> getAddonLoadOrder() {
		return addonLoadOrder;
	}
	
	public static Set<String> getDisabledAddons() {
		return disabledAddons;
	}
	
	public static boolean isEarlyInitForced() {
		return earlyInitForced;
	}

	public static int getThreads() {
		return threads;
	}
	
	public static Long getTerminationWaitTimeout() {
		return terminationWaitTimeout;
	}
	
	public static long getMaxQueueLogJunk() {
		return maxQueueLogJunk;
	}

	public static int getImportsPerTick() {
		return importsPerTick;
	}

	public static int getExportsPerTick() {
		return exportsPerTick;
	}

	public static boolean isDimensionHandled(int dimension) {
		if (+dimension >= dimensionListNatural.length) {
			return !dimensionListIsWhitelist;
		} else if (+dimension >= 0) {
			return dimensionListIsWhitelist == dimensionListNatural[+dimension];
		} else if (-dimension - 1 >= dimensionListNegative.length) {
			return !dimensionListIsWhitelist;
		} else {
			return dimensionListIsWhitelist == dimensionListNegative[-dimension - 1];
		}
	}

}
