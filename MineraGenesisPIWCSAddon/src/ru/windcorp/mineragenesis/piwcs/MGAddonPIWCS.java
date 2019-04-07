/* 
 * PIWCS addon for MineraGenesis Minecraft mod
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
package ru.windcorp.mineragenesis.piwcs;

import ru.windcorp.mineragenesis.MineraGenesis;
import ru.windcorp.mineragenesis.addon.MineraGenesisAddonLoader;
import ru.windcorp.mineragenesis.noise.DiscreteNoise;
import ru.windcorp.mineragenesis.noise.FractalNoise;
import ru.windcorp.mineragenesis.piwcs.config.MGAPConfig;
import ru.windcorp.mineragenesis.piwcs.gen.*;

import static ru.windcorp.mineragenesis.request.ChunkData.getMGID;

@SuppressWarnings("unused")
public class MGAddonPIWCS {
	
	private static boolean isDebugging = false;
	
	@MineraGenesisAddonLoader(
		id = "piwcs",
		name = "PIWCS",
		version = "1.0",
		minimumMgApiVersion = 0
	)
	public static void load() {
		MineraGenesis.setProcessor(MGAPConfig.loadConfig());
		
//		BlockList replaceDefault = new BlockList(
//				1, 0, // Stone
//				14, 0, // Gold
//				15, 0, // Iron
//				16, 0, // Coal
//				13, 0, // Gravel
//				56, 0, // Diamond
//				73, 0, 74, 0, // Redstone
//				21, 0, // Lapis
//				129, 0 // Emerald
//		);
//		
//		RockBiomeType bedrock = new RockBiomeType(new DiscreteNoise<>(
//				new RockBiome[] {
//						
//						// Obsidian
//						new RockBiome(
//								replaceDefault,
//								getMGID(49, 0), // Obsidian 49
//								new Deposit[] {
//										new SimpliestDeposit( // Diamonds
//												new BlockWeightedList(
//														56, 0, 1,
//														10, 0, 0.02f
//												),
//												new FractalNoise(1, 8, 0,    0.02, 4, 0.04, 2),
//												new FractalNoise(2, -2, 1,    0.001, 2, 0.002, 1.8, 0.004, 1.3, 0.008, 1),
//												8, 100
//										)
//								}
//						),
//						
//						// Netherstone
//						new RockBiome(
//								replaceDefault,
//								getMGID(87, 0), // Netherstone 87
//								new Deposit[] {
//										new SimpliestDeposit( // Quartz
//												new BlockWeightedList(
//														153, 0, 1,
//														155, 0, 0.2f
//												),
//												new FractalNoise(3, 8, 0,    0.02, 4, 0.04, 2),
//												new FractalNoise(4, -2, 1,    0.001, 2, 0.002, 1.8, 0.004, 1.3, 0.008, 1),
//												40, 100
//										),
//										new SimpliestDeposit( // Soulsand
//												new BlockWeightedList(
//														88, 0, 1
//												),
//												new FractalNoise(5, 8, 0,    0.02, 4, 0.04, 2),
//												new FractalNoise(6, -2, 1,    0.001, 2, 0.002, 1.8, 0.004, 1.3, 0.008, 1),
//												40, 2
//										)
//								}
//						)
//						
//				},
//				new FractalNoise[] {
//						new FractalNoise(10, 0, 0,    0.01, 2, 0.08, 0.2),
//						new FractalNoise(11, 0, 0,    0.01, 2, 0.08, 0.2)
//				}
//		));
//		
//		MineraGenesis.setProcessor(new MGAPChunkProcessor(
//				
//				bedrock,
//				new DiscreteNoise<>(
//						new RockBiomeType[] {
//								bedrock,
//								
//								new RockBiomeType(new DiscreteNoise<>(
//										new RockBiome[] {
//												
//												// Stone
//												new RockBiome(
//														replaceDefault,
//														getMGID(1, 0), // Stone 1
//														new Deposit[] {
//																new SimpliestDeposit( // Iron
//																		new BlockWeightedList(
//																				15, 0, 1,
//																				14, 0, 0.02f
//																		),
//																		new FractalNoise(78, 64, 0,    0.02, 4, 0.04, 2),
//																		new FractalNoise(98, -2, 1,    0.001, 2, 0.002, 1.8, 0.004, 1.3, 0.008, 1),
//																		40, 100
//																)
//														}
//												),
//												
//												// Sandstone
//												new RockBiome(
//														replaceDefault,
//														getMGID(24, 0), // Sandstone 24
//														new Deposit[] {
//																new SimpliestDeposit( // Coal
//																		new BlockWeightedList(
//																				16, 0, 1
//																		),
//																		new FractalNoise(24, 64, 0,    0.02, 4, 0.04, 2),
//																		new FractalNoise(28, -2, 1,    0.001, 2, 0.002, 1.8, 0.004, 1.3, 0.008, 1),
//																		60, 1
//																),
//																new SimpliestDeposit( // Sand
//																		new BlockWeightedList(
//																				12, 0, 1
//																		),
//																		new FractalNoise(108, 64, 0,    0.02, 4, 0.04, 2),
//																		new FractalNoise(256, -2, 1,    0.01, 2, 0.02, 1.8, 0.04, 1.3, 0.08, 1),
//																		5, 100
//																)
//														}
//												)
//												
//										},
//										new FractalNoise[] {
//												new FractalNoise(13, 0, 0,    0.01, 2, 0.08, 0.2),
//												new FractalNoise(14, 0, 0,    0.01, 2, 0.08, 0.2)
//										}
//								))
//						},
//						new FractalNoise[] {
//								new FractalNoise(198, 0, 0,    0.001, 2, 0.008, 0.2),
//								new FractalNoise(13313, 0, 0,    0.001, 2, 0.008, 0.2)
//						}
//				),
//				
//				new FractalNoise(6656, 15, 0, 0.01, 4, 0.02, 2, 0.04, 1)
//				
//				));
	}

	public static boolean isDebugging() {
		return isDebugging;
	}

	public static void setDebugging(boolean isDebugging) {
		MGAddonPIWCS.isDebugging = isDebugging;
	}

}
