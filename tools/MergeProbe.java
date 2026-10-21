/*
 * Age of Epoch - Loom merge probe
 *
 * Why this file exists
 * --------------------
 * Loom builds `minecraft-merged.jar` with net.fabricmc.loom.configuration.providers.minecraft
 * .MinecraftJarMerger. Its merge() looks (abridged) like this:
 *
 *     try (ExecutorService service = Executors.newFixedThreadPool(2)) {
 *         service.submit(() -> readToMap(entriesClient, inputClient));
 *         service.submit(() -> readToMap(entriesServer, inputServer));
 *     }
 *     entriesAll.addAll(entriesClient.keySet());
 *     entriesAll.addAll(entriesServer.keySet());
 *     try (ExecutorService service = Executors.newWorkStealingPool()) {
 *         for (String entry : entriesAll) processEntry(entry, service);   // add() is async
 *     }
 *
 * Every task is submitted WITHOUT keeping the Future, so any exception thrown inside
 * readToMap()/add() is discarded. The constructor has already created the (empty) output
 * zip, so the result is a perfectly valid but EMPTY jar - 22 bytes, 0 entries - and nothing
 * is printed. Downstream, remapping that "succeeds", the compile classpath gets a 22 byte
 * jar, javac cannot see net/minecraft/** and the mixin annotation processor dies with
 * "NullPointerException: ... TypeElement.getNestingKind() because typeElement is null".
 *
 * This probe performs the same primitives with the same JDK and prints the real exception.
 *
 * Run it with the project's JDK 25 (single-file source mode, nothing to compile):
 *
 *     build.bat probe
 * or
 *     "%JAVA_HOME%\bin\java" tools\MergeProbe.java
 *
 * It only reads the Loom cache and writes temporary files into build\probe\.
 */

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public final class MergeProbe {
	private static int failures;
	private static int depth;
	private static boolean zipfsReadOnly;

	public static void main(String[] args) throws Exception {
		final Path projectDir = resolveProjectDir();
		final String mcVersion = readProperty(projectDir, "minecraft_version", "26.3");
		final Path gradleHome = resolveGradleHome(projectDir);
		final Path loomDir = gradleHome.resolve("caches").resolve("fabric-loom").resolve(mcVersion);
		final Path workDir = projectDir.resolve("build").resolve("probe");

		header("Age of Epoch - Loom merge probe");
		line("project      : " + projectDir);
		line("gradle home  : " + gradleHome);
		line("minecraft    : " + mcVersion);
		line("loom dir     : " + loomDir);
		line("java         : " + System.getProperty("java.version") + "  (" + System.getProperty("java.home") + ")");
		line("os           : " + System.getProperty("os.name") + " " + System.getProperty("os.version"));
		line("free space   : " + freeMb(projectDir) + " MB on the project drive");

		if (!Files.isDirectory(loomDir)) {
			header("FAILED - Loom cache does not exist");
			line("Expected " + loomDir);
			line("Run a build once (build.bat build) so Loom downloads Minecraft, then retry.");
			System.exit(2);
		}

		Files.createDirectories(workDir);
		line("work dir     : " + workDir);

		// ---------------------------------------------------------------
		header("1. The jars Loom works with");
		reportJars(loomDir);

		// ---------------------------------------------------------------
		header("2. Reading a jar the way Loom's readToMap() does");
		final Path clientJar = loomDir.resolve("minecraft-client.jar");
		final Path serverJar = loomDir.resolve("minecraft-server.jar");
		final Path extractedServerJar = loomDir.resolve("minecraft-extracted_server.jar");

		probe("client jar", clientJar);
		probe("server jar (bundler)", serverJar);

		if (Files.exists(extractedServerJar)) {
			probe("extracted server jar", extractedServerJar);
		}

		// ---------------------------------------------------------------
		header("3. Writing a zip through the same zipfs API Loom uses");
		writeWithZipFileSystem(workDir.resolve("probe-zipfs.jar"));

		// ---------------------------------------------------------------
		header("4. Control: writing the same entries with java.util.zip");
		writeWithZipOutputStream(workDir.resolve("probe-control.jar"));

		// ---------------------------------------------------------------
		header("5. What the file system says about writability");
		describeWritability("probe dir", workDir);
		describeWritability("loom dir", loomDir);
		describeWritability("client jar", clientJar);
		describeWritability("merged jar", loomDir.resolve("minecraft-merged.jar"));

		// ---------------------------------------------------------------
		header("6. Re-opening a copy read-write (JDK 25 accessMode)");
		checkAccessMode(loomDir.resolve("minecraft-merged.jar"), workDir);

		// ---------------------------------------------------------------
		header("7. Result");
		line("  zipfs write test   : " + describe(workDir.resolve("probe-zipfs.jar")));
		line("  control write test : " + describe(workDir.resolve("probe-control.jar")));
		line("  client jar         : " + describe(clientJar));
		line("  server jar         : " + describe(serverJar));

		if (Files.exists(extractedServerJar)) {
			line("  extracted server   : " + describe(extractedServerJar));
		}

		line("");

		if (zipfsReadOnly) {
			line("  Cause found: the JDK zip file system reports the freshly created jar as");
			line("  READ-ONLY, so MinecraftJarMerger.add() cannot write a single entry.");
			line("");
			line("  On JDK 25 ZipFileSystem's constructor ends with");
			line("      this.readOnly = forceReadOnly || multiRelease.isPresent() || !Files.isWritable(zfpath);");
			line("  i.e. one Files.isWritable() returning false is enough to make the zip");
			line("  read-only, and every write then throws ReadOnlyFileSystemException. Loom");
			line("  does not look at its futures, so the exception is lost and the already");
			line("  created file stays empty - the 22 byte minecraft-merged.jar.");
			line("");
			line("  Something on this machine makes a just-created file look non-writable.");
			line("  Check, in this order:");
			line("    1) antivirus / EDR: exclude the project folder and " + gradleHome);
			line("       (Windows Security > Virus & threat protection > Exclusions)");
			line("    2) Ransomware protection > Controlled folder access: allow this JDK's");
			line("       java.exe, or turn the feature off");
			line("    3) backup/sync agents (OneDrive, Dropbox, Acronis...) holding new files");
			line("    4) the folder's permissions or a dos:readonly attribute set by policy");
			line("");
			line("  Until it is fixed, the build can run without the merge:");
			line("      build.bat singlejar client");
			line("  (development workaround: server-side classes are then missing from the classpath)");
		} else if (failures == 0) {
			line("  All primitives work with this JVM.");
			line("  If Loom still writes an empty merged jar, the failure is inside Loom's own");
			line("  merge (a swallowed exception in its async tasks). Attach this output with");
			line("      build.bat build --info --stacktrace > build-log.txt");
		} else {
			line("  Something above failed. The exception printed there is what Loom hides, and");
			line("  it is the reason minecraft-merged.jar comes out empty. Most likely causes:");
			line("    1) a corrupted download - delete the jars listed under section 1 and rerun");
			line("       build.bat repair");
			line("    2) antivirus blocking or deleting zip content - add an exclusion for the");
			line("       project folder and for the Gradle cache (" + gradleHome + ")");
			line("    3) the jar really is empty upstream (section 1 shows 0 entries for a download)");
		}

		System.exit(failures == 0 ? 0 : 1);
	}

	// -----------------------------------------------------------------------
	// Project / cache resolution
	// -----------------------------------------------------------------------

	private static Path resolveProjectDir() {
		final String fromProp = System.getProperty("probe.projectDir");

		if (fromProp != null && fromProp.length() > 0) {
			return Paths.get(fromProp).toAbsolutePath().normalize();
		}

		Path dir = Paths.get("").toAbsolutePath().normalize();

		while (dir != null) {
			if (Files.exists(dir.resolve("gradle.properties")) && Files.exists(dir.resolve("build.gradle"))) {
				return dir;
			}

			dir = dir.getParent();
		}

		return Paths.get("").toAbsolutePath().normalize();
	}

	private static Path resolveGradleHome(final Path projectDir) {
		final String env = System.getenv("GRADLE_USER_HOME");

		if (env != null && env.length() > 0) {
			return Paths.get(env).toAbsolutePath().normalize();
		}

		final Path local = projectDir.resolve(".gradle-home");

		if (Files.isDirectory(local)) {
			return local;
		}

		return Paths.get(System.getProperty("user.home"), ".gradle");
	}

	private static String readProperty(final Path projectDir, final String key, final String fallback) {
		final Path props = projectDir.resolve("gradle.properties");

		if (!Files.exists(props)) {
			return fallback;
		}

		try {
			final List<String> lines = Files.readAllLines(props);

			for (int i = 0; i < lines.size(); i++) {
				final String trimmed = lines.get(i).trim();

				if (trimmed.startsWith("#") || trimmed.indexOf('=') < 0) {
					continue;
				}

				final int eq = trimmed.indexOf('=');

				if (trimmed.substring(0, eq).trim().equals(key)) {
					return trimmed.substring(eq + 1).trim();
				}
			}
		} catch (IOException e) {
			line("  (could not read gradle.properties: " + e + ")");
		}

		return fallback;
	}

	private static long freeMb(final Path path) {
		try {
			return Files.getFileStore(path).getUsableSpace() / (1024L * 1024L);
		} catch (IOException e) {
			return -1L;
		}
	}

	// -----------------------------------------------------------------------
	// Probes
	// -----------------------------------------------------------------------

	private static void reportJars(final Path dir) {
		try {
			final Stream<Path> stream = Files.list(dir);

			try {
				final List<Path> jars = new ArrayList<Path>();
				final java.util.Iterator<Path> it = stream.iterator();

				while (it.hasNext()) {
					final Path path = it.next();

					if (Files.isRegularFile(path) && path.getFileName().toString().endsWith(".jar")) {
						jars.add(path);
					}
				}

				if (jars.isEmpty()) {
					line("  (no jars in " + dir + ")");
				}

				for (int i = 0; i < jars.size(); i++) {
					final Path jar = jars.get(i);
					line("  " + jar.getFileName() + "  " + (Files.size(jar) / 1024L) + " KB");
				}
			} finally {
				stream.close();
			}
		} catch (IOException e) {
			line("  (cannot list " + dir + ": " + e + ")");
		}
	}

	/** Opens the jar as a zip filesystem and walks it, like Loom's readToMap() does. */
	private static void probe(final String what, final Path jar) {
		line("");
		line("  " + what + ": " + jar.getFileName());

		if (!Files.exists(jar)) {
			line("    MISSING - the file does not exist");
			failures++;
			return;
		}

		final long[] counts = new long[2]; // 0 = all entries, 1 = net/minecraft entries
		final String[] firstNames = new String[3];

		try {
			final FileSystem fs = openJar(jar);

			try {
				final Path root = fs.getPath("/");

				Files.walkFileTree(root, new SimpleFileVisitor<Path>() {
					@Override
					public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) {
						if (counts[0] < 3) {
							firstNames[(int) counts[0]] = file.toString();
						}

						counts[0]++;

						if (file.toString().startsWith("/net/minecraft/")) {
							counts[1]++;
						}

						return FileVisitResult.CONTINUE;
					}

					@Override
					public FileVisitResult visitFileFailed(final Path file, final IOException exc) throws IOException {
						// Loom's visitor does not override this, and it never sees the result.
						throw new IOException("walkFileTree could not visit " + file + ": " + exc, exc);
					}
				});
			} finally {
				fs.close();
			}
		} catch (Throwable t) {
			line("    FAILED - this is the exception Loom discards:");
			printThrowable(t, "    ");
			failures++;
			return;
		}

		line("    entries            : " + counts[0]);
		line("    net/minecraft ones : " + counts[1]);

		if (counts[0] > 0 && counts[0] <= 3) {
			for (int i = 0; i < firstNames.length; i++) {
				if (firstNames[i] != null) {
					line("    entry: " + firstNames[i]);
				}
			}
		}

		if (counts[0] == 0) {
			line("    EMPTY - a valid zip with no entries (this is the 22 byte jar)");
			failures++;
		}
	}

	/** Prints what the file system thinks about a path: writable, attributes, ACL. */
	private static void describeWritability(final String what, final Path path) {
		line("");
		line("  " + what + ": " + path);

		if (!Files.exists(path)) {
			line("    does not exist");
			return;
		}

		line("    Files.isWritable   : " + Files.isWritable(path));
		line("    Files.isReadable   : " + Files.isReadable(path));

		if (Files.isRegularFile(path)) {
			try {
				line("    Files.size         : " + Files.size(path) + " bytes");
			} catch (IOException e) {
				line("    Files.size         : " + e);
			}

			try {
				line("    dos:readonly       : " + Files.getAttribute(path, "dos:readonly"));
			} catch (Exception e) {
				line("    dos:readonly       : n/a (" + e.getClass().getSimpleName() + ")");
			}

			// The real question: can this JVM open the file for writing right now?
			try {
				final java.nio.channels.FileChannel channel =
						java.nio.channels.FileChannel.open(path, java.nio.file.StandardOpenOption.WRITE,
								java.nio.file.StandardOpenOption.APPEND);

				channel.close();
				line("    open for WRITE     : OK");
			} catch (IOException e) {
				line("    open for WRITE     : FAILED - " + e);
			}

			return;
		}

		// Directory: the meaningful test is creating a file in it.
		try {
			final Path probe = Files.createTempFile(path, ".aoe-probe", ".tmp");
			Files.delete(probe);
			line("    create+delete file : OK");
		} catch (IOException e) {
			line("    create+delete file : FAILED - " + e);
		}
	}

	/**
	 * Copies the merged jar next to the project and opens the copy with
	 * accessMode=readWrite. On JDK 25 zipfs refuses that loudly ("the ZIP file is
	 * not writable") when Files.isWritable() says false for it - the same state in
	 * which Loom's merge writes nothing.
	 *
	 * A copy is used so that a healthy cache is never rewritten by the probe.
	 */
	private static void checkAccessMode(final Path source, final Path workDir) {
		line("");

		if (!Files.exists(source)) {
			line("  " + source.getFileName() + ": does not exist - nothing to copy");
			return;
		}

		final Path copy = workDir.resolve("probe-accessmode.jar");

		try {
			Files.deleteIfExists(copy);
			Files.copy(source, copy);
			line("  copy of " + source.getFileName() + " -> " + copy + "  (" + describe(copy) + ")");
		} catch (IOException e) {
			line("  cannot copy " + source + ": " + e);
			return;
		}

		final Map<String, Object> env = new HashMap<String, Object>();
		env.put("accessMode", "readWrite");

		try {
			final FileSystem fs = FileSystems.newFileSystem(jarUri(copy), env);

			try {
				line("    accessMode=readWrite : accepted");
				line("    isReadOnly           : " + fs.isReadOnly());

				if (fs.isReadOnly()) {
					zipfsReadOnly = true;
				}

				try {
					final Path marker = fs.getPath("/probe-marker.txt");
					Files.write(marker, "x".getBytes(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
					Files.delete(marker);
					line("    write into the zip   : OK");
				} catch (Throwable t) {
					line("    write into the zip   : FAILED - " + t.getClass().getName() + ": " + t.getMessage());
					zipfsReadOnly = true;
				}
			} finally {
				fs.close();
			}
		} catch (Throwable t) {
			line("    accessMode=readWrite : REJECTED");
			line("    " + t.getClass().getName() + ": " + t.getMessage());
			line("    (JDK 25 throws this when the file is not writable - the same state");
			line("     that makes Loom's merge silently produce an empty jar)");
			zipfsReadOnly = true;
		}
	}

	/** Creates a jar through zipfs, the way MinecraftJarMerger.add() does. */
	private static void writeWithZipFileSystem(final Path out) {
		line("  target: " + out);

		final int expected = 5;

		try {
			Files.deleteIfExists(out);
			Files.createDirectories(out.getParent());

			final Map<String, Object> env = new HashMap<String, Object>();
			env.put("create", "true");

			final FileSystem fs = FileSystems.newFileSystem(jarUri(out), env);

			try {
				for (int i = 0; i < expected; i++) {
					final Path file = fs.getPath("/probe/Entry" + i + ".class");
					Files.createDirectories(file.getParent());
					Files.write(file, ("payload" + i).getBytes(), StandardOpenOption.CREATE_NEW);
					Files.getFileAttributeView(file, BasicFileAttributeView.class).setTimes(null, null, null);
				}

				line("    wrote " + expected + " entries, closing ...");
			} finally {
				// Closing is what writes the central directory of the zip.
				fs.close();
			}
		} catch (Throwable t) {
			line("    FAILED while writing:");
			printThrowable(t, "    ");
			noteReadOnly(t);
			failures++;
			return;
		}

		countAfterWrite(out, expected, "zipfs");
	}

	/** Control: the same content through java.util.zip, to separate zipfs from disk problems. */
	private static void writeWithZipOutputStream(final Path out) {
		line("  target: " + out);

		final int expected = 5;

		try {
			Files.deleteIfExists(out);
			Files.createDirectories(out.getParent());

			final OutputStream os = new FileOutputStream(out.toFile());

			try {
				final ZipOutputStream zos = new ZipOutputStream(os);

				for (int i = 0; i < expected; i++) {
					zos.putNextEntry(new ZipEntry("probe/Entry" + i + ".class"));
					zos.write(("payload" + i).getBytes());
					zos.closeEntry();
				}

				zos.finish();
			} finally {
				os.close();
			}
		} catch (Throwable t) {
			line("    FAILED while writing:");
			printThrowable(t, "    ");
			failures++;
			return;
		}

		countAfterWrite(out, expected, "java.util.zip");
	}

	private static void countAfterWrite(final Path out, final int expected, final String how) {
		long size;

		try {
			size = Files.size(out);
		} catch (IOException e) {
			size = -1L;
		}

		line("    on disk: " + size + " bytes");

		try {
			final FileSystem fs = openJar(out);

			try {
				final long actual = countEntries(fs.getPath("/"));
				line("    reopened: " + actual + " entries (expected " + expected + ")");

				if (actual != expected) {
					line("    [!!] " + how + " wrote an incomplete jar");
					failures++;
				}
			} finally {
				fs.close();
			}
		} catch (Throwable t) {
			line("    [!!] cannot reopen what " + how + " wrote:");
			printThrowable(t, "    ");
			failures++;
		}
	}

	// -----------------------------------------------------------------------
	// Helpers
	// -----------------------------------------------------------------------

	/** Marks the run as "zip refused to write" when the throwable chain says so. */
	private static void noteReadOnly(final Throwable throwable) {
		Throwable current = throwable;
		int guard = 0;

		while (current != null && guard < 12) {
			if (current instanceof java.nio.file.ReadOnlyFileSystemException) {
				zipfsReadOnly = true;
				return;
			}

			current = current.getCause();
			guard++;
		}
	}

	private static FileSystem openJar(final Path jar) throws IOException {
		Throwable first;

		// tiny-remapper's FileSystemReference (used by Loom) opens jars through the URI form.
		try {
			return FileSystems.newFileSystem(jarUri(jar), new HashMap<String, Object>());
		} catch (java.nio.file.FileSystemAlreadyExistsException e) {
			return FileSystems.getFileSystem(jarUri(jar));
		} catch (Throwable e) {
			first = e;
		}

		// Fall back to the path form, keeping both failures visible if this also fails.
		try {
			return FileSystems.newFileSystem(jar, (ClassLoader) null);
		} catch (Throwable second) {
			second.addSuppressed(first);
			throw new IOException("cannot open " + jar + ": " + second, second);
		}
	}

	private static URI jarUri(final Path jar) {
		return URI.create("jar:" + jar.toUri().toString());
	}

	private static long countEntries(final Path root) throws IOException {
		final long[] count = new long[1];

		Files.walkFileTree(root, new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) {
				count[0]++;
				return FileVisitResult.CONTINUE;
			}
		});

		return count[0];
	}

	private static String describe(final Path path) {
		if (!Files.exists(path)) {
			return "missing";
		}

		try {
			final long bytes = Files.size(path);
			long entries = -1L;

			try {
				final FileSystem fs = openJar(path);

				try {
					entries = countEntries(fs.getPath("/"));
				} finally {
					fs.close();
				}
			} catch (Throwable ignored) {
				entries = -1L;
			}

			if (entries < 0) {
				return bytes + " bytes, unreadable";
			}

			return bytes + " bytes, " + entries + " entries";
		} catch (IOException e) {
			return "unreadable: " + e;
		}
	}

	private static void printThrowable(final Throwable throwable, final String indent) {
		Throwable current = throwable;
		depth = 0;

		while (current != null && depth < 12) {
			line(indent + current.getClass().getName() + ": " + current.getMessage());

			final StackTraceElement[] trace = current.getStackTrace();
			final int shown = Math.min(trace.length, 8);

			for (int i = 0; i < shown; i++) {
				line(indent + "    at " + trace[i].toString());
			}

			if (trace.length > shown) {
				line(indent + "    ... " + (trace.length - shown) + " more");
			}

			current = current.getCause();
			depth++;
		}
	}

	private static void header(final String text) {
		System.out.println();
		System.out.println("=== " + text + " " + repeat('=', Math.max(3, 60 - text.length())));
	}

	private static String repeat(final char c, final int count) {
		final StringBuilder sb = new StringBuilder();

		for (int i = 0; i < count; i++) {
			sb.append(c);
		}

		return sb.toString();
	}

	private static void line(final String text) {
		System.out.println(text);
	}

	private MergeProbe() {
	}
}
