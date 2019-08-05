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

import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.gameevent.TickEvent.ServerTickEvent;
import net.minecraft.server.management.PlayerManager;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.chunk.NibbleArray;
import net.minecraft.world.chunk.storage.AnvilChunkLoader;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.event.terraingen.PopulateChunkEvent;
import ru.windcorp.mineragenesis.MGQueues;
import ru.windcorp.mineragenesis.MineraGenesis;
import ru.windcorp.mineragenesis.request.ApplicationRequest;
import ru.windcorp.mineragenesis.request.ChunkData;
import ru.windcorp.mineragenesis.request.ChunkLocator;
import ru.windcorp.mineragenesis.request.ChunkPatch;

public class MGAdapterForge {
	
	private static final int CHUNK_SEGMENTS = 16;
	private static final int CHUNK_SEGMENT_SIZE = ChunkData.CHUNK_HEIGHT / CHUNK_SEGMENTS;
	
	public static ApplicationRequest buildApplicationRequest(ChunkLocator chunk, ChunkPatch patch, ChunkData original) {
		ExtendedBlockStorage[] result = new ExtendedBlockStorage[CHUNK_SEGMENTS];
		
		for (int i = 0, yAbs = 0; i < result.length; ++i) {
			
			ExtendedBlockStorage current = new ExtendedBlockStorage(i * CHUNK_SEGMENT_SIZE, false);
			result[i] = current;
			byte[] blockIdLSBArray = current.getBlockLSBArray();
			NibbleArray blockIdMSBArray = current.getBlockMSBArray();
			boolean shouldISetBlockIdMSBArray = false;
			NibbleArray blockMetadataArray = current.getMetadataArray();
			
			for (int y = 0; y < CHUNK_SEGMENT_SIZE; ++y, ++yAbs) {
				for (int x = 0; x < ChunkData.CHUNK_SIZE; ++x) {
					for (int z = 0; z < ChunkData.CHUNK_SIZE; ++z) {
						
						int id, meta;
						if (patch.hasBlock(x, z, yAbs)) {
							id = patch.getBlockId(x, z, yAbs);
							meta = patch.getBlockMetadata(x, z, yAbs);
						} else {
							id = original.getBlockId(x, z, yAbs);
							meta = original.getBlockMetadata(x, z, yAbs);
						}
	
						// Hackidy-hacky. Going to recalculate refCounts later...
						if (id > 0xFF) {
							if (blockIdMSBArray == null) {
								blockIdMSBArray = new NibbleArray(blockIdLSBArray.length, 4);
								shouldISetBlockIdMSBArray = true;
							}
							
							blockIdMSBArray.set(x, y, z, (id & 0xF00) >> Byte.SIZE);
						}
						
						blockIdLSBArray[y << 8 | z << 4 | x] = (byte) (id & 0x0FF);
						
						blockMetadataArray.set(x, y, z, meta);
						
					}
				}
			}
			
			if (shouldISetBlockIdMSBArray)
				current.setBlockMSBArray(blockIdMSBArray);
			
			// ... Here.
			// Bottleneck (Block lookup *16*16*16)?
			current.removeInvalidBlocks();
			
		}
		
		return new ForgeApplicationRequest(chunk, result);
	}
	
