package com.jetbrains.filesystem.nodes;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class DirectoryNode extends FileSystemNode {

    private final Map<String, FileSystemNode> contents;

    public DirectoryNode(final String name) {
        super(name);
        this.contents = new HashMap<>();
    }

    public void add(final FileSystemNode node) {
        contents.put(node.getName(), node);
    }

    public FileSystemNode getNode(final String directoryName) {

        if (!contents.containsKey(directoryName)) throw new RuntimeException("Could not find " + directoryName);

        return contents.get(directoryName);
    }

    public DirectoryNode getDirectory(final String directoryName) {
        if (!contents.containsKey(directoryName))
            throw new RuntimeException("Could not find the directory " + directoryName);

        final FileSystemNode fileSystemNode = contents.get(directoryName);

        if (fileSystemNode instanceof FileNode) throw new RuntimeException(directoryName + " is a file");

        return (DirectoryNode) fileSystemNode;
    }

    public FileNode getFile(final String fileName) {
        if (!contents.containsKey(fileName))
            throw new RuntimeException("Could not find the file with the name " + fileName);

        final FileSystemNode fileSystemNode = contents.get(fileName);

        if (fileSystemNode instanceof DirectoryNode) throw new RuntimeException(fileName + " is a directory");

        return (FileNode) fileSystemNode;
    }

    public Set<String> getContents() {
        return contents.keySet();
    }

    public boolean isEmpty() {
        return contents.isEmpty();
    }

    public boolean contains(final String name) {
        return contents.containsKey(name);
    }

    public boolean isDirectory(final String name) {
        return contents.get(name) instanceof DirectoryNode;
    }

    public boolean isFile(final String name) {
        return contents.get(name) instanceof FileNode;
    }

    public void remove(final String name) {
        contents.remove(name);
    }

    @Override
    public String toString() {
        return "DirectoryNode{" +
                "name='" + getName() +
                "', contents=" + contents +
                '}';
    }
}
