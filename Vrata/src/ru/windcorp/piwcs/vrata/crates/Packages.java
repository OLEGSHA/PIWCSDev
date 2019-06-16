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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import ru.windcorp.jputil.SyncStreams;
import ru.windcorp.piwcs.vrata.VrataPlugin;

public class Packages {
	
	private static final Path SAVE_DIR = VrataPlugin.getDataPath("packages");
	
	private static final Map<UUID, Package> PACKAGES = Collections.synchronizedMap(new HashMap<>());
	private static final Set<Path> FILES_TO_DELETE = Collections.synchronizedSet(new HashSet<>());
	
	public static Path getSaveDirectory() {
		return SAVE_DIR;
	}

	public static Collection<Package> getPackages() {
		return PACKAGES.values();
	}
	
	public static Stream<Package> packages() {
		return SyncStreams.synchronizedStream(getPackages().stream(), getPackages());
	}
	
	public static Package getPackage(UUID uuid) {
		return PACKAGES.get(uuid);
	}
	
	static void registerPackage(Package pkg) {
		Package existing = PACKAGES.put(pkg.getUuid(), pkg);
		FILES_TO_DELETE.remove(pkg.getFile());
		
		if (existing != null) {
			throw new IllegalArgumentException("Duplicate package UUID: " + existing + " and " + pkg);
		}
	}
	
	public static void removePackage(Package pkg) {
		if (PACKAGES.get(pkg.getUuid()) != pkg) {
			throw new IllegalArgumentException("Package " + pkg + " was never registered");
		}
		
		pkg.setCurrentUser(null);
		FILES_TO_DELETE.add(pkg.getFile());
		FILES_TO_DELETE.add(pkg.getDescriptionFile()); // Description file will be empty
		PACKAGES.remove(pkg.getUuid());
	}
	
	public static void load() throws IOException {
		Files.createDirectories(getSaveDirectory());
		
		synchronized (PACKAGES) {
			if (!PACKAGES.isEmpty()) {
				throw new IllegalStateException("Packages already loaded");
			}
			
			for (Path path : Files.newDirectoryStream(getSaveDirectory(), "*.package")) {
				try {
					Package.load(
							new DataInputStream(
									new BufferedInputStream(
											Files.newInputStream(path)
									)
							)
					);
				} catch (IllegalArgumentException e) {
					throw new IOException("Duplicate packages detected", e);
				} catch (IOException e) {
					throw e;
				} catch (Exception e) {
					throw new IOException("Could not read package from file " + path, e);
				}
				
			}
		}
		
		VrataPlugin.getInst().getLogger().info("Loaded " + PACKAGES.size() + " packages");
	}

	public static void save() throws IOException {
		synchronized (PACKAGES) {
			for (Package pkg : PACKAGES.values()) {
				try {
					if (pkg.needsSaving()) {
						try (DataOutputStream output =
								new DataOutputStream(
										new BufferedOutputStream(
												Files.newOutputStream(pkg.getFile())
										)
								)
						) {
							pkg.save(output);
						}
					}
					if (pkg.needsDescriptionRewrite()) {
						try (Writer jkRowling = Files.newBufferedWriter(pkg.getDescriptionFile(), StandardCharsets.UTF_8)) {
							pkg.saveDescriptions(jkRowling);
						}
					}
				} catch (IOException e) {
					throw e;
				} catch (Exception e) {
					throw new IOException("Could not save package " + pkg, e);
				}
			}
		}
		
		VrataPlugin.getInst().getLogger().info("Saved " + PACKAGES.size() + " packages");
		
		synchronized (FILES_TO_DELETE) {
			for (Iterator<Path> it = FILES_TO_DELETE.iterator(); it.hasNext();) {
				Path file = it.next();
				
				try {
					Files.deleteIfExists(file);
					it.remove();
					continue;
				} catch (IOException e) {
					VrataPlugin.getInst().getLogger().warning("Could not delete file " + file + ": " + e);
					// Do not remove
					continue;
				}
			}
		}
	}

	public static void attemptSave() {
		synchronized (PACKAGES) {
			if (packages().allMatch(pkg -> pkg.getCurrentUser() == null)) {
				VrataPlugin.save();
			}
		}
	}

}
