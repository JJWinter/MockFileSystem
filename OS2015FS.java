import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import uk.ac.sussex.OS2015.FileSystem;
import uk.ac.sussex.OS2015.hardware.HDD;

public class OS2015FS extends FileSystem {

    /**
     * Author: Candidate number 118466
     *
     * Date: February/March 2015
     */
    protected HDD disk;     //Hard Disc
    int index;              //Pointer Index along disk
    int blockSize;          //Size of blocks in bytes
    int blockIndex;         //Pointer Index along blocks
    double space;           //Percentage of disk reserved for file system
    String[] blocks;        //List of blocks with their assignment
    //List of files present on disk
    ArrayList<File> fileList = new ArrayList<File>();
    int fileListSize;       //Amount of files on disc

    public OS2015FS(HDD storage, int blockSize, double percentage) {
        super(storage);
        disk = storage;
        this.blockSize = blockSize;

        space = percentage / 100;     //Set variable space to be desired percentage

        blocks = new String[(disk.capacity() / blockSize)];         //Set amount of blocks disk is divided into

        System.out.println("-------------------");
        System.out.println("Disk size " + disk.capacity() + " bytes");
        System.out.println(blockSize + " bytes in each block");
        System.out.println((disk.capacity() / blockSize) + " blocks");
        System.out.println("-------------------");




    }

    /**
     * Reset Entirety of disk Allocate a percentage of the disk to the file
     * system management
     *
     * Store filesystem metadata
     */
    @Override
    public void format() {
        disk.reset();
        index = 0;
        blockIndex = 0;

        int filesInCache = fileListSize;

        //Set all blocks to null
        for (int x = 0; x < blocks.length; x++) {
            blocks[x] = null;
        }

        fileList.clear();      //Reset fileList
        //Indicate reservation of desired % of blocks for the filesystem
        for (int i = 0; i < ((disk.capacity() / blockSize) * space); i++) {
            blocks[i] = "Reserved for File System Management";
        }

        disk.seek(0);
        try {
            if (disk.read() != 0) {
                filesInCache = disk.read();
                loadMetadata(filesInCache);
            }
        } catch (HDD.HDDException ex) {
            Logger.getLogger(OS2015FS.class.getName()).log(Level.SEVERE, null, ex);
        }



        disk.seek(disk.capacity() / blockSize);     //Seek disk to first free space
        index = (disk.capacity() / blockSize);      //Seek index to first free space

    }

    /**
     * Read a file from disc
     *
     * @param filename Name of file to return data of
     * @return Data from disc
     */
    @Override
    public byte[] readFile(String filename) {
        System.out.println("Read File: " + filename);
        File file = null;

        //Search for file in filelist
        for (int i = 0; i < fileList.size(); i++) {
            if (fileList.get(i).name == filename) {
                file = fileList.get(i);
            }
        }
        int blocks = file.blockList.size();             //Number of Blocks the file is using

        byte[] data = new byte[blockSize * blocks];     //Array to return 

        //Add the 8 bytes at each block the file uses to the array that will be returned, data
        int fileIndex = 0;
        for (int k = 0; k < blocks; k++) {
            disk.seek((file.blockList.get(k)) * blockSize);
            for (int j = 0; j < blockSize; j++) {
                try {
                    data[fileIndex + j] = disk.read();
                } catch (HDD.HDDException ex) {
                    Logger.getLogger(OS2015FS.class.getName()).log(Level.SEVERE, null, ex);
                }


            }
            fileIndex = fileIndex + blockSize;
        }

        System.out.println("Output: " + (new String(data)));

        return data;
    }

