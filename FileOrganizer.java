import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.nio.file.*;
import java.util.*;
import java.io.*;

public class FileOrganizer extends JFrame {

    private DefaultTableModel tableModel;
    private JTable table;
    private final Path dbFile = Paths.get(System.getProperty("user.home"), ".file_organizer", "tags.db");
    private final Map<String, Set<String>> tagDB = new HashMap<>();

    public FileOrganizer() {

        // ==== NEON DARK THEME ====
        Color bg = new Color(18, 18, 18);
        Color panel = new Color(26, 26, 26);
        Color neon = new Color(0, 255, 255);
        Color text = new Color(230, 230, 230);

        UIManager.put("Table.background", panel);
        UIManager.put("Table.foreground", text);
        UIManager.put("Table.selectionBackground", neon);
        UIManager.put("Table.selectionForeground", Color.black);
        UIManager.put("Panel.background", panel);
        UIManager.put("Label.foreground", neon);
        UIManager.put("Button.background", new Color(20,20,40));
        UIManager.put("Button.foreground", neon);
        UIManager.put("TextField.background", new Color(40,40,40));
        UIManager.put("TextField.foreground", text);

        setTitle("File Organizer — Neon Mode");
        setSize(900, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        getContentPane().setBackground(bg);
        setLocationRelativeTo(null);

        loadDB();

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton addFilesBtn = new JButton("➕ Add Files");
        JButton refreshBtn = new JButton("🔄 Refresh");
        JTextField searchField = new JTextField(12);
        JButton searchBtn = new JButton("🔍 Search Tag");
        JButton organizeBtn = new JButton("📂 Organize");
        top.add(addFilesBtn); top.add(refreshBtn);
        top.add(new JLabel("Tag:")); top.add(searchField); top.add(searchBtn); top.add(organizeBtn);

        tableModel = new DefaultTableModel(new Object[]{"Select","File Path","Tags"}, 0) {
            public Class<?> getColumnClass(int c) { return c == 0 ? Boolean.class : String.class; }
            public boolean isCellEditable(int r, int c) { return c == 0; }
        };
        table = new JTable(tableModel);
        table.setShowGrid(false);
        table.setRowHeight(28);
        table.getColumnModel().getColumn(0).setMaxWidth(60);
        JScrollPane scroll = new JScrollPane(table);
        scroll.getViewport().setBackground(panel);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JTextField tagField = new JTextField(20);
        JButton addTagBtn = new JButton("✨ Add Tag");
        JButton removeTagBtn = new JButton("🔥 Remove Tag");
        bottom.add(new JLabel("Tag:")); bottom.add(tagField); bottom.add(addTagBtn); bottom.add(removeTagBtn);

        add(top, BorderLayout.NORTH);
        add(scroll, BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);

        addFilesBtn.addActionListener(e -> addFiles());
        refreshBtn.addActionListener(e -> loadTable());
        addTagBtn.addActionListener(e -> applyTag(tagField.getText().trim(), true));
        removeTagBtn.addActionListener(e -> applyTag(tagField.getText().trim(), false));
        searchBtn.addActionListener(e -> searchByTag(searchField.getText().trim()));
        organizeBtn.addActionListener(e -> organizeSelected());

        loadTable();
    }

    private void addFiles() {
        JFileChooser fc = new JFileChooser(); fc.setMultiSelectionEnabled(true);
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            for (File f : fc.getSelectedFiles())
                tagDB.putIfAbsent(f.getAbsolutePath(), new LinkedHashSet<>());
            saveDB(); loadTable();
        }
    }

    private void applyTag(String tag, boolean add) {
        if (tag.isEmpty()) return;
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            if (Boolean.TRUE.equals(tableModel.getValueAt(i, 0))) {
                String path = (String) tableModel.getValueAt(i, 1);
                tagDB.putIfAbsent(path, new LinkedHashSet<>());
                if (add) tagDB.get(path).add(tag);
                else tagDB.get(path).remove(tag);
            }
        }
        saveDB(); loadTable();
    }

    private void searchByTag(String tag) {
        if (tag.isEmpty()) return;
        DefaultTableModel m = new DefaultTableModel(new Object[]{"File","Tags"}, 0);
        tagDB.forEach((path,tags)->{ if(tags.contains(tag)) m.addRow(new Object[]{path,String.join(",",tags)}); });
        JTable t = new JTable(m);
        JOptionPane.showMessageDialog(this,new JScrollPane(t),"Search Results",JOptionPane.PLAIN_MESSAGE);
    }

    private void organizeSelected() {
        JFileChooser fc = new JFileChooser(); fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            Path dest = fc.getSelectedFile().toPath();
            for (int i = 0;i < tableModel.getRowCount();i++) {
                if (Boolean.TRUE.equals(tableModel.getValueAt(i,0))) {
                    String path = (String) tableModel.getValueAt(i,1);
                    Set<String> tags = tagDB.get(path);
                    if (tags != null && !tags.isEmpty()) {
                        String tag = tags.iterator().next();
                        try {
                            Path targetDir = dest.resolve(tag.replaceAll("[\\\\/:*?\"<>|]", "_"));
                            Files.createDirectories(targetDir);
                            Path moved = Files.move(Paths.get(path), targetDir.resolve(Paths.get(path).getFileName()));
                            tagDB.remove(path);
                            tagDB.put(moved.toString(), tags);
                        } catch (Exception ex) { ex.printStackTrace(); }
                    }
                }
            }
            saveDB(); loadTable();
        }
    }

    private void loadDB() {
        try {
            Files.createDirectories(dbFile.getParent());
            if (!Files.exists(dbFile)) return;
            for (String line : Files.readAllLines(dbFile)) {
                String[] parts = line.split("\\|");
                if (parts.length == 2) tagDB.put(parts[0], new LinkedHashSet<>(Arrays.asList(parts[1].split(","))));
            }
        } catch (Exception ignored) {}
    }

    private void saveDB() {
        try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(dbFile))) {
            for (var e : tagDB.entrySet())
                pw.println(e.getKey()+"|"+String.join(",",e.getValue()));
        } catch (Exception ignored) {}
    }

    private void loadTable() {
        tableModel.setRowCount(0);
        tagDB.forEach((p,t)->tableModel.addRow(new Object[]{false,p,String.join(",",t)}));
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new FileOrganizer().setVisible(true));
    }
}
