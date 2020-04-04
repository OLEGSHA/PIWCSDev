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

import org.apache.logging.log4j.Logger;

import net.minecraft.block.Block;
import net.minecraft.crash.CrashReport;
import net.minecraft.server.management.PlayerManager;
import net.minecraft.util.ReportedException;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.NibbleArray;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
import net.minecraftforge.common.DimensionManager;
import ru.windcorp.mineragenesis.MineraGenesis;
import ru.windcorp.mineragenesis.interfaces.MGImplementation;
import ru.windcorp.mineragenesis.request.ApplicationRequest;
import ru.windcorp.mineragenesis.request.ChunkData;
import ru.windcorp.mineragenesis.request.ChunkLocator;
import ru.windcorp.mineragenesis.request.ChunkPatch;

class ForgeImplementation implements MGImplementation {
	
	private static final int CHUNK_SEGMENTS = 16;
	private static final int CHUNK_SEGMENT_SIZE = ChunkData.CHUNK_HEIGHT / CHUNK_SEGMENTS;
	
	private final Logger forgeLogger;
	private final Path globalConfig;
	
	ForgeImplementation(Logger forgeLogger, Path globalConfig) {
		this.forgeLogger = forgeLogger;
		this.globalConfig = globalConfig.resolve("MineraGenesis");
		
		try {
			if (!Files.exists(this.globalConfig)) Files.createDirectory(this.globalConfig);
		} catch (IOException e) {
			MineraGenesis.crash(e, "Could not create global configuration directory %s", this.globalConfig);
		}
	}
	
	@Override
	public ApplicationRequest buildApplicationRequest(ChunkLocator chunk, ChunkPatch patch, ChunkData original) {
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
	
	@Override
	public void importChunkData(ChunkLocator chunkLocator, ChunkData output) {
		MineraGenesisMod.getProfiler().startSection("importing");
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
		
		MineraGenesisMod.getProfiler().endSection();
	}
	
	@Override
	public void exportChunkData(ApplicationRequest uncastRequest) {
		MineraGenesisMod.getProfiler().startSection("exporting");
		
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
		
		MineraGenesisMod.getProfiler().endSection();
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
	
	@Override
	public void crash(Throwable exception, String message) {
		CrashReport report = CrashReport.makeCrashReport(exception, "MineraGenesis has encountered an unrecoverable situation: " + message);
		throw new ReportedException(report);
	}

	@Override
	public void log(Object msg) {
		forgeLogger.info(msg);
	}
	
	@Override
	public void debug(String format, Object... args) {
		if (MineraGenesis.isDebugging) {
			forgeLogger.info(String.format(format, args));
		}
	}

	@Override
	public Path getGlobalConfigurationDirectory() {
		return globalConfig;
	}

	@Override
	public Path getWorldDataDirectory() {
		Path path = DimensionManager.getCurrentSaveRootDirectory().toPath().resolve(MineraGenesis.SAFE_NAME);
		if (!Files.exists(path)) {
			try {
				Files.createDirectories(path);
			} catch (IOException e) {
				MineraGenesis.crash(e, "Could not create world data directory %s", path);
			}
		}
		return path;
	}

	@Override
	public int getIdFromName(String name) {
		return Block.getIdFromBlock(Block.getBlockFromName(name));
	}

}
