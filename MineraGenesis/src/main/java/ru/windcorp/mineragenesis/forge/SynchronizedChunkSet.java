/* 
 * MineraGenesis Minecraft mod
 * Copyright (C) 2020  Javapony and contributors
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
package ru.windcorp.mineragenesis.forge;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.NoSuchElementException;

import gnu.trove.iterator.TLongIterator;
import gnu.trove.set.TLongSet;
import gnu.trove.set.hash.TLongHashSet;

public class SynchronizedChunkSet {
	private final TLongSet storage = new TLongHashSet();
	
	private static long getKey(int x, int z) {
		return ((x & 0xFFFF_FFFFl) << Integer.SIZE) | (z & 0xFFFF_FFFFl);
	}
	
	private static int getX(long key) { return (int) (key >>> Integer.SIZE); }
	private static int getZ(long key) { return (int) (key);                  }
	
	public synchronized void add(int x, int z) {
		storage.add(getKey(x, z));
	}
	
	public synchronized void remove(int x, int z) {
		storage.remove(getKey(x, z));
	}
	
	public synchronized boolean contains(int x, int z) {
		return storage.contains(getKey(x, z));
	}
	
	public synchronized int getSize() {
		return storage.size();
	}

	public static class ChunkCoordinateIterator {
		private long current;
		private boolean hasNext = true;
		private final TLongIterator iterator;
		
		public ChunkCoordinateIterator(TLongIterator iterator) {
			this.iterator = iterator;
			next(); // Initialize fields
		}

		public void next() {
			checkExhaustion();
			if (!iterator.hasNext()) {
				hasNext = false;
				return;
			}
			current = iterator.next();
		}
		
		public boolean hasNext() {
			return hasNext;
		}
		
		private void checkExhaustion() {
			if (!hasNext) {
				throw new NoSuchElementException();
			}
		}

		public int getX() {
			checkExhaustion();
			return SynchronizedChunkSet.getX(current);
		}
		
		public int getZ() {
			checkExhaustion();
			return SynchronizedChunkSet.getZ(current);
		}
	}
	
	public SynchronizedChunkSet.ChunkCoordinateIterator iterator() {
		return new ChunkCoordinateIterator(storage.iterator());
	}
	
	private static final byte[] FILE_MASK = "I hate population flags".getBytes(StandardCharsets.UTF_8);
	
	public synchronized void load(Path path) throws IOException {
		try (
				DataInputStream input = new DataInputStream(
						new BufferedInputStream(
								Files.newInputStream(path)
						)
				)
		) {
			byte[] readFileMask = new byte[FILE_MASK.length];
			input.read(readFileMask);
			if (!Arrays.equals(FILE_MASK, readFileMask)) {
				throw new IOException("Invalid file mask: expected "
						+ formatByteArray(FILE_MASK) + "; got "
						+ formatByteArray(readFileMask)
				);
			}
			
			int elements = input.readInt();
			
			for (int i = 0; i < elements; ++i) {
				int x = input.readInt();
				int z = input.readInt();
				
				add(x, z);
			}
		}
	}
	
	private static String formatByteArray(byte[] array) {
		StringBuilder sb = new StringBuilder();
		sb.append(array.length).append(" bytes: [");
		
		boolean isFirst = true;
		for (byte b : array) {
			if (isFirst) {
				isFirst = false;
			} else {
				sb.append(' ');
			}
			
			int byteAsInt = b & 0xFF;
			sb.append(Character.toUpperCase(Character.digit(byteAsInt >>  4, 0x10)));
			sb.append(Character.toUpperCase(Character.digit(byteAsInt & 0xF, 0x10)));
		}
		
		return sb.append(']').toString();
	}
	
	public synchronized void save(Path path) throws IOException {
		try (
				DataOutputStream output = new DataOutputStream(
						new BufferedOutputStream(
								Files.newOutputStream(path)
						)
				)
		) {
			output.write(FILE_MASK);
			
			output.writeInt(getSize());
			
			for (ChunkCoordinateIterator it = iterator(); it.hasNext(); it.next()) {
				output.writeInt(it.getX());
				output.writeInt(it.getZ());
			}
		}
	}
	
}