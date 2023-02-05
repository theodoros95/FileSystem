# FileSystem

## Design

### Structure of the container file

* Fixed header length: allocated for the file and directory information
* File blocks: allocated for the file contents
* The header and file block is configurable

[ header | file block 1 | file block 2 | file block 3 | ... ]


### FileSystemNode

An abstract class that contains the common information for files and directories. 
Contains the name.

### FileNode

A class that extends the FileSystemNode and contains:

* The file size
* The file block numbers

### DirectoryNode

A class that extends the FileSystemNode and contain other FileSystemNodes


### File header

The header contains the file and directory nodes which are stored as a tree data structure.

The file header contains the root directory (which points to other FileSystemNodes) and metadata regarding used blocks and free blocks.

Free blocks are stored in a PriorityQueue.
Used blocks are stored into a TreeMap that has the block number as key and the FileNode pointer as value.

Create, write, read, append, delete, rename, move operations are supported.

A routine called defragment is created that moves the file blocks to the beginning of the file (if there are any free blocks)
and shrinks the file size.

The PriorityQueue is used for getting the smallest free block number in O(logN) time.
The TreeMap is used for getting the largest block number in O(logN) time.
These will be extremely useful especially in defragmentation.


## Performance and Scalability Analysis

One of the most CPU intensive methods is the defragment, although with the usage of the appropriate data structures it has been minimized.

The RAM usage is also minimized. 
The FileSystemNode tree is kept in memory along with some other metadata regarding the blocks.
The contents of the files are stored in the container file (disk) and are read on demand.

The header is flushed on disk frequently when a change occurs, to minimize the risk of getting a corrupted container file.


## Design overview and estimates breakdown

* Design and implementation decisions: 1h
* Class diagram: 30 m
* Implementation: 5h
* Testing and bug fixes: 1h 30m


## Future work

* Make the header size expand dynamically if the allocated size is not enough
* Better error handling
* Thread safety
* More in-depth tests
