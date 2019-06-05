package ru.windcorp.piwcs.pbm;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.plugin.java.JavaPlugin;

import guava10.com.google.common.primitives.Ints;

public class PBMPlugin extends JavaPlugin {
	
	private static PBMPlugin inst = null;
	
	private final Timer timer = new Timer("IBM Timer", true);
	private final static ProcessHandler PROCESS_HANDLER = new ProcessHandler();
	
	private static Path backupDirectory;

	private static String[] linkCommand;
	private static int[] sourceIndices;
	private static int[] linkIndices;
	
	@Override
	public void onLoad() {
		inst = this;
	}

	@Override
	public void onEnable() {
		loadConfig();
		
		ZonedDateTime dateTime = ZonedDateTime.now().truncatedTo(ChronoUnit.HOURS);
		dateTime = dateTime.withHour(((dateTime.getHour() - 1) / 4 + 1) * 4);
		
		Thread phThread = new Thread(PROCESS_HANDLER::run, "IBM Process Handler");
		phThread.setDaemon(true);
		phThread.start();
		
		timer.scheduleAtFixedRate(
				new PBMWorker(),
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

		{
			String linkCommandDeclar = getConfig().getString("link-command");
			Matcher matcher = Pattern.compile(
					// Matches words and quotations, does not support escapes
					"([^\"\\s]+)|(\"[^\"]*\")"
					).matcher(linkCommandDeclar);
			
			Collection<Integer> sourceIndicesGrowable = new ArrayList<>();
			Collection<Integer> linkIndicesGrowable = new ArrayList<>();
			List<String> tokens = new ArrayList<>();
			
			for (int i = 0; matcher.find(); ++i) {
				String token = matcher.group();
				if (matcher.group(1) != null) {
					token = token.substring(1, token.length() - 1);
				}
				
				switch (token) {
				case "{SOURCE}":
					tokens.add(null);
					sourceIndicesGrowable.add(i);
					break;
				case "{LINK}":
					tokens.add(null);
					linkIndicesGrowable.add(i);
					break;
				default:
					tokens.add(token);
				}
			}
			
			if (sourceIndicesGrowable.isEmpty()) {
				throw new RuntimeException(linkCommandDeclar + " does not contain {SOURCE}");
			}
			
			if (linkIndicesGrowable.isEmpty()) {
				throw new RuntimeException(linkCommandDeclar + " does not contain {LINK}");
			}
			
			linkCommand = tokens.toArray(new String[0]);
			sourceIndices = Ints.toArray(sourceIndicesGrowable);
			linkIndices = Ints.toArray(linkIndicesGrowable);
		}
	}

	@Override
	public void onDisable() {
		timer.cancel();
		PROCESS_HANDLER.stop();
		inst = null;
	}
	
	public static PBMPlugin getInst() {
		return inst;
	}
	
	public static Path getBackupDirectory() {
		return backupDirectory;
	}
	
	public static void runLinkCommand(String source, String link) throws IOException {
		String[] cmd = linkCommand.clone();
		for (int i : sourceIndices) cmd[i] = source;
		for (int i : linkIndices) cmd[i] = link;
		Process process = Runtime.getRuntime().exec(cmd);
		getProcessHandler().handleCommandExecution(process, cmd);
	}
	
	public static ProcessHandler getProcessHandler() {
		return PROCESS_HANDLER;
	}
	
}
