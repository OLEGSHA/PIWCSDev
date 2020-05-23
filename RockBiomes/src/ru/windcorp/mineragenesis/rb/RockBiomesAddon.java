/*
 * MineraGenesis Rock Biomes Addon
 * Copyright (C) 2019  Javapony/OLEGSHA
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
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package ru.windcorp.mineragenesis.rb;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Random;

import ru.windcorp.mineragenesis.MineraGenesis;
import ru.windcorp.mineragenesis.addon.MineraGenesisAddonLoader;
import ru.windcorp.mineragenesis.rb.config.*;
import ru.windcorp.mineragenesis.rb.fields.SumField2D;
import ru.windcorp.mineragenesis.rb.fields.DiscreteFieldConstant;
import ru.windcorp.mineragenesis.rb.fields.Field2D;
import ru.windcorp.mineragenesis.rb.fields.Field2DConstant;
import ru.windcorp.mineragenesis.rb.fields.MaxNoiseDiscreteField2D;
import ru.windcorp.mineragenesis.rb.fields.NeighboringField2D;
import ru.windcorp.mineragenesis.rb.fields.Noise2D;
import ru.windcorp.mineragenesis.rb.gen.*;
import ru.windcorp.mineragenesis.request.ChunkData;

public class RockBiomesAddon {

	public static final boolean CAN_DEBUG = true;
	private static boolean isDebugging = false;

	@MineraGenesisAddonLoader(
		id = "rb",
		name = "Rock Biomes",
		version = "1.4.3" + (CAN_DEBUG ? "" : "_nodebug"),
		minimumMgApiVersion = 1
	)
	public static void loadHook() {
		RockBiomesCP chunkProcessor = new RockBiomesCP(loadConfig());
		MineraGenesis.setProcessor(chunkProcessor);
	}

	public static boolean isDebugging() {
		return isDebugging;
	}

	public static void setDebugging(boolean isDebugging) {
		if (!CAN_DEBUG)
			MineraGenesis.logger.log("Attempted to enable debugging but this build of RockBiomesAddon does not support debugging");
		
		RockBiomesAddon.isDebugging = isDebugging;
	}

	public static long computeSeed(String string) {
		try {
			return Long.parseLong(string);
		} catch (NumberFormatException e) {
			return string.hashCode();
		}
	}

	private static Random seedGenerator = null;
	private static Collection<Dimension> loadConfig() {
		try {
			ConfigReader reader = new ConfigReader(
					MineraGenesis.getHelper().getGlobalConfigurationDirectory().resolve("rockBiomes.cfg")
			);
			
			final Collection<Dimension> dimensions = new ArrayList<>();
			
			ConfigLoader loader = new ConfigLoader(reader,
				new VoidVerb("registerDimension") {
					@Override
					protected void runVoid(Arguments args) throws ConfigurationException {
						dimensions.add(args.get(null, Dimension.class));
					}
				},
				
				Verb.createBuilder(DimensionComplex.class, DimensionComplex::build),
				Verb.createBuilder(DimensionSimple.class, DimensionSimple::build),
				Verb.createBuilder(RockBiomeType.class, RockBiomeType::build),
				Verb.createBuilder(RockBiome.class, RockBiome::build),
				
				Verb.createBuilder(SimpleDeposit.class, SimpleDeposit::build),
				Verb.createBuilder(LayeredDeposit.class, LayeredDeposit::build),
				Verb.createBuilder(LayeredDeposit.Layer.class, LayeredDeposit.Layer::build),
				
				Verb.createBuilder("sum", SumField2D.class, SumField2D::build),
				Verb.createBuilder("Noise", Noise2D.class, Noise2D::build),
				Verb.createBuilder("Blobs", MaxNoiseDiscreteField2D.class, MaxNoiseDiscreteField2D::build),
				Verb.createBuilder("Blob", MaxNoiseDiscreteField2D.Value.class, MaxNoiseDiscreteField2D.Value::build),
				Verb.createBuilder("Neighboring", NeighboringField2D.class, NeighboringField2D::build),
				Verb.createBuilder("around", NeighboringField2D.class, NeighboringField2D::buildForDeposit),
				
				Verb.createBuilder("BlockOnly", BlockOnly.class, BlockOnly::build),
				Verb.createBuilder("DFOnly", DiscreteFieldConstant.class, DiscreteFieldConstant::build),
				Verb.createBuilder("Constant2D", Field2DConstant.class, Field2DConstant::build),
				Verb.createBuilder("BlockSet", BlockSet.class, BlockSet::build),
				Verb.createBuilder("BlockMix", BlockMix.class, BlockMix::build),
				
				new Verb<BlockPredicate>("AnyBlock", BlockPredicate.class) {
					@Override
					protected BlockPredicate runImpl(Arguments args) throws ConfigurationException {
						return mgid -> mgid != ChunkData.BEDROCK_MGID;
					}
				},
				new Verb<BlockPredicate>("AnyBlockExceptAir", BlockPredicate.class) {
					@Override
					protected BlockPredicate runImpl(Arguments args) throws ConfigurationException {
						return mgid -> mgid != ChunkData.AIR_MGID && mgid != ChunkData.BEDROCK_MGID;
					}
				},
				
				new Verb<Field2D>("mutate", Field2D.class) {
					@Override
					protected Field2D runImpl(Arguments args) throws ConfigurationException {
						if (seedGenerator == null) seedGenerator = new Random(0);
						return args.get(null, Field2D.class).clone(seedGenerator);
					};
				},
				
				new VoidVerb("masterSeed") {
					@Override
					protected void runVoid(Arguments args) throws ConfigurationException {
						seedGenerator = new Random(computeSeed(args.get(null, String.class)));
					}
				},
				
				new Verb<String>("getSeed", String.class) {
					@Override
					protected String runImpl(Arguments args) throws ConfigurationException {
						if (seedGenerator == null) seedGenerator = new Random(0);
						return Long.toString(seedGenerator.nextLong());
					}
				},
				
				new UBifier.Setter(),
				
				new VoidVerb("enableDebugging") {
					@Override
					protected void runVoid(Arguments args) throws ConfigurationException {
						setDebugging(true);
					}
				}
			);
			
			loader.load();
			
			return dimensions;
		} catch (IOException e) {
			MineraGenesis.crash(e, "Could not load RockBiomes configuration due to an IO error");
		} catch (ConfigurationException e) {
			MineraGenesis.crash(e, "Could not load RockBiomes configuration due to a configuration error");
		}
		
		return null;
	}
	
}
