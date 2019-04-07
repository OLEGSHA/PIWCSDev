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
package ru.windcorp.mineragenesis.piwcs.config;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Stack;

import ru.windcorp.mineragenesis.MineraGenesis;
import ru.windcorp.mineragenesis.noise.DiscreteNoise;
import ru.windcorp.mineragenesis.noise.FractalNoise;
import ru.windcorp.mineragenesis.noise.NoiseGenerator;
import ru.windcorp.mineragenesis.noise.SimplexNoiseGenerator;
import ru.windcorp.mineragenesis.piwcs.MGAPChunkProcessor;
import ru.windcorp.mineragenesis.piwcs.MGAddonPIWCS;
import ru.windcorp.mineragenesis.piwcs.gen.*;
import ru.windcorp.mineragenesis.request.ChunkData;

import static ru.windcorp.mineragenesis.MineraGenesis.logger;

public class MGAPConfig {

	public static MGAPChunkProcessor loadConfig() {
		Scanner scanner = null;
		try {
			scanner = new Scanner(MineraGenesis.getHelper().getGlobalConfigurationDirectory().resolve("piwcs.cfg"));
			
			return parseRoot(scanner);
		} catch (IOException e) {
			throw new RuntimeException(e);
		} catch (ConfigurationException e) {
			throw new RuntimeException(e);
		} finally {
			if (scanner != null) {
				scanner.close();
			}
		}
	}
	
	private static class ConfigScanner {
		private final Scanner parent;
		
		int commentaryLevel = 0;
		int tokenNumber = 0;
		String token = null;
		
		boolean tokenOutdated = true;
		
		ConfigScanner(Scanner parent) {
			this.parent = parent;
		}

		boolean hasNext() throws ConfigurationException {
			skipCommentary();
			if (!tokenOutdated) {
				return true;
			}
			return parent.hasNext();
		}
		
		String next() throws ConfigurationException {
			skipCommentary();
			tokenOutdated = true;
			tokenNumber++;
			return token;
		}
		
		private void skipCommentary() throws ConfigurationException {
			if (!tokenOutdated) {
				return;
			}
			
			if (!parent.hasNext()) {
				return;
			}
			
			token = parent.next();
			while (token.equals("/*")) {
				++commentaryLevel;
				token = parent.next();
				
				while (parent.hasNext() && commentaryLevel != 0) {
					if (token.equals("/*")) {
						++commentaryLevel;
					} else if (token.equals("*/")) {
						if (commentaryLevel != 0) {
							--commentaryLevel;
						} else {
							throw new ConfigurationException("No commentary to close");
						}
					}

					token = parent.next();
				}
			}
			
			tokenOutdated = false;
		}
	}

