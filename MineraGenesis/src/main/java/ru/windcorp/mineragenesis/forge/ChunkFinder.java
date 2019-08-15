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

import java.util.Arrays;

import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.chunk.storage.AnvilChunkLoader;
import net.minecraftforge.event.terraingen.PopulateChunkEvent;
import ru.windcorp.mineragenesis.MGConfig;
import ru.windcorp.mineragenesis.MGQueues;

class ChunkFinder {

	private static enum ChunkState {
		/**
		 * Cache flag for {@link #populatedFlagCache} that indicates that this chunk is not yet cached.
		 */
		UNKNOWN,
		
		/**
		 * Cache flag for {@link #populatedFlagCache} that indicates that this chunk is known to be populated already.
		 * This is known because chunk's {@link Chunk#isTerrainPopulated} flag has been actually examined.
		 */
		POPULATED_ACCORDING_TO_FLAG,
		
		/**
		 * Cache flag for {@link #populatedFlagCache} that indicates that this chunk exists but has not been populated yet.
		 */
		NOT_POPULATED,
		
		/**
		 * Cache flag for {@link #populatedFlagCache} that indicates that this chunk does not exist.
		 */
		DOES_NOT_EXIST,
		
		/**
		 * Cache flag for {@link #populatedFlagCache} that indicates that this chunk is known to be populated already.
		 * This is known because this is the subject of the {@link PopulateChunkEvent.Post} event. 
		 */
		POPULATED_ACCORDING_TO_EVENT;

		/**
		 * Cache holding information about chunks surrounding a given chunk. This cache is accessed by
		 * {@link #isChunkReady(World, AnvilChunkLoader, int, int, int, int)} and is reset by
		 * {@link #onChunkFinishedPopulating(net.minecraftforge.event.terraingen.PopulateChunkEvent.Post)}.
		 * Event subject chunk is always {@link ChunkState#POPULATED_ACCORDING_TO_EVENT POPULATED_ACCORDING_TO_EVENT}.
		 */
		static class Cache {
			
			private static final int RADIUS = 1;
			private static final int SIZE = 1 + 2 * RADIUS;
			
			final ChunkState[] data = new ChunkState[SIZE * SIZE];
			int offsetX, offsetZ;
			
			void init(int centerX, int centerZ) {
				Arrays.fill(data, UNKNOWN);
				this.offsetX = centerX - RADIUS;
				this.offsetZ = centerZ - RADIUS;
			}
			
			/*
			 * Event subject chunk is manually set to be POPULATED because its isTerrainPopulated flag is unreliable.
			 */
			ChunkState get(int x, int z) {
				x -= offsetX;
				z -= offsetZ;
				if (x == RADIUS && z == RADIUS) return ChunkState.POPULATED_ACCORDING_TO_EVENT;
				return data[x * SIZE + z];
			}
			
			void set(int x, int z, ChunkState state) {
				x -= offsetX;
				z -= offsetZ;
				if (x == RADIUS && z == RADIUS)
					throw new IllegalArgumentException("Cannot cache center chunk, requested new state " + state
							+ " (cache is " + this + ")");
				data[x * SIZE + z] = state;
			}
			
			@Override
			public String toString() {
				int centerX = offsetX + RADIUS;
				int centerZ = offsetZ + RADIUS;
				
				StringBuilder sb = new StringBuilder("ChunkState.Cache<RADIUS=" + RADIUS + ", SIZE=" + SIZE + ">[offsetX=");
				
				sb.append(offsetX)
				.append(", offsetZ=").append(offsetZ)
				.append(", center=(").append(centerX).append("; ").append(centerZ)
				.append("), data={");
				
				for (int x = centerX - RADIUS; x <= centerX + RADIUS; ++x) {
					for (int z = centerZ - RADIUS; z <= centerZ + RADIUS; ++z) {
						sb.append("(").append(x).append("; ").append(z).append(")=").append(get(x, z)).append(", ");
					}
				}
				
				sb.setLength(sb.length() - 2);
				
				return sb.append("}]").toString();
			}
			
		}
	}
	
