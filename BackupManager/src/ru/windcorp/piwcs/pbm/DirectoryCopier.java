package ru.windcorp.piwcs.pbm;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;

import static java.nio.file.FileVisitResult.*;

class DirectoryCopier extends SimpleFileVisitor<Path> {
	
	private final Path source;
	private final Path destination;
	
	public DirectoryCopier(Path source, Path destination) {
		this.source = source;
		this.destination = destination;
	}
	
	@Override
	public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
		if (!dir.equals(source)) {
			Files.copy(dir, destination.resolve(source.relativize(dir)), StandardCopyOption.COPY_ATTRIBUTES);
		}
		return CONTINUE;
	}
	
	@Override
	public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
		Files.copy(file, destination.resolve(source.relativize(file)), StandardCopyOption.COPY_ATTRIBUTES);
		return CONTINUE;
	}
	
}
