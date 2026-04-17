package toko.aplikasipos;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import javax.swing.JOptionPane;
import javax.swing.table.DefaultTableModel;

public class KasirFrame extends javax.swing.JFrame {

    private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(KasirFrame.class.getName());

    DefaultTableModel modelKeranjang;
    int totalBelanja = 0; // Variabel untuk menyimpan total uang

    public KasirFrame() {
        initComponents();

        // URUTANNYA HARUS SEPERTI INI:
        loadKategoriKasir(); // 1. Muat kategori dulu
        loadBarang();        // 2. Baru muat barang

        modelKeranjang = new DefaultTableModel();
        modelKeranjang.addColumn("Nama Barang");
        modelKeranjang.addColumn("Harga");
        modelKeranjang.addColumn("Qty");
        modelKeranjang.addColumn("Subtotal");
        tblKeranjang.setModel(modelKeranjang);
    }

    private void loadBarang() {
        // Hindari error NullPointer saat form baru pertama kali dibangun
        if (cbPilihBarang == null || cbKategori == null) {
            return;
        }

        cbPilihBarang.removeAllItems();
        String url = "jdbc:sqlite:pos_db.db";

        // Ambil kategori yang sedang dipilih
        String kategoriPilihan = "Semua Kategori";
        if (cbKategori.getSelectedItem() != null) {
            kategoriPilihan = cbKategori.getSelectedItem().toString();
        }

        // Siapkan query dasar
        String sql = "SELECT nama_barang FROM barang WHERE stok > 0";

        // Tambahkan filter jika bukan "Semua Kategori"
        if (!kategoriPilihan.equals("Semua Kategori")) {
            sql += " AND kategori = ?";
        }

        sql += " ORDER BY nama_barang ASC";

        try (Connection conn = DriverManager.getConnection(url); PreparedStatement pstmt = conn.prepareStatement(sql)) {

            // Masukkan parameter kategori jika filter aktif
            if (!kategoriPilihan.equals("Semua Kategori")) {
                pstmt.setString(1, kategoriPilihan);
            }

            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                cbPilihBarang.addItem(rs.getString("nama_barang"));
            }

            // Panggil event agar label harga/stok menyesuaikan dengan barang pertama di list
            cbPilihBarangActionPerformed(null);

        } catch (Exception e) {
            System.out.println("Error Load Barang: " + e.getMessage());
        }
    }

    private void loadKategoriKasir() {
        cbKategori.removeAllItems();
        cbKategori.addItem("Semua Kategori"); // Opsi default

        String url = "jdbc:sqlite:pos_db.db";

        try (Connection conn = DriverManager.getConnection(url); Statement st = conn.createStatement(); ResultSet rs = st.executeQuery("SELECT nama_kategori FROM kategori ORDER BY nama_kategori ASC")) {

            while (rs.next()) {
                cbKategori.addItem(rs.getString("nama_kategori"));
            }

        } catch (Exception e) {
            System.out.println("Error Load Kategori Kasir: " + e.getMessage());
        }
    }

    private void hitungTotal() {
        int subtotal = 0;
        // Jumlahkan semua subtotal dari tabel keranjang
        for (int i = 0; i < modelKeranjang.getRowCount(); i++) {
            subtotal += Integer.parseInt(modelKeranjang.getValueAt(i, 3).toString());
        }

        try {
            // Ambil nilai langsung dari JSpinner (Cast ke Number lalu jadikan double untuk perhitungan pecahan)
            double diskonPersen = ((Number) spnDiskon.getValue()).doubleValue();
            double pajakPersen = ((Number) spnPajak.getValue()).doubleValue();

            // Hitung Potongan dan Pajak
            double potonganDiskon = subtotal * (diskonPersen / 100.0);
            double setelahDiskon = subtotal - potonganDiskon;
            double nominalPajak = setelahDiskon * (pajakPersen / 100.0);

            // Total Akhir (Dibulatkan)
            totalBelanja = (int) Math.round(setelahDiskon + nominalPajak);

            lblTotalHarga.setText("Total: Rp. " + totalBelanja);

            // Panggil event kembalian agar nilai kembalian ikut terupdate jika kasir sudah mengetik uang bayar
            txtBayarKeyReleased(null);

        } catch (Exception e) {
            System.out.println("Error hitung total: " + e.getMessage());
        }
    }

    private void resetKasir() {
        // Kosongkan tabel keranjang
        modelKeranjang.setRowCount(0);

        // Kembalikan total ke 0
        totalBelanja = 0;
        lblTotalHarga.setText("Total: Rp. 0");

        // Kosongkan input dan label bayar/kembalian
        txtBayar.setText("");
        lblKembalian.setText("Kembalian: Rp. 0");

        spnDiskon.setValue(0);
        spnPajak.setValue(0);

        // Muat ulang daftar barang di ComboBox (agar barang yang stoknya habis setelah transaksi ini hilang dari daftar)
        loadBarang();
    }

    private void cetakStruk(int total, int diskon, int pajak, int bayar, int kembalian, String kasir, String jenisBayar) {
        // 1. AMBIL DATA PROFIL TOKO DARI DATABASE
        String namaToko = "NAMA TOKO";
        String alamatToko = "Alamat Toko";
        String url = "jdbc:sqlite:pos_db.db";

        try (Connection conn = DriverManager.getConnection(url); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery("SELECT nama_toko, alamat FROM profil_toko LIMIT 1")) {

            if (rs.next()) {
                // Ambil data dari database ke variabel
                namaToko = rs.getString("nama_toko");
                alamatToko = rs.getString("alamat");
            }
        } catch (Exception e) {
            System.out.println("Error load profil toko untuk struk: " + e.getMessage());
        }

// 2. MULAI SUSUN TEKS STRUK
        StringBuilder struk = new StringBuilder();
        int lebarStruk = 36; // Sesuai dengan jumlah tanda "=" di bawah ini

        struk.append("====================================\n");

        // Gunakan fungsi centerText agar teks otomatis berada di tengah
        struk.append(centerText(namaToko, lebarStruk)).append("\n");
        struk.append(centerText(alamatToko, lebarStruk)).append("\n");

        // Buat format tanggal yang ringkas (Hari-Bulan-Tahun Jam:Menit)
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd-MM-yyyy HH:mm");
        String tanggalRingkas = sdf.format(new java.util.Date());

        struk.append("====================================\n");
        struk.append("Kasir : ").append(kasir).append("\n");
        struk.append("Tgl   : ").append(tanggalRingkas).append("\n");
        struk.append("------------------------------------\n");

        for (int i = 0; i < modelKeranjang.getRowCount(); i++) {
            String nama = modelKeranjang.getValueAt(i, 0).toString();
            String harga = modelKeranjang.getValueAt(i, 1).toString();
            String qty = modelKeranjang.getValueAt(i, 2).toString();
            String sub = modelKeranjang.getValueAt(i, 3).toString();

            struk.append(nama).append("\n");
            struk.append(qty).append(" x Rp ").append(harga)
                    .append("             Rp ").append(sub).append("\n");
        }

        struk.append("------------------------------------\n");
        struk.append("Diskon      : ").append(diskon).append("%\n");
        struk.append("Pajak (PPN) : ").append(pajak).append("%\n");
        struk.append("Total Akhir : Rp ").append(total).append("\n");
        struk.append("Metode Bayar: ").append(jenisBayar).append("\n");
        struk.append("Bayar       : Rp ").append(bayar).append("\n");
        struk.append("Kembalian   : Rp ").append(kembalian).append("\n");
        struk.append("====================================\n");
        struk.append("   TERIMA KASIH ATAS KUNJUNGANNYA   \n");
        struk.append("====================================\n");

        // 3. TAMPILKAN DI LAYAR DAN OPSI CETAK FISIK
        javax.swing.JTextArea txtStruk = new javax.swing.JTextArea(struk.toString());
        txtStruk.setFont(new java.awt.Font("Monospaced", java.awt.Font.PLAIN, 12));
        txtStruk.setEditable(false);

        javax.swing.JOptionPane.showMessageDialog(this, new javax.swing.JScrollPane(txtStruk), "Struk Pembayaran", javax.swing.JOptionPane.INFORMATION_MESSAGE);

        int print = javax.swing.JOptionPane.showConfirmDialog(this, "Cetak struk ini ke printer fisik?", "Print", javax.swing.JOptionPane.YES_NO_OPTION);
        if (print == javax.swing.JOptionPane.YES_OPTION) {
            try {
                txtStruk.print();
            } catch (Exception e) {
                javax.swing.JOptionPane.showMessageDialog(this, "Gagal print: " + e.getMessage(), "Error", javax.swing.JOptionPane.ERROR_MESSAGE);
            }
        }
    }

