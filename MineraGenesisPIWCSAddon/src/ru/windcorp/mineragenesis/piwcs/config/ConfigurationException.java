/* 
 * PIWCS addon for MineraGenesis Minecraft mod
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
package ru.windcorp.mineragenesis.piwcs.config;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

class ConfigurationException extends Exception {

	private static final long serialVersionUID = -6465688194758528868L;
	
	private Map<String, Object> variables;
	private Stack<Object> stack;
	private String token;
	private int tokenNumber;
	
	public ConfigurationException(String message) {
		super(message);
	}

	ConfigurationException(String message, Throwable cause) {
		super(message, cause);
	}
	
	void initDetails(Map<String, Object> variables, Stack<Object> stack,
			String token, int tokenNumber) {
		this.variables = new HashMap<>(variables);
		this.stack = new Stack<>();
		this.stack.addAll(stack);
		this.token = token;
		this.tokenNumber = tokenNumber;
	}

	@Override
	public String getMessage() {
		if (token == null) return super.getMessage();
		return super.getMessage() + " (Token: " + token + " (" + tokenNumber + "); variables: " + variables + "; stack: " + stack + ")";
	}

}
