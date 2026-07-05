import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.util.List;

public class FileSystemGUI extends JFrame {
    private FileSystem fs;
    private JTable fileTable;
    private DefaultTableModel tableModel;
    private JLabel pathLabel;
    private JPanel diskStatusPanel;
    private JTextArea fileContentArea;
    private JButton backButton;
    private JButton viewFileButton;

    public FileSystemGUI() {
        setTitle("模拟文件系统 - FAT");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 800);
        setLayout(new BorderLayout());

      
        fs = new FileSystem("filesystem.dat");

     
        createMenuBar();

       
        JPanel mainPanel = new JPanel(new BorderLayout());

    
        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.setPreferredSize(new Dimension(400, 0));

      
        JPanel navPanel = new JPanel(new BorderLayout());

      
        JPanel pathPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        pathLabel = new JLabel("当前路径: /");
        pathPanel.add(pathLabel);

       
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

       
        backButton = new JButton("← 返回上级");
        backButton.addActionListener(e -> goBack());
        backButton.setEnabled(false);
        buttonPanel.add(backButton);

        JButton refreshButton = new JButton("刷新");
        refreshButton.addActionListener(e -> {
            refreshFileList();
            updateDiskStatus();
        });
        buttonPanel.add(refreshButton);

        navPanel.add(pathPanel, BorderLayout.WEST);
        navPanel.add(buttonPanel, BorderLayout.EAST);

