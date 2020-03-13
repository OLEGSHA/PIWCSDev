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

import java.util.ArrayList;
import java.util.List;

import ru.windcorp.mineragenesis.rb.RockBiomesCP.Workspace;
import ru.windcorp.mineragenesis.rb.fields.Field2D;

/**
 * @author Javapony
 *
 */
public abstract class Deposit implements BlockSupplier {
	
	public static class Cache {
		public double columnDensity;
		public double baseHeight;
		public double blockDensity;
	}
	
	private static final List<Deposit> DEPOSITS = new ArrayList<>();
	
	public static Cache[] createDepositCaches() {
		Cache[] result = new Cache[DEPOSITS.size()];
		
		for (int i = 0; i < result.length; ++i) {
			result[i] = DEPOSITS.get(i).createCache();
		}
		
		return result;
	}
	
	private final String name;
	private final int id;
	
	private final Field2D columnDensity;
	private final Field2D baseHeight;
	private final double unitThickness;
	
	private final BlockPredicate replaceableBlocks;
	
	public Deposit(String name, Field2D density, Field2D baseHeight, double unitThickness, BlockPredicate replaceableBlocks) {
		this.name = name;
		this.columnDensity = density;
		this.baseHeight = baseHeight;
		this.unitThickness = unitThickness;
		this.replaceableBlocks = replaceableBlocks;
		
		synchronized (DEPOSITS) {
			this.id = DEPOSITS.size();
			DEPOSITS.add(this);
		}
	}
	
	@Override
	public void addBlocks(BlockData block, BlockCollector collector, Workspace w) {
		Cache cache = w.getDepositCache(this);
		if (cache.columnDensity > 0) {
			block.currentDensity = cache.blockDensity = getDensity(cache, block.yInt);
			
			if (block.currentDensity > 0 && replaceableBlocks.check(block.original)) {
				addBlocks0(block, collector, w);
			}
			
			block.currentDensity = Double.NaN;
		}
	}
	
	protected abstract void addBlocks0(BlockData data, BlockCollector collector, Workspace w);

	protected double getDensity(Cache cache, int y) {
		return cache.columnDensity - (1/unitThickness) * Math.abs(y - cache.baseHeight);
	}
	
	protected Cache createCache() {
		return new Cache();
	}
	
	public String getName() {
		return name;
	}
	
	public int getId() {
		return id;
	}
	
	public Field2D getColumnDensityField() {
		return columnDensity;
	}
	
	public Field2D getBaseHeightField() {
		return baseHeight;
	}
	
	public BlockPredicate getReplaceableBlocks() {
		return replaceableBlocks;
	}
	
	protected boolean cacheColumn(ColumnData data, Cache cache) {
		cache.columnDensity = getColumnDensity(data.xDouble, data.zDouble);
		if (cache.columnDensity > 0) {
			cache.baseHeight = baseHeight.get(data.xDouble, data.zDouble);
			return true;
		}
		return false;
	}
	
	public double getColumnDensity(double absX, double absZ) {
		return buldge(columnDensity.get(absX, absZ));
	}
	
	@Override
	public String toString() {
		return getClass().getSimpleName() + " " + name + " (ID " + id + ")";
	}
	
	private static final double
			BULDGE_A = 0.1,
			BULDGE_B = -BULDGE_A * (BULDGE_A + 1),
			BULDGE_C = BULDGE_A + 1;
	
	
	public static double buldge(double x) {
		if (x < 0) return x;
		return BULDGE_B / (BULDGE_A + x) + BULDGE_C;
	}

	@Override
	public int hashCode() {
		return id;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Deposit other = (Deposit) obj;
		if (id != other.id)
			return false;
		return true;
	}
	
	

}
