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

/**
 * @author Javapony
 *
 */
public abstract class VoidVerb extends Verb<Void> {

	public VoidVerb(String name) {
		super(name, Void.TYPE);
	}

	/**
	 * @see ru.windcorp.mineragenesis.rb.config.Verb#runImpl(ru.windcorp.mineragenesis.rb.config.Arguments)
	 */
	@Override
	protected final Void runImpl(Arguments args) throws ConfigurationException {
		runVoid(args);
		return null;
	}
	
	protected abstract void runVoid(Arguments args) throws ConfigurationException;

}
