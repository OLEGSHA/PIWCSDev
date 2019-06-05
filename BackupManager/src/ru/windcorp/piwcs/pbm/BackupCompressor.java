package ru.windcorp.piwcs.pbm;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import static java.nio.file.FileVisitResult.*;

class BackupCompressor extends SimpleFileVisitor<Path> {
	
	private final Path reference, backup;

	public BackupCompressor(Path reference, Path backup) {
		this.reference = reference;
		this.backup = backup;
	}
	
	@Override
	public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
		Path ref = reference.resolve(backup.relativize(file));
		
		if (
				Files.exists(ref) &&
				Files.getLastModifiedTime(ref).equals(attrs.lastModifiedTime()) &&
				Files.size(ref) == attrs.size() &&
				filesAreEqual(ref, file, attrs)
				
				) {
			// hardlink
			hardLink(ref, file);
		}
		
		return CONTINUE;
	}
	
	private static void hardLink(Path source, Path link) throws IOException {
		PBMPlugin.runLinkCommand(source.toString(), link.toString());
	}

	private final byte[] refBuffer = new byte[64 * 1024];
	private final byte[] fileBuffer = new byte[64 * 1024];

	private boolean filesAreEqual(Path ref, Path file,  BasicFileAttributes attrs) throws IOException {
		if (!Files.getLastModifiedTime(ref).equals(attrs.lastModifiedTime())) {
			return false;
		}
		
		if (Files.size(ref) != attrs.size()) {
			return false;
		}
		
		InputStream refIn = Files.newInputStream(ref);
		InputStream fileIn = Files.newInputStream(file);
		
		int read;
		while (true) {
			read = refIn.read(refBuffer);
			if (read <= 0) {
				// File sizes are equal
				return true;
			}
			if (fileIn.read(fileBuffer) != read) {
				// This should never happen, but if it did, the files are different
				return false;
			}
			
			if (!Arrays.equals(refBuffer, fileBuffer)) {
				return false;
			}
		}
	}

}
