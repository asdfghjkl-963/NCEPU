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

        
        for (DirectoryEntry entry : parentEntries) {
            if (entry.getName().equals(name)) {
                return false;
            }
        }

       
        int newBlock = disk.allocateBlock();
        if (newBlock == -1) {
            return false;
        }

   
        DirectoryEntry newEntry = new DirectoryEntry(name, DirectoryEntry.FileType.DIRECTORY, newBlock, 0);
        parentEntries.add(newEntry);
        writeDirectory(parentBlock, parentEntries);

   
        String newPath = currentPath.equals("/") ? "/" + name : currentPath + "/" + name;
        pathToBlock.put(newPath, newBlock);

    
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

     
        for (DirectoryEntry entry : parentEntries) {
            if (entry.getName().equals(name)) {
                return false;
            }
        }

      
        byte[] contentBytes = content.getBytes();
        int numBlocks = (contentBytes.length + Disk.BLOCK_SIZE - 1) / Disk.BLOCK_SIZE;
        List<Integer> blocks = new ArrayList<>();

        for (int i = 0; i < numBlocks; i++) {
            int block = disk.allocateBlock();
            if (block == -1) {
           
                for (int allocated : blocks) {
                    disk.freeBlocks(allocated);
                }
                return false;
            }
            blocks.add(block);
        }

      
        int offset = 0;
        for (int i = 0; i < blocks.size(); i++) {
            int blockNum = blocks.get(i);
            int length = Math.min(Disk.BLOCK_SIZE, contentBytes.length - offset);
            byte[] blockData = new byte[Disk.BLOCK_SIZE];
            System.arraycopy(contentBytes, offset, blockData, 0, length);
            disk.writeBlock(blockNum, blockData);
            offset += length;
        }

      
        for (int i = 0; i < blocks.size(); i++) {
            int current = blocks.get(i);
            if (i < blocks.size() - 1) {
                disk.writeFAT(current, (short)(int)blocks.get(i + 1));
            } else {
                disk.writeFAT(current, (short)-2);
            }
        }

      
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

        disk.freeBlocks(entry.getFirstBlock());

    
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

        List<DirectoryEntry> dirEntries = readDirectory(entry.getFirstBlock());
        if (!dirEntries.isEmpty()) {
            return false; 
        }

        
        disk.freeBlocks(entry.getFirstBlock());

        
        parentEntries.remove(index);
        writeDirectory(parentBlock, parentEntries);

    
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

      
        for (DirectoryEntry entry : parentEntries) {
            if (entry.getName().equals(destName)) {
                return false;
            }
        }

      
        String content = readFileContent(sourceName);
        if (content == null) {
            return false;
        }

        
        return createFile(destName, content);
    }

    public boolean copyDirectory(String sourceName, String destName) {
        if (sourceName == null || destName == null ||
                sourceName.isEmpty() || destName.isEmpty()) {
            return false;
        }

        int parentBlock = pathToBlock.get(currentPath);
        List<DirectoryEntry> parentEntries = readDirectory(parentBlock);

     
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

   
        for (DirectoryEntry entry : parentEntries) {
            if (entry.getName().equals(destName)) {
                return false;
            }
        }

       
        String originalPath = currentPath;

        try {
            
            if (!createDirectory(destName)) {
                return false;
            }

         
            if (!changeDirectory(sourceName)) {
                return false;
            }
            List<DirectoryEntry> sourceEntries = getCurrentDirectoryEntries();

          
            if (!changeDirectory("..")) {
                return false;
            }
            if (!changeDirectory(destName)) {
                return false;
            }

          
            for (DirectoryEntry entry : sourceEntries) {
                if (entry.getType() == DirectoryEntry.FileType.FILE) {
              
                    copyFile(entry.getName(), entry.getName());
                } else if (entry.getType() == DirectoryEntry.FileType.DIRECTORY) {
                 
                    copyDirectory(entry.getName(), entry.getName());
                }
            }

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
         
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
