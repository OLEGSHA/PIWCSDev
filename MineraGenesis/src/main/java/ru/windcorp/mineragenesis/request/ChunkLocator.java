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
package ru.windcorp.mineragenesis.request;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class ChunkLocator {
	
	public static final int WRITTEN_SIZE_IN_BYTES = Integer.BYTES * 3;
	
	public final int dimension;
	public final int chunkX, chunkZ;
	
	public ChunkLocator(int dimension, int chunkX, int chunkZ) {
		this.dimension = dimension;
		this.chunkX = chunkX;
		this.chunkZ = chunkZ;
	}
	
	public void write(DataOutputStream dataOutputStream) throws IOException {
		dataOutputStream.writeInt(dimension);
		dataOutputStream.writeInt(chunkX);
		dataOutputStream.writeInt(chunkZ);
	}

	public static int getWrittenSize() {
		return Integer.BYTES * 3;
	}
	
	public static ChunkLocator read(DataInputStream dataInputStream) throws IOException {
		return new ChunkLocator(dataInputStream.readInt(), dataInputStream.readInt(), dataInputStream.readInt());
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + chunkX;
		result = prime * result + chunkZ;
		result = prime * result + dimension;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ChunkLocator other = (ChunkLocator) obj;
		if (chunkX != other.chunkX)
			return false;
		if (chunkZ != other.chunkZ)
			return false;
		if (dimension != other.dimension)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "[dim=" + dimension + ", x=" + chunkX + ", z=" + chunkZ + "]";
	}

}
