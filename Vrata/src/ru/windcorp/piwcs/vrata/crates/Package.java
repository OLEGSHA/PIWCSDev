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
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;

import ru.windcorp.piwcs.vrata.users.VrataUser;
import ru.windcorp.tge2.util.synch.SyncStreams;

public class Package extends AbstractSet<Crate> {
	
	private final UUID uuid;
	private final long universeId;
	private String name;
	private final Set<String> owners = new HashSet<>();
	
	private final List<Crate> crates = new ArrayList<>();
	
	private String sorter = null;
	private Comparator<? super Crate> currentComparator = Crate.TOTAL_DEPLOY_ORDER;
	private int deployIndex = 0;
	
	private final File file;
	private final File descFile;
	
	private VrataUser currentUser = null;
	
	private boolean isModified = true;
	private boolean isDescriptionOutdated = true;
	
	public Package(UUID uuid, long universeId, String name) {
		this.uuid = uuid;
		this.universeId = universeId;
		this.name = name;
		
		this.file = new File(Packages.getSaveDirectory(), escapeFileUnsafes(name) + "__" + uuid.toString() + ".package");
		this.descFile = new File(Packages.getSaveDirectory(), escapeFileUnsafes(name) + "__" + uuid.toString() + ".desc.txt");
		
		Packages.registerPackage(this);
	}
	
	private static String escapeFileUnsafes(String filename) {
		char[] chars = filename.toCharArray();
		for (int i = 0; i < chars.length; ++i) {
			char c = chars[i];
			if (
					(c >= 'a' && c <= 'z') ||
					(c >= 'A' && c <= 'Z') ||
					(c >= '0' && c <= '9') ||
					(c >= '\u0430' && c <= '\u044f') ||
					(c >= '\u0410' && c <= '\u042f') ||
					c == '-' || c == '_'
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
				input.readLong(),
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
			result.add(Crate.load(input));
		}
		
		result.isModified = false;
		result.isDescriptionOutdated = false;
		
		return result;
	}
	
	public synchronized void save(DataOutput output) throws IOException {
		output.writeLong(uuid.getMostSignificantBits());
		output.writeLong(uuid.getLeastSignificantBits());
		output.writeLong(universeId);
		output.writeUTF(name);
		
		output.writeInt(owners.size());
		for (String owner : owners) output.writeUTF(owner);
		
		output.writeInt(size());
		for (Crate crate : this) crate.save(output);
		
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
		
		for (Crate crate : this) {
			if (crate.needsSaving()) {
				return true;
			}
		}
		
		return false;
	}
	
	private synchronized void modify() {
		this.isModified = true;
		this.isDescriptionOutdated = true;
	}
	
	public synchronized boolean needsDescriptionRewrite() {
		return isDescriptionOutdated;
	}

	public UUID getUuid() {
		return uuid;
	}
	
	public long getUniverseId() {
		return universeId;
	}
	
	public boolean isLocal() {
		return universeId == getLocalUniverseId();
	}
	
	private static long localUniverseId = 0;
	
	public static synchronized long getLocalUniverseId() {
		if (localUniverseId == 0) {
			localUniverseId = 1;
			for (World w : Bukkit.getWorlds()) {
				UUID uid = w.getUID();
				localUniverseId = localUniverseId * 31 + uid.getMostSignificantBits() + uid.getLeastSignificantBits();
			}
			if (localUniverseId == 0) {
				localUniverseId = ~0;
			}
		}
		
		return localUniverseId;
	}
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	@Override
	public synchronized int size() {
		return crates.size();
	}
	
	@Override
	public synchronized boolean add(Crate crate) {
		if (crate.getPackage() != null) {
			throw new IllegalArgumentException("Attempted to add " + crate + " to " + this
					+ " although it is already contained in " + crate.getPackage());
		}
		
		int index = Collections.binarySearch(crates, crate, currentComparator);
		modify();
		try {
			crates.add(-index - 1, crate);
			crate.setPackage(this);
		} catch (IndexOutOfBoundsException e) {
			throw new IllegalArgumentException("Attempted to add " + crate + " to " + this
					+ ", in which it is already contained");
		}
		return true;
	}
	
	@Override
	public boolean remove(Object o) {
		if (o instanceof Crate) {
			return remove((Crate) o);
		}
		return false;
	}
	
	public synchronized boolean remove(Crate crate) {
		int index = Collections.binarySearch(crates, crate, currentComparator);
		if (index < 0) {
			return false;
		}
		crates.remove(index);
		crate.setPackage(null);
		modify();
		return true;
	}
	
	public synchronized Crate getNext() {
		if (deployIndex >= crates.size()) return null;
		return crates.get(deployIndex);
	}
	
	public synchronized void skip() {
		deployIndex++;
	}
	
	@Override
	public boolean contains(Object o) {
		if (o instanceof Crate) {
			return this == ((Crate) o).getPackage();
		}
		
		return false;
	}
	
	public synchronized void sort(String sorter) {
		if (Objects.equals(this.sorter, sorter)) {
			return;
		}
		
		this.sorter = sorter;
		if (sorter == null) {
			final Matcher matcher = Pattern.compile(sorter, Pattern.UNICODE_CASE).matcher("");
			currentComparator =
					Comparator.comparing(
							(Function<Crate, Boolean>)
							crate -> 
							matcher.reset(crate.getBatch()).matches())
					.thenComparing(Crate.TOTAL_DEPLOY_ORDER);
		} else {
			currentComparator = Crate.TOTAL_DEPLOY_ORDER;
		}
		
		crates.sort(currentComparator);
		deployIndex = 0;
	}
	
	public String getSorter() {
		return sorter;
	}
	
	public Set<String> getOwners() {
		return owners;
	}
	
	public synchronized void addOwner(VrataUser user) {
		addOwner(user.getProfile().getName());
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
	
	public boolean isOwner(VrataUser user) {
		return isOwner(user.getProfile().getName());
	}
	
	public boolean isOwner(Player player) {
		return isOwner(player.getName());
	}
	
	public boolean isOwner(String name) {
		return getOwners().contains(name);
	}

	@Override
	public Iterator<Crate> iterator() {
		return crates.iterator();
	}
	
	@Override
	public Spliterator<Crate> spliterator() {
		return Spliterators.spliteratorUnknownSize(iterator(),
				Spliterator.DISTINCT | Spliterator.NONNULL | Spliterator.ORDERED);
	}
	
	@Override
	public Stream<Crate> stream() {
		return SyncStreams.synchronizedStream(
				StreamSupport.stream(
						this::spliterator,
						Spliterator.DISTINCT | Spliterator.NONNULL | Spliterator.ORDERED,
						false),
				this);
	}
	
	@Override
	public synchronized void forEach(Consumer<? super Crate> action) {
		super.forEach(action);
	}
	
	public VrataUser getCurrentUser() {
		return currentUser;
	}

	public synchronized void setCurrentUser(VrataUser user) {
		if (this.currentUser != null) {
			this.currentUser.setCurrentPackage(null);
		}
		
		this.currentUser = user;
		
		if (user != null) {
			user.setCurrentPackage(this);
		}
	}

	public File getFile() {
		return file;
	}
	
	public File getDescriptionFile() {
		return descFile;
	}

	@Override
	public String toString() {
		return "P" + getUuid().toString().substring(0, 6) + (isLocal() ? "-L" : "-R");
	}

}
