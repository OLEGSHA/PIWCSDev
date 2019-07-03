/*
 * PIWCS Accountant Plugin
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
package ru.windcorp.piwcs.acc.db.mods;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Collection;

import ru.windcorp.jputil.Version;
import ru.windcorp.piwcs.acc.Accountant;
import ru.windcorp.piwcs.acc.db.DatabaseEntry;
import ru.windcorp.piwcs.acc.db.Field;
import ru.windcorp.piwcs.acc.db.FieldBuilder;
import ru.windcorp.piwcs.acc.db.FieldList;
import ru.windcorp.piwcs.acc.db.FieldListWrapper;
import ru.windcorp.piwcs.acc.db.FieldManager;

/**
 * @author Javapony
 *
 */
public class Modification extends DatabaseEntry {
	
	public static enum State {
		SUGGESTED (0b01110),
		INSTALLED (0b10000),
		ACCEPTED  (0b01011),
		REJECTED  (0b00001),
		DELETING  (0b00010);
		
		private final int changeFlags;

		private State(int changeFlags) {
			this.changeFlags = changeFlags;
		}
		
		public static boolean canChangeTo(State from, State to) {
			return (from.changeFlags & (1 << to.ordinal())) != 0;
		}
	}
	
	public static enum Side {
		BOTH,
		CLIENT_REQUIRED,
		CLIENT_OPTIONAL,
		SERVER;
	}
	
	private final FieldManager manager = new FieldManager();
	
	private final Field<String> id =
			manager.newField(String.class).initial(null).name("id");
	private final Field<String> name =
			manager.newField(String.class).initial(null).name("name");
	private final Field<State> state =
			manager.newField(State.class).initial(null).name("state");
	private final Field<Side> side =
			manager.newField(Side.class).optional(Side.BOTH).name("side");
	private final Field<LocalDate> suggested =
			manager.newField(LocalDate.class).initial(null).name("suggested-date");
	private final Field<String> suggester =
			manager.newField(String.class).initial(null).name("suggested-by");
	private final Field<Version> installedVersion =
			manager.newField(Version.class).ioReader(Version::new, Version::toString).optionalNull().name("version-installed");
	private final Field<Version> newestVersion =
			manager.newField(Version.class).ioReader(Version::new, Version::toString).optionalNull().name("version-newest");
	private final Field<LocalDate> lastUpdateCheck =
			manager.newField(LocalDate.class).optionalNull().name("version-last-check");
	private final Field<URI> uri =
			manager.newField(URI.class).ioReader(URI::new, URI::toString).initial(null).name("uri");
	private final Field<String> comment =
			manager.newField(String.class).optionalNull().name("comments");
	
	private final FieldList<Field<String>> dependenciesField =
			manager.newFieldList(new FieldBuilder<>(String.class).initial(null)).optional().name("dependencies");
	private final Collection<Modification> dependencies = FieldListWrapper.wrap(dependenciesField,
			id -> Accountant.getMods().get(id), mod -> mod.getDatabaseId());
			
	protected Modification() {
		
	}

	/**
	 * @see ru.windcorp.piwcs.acc.db.DatabaseEntry#getDatabaseId()
	 */
	@Override
	public String getDatabaseId() {
		return getId();
	}

	/**
	 * @see ru.windcorp.piwcs.acc.db.DatabaseEntry#getFileName()
	 */
	@Override
	public String getFileName() {
		return getId();
	}
	
	/**
	 * @see ru.windcorp.piwcs.acc.db.DatabaseEntry#getFieldManager()
	 */
	@Override
	public FieldManager getFieldManager() {
		return manager;
	}
	
	static Modification create(
			String id, String name,
			LocalDate today, String suggester,
			URI uri
			) {
		Modification mod = new Modification();
		
		mod.id.set(id);
		mod.name.set(name);
		mod.state.set(State.SUGGESTED);
		mod.suggested.set(today);
		mod.suggester.set(suggester);
		mod.uri.set(uri);
		
		return mod;
	}
	
	public static Modification load(Path file) throws IOException {
		try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
			Modification user = new Modification();
			user.manager.load(reader, file.toString());
			return user;
		}
	}
	
	public void save(Path file) throws IOException {
		try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
			this.manager.save(writer);
		}
	}
	
	/**
	 * @return the id
	 */
	public String getId() {
		return id.get();
	}
	
	/**
	 * @return the name
	 */
	public String getName() {
		return name.get();
	}
	
	/**
	 * @return the state
	 */
	public State getState() {
		return state.get();
	}
	
	public void setState(State state) {
		this.state.set(state);
	}
	
	/**
	 * @return the side
	 */
	public Side getSide() {
		return side.get();
	}
	
	/**
	 * @return the registered
	 */
	public LocalDate getSuggestionDate() {
		return suggested.get();
	}
	
	/**
	 * @return the suggester
	 */
	public String getSuggester() {
		return suggester.get();
	}
	
	/**
	 * @return the installedVersion
	 */
	public Version getInstalledVersion() {
		return installedVersion.get();
	}
	
	public void setInstalledVersion(Version version) {
		installedVersion.set(version);
	}
	
	/**
	 * @return the newestVersion
	 */
	public Version getNewestVersion() {
		return newestVersion.get();
	}
	
	public void setNewestVersion(Version version) {
		newestVersion.set(version);
	}
	
	/**
	 * @return the lastUpdateCheck
	 */
	public LocalDate getLastUpdateCheckDate() {
		return lastUpdateCheck.get();
	}
	
	public void setLastUpdateCheckDate(LocalDate date) {
		lastUpdateCheck.set(date);
	}
	
	/**
	 * @return the URI
	 */
	public URI getURI() {
		return uri.get();
	}
	
	public void setURI(URI uri) {
		this.uri.set(uri);
	}
	
	/**
	 * @return the comment
	 */
	public String getComment() {
		return comment.get();
	}
	
	public void setComment(String comment) {
		this.comment.set(comment);
	}
	
	public Collection<Modification> getDependencies() {
		return dependencies;
	}
	
	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return getName();
	}

}