	private static MGAPChunkProcessor parseRoot(Scanner fileScanner) throws ConfigurationException {
		Stack<Object> stack = new Stack<>();
		Map<String, Object> variables = new HashMap<>();
		ConfigScanner scanner = new ConfigScanner(fileScanner);
		boolean debug = false;
		
		Collection<DiscreteNoiseEntry<RockBiomeType>> regoliths = new ArrayList<>();
		RockBiomeType bedrock = null;
		FractalNoise bedrockHeight = null;
		
		try {
			while (scanner.hasNext()) {
				String token = scanner.next();
				
				if (debug) {
					logger.logf("About to process master token %03d: \"%s\"", scanner.tokenNumber, scanner.token);
					logger.logf("\tVars: %s", variables.toString());
					logger.logf("\tStack: %d items (0 is head)", stack.size());
					int i = 0;
					for (Object obj : stack) {
						logger.logf("\t\t%02d %s", i, obj);
						++i;
					}
					logger.log("");
				}
				
				switch (token) {
				case "SimpliestDeposit":
					stack.push(readSimpliestDeposit(scanner, stack, variables));
					break;
				
				case "buildRBT":
					stack.push(readBiomeType(scanner, stack, variables));
					break;
				
				case "buildBiome":
					stack.push(readBiome(scanner, stack, variables));
					break;
				
				case "Noise":
					stack.push(readNoise(scanner, variables));
					break;
					
				case "Block":
					stack.push(BlockSpecificator.parse(getExtraToken(scanner)));
					break;
					
				case "BlockList":
					stack.push(readBlockList(scanner, variables));
					break;
					
				case "BlockWeightedList":
					stack.push(readBlockWeightedList(scanner, variables));
					break;
				
				case "intoDNE":
					stack.push(new DiscreteNoiseEntry<Object>(pop(stack, FractalNoise.class), pop(stack, Object.class)));
					break;
				
				case "set":
					variables.put(getExtraToken(scanner), pop(stack, Object.class));
					break;
				case "get":
					stack.push(variables.get(getExtraToken(scanner)));
					break;
					
				case "addRegolithDNE":
					regoliths.add(popDNE(stack, RockBiomeType.class));
					break;
				case "addRegolith":
					regoliths.add(new DiscreteNoiseEntry<RockBiomeType>(pop(stack, FractalNoise.class), pop(stack, RockBiomeType.class)));
					break;
				case "setBedrock":
					bedrock = pop(stack, RockBiomeType.class);
					break;
				case "setBedrockHeight":
					bedrockHeight = pop(stack, FractalNoise.class);
					break;
					
				case "debugParser":
					debug = true;
					logger.log("[MGAP Config Parser] Debugging enabled");
					logger.log("");
					break;
				case "debugGeneration":
					MGAddonPIWCS.setDebugging(true);
					logger.log("Generation debugging enabled. This will produce incomprehensible worlds!");
					break;
					
				default:
					throw new ConfigurationException("Unknown token");
				}
			}
		} catch (ConfigurationException e) {
			e.initDetails(variables, stack, scanner.token, scanner.tokenNumber);
			throw e;
		}
		
		if (bedrock == null) {
			throw new ConfigurationException("Bedrock has not been set");
		}
		
		if (bedrockHeight == null) {
			throw new ConfigurationException("Bedrock height has not been set");
		}
		
		if (regoliths.isEmpty()) {
			throw new ConfigurationException("No regoliths have been added");
		}
		
		if (scanner.commentaryLevel != 0) {
			logger.logf("%d commentary levels not closed. This may be a result of a problem", scanner.commentaryLevel);
		} else if (debug) {
			logger.log("All commentaries closed");
		}
		
		if (!stack.isEmpty()) {
			logger.debug("Stack still has %d elements: %s", stack.size(), stack.toString());
		} else if (debug) {
			logger.log("Stack empty");
		}
		
		if (debug) {
			logger.log("Configuration parsed");
		}
		
		return new MGAPChunkProcessor(bedrock, createNoise(regoliths, RockBiomeType.class), bedrockHeight);
	}

	private static SimpliestDeposit readSimpliestDeposit(ConfigScanner scanner, Stack<Object> stack,
			Map<String, Object> variables) throws ConfigurationException {
		
		if (stack.size() <= 3) {
			throw new ConfigurationException("Stack has " + stack.size() + " elements, 3 required");
		}
		
		FractalNoise heightNoise = pop(stack, FractalNoise.class);
		FractalNoise densityNoise = pop(stack, FractalNoise.class);
		BlockWeightedList blocks = pop(stack, BlockWeightedList.class);
		
		double unitThickness = Double.NaN;
		float unitWeight = 100.0f;
		
		loop:
		while (true) {
			if (!scanner.hasNext()) {
				throw new ConfigurationException("Unexpected end");
			}
			
			String token = scanner.next();
			switch (token) {
			case "build":
				break loop;
			case "unitThickness":
				unitThickness = parseDouble(scanner, variables);
				break;
			case "unitWeight":
				unitWeight = parseFloat(scanner, variables);
				break;
			default:
				throw new ConfigurationException("Unknown token");
			}
		}
		
		if (unitThickness == Double.NaN) {
			throw new ConfigurationException("No unit thickness given");
		}
		
		return new SimpliestDeposit(blocks, heightNoise, densityNoise, unitThickness, unitWeight);
	}

