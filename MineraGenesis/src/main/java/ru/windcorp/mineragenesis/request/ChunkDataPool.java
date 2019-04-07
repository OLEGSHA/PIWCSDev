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

import ru.windcorp.mineragenesis.util.Pool;

public class ChunkDataPool {
	
	private static final Pool<ChunkData> POOL_ORIGINAL = new Pool<>(() -> new ChunkData(), null);
	private static final Pool<ChunkPatch> POOL_PATCH = new Pool<>(() -> new ChunkPatch(), patch -> patch.clear());

	public static ChunkData getOriginal() {
		return POOL_ORIGINAL.get();
	}
	
	public static ChunkPatch getPatch() {
		return POOL_PATCH.get();
	}
	
	public static void releaseOriginal(ChunkData obj) {
		POOL_ORIGINAL.release(obj);
	}
	
	public static void releasePatch(ChunkPatch obj) {
		POOL_PATCH.release(obj);
	}
	
}
