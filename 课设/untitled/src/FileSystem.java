import java.util.*;

public class FileSystem {
    private Disk disk;
    private String currentPath;
    private Map<String, Integer> pathToBlock;

    public FileSystem(String diskFile) {
        this.disk = new Disk(diskFile);
        this.currentPath = "/";
        this.pathToBlock = new HashMap<>();
        pathToBlock.put("/", Disk.ROOT_DIR_BLOCK);
        loadDirectoryCache("/", Disk.ROOT_DIR_BLOCK);
    }

    private void loadDirectoryCache(String path, int blockNum) {
        List<DirectoryEntry> entries = disk.readDirectory(blockNum);
        for (DirectoryEntry entry : entries) {
            if (entry.getType() == DirectoryEntry.FileType.DIRECTORY) {
                String subPath = path.equals("/") ? "/" + entry.getName() : path + "/" + entry.getName();
                pathToBlock.put(subPath, entry.getFirstBlock());
                loadDirectoryCache(subPath, entry.getFirstBlock());
            }
        }
    }

    public List<DirectoryEntry> readDirectory(int blockNum) {
        return disk.readDirectory(blockNum);
    }

    private void writeDirectory(int blockNum, List<DirectoryEntry> entries) {
        disk.writeDirectory(blockNum, entries);
        disk.saveToFile();
    }

    public boolean createDirectory(String name) {
        if (name == null || name.isEmpty() || name.contains("/")) {
            return false;
        }

        int parentBlock = pathToBlock.get(currentPath);
        List<DirectoryEntry> parentEntries = readDirectory(parentBlock);

        // 检查是否已存在
        for (DirectoryEntry entry : parentEntries) {
            if (entry.getName().equals(name)) {
                return false;
            }
        }

        // 分配新目录块
        int newBlock = disk.allocateBlock();
        if (newBlock == -1) {
            return false;
        }

        // 创建目录项
        DirectoryEntry newEntry = new DirectoryEntry(name, DirectoryEntry.FileType.DIRECTORY, newBlock, 0);
        parentEntries.add(newEntry);
        writeDirectory(parentBlock, parentEntries);

        // 更新缓存
        String newPath = currentPath.equals("/") ? "/" + name : currentPath + "/" + name;
        pathToBlock.put(newPath, newBlock);

        // 初始化空目录
        writeDirectory(newBlock, new ArrayList<>());

        disk.saveToFile();
        return true;
    }

    public boolean createFile(String name, String content) {
        if (name == null || name.isEmpty() || name.contains("/")) {
            return false;
        }

        int parentBlock = pathToBlock.get(currentPath);
        List<DirectoryEntry> parentEntries = readDirectory(parentBlock);

        // 检查是否已存在
        for (DirectoryEntry entry : parentEntries) {
            if (entry.getName().equals(name)) {
                return false;
            }
        }

        // 分配数据块
        byte[] contentBytes = content.getBytes();
        int numBlocks = (contentBytes.length + Disk.BLOCK_SIZE - 1) / Disk.BLOCK_SIZE;
        List<Integer> blocks = new ArrayList<>();

        for (int i = 0; i < numBlocks; i++) {
            int block = disk.allocateBlock();
            if (block == -1) {
                // 回滚已分配的块
                for (int allocated : blocks) {
                    disk.freeBlocks(allocated);
                }
                return false;
            }
            blocks.add(block);
        }

        // 写入数据
        int offset = 0;
        for (int i = 0; i < blocks.size(); i++) {
            int blockNum = blocks.get(i);
            int length = Math.min(Disk.BLOCK_SIZE, contentBytes.length - offset);
            byte[] blockData = new byte[Disk.BLOCK_SIZE];
            System.arraycopy(contentBytes, offset, blockData, 0, length);
            disk.writeBlock(blockNum, blockData);
            offset += length;
        }

        // 设置FAT链
        for (int i = 0; i < blocks.size(); i++) {
            int current = blocks.get(i);
            if (i < blocks.size() - 1) {
                disk.writeFAT(current, (short)(int)blocks.get(i + 1));
            } else {
                disk.writeFAT(current, (short)-2);
            }
        }

        // 创建目录项
        int firstBlock = blocks.get(0);
        DirectoryEntry newEntry = new DirectoryEntry(name, DirectoryEntry.FileType.FILE, firstBlock, contentBytes.length);
        parentEntries.add(newEntry);
        writeDirectory(parentBlock, parentEntries);

        disk.saveToFile();
        return true;
    }

    public boolean deleteFile(String name) {
        int parentBlock = pathToBlock.get(currentPath);
        List<DirectoryEntry> parentEntries = readDirectory(parentBlock);

        int index = -1;
        for (int i = 0; i < parentEntries.size(); i++) {
            if (parentEntries.get(i).getName().equals(name) && parentEntries.get(i).getType() == DirectoryEntry.FileType.FILE) {
                index = i;
                break;
            }
        }

        if (index == -1) {
            return false;
        }

        DirectoryEntry entry = parentEntries.get(index);
        // 释放数据块
        disk.freeBlocks(entry.getFirstBlock());

        // 删除目录项
        parentEntries.remove(index);
        writeDirectory(parentBlock, parentEntries);

        disk.saveToFile();
        return true;
    }

