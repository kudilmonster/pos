package toko.aplikasipos;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;

public class SupplierPurchaseFrame extends JFrame {


    private final JTextField txtNamaSupplier = new JTextField(12);
    private final JTextField txtKontak = new JTextField(10);
    private final JTextField txtAlamat = new JTextField(14);
    private final JComboBox<String> cbSupplier = new JComboBox<>();
    private final JComboBox<String> cbBarang = new JComboBox<>();
    private final JTextField txtQty = new JTextField("1", 5);
    private final JTextField txtHargaBeli = new JTextField(8);

    private final DefaultTableModel modelSupplier = new DefaultTableModel(
            new String[]{"ID", "Nama Supplier", "Kontak", "Alamat"}, 0
    ) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };

    public SupplierPurchaseFrame() {
        setTitle("Manajemen Supplier & Pembelian Barang");
        setSize(1000, 580);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        
        // 1. Pasang ikon aplikasi (Bebas cangkir kopi)
        AppUtil.setWindowIcon(this);
        
        // 2. Jalankan Auto-Patch Database sebelum memuat UI
        pastikanTabelAman();
        
        // 3. Bangun UI dan muat data
        initUi();
        loadSupplier();
        loadBarang();
    }
    
    // --- FITUR BARU: AUTO-PATCH DATABASE ---
    private void pastikanTabelAman() {
        try (Connection conn = DatabaseManager.getConnection();
             Statement st = conn.createStatement()) {
            
            // A. Pastikan tabel supplier ada
            st.execute("CREATE TABLE IF NOT EXISTS supplier (id_supplier INTEGER PRIMARY KEY AUTOINCREMENT, nama_supplier TEXT, kontak TEXT, alamat TEXT)");
            
            // B. Trik deteksi kolom 'kontak' yang hilang (solusi untuk pesan error Anda!)
            try {
                st.executeQuery("SELECT kontak FROM supplier LIMIT 1");
            } catch (Exception e) {
                // Jika masuk ke catch ini, berarti kolom 'kontak' belum ada di DB lama.
                // Mari kita tambahkan secara otomatis melalui kode!
                st.execute("ALTER TABLE supplier ADD COLUMN kontak TEXT");
                st.execute("ALTER TABLE supplier ADD COLUMN alamat TEXT");
                System.out.println("Database Berhasil Di-Patch: Kolom kontak dan alamat ditambahkan!");
            }
            
            // C. Pastikan tabel pembelian dan detailnya juga siap
            st.execute("CREATE TABLE IF NOT EXISTS pembelian (id_pembelian INTEGER PRIMARY KEY AUTOINCREMENT, id_supplier INTEGER, total_beli INTEGER, dibuat_oleh TEXT, tanggal DATETIME DEFAULT CURRENT_TIMESTAMP)");
            st.execute("CREATE TABLE IF NOT EXISTS detail_pembelian (id_detail INTEGER PRIMARY KEY AUTOINCREMENT, id_pembelian INTEGER, nama_barang TEXT, qty INTEGER, harga_beli INTEGER, subtotal INTEGER)");
            
        } catch (Exception e) {
            System.out.println("Error Cek Database: " + e.getMessage());
        }
    }

    private void initUi() {
        // Form Atas (Supplier)
        JPanel formSupplier = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        formSupplier.setBorder(BorderFactory.createTitledBorder("Input Supplier Baru"));
        JButton btnTambahSupplier = new JButton("Tambah Supplier");
        formSupplier.add(new JLabel("Nama:"));
        formSupplier.add(txtNamaSupplier);
        formSupplier.add(new JLabel("Kontak:"));
        formSupplier.add(txtKontak);
        formSupplier.add(new JLabel("Alamat:"));
        formSupplier.add(txtAlamat);
        formSupplier.add(btnTambahSupplier);

        // Tabel Tengah
        JTable tblSupplier = new JTable(modelSupplier);
        JScrollPane spSupplier = new JScrollPane(tblSupplier);
        spSupplier.setBorder(BorderFactory.createTitledBorder("Daftar Supplier"));

        // Form Bawah (Pembelian)
        JPanel formPembelian = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        formPembelian.setBorder(BorderFactory.createTitledBorder("Transaksi Pembelian Barang"));
        JButton btnSimpanBeli = new JButton("Simpan Pembelian");
        formPembelian.add(new JLabel("Pilih Supplier:"));
        formPembelian.add(cbSupplier);
        formPembelian.add(new JLabel("Pilih Barang:"));
        formPembelian.add(cbBarang);
        formPembelian.add(new JLabel("Qty (Jumlah):"));
        formPembelian.add(txtQty);
        formPembelian.add(new JLabel("Harga Beli (Satuan):"));
        formPembelian.add(txtHargaBeli);
        formPembelian.add(btnSimpanBeli);

        // PERBAIKAN LAYOUT: Menggunakan BorderLayout agar proporsional
        setLayout(new BorderLayout(10, 10));
        add(formSupplier, BorderLayout.NORTH);
        add(spSupplier, BorderLayout.CENTER); // Tabel akan mengisi ruang kosong di tengah
        add(formPembelian, BorderLayout.SOUTH);

        // Action Listener
        btnTambahSupplier.addActionListener(e -> simpanSupplier());
        btnSimpanBeli.addActionListener(e -> simpanPembelian());
    }

    private void loadSupplier() {
        modelSupplier.setRowCount(0);
        cbSupplier.removeAllItems();
        String sql = "SELECT id_supplier, nama_supplier, COALESCE(kontak,''), COALESCE(alamat,'') FROM supplier ORDER BY nama_supplier";
        try (Connection conn = DatabaseManager.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                int id = rs.getInt("id_supplier");
                String nama = rs.getString("nama_supplier");
                modelSupplier.addRow(new Object[]{id, nama, rs.getString(3), rs.getString(4)});
                cbSupplier.addItem(id + " - " + nama);
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Gagal load supplier: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadBarang() {
        cbBarang.removeAllItems();
        String sql = "SELECT nama_barang FROM barang ORDER BY nama_barang";
        try (Connection conn = DatabaseManager.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                cbBarang.addItem(rs.getString(1));
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Gagal load barang: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

private void simpanSupplier() {
        String nama = txtNamaSupplier.getText().trim();
        if (nama.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Nama supplier wajib diisi.", "Peringatan", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        String sql = "INSERT INTO supplier (nama_supplier, kontak, alamat) VALUES (?, ?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
             
            ps.setString(1, nama);
            ps.setString(2, txtKontak.getText().trim());
            ps.setString(3, txtAlamat.getText().trim());
            ps.executeUpdate();
            
            txtNamaSupplier.setText("");
            txtKontak.setText("");
            txtAlamat.setText("");
            loadSupplier();
            JOptionPane.showMessageDialog(this, "Data Supplier berhasil tersimpan!");
            
        } catch (Exception e) {
            // --- LOGIKA BARU: MENERJEMAHKAN ERROR DATABASE ---
            if (e.getMessage().contains("UNIQUE constraint failed")) {
                JOptionPane.showMessageDialog(this, 
                    "Supplier dengan nama '" + nama + "' sudah terdaftar!\nSilakan gunakan nama lain (misalnya: " + nama + " Cabang 2).", 
                    "Data Kembar (Ganda)", 
                    JOptionPane.WARNING_MESSAGE);
                    
                // Kembalikan kursor ke kotak nama agar user bisa langsung mengetik ulang
                txtNamaSupplier.selectAll();
                txtNamaSupplier.requestFocus();
            } else {
                // Tampilkan error lain jika ada masalah di luar data ganda
                JOptionPane.showMessageDialog(this, "Gagal simpan supplier:\n" + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private int parseInt(String value, String name) throws Exception {
        try {
            int v = Integer.parseInt(value.trim());
            if (v <= 0) {
                throw new Exception(name + " harus lebih dari 0.");
            }
            return v;
        } catch (NumberFormatException e) {
            throw new Exception(name + " harus berupa angka yang valid.");
        }
    }

    private void simpanPembelian() {
        if (cbSupplier.getSelectedItem() == null || cbBarang.getSelectedItem() == null) {
            JOptionPane.showMessageDialog(this, "Supplier dan barang wajib dipilih.", "Peringatan", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try (Connection conn = DatabaseManager.getConnection()) {
            int qty = parseInt(txtQty.getText(), "Qty");
            int hargaBeli = parseInt(txtHargaBeli.getText(), "Harga Beli");
            int subtotal = qty * hargaBeli;

            String supplierItem = cbSupplier.getSelectedItem().toString();
            int idSupplier = Integer.parseInt(supplierItem.split(" - ")[0]);
            String namaBarang = cbBarang.getSelectedItem().toString();

            // Mencegah error jika dijalankan tanpa login
            String kasir = (LoginFrame.kasirAktif != null && !LoginFrame.kasirAktif.isEmpty()) ? LoginFrame.kasirAktif : "Admin";

            conn.setAutoCommit(false);
            try {
                int idPembelian;
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO pembelian (id_supplier, total_beli, dibuat_oleh) VALUES (?, ?, ?)",
                        Statement.RETURN_GENERATED_KEYS)) {
                    ps.setInt(1, idSupplier);
                    ps.setInt(2, subtotal);
                    ps.setString(3, kasir);
                    ps.executeUpdate();
                    try (ResultSet gk = ps.getGeneratedKeys()) {
                        gk.next();
                        idPembelian = gk.getInt(1);
                    }
                }

                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO detail_pembelian (id_pembelian, nama_barang, qty, harga_beli, subtotal) VALUES (?, ?, ?, ?, ?)")) {
                    ps.setInt(1, idPembelian);
                    ps.setString(2, namaBarang);
                    ps.setInt(3, qty);
                    ps.setInt(4, hargaBeli);
                    ps.setInt(5, subtotal);
                    ps.executeUpdate();
                }

                // Update stok dan Harga Beli terbaru di tabel barang
                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE barang SET stok = stok + ?, harga_modal = ? WHERE nama_barang = ?")) {
                    ps.setInt(1, qty);
                    ps.setInt(2, hargaBeli);
                    ps.setString(3, namaBarang);
                    ps.executeUpdate();
                }

                conn.commit();
                JOptionPane.showMessageDialog(this, "Transaksi Pembelian berhasil disimpan!\nStok barang bertambah otomatis.");
                
                // Reset form
                txtQty.setText("1");
                txtHargaBeli.setText("");
                cbSupplier.setSelectedIndex(0);
                
            } catch (Exception e) {
                conn.rollback();
                throw e;
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Gagal simpan pembelian:\n" + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