	public static void importChunk(ChunkLocator chunkLocator, ChunkData output) {
		Chunk chunk = findChunk(chunkLocator);
		
		int[] heightMap = output.getHeightMap();
		int topFilledSegment = chunk.getTopFilledSegment() >> 4; // This method actually returns the top segment's Y position
		ExtendedBlockStorage[] segments = chunk.getBlockStorageArray();
		
		/*
		 * We iterate over segments, then over y-layers in each segment, then over x, then over z.
		 * We stop if we get over heightmap maximum. Finally, we also compute MGIDs by hand to
		 * bypass Block lookups and stuff.
		 * 
		 * All craftsmanship is of the highest quality. No need to change anything here.
		 * I hope.
		 *
		 * May God have mercy on your soul if you still have to dig into this.
		 * 
		 * 20.07.2019: Nope! Fixin' mah heightmaps. MC's HMs are a lie. Trust no one.
		 */
		
		int yAbs = 0;
		boolean wasMSBPresent = false;
		
		copyBlockData:
		for (int chunkSegment = 0; chunkSegment <= topFilledSegment; ++chunkSegment) {
			
			if (segments[chunkSegment] == null) {
				yAbs += CHUNK_SEGMENT_SIZE;
				continue copyBlockData;
			}
			
			// Cache EVERYTHING
			byte[] blockIdLSBArray = segments[chunkSegment].getBlockLSBArray();
			NibbleArray blockIdMSBArray = segments[chunkSegment].getBlockMSBArray();
			NibbleArray blockMetadataArray = segments[chunkSegment].getMetadataArray();
			
			wasMSBPresent |= blockIdMSBArray != null;
			
			for (int y = 0; y < CHUNK_SEGMENT_SIZE; ++y, ++yAbs) {
				
				for (int x = 0; x < ChunkData.CHUNK_SIZE; ++x) {
					for (int z = 0; z < ChunkData.CHUNK_SIZE; ++z) {

						// Most Significant Byte (blockIdMSBArray may be null)
						int idMSB = (blockIdMSBArray == null) ? 0 : blockIdMSBArray.get(x, y, z);

						// Least Significant Byte
						int idLSB = blockIdLSBArray[y << 8 | z << 4 | x] & 0xFF;
						
						int id = idMSB << Byte.SIZE | idLSB;
						
						if (id != 0) { // If is not air
							heightMap[z << 4 | x] = yAbs;
						}

						int meta = blockMetadataArray.get(x, y, z);
						
						output.setBlock(x, z, yAbs, (short) (id << ChunkData.METADATA_SIZE | meta));
						
					}
				}
				
			}
		}
		
		if (!wasMSBPresent) {
			MineraGenesis.logger.logf("%s: no MSBs at all", chunkLocator);
		}
	}
	
	public static void exportChunk(ApplicationRequest uncastRequest) {
		ForgeApplicationRequest request = (ForgeApplicationRequest) uncastRequest;
		Chunk chunk = findChunk(request.getChunk());
		
		ExtendedBlockStorage[] previous = chunk.getBlockStorageArray();
		ExtendedBlockStorage[] generated = request.getData();
		
		for (int i = 0; i < previous.length; ++i) {
			if (previous[i] == null) {
				generated[i].setSkylightArray(new NibbleArray(generated[i].getBlockLSBArray().length, 4));
				continue;
			}
			
			generated[i].setBlocklightArray(previous[i].getBlocklightArray());
			generated[i].setSkylightArray(previous[i].getSkylightArray());
		}
		
		chunk.setStorageArrays(generated);
		chunk.setChunkModified();
		chunk.enqueueRelightChecks();
		markChunkForSending(chunk);
	}
	
	private static void markChunkForSending(Chunk chunk) {
		PlayerManager manager = ((WorldServer) chunk.worldObj).getPlayerManager();
		
		int startX = chunk.xPosition << 4, startZ = chunk.zPosition << 4;
		int updates = 0;
		
		for (int x = startX; x < startX + ChunkData.CHUNK_SIZE; ++x) {
			for (int z = startZ; z < startZ + ChunkData.CHUNK_SIZE; ++z) {
				for (int y = 0; y < ChunkData.CHUNK_HEIGHT; y += 8) {
					manager.markBlockForUpdate(x, y, z);
					if (++updates >= net.minecraftforge.common.ForgeModContainer.clumpingThreshold) {
						return;
					}
				}
			}
		}
	}

	private static Chunk findChunk(ChunkLocator locator) {
		return DimensionManager.getWorld(locator.dimension)
				.getChunkFromChunkCoords(locator.chunkX, locator.chunkZ);
	}
	
