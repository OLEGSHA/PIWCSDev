/*
 * PIWCS Vrata Plugin
 * Copyright (C) 2019  PIWCS Team
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

package ru.windcorp.piwcs.vrata.crates;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Comparator;
import java.util.UUID;
import java.util.function.Function;

public class Crate {
	
	private static final long MAGIC_NUMBER = 0xFFFF_3103_2019_FFFFl;
	
	public static final Comparator<Crate> INTRABATCH_DEPLOY_ORDER = Comparator.comparing(crate -> crate.getCreationTime());
	public static final Comparator<Crate> TOTAL_DEPLOY_ORDER =
			Comparator.comparing((Function<Crate, String>) crate -> crate.getBatch())
			.thenComparing(INTRABATCH_DEPLOY_ORDER);
	
	private final UUID uuid;
	private final UUID pkg;
	private String batch;
	
	private final Instant creationTime;
	
	private final byte[] nbtData;
	private final int slots;
	private final String description;
	
	private boolean deployed;
	private boolean moderated;
	
	private Crate(UUID uuid, UUID pkg, String batch, Instant creationTime, byte[] nbtData, int slots,
			String description, boolean deployed, boolean moderated) {
		this.uuid = uuid;
		this.pkg = pkg;
		this.batch = batch;
		this.creationTime = creationTime;
		this.nbtData = nbtData;
		this.slots = slots;
		this.description = description;
		this.deployed = deployed;
		this.moderated = moderated;
	}
	
	public static Crate createNew(Package pkg, byte[] nbtData, int slots, String description) {
		return new Crate(
				
				UUID.randomUUID(),
				pkg.getUuid(),
				
				null,
				Instant.now(),
				
				nbtData,
				slots,
				description,
				
				false,
				false
				
				);
	}
	
	public static Crate load(DataInput input) throws IOException {
		long number = input.readLong();
		if (number != MAGIC_NUMBER) {
			throw new IOException("Could not load crate: invalid magic number, trying to read garbage."
					+ " Expected " + Long.toHexString(MAGIC_NUMBER) + ", got " + Long.toHexString(number));
		}
		
		UUID own = null, pkg = null;
		try {
			return new Crate(
					
					own = new UUID(input.readLong(), input.readLong()),
					pkg = new UUID(input.readLong(), input.readLong()),
					
					readBatch(input),
					Instant.ofEpochSecond(input.readLong()),
					
					readNBTData(input),
					input.readInt(),
					readPureUTF8(input),
					
					input.readBoolean(),
					input.readBoolean()
					
					);
		} catch (Exception e) {
			throw new IOException("Could not load crate " + ((own != null && pkg != null) ? getShortId(own, pkg) : "N/A") + " = " + own + "/" + pkg, e);
		}
	}

	private static String readBatch(DataInput input) throws IOException {
		String batch = input.readUTF();
		if (batch.equals("")) {
			return null;
		}
		return batch;
	}

	private static byte[] readNBTData(DataInput input) throws IOException {
		int length = input.readInt();
		
		if (length < 0) {
			throw new IOException("Could not read NBT data: negative length " + length);
		}
		
		try {
			byte[] array = new byte[length];
			input.readFully(array);
			return array;
		} catch (OutOfMemoryError e) {
			throw new IOException("Could not read allocate memory for NBT data: " + length + " bytes requested");
		}
	}

	private static String readPureUTF8(DataInput input) throws IOException {
		int length = input.readUnsignedShort();
		byte[] bytes = new byte[length];
		input.readFully(bytes);
		return new String(bytes, StandardCharsets.UTF_8);
	}
	
	public void save(DataOutput output) throws IOException {
		output.writeLong(MAGIC_NUMBER);
		
		output.writeLong(uuid.getMostSignificantBits());
		output.writeLong(uuid.getLeastSignificantBits());
		output.writeLong(pkg.getMostSignificantBits());
		output.writeLong(pkg.getLeastSignificantBits());
		
		if (batch == null) {
			output.writeUTF("");
		} else {
			output.writeUTF(batch);
		}
		
		output.writeLong(creationTime.getEpochSecond());
		
		output.writeInt(nbtData.length);
		output.write(nbtData);
		output.writeInt(slots);
		
		byte[] descriptionBytes = description.getBytes(StandardCharsets.UTF_8);
		output.writeShort(descriptionBytes.length);
		output.write(descriptionBytes);
		
		output.writeBoolean(deployed);
		output.writeBoolean(moderated);
	}
	
	public boolean isDeployed() {
		return deployed;
	}
	
	public void setDeployed(boolean deployed) {
		this.deployed = deployed;
	}
	
	public boolean isModerated() {
		return moderated;
	}
	
	public void setModerated(boolean moderated) {
		this.moderated = moderated;
	}
	
	public String getBatch() {
		return batch;
	}

	public void setBatch(String batch) {
		this.batch = batch;
	}

	public UUID getUuid() {
		return uuid;
	}

	public UUID getPackageUuid() {
		return pkg;
	}

	public Instant getCreationTime() {
		return creationTime;
	}

	public byte[] getNbtData() {
		return nbtData;
	}

	public int getSlots() {
		return slots;
	}

	public String getDescription() {
		return description;
	}
	
	@Override
	public String toString() {
		if (getBatch() != null) {
			return getBatch() + ":" + getShortId(getUuid(), getPackageUuid());
		}
		return getShortId(getUuid(), getPackageUuid());
	}

	private static String getShortId(UUID own, UUID pkg) {
		return pkg.toString().substring(0, 6) + "-" + own.toString().substring(0, 6);
	}

}
