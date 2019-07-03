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

import java.util.Deque;
import java.util.LinkedList;
import java.util.function.Supplier;

import ru.windcorp.jputil.functions.ThrowingRunnable;
import ru.windcorp.jputil.functions.ThrowingSupplier;
import ru.windcorp.piwcs.acc.Agent.AccessLevel;

public class Access {
	
	private static final ThreadLocal<Deque<Agent>> STACKS = ThreadLocal.withInitial(LinkedList::new);
	
	public static boolean hasAccess(AccessLevel lvl) {
		Deque<Agent> stack = STACKS.get();
		
		if (stack.isEmpty()) {
			throw new IllegalStateException("Access not set in thread " + Thread.currentThread());
		}
		
		return stack.peek().hasAccessLevel(lvl);
	}
	
	public static void check(AccessLevel lvl) throws PermissionDenied {
		if (hasAccess(lvl)) {
			throw new PermissionDenied();
		}
	}
	
	public static void start(Agent agent) {
		STACKS.get().push(agent);
	}
	
	public static void end(Agent agent) {
		while (STACKS.get().pop() != agent);
	}
	
	public static <T> T run(Agent agent, Supplier<T> action) {
		try {
			start(agent);
			return action.get();
		} finally {
			end(agent);
		}
	}
	
	public static <T, E extends Exception> T run(Agent agent, ThrowingSupplier<T, E> action) throws E {
		try {
			start(agent);
			return action.get();
		} finally {
			end(agent);
		}
	}
	
	public static void run(Agent agent, Runnable action) {
		try {
			start(agent);
			action.run();
		} finally {
			end(agent);
		}
	}
	
	public static <E extends Exception> void run(Agent agent, ThrowingRunnable<E> action) throws E {
		try {
			start(agent);
			action.run();
		} finally {
			end(agent);
		}
	}
	
	public static <T> T run(Agent agent, AccessLevel lvl, Supplier<T> action) throws PermissionDenied {
		if (agent.hasAccessLevel(lvl)) {
			throw new PermissionDenied();
		}
		
		try {
			start(agent);
			return action.get();
		} finally {
			end(agent);
		}
	}
	
	public static <T, E extends Exception> T run(Agent agent, AccessLevel lvl, ThrowingSupplier<T, E> action) throws E, PermissionDenied {
		if (agent.hasAccessLevel(lvl)) {
			throw new PermissionDenied();
		}
		
		try {
			start(agent);
			return action.get();
		} finally {
			end(agent);
		}
	}
	
	public static void run(Agent agent, AccessLevel lvl, Runnable action) throws PermissionDenied {
		if (agent.hasAccessLevel(lvl)) {
			throw new PermissionDenied();
		}
		
		try {
			start(agent);
			action.run();
		} finally {
			end(agent);
		}
	}
	
	public static <E extends Exception> void run(Agent agent, AccessLevel lvl, ThrowingRunnable<E> action) throws E, PermissionDenied {
		if (agent.hasAccessLevel(lvl)) {
			throw new PermissionDenied();
		}
		try {
			start(agent);
			action.run();
		} finally {
			end(agent);
		}
	}

}
