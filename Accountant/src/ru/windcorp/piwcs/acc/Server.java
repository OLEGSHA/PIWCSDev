package ru.windcorp.piwcs.acc;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;

public class Server implements Runnable {
	
	private final Thread thread = new Thread(this, "Accountant Server");
	
	private ServerSocket socket;
	
	private static enum State {
		NOT_STARTED,
		STARTING,
		RUNNING,
		STOPPING,
		STOPPED;
	}

	private int port;
	private volatile State state = State.NOT_STARTED;
	private volatile IOException startFailureReason = null;
	
	public void start(int port) throws IOException {
		synchronized (this) {
			if (state != State.NOT_STARTED) {
				throw new IllegalStateException("Server is in state " + state + ", not " + State.NOT_STARTED);
			}
			
			this.port = port;
			this.state = State.STARTING;
			Accountant.log("Starting server on port %d", port);
			
			thread.start();
			
			try {
				while (state == State.STARTING) wait();
			} catch (InterruptedException e) {
				
			}
			
			if (state != State.RUNNING) {
				throw new IOException("Server is in state " + state + " after " + State.STARTING);
			}
			
			if (startFailureReason != null) {
				state = State.STOPPED;
				throw startFailureReason;
			}
		}
	}
	
	@Override
	public void run() {
		try {
			socket = new ServerSocket(port, 10, InetAddress.getLoopbackAddress());
		} catch (IOException e) {
			startFailureReason = e;
		} finally {
			synchronized (this) {
				state = State.RUNNING;
				notifyAll();
			}
		}
		
		Accountant.log("Server started");
		
		while (state == State.RUNNING) {
			try {
				ClientHandler.handle(socket.accept());
			} catch (Exception e) {
				if (!socket.isClosed())
					Accountant.reportExceptionAndExit(e, "Could not handle new client");
				break;
			}
		}
		
		Accountant.log("Server stopped");
		
		synchronized (this) {
			state = State.STOPPED;
			notifyAll();
		}
	}
	
	public void stop() {
		synchronized (this) {
			if (state == State.STOPPING || state == State.STOPPED) {
				return;
			}
			
			if (socket != null) {
				Accountant.log("Stopping server");
				
				state = State.STOPPED;
				try {
					socket.close();
				} catch (IOException e) {
					Accountant.reportException(e, "Could not close socket");
				}
				notifyAll();
				
				while (state != State.STOPPED) {
					try {
						wait();
					} catch (InterruptedException e) {}
				}
			} else {
				state = State.STOPPED;
			}
		}
	}

}
