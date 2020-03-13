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
package ru.windcorp.mineragenesis.rb.gen;

import ru.windcorp.mineragenesis.rb.RockBiomesCP.Workspace;
import ru.windcorp.mineragenesis.rb.config.Arguments;
import ru.windcorp.mineragenesis.rb.config.ConfigurationException;
import ru.windcorp.mineragenesis.rb.fields.Field2D;

/**
 * @author Javapony
 *
 */
public class LayeredDeposit extends Deposit {
	
	public static class Layer implements BlockSupplier {
		private final BlockSupplier contents;
		
		private final double densityMin;
		private final double densityFullMin;
		
		private final double densityMax;
		private final double densityFullMax;
		
		private final double densityEdgeWidth;
		private final double unitWeight;
		
		public Layer(BlockSupplier contents, double densityMin, double densityMax, double densityEdgeWidth,
				double unitWeight) {
			this.contents = contents;
			
			this.densityMin = densityMin;
			this.densityMax = densityMax;
			this.densityEdgeWidth = densityEdgeWidth;
			this.unitWeight = unitWeight;
			
			this.densityFullMin = densityMin + densityEdgeWidth;
			this.densityFullMax = densityMax - densityEdgeWidth;
		}

		public double getWeightAt(double density) {
			if (density < densityMin)
				return 0;
			
			if (density < densityFullMin)
				return (density - densityMin) / (densityEdgeWidth) * unitWeight;
			
			if (density < densityFullMax)
				return unitWeight;
			
			if (density < densityMax)
				return (1 - (density - densityFullMax) / (densityEdgeWidth)) * unitWeight;
			
			return 0;
		}
		
		@Override
		public void addBlocks(BlockData block, BlockCollector collector, Workspace w) {
			double weight = getWeightAt(block.currentDensity);
			
			if (weight > 0) {
				collector.pushMultiplier(weight);
				contents.addBlocks(block, collector, w);
				collector.popMultiplier();
			}
		}
		
		public static Layer build(Arguments args) throws ConfigurationException {
			return new Layer(
					args.get(null, BlockSupplier.class),
					args.get("min", Double.class, Double.NEGATIVE_INFINITY),
					args.get("max", Double.class, Double.POSITIVE_INFINITY),
					args.get("edge", Double.class, 0.0),
					args.get("unitWeight", Double.class, 1.0)
			);
		}
	}
	
	private final Layer[] layers;
	private final double unitWeight;
	
	public LayeredDeposit(String name, Field2D density, Field2D baseHeight, double unitThickness,
			BlockPredicate replaceableBlocks, double unitWeight, Layer[] layers) {
		super(name, density, baseHeight, unitThickness, replaceableBlocks);
		this.layers = layers;
		this.unitWeight = unitWeight;
	}

	@Override
	protected void addBlocks0(BlockData block, BlockCollector collector, Workspace w) {
		collector.addBlockSupplier(this::addBlocks1, block.currentDensity * unitWeight);
	}
	
	protected void addBlocks1(BlockData block, BlockCollector collector, Workspace w) {
		block.currentDensity = w.getDepositCache(this).blockDensity;
		
		for (Layer layer : layers) {
			layer.addBlocks(block, collector, w);
		}
		
		block.currentDensity = Double.NaN;
	}
	
	public static LayeredDeposit build(Arguments args) throws ConfigurationException {
		return new LayeredDeposit(
				args.get("name", String.class),
				args.get("density", Field2D.class),
				args.get("height", Field2D.class),
				args.get("unitThickness", Double.class),
				args.get("replace", BlockPredicate.class),
				args.get("unitWeight", Double.class),
				args.get("layers", Layer[].class)
		);
	}

}
