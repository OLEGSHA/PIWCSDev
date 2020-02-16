/**
 * Copyright (C) 2019 OLEGSHA
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
package ru.windcorp.piwcs.acc;

import java.io.IOException;
import java.nio.file.Path;
import java.util.function.Consumer;

import ru.windcorp.jputil.cmd.AutoCommand;
import ru.windcorp.jputil.cmd.Command;
import ru.windcorp.jputil.cmd.CommandErrorException;
import ru.windcorp.jputil.cmd.CommandExceptions;
import ru.windcorp.jputil.cmd.CommandRunner;
import ru.windcorp.jputil.cmd.CommandSystem;
import ru.windcorp.jputil.cmd.HelpCommand;
import ru.windcorp.jputil.cmd.Invocation;
import ru.windcorp.jputil.cmd.parsers.Parsers;
import ru.windcorp.piwcs.acc.db.FieldManager;
import ru.windcorp.piwcs.acc.db.mods.ModDatabase;
import ru.windcorp.piwcs.acc.db.mods.ModDatabaseCommand;
import ru.windcorp.piwcs.acc.db.mods.ModificationParser;
import ru.windcorp.piwcs.acc.db.settlement.SettlementDatabase;
import ru.windcorp.piwcs.acc.db.settlement.SettlementDatabaseCommand;
import ru.windcorp.piwcs.acc.db.settlement.SettlementParser;
import ru.windcorp.piwcs.acc.db.user.UserDatabase;
import ru.windcorp.piwcs.acc.db.user.UserDatabaseCommand;
import ru.windcorp.piwcs.acc.db.user.UserParser;

public class Accountant {

	private static UserDatabase userDb = null;
	private static ModDatabase modDb = null;
	private static SettlementDatabase settlementDb = null;
	private static Server server = null;
	private static CommandSystem commandSystem = null;
	
	private static Runnable exit;
	private static Consumer<String> logger;
	private static Path root;
	
	public static void reportExceptionAndExit(Exception e, String msg, Object... args) {
		reportException(e, msg, args);
		exit.run();
	}
	
	public static void reportException(Exception e, String msg, Object... args) {
		e.printStackTrace();
		logger.accept(String.format(msg, args));
	}
	
	public static void log(String msg) {
		logger.accept(msg);
	}
	
	public static void log(String msg, Object... args) {
		logger.accept(String.format(msg, args));
	}

	public static Path getRoot() {
		return root;
	}

	public static void setImplementation(Runnable exit, Consumer<String> logger, Path root) {
		Accountant.exit = exit;
		Accountant.logger = logger;
		Accountant.root = root;
	}
	
	public static void start() throws Exception {
		FieldManager.registerStandardTypes();
		setupUserDatabase();
		setupModDatabase();
		setupSettlementDatabase();
		setupCommandSystem();
	}

	/**
	 * 
	 */
	private static void setupCommandSystem() throws Exception {
		AutoCommand.regsiterDefaultParsers();
		ru.windcorp.jputil.cmd.Context context = new ru.windcorp.jputil.cmd.Context();
		context.addDefaultExceptionHandlers();
		context.setHelpCommand(new HelpCommand());
		
		Parsers.registerCreator("version", VersionParser::new);
		Parsers.registerCreator("date", LocalDateParser::new);
		
		Parsers.registerCreator("mod", ModificationParser::new);
		Parsers.registerCreator("settlement", SettlementParser::new);
		Parsers.registerCreator("user", UserParser::new);
		
		commandSystem = new CommandSystem(context);
		commandSystem.addRootCommands(
				new UserDatabaseCommand(),
				new ModDatabaseCommand(),
				new SettlementDatabaseCommand(),
				new SaveAllCommand()
		);
	}

	/**
	 * @throws Exception 
	 * 
	 */
	private static void setupUserDatabase() throws Exception {
		userDb = new UserDatabase(root.resolve("users"));
		
		log("Loading user database");
		userDb.load();
		log("User database loaded %d entries", userDb.size());
	}
	
	/**
	 * 
	 * @throws Exception
	 */
	private static void setupModDatabase() throws Exception {
		modDb = new ModDatabase(root.resolve("mods"));
		
		log("Loading modification database");
		modDb.load();
		log("Modification database loaded %d entries", modDb.size());
	}
	
	/**
	 * 
	 * @throws Exception
	 */
	private static void setupSettlementDatabase() throws Exception {
		settlementDb = new SettlementDatabase(root.resolve("settlements"));
		
		log("Loading settlement database");
		settlementDb.load();
		log("Settlement database loaded %d entries", settlementDb.size());
	}

	/**
	 * 
	 */
	public static void startServer() throws Exception {
		server = new Server();
		server.start(21996);
	}
	
	public static void saveAll(CommandRunner runner) throws IOException {
		runner.respond("Saving user database");
		getUsers().save(runner);
		runner.respond("User database saved %d users", getUsers().size());
		runner.respond("Saving modification database");
		getMods().save(runner);
		runner.respond("Modification database saved %d modifications", getMods().size());
		runner.respond("Saving settlement database");
		getSettlements().save(runner);
		runner.respond("Settlement database saved %d settlements", getSettlements().size());
	}

	public static UserDatabase getUsers() {
		return userDb;
	}
	
	public static ModDatabase getMods() {
		return modDb;
	}
	
	public static SettlementDatabase getSettlements() {
		return settlementDb;
	}
	
	/**
	 * @return the commandSystem
	 */
	public static CommandSystem getCommandSystem() {
		return commandSystem;
	}
	
	private static class SaveAllCommand extends Command {
		
		public SaveAllCommand() {
			super(new String[] {"save", "saveall"}, "", "Saves all databases");
		}
	
		/**
		 * @see ru.windcorp.jputil.cmd.Command#run(ru.windcorp.jputil.cmd.Invocation)
		 */
		@Override
		public void run(Invocation inv) throws CommandExceptions {
			try {
				saveAll(inv.getRunner());
			} catch (IOException e) {
				throw new CommandErrorException(inv, "An IO problem prevented database save", e);
			}
		}
	
	}

}
