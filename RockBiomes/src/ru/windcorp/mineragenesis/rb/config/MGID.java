/* MineraGenesis Rock Biomes Addon
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

import ru.windcorp.mineragenesis.MineraGenesis;
import ru.windcorp.mineragenesis.request.ChunkData;

/**
 * @author Javapony
 *
 */
public class MGID {
	
	public static final int META_WILDCARD = -1;
	
	public final int id;
	public final int meta;
	
	public MGID(int id, int meta) {
		MineraGenesis.logger.debug("[MGID DEBUG] New MGID object: id = %d, meta = %d, short = %d", id, meta, ChunkData.getMGID(id, meta));
		
		if (id < 0 || id > 0x0FFF) {
			throw new IllegalArgumentException("Illegal ID " + id);
		}
		
		this.id = id;
		this.meta = meta;
	}

	public short toShort() throws ConfigurationException {
		if (isMetaWildcard())
			throw new ConfigurationException(this + " is a wildcard but a concrete block requested");
		
		return ChunkData.getMGID(id, meta);
	}
	
	public boolean isMetaWildcard() {
		return meta == META_WILDCARD;
	}
	
	@Override
	public String toString() {
		return id + (isMetaWildcard() ? ":*" : ":" + meta);
	}

	@Override
	public int hashCode() {
		if (isMetaWildcard()) 
			return -ChunkData.getMGID(id, 0);
		else return ChunkData.getMGID(id, meta);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		MGID other = (MGID) obj;
		if (id != other.id)
			return false;
		if (meta != other.meta)
			return false;
		return true;
	}

}
