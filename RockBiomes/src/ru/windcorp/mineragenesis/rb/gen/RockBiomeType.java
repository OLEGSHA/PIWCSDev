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

import java.util.Collection;

import ru.windcorp.mineragenesis.rb.config.Arguments;
import ru.windcorp.mineragenesis.rb.config.ConfigurationException;
import ru.windcorp.mineragenesis.rb.fields.DiscreteField2D;

public class RockBiomeType implements DiscreteField2D<RockBiome> {
	
	private final String name;
	private final DiscreteField2D<RockBiome> biomes;
	
	public RockBiomeType(String name, DiscreteField2D<RockBiome> biomes) {
		this.name = name;
		this.biomes = biomes;
	}
	
	@SuppressWarnings("unchecked")
	public static RockBiomeType build(Arguments args) throws ConfigurationException {
		return new RockBiomeType(
				args.get("name", String.class),
				(DiscreteField2D<RockBiome>) args.get(null, DiscreteField2D.class));
	}
	
	@Override
	public RockBiome get(double chunkX, double chunkZ) {
		return biomes.get(chunkX, chunkZ);
	}
	
	/**
	 * @see ru.windcorp.mineragenesis.rb.fields.DiscreteField2D#getAll()
	 */
	@Override
	public Collection<RockBiome> getAll() {
		return biomes.getAll();
	}
	
	@Override
	public String toString() {
		return "RBT " + name;
	}
	
	/**
	 * @return the biomes
	 */
	public DiscreteField2D<RockBiome> getBiomes() {
		return biomes;
	}

}
