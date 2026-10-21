/*
 * Age of Epoch - writability check
 *
 * What it answers
 * ---------------
 * "Is Files.isWritable() telling the truth on this machine?"
 *
 * That single question decides whether Loom can build at all on JDK 25:
 *
 *     // jdk.nio.zipfs.ZipFileSystem, JDK 25
 *     this.readOnly = forceReadOnly || multiReleaseVersion.isPresent() || !Files.isWritable(zfpath);
 *
 * One "false" from Files.isWritable() makes every zip file system read-only, and
 * everything Loom writes into a jar (the client+server merge, jar processors,
 * tiny-remapper's OutputConsumerPath) then throws ReadOnlyFileSystemException.
 * Loom loses those exceptions (its merge submits tasks without keeping the
 * Futures), so all you see is an empty 22 byte minecraft-merged.jar.
 *
 * On the machine this was written for, Files.isWritable() returns false for
 * EVERY path - the project folder, the Loom cache, the jars - while opening the
 * same files for write and creating files in the same folders works fine. That
 * shows differing access-check behavior. Compare another JDK build; security
 * software and process-specific policy are not excluded by this test alone.
 *
 * Usage (works on Java 11+ so an older JDK can be compared with JDK 25):
 *
 *     build.bat jcheck
 *     java tools/WritableCheck.java [path ...]
 *
 * Exit codes: 0 = isWritable is usable, 3 = access check disagrees with actual writes,
 *             2 = writes really are denied, 1 = usage error.
 */

import java.io.File;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

public final class WritableCheck {
	private static final boolean[] flags = new boolean[4]; // [0] lied, [1] denied, [2] checked, [3] writable

	public static void main(String[] args) throws Exception {
		final List<Path> targets = new ArrayList<Path>();

		for (int i = 0; i < args.length; i++) {
			if (args[i] != null && args[i].length() > 0) {
				targets.add(Paths.get(args[i]).toAbsolutePath().normalize());
			}
		}

		if (targets.isEmpty()) {
			targets.add(Paths.get("").toAbsolutePath().normalize());
			targets.add(Paths.get(System.getProperty("java.io.tmpdir")).toAbsolutePath().normalize());
		}

		System.out.println("  java          : " + System.getProperty("java.version")
				+ "  (" + System.getProperty("java.vendor") + ")");
		System.out.println("  java.home     : " + System.getProperty("java.home"));
		System.out.println("  os            : " + System.getProperty("os.name") + " " + System.getProperty("os.version"));

		for (int i = 0; i < targets.size(); i++) {
			check(targets.get(i));
		}

		System.out.println();

		if (!flags[2]) {
			System.out.println("  nothing could be checked (paths missing?)");
			System.exit(1);
		}

		if (flags[0]) {
			System.out.println("  VERDICT: MISREPORT - Files.isWritable is false while actual writes succeed.");
			System.out.println("           JDK 25 zipfs can mark jars at the affected paths read-only,");
			System.out.println("           so Loom cannot produce minecraft-merged.jar with it.");
			System.out.println("           Compare another JDK 25 build; this check alone cannot exclude security software.");
			System.out.println("           Use Microsoft / Corretto / Zulu / Oracle and point JAVA_HOME at it.");
			System.exit(3);
		}

		if (flags[1]) {
			System.out.println("  VERDICT: DENIED - writes really fail here; fix the folder permissions");
			System.out.println("           (icacls \"<dir>\" /grant \"%USERNAME%:(OI)(CI)F\" /T) or the antivirus.");
			System.exit(2);
		}

		if (flags[3]) {
			System.out.println("  VERDICT: OK - all checked paths passed the writability check.");
			System.exit(0);
		}

		System.out.println("  VERDICT: inconclusive");
		System.exit(1);
	}

	private static void check(final Path path) {
		System.out.println();
		System.out.println("  " + path);

		if (!Files.exists(path)) {
			System.out.println("    does not exist");
			flags[1] = true;
			flags[2] = true;
			return;
		}

		flags[2] = true;

		try {
			System.out.println("    real path          : " + path.toRealPath());
		} catch (IOException e) {
			System.out.println("    real path          : " + e);
		}

		final boolean isWritable = Files.isWritable(path);
		final boolean canWrite = new File(path.toString()).canWrite();

		System.out.println("    Files.isWritable   : " + isWritable);
		System.out.println("    File.canWrite()    : " + canWrite);

		if (isWritable) {
			flags[3] = true;
		}

		if (Files.isRegularFile(path)) {
			try {
				System.out.println("    size               : " + Files.size(path) + " bytes");
			} catch (IOException e) {
				System.out.println("    size               : " + e);
			}

			try {
				System.out.println("    dos:readonly       : " + Files.getAttribute(path, "dos:readonly"));
			} catch (Exception e) {
				System.out.println("    dos:readonly       : n/a (" + e.getClass().getSimpleName() + ")");
			}

			try {
				System.out.println("    owner              : " + Files.getOwner(path));
			} catch (Exception e) {
				System.out.println("    owner              : n/a (" + e.getClass().getSimpleName() + ")");
			}

			final boolean opened = openForWrite(path);
			System.out.println("    open for WRITE     : " + (opened ? "OK" : "FAILED"));

			if (!isWritable && opened) {
				flags[0] = true;
			} else if (!opened) {
				flags[1] = true;
			}

			return;
		}

		// Directory: creating a file is what "writable" actually means.
		final boolean created = createIn(path);
		System.out.println("    create+delete file : " + (created ? "OK" : "FAILED"));

		if (!isWritable && created) {
			flags[0] = true;
		} else if (!created) {
			flags[1] = true;
		}
	}

	private static boolean openForWrite(final Path path) {
		try {
			final FileChannel channel = FileChannel.open(path, StandardOpenOption.WRITE, StandardOpenOption.APPEND);
			channel.close();
			return true;
		} catch (Throwable t) {
			System.out.println("      " + t.getClass().getName() + ": " + t.getMessage());
			return false;
		}
	}

	private static boolean createIn(final Path dir) {
		try {
			final Path probe = Files.createTempFile(dir, ".aoe-writable", ".tmp");
			Files.delete(probe);
			return true;
		} catch (Throwable t) {
			System.out.println("      " + t.getClass().getName() + ": " + t.getMessage());
			return false;
		}
	}

	private WritableCheck() {
	}
}
