package ru.windcorp.piwcs.acc.db;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;

public abstract class DatabaseEntry {
	
	private boolean present = false;
	
	public DatabaseEntry() {
		
	}
	
	public abstract String getDatabaseId();
	public abstract String getFileName();
	public abstract FieldManager getFieldManager();
	public abstract void save(Path path) throws IOException;
	
	final Path getFile(Database<?> db) {
		return db.getDirectory().resolve(getFileName() + db.getExtension());
	}

	boolean isPresent() {
		return present;
	}
	
	boolean isAbsent() {
		return !present;
	}

	void setPresent(boolean present) {
		this.present = present;
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(getDatabaseId());
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		DatabaseEntry other = (DatabaseEntry) obj;
		return getDatabaseId() == other.getDatabaseId();
	}

}
