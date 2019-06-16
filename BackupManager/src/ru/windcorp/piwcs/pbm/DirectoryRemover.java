/* 
 * PIWCS Backup Manager (PBM)
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
 */
package ru.windcorp.piwcs.pbm;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

import static java.nio.file.FileVisitResult.*;

class DirectoryRemover extends SimpleFileVisitor<Path> {
	
	private static final DirectoryRemover INST = new DirectoryRemover();
	
	public static DirectoryRemover get() {
		return INST;
	}
	
	private DirectoryRemover() {}
	
	@Override
	public FileVisitResult postVisitDirectory(Path dir, IOException e) throws IOException {
		if (e != null) throw e;
		Files.delete(dir);
		return CONTINUE;
	}
	
	@Override
	public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
		Files.delete(file);
		return CONTINUE;
	}
	
}