	private static RockBiomeType readBiomeType(ConfigScanner scanner, Stack<Object> stack, Map<String, Object> variables) throws ConfigurationException {
		int elements = parseInt(scanner, variables);
		
		if (stack.size() < elements * 2) {
			throw new ConfigurationException("Stack has " + stack.size() + " elements, " + elements * 2 + " requested");
		}
		
		RockBiome[] biomes = new RockBiome[elements];
		FractalNoise[] noises = new FractalNoise[elements];
		
		for (int i = 0; i < biomes.length; ++i) {
			noises[i] = pop(stack, FractalNoise.class);
			biomes[i] = pop(stack, RockBiome.class);
		}
		
		return new RockBiomeType(new DiscreteNoise<>(biomes, noises));
	}

	private static RockBiome readBiome(ConfigScanner scanner, Stack<Object> stack, Map<String, Object> variables) throws ConfigurationException {
		BlockList replaceables;
		BlockSpecificator rock;
		Deposit[] deposits;
		
		int elements = parseInt(scanner, variables);
		
		if (stack.size() < elements + 2) {
			throw new ConfigurationException("Stack has " + stack.size() + " elements, " + (elements + 2) + " requested");
		}
		
		deposits = new Deposit[elements];
		for (int i = 0; i < elements; ++i) {
			deposits[i] = pop(stack, Deposit.class);
		}
		
		replaceables = pop(stack, BlockList.class);
		rock = pop(stack, BlockSpecificator.class);
		
		return new RockBiome(replaceables, rock.getMGID(), deposits);
	}

	private static BlockWeightedList readBlockWeightedList(ConfigScanner scanner, Map<String, Object> variables) throws ConfigurationException {
		List<WeightedBlockSpecificator> blocks = new ArrayList<>();
		
		while (true) {
			if (!scanner.hasNext()) {
				throw new ConfigurationException("Unexpected end");
			}
			
			String token = scanner.next();
			if (token.equals("build")) {
				break;
			}
			
			if (token.startsWith("$")) {
				blocks.add(getVar(variables, token, WeightedBlockSpecificator.class));
			}
			
			blocks.add(WeightedBlockSpecificator.parse(token));
		}
		
		short[] mgids = new short[blocks.size()];
		float[] weights = new float[blocks.size()];
		
		for (int i = 0; i < mgids.length; ++i) {
			WeightedBlockSpecificator spec = blocks.get(i);
			mgids[i] = spec.getMGID();
			weights[i] = spec.weight;
		}
		
		return new BlockWeightedList(mgids, weights);
	}

	private static BlockList readBlockList(ConfigScanner scanner, Map<String, Object> variables) throws ConfigurationException {
		List<BlockSpecificator> blocks = new ArrayList<>();
		
		while (true) {
			if (!scanner.hasNext()) {
				throw new ConfigurationException("Unexpected end");
			}
			
			String token = scanner.next();
			if (token.equals("build")) {
				break;
			}
			
			if (token.startsWith("$")) {
				blocks.add(getVar(variables, token, BlockSpecificator.class));
			}
			
			blocks.add(BlockSpecificator.parse(token));
		}
		
		int[] ids = new int[blocks.size()];
		int[] metas = new int[blocks.size()];
		for (int i = 0; i < ids.length; ++i) {
			BlockSpecificator spec = blocks.get(i);
			ids[i] = spec.id;
			metas[i] = spec.meta;
		}
		
		return new BlockList(ids, metas);
	}

