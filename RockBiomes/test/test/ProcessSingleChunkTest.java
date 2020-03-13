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
package test;

import ru.windcorp.mineragenesis.MineraGenesis;
import ru.windcorp.mineragenesis.request.ChunkData;
import ru.windcorp.mineragenesis.request.ChunkLocator;
import ru.windcorp.mineragenesis.request.GenerationRequest;

/**
 * @author Javapony
 *
 */
public class ProcessSingleChunkTest {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		FakeLoader.load(args[0]);
		
		ChunkData cd = new ChunkData();
		
		for (int x = 0; x < ChunkData.CHUNK_SIZE; ++x) {
			for (int z = 0; z < ChunkData.CHUNK_SIZE; ++z) {
				for (int y = 0; y < ChunkData.CHUNK_HEIGHT; ++y) {
					cd.setBlock(x, z, y, (short) 0x10);
				}
			}
		}
		
		MineraGenesis.getProcessor().processChunk(new GenerationRequest(new ChunkLocator(0, 0, 0), cd));
		
		for (int x = 0; x < ChunkData.CHUNK_SIZE; ++x) {
			for (int z = 0; z < ChunkData.CHUNK_SIZE; ++z) {
				for (int y = 0; y < ChunkData.CHUNK_HEIGHT; ++y) {
					cd.setBlock(x, z, y, (short) 0x1000);
				}
				cd.setHeight(x, z, ChunkData.CHUNK_HEIGHT - 10);
			}
		}
		
		MineraGenesis.getProcessor().processChunk(new GenerationRequest(new ChunkLocator(0, +3, +3), cd));
	}

}
