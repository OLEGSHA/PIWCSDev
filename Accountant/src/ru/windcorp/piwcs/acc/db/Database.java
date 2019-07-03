package ru.windcorp.piwcs.acc.db;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import ru.windcorp.jputil.SyncStreams;
import ru.windcorp.jputil.cmd.CommandRunner;
import ru.windcorp.jputil.functions.ThrowingFunction;
import ru.windcorp.piwcs.acc.Accountant;

public class Database<T extends DatabaseEntry> {
	
	private final Map<String, T> entries = new HashMap<>();
	private final Collection<T> deletingEntries = new ArrayList<>();
	private final Path directory;
	
	private final ThrowingFunction<Path, T, IOException> entryCreator;
	private final String extension;

	public Database(ThrowingFunction<Path, T, IOException> entryCreator, Path directory, String extension) {
		this.entryCreator = entryCreator;
		this.directory = directory;
		this.extension = extension;
	}

	public synchronized T get(String id) {
		return entries.get(id);
	}
	
	protected synchronized void add(T entry) {
		if (entry.isPresent())
			throw new IllegalArgumentException("Attempted to add entry that is already present: " + entry);
		
		T previous = entries.putIfAbsent(entry.getDatabaseId(), entry);
		if (previous != null)
			throw new IllegalArgumentException("Duplicate database ID: " + entry + ", " + previous);
		deletingEntries.remove(entry);
		
		entry.setPresent(true);
	}
	
	public synchronized void remove(T entry) {
		if (entries.remove(entry.getDatabaseId(), entry)) {
			entry.setPresent(false);
			deletingEntries.add(entry);
		} else
			throw new IllegalArgumentException("Entry " + entry + " is not registered");
	}
	
	public synchronized Collection<T> entries() {
		return new ArrayList<>(entries.values());
	}
	
	public synchronized int size() {
		return entries.size();
	}
	
	public Stream<T> stream() {
		return SyncStreams.synchronizedStream(entries.values().stream(), this);
	}
	
	public synchronized void load() throws IOException {
		if (!Files.exists(getDirectory())) {
			Files.createDirectories(getDirectory());
			return;
		}
		
		Set<T> loaded = new HashSet<>();
		for (Iterator<Path> it = Files.list(getDirectory()).iterator(); it.hasNext();) {
			Path path = it.next();
			
			if (
					!Files.isRegularFile(path) ||
					!path.toString().endsWith(getExtension())
					) {
				continue;
			}
			
			T entry = getEntryCreator().apply(path);
			if (!loaded.add(entry))
				throw new IOException("File contains duplicate ID " + entry.getDatabaseId());
		}

		if (!entries.isEmpty()) {
			for (T entry : entries.values()) entry.setPresent(false);
			entries.clear();
		}
		deletingEntries.clear();
		
		loaded.forEach(this::add);
	}
	
	public synchronized void save(CommandRunner runner) throws IOException {
		for (T entry : entries.values()) {
			entry.save(entry.getFile(this));
		}
		
		for (Iterator<T> it = deletingEntries.iterator(); it.hasNext();) {
			T entry = it.next();
			String baseName = entry.getFileName() + getExtension();
			
			try {
				Path target = getDirectory().resolve(baseName + ".deleted");
				
				for (int i = 0; Files.exists(target); ++i) {
					target = getDirectory().resolve(baseName + ".deleted" + i);
				}
				
				entry.save(target);
				Files.deleteIfExists(entry.getFile(this));
				it.remove();
			} catch (IOException e) {
				runner.respond("Could not delete user %s due to an IO problem", entry);
				runner.reportException(e);
				Accountant.reportException(e, "Could not delete entry %s", entry);
			}
		}
	}

	public ThrowingFunction<Path, T, IOException> getEntryCreator() {
		return entryCreator;
	}

	public Path getDirectory() {
		return directory;
	}

	public String getExtension() {
		return extension;
	}

}