    public boolean deleteDirectory(String name) {
        int parentBlock = pathToBlock.get(currentPath);
        List<DirectoryEntry> parentEntries = readDirectory(parentBlock);

        int index = -1;
        for (int i = 0; i < parentEntries.size(); i++) {
            if (parentEntries.get(i).getName().equals(name) && parentEntries.get(i).getType() == DirectoryEntry.FileType.DIRECTORY) {
                index = i;
                break;
            }
        }

        if (index == -1) {
            return false;
        }

        DirectoryEntry entry = parentEntries.get(index);
        // 检查目录是否为空
        List<DirectoryEntry> dirEntries = readDirectory(entry.getFirstBlock());
        if (!dirEntries.isEmpty()) {
            return false; // 目录非空，不能删除
        }

        // 释放目录块
        disk.freeBlocks(entry.getFirstBlock());

        // 删除目录项
        parentEntries.remove(index);
        writeDirectory(parentBlock, parentEntries);

        // 更新缓存
        String dirPath = currentPath.equals("/") ? "/" + name : currentPath + "/" + name;
        pathToBlock.remove(dirPath);

        disk.saveToFile();
        return true;
    }

    public boolean copyFile(String sourceName, String destName) {
        if (sourceName == null || destName == null ||
                sourceName.isEmpty() || destName.isEmpty()) {
            return false;
        }

        int parentBlock = pathToBlock.get(currentPath);
        List<DirectoryEntry> parentEntries = readDirectory(parentBlock);

        // 查找源文件
        DirectoryEntry sourceEntry = null;
        for (DirectoryEntry entry : parentEntries) {
            if (entry.getName().equals(sourceName) &&
                    entry.getType() == DirectoryEntry.FileType.FILE) {
                sourceEntry = entry;
                break;
            }
        }

        if (sourceEntry == null) {
            return false;
        }

        // 检查目标文件是否已存在
        for (DirectoryEntry entry : parentEntries) {
            if (entry.getName().equals(destName)) {
                return false;
            }
        }

        // 读取源文件内容
        String content = readFileContent(sourceName);
        if (content == null) {
            return false;
        }

        // 创建新文件
        return createFile(destName, content);
    }

    public boolean copyDirectory(String sourceName, String destName) {
        if (sourceName == null || destName == null ||
                sourceName.isEmpty() || destName.isEmpty()) {
            return false;
        }

        int parentBlock = pathToBlock.get(currentPath);
        List<DirectoryEntry> parentEntries = readDirectory(parentBlock);

        // 查找源目录
        DirectoryEntry sourceEntry = null;
        for (DirectoryEntry entry : parentEntries) {
            if (entry.getName().equals(sourceName) &&
                    entry.getType() == DirectoryEntry.FileType.DIRECTORY) {
                sourceEntry = entry;
                break;
            }
        }

        if (sourceEntry == null) {
            return false;
        }

        // 检查目标目录是否已存在
        for (DirectoryEntry entry : parentEntries) {
            if (entry.getName().equals(destName)) {
                return false;
            }
        }

        // 保存当前路径
        String originalPath = currentPath;

        try {
            // 创建目标目录
            if (!createDirectory(destName)) {
                return false;
            }

            // 进入源目录
            if (!changeDirectory(sourceName)) {
                return false;
            }
            List<DirectoryEntry> sourceEntries = getCurrentDirectoryEntries();

            // 进入目标目录
            if (!changeDirectory("..")) {
                return false;
            }
            if (!changeDirectory(destName)) {
                return false;
            }

            // 复制所有文件和子目录
            for (DirectoryEntry entry : sourceEntries) {
                if (entry.getType() == DirectoryEntry.FileType.FILE) {
                    // 复制文件
                    copyFile(entry.getName(), entry.getName());
                } else if (entry.getType() == DirectoryEntry.FileType.DIRECTORY) {
                    // 递归复制子目录
                    copyDirectory(entry.getName(), entry.getName());
                }
            }

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            // 返回原路径
            changeDirectory(originalPath);
        }
    }

    public String readFileContent(String name) {
        int parentBlock = pathToBlock.get(currentPath);
        List<DirectoryEntry> parentEntries = readDirectory(parentBlock);

        DirectoryEntry fileEntry = null;
        for (DirectoryEntry entry : parentEntries) {
            if (entry.getName().equals(name) && entry.getType() == DirectoryEntry.FileType.FILE) {
                fileEntry = entry;
                break;
            }
        }

        if (fileEntry == null) {
            return null;
        }

        StringBuilder content = new StringBuilder();
        int currentBlock = fileEntry.getFirstBlock();
        int bytesRead = 0;

        while (currentBlock != -1 && currentBlock != -2) {
            byte[] blockData = disk.readBlock(currentBlock);
            int length = Math.min(Disk.BLOCK_SIZE, fileEntry.getSize() - bytesRead);
            content.append(new String(blockData, 0, length));
            bytesRead += length;
            currentBlock = disk.readFAT(currentBlock);
        }

        return content.toString();
    }

    public boolean changeDirectory(String path) {
        if (path == null || path.isEmpty()) {
            return false;
        }

        if (path.equals("..")) {
            if (currentPath.equals("/")) {
                return false;
            }
            int lastSlash = currentPath.lastIndexOf('/');
            currentPath = lastSlash == 0 ? "/" : currentPath.substring(0, lastSlash);
            return true;
        }

        String newPath;
        if (path.startsWith("/")) {
            newPath = path;
        } else {
            newPath = currentPath.equals("/") ? "/" + path : currentPath + "/" + path;
        }

        if (pathToBlock.containsKey(newPath)) {
            currentPath = newPath;
            return true;
        }

        return false;
    }

    public String getCurrentPath() {
        return currentPath;
    }

    public List<DirectoryEntry> getCurrentDirectoryEntries() {
        int blockNum = pathToBlock.get(currentPath);
        return readDirectory(blockNum);
    }

    public short[] getFAT() {
        return disk.getFAT();
    }

    public Disk getDisk() {
        return disk;
    }
}