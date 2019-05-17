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

import ru.windcorp.piwcs.vrata.users.VrataUser;

public class Crate {
	
	private static final long MAGIC_NUMBER = 0xFFFF_3103_2019_FFFFl;
	
	public static final Comparator<Crate> INTRABATCH_DEPLOY_ORDER = Comparator.comparing(crate -> crate.getCreationTime());
	public static final Comparator<Crate> TOTAL_DEPLOY_ORDER =
			Comparator.comparing(
					(Function<Crate, Boolean>)
					crate -> crate.isModerated()
					)
			.thenComparing(
					Comparator.nullsLast(
							Comparator.comparing(
									(Function<Crate, String>)
									crate -> crate.getBatch()
									)
							)
					)
			.thenComparing(
					INTRABATCH_DEPLOY_ORDER
					);
	
	private final UUID uuid;
	private Package pkg = null;
	private String batch = null;
	
	private final Instant creationTime;
	
	private final byte[] nbtData;
	private final int slots;
	private final String description;
	
	private boolean deployed;
	private boolean moderated;
	
	private boolean isModified;
	private boolean hasBeenAdded;
	
	private Crate(
			UUID uuid,
			String batch,
			Instant creationTime,
			byte[] nbtData, int slots,
			String description,
			boolean deployed, boolean moderated,
			boolean modifiedFlag, boolean hasBeenAdded) {
		this.uuid = uuid;
		this.batch = batch;
		this.creationTime = creationTime;
		this.nbtData = nbtData;
		this.slots = slots;
		this.description = description;
		this.deployed = deployed;
		this.moderated = moderated;
		
		this.isModified = modifiedFlag;
		this.hasBeenAdded = hasBeenAdded;
	}
	
	public static Crate createNew(byte[] nbtData, int slots, String description) {
		return new Crate(
				
				UUID.randomUUID(),
				
				null,
				Instant.now(),
				
				nbtData,
				slots,
				description,
				
				false,
				false,
				
				true,
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
					
					readBatch(input),
					Instant.ofEpochSecond(input.readLong()),
					
					readNBTData(input),
					input.readInt(),
					readPureUTF8(input),
					
					input.readBoolean(),
					input.readBoolean(),
					
					false,
					true
					
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
		
		isModified = false;
	}
	
	public boolean isDeployed() {
		return deployed;
	}
	
	public void setDeployed(boolean deployed) {
		this.deployed = deployed;
		markForSaving();
	}
	
	public boolean isModerated() {
		return moderated;
	}
	
	public void setModerated(boolean moderated) {
		this.moderated = moderated;
		markForSaving();
	}
	
	public boolean canDeploy(VrataUser user) {
		return !isDeployed()
				&& (
						user.getProfile().isModerator()
					 || isModerated()
					 || getPackage().isLocal()
				   );
	}
	
	public String getBatch() {
		return batch;
	}

	public void setBatch(String batch) {
		if (hasBeenAdded) {
			throw new IllegalStateException("Crate " + this + " is already added to a package yet an attempt has been made to modify its batch");
		}
		this.batch = batch;
		// No need to markForSaving() since we should not have been saved ever so far
	}

	public UUID getUuid() {
		return uuid;
	}

	public UUID getPackageUuid() {
		if (pkg == null) return null;
		return pkg.getUuid();
	}
	
	public Package getPackage() {
		return pkg;
	}
	
	void setPackage(Package pkg) {
		this.pkg = pkg;
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
			return "C-" + getBatch() + "-" + getShortId(getUuid(), getPackageUuid());
		}
		return "C-X-" + getShortId(getUuid(), getPackageUuid());
	}

	private static String getShortId(UUID own, UUID pkg) {
		return pkg.toString().substring(0, 6) + "-" + own.toString().substring(0, 6);
	}

	public boolean needsSaving() {
		return isModified;
	}
	
	public void markForSaving() {
		isModified = true;
	}

}
