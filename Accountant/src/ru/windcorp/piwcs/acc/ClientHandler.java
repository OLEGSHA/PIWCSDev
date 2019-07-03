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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;

/**
 * @author Javapony
 *
 */
class ClientHandler implements Runnable {
	
	private class NetworkAgent extends Agent {

		/**
		 * @param name
		 * @param accessLevel
		 */
		public NetworkAgent(int id) {
			super("NETWORK-" + id, AccessLevel.ADMIN);
		}
		
		/**
		 * @see ru.windcorp.jputil.cmd.CommandRunner#respond(java.lang.String)
		 */
		@Override
		public void respond(String msg) {
			sendMessage(msg);
		}
		
		/**
		 * @see ru.windcorp.jputil.cmd.CommandRunner#complain(java.lang.String)
		 */
		@Override
		public void complain(String msg) {
			sendMessage("[!] " + msg);
		}
		
	}
	
	private static int nextId = 0;
	
	private final Socket socket;
	private final Agent agent;
	
	private final BufferedReader in;
	private final BufferedWriter out;

	/**
	 * @param socket
	 * @param nextId2 
	 * @throws IOException 
	 */
	public ClientHandler(Socket socket, int id) throws IOException {
		this.socket = socket;
		this.agent = new NetworkAgent(id);
		
		this.in =
				new BufferedReader(
						new InputStreamReader(
								socket.getInputStream(),
								NetworkConstants.CHARSET
						)
				);
		
		this.out =
				new BufferedWriter(
						new OutputStreamWriter(
								socket.getOutputStream(),
								NetworkConstants.CHARSET
						)
				);
	}

	/**
	 * @param msg
	 */
	public void sendMessage(String msg) {
		synchronized (out) {
			try {
				out.write(msg);
				out.newLine();
				out.flush();
			} catch (IOException e) {
				Accountant.reportException(e, "Could not send \"%s\" to client %s", msg, agent);
				closeSocketAndWhine();
			}
		}
	}
	
	private String nextCommand() {
		synchronized (in) {
			try {
				return in.readLine();
			} catch (IOException e) {
				if (!socket.isClosed())
					Accountant.reportException(e, "Could not read next command from client %s", agent);
				closeSocketAndWhine();
				return null;
			}
		}
	}
	
	public void quit() {
		closeSocketAndWhine();
	}
	
	private void closeSocketAndWhine() {
		try {
			if (!socket.isClosed()) socket.close();
		} catch (IOException e) {
			Accountant.reportException(e, "Could not close socket of client %s", agent);
		}
	}

	/**
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
		Accountant.log("Client %s connected from %s", agent, socket);
		
		try {
			String command;
			while ((command = nextCommand()) != null) {
				if (NetworkConstants.QUIT_COMMAND.equals(command)) {
					quit();
				}
				
				agent.runCommand(command);
			}
		} catch (Exception e) {
			Accountant.reportException(e, "Error while handling client %s", agent);
		}

		closeSocketAndWhine();
		Accountant.log("Client %s disconnected", agent);
	}

	/**
	 * @param accept
	 * @throws IOException 
	 */
	public static void handle(Socket socket) throws IOException {
		new Thread(new ClientHandler(socket, nextId), "PIWCS Accountant network client handler thread " + nextId++).start();
	}

}
