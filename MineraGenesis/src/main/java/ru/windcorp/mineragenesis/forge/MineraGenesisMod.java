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

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.event.FMLServerStoppingEvent;
import net.minecraft.profiler.Profiler;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.common.MinecraftForge;
import ru.windcorp.mineragenesis.MineraGenesis;

@Mod (
		modid = MineraGenesis.SAFE_NAME,
		name = MineraGenesis.DISPLAY_NAME,
		version = MineraGenesis.VERSION,
		
		// Server-only mod
		acceptableRemoteVersions = "*"
)

public class MineraGenesisMod {

	@Mod.EventHandler
	public void onPreInit(FMLPreInitializationEvent event) {
		Object eventHandler = new MineraGenesisForgeEventHandler();
		
		MinecraftForge.EVENT_BUS.register(eventHandler);
		FMLCommonHandler.instance().bus().register(eventHandler);
		
		ForgeImplementation impl = new ForgeImplementation(
				event.getModLog(),
				event.getModConfigurationDirectory().toPath()
		);
		
		MineraGenesis.setImplementation(impl);
	}
	
	@Mod.EventHandler
	public void onInit(FMLInitializationEvent event) {
		// Do nothing
	}
	
	@Mod.EventHandler
	public void onPostInit(FMLPostInitializationEvent event) {
		MineraGenesis.loadConfig();
		MineraGenesis.attemptEarlyAddonInit();
	}
	
	@Mod.EventHandler
	public void onPostWorldLoad(FMLServerStartingEvent event) {
		MineraGenesis.onServerStarted();
	}
	
	@Mod.EventHandler
	public void onWorldStopping(FMLServerStoppingEvent event) {
		MineraGenesis.onServerStopping();
	}
	
	public static Profiler getProfiler() {
		return MinecraftServer.getServer().theProfiler;
	}
	
}