// Fungsi untuk menengahkan teks pada struk (lebar default 36 karakter)
    private String centerText(String text, int width) {
        if (text.length() >= width) {
            return text; // Jika teks sudah kepanjangan, biarkan saja
        }
        int padding = (width - text.length()) / 2;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < padding; i++) {
            sb.append(" "); // Tambahkan spasi di kiri
        }
        sb.append(text);
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        cbPilihBarang = new javax.swing.JComboBox<>();
        jLabel1 = new javax.swing.JLabel();
        btnTambahKeranjang = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        tblKeranjang = new javax.swing.JTable();
        lblTotalHarga = new javax.swing.JLabel();
        txtBayar = new javax.swing.JTextField();
        jLabel2 = new javax.swing.JLabel();
        lblKembalian = new javax.swing.JLabel();
        btnSimpanTransaksi = new javax.swing.JButton();
        spnQty = new javax.swing.JSpinner();
        lblInfoHarga = new javax.swing.JLabel();
        lblInfoStok = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        cbKategori = new javax.swing.JComboBox<>();
        spnDiskon = new javax.swing.JSpinner();
        spnPajak = new javax.swing.JSpinner();
        cbJenisBayar = new javax.swing.JComboBox<>();
        jLabel4 = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        cbPilihBarang.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        cbPilihBarang.addActionListener(this::cbPilihBarangActionPerformed);

        jLabel1.setText("Qty");

        btnTambahKeranjang.setText("Tambah Keranjang");
        btnTambahKeranjang.addActionListener(this::btnTambahKeranjangActionPerformed);

        tblKeranjang.setModel(new javax.swing.table.DefaultTableModel(
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
        jScrollPane1.setViewportView(tblKeranjang);

        lblTotalHarga.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        lblTotalHarga.setText("Rp. 0");

        txtBayar.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                txtBayarKeyReleased(evt);
            }
        });

        jLabel2.setText("Bayar");

        lblKembalian.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        lblKembalian.setText("Rp. 0");

        btnSimpanTransaksi.setText("Simpan Transaksi");
        btnSimpanTransaksi.addActionListener(this::btnSimpanTransaksiActionPerformed);

        spnQty.setModel(new javax.swing.SpinnerNumberModel(1, 1, null, 1));

        lblInfoHarga.setText("Harga: Rp. 0");

        lblInfoStok.setText("Sisa Stok: 0");

        jLabel3.setText("Pilih Kategori");

        cbKategori.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        cbKategori.addActionListener(this::cbKategoriActionPerformed);

        spnDiskon.setModel(new javax.swing.SpinnerNumberModel(0, 0, 100, 1));
        spnDiskon.addChangeListener(this::spnDiskonStateChanged);

        spnPajak.setModel(new javax.swing.SpinnerNumberModel(0, 0, 100, 1));
        spnPajak.addChangeListener(this::spnPajakStateChanged);

        cbJenisBayar.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Tunai", "Debit", "Qris" }));

        jLabel4.setText("Jenis Bayar");

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                        .addGroup(jPanel1Layout.createSequentialGroup()
                            .addComponent(jLabel1)
                            .addGap(111, 111, 111))
                        .addComponent(spnQty, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(cbPilihBarang, javax.swing.GroupLayout.PREFERRED_SIZE, 130, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lblInfoHarga)
                    .addComponent(lblInfoStok)
                    .addComponent(btnTambahKeranjang))
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(18, 18, 18)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                                .addComponent(jLabel2)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(jPanel1Layout.createSequentialGroup()
                                        .addComponent(txtBayar, javax.swing.GroupLayout.PREFERRED_SIZE, 120, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addGap(18, 18, 18)
                                        .addComponent(lblTotalHarga))
                                    .addGroup(jPanel1Layout.createSequentialGroup()
                                        .addComponent(btnSimpanTransaksi)
                                        .addGap(18, 18, 18)
                                        .addComponent(lblKembalian)))
                                .addGap(208, 208, 208))
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(71, 71, 71)
                        .addComponent(jLabel3)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(cbKategori, javax.swing.GroupLayout.PREFERRED_SIZE, 141, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 0, Short.MAX_VALUE))))
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(162, 162, 162)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(spnDiskon, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(spnPajak, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jLabel4)
                .addGap(69, 69, 69)
                .addComponent(cbJenisBayar, javax.swing.GroupLayout.PREFERRED_SIZE, 131, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(40, 40, 40))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(cbPilihBarang, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel3)
                    .addComponent(cbKategori, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(lblInfoHarga)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(18, 18, 18)
                        .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 167, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(lblInfoStok)
                        .addGap(18, 18, 18)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel1)
                            .addComponent(spnQty, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(18, 18, 18)
                        .addComponent(btnTambahKeranjang)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(txtBayar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel2)
                    .addComponent(lblTotalHarga))
                .addGap(9, 9, 9)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnSimpanTransaksi)
                    .addComponent(lblKembalian))
                .addGap(27, 27, 27)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(spnDiskon, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(cbJenisBayar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel4))
                .addGap(18, 18, 18)
                .addComponent(spnPajak, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(87, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 6, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btnTambahKeranjangActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnTambahKeranjangActionPerformed
// Pastikan ada barang yang dipilih
        if (cbPilihBarang.getSelectedItem() == null) {
            JOptionPane.showMessageDialog(this, "Pilih barang terlebih dahulu!");
            return;
        }

        String namaBarang = cbPilihBarang.getSelectedItem().toString();

        // 1. Ambil nilai langsung dari JSpinner
        int qty = (Integer) spnQty.getValue();

        // 2. BARIS INI YANG KEMUNGKINAN HILANG
        String url = "jdbc:sqlite:pos_db.db";

        try (Connection conn = DriverManager.getConnection(url); PreparedStatement pstmt = conn.prepareStatement("SELECT harga, stok FROM barang WHERE nama_barang = ?")) {

            pstmt.setString(1, namaBarang);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                int harga = rs.getInt("harga");
                int stokTersedia = rs.getInt("stok");

                // Cek apakah stok mencukupi
                if (qty > stokTersedia) {
                    JOptionPane.showMessageDialog(this, "Stok tidak mencukupi! Sisa stok: " + stokTersedia);
                    return;
                }

                int subtotal = harga * qty;

                // Masukkan ke tabel keranjang
                modelKeranjang.addRow(new Object[]{
                    namaBarang,
                    harga,
                    qty,
                    subtotal
                });

                // Update Total Belanja
                hitungTotal();

                // 3. Kembalikan nilai spinner ke angka 1
                spnQty.setValue(1);
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error Tambah Keranjang: " + e.getMessage());
        }
    }//GEN-LAST:event_btnTambahKeranjangActionPerformed

    private void txtBayarKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_txtBayarKeyReleased
        try {
            // Ambil nominal bayar, ubah ke angka
            String bayarStr = txtBayar.getText();
            if (bayarStr.isEmpty()) {
                lblKembalian.setText("Kembalian: Rp. 0");
                return;
            }

            int bayar = Integer.parseInt(bayarStr);
            int kembalian = bayar - totalBelanja;

            // Tampilkan ke label kembalian
            if (kembalian < 0) {
                lblKembalian.setText("Uang Kurang!");
            } else {
                lblKembalian.setText("Kembalian: Rp. " + kembalian);
            }
        } catch (NumberFormatException e) {
            // Abaikan jika kasir mengetik huruf, atau kosongkan
            lblKembalian.setText("Input tidak valid!");
        }
    }//GEN-LAST:event_txtBayarKeyReleased

    private void btnSimpanTransaksiActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSimpanTransaksiActionPerformed
