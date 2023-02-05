package com.jetbrains.filesystem;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Set;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class FileSystemTest {

    private static final String FILE_NAME = "container.txt";

    @Test
    public void shouldCreateOneDirectory() throws Exception {

        try (final FileSystem fileSystem = new FileSystem(FILE_NAME)) {

            fileSystem.createDirectory("/etc");

            final Set<String> firstLevelDirectories = fileSystem.getDirectoryContents("/");
            Assertions.assertEquals(1, firstLevelDirectories.size());
            Assertions.assertTrue(firstLevelDirectories.contains("etc"));

            fileSystem.deleteFileSystem();
        }
    }

    @Test
    public void shouldCreateMultipleDirectories() throws Exception {

        try (final FileSystem fileSystem = new FileSystem(FILE_NAME)) {

            fileSystem.createDirectory("/etc");
            fileSystem.createDirectory("/usr");
            fileSystem.createDirectory("/usr/bin");

            final Set<String> firstLevelDirectories = fileSystem.getDirectoryContents("/");
            Assertions.assertEquals(2, firstLevelDirectories.size());
            Assertions.assertTrue(firstLevelDirectories.contains("etc"));
            Assertions.assertTrue(firstLevelDirectories.contains("usr"));

            final Set<String> secondLevelDirectories = fileSystem.getDirectoryContents("/usr");
            Assertions.assertEquals(1, secondLevelDirectories.size());
            Assertions.assertTrue(secondLevelDirectories.contains("bin"));

            fileSystem.deleteFileSystem();
        }
    }

    @Test
    public void shouldRemoveOneDirectory() throws Exception {

        try (final FileSystem fileSystem = new FileSystem(FILE_NAME)) {

            fileSystem.createDirectory("/etc");

            final Set<String> secondLevelDirectories = fileSystem.getDirectoryContents("/");
            Assertions.assertEquals(1, secondLevelDirectories.size());
            Assertions.assertTrue(secondLevelDirectories.contains("etc"));


            fileSystem.removeDirectory("/etc");

            final Set<String> rootContents = fileSystem.getDirectoryContents("/");
            assertTrue(rootContents.isEmpty());

            fileSystem.deleteFileSystem();
        }
    }

    @Test
    public void shouldRemoveMultipleDirectories() throws Exception {

        try (final FileSystem fileSystem = new FileSystem(FILE_NAME)) {

            fileSystem.createDirectory("/etc");
            fileSystem.createDirectory("/usr");
            final Set<String> secondLevelDirectories = fileSystem.getDirectoryContents("/");
            Assertions.assertEquals(2, secondLevelDirectories.size());
            Assertions.assertTrue(secondLevelDirectories.contains("etc"));
            Assertions.assertTrue(secondLevelDirectories.contains("usr"));

            fileSystem.removeDirectory("/etc");
            fileSystem.removeDirectory("/usr");

            final Set<String> rootContents = fileSystem.getDirectoryContents("/");
            assertTrue(rootContents.isEmpty());

            fileSystem.deleteFileSystem();
        }
    }

    @Test
    public void shouldCreateADirectoryWithOneFile() throws Exception {

        try (final FileSystem fileSystem = new FileSystem(FILE_NAME)) {

            fileSystem.createDirectory("/usr");
            fileSystem.createFile("/usr/a.txt");

            final Set<String> contents = fileSystem.getDirectoryContents("/usr");
            assertEquals(contents.size(), 1);
            assertThat(contents, hasItems("a.txt"));

            fileSystem.deleteFileSystem();
        }
    }

    @Test
    public void shouldCreateADirectoryWithMultipleFiles() throws Exception {

        try (final FileSystem fileSystem = new FileSystem(FILE_NAME)) {

            fileSystem.createDirectory("/usr");
            fileSystem.createFile("/usr/a.txt");
            fileSystem.createFile("/usr/b.txt");

            final Set<String> contents = fileSystem.getDirectoryContents("/usr");
            assertEquals(contents.size(), 2);
            assertThat(contents, hasItems("a.txt", "b.txt"));

            fileSystem.deleteFileSystem();
        }
    }

    @Test
    public void shouldRemoveFiles() throws Exception {

        try (final FileSystem fileSystem = new FileSystem(FILE_NAME)) {

            fileSystem.createDirectory("/usr");
            fileSystem.createFile("/usr/a.txt");
            fileSystem.createFile("/usr/b.txt");
            fileSystem.removeFile("/usr/a.txt");
            fileSystem.removeFile("/usr/b.txt");

            final Set<String> contents = fileSystem.getDirectoryContents("/usr");
            assertEquals(contents.size(), 0);

            fileSystem.deleteFileSystem();
        }
    }

    @Test
    public void shouldMoveFilesToNewDirectories() throws Exception {

        try (final FileSystem fileSystem = new FileSystem(FILE_NAME)) {

            fileSystem.createDirectory("/usr");
            fileSystem.createDirectory("/etc");
            fileSystem.createDirectory("/etc/bin");
            fileSystem.createFile("/a.txt");
            fileSystem.createFile("/b.txt");

            fileSystem.move("/etc/bin", "/usr");
            fileSystem.move("/a.txt", "/usr");
            fileSystem.move("/b.txt", "/usr");

            final Set<String> etcContents = fileSystem.getDirectoryContents("/etc");
            assertEquals(etcContents.size(), 0);

            final Set<String> contents = fileSystem.getDirectoryContents("/usr");
            assertEquals(contents.size(), 3);
            assertThat(contents, hasItems("bin", "a.txt", "b.txt"));

            fileSystem.deleteFileSystem();
        }
    }

    @Test
    public void shouldRenameDirectoryAndFiles() throws Exception {

        try (final FileSystem fileSystem = new FileSystem(FILE_NAME)) {

            fileSystem.createDirectory("/usr");
            fileSystem.createFile("/a.txt");

            fileSystem.rename("/usr", "etc");
            fileSystem.rename("/a.txt", "b.txt");

            final Set<String> contents = fileSystem.getDirectoryContents("/");
            assertEquals(contents.size(), 2);
            assertThat(contents, hasItems("etc", "b.txt"));

            fileSystem.deleteFileSystem();
        }
    }

    @Test
    public void shouldCreateOneFile() throws Exception {

        try (final FileSystem fileSystem = new FileSystem(FILE_NAME)) {

            final String filePath = "/a.txt";
            fileSystem.createFile(filePath);

            fileSystem.writeToFile(filePath, "test12".getBytes());
            long fileSize = fileSystem.getFileSize(filePath);
            assertEquals(fileSize, 6);
            assertEquals("test12", new String(fileSystem.readFile("/a.txt")));


            fileSystem.writeToFile(filePath, "abc".getBytes());
            fileSize = fileSystem.getFileSize(filePath);
            assertEquals(fileSize, 3);
            assertEquals("abc", new String(fileSystem.readFile("/a.txt")));

            fileSystem.deleteFileSystem();
        }
    }


    @Test
    public void shouldCreateTwoFiles() throws Exception {

        try (final FileSystem fileSystem = new FileSystem(FILE_NAME)) {

            final String firstFile = "/a.txt";
            fileSystem.createFile(firstFile);
            fileSystem.writeToFile(firstFile, "test12".getBytes());


            final String secondFile = "/b.txt";
            fileSystem.createFile(secondFile);
            fileSystem.writeToFile(secondFile, "test93".getBytes());

            fileSystem.removeFile("/b.txt");

            final String thirdFile = "/c.txt";
            fileSystem.createFile(thirdFile);
            fileSystem.writeToFile(thirdFile, "test11".getBytes());


            fileSystem.deleteFileSystem();
        }
    }

    @Test
    public void shouldAppendToFile() throws Exception {

        try (final FileSystem fileSystem = new FileSystem(FILE_NAME)) {

            final String filePath = "/a.txt";
            fileSystem.createFile(filePath);

            fileSystem.appendToFile(filePath, "tes".getBytes());
            fileSystem.appendToFile(filePath, "t12345".getBytes());
            fileSystem.appendToFile(filePath, "6789".getBytes());
            final long fileSize = fileSystem.getFileSize(filePath);
            assertEquals(fileSize, 13);
            assertEquals("test123456789", new String(fileSystem.readFile("/a.txt")));

            fileSystem.deleteFileSystem();
        }
    }

    public static byte[] fileToByteArray(File file) throws IOException {
        return Files.readAllBytes(file.toPath());
    }

    private void checkDirectoryContents(final FileSystem fileSystem,
                                        final File directory,
                                        final String pathPrefix) throws IOException {

        final File[] files = directory.listFiles();

        if (files == null) return;

        for (final File file : files) {

            final String path = pathPrefix + file.getName();
            if (file.isFile()) {

                assertArrayEquals(fileSystem.readFile(path), fileToByteArray(file));
            } else {
                fileSystem.createDirectory(path);
                checkDirectoryContents(fileSystem, file, path + "/");
            }
        }
    }

    private static void addDirectoryContentsToFileSystem(final FileSystem fileSystem,
                                                         final File directory,
                                                         final String pathPrefix) throws IOException {
        final File[] files = directory.listFiles();

        if (files == null) return;

        for (final File file : files) {

            final String path = pathPrefix + file.getName();
            if (file.isFile()) {
                fileSystem.createFile(path);
                fileSystem.writeToFile(path, fileToByteArray(file));
            } else {
                fileSystem.createDirectory(path);
                addDirectoryContentsToFileSystem(fileSystem, file, path + "/");
            }
        }
    }

    private static void removeDirectoryContentsFromFileSystem(final FileSystem fileSystem,
                                                              final File directory,
                                                              final String pathPrefix) throws IOException {
        final File[] files = directory.listFiles();

        if (files == null) return;

        for (final File file : files) {

            final String path = pathPrefix + file.getName();
            if (file.isFile()) {

                fileSystem.removeFile(path);
            } else {
                removeDirectoryContentsFromFileSystem(fileSystem, file, path + "/");
                fileSystem.removeDirectory(path);
            }
        }
    }

    @Test
    public void completeFunctionalTest() throws Exception {

        final File filesystemDirectory = new File("src/main");
        try (final FileSystem fileSystem = new FileSystem(FILE_NAME)) {

            final File sourcesDirectory = new File("src");
            addDirectoryContentsToFileSystem(fileSystem, sourcesDirectory, "/");
            removeDirectoryContentsFromFileSystem(fileSystem, filesystemDirectory, "/main/");
            fileSystem.defragment();

            fileSystem.removeDirectory("/main");
            fileSystem.createDirectory("/newDir");
            addDirectoryContentsToFileSystem(fileSystem, filesystemDirectory, "/newDir/");
        }

        try (final FileSystem fileSystem = new FileSystem(FILE_NAME)) {
            checkDirectoryContents(fileSystem, filesystemDirectory, "/newDir/");
            fileSystem.deleteFileSystem();
        }
    }
}