	private static final ChunkState.Cache CHUNK_STATE_CACHE = new ChunkState.Cache();

	public static void onChunkFinishedPopulating(PopulateChunkEvent.Post event) {
		if (!MGConfig.isDimensionHandled(event.world.provider.dimensionId)) {
			return;
		}
		
		event.world.theProfiler.startSection("mineragenesis-findingChunks");
		
		AnvilChunkLoader anvilLoader =
				(AnvilChunkLoader) (
						((WorldServer) event.world)
						.theChunkProviderServer
						.currentChunkLoader
				);
		
		CHUNK_STATE_CACHE.init(event.chunkX, event.chunkZ);
		
		for (int dx = 0; dx <= 1; ++dx) {
			int x = event.chunkX + dx;
			for (int dz = 0; dz <= 1; ++dz) {
				int z = event.chunkZ + dz;
				if (isChunkReady(event.world, anvilLoader, x, z, CHUNK_STATE_CACHE)) {
					MGQueues.queueImportRequest(event.world.provider.dimensionId, x, z);
				}
			}
		}

		event.world.theProfiler.endSection();
	}

	static Exception stacktrace = null;
	static Thread thread = null;
	
	/**
	 * Determines whether the chunk specified by <code>chunkX</code> and <code>chunkZ</code> is ready to be processed by
	 * MineraGenesis.
	 * @param world the {@link World} object for queries
	 * @param anvilLoader the {@link IChunkProvider} to query for chunk existence
	 * @param chunkX the X coordinate of the chunk to check
	 * @param chunkZ the Z coordinate of the chunk to check
	 * @param cache the {@link ChunkState.Cache} to use
	 * @return whether the chunk is fit for further processing
	 */
	private static boolean isChunkReady(World world, AnvilChunkLoader anvilLoader, int chunkX, int chunkZ, ChunkState.Cache cache) {
		if (stacktrace == null) {
			stacktrace = new Exception();
			thread = Thread.currentThread();
		} else {
			stacktrace.printStackTrace();
			System.out.println(thread);
			System.out.println(Thread.currentThread());
			throw new IllegalStateException("Recursion", stacktrace);
		}
		
		try {
			
			IChunkProvider provider = world.getChunkProvider();
			
			for (int dx = -1; dx <= 0; ++dx) {
				int x = chunkX + dx;
				for (int dz = -1; dz <= 0; ++dz) {
					int z = chunkZ + dz;
					
					switch (cache.get(x, z)) {
					case POPULATED_ACCORDING_TO_FLAG:
					case POPULATED_ACCORDING_TO_EVENT:
						continue;
					case NOT_POPULATED:
					case DOES_NOT_EXIST:
						return false;
					case UNKNOWN:
						boolean chunkIsLoaded = provider.chunkExists(x, z);
						boolean chunkExists = chunkIsLoaded || anvilLoader.chunkExists(world, x, z);
						
						try {
							if (!chunkExists) {
								cache.set(x, z, ChunkState.DOES_NOT_EXIST);
								return false;
							} else if (!provider.provideChunk(x, z).isTerrainPopulated) {
								cache.set(x, z, ChunkState.NOT_POPULATED);
								return false;
							} else {
								cache.set(x, z, ChunkState.POPULATED_ACCORDING_TO_FLAG);
							}
						} catch (RuntimeException e) {
							System.out.println("chunkIsLoaded = " + chunkIsLoaded + "; chunkExists = " + chunkExists);
							System.out.println("(By the way, anvil says " + anvilLoader.chunkExists(world, x, z) + ")");
							throw e;
						}
					}
				}
			}
			
			return true;
			
		} finally {
			stacktrace = null;
			thread = null;
		}
	}
	
}
