package com.jetbrains.filesystem.nodes;

import java.io.Serializable;

public abstract class FileSystemNode implements Serializable {

    private String name;

    protected FileSystemNode(final String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void rename(final String newName) {
        name = newName;
    }
}
