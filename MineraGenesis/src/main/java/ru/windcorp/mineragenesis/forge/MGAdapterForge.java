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

import java.awt.image.BufferedImage;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import javax.imageio.ImageIO;

import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.gameevent.TickEvent.ServerTickEvent;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.chunk.NibbleArray;
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
								blockIdMSBArray = current.createBlockMSBArray();
							}
							
							blockIdMSBArray.set(x, y, z, id >> Byte.SIZE);
						}
						
						blockIdLSBArray[y << 8 | z << 4 | x] = (byte) id;
						
						blockMetadataArray.set(x, y, z, meta);
						
					}
				}
			}
			
			// ... Here.
			// Bottleneck (Block lookup *16*16*16)?
			current.removeInvalidBlocks();
			
		}
		
		return new ForgeApplicationRequest(chunk, result);
	}
	
	public static void importChunk(ChunkLocator chunkLocator, ChunkData output) {
		Chunk chunk = findChunk(chunkLocator);
		
		int[] heightMap = output.getHeightMap();
		System.arraycopy(chunk.heightMap, 0, heightMap, 0, chunk.heightMap.length);
		
		int topFilledSegment = chunk.getTopFilledSegment() >> 4; // This method actually returns the top segment's Y position
		ExtendedBlockStorage[] segments = chunk.getBlockStorageArray();
		
		/*
		 * We iterate over segments, then over y-layers in each segment, then over x, then over z.
		 * We skip blocks that are beyond heightmap values. We also stop if we get over heightmap
		 * maximum. Finally, we also compute MGIDs by hand to bypass Block lookups and stuff.
		 * 
		 * All craftsmanship is of the highest quality. No need to change anything here.
		 * I hope.
		 *
		 * May God have mercy on your soul if you still have to dig into this.
		 */
		
		int yAbs = 0;
		
		copyBlockData:
		for (int chunkSegment = 0; chunkSegment <= topFilledSegment; ++chunkSegment) {
			
			if (segments[chunkSegment] == null) {
				continue copyBlockData;
			}
			
			// Cache EVERYTHING
			byte[] blockIdLSBArray = segments[chunkSegment].getBlockLSBArray();
			NibbleArray blockIdMSBArray = segments[chunkSegment].getBlockMSBArray();
			NibbleArray blockMetadataArray = segments[chunkSegment].getMetadataArray();
			
			for (int y = 0; y < CHUNK_SEGMENT_SIZE; ++y, ++yAbs) {
				
				boolean blocksFoundOnLayer = false;
				
				for (int x = 0; x < ChunkData.CHUNK_SIZE; ++x) {
					blockLoop:
					for (int z = 0; z < ChunkData.CHUNK_SIZE; ++z) {
						
						if (heightMap[(z << 4) | x] <= yAbs) {
							// In ChunkData, block data that is beyond heightmap values is never used - no need to set it
							continue blockLoop;
						}
						
						blocksFoundOnLayer = true;
						
						output.setBlock(x, z, yAbs, (short) (
								
								// ID
								((
										
										// Most Significant Byte (blockIdMSBArray may be null)
										(blockIdMSBArray == null
												? 0
												: blockIdMSBArray.get(x, y, z) << Byte.SIZE)
										
										// Least Significant Byte
										| (blockIdLSBArray[(y << 8) | (z << 4) | (x)] & 0xFF)
										
								) << ChunkData.METADATA_SIZE)
								
								// Metadata
								| blockMetadataArray.get(x, y, z)
								
								));
						
					}
				}
				
				// Whoops! No blocks on this layer are in heightmap. We can stop
				if (!blocksFoundOnLayer) {
					break copyBlockData;
				}
			}
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
		
		chunk.setStorageArrays(request.getData());
		chunk.setChunkModified();
	}
	
	private static Chunk findChunk(ChunkLocator locator) {
		return DimensionManager.getWorld(locator.dimension)
				.getChunkFromChunkCoords(locator.chunkX, locator.chunkZ);
	}
	
	@SubscribeEvent
	public void onTick(ServerTickEvent event) {
		if (event.phase == TickEvent.Phase.START) {
			return;
		}
		
		MineraGenesis.actInServerThread();
		
		if (i == 200) {
			i = 0;
			tmp_save();
		} else {
			i++;
		}
	}

	int i = 0;
	BufferedImage img = new BufferedImage(1000, 1000, BufferedImage.TYPE_INT_RGB);
	private void tmp_save() {
		try {
			ImageIO.write(img, "png", new FileOutputStream("tmp.png"));
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@SubscribeEvent (priority = EventPriority.LOWEST)
	public void onChunkFinishedPopulating(PopulateChunkEvent.Post event) {
		for (int x = 0; x <= 1; ++x) {
			for (int z = 0; z <= 1; ++z) {
				img.setRGB(event.chunkX + x + 500, event.chunkZ + z + 500, 0xFFFFFF);
				if (isChunkReady(event.world, event.chunkX + x, event.chunkZ + z)) {
					MGQueues.queueImportRequest(event.world.provider.dimensionId, event.chunkX + x, event.chunkZ + z);
				}
			}
		}
	}

	private boolean isChunkReady(World world, int chunkX, int chunkZ) {
		IChunkProvider provider = world.getChunkProvider();
		for (int x = -1; x <= 0; ++x) {
			for (int z = -1; z <= 0; ++z) {
				if (!provider.chunkExists(chunkX + x, chunkZ + z)) {
					return false;
				}
				if (!provider.provideChunk(chunkX + x, chunkZ + z).isTerrainPopulated) {
					return false;
				}
			}
		}
		

		img.setRGB(chunkX + 500, chunkZ + 500, 0x44FF44);
		return true;
	}
	
	@Override
	public String toString() {
		return "MineraGenesisForgeAdapter";
	}

}