    /**
     * Write a file to disc
     *
     * @param filename Name of file to write
     * @param contents Data to write to disc
     *
     * @return Index location of data
     */
    @Override
    public int newFile(String filename, byte[] contents) {
        File file = new File(filename);
        fileList.add(file);


        //Find out how many blocks the file will use
        int blocksUsed;
        if (contents.length <= blockSize) {
            blocksUsed = 1;
        } else if (contents.length % blockSize == 0) {
            blocksUsed = (contents.length / blockSize);
        } else {
            blocksUsed = (contents.length / blockSize) + 1;
        }

        blockIndex = nextFreeBlock();       //Set file's first block allocation
        index = (blockIndex * blockSize);   //Set file's disk index
        file.setIndex(index);               //Set file's index location

        //Find next available block and fill blocks for length of file
        for (int j = 0; j < blocksUsed; j++) {
            blockIndex = nextFreeBlock();
            blocks[blockIndex] = filename;
            file.blockList.add(blockIndex);
        }

        //Get list of blocks the file will occupy
        int[] blocklist = new int[file.blockList.size()];
        for (int k = 0; k < blocklist.length; k++) {
            blocklist[k] = file.blockList.get(k);
        }

        System.out.println("                       ");
        System.out.println("Write file: " + file.name);
        System.out.println("                       ");


        //Add 8 bytes of the file at a time to blocks
        int fileIndex = 0;
        for (int i = 0; i < blocklist.length; i++) {
            index = (blocklist[i] * blockSize);
            byte[] writeBytes = Arrays.copyOfRange(contents, fileIndex, blockSize + fileIndex);
            try {
                write(index, writeBytes);
            } catch (HDD.HDDException ex) {
                Logger.getLogger(OS2015FS.class.getName()).log(Level.SEVERE, null, ex);
            }
            fileIndex = fileIndex + blockSize;
        }

        for (int x = 0; x < blocks.length; x++) {
            System.out.println("Block" + x + ": " + blocks[x]);
        }

        return file.byteLocation;
    }

    /**
     * Write parameter block to disk
     *
     * @param index Where on disc to write block to
     * @param block Data to write to disc
     * @throws uk.ac.sussex.OS2015.hardware.HDD.HDDException
     */
    public void write(int index, byte[] block) throws HDD.HDDException {
        disk.seek(index);
        for (int i = 0; i < block.length; i++) {
            disk.write(block[i]);
        }
    }

    /**
     * Find the next block with no data in it
     *
     * @return index value of next available block
     */
    public int nextFreeBlock() {
        int block = -1;
        int i = 0;
        while (block == -1 && i < blocks.length) {
            if (blocks[i] == null) {
                block = i;
            } else {
                i++;
            }
        }
        return block;
    }

    /**
     * Delete a file, removing from block list and byte data from disc
     *
     * @param filename Name of file to delete
     */
    @Override
    public void deleteFile(String filename) {
        //Set blocks where filename appears to null
        for (int i = 0; i < blocks.length; i++) {
            if (blocks[i] == (filename)) {
                blocks[i] = null;
            }
        }

        //Remove file from filelist
        for (int j = 0; j < fileList.size(); j++) {
            if (fileList.get(j).name == filename) {
                fileList.remove(j);
            }
        }

        for (int x = 0; x < blocks.length; x++) {
            System.out.println("Block" + x + ": " + blocks[x]);
        }


    }

    /**
     * Print of a representation of all bytes on disk, either as strings or
     * bytes
     */
    @Override
    public void dumpContents() {
        disk.seek(0);       //Seek to start
        byte[] data = new byte[disk.capacity()];
        for (int i = 0; i < disk.capacity(); i++) {

            //Uncomment to Show as Bytes
            try {
                System.out.print(disk.read());
            } catch (HDD.HDDException ex) {
                Logger.getLogger(OS2015FS.class.getName()).log(Level.SEVERE, null, ex);
            }

            //Uncomment to Show as string
//            try {
//                data[i] = disk.read();
//            } catch (HDD.HDDException ex) {
//                Logger.getLogger(OS2015FS.class.getName()).log(Level.SEVERE, null, ex);
//            }

        }
        System.out.println(new String(data));



    }

