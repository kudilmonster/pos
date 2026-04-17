package toko.aplikasipos;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import javax.swing.table.DefaultTableModel;
import java.sql.PreparedStatement;
import javax.swing.JOptionPane;

public class MainFrame extends javax.swing.JFrame {

    private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(MainFrame.class.getName());
    private String idBarangTerpilih = "";

    public MainFrame() {
        initComponents();
        initDatabase();    // Membuat tabel
        loadKategori();    // <- TAMBAHKAN BARIS INI: Mengisi ComboBox
        tampilkanData();   // Menampilkan data di tabel
    }

    private void initDatabase() {
        String url = "jdbc:sqlite:pos_db.db";

        try (Connection conn = DriverManager.getConnection(url); Statement stmt = conn.createStatement()) {

            // 1. Buat tabel kategori
            String sqlKategori = "CREATE TABLE IF NOT EXISTS kategori ("
                    + "id_kategori INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "nama_kategori TEXT UNIQUE NOT NULL)";
            stmt.execute(sqlKategori);

            // 2. Buat tabel barang
            String sqlBarang = "CREATE TABLE IF NOT EXISTS barang ("
                    + " id_barang INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + " nama_barang TEXT UNIQUE NOT NULL,"
                    + " kategori TEXT NOT NULL,"
                    + " harga INTEGER NOT NULL,"
                    + " stok INTEGER NOT NULL"
                    + ");";
            stmt.execute(sqlBarang);

            // 3. Buat tabel transaksi
            String sqlTransaksi = "CREATE TABLE IF NOT EXISTS transaksi ("
                    + "id_transaksi INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "tanggal DATETIME DEFAULT CURRENT_TIMESTAMP, "
                    + "total_harga INTEGER, "
                    + "diskon_persen INTEGER, "
                    + "pajak_persen INTEGER, "
                    + "bayar INTEGER, "
                    + "kembalian INTEGER)";
            stmt.execute(sqlTransaksi);

            // 4. Buat tabel detail transaksi
            String sqlDetail = "CREATE TABLE IF NOT EXISTS detail_transaksi ("
                    + "id_detail INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "id_transaksi INTEGER, "
                    + "nama_barang TEXT, "
                    + "harga INTEGER, "
                    + "qty INTEGER, "
                    + "subtotal INTEGER)";
            stmt.execute(sqlDetail);

            // 5. Isi kategori dasar jika masih kosong (DISEMPURNAKAN)
            boolean butuhIsiData = false;
            // Gunakan try khusus untuk ResultSet agar langsung otomatis ditutup
            try (ResultSet rsKategori = stmt.executeQuery("SELECT COUNT(*) AS total FROM kategori")) {
                if (rsKategori.next() && rsKategori.getInt("total") == 0) {
                    butuhIsiData = true;
                }
            }

            // Jika kosong, baru lakukan penambahan data
            if (butuhIsiData) {
                stmt.execute("INSERT INTO kategori (nama_kategori) VALUES ('Makanan')");
                stmt.execute("INSERT INTO kategori (nama_kategori) VALUES ('Minuman')");
                stmt.execute("INSERT INTO kategori (nama_kategori) VALUES ('Sembako')");
            }

            System.out.println("Database dan Tabel berhasil diinisialisasi!");

        } catch (Exception e) {
            System.out.println("Error Inisialisasi Database: " + e.getMessage());
        }
    }

    private void tampilkanData() {
        DefaultTableModel model = new DefaultTableModel();
        model.addColumn("ID Barang");
        model.addColumn("Nama Barang");
        model.addColumn("Kategori"); // Kolom baru
        model.addColumn("Harga");
        model.addColumn("Stok");

        jTable1.setModel(model);

        String url = "jdbc:sqlite:pos_db.db";

        try (Connection conn = DriverManager.getConnection(url); Statement st = conn.createStatement(); ResultSet rs = st.executeQuery("SELECT * FROM barang")) {

            while (rs.next()) {
                model.addRow(new Object[]{
                    rs.getInt("id_barang"),
                    rs.getString("nama_barang"),
                    rs.getString("kategori"), // Ambil data kategori dari database
                    rs.getInt("harga"),
                    rs.getInt("stok")
                });
            }
        } catch (Exception e) {
            System.out.println("Error Tampil Data: " + e.getMessage());
        }
    }

    private void cariData() {
        DefaultTableModel model = new DefaultTableModel();
        model.addColumn("ID Barang");
        model.addColumn("Nama Barang");
        model.addColumn("Kategori");
        model.addColumn("Harga");
        model.addColumn("Stok");

        jTable1.setModel(model);

        // Ambil nilai dari komponen GUI
        String keyword = txtCari.getText();
        String kategoriFilter = "Semua Kategori";

        if (cbFilterKategori.getSelectedItem() != null) {
            kategoriFilter = cbFilterKategori.getSelectedItem().toString();
        }

        String url = "jdbc:sqlite:pos_db.db";

        // Gunakan klausa LIKE untuk mencari nama yang "mengandung" huruf tertentu
        String sql = "SELECT * FROM barang WHERE nama_barang LIKE ?";

        // Jika filter tidak "Semua Kategori", tambahkan kondisi filter kategori ke SQL
        if (!kategoriFilter.equals("Semua Kategori")) {
            sql += " AND kategori = ?";
        }

        try (Connection conn = DriverManager.getConnection(url); PreparedStatement pstmt = conn.prepareStatement(sql)) {

            // Parameter 1: Untuk nama_barang (tanda % artinya "karakter apa saja di depan/belakang")
            pstmt.setString(1, "%" + keyword + "%");

            // Parameter 2: Untuk kategori (hanya jika filter aktif)
            if (!kategoriFilter.equals("Semua Kategori")) {
                pstmt.setString(2, kategoriFilter);
            }

            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                model.addRow(new Object[]{
                    rs.getInt("id_barang"),
                    rs.getString("nama_barang"),
                    rs.getString("kategori"),
                    rs.getInt("harga"),
                    rs.getInt("stok")
                });
            }
        } catch (Exception e) {
            System.out.println("Error Cari Data: " + e.getMessage());
        }
    }

    private void loadKategori() {
        // Bersihkan kedua ComboBox
        cbKategori.removeAllItems();
        cbFilterKategori.removeAllItems();

        // Tambahkan opsi default untuk filter pencarian
        cbFilterKategori.addItem("Semua Kategori");

        String url = "jdbc:sqlite:pos_db.db";

        try (Connection conn = DriverManager.getConnection(url); Statement st = conn.createStatement(); ResultSet rs = st.executeQuery("SELECT nama_kategori FROM kategori ORDER BY nama_kategori ASC")) {

            while (rs.next()) {
                String namaKategori = rs.getString("nama_kategori");
                cbKategori.addItem(namaKategori);          // Masuk ke form input
                cbFilterKategori.addItem(namaKategori);    // Masuk ke form filter
            }

        } catch (Exception e) {
            System.out.println("Error Load Kategori: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        cbKategori = new javax.swing.JComboBox<>();
        txtStok = new javax.swing.JTextField();
        btnTambahKategori = new javax.swing.JButton();
        jLabel3 = new javax.swing.JLabel();
        jLabel1 = new javax.swing.JLabel();
        btnHapus = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTable1 = new javax.swing.JTable();
        txtHarga = new javax.swing.JTextField();
        jLabel2 = new javax.swing.JLabel();
        btnSimpan = new javax.swing.JButton();
        btnHapusKategori = new javax.swing.JButton();
        txtNama = new javax.swing.JTextField();
        btnEdit = new javax.swing.JButton();
        jLabel4 = new javax.swing.JLabel();
        txtCari = new javax.swing.JTextField();
        cbFilterKategori = new javax.swing.JComboBox<>();
        jLabel5 = new javax.swing.JLabel();
        jButton1 = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        cbKategori.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Makanan", "Minuman", "Sembako" }));

        btnTambahKategori.setText("+");
        btnTambahKategori.addActionListener(this::btnTambahKategoriActionPerformed);

        jLabel3.setText("Stok");

        jLabel1.setText("Nama Barang");

        btnHapus.setText("Hapus");
        btnHapus.addActionListener(this::btnHapusActionPerformed);

        jTable1.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null}
            },
            new String [] {
                "Title 1", "Title 2", "Title 3", "Title 4"
            }
        ));
        jTable1.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jTable1MouseClicked(evt);
            }
        });
        jScrollPane1.setViewportView(jTable1);

        jLabel2.setText("Harga");

        btnSimpan.setText("Simpan");
        btnSimpan.addActionListener(this::btnSimpanActionPerformed);

        btnHapusKategori.setText("-");
        btnHapusKategori.addActionListener(this::btnHapusKategoriActionPerformed);

        btnEdit.setText("Edit");
        btnEdit.addActionListener(this::btnEditActionPerformed);

        jLabel4.setText("Cari Barang");

        txtCari.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                txtCariKeyReleased(evt);
            }
        });

        cbFilterKategori.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        cbFilterKategori.addActionListener(this::cbFilterKategoriActionPerformed);

        jLabel5.setText("Filter Kategori");

        jButton1.setText("Buka Menu Kasir");
        jButton1.addActionListener(this::jButton1ActionPerformed);

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane1, javax.swing.GroupLayout.Alignment.TRAILING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel1)
                    .addComponent(jLabel2)
                    .addComponent(jLabel3))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(txtHarga, javax.swing.GroupLayout.PREFERRED_SIZE, 84, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                            .addComponent(txtNama, javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanel1Layout.createSequentialGroup()
                                .addComponent(txtStok, javax.swing.GroupLayout.PREFERRED_SIZE, 84, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(cbKategori, javax.swing.GroupLayout.PREFERRED_SIZE, 139, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addGap(18, 18, 18)
                        .addComponent(btnTambahKategori)
                        .addGap(18, 18, 18)
                        .addComponent(btnHapusKategori)))
                .addContainerGap(80, Short.MAX_VALUE))
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(btnSimpan)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(btnEdit)
                        .addGap(15, 15, 15)
                        .addComponent(btnHapus))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                            .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanel1Layout.createSequentialGroup()
                                .addComponent(jLabel5)
                                .addGap(18, 18, 18)
                                .addComponent(cbFilterKategori, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addComponent(jLabel4)
                                .addGap(18, 18, 18)
                                .addComponent(txtCari, javax.swing.GroupLayout.PREFERRED_SIZE, 197, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jButton1)
                        .addGap(14, 14, 14))))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                .addGap(15, 15, 15)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel4)
                    .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(txtCari, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(jButton1)))
                .addGap(18, 18, 18)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(cbFilterKategori, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel5))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 46, Short.MAX_VALUE)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(txtNama, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel1))
                        .addGap(18, 18, 18)
                        .addComponent(txtHarga, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(jLabel2, javax.swing.GroupLayout.Alignment.TRAILING))
                .addGap(18, 18, 18)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(txtStok, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel3)
                    .addComponent(cbKategori, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnTambahKategori)
                    .addComponent(btnHapusKategori))
                .addGap(18, 18, 18)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnEdit)
                    .addComponent(btnHapus)
                    .addComponent(btnSimpan))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 189, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btnSimpanActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSimpanActionPerformed
