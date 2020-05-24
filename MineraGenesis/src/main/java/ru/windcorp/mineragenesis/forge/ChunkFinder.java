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
package ru.windcorp.mineragenesis.forge;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.world.World;
import net.minecraftforge.common.DimensionManager;
import ru.windcorp.mineragenesis.MGConfig;
import ru.windcorp.mineragenesis.MGQueues;
import ru.windcorp.mineragenesis.MineraGenesis;

class ChunkFinder {

	private static final Map<World, SynchronizedChunkSet> POPULATED_CHUNKS =
			Collections.synchronizedMap(new HashMap<>());
	
	public static void load() {
		makeSurePopulatedChunksDirectoryExists();
		
		try {
			Files.list(getPopulatedChunksDirectory()).forEach(path -> {
				if (!Files.isRegularFile(path)) return;
				
				String name = path.getFileName().toString();
				if (!name.startsWith("populated-chunks_") || !name.endsWith(".mgpc")) return;
				
				int dimension;
				try {
					dimension = Integer.parseInt(name.substring(
							"populated-chunks_".length(),
							name.length() - ".mgpc".length()
					));
				} catch (NumberFormatException e) {
					return;
				}
				
				loadOnePath(path, dimension);
			});
		} catch (IOException e) {
			MineraGenesis.crash(e, "Could not load populated chunk cache from directory %s",
					getPopulatedChunksDirectory()
			);
		}
	}
	
	private static void loadOnePath(Path path, int dimension) {
		World world = DimensionManager.getWorld(dimension);
		
		if (world == null) {
			MineraGenesis.logger.logf("Unable to find dimension with ID %d. Its presence is suggested by populated chunk cache file %s",
					dimension, path
			);
			return;
		}
		
		SynchronizedChunkSet result = new SynchronizedChunkSet();
		
		try {
			result.load(path);
		} catch (IOException e) {
			MineraGenesis.crash(e, "Could not load populated chunk cache from %s", path);
		}

		MineraGenesis.logger.debug("Loaded populated chunk cache for dimension %d. Cache contains %d entries",
				dimension, result.getSize()
		);
		
		POPULATED_CHUNKS.put(world, result);
	}
	
	public static void unload() {
		makeSurePopulatedChunksDirectoryExists();
		
		POPULATED_CHUNKS.forEach((world, set) -> {
			
			Path path = getPopulatedChunksDirectory().resolve(
					"populated-chunks_" + world.provider.dimensionId + ".mgpc"
			);
			
			try {
				set.save(path);
			} catch (IOException e) {
				MineraGenesis.crash(e, "Could not save populated chunk cache for dimension %d into %s",
						world.provider.dimensionId,
						path
				);
			}
		});
		
		POPULATED_CHUNKS.clear();
	}
	
	private static void makeSurePopulatedChunksDirectoryExists() {
		Path path = getPopulatedChunksDirectory();
		
		if (!Files.isDirectory(path)) {
			try {
				Files.createDirectory(path);
			} catch (IOException e) {
				MineraGenesis.crash(e, "Could not create populated chunk cache directory %s",
						path
				);
			}
		}
	}

	private static Path getPopulatedChunksDirectory() {
		return MineraGenesis.getHelper().getWorldDataDirectory().resolve("populated-chunks");
	}

	public static void onChunkFinishedPopulating(World world, int chunkX, int chunkZ) {
		if (!MGConfig.isDimensionHandled(world.provider.dimensionId)) {
			MineraGenesis.logger.debug("Skipping request for dimension ID %d: not enabled", world.provider.dimensionId);
			return;
		}
		
		SynchronizedChunkSet chunkData = POPULATED_CHUNKS.computeIfAbsent(world, w -> new SynchronizedChunkSet());
		chunkData.add(chunkX, chunkZ);
		
		world.theProfiler.startSection("mineragenesis-findingChunks");
		
		for (int dx = 0; dx <= 1; ++dx) {
			int x = chunkX + dx;
			for (int dz = 0; dz <= 1; ++dz) {
				int z = chunkZ + dz;
				if (isChunkReady(x, z, chunkData)) {
					MGQueues.queueImportRequest(world.provider.dimensionId, x, z);
				}
			}
		}

		world.theProfiler.endSection();
	}
	
	/**
	 * Determines whether the chunk specified by <code>chunkX</code> and <code>chunkZ</code> is ready to be processed by
	 * MineraGenesis.
	 * @param chunkX the X coordinate of the chunk to check
	 * @param chunkZ the Z coordinate of the chunk to check
	 * @param chunkData the chunk set to consult for chunk population state
	 * @return whether the chunk is fit for further processing
	 */
	private static boolean isChunkReady(int chunkX, int chunkZ, SynchronizedChunkSet chunkData) {
		for (int dx = -1; dx <= 0; ++dx) {
			int x = chunkX + dx;
			for (int dz = -1; dz <= 0; ++dz) {
				int z = chunkZ + dz;
				
				if (!chunkData.contains(x, z)) {
					return false;
				}
				
			}
		}
		
		return true;
	}
	
}
