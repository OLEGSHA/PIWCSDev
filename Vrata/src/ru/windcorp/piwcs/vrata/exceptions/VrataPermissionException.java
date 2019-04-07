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

package ru.windcorp.piwcs.vrata.exceptions;

import java.util.Objects;

import ru.windcorp.piwcs.vrata.VrataTemplates;

public class VrataPermissionException extends Exception {
	
	private static final long serialVersionUID = -3181709120844515138L;
	
	private final String fullMessage;

	protected VrataPermissionException(String userMessage, String fullMessage) {
		super(userMessage);
		this.fullMessage = fullMessage;
	}
	
	public String getFullMessage() {
		return fullMessage;
	}

	public static VrataPermissionException create(String key, Object[] fullOnlyArgs, Object... args) {
		Objects.requireNonNull(fullOnlyArgs, "fullOnlyArgs is null");
		Objects.requireNonNull(args, "args is null");
		
		Object[] fullArray = new Object[fullOnlyArgs.length + args.length];
		System.arraycopy(args, 0, fullArray, 0, args.length);
		System.arraycopy(fullOnlyArgs, 0, fullArray, args.length, fullOnlyArgs.length);
		
		return new VrataPermissionException(VrataTemplates.getf(key, args), VrataTemplates.getf(key + ".full", fullArray));
	}

}