        leftPanel.add(navPanel, BorderLayout.NORTH);

     
        tableModel = new DefaultTableModel(new String[]{"名称", "类型", "大小", "修改时间"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        fileTable = new JTable(tableModel);
        fileTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        fileTable.setRowHeight(25);

  
        fileTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int row = fileTable.getSelectedRow();
                    if (row != -1) {
                        String name = (String) tableModel.getValueAt(row, 0);
                        String type = (String) tableModel.getValueAt(row, 1);
                        if (type.equals("目录")) {
                         
                            if (fs.changeDirectory(name)) {
                                pathLabel.setText("当前路径: " + fs.getCurrentPath());
                                backButton.setEnabled(!fs.getCurrentPath().equals("/"));
                                refreshFileList();
                                updateDiskStatus();
                            }
                        } else {
                      
                            showFileContent(name);
                        }
                    }
                }
            }
        });

        
        JPopupMenu popupMenu = createPopupMenu();

        fileTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    int row = fileTable.rowAtPoint(e.getPoint());
                    if (row != -1) {
                        fileTable.setRowSelectionInterval(row, row);
                    }
                    popupMenu.show(fileTable, e.getX(), e.getY());
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    int row = fileTable.rowAtPoint(e.getPoint());
                    if (row != -1) {
                        fileTable.setRowSelectionInterval(row, row);
                    }
                    popupMenu.show(fileTable, e.getX(), e.getY());
                }
            }
        });

        JScrollPane tableScrollPane = new JScrollPane(fileTable);
        leftPanel.add(tableScrollPane, BorderLayout.CENTER);

        mainPanel.add(leftPanel, BorderLayout.WEST);

        
        JPanel rightPanel = new JPanel(new BorderLayout());

        
        diskStatusPanel = new JPanel(new GridLayout(10, 10, 2, 2));
        diskStatusPanel.setBorder(BorderFactory.createTitledBorder("磁盘块状态"));
        updateDiskStatus();

        rightPanel.add(diskStatusPanel, BorderLayout.NORTH);

        
        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.setBorder(BorderFactory.createTitledBorder("文件内容查看器"));

        
        JPanel toolBar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        viewFileButton = new JButton("查看选中文件");
        viewFileButton.addActionListener(e -> {
            int row = fileTable.getSelectedRow();
            if (row != -1) {
                String name = (String) tableModel.getValueAt(row, 0);
                String type = (String) tableModel.getValueAt(row, 1);
                if (type.equals("文件")) {
                    showFileContent(name);
                } else {
                    JOptionPane.showMessageDialog(this, "请选择文件，而不是目录！");
                }
            } else {
                JOptionPane.showMessageDialog(this, "请先选择一个文件！");
            }
        });
        toolBar.add(viewFileButton);

        JButton clearButton = new JButton("清空显示");
        clearButton.addActionListener(e -> {
            fileContentArea.setText("");
            fileContentArea.setBackground(Color.WHITE);
        });
        toolBar.add(clearButton);

        contentPanel.add(toolBar, BorderLayout.NORTH);

        fileContentArea = new JTextArea(10, 30);
        fileContentArea.setEditable(false);
        fileContentArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        fileContentArea.setBackground(Color.WHITE);
        fileContentArea.setForeground(Color.BLACK);
        fileContentArea.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));

      
        JPanel contentDisplayPanel = new JPanel(new BorderLayout());
        contentDisplayPanel.add(new JScrollPane(fileContentArea), BorderLayout.CENTER);

       
        JLabel fileInfoLabel = new JLabel("当前未选择文件");
        fileInfoLabel.setFont(new Font("Arial", Font.PLAIN, 10));
        fileInfoLabel.setForeground(Color.GRAY);
        contentDisplayPanel.add(fileInfoLabel, BorderLayout.SOUTH);

        contentPanel.add(contentDisplayPanel, BorderLayout.CENTER);

        rightPanel.add(contentPanel, BorderLayout.CENTER);

        mainPanel.add(rightPanel, BorderLayout.CENTER);

        add(mainPanel, BorderLayout.CENTER);

      
        createStatusBar();

       
        refreshFileList();
        backButton.setEnabled(!fs.getCurrentPath().equals("/"));

        setVisible(true);
    }

    private void goBack() {
        if (fs.changeDirectory("..")) {
            pathLabel.setText("当前路径: " + fs.getCurrentPath());
            backButton.setEnabled(!fs.getCurrentPath().equals("/"));
            refreshFileList();
            updateDiskStatus();
            // 清空文件内容显示
            fileContentArea.setText("");
            fileContentArea.setBackground(Color.WHITE);
        }
    }

    private JPopupMenu createPopupMenu() {
        JPopupMenu popupMenu = new JPopupMenu();

        JMenuItem openItem = new JMenuItem("打开");
        openItem.addActionListener(e -> {
            int row = fileTable.getSelectedRow();
            if (row != -1) {
                String name = (String) tableModel.getValueAt(row, 0);
                String type = (String) tableModel.getValueAt(row, 1);
                if (type.equals("目录")) {
                    if (fs.changeDirectory(name)) {
                        pathLabel.setText("当前路径: " + fs.getCurrentPath());
                        backButton.setEnabled(!fs.getCurrentPath().equals("/"));
                        refreshFileList();
                        updateDiskStatus();
                    }
                } else {
                    showFileContent(name);
                }
            }
        });
        popupMenu.add(openItem);

        JMenuItem viewItem = new JMenuItem("查看文件内容");
        viewItem.addActionListener(e -> {
            int row = fileTable.getSelectedRow();
            if (row != -1) {
                String name = (String) tableModel.getValueAt(row, 0);
                String type = (String) tableModel.getValueAt(row, 1);
                if (type.equals("文件")) {
                    showFileContent(name);
                }
            }
        });
        popupMenu.add(viewItem);

        popupMenu.addSeparator();

        JMenuItem copyItem = new JMenuItem("复制");
        copyItem.addActionListener(e -> showCopyDialog());
        popupMenu.add(copyItem);

        JMenuItem deleteItem = new JMenuItem("删除");
        deleteItem.addActionListener(e -> deleteSelectedItem());
        popupMenu.add(deleteItem);

        return popupMenu;
    }

    private void createMenuBar() {
        JMenuBar menuBar = new JMenuBar();

       
        JMenu fileMenu = new JMenu("文件");
        JMenuItem createFile = new JMenuItem("创建文件");
        createFile.addActionListener(e -> showCreateFileDialog());
        fileMenu.add(createFile);

        JMenuItem createDir = new JMenuItem("创建目录");
        createDir.addActionListener(e -> showCreateDirectoryDialog());
        fileMenu.add(createDir);

        fileMenu.addSeparator();

        JMenuItem deleteItem = new JMenuItem("删除");
        deleteItem.addActionListener(e -> deleteSelectedItem());
        fileMenu.add(deleteItem);

        JMenuItem copyItem = new JMenuItem("复制");
        copyItem.addActionListener(e -> showCopyDialog());
        fileMenu.add(copyItem);

        fileMenu.addSeparator();

        JMenuItem viewItem = new JMenuItem("查看文件");
        viewItem.addActionListener(e -> {
            int row = fileTable.getSelectedRow();
            if (row != -1) {
                String name = (String) tableModel.getValueAt(row, 0);
                String type = (String) tableModel.getValueAt(row, 1);
                if (type.equals("文件")) {
                    showFileContent(name);
                }
            }
        });
        fileMenu.add(viewItem);

        fileMenu.addSeparator();

        JMenuItem exitItem = new JMenuItem("退出");
        exitItem.addActionListener(e -> System.exit(0));
        fileMenu.add(exitItem);

        menuBar.add(fileMenu);

      
        JMenu navMenu = new JMenu("导航");
        JMenuItem backItem = new JMenuItem("返回上级");
        backItem.addActionListener(e -> goBack());
        navMenu.add(backItem);

        JMenuItem rootItem = new JMenuItem("返回根目录");
        rootItem.addActionListener(e -> {
            while (fs.changeDirectory("..")) {
             
            }
            pathLabel.setText("当前路径: " + fs.getCurrentPath());
            backButton.setEnabled(!fs.getCurrentPath().equals("/"));
            refreshFileList();
            updateDiskStatus();
        });
        navMenu.add(rootItem);

        menuBar.add(navMenu);

        
        JMenu viewMenu = new JMenu("查看");
        JMenuItem refreshItem = new JMenuItem("刷新");
        refreshItem.addActionListener(e -> {
            refreshFileList();
            updateDiskStatus();
        });
        viewMenu.add(refreshItem);

        JMenuItem diskStatusItem = new JMenuItem("显示磁盘状态");
        diskStatusItem.addActionListener(e -> {
            showDiskStatusDialog();
        });
        viewMenu.add(diskStatusItem);

        menuBar.add(viewMenu);

  
        JMenu helpMenu = new JMenu("帮助");
        JMenuItem aboutItem = new JMenuItem("关于");
        aboutItem.addActionListener(e -> {
            JOptionPane.showMessageDialog(this,
                    "模拟文件系统 - FAT实现\n" +
                            "磁盘块大小: 512字节\n" +
                            "磁盘块数量: 100块\n" +
                            "使用FAT表管理磁盘空间\n\n" +
                            "功能说明:\n" +
                            "• 双击目录进入\n" +
                            "• 双击文件查看内容\n" +
                            "• 右键菜单提供更多操作\n" +
                            "• 点击磁盘块查看详细信息",
                    "关于", JOptionPane.INFORMATION_MESSAGE);
        });
        helpMenu.add(aboutItem);

        menuBar.add(helpMenu);

        setJMenuBar(menuBar);
    }

    private void createStatusBar() {
        JPanel statusBar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        statusBar.setBorder(BorderFactory.createLoweredBevelBorder());

        JLabel statusLabel = new JLabel("就绪 | 双击目录进入，双击文件查看内容");
        statusBar.add(statusLabel);

        add(statusBar, BorderLayout.SOUTH);
    }

    private void refreshFileList() {
        tableModel.setRowCount(0);
        List<DirectoryEntry> entries = fs.getCurrentDirectoryEntries();

        for (DirectoryEntry entry : entries) {
            String type = entry.getType() == DirectoryEntry.FileType.DIRECTORY ? "目录" : "文件";
            String size = entry.getType() == DirectoryEntry.FileType.DIRECTORY ? "-" : entry.getSize() + " B";
            String time = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date(entry.getModifyTime()));
            tableModel.addRow(new Object[]{entry.getName(), type, size, time});
        }
    }

    private void updateDiskStatus() {
        diskStatusPanel.removeAll();
        short[] fat = fs.getFAT();

        for (int i = 0; i < fat.length; i++) {
            final int blockIndex = i;
            JButton blockBtn = new JButton(String.valueOf(i));
            blockBtn.setFont(new Font("Arial", Font.PLAIN, 8));

            if (i == 0) {
                blockBtn.setBackground(Color.GRAY);
                blockBtn.setText("FAT");
            } else if (i == 1) {
                blockBtn.setBackground(Color.GRAY);
                blockBtn.setText("FAT");
            } else if (i == Disk.ROOT_DIR_BLOCK) {
                blockBtn.setBackground(Color.ORANGE);
                blockBtn.setText("根");
            } else if (fat[i] == -1) {
                blockBtn.setBackground(Color.GREEN);
                blockBtn.setText("空");
            } else if (fat[i] == -2) {
                blockBtn.setBackground(Color.RED);
                blockBtn.setText("末");
            } else {
                blockBtn.setBackground(Color.YELLOW);
                blockBtn.setText(String.valueOf(fat[i]));
            }

            blockBtn.setToolTipText("块 " + i + ": " + getBlockStatus(fat, i));
            blockBtn.addActionListener(e -> showBlockInfo(blockIndex));

            diskStatusPanel.add(blockBtn);
        }

        diskStatusPanel.revalidate();
        diskStatusPanel.repaint();
    }

    private String getBlockStatus(short[] fat, int index) {
        if (index == 0 || index == 1) return "FAT表";
        if (index == Disk.ROOT_DIR_BLOCK) return "根目录";
        if (fat[index] == -1) return "空闲";
        if (fat[index] == -2) return "文件结束";
        return "指向块 " + fat[index];
    }

    private void showBlockInfo(int blockNum) {
        short[] fat = fs.getFAT();
        String status = getBlockStatus(fat, blockNum);

        StringBuilder info = new StringBuilder();
        info.append("=== 磁盘块信息 ===\n");
        info.append("块号: ").append(blockNum).append("\n");
        info.append("状态: ").append(status).append("\n");

        if (fat[blockNum] != -1 && fat[blockNum] != -2) {
          
            List<DirectoryEntry> entries = fs.getCurrentDirectoryEntries();
            boolean found = false;
            for (DirectoryEntry entry : entries) {
                if (entry.getType() == DirectoryEntry.FileType.FILE) {
                    int current = entry.getFirstBlock();
                    while (current != -1 && current != -2) {
                        if (current == blockNum) {
                            info.append("属于文件: ").append(entry.getName()).append("\n");
                            info.append("文件大小: ").append(entry.getSize()).append(" 字节\n");
                            found = true;
                            break;
                        }
                        current = fat[current];
                    }
                }
                if (found) break;
            }
        }

        
        if (blockNum >= 0 && blockNum < Disk.TOTAL_BLOCKS) {
            byte[] data = fs.getDisk().readBlock(blockNum);
            info.append("\n数据预览:\n");
            int previewLen = Math.min(100, data.length);
            for (int i = 0; i < previewLen; i++) {
                if (data[i] != 0) {
                    info.append(String.format("%02X ", data[i]));
                }
                if ((i + 1) % 16 == 0) {
                    info.append("\n");
                }
            }
            info.append("\n");
        }

        JOptionPane.showMessageDialog(this, info.toString(), "磁盘块详细信息", JOptionPane.INFORMATION_MESSAGE);
    }

    private void showFileContent(String fileName) {
        String content = fs.readFileContent(fileName);
        if (content != null) {
          
            List<DirectoryEntry> entries = fs.getCurrentDirectoryEntries();
            DirectoryEntry fileEntry = null;
            for (DirectoryEntry entry : entries) {
                if (entry.getName().equals(fileName) && entry.getType() == DirectoryEntry.FileType.FILE) {
                    fileEntry = entry;
                    break;
                }
            }

            if (fileEntry != null) {
                StringBuilder displayContent = new StringBuilder();
                displayContent.append("=== 文件信息 ===\n");
                displayContent.append("文件名: ").append(fileEntry.getName()).append("\n");
                displayContent.append("大小: ").append(fileEntry.getSize()).append(" 字节\n");
                displayContent.append("创建时间: ").append(new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date(fileEntry.getCreateTime()))).append("\n");
                displayContent.append("修改时间: ").append(new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date(fileEntry.getModifyTime()))).append("\n");
                displayContent.append("\n=== 文件内容 ===\n");
                displayContent.append(content);

                fileContentArea.setText(displayContent.toString());
                fileContentArea.setBackground(new Color(245, 245, 255));
                fileContentArea.setCaretPosition(0);
            } else {
                fileContentArea.setText("无法获取文件信息");
                fileContentArea.setBackground(Color.WHITE);
            }
        } else {
            fileContentArea.setText("无法读取文件内容\n\n可能原因：\n1. 文件不存在\n2. 文件已损坏\n3. 权限不足");
            fileContentArea.setBackground(new Color(255, 240, 240));
        }
    }

    private void showCreateDirectoryDialog() {
        String name = JOptionPane.showInputDialog(this, "请输入目录名称:");
        if (name != null && !name.trim().isEmpty()) {
            if (fs.createDirectory(name.trim())) {
                JOptionPane.showMessageDialog(this, "目录创建成功!");
                refreshFileList();
                updateDiskStatus();
            } else {
                JOptionPane.showMessageDialog(this, "创建目录失败!", "错误", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void showCreateFileDialog() {
        JPanel panel = new JPanel(new GridLayout(2, 2));
        JTextField nameField = new JTextField();
        JTextArea contentArea = new JTextArea(5, 20);

        panel.add(new JLabel("文件名:"));
        panel.add(nameField);
        panel.add(new JLabel("内容:"));
        panel.add(new JScrollPane(contentArea));

        int result = JOptionPane.showConfirmDialog(this, panel, "创建文件", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            String name = nameField.getText().trim();
            String content = contentArea.getText();
            if (!name.isEmpty()) {
                if (fs.createFile(name, content)) {
                    JOptionPane.showMessageDialog(this, "文件创建成功!");
                    refreshFileList();
                    updateDiskStatus();
                } else {
                    JOptionPane.showMessageDialog(this, "创建文件失败!", "错误", JOptionPane.ERROR_MESSAGE);
                }
            }
        }
    }

    private void deleteSelectedItem() {
        int row = fileTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "请先选择要删除的项目!");
            return;
        }

        String name = (String) tableModel.getValueAt(row, 0);
        String type = (String) tableModel.getValueAt(row, 1);

        int confirm = JOptionPane.showConfirmDialog(this,
                "确定要删除 " + (type.equals("目录") ? "目录" : "文件") + " '" + name + "' 吗?\n" +
                        (type.equals("目录") ? "注意：目录必须为空才能删除！" : ""),
                "确认删除", JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            boolean success;
            if (type.equals("目录")) {
                success = fs.deleteDirectory(name);
            } else {
                success = fs.deleteFile(name);
            }

            if (success) {
                JOptionPane.showMessageDialog(this, "删除成功!");
                refreshFileList();
                updateDiskStatus();
                fileContentArea.setText("");
                fileContentArea.setBackground(Color.WHITE);
            } else {
                JOptionPane.showMessageDialog(this,
                        "删除失败!\n" + (type.equals("目录") ? "目录可能不为空！" : "文件可能正在使用中！"),
                        "错误", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void showCopyDialog() {
   
        JDialog dialog = new JDialog(this, "复制文件/目录", true);
        dialog.setLayout(new BorderLayout());
        dialog.setSize(500, 350);
        dialog.setLocationRelativeTo(this);

        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        
        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(new JLabel("源名称:"), gbc);

        gbc.gridx = 1;
        gbc.gridy = 0;
        JTextField sourceField = new JTextField(20);

        int row = fileTable.getSelectedRow();
        if (row != -1) {
            String name = (String) tableModel.getValueAt(row, 0);
            sourceField.setText(name);
        }
        panel.add(sourceField, gbc);

   
        gbc.gridx = 2;
        gbc.gridy = 0;
        JLabel typeLabel = new JLabel("文件");
        panel.add(typeLabel, gbc);

   
        gbc.gridx = 0;
        gbc.gridy = 1;
        panel.add(new JLabel("目标名称:"), gbc);

        gbc.gridx = 1;
        gbc.gridy = 1;
        JTextField destField = new JTextField();
        panel.add(destField, gbc);

      
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 3;
        JCheckBox recursiveCheck = new JCheckBox("递归复制目录内容");
        recursiveCheck.setSelected(true);
        panel.add(recursiveCheck, gbc);

        dialog.add(panel, BorderLayout.CENTER);

      
        JPanel buttonPanel = new JPanel(new FlowLayout());
        JButton copyButton = new JButton("复制");
        JButton cancelButton = new JButton("取消");

        copyButton.addActionListener(e -> {
            String source = sourceField.getText().trim();
            String dest = destField.getText().trim();

            if (source.isEmpty() || dest.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "请输入源名称和目标名称！");
                return;
            }

          
            List<DirectoryEntry> entries = fs.getCurrentDirectoryEntries();
            DirectoryEntry sourceEntry = null;
            for (DirectoryEntry entry : entries) {
                if (entry.getName().equals(source)) {
                    sourceEntry = entry;
                    break;
                }
            }

            if (sourceEntry == null) {
                JOptionPane.showMessageDialog(dialog, "源文件/目录不存在！");
                return;
            }

            boolean success = false;
            if (sourceEntry.getType() == DirectoryEntry.FileType.FILE) {
               
                success = fs.copyFile(source, dest);
            } else {
              
                if (recursiveCheck.isSelected()) {
                    success = fs.copyDirectory(source, dest);
                } else {
                    JOptionPane.showMessageDialog(dialog,
                            "复制目录需要选择递归复制选项！");
                    return;
                }
            }

            if (success) {
                JOptionPane.showMessageDialog(dialog, "复制成功！");
                dialog.dispose();
                refreshFileList();
                updateDiskStatus();
            } else {
                JOptionPane.showMessageDialog(dialog,
                        "复制失败！\n可能原因：\n1. 目标名称已存在\n2. 空间不足\n3. 权限问题",
                        "错误", JOptionPane.ERROR_MESSAGE);
            }
        });

        cancelButton.addActionListener(e -> dialog.dispose());

        buttonPanel.add(copyButton);
        buttonPanel.add(cancelButton);
        dialog.add(buttonPanel, BorderLayout.SOUTH);

        
        sourceField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void changedUpdate(javax.swing.event.DocumentEvent e) { updateType(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { updateType(); }
            public void insertUpdate(javax.swing.event.DocumentEvent e) { updateType(); }

            private void updateType() {
                String name = sourceField.getText().trim();
                List<DirectoryEntry> entries = fs.getCurrentDirectoryEntries();
                for (DirectoryEntry entry : entries) {
                    if (entry.getName().equals(name)) {
                        typeLabel.setText(entry.getType() == DirectoryEntry.FileType.DIRECTORY ? "目录" : "文件");
                        return;
                    }
                }
                typeLabel.setText("未找到");
            }
        });

        dialog.setVisible(true);
    }

    private void showDiskStatusDialog() {
        short[] fat = fs.getFAT();
        int freeBlocks = 0;
        int usedBlocks = 0;

        for (int i = 0; i < fat.length; i++) {
            if (i >= 2 && fat[i] == -1) {
                freeBlocks++;
            } else if (i >= 2) {
                usedBlocks++;
            }
        }

        StringBuilder info = new StringBuilder();
        info.append("=== 磁盘使用情况 ===\n");
        info.append("总块数: ").append(Disk.TOTAL_BLOCKS).append("\n");
        info.append("已使用块: ").append(usedBlocks).append("\n");
        info.append("空闲块: ").append(freeBlocks).append("\n");
        info.append("使用率: ").append(String.format("%.2f%%", (double)usedBlocks / Disk.TOTAL_BLOCKS * 100)).append("\n");
        info.append("\n=== FAT表状态 ===\n");
        for (int i = 0; i < fat.length; i++) {
            info.append(String.format("块%3d: %3d  ", i, fat[i]));
            if ((i + 1) % 5 == 0) {
                info.append("\n");
            }
        }

        JOptionPane.showMessageDialog(this, info.toString(), "磁盘状态", JOptionPane.INFORMATION_MESSAGE);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                e.printStackTrace();
            }
            new FileSystemGUI();
        });
    }
}
