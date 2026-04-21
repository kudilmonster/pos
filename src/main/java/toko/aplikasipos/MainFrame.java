package toko.aplikasipos;

import java.awt.Color;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Statement;
import javax.swing.table.DefaultTableModel;
import java.sql.PreparedStatement;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

public class MainFrame extends javax.swing.JFrame {

    private static final String DB_URL = "jdbc:sqlite:pos_db.db";

    private Connection getConnection() throws Exception {
        return DriverManager.getConnection(DB_URL);
    }

    private int parseInteger(String value, String fieldName) throws Exception {
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            throw new Exception(fieldName + " harus berupa angka!");
        }
    }

    private void validateNonNegative(int value, String fieldName) throws Exception {
        if (value < 0) {
            throw new Exception(fieldName + " tidak boleh negatif!");
        }
    }
    
    private KasirFrame formKasirAktif = null;
    private AdminWorkspaceFrame formWorkspaceAktif = null;
    
    private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(MainFrame.class.getName());
    private String idBarangTerpilih = "";

    public MainFrame() {
        initComponents();
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        initDatabase();    // Membuat tabel
        applyRolePermissions();
        loadDataUser();
        loadKategori();    // <- TAMBAHKAN BARIS INI: Mengisi ComboBox
        tampilkanData();   // Menampilkan data di tabel
        
        setBackground(new Color(0, 0, 0, 0));
    }

    private void applyRolePermissions() {
        boolean isAdmin = "Admin".equalsIgnoreCase(LoginFrame.roleAktif);
        if (isAdmin) {
            return;
        }

        // Batasi menu sensitif untuk non-admin jika suatu saat MainFrame dibuka dari role lain.
        btnHapus.setEnabled(false);
        btnHapusUser.setEnabled(false);
        btnSimpanUser.setEnabled(false);
        btnLaporan.setEnabled(false);
    }

    private void resetForm() {
        txtNama.setText("");
        txtHarga.setText("");
        txtStok.setText("");
        cbKategori.setSelectedIndex(0);
        idBarangTerpilih = "";
    }

    private boolean tableHasColumn(Connection conn, String tableName, String columnName) throws Exception {
        DatabaseMetaData meta = conn.getMetaData();
        try (ResultSet rs = meta.getColumns(null, null, tableName, columnName)) {
            return rs.next();
        }
    }

    private void addColumnIfMissing(Connection conn, String tableName, String columnName, String typeDef) throws Exception {
        if (!tableHasColumn(conn, tableName, columnName)) {
            try (Statement st = conn.createStatement()) {
                st.execute("ALTER TABLE " + tableName + " ADD COLUMN " + columnName + " " + typeDef);
            }
        }
    }

    private void initDatabase() {
        try (Connection conn = DriverManager.getConnection(DB_URL); Statement stmt = conn.createStatement()) {

            // KATEGORI
            stmt.execute("""
            CREATE TABLE IF NOT EXISTS kategori (
                id_kategori INTEGER PRIMARY KEY AUTOINCREMENT,
                nama_kategori TEXT UNIQUE NOT NULL
            )
        """);

            // BARANG
            stmt.execute("""
            CREATE TABLE IF NOT EXISTS barang (
                id_barang INTEGER PRIMARY KEY AUTOINCREMENT,
                nama_barang TEXT UNIQUE NOT NULL,
                kategori TEXT NOT NULL,
                harga INTEGER NOT NULL,
                stok INTEGER NOT NULL
            )
        """);

            addColumnIfMissing(conn, "barang", "stok_minimum", "INTEGER DEFAULT 5");
            addColumnIfMissing(conn, "barang", "harga_modal", "INTEGER DEFAULT 0");

            // TRANSAKSI
            stmt.execute("""
            CREATE TABLE IF NOT EXISTS transaksi (
                id_transaksi INTEGER PRIMARY KEY AUTOINCREMENT,
                tanggal DATETIME DEFAULT CURRENT_TIMESTAMP,
                total_harga INTEGER,
                diskon_persen INTEGER,
                pajak_persen INTEGER,
                bayar INTEGER,
                kembalian INTEGER
            )
        """);

            // DETAIL TRANSAKSI
            stmt.execute("""
            CREATE TABLE IF NOT EXISTS detail_transaksi (
                id_detail INTEGER PRIMARY KEY AUTOINCREMENT,
                id_transaksi INTEGER,
                nama_barang TEXT,
                harga INTEGER,
                qty INTEGER,
                subtotal INTEGER
            )
        """);

            addColumnIfMissing(conn, "transaksi", "status_transaksi", "TEXT DEFAULT 'NORMAL'");
            addColumnIfMissing(conn, "transaksi", "catatan_status", "TEXT");
            addColumnIfMissing(conn, "transaksi", "updated_by", "TEXT");
            addColumnIfMissing(conn, "detail_transaksi", "harga_modal", "INTEGER DEFAULT 0");
            addColumnIfMissing(conn, "detail_transaksi", "laba_kotor", "INTEGER DEFAULT 0");

            // ✅ USERS (FIX ERROR BESAR)
            stmt.execute("""
            CREATE TABLE IF NOT EXISTS users (
                id_user INTEGER PRIMARY KEY AUTOINCREMENT,
                username TEXT UNIQUE NOT NULL,
                password TEXT NOT NULL,
                role TEXT NOT NULL
            )
        """);

            // SUPPLIER + PEMBELIAN
            stmt.execute("""
            CREATE TABLE IF NOT EXISTS supplier (
                id_supplier INTEGER PRIMARY KEY AUTOINCREMENT,
                nama_supplier TEXT UNIQUE NOT NULL,
                kontak TEXT,
                alamat TEXT
            )
        """);

            stmt.execute("""
            CREATE TABLE IF NOT EXISTS pembelian (
                id_pembelian INTEGER PRIMARY KEY AUTOINCREMENT,
                tanggal DATETIME DEFAULT CURRENT_TIMESTAMP,
                id_supplier INTEGER,
                total_beli INTEGER DEFAULT 0,
                dibuat_oleh TEXT
            )
        """);

            stmt.execute("""
            CREATE TABLE IF NOT EXISTS detail_pembelian (
                id_detail_beli INTEGER PRIMARY KEY AUTOINCREMENT,
                id_pembelian INTEGER,
                nama_barang TEXT,
                qty INTEGER,
                harga_beli INTEGER,
                subtotal INTEGER
            )
        """);

            // RETUR / VOID LOG
            stmt.execute("""
            CREATE TABLE IF NOT EXISTS retur_transaksi (
                id_retur INTEGER PRIMARY KEY AUTOINCREMENT,
                id_transaksi INTEGER,
                tanggal DATETIME DEFAULT CURRENT_TIMESTAMP,
                kasir TEXT,
                jenis TEXT,
                alasan TEXT,
                total_retur INTEGER DEFAULT 0
            )
        """);

            stmt.execute("""
            CREATE TABLE IF NOT EXISTS detail_retur (
                id_detail_retur INTEGER PRIMARY KEY AUTOINCREMENT,
                id_retur INTEGER,
                nama_barang TEXT,
                qty INTEGER,
                harga INTEGER,
                subtotal INTEGER
            )
        """);

            // INSERT DEFAULT KATEGORI
            try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) AS total FROM kategori")) {
                if (rs.next() && rs.getInt("total") == 0) {
                    stmt.execute("INSERT INTO kategori (nama_kategori) VALUES ('Makanan')");
                    stmt.execute("INSERT INTO kategori (nama_kategori) VALUES ('Minuman')");
                    stmt.execute("INSERT INTO kategori (nama_kategori) VALUES ('Sembako')");
                }
            }

            System.out.println("Database siap!");

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error DB: " + e.getMessage());
        }
    }

    private void tampilkanData() {
        DefaultTableModel model = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

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

    private void loadDataUser() {
        // Buat tabel Read-Only (Tidak bisa diklik ganda/diedit manual)
        DefaultTableModel model = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        model.addColumn("ID User");
        model.addColumn("Username");
        model.addColumn("Role");

        tblUser.setModel(model);

        String url = "jdbc:sqlite:pos_db.db";

        try (Connection conn = DriverManager.getConnection(url); Statement st = conn.createStatement(); ResultSet rs = st.executeQuery("SELECT id_user, username, role FROM users")) {

            while (rs.next()) {
                model.addRow(new Object[]{
                    rs.getInt("id_user"),
                    rs.getString("username"),
                    rs.getString("role")
                });
            }
        } catch (Exception e) {
            System.out.println("Error Load Data User: " + e.getMessage());
        }
    }

    private void cariData() {
        DefaultTableModel model = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

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

        PanelUtama = new toko.aplikasipos.CustomRoundedPanel();
        jLabel7 = new javax.swing.JLabel();
        PanelCenter = new toko.aplikasipos.CustomRoundedPanel();
        panelInputBarang = new toko.aplikasipos.CustomRoundedPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTable1 = new javax.swing.JTable();
        panelSEH = new toko.aplikasipos.CustomRoundedPanel();
        btnHapusKategori = new javax.swing.JButton();
        cbKategori = new javax.swing.JComboBox<>();
        btnTambahKategori = new javax.swing.JButton();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        txtNama = new javax.swing.JTextField();
        txtHarga = new javax.swing.JTextField();
        txtStok = new javax.swing.JTextField();
        btnSimpan = new javax.swing.JButton();
        btnEdit = new javax.swing.JButton();
        btnHapus = new javax.swing.JButton();
        CariBarang = new toko.aplikasipos.CustomRoundedPanel();
        txtCari = new javax.swing.JTextField();
        jLabel4 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        cbFilterKategori = new javax.swing.JComboBox<>();
        panelAkunUser = new toko.aplikasipos.CustomRoundedPanel();
        jScrollPane2 = new javax.swing.JScrollPane();
        tblUser = new javax.swing.JTable();
        panelAkun = new toko.aplikasipos.CustomRoundedPanel();
        lblUsername = new javax.swing.JLabel();
        lblPassword = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        btnLaporan = new javax.swing.JButton();
        txtUsernameUser = new javax.swing.JTextField();
        txtPasswordUser = new javax.swing.JPasswordField();
        cbRoleUser = new javax.swing.JComboBox<>();
        lblMenuKasir = new javax.swing.JButton();
        btnSimpanUser = new javax.swing.JButton();
        btnHapusUser = new javax.swing.JButton();
        PanelLink = new toko.aplikasipos.CustomRoundedPanel();
        lblLinkSosmed = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setUndecorated(true);

        PanelUtama.setcolorEnd(new java.awt.Color(72, 126, 176));
        PanelUtama.setcolorStart(new java.awt.Color(72, 126, 176));

        jLabel7.setFont(new java.awt.Font("Times New Roman", 1, 18)); // NOI18N
        jLabel7.setForeground(new java.awt.Color(204, 204, 204));
        jLabel7.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel7.setText("sanFK POS");
        jLabel7.setToolTipText("Close");
        jLabel7.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        jLabel7.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jLabel7.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jLabel7MouseClicked(evt);
            }
        });

        PanelCenter.setBottomLeftRound(false);
        PanelCenter.setBottomRightRound(false);
        PanelCenter.setcolorEnd(new java.awt.Color(72, 126, 176));
        PanelCenter.setcolorStart(new java.awt.Color(72, 126, 176));
        PanelCenter.setTopLeftRound(false);
        PanelCenter.setTopRightRound(false);

        panelInputBarang.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Input Stok Barang", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Segoe UI Black", 1, 18), new java.awt.Color(255, 255, 255))); // NOI18N
        panelInputBarang.setBottomLeftRound(false);
        panelInputBarang.setBottomRightRound(false);
        panelInputBarang.setcolorEnd(new java.awt.Color(39, 60, 117));
        panelInputBarang.setcolorStart(new java.awt.Color(39, 60, 117));
        panelInputBarang.setMinimumSize(new java.awt.Dimension(300, 200));
        panelInputBarang.setName(""); // NOI18N
        panelInputBarang.setPreferredSize(new java.awt.Dimension(300, 200));
        panelInputBarang.setTopLeftRound(false);
        panelInputBarang.setTopRightRound(false);

        jTable1.setFont(new java.awt.Font("Segoe UI Semibold", 0, 14)); // NOI18N
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

        panelSEH.setBorder(javax.swing.BorderFactory.createEmptyBorder(5, 5, 5, 5));
        panelSEH.setBottomLeftRound(false);
        panelSEH.setBottomRightRound(false);
        panelSEH.setcolorEnd(new java.awt.Color(39, 60, 117));
        panelSEH.setcolorStart(new java.awt.Color(39, 60, 117));
        panelSEH.setTopLeftRound(false);
        panelSEH.setTopRightRound(false);
        panelSEH.setLayout(new java.awt.GridLayout(4, 0, 5, 5));

        btnHapusKategori.setFont(new java.awt.Font("Segoe UI Semibold", 0, 12)); // NOI18N
        btnHapusKategori.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icon/delete.png"))); // NOI18N
        btnHapusKategori.setText("Hapus Kategori");
        btnHapusKategori.addActionListener(this::btnHapusKategoriActionPerformed);
        panelSEH.add(btnHapusKategori);

        cbKategori.setFont(new java.awt.Font("Segoe UI Semibold", 1, 18)); // NOI18N
        cbKategori.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Makanan", "Minuman", "Sembako" }));
        cbKategori.setPreferredSize(new java.awt.Dimension(85, 22));
        panelSEH.add(cbKategori);

        btnTambahKategori.setFont(new java.awt.Font("Segoe UI Semibold", 0, 12)); // NOI18N
        btnTambahKategori.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icon/plus.png"))); // NOI18N
        btnTambahKategori.setText("Tambah Kategori");
        btnTambahKategori.addActionListener(this::btnTambahKategoriActionPerformed);
        panelSEH.add(btnTambahKategori);

        jLabel1.setFont(new java.awt.Font("Segoe UI Semibold", 0, 14)); // NOI18N
        jLabel1.setForeground(new java.awt.Color(255, 255, 255));
        jLabel1.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel1.setText("Nama Barang");
        jLabel1.setBorder(new javax.swing.border.LineBorder(new java.awt.Color(255, 255, 255), 2, true));
        jLabel1.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jLabel1.setPreferredSize(new java.awt.Dimension(50, 30));
        panelSEH.add(jLabel1);

        jLabel2.setFont(new java.awt.Font("Segoe UI Semibold", 0, 14)); // NOI18N
        jLabel2.setForeground(new java.awt.Color(255, 255, 255));
        jLabel2.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel2.setText("Harga");
        jLabel2.setBorder(new javax.swing.border.LineBorder(new java.awt.Color(255, 255, 255), 2, true));
        jLabel2.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        panelSEH.add(jLabel2);

        jLabel3.setFont(new java.awt.Font("Segoe UI Semibold", 0, 14)); // NOI18N
        jLabel3.setForeground(new java.awt.Color(255, 255, 255));
        jLabel3.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel3.setText("Stok");
        jLabel3.setBorder(new javax.swing.border.LineBorder(new java.awt.Color(255, 255, 255), 2, true));
        jLabel3.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        panelSEH.add(jLabel3);

        txtNama.setFont(new java.awt.Font("Segoe UI Semibold", 0, 14)); // NOI18N
        panelSEH.add(txtNama);

        txtHarga.setFont(new java.awt.Font("Segoe UI Semibold", 0, 14)); // NOI18N
        panelSEH.add(txtHarga);

        txtStok.setFont(new java.awt.Font("Segoe UI Semibold", 0, 14)); // NOI18N
        panelSEH.add(txtStok);

        btnSimpan.setBackground(new java.awt.Color(46, 213, 115));
        btnSimpan.setFont(new java.awt.Font("Segoe UI Semibold", 0, 18)); // NOI18N
        btnSimpan.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icon/data.png"))); // NOI18N
        btnSimpan.setText("Simpan");
        btnSimpan.addActionListener(this::btnSimpanActionPerformed);
        panelSEH.add(btnSimpan);

        btnEdit.setBackground(new java.awt.Color(30, 144, 255));
        btnEdit.setFont(new java.awt.Font("Segoe UI Semibold", 0, 18)); // NOI18N
        btnEdit.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icon/edit.png"))); // NOI18N
        btnEdit.setText("Edit");
        btnEdit.addActionListener(this::btnEditActionPerformed);
        panelSEH.add(btnEdit);

        btnHapus.setBackground(new java.awt.Color(255, 165, 2));
        btnHapus.setFont(new java.awt.Font("Segoe UI Semibold", 0, 18)); // NOI18N
        btnHapus.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icon/hapus.png"))); // NOI18N
        btnHapus.setText("Hapus");
        btnHapus.addActionListener(this::btnHapusActionPerformed);
        panelSEH.add(btnHapus);

        CariBarang.setBorder(javax.swing.BorderFactory.createEmptyBorder(5, 5, 5, 5));
        CariBarang.setBottomLeftRound(false);
        CariBarang.setBottomRightRound(false);
        CariBarang.setcolorEnd(new java.awt.Color(39, 60, 117));
        CariBarang.setcolorStart(new java.awt.Color(39, 60, 117));
        CariBarang.setTopLeftRound(false);
        CariBarang.setTopRightRound(false);

        txtCari.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                txtCariKeyReleased(evt);
            }
        });

        jLabel4.setFont(new java.awt.Font("Segoe UI Semibold", 0, 14)); // NOI18N
        jLabel4.setForeground(new java.awt.Color(255, 255, 255));
        jLabel4.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel4.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icon/search.png"))); // NOI18N
        jLabel4.setText("Cari Barang");

        jLabel5.setFont(new java.awt.Font("Segoe UI Semibold", 0, 14)); // NOI18N
        jLabel5.setForeground(new java.awt.Color(255, 255, 255));
        jLabel5.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel5.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icon/filter.png"))); // NOI18N
        jLabel5.setText("Filter Kategori");

        cbFilterKategori.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        cbFilterKategori.addActionListener(this::cbFilterKategoriActionPerformed);

        javax.swing.GroupLayout CariBarangLayout = new javax.swing.GroupLayout(CariBarang);
        CariBarang.setLayout(CariBarangLayout);
        CariBarangLayout.setHorizontalGroup(
            CariBarangLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, CariBarangLayout.createSequentialGroup()
                .addGroup(CariBarangLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(txtCari)
                    .addGroup(CariBarangLayout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(jLabel4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(CariBarangLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addGroup(CariBarangLayout.createSequentialGroup()
                        .addComponent(jLabel5, javax.swing.GroupLayout.DEFAULT_SIZE, 217, Short.MAX_VALUE)
                        .addContainerGap())
                    .addComponent(cbFilterKategori, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
        );
        CariBarangLayout.setVerticalGroup(
            CariBarangLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(CariBarangLayout.createSequentialGroup()
                .addGap(6, 6, 6)
                .addGroup(CariBarangLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jLabel4, javax.swing.GroupLayout.PREFERRED_SIZE, 22, Short.MAX_VALUE)
                    .addComponent(jLabel5, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(CariBarangLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(txtCari, javax.swing.GroupLayout.PREFERRED_SIZE, 28, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(cbFilterKategori, javax.swing.GroupLayout.PREFERRED_SIZE, 28, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout panelInputBarangLayout = new javax.swing.GroupLayout(panelInputBarang);
        panelInputBarang.setLayout(panelInputBarangLayout);
        panelInputBarangLayout.setHorizontalGroup(
            panelInputBarangLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelInputBarangLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(panelInputBarangLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(CariBarang, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(panelSEH, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 728, Short.MAX_VALUE))
                .addContainerGap())
        );
        panelInputBarangLayout.setVerticalGroup(
            panelInputBarangLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, panelInputBarangLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(panelSEH, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(CariBarang, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                .addContainerGap())
        );

        panelAkunUser.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Data Akun", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Segoe UI Black", 1, 18), new java.awt.Color(255, 255, 255))); // NOI18N
        panelAkunUser.setBottomLeftRound(false);
        panelAkunUser.setBottomRightRound(false);
        panelAkunUser.setcolorEnd(new java.awt.Color(39, 60, 117));
        panelAkunUser.setcolorStart(new java.awt.Color(39, 60, 117));
        panelAkunUser.setTopLeftRound(false);
        panelAkunUser.setTopRightRound(false);

        tblUser.setModel(new javax.swing.table.DefaultTableModel(
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
        jScrollPane2.setViewportView(tblUser);

        panelAkun.setBorder(javax.swing.BorderFactory.createEmptyBorder(5, 5, 5, 5));
        panelAkun.setBottomLeftRound(false);
        panelAkun.setBottomRightRound(false);
        panelAkun.setcolorEnd(new java.awt.Color(39, 60, 117));
        panelAkun.setcolorStart(new java.awt.Color(39, 60, 117));
        panelAkun.setTopLeftRound(false);
        panelAkun.setTopRightRound(false);
        panelAkun.setLayout(new java.awt.GridLayout(3, 0, 5, 5));

        lblUsername.setFont(new java.awt.Font("Segoe UI Semibold", 0, 14)); // NOI18N
        lblUsername.setForeground(new java.awt.Color(255, 255, 255));
        lblUsername.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblUsername.setText("Username");
        lblUsername.setBorder(new javax.swing.border.LineBorder(new java.awt.Color(255, 255, 255), 2, true));
        lblUsername.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        panelAkun.add(lblUsername);

        lblPassword.setFont(new java.awt.Font("Segoe UI Semibold", 0, 14)); // NOI18N
        lblPassword.setForeground(new java.awt.Color(255, 255, 255));
        lblPassword.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblPassword.setText("Password");
        lblPassword.setBorder(new javax.swing.border.LineBorder(new java.awt.Color(255, 255, 255), 2, true));
        lblPassword.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        panelAkun.add(lblPassword);

        jLabel6.setFont(new java.awt.Font("Segoe UI Semibold", 0, 14)); // NOI18N
        jLabel6.setForeground(new java.awt.Color(255, 255, 255));
        jLabel6.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel6.setText("Role");
        jLabel6.setBorder(new javax.swing.border.LineBorder(new java.awt.Color(255, 255, 255), 2, true));
        jLabel6.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        panelAkun.add(jLabel6);

        btnLaporan.setBackground(new java.awt.Color(204, 255, 255));
        btnLaporan.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icon/laporan.png"))); // NOI18N
        btnLaporan.setText("Laporan");
        btnLaporan.addActionListener(this::btnLaporanActionPerformed);
        panelAkun.add(btnLaporan);

        txtUsernameUser.setFont(new java.awt.Font("Segoe UI Semibold", 0, 18)); // NOI18N
        panelAkun.add(txtUsernameUser);

        txtPasswordUser.setFont(new java.awt.Font("Segoe UI Semibold", 0, 18)); // NOI18N
        panelAkun.add(txtPasswordUser);

        cbRoleUser.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Admin", "Kasir" }));
        panelAkun.add(cbRoleUser);

        lblMenuKasir.setBackground(new java.awt.Color(68, 189, 50));
        lblMenuKasir.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icon/user.png"))); // NOI18N
        lblMenuKasir.setText("Kasir");
        lblMenuKasir.addActionListener(this::lblMenuKasirActionPerformed);
        panelAkun.add(lblMenuKasir);

        btnSimpanUser.setBackground(new java.awt.Color(0, 168, 255));
        btnSimpanUser.setText("Simpan");
        btnSimpanUser.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        btnSimpanUser.addActionListener(this::btnSimpanUserActionPerformed);
        panelAkun.add(btnSimpanUser);

        btnHapusUser.setBackground(new java.awt.Color(232, 65, 24));
        btnHapusUser.setText("Hapus User");
        btnHapusUser.addActionListener(this::btnHapusUserActionPerformed);
        panelAkun.add(btnHapusUser);

        javax.swing.GroupLayout panelAkunUserLayout = new javax.swing.GroupLayout(panelAkunUser);
        panelAkunUser.setLayout(panelAkunUserLayout);
        panelAkunUserLayout.setHorizontalGroup(
            panelAkunUserLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelAkunUserLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(panelAkunUserLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane2)
                    .addComponent(panelAkun, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        panelAkunUserLayout.setVerticalGroup(
            panelAkunUserLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelAkunUserLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(panelAkun, javax.swing.GroupLayout.PREFERRED_SIZE, 117, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 414, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(7, 7, 7))
        );

        javax.swing.GroupLayout PanelCenterLayout = new javax.swing.GroupLayout(PanelCenter);
        PanelCenter.setLayout(PanelCenterLayout);
        PanelCenterLayout.setHorizontalGroup(
            PanelCenterLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, PanelCenterLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(panelInputBarang, javax.swing.GroupLayout.DEFAULT_SIZE, 747, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(panelAkunUser, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
        PanelCenterLayout.setVerticalGroup(
            PanelCenterLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(PanelCenterLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(PanelCenterLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(panelAkunUser, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(panelInputBarang, javax.swing.GroupLayout.DEFAULT_SIZE, 602, Short.MAX_VALUE)))
        );

        PanelLink.setcolorEnd(new java.awt.Color(39, 60, 117));
        PanelLink.setcolorStart(new java.awt.Color(72, 126, 176));
        PanelLink.setTopLeftRound(false);
        PanelLink.setTopRightRound(false);

        lblLinkSosmed.setFont(new java.awt.Font("Times New Roman", 1, 12)); // NOI18N
        lblLinkSosmed.setForeground(new java.awt.Color(204, 204, 204));
        lblLinkSosmed.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblLinkSosmed.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icon/fb.png"))); // NOI18N
        lblLinkSosmed.setText("sanFk POS");
        lblLinkSosmed.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        lblLinkSosmed.setIconTextGap(10);
        lblLinkSosmed.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                lblLinkSosmedMouseClicked(evt);
            }
        });
        PanelLink.add(lblLinkSosmed);

        javax.swing.GroupLayout PanelUtamaLayout = new javax.swing.GroupLayout(PanelUtama);
        PanelUtama.setLayout(PanelUtamaLayout);
        PanelUtamaLayout.setHorizontalGroup(
            PanelUtamaLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(PanelLink, javax.swing.GroupLayout.DEFAULT_SIZE, 1254, Short.MAX_VALUE)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, PanelUtamaLayout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jLabel7)
                .addGap(14, 14, 14))
            .addGroup(PanelUtamaLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addComponent(PanelCenter, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        PanelUtamaLayout.setVerticalGroup(
            PanelUtamaLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, PanelUtamaLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel7)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 610, Short.MAX_VALUE)
                .addComponent(PanelLink, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addGroup(PanelUtamaLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(PanelUtamaLayout.createSequentialGroup()
                    .addGap(33, 33, 33)
                    .addComponent(PanelCenter, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addContainerGap(38, Short.MAX_VALUE)))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(PanelUtama, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(PanelUtama, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        pack();
        setLocationRelativeTo(null);
    }// </editor-fold>//GEN-END:initComponents

    private void btnSimpanActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSimpanActionPerformed
        String nama = txtNama.getText().trim();
        String hargaStr = txtHarga.getText().trim();
        String stokStr = txtStok.getText().trim();
        Object kategoriObj = cbKategori.getSelectedItem();

        if (nama.isEmpty() || hargaStr.isEmpty() || stokStr.isEmpty() || kategoriObj == null) {
            JOptionPane.showMessageDialog(this, "Semua field wajib diisi!");
            return;
        }

        try (Connection conn = getConnection()) {

            int harga = parseInteger(hargaStr, "Harga");
            int stok = parseInteger(stokStr, "Stok");
            validateNonNegative(harga, "Harga");
            validateNonNegative(stok, "Stok");
            String kategori = kategoriObj.toString();

            // CEK DUPLIKAT
            String cekSql = "SELECT 1 FROM barang WHERE nama_barang = ?";
            try (PreparedStatement cek = conn.prepareStatement(cekSql)) {
                cek.setString(1, nama);
                if (cek.executeQuery().next()) {
                    JOptionPane.showMessageDialog(this, "Barang sudah ada!");
                    return;
                }
            }

            // INSERT
            String sql = "INSERT INTO barang (nama_barang, kategori, harga, stok) VALUES (?, ?, ?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, nama);
                ps.setString(2, kategori);
                ps.setInt(3, harga);
                ps.setInt(4, stok);
                ps.executeUpdate();
            }

            JOptionPane.showMessageDialog(this, "Data berhasil disimpan!");
            cariData();
            resetForm();

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, e.getMessage());
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
            JOptionPane.showMessageDialog(this, "Pilih data dulu!");
            return;
        }

        try (Connection conn = getConnection()) {
            String nama = txtNama.getText().trim();
            if (nama.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Nama barang wajib diisi!");
                return;
            }

            Object kategoriObj = cbKategori.getSelectedItem();
            if (kategoriObj == null) {
                JOptionPane.showMessageDialog(this, "Kategori wajib dipilih!");
                return;
            }

            int harga = parseInteger(txtHarga.getText(), "Harga");
            int stok = parseInteger(txtStok.getText(), "Stok");
            validateNonNegative(harga, "Harga");
            validateNonNegative(stok, "Stok");
            String kategori = kategoriObj.toString();

            String sql = "UPDATE barang SET nama_barang=?, kategori=?, harga=?, stok=? WHERE id_barang=?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, nama);
                ps.setString(2, kategori);
                ps.setInt(3, harga);
                ps.setInt(4, stok);
                ps.setString(5, idBarangTerpilih);
                ps.executeUpdate();
            }

            JOptionPane.showMessageDialog(this, "Data diupdate!");
            cariData();
            resetForm();

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, e.getMessage());
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

    private void lblMenuKasirActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_lblMenuKasirActionPerformed
// Cek apakah form kasir belum pernah diklik ATAU sudah ditutup (silang) sebelumnya
        if (formKasirAktif == null || !formKasirAktif.isVisible()) {
            // BUKA JENDELA BARU
            formKasirAktif = new KasirFrame();
            formKasirAktif.setVisible(true);

            // (OPSIONAL) Jika Anda ingin MainFrame tertutup saat Kasir terbuka, hapus garis miring di bawah ini:
            this.dispose(); 
        } else {
            // JIKA SUDAH TERBUKA: Jangan buka baru, cukup tarik jendela yang lama ke depan layar
            formKasirAktif.toFront();
            formKasirAktif.requestFocus();

            // Beri tahu pengguna bahwa jendelanya sudah ada
            JOptionPane.showMessageDialog(this, "Menu Kasir sudah terbuka!", "Informasi", JOptionPane.INFORMATION_MESSAGE);
        }
    }//GEN-LAST:event_lblMenuKasirActionPerformed






    private void btnSimpanUserActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSimpanUserActionPerformed
        String username = txtUsernameUser.getText().trim().toLowerCase();
        String password = new String(txtPasswordUser.getPassword());
        String role = cbRoleUser.getSelectedItem().toString();

        if (username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Username & Password wajib!");
            return;
        }
        if (username.contains(" ")) {
            JOptionPane.showMessageDialog(this, "Username tidak boleh memakai spasi!");
            return;
        }

        try (Connection conn = getConnection()) {
            String sql = "INSERT INTO users (username, password, role) VALUES (?, ?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, username);
                ps.setString(2, PasswordUtil.hashPassword(password));
                ps.setString(3, role);
                ps.executeUpdate();
            }

            JOptionPane.showMessageDialog(this, "User berhasil dibuat!");
            txtUsernameUser.setText("");
            txtPasswordUser.setText("");
            loadDataUser();

        } catch (Exception e) {
            if (e.getMessage() != null && e.getMessage().contains("UNIQUE constraint failed")) {
                JOptionPane.showMessageDialog(this, "Username '" + username + "' sudah dipakai!");
            } else {
                JOptionPane.showMessageDialog(this, e.getMessage());
            }
        }
    }//GEN-LAST:event_btnSimpanUserActionPerformed

    private void btnHapusUserActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnHapusUserActionPerformed
        int barisTerpilih = tblUser.getSelectedRow();

        if (barisTerpilih == -1) {
            JOptionPane.showMessageDialog(this, "Pilih akun di tabel yang ingin dihapus terlebih dahulu!", "Peringatan", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Ambil ID dan Username dari baris yang diklik
        String idUser = tblUser.getValueAt(barisTerpilih, 0).toString();
        String username = tblUser.getValueAt(barisTerpilih, 1).toString();

        // Keamanan: Cegah Admin menghapus akun yang sedang ia pakai sendiri!
        if (username.equals(LoginFrame.kasirAktif)) {
            JOptionPane.showMessageDialog(this, "Anda tidak bisa menghapus akun Anda sendiri yang sedang aktif digunakan!", "Akses Ditolak", JOptionPane.ERROR_MESSAGE);
            return;
        }

        int konfirmasi = JOptionPane.showConfirmDialog(this, "Yakin ingin menghapus akses akun '" + username + "' secara permanen?", "Konfirmasi Hapus", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);

        if (konfirmasi == JOptionPane.YES_OPTION) {
            String url = "jdbc:sqlite:pos_db.db";
            String sql = "DELETE FROM users WHERE id_user = ?";

            try (Connection conn = DriverManager.getConnection(url); PreparedStatement pstmt = conn.prepareStatement(sql)) {

                pstmt.setInt(1, Integer.parseInt(idUser));
                pstmt.executeUpdate();

                JOptionPane.showMessageDialog(this, "Akun berhasil dihapus!");
                loadDataUser(); // Refresh tabel setelah dihapus

            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Error Hapus User: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }//GEN-LAST:event_btnHapusUserActionPerformed

    private void lblLinkSosmedMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_lblLinkSosmedMouseClicked
        // Gunakan fungsi Desktop bawaan Java untuk memanggil Browser komputer
        try {
            // Ganti URL di bawah dengan link sosmed/WhatsApp toko Anda
            String urlSosmed = "https://facebook.com/kudilmonster";

            java.awt.Desktop.getDesktop().browse(new java.net.URI(urlSosmed));

        } catch (Exception e) {
            // Jika komputer tidak memiliki browser default atau terjadi error
            javax.swing.JOptionPane.showMessageDialog(this,
                    "Gagal membuka tautan. Pastikan komputer Anda terhubung ke internet!\nError: " + e.getMessage(),
                    "Error Buka Link",
                    javax.swing.JOptionPane.ERROR_MESSAGE);
        }
    }//GEN-LAST:event_lblLinkSosmedMouseClicked

    private void btnLaporanActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnLaporanActionPerformed
        openWorkspace("Laporan");
    }//GEN-LAST:event_btnLaporanActionPerformed

    private void jLabel7MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jLabel7MouseClicked
       System.exit(0);
    }//GEN-LAST:event_jLabel7MouseClicked

    private void openWorkspace(String tabName) {
        if (formWorkspaceAktif == null || !formWorkspaceAktif.isVisible()) {
            formWorkspaceAktif = new AdminWorkspaceFrame();
            formWorkspaceAktif.setVisible(true);
        }
        formWorkspaceAktif.openTab(tabName);
        formWorkspaceAktif.toFront();
        formWorkspaceAktif.requestFocus();
    }

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
    private toko.aplikasipos.CustomRoundedPanel CariBarang;
    private toko.aplikasipos.CustomRoundedPanel PanelCenter;
    private toko.aplikasipos.CustomRoundedPanel PanelLink;
    private toko.aplikasipos.CustomRoundedPanel PanelUtama;
    private javax.swing.JButton btnEdit;
    private javax.swing.JButton btnHapus;
    private javax.swing.JButton btnHapusKategori;
    private javax.swing.JButton btnHapusUser;
    private javax.swing.JButton btnLaporan;
    private javax.swing.JButton btnSimpan;
    private javax.swing.JButton btnSimpanUser;
    private javax.swing.JButton btnTambahKategori;
    private javax.swing.JComboBox<String> cbFilterKategori;
    private javax.swing.JComboBox<String> cbKategori;
    private javax.swing.JComboBox<String> cbRoleUser;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JTable jTable1;
    private javax.swing.JLabel lblLinkSosmed;
    private javax.swing.JButton lblMenuKasir;
    private javax.swing.JLabel lblPassword;
    private javax.swing.JLabel lblUsername;
    private toko.aplikasipos.CustomRoundedPanel panelAkun;
    private toko.aplikasipos.CustomRoundedPanel panelAkunUser;
    private toko.aplikasipos.CustomRoundedPanel panelInputBarang;
    private toko.aplikasipos.CustomRoundedPanel panelSEH;
    private javax.swing.JTable tblUser;
    private javax.swing.JTextField txtCari;
    private javax.swing.JTextField txtHarga;
    private javax.swing.JTextField txtNama;
    private javax.swing.JPasswordField txtPasswordUser;
    private javax.swing.JTextField txtStok;
    private javax.swing.JTextField txtUsernameUser;
    // End of variables declaration//GEN-END:variables
}
