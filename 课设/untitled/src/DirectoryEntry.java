// DirectoryEntry.java - 修复后
import java.io.*;
import java.nio.charset.StandardCharsets;

public class DirectoryEntry {
    public static final int ENTRY_SIZE = 64;
    public static final int MAX_NAME_LENGTH = 30;

    public enum FileType {
        DIRECTORY, FILE
    }

    private String name;
    private FileType type;
    private int firstBlock;
    private int size;
    private long createTime;
    private long modifyTime;

    public DirectoryEntry() {
        this.name = "";
        this.type = FileType.FILE;
        this.firstBlock = -1;
        this.size = 0;
        this.createTime = System.currentTimeMillis();
        this.modifyTime = System.currentTimeMillis();
    }

    public DirectoryEntry(String name, FileType type, int firstBlock, int size) {
        this.name = name;
        this.type = type;
        this.firstBlock = firstBlock;
        this.size = size;
        this.createTime = System.currentTimeMillis();
        this.modifyTime = System.currentTimeMillis();
    }

    public byte[] toBytes() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);

        try {
            // 写入文件名
            byte[] nameBytes = name.getBytes(StandardCharsets.UTF_8);
            byte[] nameBuffer = new byte[MAX_NAME_LENGTH];
            System.arraycopy(nameBytes, 0, nameBuffer, 0, Math.min(nameBytes.length, MAX_NAME_LENGTH));
            dos.write(nameBuffer);

            // 写入类型
            dos.writeInt(type == FileType.DIRECTORY ? 1 : 0);

            // 写入首块号
            dos.writeInt(firstBlock);

            // 写入大小
            dos.writeInt(size);

            // 写入创建时间
            dos.writeLong(createTime);

            // 写入修改时间
            dos.writeLong(modifyTime);

            // 补齐剩余字节
            byte[] result = baos.toByteArray();
            if (result.length < ENTRY_SIZE) {
                byte[] padded = new byte[ENTRY_SIZE];
                System.arraycopy(result, 0, padded, 0, result.length);
                return padded;
            }
            return result;
        } catch (IOException e) {
            e.printStackTrace();
            return new byte[ENTRY_SIZE];
        }
    }

    public static DirectoryEntry fromBytes(byte[] data) {
        DirectoryEntry entry = new DirectoryEntry();
        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        DataInputStream dis = new DataInputStream(bais);

        try {
            // 读取文件名
            byte[] nameBuffer = new byte[MAX_NAME_LENGTH];
            dis.readFully(nameBuffer);
            String nameStr = new String(nameBuffer, StandardCharsets.UTF_8).trim();
            // 去除空字符
            int nullIndex = nameStr.indexOf('\0');
            if (nullIndex != -1) {
                nameStr = nameStr.substring(0, nullIndex);
            }
            entry.name = nameStr;

            // 读取类型
            int typeInt = dis.readInt();
            entry.type = typeInt == 1 ? FileType.DIRECTORY : FileType.FILE;

            // 读取首块号
            entry.firstBlock = dis.readInt();

            // 读取大小
            entry.size = dis.readInt();

            // 读取创建时间
            entry.createTime = dis.readLong();

            // 读取修改时间
            entry.modifyTime = dis.readLong();

        } catch (IOException e) {
            e.printStackTrace();
        }

        return entry;
    }

    public boolean isEmpty() {
        return name == null || name.isEmpty();
    }

    // Getters and Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public FileType getType() { return type; }
    public void setType(FileType type) { this.type = type; }
    public int getFirstBlock() { return firstBlock; }
    public void setFirstBlock(int firstBlock) { this.firstBlock = firstBlock; }
    public int getSize() { return size; }
    public void setSize(int size) { this.size = size; }
    public long getCreateTime() { return createTime; }
    public long getModifyTime() { return modifyTime; }
    public void setModifyTime(long modifyTime) { this.modifyTime = modifyTime; }
}