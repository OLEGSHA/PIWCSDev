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
import java.util.Random;

import gnu.trove.impl.sync.TSynchronizedIntObjectMap;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import net.minecraft.world.World;
import ru.windcorp.mineragenesis.MGConfig;
import ru.windcorp.mineragenesis.MGQueues;
import ru.windcorp.mineragenesis.MineraGenesis;

class ChunkFinder {

	private static final TIntObjectMap<SynchronizedChunkSet> POPULATED_CHUNKS =
			new TSynchronizedIntObjectMap<>(new TIntObjectHashMap<>());
	
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
		SynchronizedChunkSet result = new SynchronizedChunkSet();
		
		try {
			result.load(path);
		} catch (IOException e) {
			MineraGenesis.crash(e, "Could not load populated chunk cache from %s", path);
		}

		MineraGenesis.logger.debug("Loaded populated chunk cache for dimension %d. Cache contains %d entries",
				dimension, result.getSize()
		);
		
		POPULATED_CHUNKS.put(dimension, result);
	}
	
	public static void unload() {
		makeSurePopulatedChunksDirectoryExists();
		
		POPULATED_CHUNKS.forEachEntry((dimension, set) -> {
			
			Path path = getPopulatedChunksDirectory().resolve(
					"populated-chunks_" + dimension + ".mgpc"
			);
			
			try {
				set.save(path);
			} catch (IOException e) {
				
				try {
					Path backupPath = path.resolveSibling(
							String.format("%s_%08d_%tF_%tH-%tM-%tS",
									path.getFileName().toString(),
									(new Random()).nextInt(1_0000_0000),
									System.currentTimeMillis()
							)
					);
					
					set.save(backupPath);
					
					MineraGenesis.logger.logf(
							"Could not save populated chunk cache for dimension %d into %s, saved into %s instead",
							dimension, path, backupPath
					);
				} catch (IOException e1) {
					
					e1.addSuppressed(e);
				
					MineraGenesis.crash(e1, "Could not save populated chunk cache for dimension %d into %s",
							dimension, path
					);
				
				}
			}
			
			return true;
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
		
		SynchronizedChunkSet chunkData = getPopulatedChunksFor(world);
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
	 * Returns a {@link SynchronizedChunkSet} that describes the provided world. An empty one is created if none existed.
	 * @param world the world to look up
	 * @return the requested {@link SynchronizedChunkSet}.
	 */
	private static SynchronizedChunkSet getPopulatedChunksFor(World world) {
		SynchronizedChunkSet result = POPULATED_CHUNKS.get(world.provider.dimensionId);
		
		if (result == null) {
			result = new SynchronizedChunkSet();
			POPULATED_CHUNKS.put(world.provider.dimensionId, result);
		}
		
		return result;
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
