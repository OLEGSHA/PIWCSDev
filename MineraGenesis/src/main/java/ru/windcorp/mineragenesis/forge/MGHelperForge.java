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

import net.minecraftforge.common.DimensionManager;
import ru.windcorp.mineragenesis.MineraGenesis;
import ru.windcorp.mineragenesis.interfaces.MGHelper;

public class MGHelperForge implements MGHelper {
	
	private final Path globalConfig;

	public MGHelperForge(Path globalConfig) {
		this.globalConfig = globalConfig.resolve("MineraGenesis");
		try {
			if (!Files.exists(this.globalConfig)) Files.createDirectory(this.globalConfig);
		} catch (IOException e) {
			System.out.println("Could not create config directory " + this.globalConfig);
			e.printStackTrace();
			throw new RuntimeException("Could not create config directory");
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
				System.out.println("Could not create world data directory " + this.globalConfig);
				e.printStackTrace();
				throw new RuntimeException("Could not create world data directory");
			}
		}
		return path;
	}

}
