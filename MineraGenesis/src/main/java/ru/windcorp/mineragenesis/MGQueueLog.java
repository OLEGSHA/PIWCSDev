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

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import ru.windcorp.mineragenesis.request.ChunkLocator;

public class MGQueueLog {
	
	private static final byte[] HEADER = "MineraGenesis Queue Log File".getBytes(StandardCharsets.UTF_8);
	
	private static Set<ChunkLocator> pending;
	private static int junk;
	
	private static Path path;
	private static DataOutputStream output;
	
	public synchronized static void onWorldLoaded(Path path) {
		MGQueueLog.path = path;
		pending = new HashSet<>();
		junk = 0;
		
		MineraGenesis.logger.debug("Loading queue log file " + path);
		
		if (!Files.exists(path)) {
			MineraGenesis.logger.debug("Queue log file not present");
			try {
				output = new DataOutputStream(Files.newOutputStream(path));
				output.write(HEADER);
			} catch (IOException e) {
				handleUnrecoverableException(e);
			}
			return;
		}
		
		try {
			long size = Files.size(path);
			
			if (size < HEADER.length) {
				throw new IOException(path + " is not a MineraGenesis queue log file. Loading cannot continue. Given file length "
						+ size + ", expected header: " + Arrays.toString(HEADER));
			}

			try (DataInputStream input = new DataInputStream(new BufferedInputStream(Files.newInputStream(path)))) {
			
				byte[] fileHeader = new byte[HEADER.length];
				input.read(fileHeader);
				if (!Arrays.equals(fileHeader, HEADER)) {
					MineraGenesis.logger.log(path + " is not a MineraGenesis queue log file. Loading cannot continue");
					throw new IOException(path + " is not a MineraGenesis queue log file. Loading cannot continue. Given header: "
							+ Arrays.toString(fileHeader) + ", expected header: " + Arrays.toString(HEADER));
				}
				
				if (size > HEADER.length) {
					int entries = (int) ((size - HEADER.length + 1) / ChunkLocator.getWrittenSize() - 1); // Overestimate in case of abrupt ends to trigger EOFException
						
					try {
						MineraGenesis.logger.debug("About to read %d entries from queue log file", entries);
						for (int i = 0; i < entries; ++i) {
							ChunkLocator chunk = ChunkLocator.read(input);
							
							if (!pending.add(chunk)) {
								junk++;
								pending.remove(chunk);
							}
						}
					} catch (EOFException e) {
						MineraGenesis.logger.log("Queue log file ended abruptly. This may lead to minor world corruption (a single chunk may not be loaded)");
					}
					
					if (junk != 0) {
						MineraGenesis.logger.debug("Queue log file contains %d junk entries. Compacting", junk);
						compact();
					} else {
						output = new DataOutputStream(Files.newOutputStream(path, StandardOpenOption.APPEND));
						output.write(HEADER);
					}
					
					if (!pending.isEmpty()) {
						MineraGenesis.logger.debug("Queue log file contains %d valid entries. Queuing them", pending.size());
						for (ChunkLocator importRequest : pending) {
							MGQueues.queueImportRequest(importRequest);
						}
					}
				}
			
			}
			
		} catch (IOException e) {
			handleUnrecoverableException(e);
		}
	}
	
	public synchronized static void onWorldUnloading() {
		try {
			if (pending.isEmpty()) {
				MineraGenesis.logger.debug("Queue log is empty, deleting queue log file");
				output.close();
				Files.delete(path);
				return;
			}
			
			if (junk != 0) {
				MineraGenesis.logger.debug("Junk present in queue log file - compacting");
				compact();
			}
			
			output.close();
		} catch (IOException e) {
			handleUnrecoverableException(e);
		}
	}
	
	public static boolean add(ChunkLocator chunk) {
		if (!pending.add(chunk)) {
			MineraGenesis.logger.debug("%s has been added to queue log twice, is everything OK? Ignoring", chunk);
			return false;
		}

		synchronized (MGQueueLog.class) {
			try {
				write(chunk);
			} catch (IOException e) {
				handleUnrecoverableException(e);
			}
		}
		
		return true;
	}

	public static void remove(ChunkLocator chunk) {
		if (!pending.remove(chunk)) {
			MineraGenesis.logger.debug("%s has not been present queue log but its removal was requested, is everything OK? Ignoring", chunk);
			return;
		}
		
		synchronized (MGQueueLog.class) {
			junk++;
			
			try {
				if (shouldCompact()) {
					compact();
				} else {
					write(chunk);
				}
			} catch (IOException e) {
				handleUnrecoverableException(e);
			}
		}
	}
	
	private static void write(ChunkLocator chunk) throws IOException {
		chunk.write(output);
	}
	
	private static boolean shouldCompact() {
		return junk > MGConfig.getMaxQueueLogJunk() || (junk > 0 && pending.isEmpty());
	}

	private static void compact() throws IOException {
		if (output != null) {
			output.close();
		}
		
		output = new DataOutputStream(Files.newOutputStream(path));
		output.write(HEADER);
		junk = 0;
		
		for (ChunkLocator chunk : pending) {
			write(chunk);
		}
	}

	private static void handleUnrecoverableException(IOException e) {
		handleUnrecoverableException(e);
		MineraGenesis.logger.log("An IO error occurred. Chunk processing cannot continue");
		
		if (!pending.isEmpty()) {
			MineraGenesis.logger.logf("Dumping all pending chunks (%d total) (format: dimension, x, z):", pending.size());
			
			for (ChunkLocator chunk : pending) {
				MineraGenesis.logger.logf("    %+04d %+06d %+06d", chunk.dimension, chunk.chunkX, chunk.chunkZ);
			}
		}
		
		throw new RuntimeException("An IO error occurred. Chunk processing cannot continue. All pending chunks are dumped in log", e);
	}

}
