package com.jetbrains.filesystem;

import com.jetbrains.filesystem.nodes.DirectoryNode;
import com.jetbrains.filesystem.utils.ObjectSerializer;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

public class FileHandler implements AutoCloseable {

    private static final int HEADER_SIZE = 4096;
    private static final int BLOCK_SIZE = 4096;
    private final byte[] headerBlock;
    private final byte[] bytes;
    private final String fileName;
    private final RandomAccessFile containerFile;
    private final FileHeader header;

    public FileHandler(final String fileName) throws Exception {
        this.fileName = fileName;
        this.containerFile = new RandomAccessFile(fileName, "rw");
        this.headerBlock = new byte[HEADER_SIZE];
        this.bytes = new byte[BLOCK_SIZE];
        this.header = open();
    }

    protected DirectoryNode getRoot() {
        return header.getRoot();
    }

    public FileHeader open() throws Exception {

        if (containerFile.length() == 0) {

            final FileHeader fileHeader = new FileHeader(new DirectoryNode(""));

            flushHeaders();

            return fileHeader;
        } else {
            containerFile.seek(0);
            final int read = containerFile.read(headerBlock);
            if (read != HEADER_SIZE) throw new RuntimeException("Something went wrong with the file size");

            return ObjectSerializer.deserializeObject(headerBlock, FileHeader.class);
        }
    }

    public void flushHeaders() throws IOException {

        final byte[] headerBytes = ObjectSerializer.serializeObject(header);

        if (headerBytes.length > HEADER_SIZE)
            throw new RuntimeException("Header is larger than the allocated size of " + HEADER_SIZE);

        containerFile.seek(0);
        containerFile.write(headerBytes);
        containerFile.seek(HEADER_SIZE - 1);
        containerFile.write(0);
    }

    private void moveFilePointerToBlock(final long block, final int offset) throws IOException {
        containerFile.seek(HEADER_SIZE + block * BLOCK_SIZE + offset);
    }

    public void deleteContainerFile() {
        final File f = new File(fileName);
        if (!f.delete()) throw new RuntimeException("Could not delete the container file");
    }

    @Override
    public void close() throws Exception {
        containerFile.close();
    }
}
