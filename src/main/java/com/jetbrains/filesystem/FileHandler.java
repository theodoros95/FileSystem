package com.jetbrains.filesystem;

import com.jetbrains.filesystem.nodes.DirectoryNode;
import com.jetbrains.filesystem.nodes.FileNode;
import com.jetbrains.filesystem.utils.ObjectSerializer;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.*;

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

    public void writeToFile(final FileNode file, final byte[] contents) throws IOException {

        final List<Long> blocks = file.getBlocks();
        if (!blocks.isEmpty()) {
            header.getFreeBlocks().addAll(blocks);
            blocks.forEach(header.getUsedBlocks()::remove);
            blocks.clear();
        }

        blocks.addAll(writeToContainerFile(contents));
        blocks.forEach(block -> header.getUsedBlocks().put(block, file));

        file.updateSize(contents.length);
    }

    private List<Long> writeToContainerFile(final byte[] contents) throws IOException {

        final List<Long> blocks = new ArrayList<>(contents.length / BLOCK_SIZE + 1);

        for (int i = 0; i < contents.length / BLOCK_SIZE; i++) {

            final long freeBlock = getFreeBlock();
            blocks.add(freeBlock);
            moveFilePointerToBlock(freeBlock, 0);
            containerFile.write(contents, i * BLOCK_SIZE, BLOCK_SIZE);
        }

        final int bytesLeft = contents.length % BLOCK_SIZE;

        if (bytesLeft == 0) return blocks;

        final long freeBlock = getFreeBlock();
        blocks.add(freeBlock);
        moveFilePointerToBlock(freeBlock, 0);
        containerFile.write(contents, contents.length - bytesLeft, bytesLeft);

        return blocks;
    }

    private long getFreeBlock() throws IOException {
        if (!header.getFreeBlocks().isEmpty()) {
            return header.getFreeBlocks().remove();
        } else {
            return getNextAvailableBlockNumber();
        }
    }

    private long getNextAvailableBlockNumber() throws IOException {
        return (long) Math.ceil((containerFile.length() - HEADER_SIZE) / (double) BLOCK_SIZE);
    }

    public void appendToFile(final FileNode file, final byte[] contents) throws IOException {

        final List<Long> blocks = file.getBlocks();

        int bytesOccupiedInLastBlock = (int) (file.getSize() % BLOCK_SIZE);

        if (bytesOccupiedInLastBlock != 0) {
            moveFilePointerToBlock(blocks.get(blocks.size() - 1), bytesOccupiedInLastBlock);
            final int bytesToWrite = Math.min(BLOCK_SIZE - bytesOccupiedInLastBlock, contents.length);
            containerFile.write(contents, 0, bytesToWrite);

            int bytesLeft = contents.length - bytesToWrite;
            for (int i = 0; i < bytesLeft / BLOCK_SIZE; i++) {

                final long freeBlock = getFreeBlock();
                blocks.add(freeBlock);
                moveFilePointerToBlock(freeBlock, 0);
                containerFile.write(contents, i * BLOCK_SIZE + bytesToWrite, BLOCK_SIZE);
            }

            bytesLeft %= BLOCK_SIZE;

            if (bytesLeft != 0) {
                final long freeBlock = getFreeBlock();
                blocks.add(freeBlock);
                moveFilePointerToBlock(freeBlock, 0);
                containerFile.write(contents, contents.length - bytesLeft, bytesLeft);
            }

        } else {
            blocks.addAll(writeToContainerFile(contents));
        }

        blocks.forEach(block -> header.getUsedBlocks().put(block, file));

        file.updateSize((int) (file.getSize() + contents.length));
    }

    public byte[] read(final FileNode file) throws IOException {

        final int fileSize = (int) file.getSize();
        byte[] contents = new byte[fileSize];

        final Queue<Long> blocks = new LinkedList<>(file.getBlocks());

        for (int i = 0; i < fileSize / BLOCK_SIZE; i++) {
            final Long poll = Objects.requireNonNull(blocks.poll());
            moveFilePointerToBlock(poll, 0);
            containerFile.read(bytes);

            System.arraycopy(bytes, 0, contents, i * BLOCK_SIZE, BLOCK_SIZE);
        }

        final Long poll = blocks.poll();

        if (poll == null) return contents;

        moveFilePointerToBlock(poll, 0);
        final int bytesLeft = fileSize % BLOCK_SIZE;
        containerFile.read(bytes, 0, bytesLeft);
        System.arraycopy(bytes, 0, contents, fileSize - bytesLeft, bytesLeft);

        return contents;
    }

    public void removeFile(final FileNode file) {
        final List<Long> blocks = file.getBlocks();
        header.getFreeBlocks().addAll(blocks);
        blocks.forEach(header.getUsedBlocks()::remove);
    }

    public void defragment() throws IOException {

        while (!header.getFreeBlocks().isEmpty()) {

            final Long freeBlockNumber = header.getFreeBlocks().remove();

            final Map.Entry<Long, FileNode> blockNumberFileNodeEntry = header.getUsedBlocks().firstEntry();
            if (blockNumberFileNodeEntry == null) break;

            final Long lastUsedBlock = blockNumberFileNodeEntry.getKey();
            if (lastUsedBlock < freeBlockNumber) {
                header.getFreeBlocks().add(freeBlockNumber);
                break;
            }
            final FileNode file = blockNumberFileNodeEntry.getValue();
            final List<Long> fileBlocks = file.getBlocks();
            final int blockIndex = findBlockIndex(lastUsedBlock, fileBlocks);

            moveFilePointerToBlock(lastUsedBlock, 0);
            containerFile.read(bytes);
            moveFilePointerToBlock(freeBlockNumber, 0);
            containerFile.write(bytes);

            header.getUsedBlocks().remove(lastUsedBlock);
            header.getUsedBlocks().put(freeBlockNumber, file);

            fileBlocks.set(blockIndex, freeBlockNumber);
        }

        containerFile.setLength(HEADER_SIZE + (long) header.getUsedBlocks().size() * BLOCK_SIZE);
    }

    private static int findBlockIndex(final Long lastUsedBlock, final List<Long> blocks) {

        for (int i = 0; i < blocks.size(); i++) {
            if (blocks.get(i).equals(lastUsedBlock)) {
                return i;
            }
        }

        throw new RuntimeException("Could not find the block index " + lastUsedBlock);
    }
}
