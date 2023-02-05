package com.jetbrains.filesystem.nodes;

import java.util.ArrayList;
import java.util.List;

public class FileNode extends FileSystemNode {

    private long size;
    private final List<Long> blocks;

    public FileNode(final String name) {
        super(name);
        blocks = new ArrayList<>();
    }

    public long getSize() {
        return size;
    }

    public void updateSize(final int size) {
        this.size = size;
    }

    public List<Long> getBlocks() {
        return blocks;
    }

    @Override
    public String toString() {
        return "FileNode{" +
                "name='" + getName() +
                "', size=" + size +
                ", blocks=" + blocks +
                '}';
    }
}
