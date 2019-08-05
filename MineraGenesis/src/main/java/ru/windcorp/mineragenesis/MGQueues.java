/* 
 * MineraGenesis Minecraft mod
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
package ru.windcorp.mineragenesis;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import ru.windcorp.mineragenesis.request.ApplicationRequest;
import ru.windcorp.mineragenesis.request.ChunkData;
import ru.windcorp.mineragenesis.request.ChunkDataPool;
import ru.windcorp.mineragenesis.request.ChunkLocator;
import ru.windcorp.mineragenesis.request.GenerationRequest;

import static ru.windcorp.mineragenesis.MineraGenesis.logger;

public class MGQueues {
	
	private static byte generation = 0;
	
	private static final Queue<ChunkLocator> UNHANDLED_REQUEST_QUEUE = new LinkedList<>();
	private static ExecutorService requestQueue = null;
	private static final Queue<ApplicationRequest> APPLICATION_REQUEST_QUEUE = new ConcurrentLinkedQueue<>();
	
	public static void queueImportRequest(int dimension, int chunkX, int chunkZ) {
		if (!MGConfig.isDimensionHandled(dimension)) {
			return;
		}
		
		ChunkLocator chunk = new ChunkLocator(dimension, chunkX, chunkZ);
		if (MGQueueLog.add(chunk)) {
			queueImportRequest(chunk);
		}
	}
	
	static void queueImportRequest(ChunkLocator chunk) {
		logger.debug("Registered import request for chunk %s", chunk);
		UNHANDLED_REQUEST_QUEUE.add(chunk);
	}
	
	public static boolean importNextChunk() {
		if (UNHANDLED_REQUEST_QUEUE.isEmpty()) {
			return false;
		}
		
		ChunkLocator chunk = UNHANDLED_REQUEST_QUEUE.poll();
		
		ChunkData original = ChunkDataPool.getOriginal();
		MineraGenesis.getChunkImporter().importChunkData(chunk, original);
		GenerationRequest request = new GenerationRequest(chunk, original);
		
		logger.debug("Imported chunk %s and registered generation request with ID %d", chunk, request.getRequestId());
		requestQueue.execute(request);
		return true;
	}
	
	public static void queueApplicationRequest(GenerationRequest generationRequest) {
		logger.debug("Processed generation request %d, building application request",
				generationRequest.getRequestId());
		
		ApplicationRequest applicationRequest = MineraGenesis.getApplicationRequestBuilder()
				.buildApplicationRequest(
						generationRequest.getChunk(),
						generationRequest.getPatch(),
						generationRequest.getOriginal());

		logger.debug("Registered application request with ID %d for generation request with ID %d",
				applicationRequest.getRequestId(), generationRequest.getRequestId());
		APPLICATION_REQUEST_QUEUE.add(applicationRequest);
	}
	
	public static boolean exportNextChunk() {
		ApplicationRequest request = APPLICATION_REQUEST_QUEUE.poll();
		
		if (request == null) {
			return false;
		}
		
		MineraGenesis.getChunkExporter().exportChunkData(request);
		MGQueueLog.remove(request.getChunk());
		
		logger.debug("Exported application request with ID %d", request.getRequestId());
		return true;
	}
	
	public static void start() {
		requestQueue = Executors.newWorkStealingPool(MGConfig.getThreads());
		MGQueueLog.onWorldLoaded(MineraGenesis.getHelper().getWorldDataDirectory().resolve("MineraGenesisQueueLog.dat"));
	}
	
	public static void stop() {
		long timeout = MGConfig.getTerminationWaitTimeout();
		
		boolean forceTermination = false;
		if (timeout > 0) {
			logger.logf("Waiting at most %.2f minutes for chunk processing to finish", timeout / (60.0 * 1000.0));
			requestQueue.shutdown();
			try {
				forceTermination = !requestQueue.awaitTermination(timeout, TimeUnit.MILLISECONDS);
			} catch (InterruptedException e) {
				// Continue
			}
		} else {
			forceTermination = true;
		}
		
		if (forceTermination) {
			logger.log("Forcefully terminating chunk processing and abandoning results");
			requestQueue.shutdownNow();
		}
		
		logger.log("Termination complete");
		
		generation++;
		requestQueue = null;
		
		UNHANDLED_REQUEST_QUEUE.clear();
		APPLICATION_REQUEST_QUEUE.clear();
		
		MGQueueLog.onWorldUnloading();
	}
	
	public static byte getGeneration() {
		return generation;
	}

}
