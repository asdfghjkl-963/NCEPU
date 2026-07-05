import java.io.*;
import java.util.Arrays;

public class Disk {
    public static final int BLOCK_SIZE = 512;
    public static final int TOTAL_BLOCKS = 100;
    public static final int FAT_SIZE = TOTAL_BLOCKS;
    public static final int ROOT_DIR_BLOCK = 2;

    private byte[][] blocks;
    private short[] fat;
    private String diskFile;

    public Disk(String diskFile) {
        this.diskFile = diskFile;
        blocks = new byte[TOTAL_BLOCKS][BLOCK_SIZE];
        fat = new short[FAT_SIZE];

      
        Arrays.fill(fat, (short)-1);
     
        fat[ROOT_DIR_BLOCK] = -2;

        loadFromFile();
    }

 
    private void initializeDisk() {
      
        for (int i = 0; i < TOTAL_BLOCKS; i++) {
            Arrays.fill(blocks[i], (byte)0);
        }

       
        Arrays.fill(fat, (short)-1);

       
        fat[0] = -2;
        fat[1] = -2;

      
        fat[ROOT_DIR_BLOCK] = -2;

       
        writeDirectory(ROOT_DIR_BLOCK, new java.util.ArrayList<>());
    }

    public void loadFromFile() {
        File file = new File(diskFile);
        if (!file.exists()) {
            initializeDisk();
            saveToFile();
            return;
        }

        try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
      
            for (int i = 0; i < FAT_SIZE; i++) {
                fat[i] = raf.readShort();
            }

        
            for (int i = 0; i < TOTAL_BLOCKS; i++) {
                raf.readFully(blocks[i]);
            }
        } catch (IOException e) {
            e.printStackTrace();
      
            initializeDisk();
            saveToFile();
        }
    }

    public void saveToFile() {
        try (RandomAccessFile raf = new RandomAccessFile(diskFile, "rw")) {
         
            for (int i = 0; i < FAT_SIZE; i++) {
                raf.writeShort(fat[i]);
            }

            
            for (int i = 0; i < TOTAL_BLOCKS; i++) {
                raf.write(blocks[i]);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public short readFAT(int index) {
        return fat[index];
    }

    public void writeFAT(int index, short value) {
        fat[index] = value;
    }

    public byte[] readBlock(int blockNum) {
        return blocks[blockNum];
    }

    public void writeBlock(int blockNum, byte[] data) {
        System.arraycopy(data, 0, blocks[blockNum], 0, Math.min(data.length, BLOCK_SIZE));
    }

    public int allocateBlock() {
        for (int i = 3; i < TOTAL_BLOCKS; i++) {
            if (fat[i] == -1) {
                fat[i] = -2;
                return i;
            }
        }
        return -1;
    }

    public void freeBlocks(int startBlock) {
        int current = startBlock;
        while (current != -1 && current != -2) {
            int next = fat[current];
            fat[current] = -1;
      
            Arrays.fill(blocks[current], (byte)0);
            current = next;
        }
    }

    public short[] getFAT() {
        return fat;
    }


    public java.util.List<DirectoryEntry> readDirectory(int blockNum) {
        java.util.List<DirectoryEntry> entries = new java.util.ArrayList<>();
        byte[] blockData = readBlock(blockNum);

        int offset = 0;
        while (offset + DirectoryEntry.ENTRY_SIZE <= BLOCK_SIZE) {
            byte[] entryData = new byte[DirectoryEntry.ENTRY_SIZE];
            System.arraycopy(blockData, offset, entryData, 0, DirectoryEntry.ENTRY_SIZE);
            DirectoryEntry entry = DirectoryEntry.fromBytes(entryData);
            if (!entry.isEmpty()) {
                entries.add(entry);
            }
            offset += DirectoryEntry.ENTRY_SIZE;
        }
        return entries;
    }

    
    public void writeDirectory(int blockNum, java.util.List<DirectoryEntry> entries) {
        byte[] blockData = new byte[BLOCK_SIZE];
        int offset = 0;

        for (DirectoryEntry entry : entries) {
            if (offset + DirectoryEntry.ENTRY_SIZE > BLOCK_SIZE) {
                break;
            }
            byte[] entryData = entry.toBytes();
            System.arraycopy(entryData, 0, blockData, offset, DirectoryEntry.ENTRY_SIZE);
            offset += DirectoryEntry.ENTRY_SIZE;
        }

        writeBlock(blockNum, blockData);
    }
}
