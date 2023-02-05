package com.jetbrains.filesystem;

import com.jetbrains.filesystem.nodes.DirectoryNode;
import com.jetbrains.filesystem.nodes.FileNode;
import com.jetbrains.filesystem.nodes.FileSystemNode;

import java.io.IOException;
import java.util.Set;

public class FileSystem implements AutoCloseable {

    private final FileHandler fileHandler;

    public FileSystem(final String fileName) throws Exception {
        fileHandler = new FileHandler(fileName);
    }

    public void deleteFileSystem() throws Exception {
        close();
        fileHandler.deleteContainerFile();
    }

    @Override
    public void close() throws Exception {
        fileHandler.close();
    }

    private void validatePath(final String path) {
        if (!path.startsWith("/")) throw new RuntimeException("The path does not start with '/'");
    }

    public void createDirectory(final String path) throws IOException {

        validatePath(path);

        final String[] directories = path.split("/");
        final DirectoryNode parentDirectory = getPenultimateNode(directories);

        final String newDirectoryName = directories[directories.length - 1];
        if (parentDirectory.contains(newDirectoryName)) {
            if (parentDirectory.isDirectory(newDirectoryName)) return;

            throw new RuntimeException(newDirectoryName + " exists and it is a file");
        }

        final DirectoryNode newDirectory = new DirectoryNode(newDirectoryName);
        parentDirectory.add(newDirectory);

        fileHandler.flushHeaders();
    }

    public Set<String> getDirectoryContents(final String path) {

        validatePath(path);

        final String[] directories = path.split("/");

        DirectoryNode currentDirectory = fileHandler.getRoot();
        for (int i = 1; i < directories.length; i++) {

            final String directoryName = directories[i];
            currentDirectory = currentDirectory.getDirectory(directoryName);
        }

        return currentDirectory.getContents();
    }

    public void removeDirectory(final String path) throws IOException {

        validatePath(path);

        final String[] directories = path.split("/");
        final DirectoryNode parentDirectory = getPenultimateNode(directories);
        final DirectoryNode directoryToRemove = parentDirectory.getDirectory(directories[directories.length - 1]);

        if (!directoryToRemove.isEmpty()) throw new RuntimeException(directoryToRemove.getName() + " is not empty");

        parentDirectory.remove(directoryToRemove.getName());

        fileHandler.flushHeaders();
    }

    private DirectoryNode getPenultimateNode(final String[] namesOfDirectories) {

        DirectoryNode parentDirectory = fileHandler.getRoot();
        for (int i = 1; i < namesOfDirectories.length - 1; i++) {

            final String directoryName = namesOfDirectories[i];
            parentDirectory = parentDirectory.getDirectory(directoryName);
        }

        return parentDirectory;
    }

    private DirectoryNode getLastDirectory(final String[] namesOfDirectories) {

        DirectoryNode parentDirectory = fileHandler.getRoot();
        for (int i = 1; i < namesOfDirectories.length; i++) {

            final String directoryName = namesOfDirectories[i];
            parentDirectory = parentDirectory.getDirectory(directoryName);
        }

        return parentDirectory;
    }

    public void createFile(final String path) throws IOException {

        validatePath(path);

        final String[] directories = path.split("/");
        final DirectoryNode parentDirectory = getPenultimateNode(directories);

        final String newFileName = directories[directories.length - 1];
        if (parentDirectory.contains(newFileName)) {
            if (parentDirectory.isFile(newFileName)) return;

            throw new RuntimeException(newFileName + " exists and it is a directory");
        }

        final FileNode newFile = new FileNode(newFileName);
        parentDirectory.add(newFile);

        fileHandler.flushHeaders();
    }

    public void rename(final String path, final String newName) throws IOException {

        validatePath(path);

        if (newName.contains("/")) throw new RuntimeException(newName + " contains '/'");

        final String[] directories = path.split("/");
        final DirectoryNode parentDirectory = getPenultimateNode(directories);

        if (parentDirectory.contains(newName)) throw new RuntimeException(newName + " already exists");

        final FileSystemNode lastNode = parentDirectory.getNode(directories[directories.length - 1]);
        parentDirectory.remove(lastNode.getName());
        lastNode.rename(newName);
        parentDirectory.add(lastNode);

        fileHandler.flushHeaders();
    }

    public void move(final String path, final String newPath) throws IOException {

        validatePath(path);

        final String[] namesOfDirectories = path.split("/");
        final DirectoryNode parentDirectory = getPenultimateNode(namesOfDirectories);

        final FileSystemNode nodeToMove = parentDirectory.getNode(namesOfDirectories[namesOfDirectories.length - 1]);
        parentDirectory.remove(nodeToMove.getName());

        validatePath(newPath);

        final String[] namesOfNewDirectories = newPath.split("/");
        final DirectoryNode lastDirectory = getLastDirectory(namesOfNewDirectories);
        lastDirectory.add(nodeToMove);

        fileHandler.flushHeaders();
    }

    public long getFileSize(final String path) {

        validatePath(path);

        final String[] nodeNames = path.split("/");
        final DirectoryNode parentDirectory = getPenultimateNode(nodeNames);

        final FileNode file = parentDirectory.getFile(nodeNames[nodeNames.length - 1]);

        return file.getSize();
    }
}
