// Disk.java - 修复后的完整版本
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

        // 初始化FAT，-1表示空闲，-2表示结束
        Arrays.fill(fat, (short)-1);
        // 标记根目录
        fat[ROOT_DIR_BLOCK] = -2;

        loadFromFile();
    }

    // 添加这个方法
    private void initializeDisk() {
        // 初始化所有数据块为0
        for (int i = 0; i < TOTAL_BLOCKS; i++) {
            Arrays.fill(blocks[i], (byte)0);
        }

        // 初始化FAT表
        Arrays.fill(fat, (short)-1);

        // FAT表占用前2块，标记为已使用
        fat[0] = -2;
        fat[1] = -2;

        // 根目录占用第2块
        fat[ROOT_DIR_BLOCK] = -2;

        // 初始化根目录为空目录
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
            // 读取FAT表
            for (int i = 0; i < FAT_SIZE; i++) {
                fat[i] = raf.readShort();
            }

            // 读取数据块
            for (int i = 0; i < TOTAL_BLOCKS; i++) {
                raf.readFully(blocks[i]);
            }
        } catch (IOException e) {
            e.printStackTrace();
            // 如果读取失败，重新初始化
            initializeDisk();
            saveToFile();
        }
    }

    public void saveToFile() {
        try (RandomAccessFile raf = new RandomAccessFile(diskFile, "rw")) {
            // 写入FAT表
            for (int i = 0; i < FAT_SIZE; i++) {
                raf.writeShort(fat[i]);
            }

            // 写入数据块
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
            // 清空数据块
            Arrays.fill(blocks[current], (byte)0);
            current = next;
        }
    }

    public short[] getFAT() {
        return fat;
    }

    // 添加读目录方法
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

    // 添加写目录方法
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