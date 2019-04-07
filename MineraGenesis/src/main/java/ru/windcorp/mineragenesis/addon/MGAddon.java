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
package ru.windcorp.mineragenesis.addon;

import ru.windcorp.mineragenesis.addon.MGAddonLoader.Metadata;

public final class MGAddon {
	
	public final MGAddonLoader loader;
	public final MGAddonLoader.Metadata metadata;
	private boolean isInitialized = false;
	
	public MGAddon(MGAddonLoader loader, Metadata metadata) {
		this.loader = loader;
		this.metadata = metadata;
	}

	public boolean isInitialized() {
		return isInitialized;
	}

	void setInitialized() {
		this.isInitialized = true;
	}

	@Override
	public String toString() {
		return "MGAddon [loader=" + loader + ", metadata=" + metadata + "]";
	}

}
