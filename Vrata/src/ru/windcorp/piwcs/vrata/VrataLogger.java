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

package ru.windcorp.piwcs.vrata;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Logger;

import ru.windcorp.piwcs.vrata.crates.Crate;

public class VrataLogger {
	
	private static File logDirectory;
	private static final String FILE_PATTERN = "vrata-log-%1$tF.log";
	private static final String ENTRY_PATTERN = "[%1$tT] %2$s";
	private static final String CRATE_LOG_FILE = "vrata-crates.log";
	private static final String CRATE_ENTRY_PATTERN = "%1$tT %2$100s %3$s = %3$s";
	private static Writer writer;
	private static Writer crateWriter;
	
	private static Logger backupLogger;
	private static final String BACKUP_PREFIX = "[Log] ";
	
	private static final Timer TIMER = new Timer("VrataLogger", true);
	
	public static void setup() {
		backupLogger = VrataPlugin.getInst().getLogger();

		ZonedDateTime firstLaunch = ZonedDateTime.of(
				LocalDate.now().plusDays(1),
				LocalTime.MIDNIGHT,
				ZoneId.systemDefault());
		
		TIMER.scheduleAtFixedRate(new TimerTask() {
					@Override
					public void run() {
						updateFile();
					}
				},
				// I just really hate these 20 Java Time/Date APIs
				// While I'm on that page, I really hate calendars
				new Date(firstLaunch.toEpochSecond() * 1000),
				24l * 60 * 60 * 1000);

		updateFile();
		
		try {
			crateWriter = new BufferedWriter(
					new OutputStreamWriter(
							new FileOutputStream(
									new File(
											logDirectory,
											CRATE_LOG_FILE
									),
									true
							),
					StandardCharsets.UTF_8)
			);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		
		write("Plugin enabled");
	}
	
	public static void terminate() {
		TIMER.cancel();
		write("Plugin disabled");
		if (writer != null) {
			try {
				writer.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	private static synchronized void updateFile() {
		Writer oldWriter = writer;
		try {
			if (oldWriter != null) {
				write("Switching to next file");
			}
			
			writer = new BufferedWriter(
					new OutputStreamWriter(
							new FileOutputStream(
									new File(
											logDirectory,
											String.format(FILE_PATTERN, Calendar.getInstance())
									),
									true
							),
					StandardCharsets.UTF_8)
			);
			
			write("File switched");
		} catch (IOException e) {
			if (oldWriter != null) {
				try {
					oldWriter.write("Could not switch to next file due to " + e + ". Rerouting output to Minecraft log\n");
				} catch (IOException ignore) {}
			}
			write("Could not update log file, reverting to Minecraft log");
			handleIOException(null, e);
		} finally {
			if (oldWriter != null) {
				try {
					oldWriter.close();
				} catch (IOException ignore) {}
			}
		}
	}

	public static synchronized void write(String message) {
		if (writer == null) {
			backupLogger.info(BACKUP_PREFIX + message);
			return;
		}
		
		try {
			writer.write(String.format(ENTRY_PATTERN, System.currentTimeMillis(), message));
			writer.write("\n");
		} catch (IOException e) {
			handleIOException(message, e);
		}
	}
	
	public static void write(String format, Object... args) {
		write(String.format(format, args));
	}
	
	public static synchronized void writeCrate(Crate crate) {
		if (crateWriter == null) {
			write(CRATE_ENTRY_PATTERN, crate.getCreationTime(), crate.toString(), crate.getUuid(), crate.getDescription());
			return;
		}
		
		try {
			crateWriter.write(String.format(CRATE_ENTRY_PATTERN, crate.getCreationTime(), crate.toString(), crate.getUuid(), crate.getDescription()));
			crateWriter.write("\n");
		} catch (IOException e) {
			write("IOException occured in VrataLogger: " + e);
			write("Dumping crates to main log");
			
			try {
				crateWriter.close();
			} catch (IOException ignore) {}
			crateWriter = null;

			writeCrate(crate);
		}
	}

	private static void handleIOException(String message, IOException e) {
		if (message != null) backupLogger.info(message);
		e.printStackTrace();
		backupLogger.severe("IOException occured in VrataLogger: " + e);
		writer = null;
	}

	public static File getLogDirectory() {
		return logDirectory;
	}

	public static void setLogDirectory(File logDirectory) {
		VrataLogger.logDirectory = logDirectory;
	}

}
