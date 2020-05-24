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

import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.gameevent.TickEvent.ServerTickEvent;
import net.minecraftforge.event.terraingen.PopulateChunkEvent;
import ru.windcorp.mineragenesis.MineraGenesis;

public class MineraGenesisForgeEventHandler {
	
	@SubscribeEvent
	public void onTick(ServerTickEvent event) {
		if (event.phase != TickEvent.Phase.END) {
			return;
		}

		MineraGenesisMod.getProfiler().startSection("mineragenesis-tick");
		MineraGenesis.actInServerThread();
		MineraGenesisMod.getProfiler().endSection();
	}

	@SubscribeEvent (priority = EventPriority.LOWEST)
	public void onChunkFinishedPopulating(PopulateChunkEvent.Post event) {
		ChunkFinder.onChunkFinishedPopulating(
				event.world,
				event.chunkX,
				event.chunkZ
		);
	}
	
	@SubscribeEvent
	public void onExternalRequest(MineraGenesisChunkProcessRequest event) {
		ChunkFinder.onChunkFinishedPopulating(
				event.world,
				event.chunkX,
				event.chunkZ
		);
	}
	
}
