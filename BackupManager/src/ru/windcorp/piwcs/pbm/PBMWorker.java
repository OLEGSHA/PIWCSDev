package ru.windcorp.piwcs.pbm;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.DateTimeException;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.SortedSet;
import java.util.TimerTask;
import java.util.TreeSet;
import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;

public class PBMWorker extends TimerTask {
	
	private static final Path BACKUP_MARK = Paths.get("backup_info.txt");
	private static final DateTimeFormatter FILESYSTEM_FORMAT = DateTimeFormatter.ofPattern("uuuu-MM-dd_HH-mm-ss");
	private static final DateTimeFormatter DISPLAY_FORMAT = DateTimeFormatter.ofPattern("uuuu/MM/dd HH:mm:ss");

	@Override
	public void run() {
		synchronized (PBMWorker.class) {
			PBMPlugin plugin = PBMPlugin.getInst();
			
			try {
				
				Collection<Path> paths =
						Bukkit.getScheduler()
						.callSyncMethod(plugin, PBMWorker::doBackup)
						.get();
				
				LocalDateTime timestamp = LocalDateTime.now();
				Path newBackupDir = createNewBackupDirectory(timestamp);
				
				plugin.getLogger().info("Copying all data to " + newBackupDir);
				Collection<Path> relativePaths = copyFiles(paths, newBackupDir);
				
				Path lastBackupDir = findLastBackupDirectory();
				if (lastBackupDir == null) {
					plugin.getLogger().warning("No previous backups found. Skipping compression");
				} else {
					plugin.getLogger().info("Compressing backup, using " + lastBackupDir + " as reference");
					compress(relativePaths, newBackupDir, lastBackupDir);
					deleteOldBackups(timestamp);
				}
				markBackupReady(newBackupDir, timestamp);
				
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

	private static Path createNewBackupDirectory(LocalDateTime timestamp) throws IOException {
		String timestampString = FILESYSTEM_FORMAT.format(timestamp);
		Path path = PBMPlugin.getBackupDirectory().resolve(timestampString);
		
		timestampString = timestampString + "_";
		int attempt = 1;
		while (Files.exists(path)) {
			path = PBMPlugin.getBackupDirectory().resolve(timestampString + "_" + attempt);
			attempt++;
		}
		
		Files.createDirectories(path);
		return path;
	}

	private static Collection<Path> copyFiles(Collection<Path> paths, Path newBackupDir) throws IOException {
		Path workingDir = Paths.get("./");
		Collection<Path> relativePaths = new ArrayList<>();
		
		for (Path source : paths) {
			Path relativePath = workingDir.relativize(source);
			relativePaths.add(relativePath);
			Path destination = newBackupDir.resolve(relativePath);
			Files.createDirectories(destination);
			Files.walkFileTree(
					source,
					EnumSet.of(FileVisitOption.FOLLOW_LINKS),
					Integer.MAX_VALUE,
					new DirectoryCopier(source, destination));
		}
		
		return relativePaths;
	}
	
	private static Path findLastBackupDirectory() throws IOException {
		Iterator<Path> it = Files.list(PBMPlugin.getBackupDirectory()).iterator();
		
		Path result = null;
		LocalDateTime newestTimestamp = null;
		
		while (it.hasNext()) {
			Path currentPath = it.next();
			LocalDateTime currentTimestamp = getBackupTimestamp(currentPath);
			
			if (currentTimestamp != null
					&& (result == null || currentTimestamp.isAfter(newestTimestamp))) {
				result = currentPath;
				newestTimestamp = currentTimestamp;
			}
		}
		
		return result;
	}
	
	private static LocalDateTime getBackupTimestamp(Path path) {
		if (!Files.isDirectory(path)) return null;
		String name = path.getFileName().toString();
		if (!Files.exists(path.resolve(BACKUP_MARK))) return null;
		try {
			return FILESYSTEM_FORMAT.parse(name, LocalDateTime::from);
		} catch (DateTimeException e) {
			return null;
		}
	}

	private static void compress(Collection<Path> relativePaths, Path newBackupDir, Path lastBackupDir) throws IOException {
		for (Path path : relativePaths) {
			Path backup = newBackupDir.resolve(path);
			Path reference = lastBackupDir.resolve(path);
			
			Files.walkFileTree(
					backup,
					EnumSet.of(FileVisitOption.FOLLOW_LINKS),
					Integer.MAX_VALUE,
					new BackupCompressor(reference, backup));
		}
	}

	private static class Backup implements Comparable<Backup> {
		private final Path path;
		private final LocalDateTime timestamp;
		
		public Backup(Path path, LocalDateTime timestamp) {
			this.path = path;
			this.timestamp = timestamp;
		}

		public Path getPath() {
			return path;
		}

		public LocalDateTime getTimestamp() {
			return timestamp;
		}
		
		@Override
		public boolean equals(Object obj) {
			if (obj == null) return false;
			if (obj.getClass() != this.getClass()) return false;
			return path.equals(((Backup) obj).path);
		}
		
		@Override
		public int hashCode() {
			return path.hashCode();
		}

		@Override
		public int compareTo(Backup other) {
			return -timestamp.compareTo(other.timestamp); // Reverse order
		}

		@Override
		public String toString() {
			return path.toString();
		}
		
	}
	
	private static void deleteOldBackups(LocalDateTime now) throws IOException {
		Iterator<Path> it = Files.list(PBMPlugin.getBackupDirectory()).iterator();
		Logger log = PBMPlugin.getInst().getLogger();
		
		boolean[] dailyBackups = new boolean[7];
		EnumSet<Month> monthlyBackups = EnumSet.noneOf(Month.class);
		
		SortedSet<Backup> backups = new TreeSet<>();
		
		// Collect backups
		while (it.hasNext()) {
			Path path = it.next();
			LocalDateTime timestamp = getBackupTimestamp(path);
			
			if (timestamp == null) {
				continue;
			}
			
			if (timestamp.isAfter(now)) {
				continue;
			}
			
			backups.add(new Backup(path, timestamp));
		}
		
		Collection<Backup> remove = new ArrayList<>(backups.size());
		
		// Pick backups to delete
		for (Backup backup : backups) {
			long daysOld = ChronoUnit.DAYS.between(backup.getTimestamp(), now);
			
			if (daysOld <= 0) {
				continue;
			}
			
			if (daysOld <= 7) {
				if (dailyBackups[(int) (daysOld - 1)]) {
					remove.add(backup);
				} else {
					dailyBackups[(int) (daysOld - 1)] = true;
				}
				continue;
			}
			
			if (ChronoUnit.YEARS.between(backup.getTimestamp(), now) == 0) {
				Month month = backup.getTimestamp().getMonth();
				
				if (monthlyBackups.contains(month)) {
					remove.add(backup);
				} else {
					monthlyBackups.add(month);
				}
				continue;
			}

			remove.add(backup);
			continue;
		}
		
		// Delete backups
		if (remove.isEmpty()) {
			log.info("No backups need to be removed");
		} else for (Backup backup : remove) {
			log.info("Deleting backup from (" + DISPLAY_FORMAT.format(backup.getTimestamp()) + ") in " + backup.getPath());
			Files.walkFileTree(backup.getPath(), DirectoryRemover.get());
		}
	}

	private static void markBackupReady(Path path, LocalDateTime timestamp) throws IOException {
		try (Writer writer = Files.newBufferedWriter(path.resolve(BACKUP_MARK), StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW)) {
			writer.write("Timestamp: " + DateTimeFormatter.RFC_1123_DATE_TIME.format(timestamp.atZone(ZoneId.systemDefault())));
		}
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
		} else {
			Bukkit.getScheduler().runTask(PBMPlugin.getInst(), () -> Bukkit.broadcastMessage(message));
		}
	}

}