	private static FractalNoise readNoise(ConfigScanner scanner, Map<String, Object> variables) throws ConfigurationException {
		double normalizingBias = 0;
		double bias = 0;
		Long seed = null;
		
		List<Double> frequencies = new ArrayList<>();
		List<Double> amplitudes = new ArrayList<>();
		
		loop:
		while (true) {
			if (!scanner.hasNext()) {
				throw new ConfigurationException("Unexpected end");
			}
			
			String token = scanner.next();
			switch (token) {
			case "build":
				break loop;
			case "normalizingBias":
				normalizingBias = parseDouble(scanner, variables);
				break;
			case "bias":
				bias = parseDouble(scanner, variables);
				break;
			case "seed":
				seed = parseLong(scanner, variables);
				break;
			case "noise":
				frequencies.add(parseDouble(scanner, variables));
				amplitudes.add(parseDouble(scanner, variables));
				break;
			default:
				throw new ConfigurationException("Unknown token");
			}
		}
		
		if (frequencies.isEmpty()) {
			throw new ConfigurationException("No noises given");
		}
		
		if (seed == null) {
			throw new ConfigurationException("No seed given");
		}
		
		NoiseGenerator[] noises = new NoiseGenerator[frequencies.size()];
		double[] freqArray = new double[frequencies.size()];
		double[] ampArray = new double[amplitudes.size()];
		
		for (int i = 0; i < noises.length; ++i) {
			noises[i] = new SimplexNoiseGenerator(seed + i);
			freqArray[i] = frequencies.get(i);
			ampArray[i] = amplitudes.get(i);
		}
		
		return new FractalNoise(noises, freqArray, ampArray, bias, normalizingBias);
	}

	private static class DiscreteNoiseEntry<T> {
		final T entry;
		final FractalNoise noise;
		
		DiscreteNoiseEntry(FractalNoise noise, T entry) {
			this.entry = entry;
			this.noise = noise;
		}

		@Override
		public String toString() {
			return "DiscreteNoiseEntry [entry=" + entry + ", noise=" + noise + "]";
		}
		
	}
	
	private static class BlockSpecificator {
		final int id, meta;

		public BlockSpecificator(int id, int meta) {
			this.id = id;
			this.meta = meta;
		}
		
		short getMGID() {
			return ChunkData.getMGID(id, meta);
		}
		
		@Override
		public String toString() {
			return "BlockSpecificator [id=" + id + ", meta=" + meta + "]";
		}

		static BlockSpecificator parse(String declar) throws ConfigurationException {
			int meta, id;
			
			int splitterIndex = declar.indexOf('~');
			if (splitterIndex != -1) {
				String metaDeclar = declar.substring(splitterIndex + 1);
				declar = declar.substring(0, splitterIndex);
				if (metaDeclar.equals("*")) {
					meta = -1;
				} else {
					try {
						meta = Integer.parseInt(metaDeclar);
					} catch (NumberFormatException e) {
						throw new ConfigurationException("Could not parse meta (integer or *)", e);
					}
					
					if (meta < 0 || meta >= 16) {
						throw new ConfigurationException("Invalid meta " + meta + ", must be [0; 16)");
					}
				}
			} else {
				meta = 0;
			}
			
			try {
				id = Integer.parseInt(declar);
				if (id < 0) {
					throw new ConfigurationException("Invalid ID " + id + ", must be [0; +Inf)");
				}
			} catch (NumberFormatException e) {
				try {
					id = MineraGenesis.getBlockIdFromName(declar);
				} catch (Exception e1) {
					e1.addSuppressed(e);
					throw new ConfigurationException("Could not parse block ID " + declar, e1);
				}
			}
			
			return new BlockSpecificator(id, meta);
		}
	}
	
	private static class WeightedBlockSpecificator extends BlockSpecificator {
		
		final float weight;

		public WeightedBlockSpecificator(int id, int meta, float weight) {
			super(id, meta);
			this.weight = weight;
		}

		@Override
		public String toString() {
			return "WeightedBlockSpecificator [weight=" + weight + ", id=" + id + ", meta=" + meta + "]";
		}

		static WeightedBlockSpecificator parse(String declar) throws ConfigurationException {
			float weight;
			
			int splitterIndex = declar.indexOf('=');
			if (splitterIndex != -1) {
				try {
					weight = Float.parseFloat(declar.substring(0, splitterIndex));
					declar = declar.substring(splitterIndex + 1);
				} catch (NumberFormatException e) {
					throw new ConfigurationException("Could not parse weight", e);
				}
			} else {
				weight = 1;
			}
			
			BlockSpecificator spec = BlockSpecificator.parse(declar);
			return new WeightedBlockSpecificator(spec.id, spec.meta, weight);
		}
		
	}
	
