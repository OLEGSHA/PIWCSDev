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

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;

import net.minecraft.util.ReportedException;
import ru.windcorp.mineragenesis.MGConfig;
import ru.windcorp.mineragenesis.MineraGenesis;

import static ru.windcorp.mineragenesis.MineraGenesis.logger;

import static  org.objectweb.asm.Opcodes.*;

// This is really overkill
public class MGAddonManager {
	
	private static class MGClassVisitor extends ClassVisitor {
		
		static final String ANNOTATION_NAME = "L" + MineraGenesisAddonLoader.class.getName().replace('.', '/') + ";";
		
		class MGMethodVisitor extends MethodVisitor {

			class MGAddonDetector extends AnnotationVisitor {
				
				private String id, name, version;
				private Integer minimumMgApiVersion;
			
				MGAddonDetector() {
					super(ASM4);
				}
				
				void reset() {
					id = name = version = null;
					minimumMgApiVersion = null;
				}
				
				@Override
				public void visit(String name, Object value) {
					switch (name) {
					case "id":
						this.id = (String) value;
						break;
					case "name":
						this.name = (String) value;
						break;
					case "version":
						this.version = (String) value;
						break;
					case "minimumMgApiVersion":
						this.minimumMgApiVersion = (Integer) value;
						break;
					}
				}
				
				@Override
				public void visitEnd() {
					if (id == null || name == null || version == null || minimumMgApiVersion == null) {
						logger.logf("Load hook %s$%s.%s() is not annotated correctly, metadata missing."
								+ " Discovered: id=%s, name=%s, verison=%s, minimumMgApiVersion=%s."
								+ " This is a problem with the addon. Skipping",
								
								file, className, methodName,
								id, name, version, minimumMgApiVersion);
						return;
					}
					
					if (minimumMgApiVersion > MineraGenesis.API_VERSION) {
						logger.logf("Load hook %s$%s.%s() could not be loaded: this version of MineraGenesis is too old."
								+ " Addon requires minimum API version %d, this version of MineraGenesis implements API version %d. Skipping",
								
								file, className, methodName,
								minimumMgApiVersion, MineraGenesis.API_VERSION);
						return;
					}
					
					MGAddonLoader.Metadata meta = new MGAddonLoader.Metadata(id, name, version);
					LoadHook hook = new LoadHook(file, className, methodName);
					registerAddon(hook, meta);
				}
				
			}
			
			final MGAddonDetector detector = new MGAddonDetector();
			String methodName;
			
			MGMethodVisitor() {
				super(ASM4);
			}
			
			@Override
			public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
				if (!ANNOTATION_NAME.equals(desc)) {
					return null;
				}
				
				logger.debug("Found addon hook annotation");
				detector.reset();
				return detector;
			}
		}

		private final Path file;
		private final MGMethodVisitor methodVisitor = new MGMethodVisitor();
		private String className = null;
		
		public MGClassVisitor(Path file) {
			super(ASM4);
			this.file = file;
		}
		
		private static final int CLASS_REQUIRED_ACCESS = ACC_PUBLIC;
		private static final int CLASS_FORBIDDEN_ACCESS = ACC_INTERFACE | ACC_ENUM;
		
		@Override
		public void visit(int version, int access, String name, String signature, String superName,
				String[] interfaces) {
			if ((access & CLASS_REQUIRED_ACCESS) != CLASS_REQUIRED_ACCESS) {
				return;
			}
			
			if ((access & CLASS_FORBIDDEN_ACCESS) != 0) {
				return;
			}
			
			className = name.replace('/', '.');
			logger.debug("Examining class %s", className);
		}
		
		private static final int METHOD_REQUIRED_ACCESS = ACC_PUBLIC | ACC_STATIC;
		private static final int METHOD_FORBIDDEN_ACCESS = ACC_ABSTRACT;
		
		@Override
		public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
			if ((access & METHOD_REQUIRED_ACCESS) != METHOD_REQUIRED_ACCESS) {
				return null;
			}
			
			if ((access & METHOD_FORBIDDEN_ACCESS) != 0) {
				return null;
			}
			
			if (!"()V".equals(desc)) {
				return null;
			}
			
			if ("<init>".equals(name)) {
				return null;
			}
			
			if (exceptions != null && exceptions.length != 0) {
				return null;
			}
			
