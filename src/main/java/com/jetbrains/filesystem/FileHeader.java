package com.jetbrains.filesystem;

import com.jetbrains.filesystem.nodes.DirectoryNode;
import com.jetbrains.filesystem.nodes.FileNode;

import java.io.Serializable;
import java.util.PriorityQueue;
import java.util.TreeMap;

public class FileHeader implements Serializable {

    private final DirectoryNode root;
    private final PriorityQueue<Long> freeBlocks;
    private final TreeMap<Long, FileNode> usedBlocks;

    public FileHeader(final DirectoryNode root) {
        this.root = root;
        this.freeBlocks = new PriorityQueue<>();
        this.usedBlocks = new TreeMap<>();
    }

    public DirectoryNode getRoot() {
        return root;
    }

    public PriorityQueue<Long> getFreeBlocks() {
        return freeBlocks;
    }

    public TreeMap<Long, FileNode> getUsedBlocks() {
        return usedBlocks;
    }
}
