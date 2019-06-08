package ru.windcorp.piwcs.pbm;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Timer;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

public class PBMPlugin extends JavaPlugin {
	
	private static PBMPlugin inst = null;
	
	private final Timer timer = new Timer("PBM Timer", true);
	
	private static Path backupDirectory;
	
	private final PBMWorker worker = new PBMWorker();
	
	@Override
	public void onLoad() {
		inst = this;
	}

	@Override
	public void onEnable() {
		loadConfig();
		
		ZonedDateTime dateTime = ZonedDateTime.now().truncatedTo(ChronoUnit.HOURS);
		dateTime = dateTime.withHour(((dateTime.getHour() - 1) / 4 + 1) * 4);
		
		timer.scheduleAtFixedRate(
				worker,
				Date.from(dateTime.toInstant()),
				4 * ChronoUnit.HOURS.getDuration().toMillis());
	}
	
	private void loadConfig() {
		saveDefaultConfig();
		
		backupDirectory = Paths.get(getConfig().getString("backup-directory"));
		
		if (!Files.isDirectory(backupDirectory)) {
			try {
				Files.createDirectories(backupDirectory);
			} catch (IOException e) {
				getLogger().warning("Could not create directory " + backupDirectory + ": " + e + ". Backups will fail when the directory does not exist");
			}
		}
	}

	@Override
	public void onDisable() {
		timer.cancel();
		inst = null;
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (!sender.isOp()) {
			sender.sendMessage(ChatColor.RED + "Insufficient permissions");
			return true;
		}
		timer.schedule(new PBMWorker(), 0);
		return true;
	}
	
	public static PBMPlugin getInst() {
		return inst;
	}
	
	public static Path getBackupDirectory() {
		return backupDirectory;
	}
	
}
