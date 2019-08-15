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
package ru.windcorp.mineragenesis.request;

import ru.windcorp.mineragenesis.MGQueues;
import ru.windcorp.mineragenesis.MineraGenesis;

public class GenerationRequest implements Runnable {
	
	private static short nextRequestId = 0;
	private final short requestId;
	
	private final byte generation;

	private final ChunkLocator chunk;
	
	private final ChunkData original;
	private final ChunkPatch patch = ChunkDataPool.getPatch();

	public GenerationRequest(ChunkLocator chunk, ChunkData original) {
		this.requestId = nextRequestId++;
		this.generation = MGQueues.getGeneration();
		this.chunk = chunk;
		this.original = original;
	}

	public short getRequestId() {
		return requestId;
	}

	public ChunkLocator getChunk() {
		return chunk;
	}

	public ChunkData getOriginal() {
		return original;
	}

	public ChunkPatch getPatch() {
		return patch;
	}

	@Override
	public void run() {
		try {
			if (generation != MGQueues.getGeneration()) {
				// We're stopping
				return;
			}
			
			try {
				MineraGenesis.getProcessor().processChunk(this);
			} catch (Exception e) {
				MineraGenesis.crash(e, "Could not process GenerationRequest " + this);
			}
			
			if (generation != MGQueues.getGeneration()) {
				// We're stopped
				return;
			}
			
			MGQueues.queueApplicationRequest(this);
		} catch (Exception e) {
			MineraGenesis.crash(e, "Could not run GenerationRequest " + this);
		}
	}

	@Override
	public String toString() {
		return "GenerationRequest [requestId=" + requestId + ", generation=" + generation + ", chunk=" + chunk + "]";
	}
	
}