// 1. Validasi awal: Keranjang tidak boleh kosong
        if (totalBelanja == 0) {
            JOptionPane.showMessageDialog(this, "Keranjang belanja masih kosong!", "Peringatan", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // 2. Validasi pembayaran
        String bayarStr = txtBayar.getText();
        if (bayarStr.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Masukkan nominal pembayaran!", "Peringatan", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int bayar = Integer.parseInt(bayarStr);
        if (bayar < totalBelanja) {
            JOptionPane.showMessageDialog(this, "Uang pembayaran kurang!", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        int kembalian = bayar - totalBelanja;

        // 3. Ambil data tambahan dari komponen GUI Kasir
        int nilaiDiskon = (Integer) spnDiskon.getValue();
        int nilaiPajak = (Integer) spnPajak.getValue();

// Ambil nama kasir langsung dari variabel global LoginFrame
        String namaKasir = LoginFrame.kasirAktif;
// Jika tiba-tiba kosong (misal di-run tanpa lewat login), beri default
        if (namaKasir.isEmpty()) {
            namaKasir = "Admin Default";
        }

        // Ambil jenis bayar (Tunai, Debit, QRIS)
        String jenisBayar = cbJenisBayar.getSelectedItem().toString();

        // 4. Siapkan URL Database
        String url = "jdbc:sqlite:pos_db.db";

        try (Connection conn = DriverManager.getConnection(url)) {

            // --- PROSES 1: Insert ke tabel transaksi ---
            String sqlTransaksi = "INSERT INTO transaksi (total_harga, diskon_persen, pajak_persen, bayar, kembalian) VALUES (?, ?, ?, ?, ?)";
            try (PreparedStatement pstTrans = conn.prepareStatement(sqlTransaksi, Statement.RETURN_GENERATED_KEYS)) {
                pstTrans.setInt(1, totalBelanja);
                pstTrans.setInt(2, nilaiDiskon);
                pstTrans.setInt(3, nilaiPajak);
                pstTrans.setInt(4, bayar);
                pstTrans.setInt(5, kembalian);
                pstTrans.executeUpdate();

                // Ambil ID Transaksi yang baru saja dibuat oleh SQLite
                ResultSet rsKeys = pstTrans.getGeneratedKeys();
                int idTransaksi = -1;
                if (rsKeys.next()) {
                    idTransaksi = rsKeys.getInt(1);
                }

                // --- PROSES 2: Loop isi keranjang untuk detail dan kurangi stok ---
                String sqlDetail = "INSERT INTO detail_transaksi (id_transaksi, nama_barang, harga, qty, subtotal) VALUES (?, ?, ?, ?, ?)";
                String sqlUpdateStok = "UPDATE barang SET stok = stok - ? WHERE nama_barang = ?";

                try (PreparedStatement pstDetail = conn.prepareStatement(sqlDetail); PreparedStatement pstUpdateStok = conn.prepareStatement(sqlUpdateStok)) {

                    int jumlahBaris = modelKeranjang.getRowCount();

                    for (int i = 0; i < jumlahBaris; i++) {
                        String nama = modelKeranjang.getValueAt(i, 0).toString();
                        int harga = Integer.parseInt(modelKeranjang.getValueAt(i, 1).toString());
                        int qty = Integer.parseInt(modelKeranjang.getValueAt(i, 2).toString());
                        int subtotal = Integer.parseInt(modelKeranjang.getValueAt(i, 3).toString());

                        // Simpan ke tabel detail_transaksi
                        pstDetail.setInt(1, idTransaksi);
                        pstDetail.setString(2, nama);
                        pstDetail.setInt(3, harga);
                        pstDetail.setInt(4, qty);
                        pstDetail.setInt(5, subtotal);
                        pstDetail.executeUpdate();

                        // Kurangi stok di tabel barang
                        pstUpdateStok.setInt(1, qty);
                        pstUpdateStok.setString(2, nama);
                        pstUpdateStok.executeUpdate();
                    }
                }

                // --- PROSES 3: Selesai! Tampilkan sukses, cetak nota, dan reset ---
                JOptionPane.showMessageDialog(this, "Transaksi Berhasil!\nKembalian: Rp. " + kembalian);

                // Panggil fungsi cetak struk (parameter harus sesuai dengan yang di-update di langkah sebelumnya)
                cetakStruk(totalBelanja, nilaiDiskon, nilaiPajak, bayar, kembalian, namaKasir, jenisBayar);

                // Bersihkan form kasir untuk pelanggan berikutnya
                resetKasir();
            }

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error Transaksi: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }//GEN-LAST:event_btnSimpanTransaksiActionPerformed

    private void cbPilihBarangActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cbPilihBarangActionPerformed
        // Pastikan ComboBox tidak dalam keadaan kosong
        if (cbPilihBarang.getSelectedItem() == null) {
            lblInfoHarga.setText("Harga: Rp. 0");
            lblInfoStok.setText("Sisa Stok: 0");
            return;
        }

        String namaBarang = cbPilihBarang.getSelectedItem().toString();
        String url = "jdbc:sqlite:pos_db.db";

        try (Connection conn = DriverManager.getConnection(url); PreparedStatement pstmt = conn.prepareStatement("SELECT harga, stok FROM barang WHERE nama_barang = ?")) {

            pstmt.setString(1, namaBarang);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                // Tampilkan harga dan stok secara real-time ke layar
                lblInfoHarga.setText("Harga: Rp. " + rs.getInt("harga"));
                lblInfoStok.setText("Sisa Stok: " + rs.getInt("stok"));
            }

        } catch (Exception e) {
            System.out.println("Error Cek Info Barang: " + e.getMessage());
        }
    }//GEN-LAST:event_cbPilihBarangActionPerformed

    private void cbKategoriActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cbKategoriActionPerformed
        loadBarang();
    }//GEN-LAST:event_cbKategoriActionPerformed

    private void spnDiskonStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_spnDiskonStateChanged
        hitungTotal();
    }//GEN-LAST:event_spnDiskonStateChanged

    private void spnPajakStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_spnPajakStateChanged
        hitungTotal();
    }//GEN-LAST:event_spnPajakStateChanged

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
        java.awt.EventQueue.invokeLater(() -> new KasirFrame().setVisible(true));
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnSimpanTransaksi;
    private javax.swing.JButton btnTambahKeranjang;
    private javax.swing.JComboBox<String> cbJenisBayar;
    private javax.swing.JComboBox<String> cbKategori;
    private javax.swing.JComboBox<String> cbPilihBarang;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JLabel lblInfoHarga;
    private javax.swing.JLabel lblInfoStok;
    private javax.swing.JLabel lblKembalian;
    private javax.swing.JLabel lblTotalHarga;
    private javax.swing.JSpinner spnDiskon;
    private javax.swing.JSpinner spnPajak;
    private javax.swing.JSpinner spnQty;
    private javax.swing.JTable tblKeranjang;
    private javax.swing.JTextField txtBayar;
    // End of variables declaration//GEN-END:variables
}
