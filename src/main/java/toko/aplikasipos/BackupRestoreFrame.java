package toko.aplikasipos;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

public class BackupRestoreFrame extends JFrame {

    // 1. UBAH JALUR DATABASE KE FOLDER USER (Sama seperti DatabaseManager)
    private static final Path DB_PATH;
    static {
        String userHome = System.getProperty("user.home");
        File dbFile = new File(userHome, ".sanFK-POS/pos_db.db");
        DB_PATH = dbFile.toPath();
    }
    
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    public BackupRestoreFrame() {
        setTitle("Backup / Restore Database");
        setSize(520, 170);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        initUi();
    }

    private void initUi() {
        JPanel content = new JPanel(new BorderLayout(8, 8));
        content.setBackground(new Color(39, 60, 117));
        content.setBorder(new EmptyBorder(15, 15, 15, 15)); // Tambah jarak margin agar lebih rapi

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT));
        actions.setOpaque(false); // Membuat background panel tombol menjadi transparan mengikuti warna biru

        JButton btnBackup = new JButton("Backup Sekarang");
        JButton btnRestore = new JButton("Restore dari File");
        actions.add(btnBackup);
        actions.add(btnRestore);

        JLabel lblInfo = new JLabel("Gunakan fitur ini untuk backup/restore file SQLite (pos_db.db).");
        lblInfo.setForeground(Color.WHITE); // 2. UBAH WARNA TEKS JADI PUTIH

        content.add(lblInfo, BorderLayout.NORTH);
        content.add(actions, BorderLayout.CENTER);
        setContentPane(content);

        btnBackup.addActionListener(e -> doBackup());
        btnRestore.addActionListener(e -> doRestore());
    }

    private void doBackup() {
        if (!Files.exists(DB_PATH)) {
            JOptionPane.showMessageDialog(this, "File database tidak ditemukan: " + DB_PATH.toAbsolutePath());
            return;
        }

        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File("backup_pos_" + LocalDateTime.now().format(FMT) + ".db"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        try {
            Files.copy(DB_PATH, chooser.getSelectedFile().toPath(), StandardCopyOption.REPLACE_EXISTING);
            JOptionPane.showMessageDialog(this, "Backup berhasil: " + chooser.getSelectedFile().getAbsolutePath());
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Gagal backup: " + e.getMessage());
        }
    }

    private void doRestore() {
        JFileChooser chooser = new JFileChooser();
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }

        int ok = JOptionPane.showConfirmDialog(this,
                "Restore akan menimpa database saat ini.\nLanjutkan?",
                "Konfirmasi Restore", JOptionPane.YES_NO_OPTION);
        if (ok != JOptionPane.YES_OPTION) {
            return;
        }

        try {
            Files.copy(chooser.getSelectedFile().toPath(), DB_PATH, StandardCopyOption.REPLACE_EXISTING);
            JOptionPane.showMessageDialog(this,
                    "Restore berhasil. Silakan tutup dan buka ulang aplikasi agar data terbaru dimuat.");
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Gagal restore: " + e.getMessage());
        }
    }
}