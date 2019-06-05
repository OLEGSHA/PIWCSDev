package ru.windcorp.piwcs.pbm;

import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.DateTimeException;
import java.time.Instant;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.TimerTask;
import java.util.concurrent.ExecutionException;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;

public class PBMWorker extends TimerTask {
	
	private static final Path BACKUP_MARK = Paths.get("backup_info.txt");

	@Override
	public void run() {
		PBMPlugin plugin = PBMPlugin.getInst();
		
		try {
			
			Collection<Path> paths =
					Bukkit.getScheduler()
					.callSyncMethod(plugin, PBMWorker::doBackup)
					.get();
			
			Path newBackupDir = createNewBackupDirectory();
			
			plugin.getLogger().info("Copying all data to " + newBackupDir);
			copyFiles(paths, newBackupDir);
			
			Path lastBackupDir = findLastBackupDirectory();
			if (lastBackupDir == null) {
				plugin.getLogger().warning("No previous backups found. Skipping compression");
			} else {
				plugin.getLogger().info("Compressing backup, using " + lastBackupDir + " as reference");
				compress(paths, newBackupDir, lastBackupDir);
				deleteOldBackups();
			}
			
			broadcastMessage(ChatColor.GRAY + "Резервная копия создана");
		} catch (IOException e) {
			broadcastMessage(ChatColor.DARK_RED + "Резервная копия не была создана: ошибка при работе с файлами");
			e.printStackTrace();
			return;
		} catch (InterruptedException | ExecutionException e) {
			broadcastMessage(ChatColor.DARK_RED + "Резервная копия не была создана: непредвиденная ошибка");
			e.printStackTrace();
			return;
		}
		
	}

	public static Collection<Path> doBackup() {
		broadcastMessage(ChatColor.GRAY + "Создание резервной копии мира, возможны лаги");
		
		Collection<Path> paths = new LinkedList<>();
		
		for (World world : Bukkit.getServer().getWorlds()) {
			PBMPlugin.getInst().getLogger().info("Saving world " + world.getName());
			world.save();
			insertPath(paths, world.getWorldFolder().toPath());
		}
		
		Bukkit.broadcastMessage(ChatColor.GRAY + "Лаги должны исчезнуть");
		return paths;
	}

	private static Path createNewBackupDirectory() throws IOException {
		String timestamp = Instant.now().toString();
		Path path = PBMPlugin.getBackupDirectory().resolve(timestamp);
		
		timestamp = timestamp + "_";
		int attempt = 1;
		while (Files.exists(path)) {
			path = PBMPlugin.getBackupDirectory().resolve(timestamp + "_" + attempt);
			attempt++;
		}
		
		Files.createDirectories(path);
		return path;
	}

	private static void copyFiles(Collection<Path> paths, Path newBackupDir) throws IOException {
		Path workingDir = Paths.get("./");
		for (Path source : paths) {
			Path destination = newBackupDir.resolve(workingDir.relativize(source));
			Files.createDirectories(destination);
			Files.walkFileTree(
					source,
					EnumSet.of(FileVisitOption.FOLLOW_LINKS),
					Integer.MAX_VALUE,
					new DirectoryCopier(source, destination));
		}
	}
	
	private static Path findLastBackupDirectory() throws IOException {
		Iterator<Path> it = Files.list(PBMPlugin.getBackupDirectory()).iterator();
		
		Path result = null;
		Instant newestTimestamp = null;
		
		while (it.hasNext()) {
			Path currentPath = it.next();
			Instant currentTimestamp = getBackupTimestamp(currentPath);
			
			if (currentTimestamp != null
					&& (result == null || currentTimestamp.isAfter(newestTimestamp))) {
				result = currentPath;
				newestTimestamp = currentTimestamp;
			}
		}
		
		return result;
	}
	
	private static Instant getBackupTimestamp(Path path) {
		if (!Files.isDirectory(path)) return null;
		String name = path.getFileName().toString();
		if (!Files.exists(path.resolve(BACKUP_MARK))) return null;
		try {
			return Instant.parse(name);
		} catch (DateTimeException e) {
			return null;
		}
	}

	private static void compress(Collection<Path> paths, Path newBackupDir, Path lastBackupDir) throws IOException {
//		for (Path path : paths) {
//			Path backup = newBackupDir.
//			
//			Files.walkFileTree(
//					newBackupDir,
//					EnumSet.of(FileVisitOption.FOLLOW_LINKS),
//					Integer.MAX_VALUE,
//					new IBMCompressor(lastBackupDir, newBackupDir));
//		}
	}

	private static void deleteOldBackups() {
		// TODO Auto-generated method stub
		System.err.println("Called auto-generated method IBMWorker.deleteOldBackups");
		
	}
	
	private static void insertPath(Collection<Path> paths, Path newPath) {
		Iterator<Path> it = paths.iterator();
		Path path = null;
		while (it.hasNext()) {
			path = it.next();
			
			if (newPath.startsWith(path)) {
				return;
			} else if (path.startsWith(newPath)) {
				it.remove();
				continue;
			}
		}
		paths.add(newPath);
	}
	
	public static void broadcastMessage(String message) {
		if (Bukkit.isPrimaryThread()) {
			Bukkit.broadcastMessage(message);
			PBMPlugin.getInst().getLogger().info(message);
		} else {
			Bukkit.getScheduler().runTask(PBMPlugin.getInst(), () -> {
				Bukkit.broadcastMessage(message);
				PBMPlugin.getInst().getLogger().info(message);
			});
		}
	}

}
