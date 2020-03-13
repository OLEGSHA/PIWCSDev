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
public class SimpleDeposit extends Deposit {
	
	private final BlockSupplier contents;
	private final double unitWeight;

	public SimpleDeposit(String name, Field2D density, Field2D baseHeight, double unitThickness,
			BlockSupplier contents, double unitWeight, BlockPredicate replaceableBlocks) {
		super(name, density, baseHeight, unitThickness, replaceableBlocks);
		this.contents = contents;
		this.unitWeight = unitWeight;
	}
	
	public static SimpleDeposit build(Arguments args) throws ConfigurationException {
		return new SimpleDeposit(
				args.get("name", String.class),
				args.get("density", Field2D.class),
				args.get("height", Field2D.class),
				args.get("unitThickness", Double.class),
				args.get("ore", BlockSupplier.class),
				args.get("unitWeight", Double.class),
				args.get("replace", BlockPredicate.class));
	}

	@Override
	protected void addBlocks0(BlockData data, BlockCollector collector, Workspace w) {
		collector.pushMultiplier(data.currentDensity * unitWeight);
		contents.addBlocks(data, collector, w);
		collector.popMultiplier();
	}

}
