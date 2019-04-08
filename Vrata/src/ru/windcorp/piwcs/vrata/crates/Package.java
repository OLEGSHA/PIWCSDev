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
import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;
import java.util.function.Consumer;

import org.bukkit.entity.Player;

import ru.windcorp.piwcs.vrata.exceptions.VrataPermissionException;
import ru.windcorp.piwcs.vrata.users.VrataUser;

public class Package implements Iterable<Crate> {
	
	private final UUID uuid;
	private String name;
	private final Set<String> owners = new HashSet<>();
	
	private final SortedMap<String, SortedSet<Crate>> batches = new TreeMap<>(Comparator.nullsLast(Comparator.naturalOrder()));
	
	private final File file;
	private final File descFile;
	
	private int modCount = 0;
	private VrataUser currentUser = null;
	
	private boolean isModified = true;
	private boolean isDescriptionOutdated = true;
	
	public Package(UUID uuid, String name) {
		this.uuid = uuid;
		this.name = name;
		
		this.file = new File(Packages.getSaveDirectory(), escapeFileUnsafes(name) + "-" + uuid.toString() + ".package");
		this.descFile = new File(Packages.getSaveDirectory(), escapeFileUnsafes(name) + "-" + uuid.toString() + ".desc.txt");
	}
	
	private static String escapeFileUnsafes(String filename) {
		char[] chars = filename.toCharArray();
		for (int i = 0; i < chars.length; ++i) {
			char c = chars[i];
			if (
					(c >= 'a' && c <= 'z') ||
					(c >= 'A' && c <= 'Z') ||
					(c >= '0' && c <= '9') ||
					(c >= 'à' && c <= 'ÿ') ||
					(c >= 'À' && c <= 'ß') ||
					c != '-' || c != '_'
					) {
				continue;
			}
			
			chars[i] = '_';
		}
		
		return new String(chars);
	}

	public static Package load(DataInput input) throws IOException {
		Package result = new Package(
				
				new UUID(input.readLong(), input.readLong()),
				input.readUTF()
				
				);
		
		int owners = input.readInt();
		
		if (owners < 0) {
			throw new IOException("Could not read package data: negative owner length " + owners);
		}
		
		for (int i = 0; i < owners; ++i) {
			result.addOwner(input.readUTF());
		}
		
		int crates = input.readInt();
		
		if (crates < 0) {
			throw new IOException("Could not read package data: negative crate length " + crates);
		}
		
		for (int i = 0; i < crates; ++i) {
			result.addCrate(Crate.load(input));
		}
		
		result.isModified = false;
		result.isDescriptionOutdated = false;
		
		return result;
	}
	
	public synchronized void save(DataOutput output) throws IOException {
		output.writeLong(uuid.getMostSignificantBits());
		output.writeLong(uuid.getLeastSignificantBits());
		output.writeUTF(name);
		
		output.writeInt(owners.size());
		for (String owner : owners) output.writeUTF(owner);
		
		Collection<Crate> crates = new ArrayList<>(64);
		batches.forEach((name, batch) -> crates.addAll(batch));
		
		output.writeInt(crates.size());
		for (Crate crate : crates) crate.save(output);
		
		this.isModified = false;
	}
	
	public synchronized void saveDescriptions(Writer output) throws IOException {
		for (Crate crate : this) {
			output.write(crate.getBatch());
			output.write(": ");
			output.write(crate.getUuid().toString());
			output.write(" {\n");
			
			String[] lines = crate.getDescription().split("\n");
			for (String line : lines) {
				output.write('\t');
				output.write(line);
				output.write('\n');
			}
			
			output.write("}\n\n");
		}
		
		this.isDescriptionOutdated = false;
	}

	public synchronized boolean needsSaving() {
		if (isModified) {
			return true;
		}
		
		for (Entry<String, SortedSet<Crate>> batch : batches.entrySet()) {
			for (Crate crate : batch.getValue()) {
				if (crate.needsSaving()) {
					return true;
				}
			}
		}
		
		return false;
	}
	
	private synchronized void modify() {
		modCount++;
		this.isModified = true;
		this.isDescriptionOutdated = true;
	}
	
	public synchronized boolean needsDescriptionRewrite() {
		return isDescriptionOutdated;
	}

	public UUID getUuid() {
		return uuid;
	}
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public SortedMap<String, SortedSet<Crate>> getBatchMap() {
		return batches;
	}
	
	public synchronized void addCrate(Crate crate) {
		if (!getUuid().equals(crate.getPackageUuid())) {
			throw new IllegalArgumentException("Attempted to add crate with package UUID " +
					crate.getPackageUuid() + " to package with UUID " + getUuid());
		}
		
		SortedSet<Crate> batch = getBatchMap().get(crate.getBatch());
		
		if (batch == null) {
			batch = new TreeSet<>(Crate.INTRABATCH_DEPLOY_ORDER);
			getBatchMap().put(crate.getBatch(), batch);
		}
		
		modify();
		batch.add(crate);
	}
	
	public synchronized void removeCrate(Crate crate) {
		SortedSet<Crate> batch = getBatchMap().get(crate.getBatch());
		if (batch == null) {
			throw new RuntimeException("The batch of crate " + crate + " is out of sync: batch " + crate.getBatch() + " not found");
		}
		if (batch.remove(crate)) {
			modify();
		}
	}
	
	public Set<String> getOwners() {
		return owners;
	}
	
	public synchronized void addOwner(Player player) {
		addOwner(player.getName());
	}
	
	public synchronized void addOwner(String player) {
		if (getOwners().add(player)) modify();
	}
	
	public synchronized boolean removeOwner(String player) {
		boolean modified = getOwners().remove(player);
		if (modified) modify();
		return modified;
	}
	
	public boolean isOwner(Player player) {
		return getOwners().contains(player.getName());
	}

	@Override
	public Iterator<Crate> iterator() {
		return new Iterator<Crate>() {
			
			private final Iterator<? extends Iterable<Crate>> superIterator = getBatchMap().values().iterator();
			private Iterator<Crate> currentBatch = null;
			private Crate nextCrate = null;
			
			private final int expectedModCount = modCount;

			@Override
			public boolean hasNext() {
				checkComodification();
				pullNextCrate();
				return nextCrate != null;
			}

			@Override
			public Crate next() {
				checkComodification();
				if (!hasNext()) throw new NoSuchElementException();
				Crate result = nextCrate;
				nextCrate = null;
				return result;
			}

			private void checkComodification() {
				if (expectedModCount != modCount) {
					throw new ConcurrentModificationException("Package has been modified since this iterator was created");
				}
			}

			private void pullNextCrate() {
				while (nextCrate == null) {
					while (currentBatch == null) {
						if (!superIterator.hasNext()) {
							return;
						}
						
						currentBatch = superIterator.next().iterator();
					}
					
					if (currentBatch.hasNext()) {
						nextCrate = null;
					} else {
						currentBatch = null;
					}
				}
			}
			
		};
	}
	
	@Override
	public synchronized void forEach(Consumer<? super Crate> action) {
		Iterable.super.forEach(action);
	}
	
	public VrataUser getCurrentUser() {
		return currentUser;
	}

	public synchronized void setCurrentUser(VrataUser currentUser) throws VrataPermissionException {
		if (currentUser != null && this.currentUser != null) {
			throw VrataPermissionException.create("package.alreadyInUse", new Object[] {this.currentUser}, this);
		}
		this.currentUser = currentUser;
	}

	public File getFile() {
		return file;
	}
	
	public File getDescriptionFile() {
		return descFile;
	}

	@Override
	public String toString() {
		return getUuid().toString().substring(0, 6);
	}

}
