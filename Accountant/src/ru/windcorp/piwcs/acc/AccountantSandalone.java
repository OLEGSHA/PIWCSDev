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
package ru.windcorp.piwcs.acc;

import java.io.IOException;
import java.nio.file.Paths;

import ru.windcorp.jputil.cmd.Command;
import ru.windcorp.jputil.cmd.CommandExceptions;
import ru.windcorp.jputil.cmd.Invocation;
import ru.windcorp.jputil.textui.TextUI;
import ru.windcorp.piwcs.acc.Agent.AccessLevel;

/**
 * @author Javapony
 *
 */
public class AccountantSandalone {

	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
		Accountant.setImplementation(() -> System.exit(1), System.out::println, Paths.get("./run"));
		Accountant.start();
		
		Agent agent = new Agent("CONSOLE", AccessLevel.ADMIN) {

			@Override
			public void respond(String msg) {
				System.out.println(msg);
			}

			@Override
			public void complain(String msg) {
				System.out.println("[!] " + msg);
			}
			
		};
		
		Accountant.getCommandSystem().addRootCommands(new Command(new String[] {"quit", "stop", "exit"}, "", "Quits") {

			@Override
			public void run(Invocation inv) throws CommandExceptions {
				try {
					inv.getRunner().respond("Saving databases...");
					Accountant.saveAll(agent);
					System.exit(0);
				} catch (IOException e) {
					e.printStackTrace();
					System.out.println("Still alive");
				}
			}
			
		});
		
		while (true) {
			String cmd = TextUI.readLine();
			agent.runCommand(cmd);
		}
	}

}
