package ru.windcorp.piwcs.pbm;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ProcessHandler {
	
	private static class Execution {
		final Process process;
		final String[] command;
		
		Execution(Process process, String[] command) {
			this.process = process;
			this.command = command;
		}
	}
	
	private volatile boolean shouldRun = false;
	private AtomicBoolean haveProcessesFailed = new AtomicBoolean(false);
	private Thread thread = null;
	private final BlockingQueue<Execution> queue = new LinkedBlockingQueue<>();
	
	private final Lock lock = new ReentrantLock();
	private final Condition processesTerminated = lock.newCondition();
	
	public void run() {
		synchronized (ProcessHandler.class) {
			if (thread != null) {
				throw new IllegalStateException("Already running");
			}
			thread = Thread.currentThread();
		}
		
		while (shouldRun) {
			try {
				Execution exe = queue.take();
				Process process = exe.process;
				process.waitFor();
				int exitValue = process.exitValue();
				if (exitValue != 0) {
					StringBuilder sb = new StringBuilder(exe.command[0]);
					for (int i = 1; i < exe.command.length; ++i) {
						sb.append(exe.command[i]);
						sb.append(' ');
					}
					PBMPlugin.getInst().getLogger().warning("Link command \"" + sb + "\" has terminated with exit value " + exitValue);
					haveProcessesFailed.set(true);
				}
				
				if (queue.isEmpty()) {
					lock.lock();
					try {
						processesTerminated.signalAll();
					} finally {
						lock.unlock();
					}
				}
			} catch (InterruptedException interrupt) {
				// Loop will fail if we want to stop
			}
		}
		
		thread = null;
	}
	
	public void handleCommandExecution(Process process, String[] command) {
		queue.add(new Execution(process, command));
	}
	
	public boolean waitForTermination() throws InterruptedException {
		lock.lock();
		try {
			while (queue.isEmpty()) processesTerminated.await();
		} finally {
			lock.unlock();
		}
		return haveProcessesFailed.getAndSet(false);
	}
	
	public void stop() {
		shouldRun = false;
		thread.interrupt();
	}

}
