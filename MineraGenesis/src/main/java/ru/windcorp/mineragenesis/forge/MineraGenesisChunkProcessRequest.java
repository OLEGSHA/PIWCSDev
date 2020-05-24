/* 
 * MineraGenesis Minecraft mod
 * Copyright (C) 2020  Javapony and contributors
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

import cpw.mods.fml.common.eventhandler.Event;
import net.minecraft.world.World;

/**
 * @author Javapony
 */
public class MineraGenesisChunkProcessRequest extends Event {
	
	public final World world;
	public final int chunkX;
	public final int chunkZ;
	
	public MineraGenesisChunkProcessRequest(World world, int chunkX, int chunkZ) {
		this.world = world;
		this.chunkX = chunkX;
		this.chunkZ = chunkZ;
	}

}
