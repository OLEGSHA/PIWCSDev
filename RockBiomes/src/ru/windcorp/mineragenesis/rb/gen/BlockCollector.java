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
import ru.windcorp.mineragenesis.request.ChunkData;

public class BlockCollector {
	
	private static final int ARRAY_SIZE = 16;
	
	private final short[] blocks = new short[ARRAY_SIZE];
	private final BlockSupplier[] suppliers = new BlockSupplier[ARRAY_SIZE];
	private final double[] weights = new double[ARRAY_SIZE];
	private int elements = 0;
	
	private float totalWeight = 0.0f;
	
	private BlockSupplier currentSupplier = null;
	
	private boolean hasDepleted = false;
	
	private final double[] multiplierStack = new double[ARRAY_SIZE]; {
		multiplierStack[0] = 1.0f;
	}
	private int lastMultiplier = 0;
	
	public void addBlock(short mgId, double weight) {
		if (weight < 0)
			throw new IllegalArgumentException("Cannot add MGID " + ChunkData.getId(mgId) + ":" + ChunkData.getMeta(mgId) +
					" with weight " + weight + ": weight not positive");
			
		blocks[elements] = mgId;
		suppliers[elements] = null;
		totalWeight += weights[elements++] = weight * multiplierStack[lastMultiplier];
	}
	
	public void addBlockSupplier(BlockSupplier supplier, double weight) {
		if (weight < 0)
			throw new IllegalArgumentException("Cannot add BlockSupplier " + supplier +
					" with weight " + weight + ": weight not positive");
		
		if (currentSupplier != null && currentSupplier.equals(supplier))
			throw new IllegalArgumentException("Cannot add BlockSupplier " + supplier +
					" with weight " + weight + ": recursion detected, current supplier is " + currentSupplier);
		
		suppliers[elements] = supplier;
		totalWeight += weights[elements++] = weight * multiplierStack[lastMultiplier];
	}
	
	public void reset() {
		elements = 0;
		lastMultiplier = 0;
		totalWeight = 0.0f;
		currentSupplier = null;
		hasDepleted = false;
	}
	
	public boolean isEmpty() {
		return elements == 0;
	}
	
	public short get(BlockData block, Workspace w) {
		double random = w.getRandom().nextDouble();
		return get(block, w, random);
	}
	
	protected short get(BlockData block, Workspace w, double random) {
		if (isEmpty()) {
			hasDepleted = true;
			return (short) 0xDEAD;
		}
		
		double value = random * totalWeight;
		
		int i;
		for (i = 0; i < elements - 1; ++i) {
			if (value <= weights[i]) break;
			value -= weights[i];
		}
		
		BlockSupplier supplier = suppliers[i];
		if (supplier == null) {
			return blocks[i];
		} else {
			reset();
			currentSupplier = supplier;
			supplier.addBlocks(block, this, w);
			return get(block, w, random);
		}
	}
	
	/**
	 * @return <code>true</code> when the last {@link #get(BlockData, Workspace)}
	 * invocation determined that no block are applicable. This can happen when a BlockSupplier does not add any blocks.
	 */
	public boolean hasDepleted() {
		return hasDepleted;
	}
	
	public void pushMultiplier(double multiplier) {
		multiplierStack[lastMultiplier + 1] = multiplierStack[lastMultiplier] * multiplier;
		lastMultiplier++;
	}
	
	public void popMultiplier() {
		lastMultiplier--;
	}
	
	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("BlockCollector[");
		
		for (int i = 0; i < elements; ++i) {
			sb.append('(');
			
			if (suppliers[i] == null) {
				sb.append("MGID; ").append(ChunkData.getId(blocks[i])).append(':').append(ChunkData.getMeta(blocks[i]));
			} else {
				sb.append("BS; ").append(suppliers[i]);
			}
			
			sb.append("; ").append(weights[i]).append(')');
		}
		
		return sb.append(']').toString();
	}

}