	@SubscribeEvent
	public void onTick(ServerTickEvent event) {
		if (event.phase != TickEvent.Phase.END) {
			return;
		}
		
		MineraGenesis.actInServerThread();
	}
	
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
					throw new IllegalArgumentException("Cannot cache center chunk, requested new state " + state);
				data[x * SIZE + z] = state;
			}
			
		}
	}
	
	private static final ChunkState.Cache CHUNK_STATE_CACHE = new ChunkState.Cache();

	@SubscribeEvent (priority = EventPriority.LOWEST)
	public void onChunkFinishedPopulating(PopulateChunkEvent.Post event) {
		AnvilChunkLoader anvilLoader =
				(AnvilChunkLoader) (
						((WorldServer) event.world)
						.theChunkProviderServer
						.currentChunkLoader
				);
		
		CHUNK_STATE_CACHE.init(event.chunkX, event.chunkZ);
		
		for (int x = 0; x <= 1; ++x) {
			for (int z = 0; z <= 1; ++z) {
				if (isChunkReady(event.world, anvilLoader, event.chunkX + x, event.chunkZ + z, CHUNK_STATE_CACHE)) {
					MGQueues.queueImportRequest(event.world.provider.dimensionId, event.chunkX + x, event.chunkZ + z);
				}
			}
		}
	}

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
	private boolean isChunkReady(World world, AnvilChunkLoader anvilLoader, int chunkX, int chunkZ, ChunkState.Cache cache) {
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
					if (!provider.chunkExists(x, z) && !anvilLoader.chunkExists(world, x, z)) {
						cache.set(x, z, ChunkState.DOES_NOT_EXIST);
						return false;
					}
					if (!provider.provideChunk(x, z).isTerrainPopulated) {
						cache.set(x, z, ChunkState.NOT_POPULATED);
						return false;
					}
					cache.set(x, z, ChunkState.POPULATED_ACCORDING_TO_FLAG);
				}
			}
		}
		
		return true;
	}
	
//	private boolean isChunkReady_Suspect(World world, AnvilChunkLoader anvilLoader, int chunkX, int chunkZ, ChunkState.Cache cache) {
//		IChunkProvider provider = world.getChunkProvider();
//		
//		MineraGenesis.logger.logf("  CHECK x=%d z=%d", chunkX, chunkZ);
//		
//		for (int dx = -1; dx <= 0; ++dx) {
//			int x = chunkX + dx;
//			for (int dz = -1; dz <= 0; ++dz) {
//				int z = chunkZ + dz;
//				
//				MineraGenesis.logger.logf("    EXAMINE x=%d z=%d: cached %s", x, z, cache.get(x, z));
//				
//				switch (cache.get(x, z)) {
//				case POPULATED_ACCORDING_TO_FLAG:
//				case POPULATED_ACCORDING_TO_EVENT:
//					MineraGenesis.logger.logf("    continue");
//					continue;
//				case NOT_POPULATED:
//				case DOES_NOT_EXIST:
//					MineraGenesis.logger.logf("  NOT FIT");
//					return false;
//				case UNKNOWN:
//					if (provider.chunkExists(x, z)) {
//						MineraGenesis.logger.logf("    exists according to PROVIDER");
//					} else if (anvilLoader.chunkExists(world, x, z)) {
//						MineraGenesis.logger.logf("    exists according to LOADER");
//					} else {
//						MineraGenesis.logger.logf("    DOES_NOT_EXIST");
//						cache.set(x, z, ChunkState.DOES_NOT_EXIST);
//						MineraGenesis.logger.logf("  NOT FIT");
//						return false;
//					}
//					
//					if (!provider.provideChunk(x, z).isTerrainPopulated) {
//						MineraGenesis.logger.logf("    NOT_POPULATED");
//						cache.set(x, z, ChunkState.NOT_POPULATED);
//						MineraGenesis.logger.logf("  NOT FIT");
//						return false;
//					}
//
//					MineraGenesis.logger.logf("    is populated, continue");
//					cache.set(x, z, ChunkState.POPULATED_ACCORDING_TO_FLAG);
//				}
//			}
//		}
//		
//		MineraGenesis.logger.logf("  YES FIT");
//		
//		return true;
//	}
	
	@Override
	public String toString() {
		return "MineraGenesisForgeAdapter";
	}

}