    /**
     * Before shutdown, store metadata onto disk
     */
    @Override
    public void signalShutdown() {
        disk.reset();           //Reset disk index to 0
        int sIndex = 2;         //Pointer location along file system reservation
        fileListSize = fileList.size();

        //Store values for blockSize and file List size
        try {
            disk.write((byte) blockSize);
            disk.write((byte) fileListSize);
        } catch (HDD.HDDException ex) {
            Logger.getLogger(OS2015FS.class.getName()).log(Level.SEVERE, null, ex);
        }

        //For each file store name of file on Disk along with their block numbers
        for (int i = 0; i < fileList.size(); i++) {
            byte[] fileData;
            //fileData[0] = ((byte) fileList.get(i).name.length());
            fileData = (fileList.get(i).name.getBytes());                   //Store the filename
            try {
                disk.write((byte) fileList.get(i).name.length());           //Store the length of the file's name before the file
                sIndex++;
                write(sIndex, fileData);
            } catch (HDD.HDDException ex) {
                Logger.getLogger(OS2015FS.class.getName()).log(Level.SEVERE, null, ex);
            }

            //Add file's blocks
            byte[] fileBlocks = new byte[fileList.get(i).blockList.size()];
            for (int j = 0; j < fileList.get(i).blockList.size(); j++) {
                fileBlocks[j] = ((fileList.get(i).blockList.get(j)).byteValue());
                //System.out.println("attempting blocks " + fileList.get(i).blockList.get(j));
            }
            try {
                disk.write((byte) fileList.get(i).blockList.size());         //Store the length of the file's blocklist
                sIndex++;
                write(sIndex + fileList.get(i).name.length(), fileBlocks);  //Store the file's blocks
            } catch (HDD.HDDException ex) {
                Logger.getLogger(OS2015FS.class.getName()).log(Level.SEVERE, null, ex);
            }

            sIndex = sIndex + fileList.get(i).name.length() + fileList.get(i).blockList.size();
        }

    }

    /**
     * Loads metadata present on file system partition of disk
     *
     * @param filesInCache Number of files that are on disk
     * @throws uk.ac.sussex.OS2015.hardware.HDD.HDDException
     */
    public void loadMetadata(int filesInCache) throws HDD.HDDException {
        disk.reset();


        blockSize = ((int) disk.read());                //Get blockSize from first byte on disk.
        fileListSize = ((int) disk.read());             //Get fileListSize from second byte on disk
        System.out.println("blocksize: " + blockSize);
        System.out.println("files in cache: " + filesInCache);

        index = 2;

        //For each file in metadata, place into fileList
        for (int x = 0; x < filesInCache; x++) {
            int nameLength = 0;
            nameLength = ((int) disk.read());           //Get the length of the filename
            System.out.println("Length of filename in cache: " + nameLength);

            //Read the filename
            byte[] fileName = new byte[nameLength];
            for (int i = 0; i < nameLength; i++) {
                fileName[i] = (disk.read());
            }
            System.out.println("Filename in cache: " + new String(fileName));

            int amountOfBlocks = 0;
            amountOfBlocks = ((int) disk.read());       //Get the amount of blocks the file uses
            System.out.println("Amount of Blocks file uses: " + amountOfBlocks);

            
            int[] listOfBlocks = new int[amountOfBlocks];
            System.out.print("Blocks the file uses: ");
            for (int j = 0; j < amountOfBlocks; j++) {
                listOfBlocks[j] = (disk.read());        //Get the blocks the file uses
                System.out.print(listOfBlocks[j] + ", ");
            }
            File file = new File(fileName.toString());  
            fileList.add(file);                         //Add the file to the file list
            for (int k = 0; k < listOfBlocks.length; k++) {
                file.blockList.add(listOfBlocks[k]);
            }

            //Re-allocate the file's blocks 
            for (int m = 0; m < amountOfBlocks; m++) {
                blockIndex = file.blockList.get(m);
                blocks[blockIndex] = (new String(fileName));
                file.blockList.add(blockIndex);
            }

        }


    }

    /**
     * Calculates amount of free space on disk
     *
     * @return int amount of space in bytes
     */
    @Override
    public int freeSpace() {
        int freeBlocks = 0;

        for (int i = 0; i < (blocks.length); i++) {
            if (blocks[i] == null) {
                freeBlocks++;
            }

        }
        return freeBlocks * blockSize;

    }
}