			logger.debug("Examining method %s()", name);
			methodVisitor.methodName = name;
			return methodVisitor;
		}
		
	}
	
	private static class LoadHook implements MGAddonLoader {
		static final Map<Path, WeakReference<ClassLoader>> CLASSLOADERS = new HashMap<>();
		
		final String className, methodName;
		final Path file;
		
		LoadHook(Path file, String className, String methodName) {
			this.file = file;
			this.className = className;
			this.methodName = methodName;
		}
		
		@Override
		public void run() throws InvocationTargetException {
			try {
				getClassLoader(file).loadClass(className).getMethod(methodName).invoke(null);
			} catch (ClassNotFoundException | NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException e) {
				throw new RuntimeException(e);
			}
		}

		static ClassLoader getClassLoader(Path file) {
			WeakReference<ClassLoader> reference = CLASSLOADERS.get(file);
			
			ClassLoader result;
			if (reference == null || (result = reference.get()) == null) {
				try {
					CLASSLOADERS.put(file, new WeakReference<>(result =
							new URLClassLoader(new URL[] { file.toUri().toURL() }, MineraGenesis.class.getClassLoader())));
				} catch (MalformedURLException e) {
					// This ought not to happen
					throw new RuntimeException(
							"Could not obtain URL of Path " + file.toString() + " (FileSystem " + file.getFileSystem().toString() + ")",
							e);
				}
			}
			return result;
		}

		@Override
		public String toString() {
			return "LoadHook[" + file + "$" + className + "." + methodName + "()]";
		}
	}
	
	private static final Map<String, MGAddon> ADDONS = new HashMap<>();
	private static boolean areAddonsInitialized = false;
	
	private static void loadAddons(Path addonDirectory) {
		if (!Files.isDirectory(addonDirectory)) {
			logger.debug("Skipping addon load: addon directory %s does not exist", addonDirectory);
			try {
				Files.createDirectories(addonDirectory);
			} catch (IOException e) {
				logger.logf("Could not create addon directory %s: %s", addonDirectory, e);
				e.printStackTrace();
			}
			return;
		} else {
			logger.debug("Examining addon directory %s", addonDirectory);
		}
		
		try {
			PathMatcher addonCandidateFilter = addonDirectory.getFileSystem().getPathMatcher("glob:**.{jar,zip,mgaddon}");
			
			Files.list(addonDirectory)
			.filter(Files::isRegularFile)
			.filter(addonCandidateFilter::matches)
			.forEach(file -> {
				
				if (!Files.isReadable(file)) {
					logger.logf("Could not read addon candidate %s: no read access", file);
					return;
				}
				
				try {
					loadAddon(file);
				} catch (IOException e) {
					logger.logf("Could not read addon candidate %s due to %s", file, e.toString());
					e.printStackTrace();
				}
				
			});
		} catch (IOException e) {
			logger.logf("Could not list addon directory %s due to %s", addonDirectory, e.toString());
			e.printStackTrace();
		}
	}
	
	private static void loadAddon(Path file) throws IOException {
		logger.debug("Examining file %s for addons", file);
		JarInputStream stream = new JarInputStream(Files.newInputStream(file));
		
		MGClassVisitor visitor = new MGClassVisitor(file);
		
		for (JarEntry entry; (entry = stream.getNextJarEntry()) != null;) {
			if (entry.isDirectory()) {
				continue;
			}
			
			if (!entry.getName().endsWith(".class")) {
				continue;
			}
			
			ClassReader reader = new ClassReader(stream);
			reader.accept(visitor, ClassReader.SKIP_CODE);
		}
	}

	public static void initializeAddons() {
		loadAddons(MineraGenesis.getHelper().getGlobalConfigurationDirectory().resolve("addons"));
		
		logger.debug("About to initialize %d addons", ADDONS.size());
		
		Map<String, MGAddon> todo = new HashMap<>(ADDONS);
		logger.debug("Initializing addons with specified load order");
		MGConfig.getAddonLoadOrder().forEach(id -> {
			MGAddon addon = todo.remove(id);
			if (addon != null) initializeAddon(addon);
		});
		
		logger.debug("Initializing addons without specified load order");
		todo.values().forEach(MGAddonManager::initializeAddon);
		
		areAddonsInitialized = true;
		logger.debug("Addon initialization complete");
	}
	
	private static void initializeAddon(MGAddon addon) {
		if (MGConfig.getDisabledAddons().contains(addon.metadata.id)) {
			logger.logf("Skipping addon %s: disabled in config", addon.metadata.name);
		}
		
		logger.logf("Initializing addon %s version %s (ID %s)", addon.metadata.name, addon.metadata.version, addon.metadata.id);
		logger.debug("Loader: %s", addon.loader);
		
		try {
			addon.loader.run();
		} catch (Exception e) {
			logger.logf("Addon %s failed to initialize: %s", addon.metadata.name, e);
			
			Throwable toReport = e;
			if (e instanceof InvocationTargetException) toReport = e.getCause();
			
			if (toReport instanceof ReportedException) throw (ReportedException) toReport;
			
			MineraGenesis.crash(toReport, "Addon %s failed to initialize", addon.metadata.name);
		}
		
		logger.logf("Addon %s initialized successfully", addon.metadata.name);
		addon.setInitialized();
	}
	
	public static void registerAddon(MGAddonLoader loader, MGAddonLoader.Metadata meta) {
		MGAddon addon = new MGAddon(loader, meta);
		logger.debug("Registered addon %s", addon);
		ADDONS.put(meta.id, addon);
		
		if (areAddonsInitialized()) {
			logger.log("Initializing addon right now: main initialization already done");
			initializeAddon(addon);
		}
	}

	public static boolean areAddonsInitialized() {
		return areAddonsInitialized;
	}

	public boolean isAddonPresent(String id) {
		return ADDONS.containsKey(id);
	}
	
	public boolean isAddonInitialized(String id) {
		MGAddon addon = getAddon(id);
		if (addon == null) {
			return false;
		}
		return addon.isInitialized();
	}

	public MGAddon getAddon(String id) {
		return ADDONS.get(id);
	}

	public Collection<MGAddon> getAddons() {
		return ADDONS.values();
	}
	
}
