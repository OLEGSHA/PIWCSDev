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

import cpw.mods.fml.common.FMLCommonHandler;
import ru.windcorp.mineragenesis.addon.MGAddonManager;
import ru.windcorp.mineragenesis.interfaces.*;
import ru.windcorp.mineragenesis.request.GenerationRequest;

public class MineraGenesis {
	
	public static final String DISPLAY_NAME = "MineraGenesis";
	public static final String SAFE_NAME = "mineragenesis";
	public static final int API_VERSION = 2;
	public static final String VERSION = "1.7.10-" + API_VERSION + "-1.2";
	
	private static MGApplicationRequestBuilder applicationRequestBuilder = null;
	private static MGChunkExporter chunkExporter = null;
	private static MGChunkImporter chunkImporter = null;
	private static MGHelper helper = null;
	private static MGIdTranslator idTranslator = null;
	
	public static MGLogger logger = null;
	public static boolean isDebugging = false;
	
	private static MGChunkProcessor processor = new MGChunkProcessor() {
		final java.util.concurrent.atomic.AtomicBoolean flag = new java.util.concurrent.atomic.AtomicBoolean(false);
		@Override
		public void processChunk(GenerationRequest request) {
			if (flag.compareAndSet(false, true)) {
				logger.log("!!!    MineraGenesis  Warning   !!!");
				logger.log("!!! No MGChunkProcessor present !!!");
			}
		}
	};
	
	public static MGApplicationRequestBuilder getApplicationRequestBuilder() {
		if (applicationRequestBuilder == null) {
			throw new IllegalStateException("Application Request Builder implementation not set");
		}
		
		return applicationRequestBuilder;
	}
	
	public static MGChunkExporter getChunkExporter() {
		if (chunkExporter == null) {
			throw new IllegalStateException("Chunk Exporter implementation not set");
		}
		
		return chunkExporter;
	}
	
	public static MGChunkImporter getChunkImporter() {
		if (chunkImporter == null) {
			throw new IllegalStateException("Chunk Importer implementation not set");
		}
		
		return chunkImporter;
	}
	
	public static MGHelper getHelper() {
		if (helper == null) {
			throw new IllegalStateException("Helper not set");
		}
		
		return helper;
	}
	
	public static int getBlockIdFromName(String name) {
		return idTranslator.getIdFromName(name);
	}

	public static void setImplementation(
			MGApplicationRequestBuilder applicationRequestBuilder,
			MGChunkExporter chunkExporter,
			MGChunkImporter chunkImporter,
			MGHelper helper,
			MGLogger logger,
			MGIdTranslator idTranslator
			) {
		
		MineraGenesis.applicationRequestBuilder = applicationRequestBuilder;
		MineraGenesis.chunkExporter = chunkExporter;
		MineraGenesis.chunkImporter = chunkImporter;
		MineraGenesis.helper = helper;
		MineraGenesis.logger = logger;
		MineraGenesis.idTranslator = idTranslator;
		
		if (isDebugging) {
			logger.debug("Implementation set by %s", Thread.currentThread().getStackTrace()[2]);
		}
	}
	
	public static MGChunkProcessor getProcessor() {
		return processor;
	}

	public static void setProcessor(MGChunkProcessor processor) {
		MineraGenesis.processor = processor;
		if (isDebugging) {
			logger.debug("MGChunkProcessor set to %s by %s", processor, Thread.currentThread().getStackTrace()[2]);
		}
	}

	public static void actInServerThread() {
		int i;
		for (i = 0; i < 8 && MGQueues.importNextChunk(); ++i);
		if (i != 0) logger.debug("Imported %d chunks", i);
		
		for (i = 0; i < 8 && MGQueues.exportNextChunk(); ++i);
		if (i != 0) logger.debug("Exported %d chunks", i);
	}
	
	public static void loadConfig() {
		MGConfig.load();
	}
	
	public static void attemptEarlyAddonInit() {
		if (MGAddonManager.areAddonsInitialized()) {
			logger.log("Early addon initialization requested but is already done. This should not happen! Skipping!");
			logger.logf("Offender: " + Thread.currentThread().getStackTrace()[2]);
			return;
		}
		
		if (MGConfig.isEarlyInitForced()) {
			logger.log("Beginning early addon initialization: forced by config");
			MGAddonManager.initializeAddons();
			return;
		}
		
		if (isBukkitPresent()) {
			logger.log("Skipping early addon initialization: Bukkit detected (\"Early\", faster init can be forced in config)");
			return;
		}
		
		logger.log("Beginning early addon initialization: no exceptions apply (this is normal)");
		MGAddonManager.initializeAddons();
	}
	
	private static boolean isBukkitPresent() {
		try {
			Class.forName("org.bukkit.Bukkit", false, null);
			
			return true;
		} catch (ClassNotFoundException e) {
			return false;
		}
	}

	public static void onServerStarted() {
		if (!MGAddonManager.areAddonsInitialized()) {
			logger.log("Beginning late addon initialization: server starting but initialization has not been done yet");
			MGAddonManager.initializeAddons();
		} else {
			logger.debug("Skipping late addon initialization: already initialized");
		}
		
		MGQueues.start();
	}
	
	public static void onServerStopping() {
		MGQueues.stop();
	}

	public static void crash(Exception exception) {
		logger.log("A fatal error has occurred, terminating");
		exception.printStackTrace();
		FMLCommonHandler.instance().exitJava(719, false);
	}
	
}
