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
package ru.windcorp.mineragenesis.rb.config;

public abstract class Verb<T> {
	
	@FunctionalInterface
	public static interface Builder<T> {
		T build(Arguments args) throws ConfigurationException;
	}
	
	public static <T> Verb<T> createBuilder(Class<T> clazz, Builder<T> builder) {
		return createBuilder(clazz.getSimpleName(), clazz, builder);
	}
	
	public static <T> Verb<T> createBuilder(String name, Class<T> clazz, Builder<T> builder) {
		return new Verb<T>(name, clazz) {

			@Override
			protected T runImpl(Arguments args) throws ConfigurationException {
				return builder.build(args);
			}
			
		};
	}
	
	private final String name;
	private final Class<T> returnType;

	public Verb(String name, Class<T> returnType) {
		this.name = name;
		this.returnType = returnType;
	}

	protected abstract T runImpl(Arguments args) throws ConfigurationException;
	
	public final T run(Arguments args) throws ConfigurationException {
		try {
			T result = runImpl(args);
			
			if (!args.get().isEmpty())
				throw new ConfigurationException("Verb " + getName() + " was given exsessive arguments " + args.get());
			
			if (returnType == Void.TYPE ? result != null : !returnType.isInstance(result))
				throw new ConfigurationException("(Code fault) Verb " + getName() + " returned \"" + result + "\", which is not a " + returnType);

			return result;
		} catch (ConfigurationException e) {
			throw e;
		} catch (RuntimeException e) {
			throw new ConfigurationException("(Code fault) Unexpected error in verb " + getName(), e);
		}
	}

	public String getName() {
		return name;
	}
	
	/**
	 * @return the returnType
	 */
	public Class<T> getReturnType() {
		return returnType;
	}
}