// 1. Validasi input kosong (menggunakan trim() untuk menghapus spasi tidak sengaja)
        String namaBarangInput = txtNama.getText().trim();
        String hargaInput = txtHarga.getText().trim();
        String stokInput = txtStok.getText().trim();

        if (namaBarangInput.isEmpty() || hargaInput.isEmpty() || stokInput.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Semua kolom harus diisi!", "Peringatan", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String url = "jdbc:sqlite:pos_db.db";

        try (Connection conn = DriverManager.getConnection(url)) {

            // 2. CEK APAKAH BARANG SUDAH ADA DI DATABASE
            String sqlCek = "SELECT COUNT(*) AS total FROM barang WHERE nama_barang = ?";
            try (PreparedStatement pstCek = conn.prepareStatement(sqlCek)) {
                pstCek.setString(1, namaBarangInput);
                ResultSet rsCek = pstCek.executeQuery();

                // Jika hasil pencarian > 0, berarti barang sudah ada
                if (rsCek.next() && rsCek.getInt("total") > 0) {
                    JOptionPane.showMessageDialog(this,
                            "Barang '" + namaBarangInput + "' sudah ada di database!\nSilakan gunakan tombol Edit jika ingin mengubah stok/harga.",
                            "Data Ganda",
                            JOptionPane.WARNING_MESSAGE);
                    return; // Hentikan proses simpan di sini
                }
            }

            // 3. JIKA BELUM ADA, LANJUTKAN PROSES SIMPAN
            String sqlInsert = "INSERT INTO barang (nama_barang, kategori, harga, stok) VALUES (?, ?, ?, ?)";
            try (PreparedStatement pstmt = conn.prepareStatement(sqlInsert)) {
                pstmt.setString(1, namaBarangInput);
                pstmt.setString(2, cbKategori.getSelectedItem().toString());
                pstmt.setInt(3, Integer.parseInt(hargaInput));
                pstmt.setInt(4, Integer.parseInt(stokInput));

                pstmt.executeUpdate();
                JOptionPane.showMessageDialog(this, "Data barang berhasil disimpan!");

                cariData(); // Refresh tabel menggunakan fungsi cariData() agar filter tetap jalan

                // Bersihkan form
                txtNama.setText("");
                txtHarga.setText("");
                txtStok.setText("");
                txtNama.requestFocus();
            }

        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Harga dan Stok harus berupa angka!", "Error", JOptionPane.ERROR_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Gagal menyimpan data: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }//GEN-LAST:event_btnSimpanActionPerformed

    private void jTable1MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jTable1MouseClicked
        int baris = jTable1.rowAtPoint(evt.getPoint());

        if (baris > -1) {
            // Indeks kolom bergeser karena ada tambahan kategori
            idBarangTerpilih = jTable1.getValueAt(baris, 0).toString();
            String nama = jTable1.getValueAt(baris, 1).toString();
            String kategori = jTable1.getValueAt(baris, 2).toString(); // Ambil kategori
            String harga = jTable1.getValueAt(baris, 3).toString();
            String stok = jTable1.getValueAt(baris, 4).toString();

            txtNama.setText(nama);
            cbKategori.setSelectedItem(kategori); // Ubah pilihan ComboBox
            txtHarga.setText(harga);
            txtStok.setText(stok);
        }
    }//GEN-LAST:event_jTable1MouseClicked

    private void btnEditActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnEditActionPerformed
        if (idBarangTerpilih.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Pilih data di tabel terlebih dahulu!", "Peringatan", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // BARIS INI YANG SEBELUMNYA HILANG
        String url = "jdbc:sqlite:pos_db.db";
        String sql = "UPDATE barang SET nama_barang = ?, kategori = ?, harga = ?, stok = ? WHERE id_barang = ?";

        try (Connection conn = DriverManager.getConnection(url); PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, txtNama.getText());
            pstmt.setString(2, cbKategori.getSelectedItem().toString());
            pstmt.setInt(3, Integer.parseInt(txtHarga.getText()));
            pstmt.setInt(4, Integer.parseInt(txtStok.getText()));
            pstmt.setString(5, idBarangTerpilih);

            pstmt.executeUpdate();
            JOptionPane.showMessageDialog(this, "Data berhasil diupdate!");
            cariData();

            txtNama.setText("");
            txtHarga.setText("");
            txtStok.setText("");
            idBarangTerpilih = "";

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Gagal mengupdate data: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }//GEN-LAST:event_btnEditActionPerformed

    private void btnHapusActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnHapusActionPerformed
        // 1. Cek apakah ada data yang dipilih
        if (idBarangTerpilih.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Pilih data di tabel terlebih dahulu!", "Peringatan", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // 2. Munculkan dialog konfirmasi (Ya / Tidak)
        int konfirmasi = JOptionPane.showConfirmDialog(this, "Apakah Anda yakin ingin menghapus barang ini?", "Konfirmasi Hapus", JOptionPane.YES_NO_OPTION);

        if (konfirmasi == JOptionPane.YES_OPTION) {
            String url = "jdbc:sqlite:pos_db.db";
            String sql = "DELETE FROM barang WHERE id_barang = ?";

            try (Connection conn = DriverManager.getConnection(url); PreparedStatement pstmt = conn.prepareStatement(sql)) {

                // 3. Masukkan ID yang akan dihapus
                pstmt.setString(1, idBarangTerpilih);

                // 4. Eksekusi hapus dan refresh
                pstmt.executeUpdate();
                JOptionPane.showMessageDialog(this, "Data berhasil dihapus!");
                cariData();

                // 5. Bersihkan form
                txtNama.setText("");
                txtHarga.setText("");
                txtStok.setText("");
                idBarangTerpilih = ""; // Reset ID

            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Gagal menghapus data: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }//GEN-LAST:event_btnHapusActionPerformed

    private void btnTambahKategoriActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnTambahKategoriActionPerformed
        // 1. Munculkan kotak dialog input
        String kategoriBaru = JOptionPane.showInputDialog(this, "Masukkan Nama Kategori Baru:");

        // 2. Pastikan user mengisi sesuatu (tidak klik Cancel atau kosong)
        if (kategoriBaru != null && !kategoriBaru.trim().isEmpty()) {

            String url = "jdbc:sqlite:pos_db.db";
            String sql = "INSERT INTO kategori (nama_kategori) VALUES (?)";

            try (Connection conn = DriverManager.getConnection(url); PreparedStatement pstmt = conn.prepareStatement(sql)) {

                // 3. Simpan ke database (trim untuk menghapus spasi berlebih)
                pstmt.setString(1, kategoriBaru.trim());
                pstmt.executeUpdate();

                JOptionPane.showMessageDialog(this, "Kategori berhasil ditambahkan!");

                // 4. Refresh daftar di ComboBox
                loadKategori();

                // 5. Otomatis pilih kategori yang baru saja ditambahkan
                cbKategori.setSelectedItem(kategoriBaru.trim());

            } catch (Exception e) {
                // Menangkap error jika user memasukkan nama yang sudah ada (efek dari UNIQUE di database)
                if (e.getMessage().contains("UNIQUE")) {
                    JOptionPane.showMessageDialog(this, "Kategori tersebut sudah ada di daftar!", "Peringatan", JOptionPane.WARNING_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(this, "Gagal menambah kategori: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }
    }//GEN-LAST:event_btnTambahKategoriActionPerformed

    private void btnHapusKategoriActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnHapusKategoriActionPerformed
        // 1. Ambil kategori yang sedang dipilih di ComboBox
        Object selectedItem = cbKategori.getSelectedItem();

        // Pastikan ada kategori yang dipilih (ComboBox tidak kosong)
        if (selectedItem == null) {
            JOptionPane.showMessageDialog(this, "Tidak ada kategori yang dipilih!", "Peringatan", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String kategoriDihapus = selectedItem.toString();

        // 2. Munculkan dialog konfirmasi
        int konfirmasi = JOptionPane.showConfirmDialog(this,
                "Yakin ingin menghapus kategori '" + kategoriDihapus + "'?",
                "Konfirmasi Hapus Kategori",
                JOptionPane.YES_NO_OPTION);

        if (konfirmasi == JOptionPane.YES_OPTION) {
            String url = "jdbc:sqlite:pos_db.db";

            try (Connection conn = DriverManager.getConnection(url)) {

                // 3. FITUR KEAMANAN: Cek apakah kategori ini masih dipakai di tabel barang
                String checkSql = "SELECT COUNT(*) AS total FROM barang WHERE kategori = ?";
                try (PreparedStatement pstCheck = conn.prepareStatement(checkSql)) {
                    pstCheck.setString(1, kategoriDihapus);
                    ResultSet rs = pstCheck.executeQuery();

                    if (rs.next() && rs.getInt("total") > 0) {
                        // Jika masih dipakai, batalkan proses hapus dan beri peringatan
                        JOptionPane.showMessageDialog(this,
                                "Kategori tidak dapat dihapus karena masih digunakan oleh " + rs.getInt("total") + " barang!\nSilakan ubah/hapus barang tersebut terlebih dahulu.",
                                "Gagal Menghapus",
                                JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                }

                // 4. Jika aman (tidak dipakai barang apapun), eksekusi perintah DELETE
                String sqlDelete = "DELETE FROM kategori WHERE nama_kategori = ?";
                try (PreparedStatement pstmt = conn.prepareStatement(sqlDelete)) {
                    pstmt.setString(1, kategoriDihapus);
                    pstmt.executeUpdate();

                    JOptionPane.showMessageDialog(this, "Kategori berhasil dihapus!");

                    // 5. Refresh daftar kategori di ComboBox
                    loadKategori();
                }

            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Gagal menghapus kategori: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }//GEN-LAST:event_btnHapusKategoriActionPerformed

    private void txtCariKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_txtCariKeyReleased
        cariData();
    }//GEN-LAST:event_txtCariKeyReleased

    private void cbFilterKategoriActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cbFilterKategoriActionPerformed
        cariData();
    }//GEN-LAST:event_cbFilterKategoriActionPerformed

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
        KasirFrame kasir = new KasirFrame();
        kasir.setVisible(true);
    }//GEN-LAST:event_jButton1ActionPerformed

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ReflectiveOperationException | javax.swing.UnsupportedLookAndFeelException ex) {
            logger.log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(() -> new MainFrame().setVisible(true));
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnEdit;
    private javax.swing.JButton btnHapus;
    private javax.swing.JButton btnHapusKategori;
    private javax.swing.JButton btnSimpan;
    private javax.swing.JButton btnTambahKategori;
    private javax.swing.JComboBox<String> cbFilterKategori;
    private javax.swing.JComboBox<String> cbKategori;
    private javax.swing.JButton jButton1;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTable jTable1;
    private javax.swing.JTextField txtCari;
    private javax.swing.JTextField txtHarga;
    private javax.swing.JTextField txtNama;
    private javax.swing.JTextField txtStok;
    // End of variables declaration//GEN-END:variables
}
