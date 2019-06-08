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
