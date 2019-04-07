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

import java.lang.ref.SoftReference;
import java.util.LinkedList;
import java.util.Objects;
import java.util.Queue;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class Pool<T> {
	
	private final Queue<SoftReference<T>> available = new LinkedList<>();
	
	private final Supplier<T> generator;
	private final Consumer<T> resetter;
	
	public Pool(Supplier<T> generator, Consumer<T> resetter) {
		this.generator = generator;
		this.resetter = resetter;
	}

	public synchronized T get() {
		T result = null;
		
		while (result == null) {
			if (available.isEmpty()) {
				return generator.get();
			}
			
			result = available.poll().get();
		}
		
		return result;
	}
	
	public synchronized void release(T obj) {
		Objects.requireNonNull(obj);
		if (resetter != null) resetter.accept(obj);
		available.add(new SoftReference<T>(obj));
	}

}
