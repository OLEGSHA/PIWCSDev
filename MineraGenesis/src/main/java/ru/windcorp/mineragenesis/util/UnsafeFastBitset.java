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
package ru.windcorp.mineragenesis.util;

public class UnsafeFastBitset {
	
	private final long[] data;
	
	public UnsafeFastBitset(int bits) {
		this.data = new long[bits / 8];
	}

	public void set(int index) {
		data[index >> 6] |= 1l << (index & 63); 
	}
	
	public void unset(int index) {
		data[index >> 6] &= ~(1l << (index & 63));
	}
	
	public boolean get(int index) {
		return (data[index >> 6] & (1l << (index & 63))) != 0;
	}
	
	public void flip(int index) {
		data[index >> 6] ^= 1l << (index & 63); 
	}
	
	public void set(int index, boolean bit) {
		if (bit) set(index);
		else unset(index);
	}
	
	public void copyData(long[] src) {
		if (src.length != data.length) {
			throw new IllegalArgumentException("src.length (" + src.length + ") != data.length (" + data.length + ")");
		}
		
		System.arraycopy(src, 0, data, 0, data.length);
	}
	
	@Override
	public String toString() {
		char[] result = new char[data.length * 9 - 1];
		
		int index = 0;
		for (int i = 0; i < result.length; ++i) {
			if (i % 9 == 8) {
				result[i] = ' ';
			} else {
				result[i] = get(index++) ? '1' : '0';
			}
		}
		
		return new String(result);
	}

}
