/*
 * MineraGenesis Rock Biomes Addon
 * Copyright (C) 2019  Javapony/OLEGSHA
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
package ru.windcorp.mineragenesis.rb.config;

public class ConfigurationException extends Exception {

	private static final long serialVersionUID = -2003761548090165262L;
	
	private String readerSnapshot = null;
	private String variablesSnapshot = null;
	private String stackTraceSnapshot = null;
	private String argumentsSnapshot = null;

	public ConfigurationException(String message, Throwable cause) {
		super(message, cause);
	}
	
	public ConfigurationException(String message) {
		super(message);
	}
	
	void setContext(ConfigReader reader, Arguments arguments, Variables vars, StackTrace stackTrace) {
		if (reader != null && this.readerSnapshot == null) {
			this.readerSnapshot = reader.toString();
		}
		
		if (arguments != null && this.argumentsSnapshot == null) {
			this.argumentsSnapshot = arguments.toString();
		}
		
		if (vars != null && this.variablesSnapshot == null) {
			this.variablesSnapshot = vars.toString();
		}
		
		if (stackTrace != null && this.stackTraceSnapshot == null) {
			this.stackTraceSnapshot = stackTrace.toString();
		}
	}
	
	@Override
	public String getMessage() {
		StringBuilder sb = new StringBuilder(super.getMessage());
		
		if (readerSnapshot != null) {
			sb.append("\nReader:\n");
			sb.append(readerSnapshot);
		}
		
		if (argumentsSnapshot != null) {
			sb.append("\nArguments:\n");
			sb.append(argumentsSnapshot);
		}
		
		if (variablesSnapshot != null) {
			sb.append("\nVariables:\n");
			sb.append(variablesSnapshot);
		}
		
		if (stackTraceSnapshot != null) {
			sb.append("\nParser stack trace:\n");
			sb.append(stackTraceSnapshot);
		}
		
		return sb.toString();
	}

}