	private static <T> DiscreteNoise<T> createNoise(Collection<DiscreteNoiseEntry<T>> entries, Class<T> clazz) {
		@SuppressWarnings("unchecked")
		T[] values = (T[]) Array.newInstance(clazz, entries.size());
		FractalNoise[] noises = new FractalNoise[entries.size()];
		
		int i = 0;
		for (DiscreteNoiseEntry<T> entry : entries) {
			values[i] = entry.entry;
			noises[i] = entry.noise;
			++i;
		}
		
		return new DiscreteNoise<>(values, noises);
	}

	private static <T> T pop(Stack<Object> stack, Class<T> clazz) throws ConfigurationException {
		if (stack.isEmpty()) {
			throw new ConfigurationException("Stack is empty");
		}
		Object obj = stack.pop();
		if (!clazz.isInstance(obj)) {
			throw new ConfigurationException(obj.getClass().getSimpleName() + " is not a " + clazz.getSimpleName());
		}
		return clazz.cast(obj);
	}
	
	@SuppressWarnings("unchecked")
	private static <T> DiscreteNoiseEntry<T> popDNE(Stack<Object> stack, Class<T> clazz) throws ConfigurationException {
		DiscreteNoiseEntry<?> result = pop(stack, DiscreteNoiseEntry.class);
		if (!clazz.isInstance(result.entry)) {
			throw new ConfigurationException("DiscreteNoiseEntry " + result.entry.getClass().getSimpleName() + " is not a " + clazz.getSimpleName());
		}
		return (DiscreteNoiseEntry<T>) result;
	}
	
	private static <T> T getVar(Map<String, Object> variables, String name, Class<T> clazz) throws ConfigurationException {
		Object obj = variables.get(name);
		
		if (obj == null) {
			throw new ConfigurationException("Variable " + name + " does not exist");
		}
		if (!clazz.isInstance(obj)) {
			throw new ConfigurationException("(var " + name + ") " + obj.getClass().getSimpleName() + " is not a " + clazz.getSimpleName());
		}
		
		return clazz.cast(obj);
	}
	
	private static String getExtraToken(ConfigScanner reader) throws ConfigurationException {
		if (!reader.hasNext()) {
			throw new ConfigurationException("Out of tokens");
		}
		return reader.next();
	}

	private static int parseInt(ConfigScanner scanner, Map<String, Object> variables) throws ConfigurationException {
		String token = getExtraToken(scanner);
		if (token.startsWith("$")) {
			return (int) getVar(variables, token.substring(1), Integer.class);
		}
		try {
			return Integer.parseInt(token);
		} catch (NumberFormatException e) {
			throw new ConfigurationException("Could not parse an integer", e);
		}
	}
	
	private static long parseLong(ConfigScanner scanner, Map<String, Object> variables) throws ConfigurationException {
		String token = getExtraToken(scanner);
		if (token.startsWith("$")) {
			return (long) getVar(variables, token.substring(1), Long.class);
		}
		try {
			return Long.parseLong(token);
		} catch (NumberFormatException e) {
			throw new ConfigurationException("Could not parse an integer (long)", e);
		}
	}
	
	private static double parseDouble(ConfigScanner scanner, Map<String, Object> variables) throws ConfigurationException {
		String token = getExtraToken(scanner);
		if (token.startsWith("$")) {
			return (double) getVar(variables, token.substring(1), Double.class);
		}
		try {
			return Double.parseDouble(token);
		} catch (NumberFormatException e) {
			throw new ConfigurationException("Could not parse a double", e);
		}
	}
	
	private static float parseFloat(ConfigScanner scanner, Map<String, Object> variables) throws ConfigurationException {
		String token = getExtraToken(scanner);
		if (token.startsWith("$")) {
			return (float) getVar(variables, token.substring(1), Float.class);
		}
		try {
			return Float.parseFloat(token);
		} catch (NumberFormatException e) {
			throw new ConfigurationException("Could not parse a float", e);
		}
	}
}
