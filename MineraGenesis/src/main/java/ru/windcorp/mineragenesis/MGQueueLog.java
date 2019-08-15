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
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static java.nio.file.StandardOpenOption.APPEND;
import static java.nio.file.StandardOpenOption.CREATE;

import ru.windcorp.mineragenesis.request.ChunkLocator;

public class MGQueueLog {
	
	private static final byte[] HEADER = "MineraGenesis Queue Log File".getBytes(StandardCharsets.UTF_8);
	
	private static Set<ChunkLocator> pending;
	private static int junk;
	
	private static Path path;
	private static DataOutputStream output;
	
	public synchronized static void onWorldLoaded(Path path) {
		MGQueueLog.path = path;
		pending = Collections.synchronizedSet(new HashSet<>());
		junk = 0;
		
		MineraGenesis.logger.debug("Loading queue log file " + path);
		
		if (Files.exists(path)) {
			readExistingLog();
		} else {
			setupNewLog();
		}
	}
	
	private synchronized static void createStream(OpenOption... options) {
		try {
			if (output != null) {
				output.close();
			}
		} catch (IOException e) {
			crash(e, "Could not close queue log output stream (path %s)", path);
		}
		
		try {
			// Stream is not buffered to make sure data is written ASAP
			output = new DataOutputStream(Files.newOutputStream(path, options));
			output.write(HEADER);
		} catch (IOException e) {
			crash(e, "Could not create queue log (path %s)", path);
		}
	}
	
	private static void setupNewLog() {
		MineraGenesis.logger.debug("Queue log file not present");
		createStream(CREATE);
	}

	private static void readExistingLog() {
		try (DataInputStream input = new DataInputStream(new BufferedInputStream(Files.newInputStream(path)))) {
			readHeader(input);
			int entries = estimateEntries();
				
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
				crash(e, "Queue log file ended abruptly. This may lead to minor world corruption (a single chunk may not be loaded). Path: %s", path);
			}
			
			if (junk != 0) {
				MineraGenesis.logger.debug("Queue log file contains %d junk entries. Compacting", junk);
				compact(); // compact will create a stream
			} else {
				createStream(APPEND);
			}
			
			if (!pending.isEmpty()) {
				MineraGenesis.logger.debug("Queue log file contains %d valid entries. Queuing them", pending.size());
				for (ChunkLocator importRequest : pending) {
					MGQueues.queueImportRequest(importRequest);
				}
			}
		} catch (IOException e) {
			crash(e, "Could not read existing queue log file (path %s)", path);
		}
	}

	private static void readHeader(DataInputStream input) throws IOException {
		byte[] buffer = new byte[HEADER.length];
		input.read(buffer);
		
		if (!Arrays.equals(buffer, HEADER)) {
			throw new IOException(path + " is not a MineraGenesis queue log file. Loading cannot continue. Given header: "
					+ Arrays.toString(buffer) + ", expected header: " + Arrays.toString(HEADER));
		}
	}
	
	/*
	 * Overestimate in case of abrupt ends to trigger EOFException
	 */
	private static int estimateEntries() throws IOException {
		long fileSize = Files.size(path);
		fileSize -= HEADER.length;
		return (int) divideRoundingUp(fileSize, ChunkLocator.getWrittenSize());
	}
	
	private static long divideRoundingUp(long divident, long divisor) {
		return (divident + 1) / divisor - 1;
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
			crash(e, "Could not save queue log to %s", path);
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
				crash(e, "Could not write chunk %s to queue log %s", chunk, path);
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
				crash(e, "Could not remove chunk %s from queue log %s", chunk, path);
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

	private static void crash(Exception exception, String message, Object... args) {
		if (!pending.isEmpty()) {
			MineraGenesis.logger.logf("Dumping all pending chunks (%d total) (format: dimension, x, z):", pending.size());
			
			for (ChunkLocator chunk : pending) {
				MineraGenesis.logger.logf("    %+04d %+06d %+06d", chunk.dimension, chunk.chunkX, chunk.chunkZ);
			}
		}
		
		MineraGenesis.crash(exception, message, args);
	}

}
