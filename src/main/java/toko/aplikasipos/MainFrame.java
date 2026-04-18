package toko.aplikasipos;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import javax.swing.table.DefaultTableModel;
import java.sql.PreparedStatement;
import javax.swing.JOptionPane;

public class MainFrame extends javax.swing.JFrame {

    private KasirFrame formKasirAktif = null;
    private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(MainFrame.class.getName());
    private String idBarangTerpilih = "";

    public MainFrame() {
        initComponents();
        initDatabase();    // Membuat tabel
        loadDataUser();
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

        customRoundedPanel2 = new toko.aplikasipos.CustomRoundedPanel();
        PanelBawah = new toko.aplikasipos.CustomRoundedPanel();
        panelInput = new toko.aplikasipos.CustomRoundedPanel();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        txtNama = new javax.swing.JTextField();
        txtHarga = new javax.swing.JTextField();
        txtStok = new javax.swing.JTextField();
        panelCari = new toko.aplikasipos.CustomRoundedPanel();
        jLabel4 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        txtCari = new javax.swing.JTextField();
        cbFilterKategori = new javax.swing.JComboBox<>();
        panelTabel = new toko.aplikasipos.CustomRoundedPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTable1 = new javax.swing.JTable();
        panelSEH = new toko.aplikasipos.CustomRoundedPanel();
        panelKategori = new toko.aplikasipos.CustomRoundedPanel();
        btnHapusKategori = new javax.swing.JButton();
        cbKategori = new javax.swing.JComboBox<>();
        btnTambahKategori = new javax.swing.JButton();
        btnSimpan = new javax.swing.JButton();
        btnEdit = new javax.swing.JButton();
        btnHapus = new javax.swing.JButton();
        panelAkunUser = new toko.aplikasipos.CustomRoundedPanel();
        jScrollPane2 = new javax.swing.JScrollPane();
        tblUser = new javax.swing.JTable();
        panelAkun = new toko.aplikasipos.CustomRoundedPanel();
        lblUsername = new javax.swing.JLabel();
        txtUsernameUser = new javax.swing.JTextField();
        cbRoleUser = new javax.swing.JComboBox<>();
        lblMenuKasir = new javax.swing.JButton();
        lblPassword = new javax.swing.JLabel();
        txtPasswordUser = new javax.swing.JPasswordField();
        btnSimpanUser = new javax.swing.JButton();
        btnHapusUser = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setResizable(false);

        customRoundedPanel2.setBottomLeftRound(false);
        customRoundedPanel2.setBottomRightRound(false);
        customRoundedPanel2.setcolorEnd(new java.awt.Color(39, 60, 117));
        customRoundedPanel2.setcolorStart(new java.awt.Color(72, 126, 176));

        PanelBawah.setBottomLeftRound(false);
        PanelBawah.setBottomRightRound(false);
        PanelBawah.setcolorEnd(new java.awt.Color(72, 126, 176));
        PanelBawah.setcolorStart(new java.awt.Color(72, 126, 176));
        PanelBawah.setTopLeftRound(false);
        PanelBawah.setTopRightRound(false);

        panelInput.setBottomLeftRound(false);
        panelInput.setBottomRightRound(false);
        panelInput.setcolorEnd(new java.awt.Color(39, 60, 117));
        panelInput.setcolorStart(new java.awt.Color(39, 60, 117));
        panelInput.setTopLeftRound(false);
        panelInput.setTopRightRound(false);
        panelInput.setLayout(new java.awt.GridLayout(2, 3, 10, 2));

        jLabel1.setFont(new java.awt.Font("Segoe UI Semibold", 0, 14)); // NOI18N
        jLabel1.setForeground(new java.awt.Color(255, 255, 255));
        jLabel1.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel1.setText("Nama Barang");
        jLabel1.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jLabel1.setPreferredSize(new java.awt.Dimension(50, 30));
        panelInput.add(jLabel1);

        jLabel2.setFont(new java.awt.Font("Segoe UI Semibold", 0, 14)); // NOI18N
        jLabel2.setForeground(new java.awt.Color(255, 255, 255));
        jLabel2.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel2.setText("Harga");
        jLabel2.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        panelInput.add(jLabel2);

        jLabel3.setFont(new java.awt.Font("Segoe UI Semibold", 0, 14)); // NOI18N
        jLabel3.setForeground(new java.awt.Color(255, 255, 255));
        jLabel3.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel3.setText("Stok");
        jLabel3.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        panelInput.add(jLabel3);
        panelInput.add(txtNama);
        panelInput.add(txtHarga);
        panelInput.add(txtStok);

        panelCari.setBottomLeftRound(false);
        panelCari.setBottomRightRound(false);
        panelCari.setcolorEnd(new java.awt.Color(39, 60, 117));
        panelCari.setcolorStart(new java.awt.Color(39, 60, 117));
        panelCari.setTopLeftRound(false);
        panelCari.setTopRightRound(false);
        panelCari.setLayout(new java.awt.GridLayout(2, 2, 5, 5));

        jLabel4.setFont(new java.awt.Font("Segoe UI Semibold", 0, 14)); // NOI18N
        jLabel4.setForeground(new java.awt.Color(255, 255, 255));
        jLabel4.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel4.setText("Cari Barang");
        jLabel4.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        panelCari.add(jLabel4);

        jLabel5.setFont(new java.awt.Font("Segoe UI Semibold", 0, 14)); // NOI18N
        jLabel5.setForeground(new java.awt.Color(255, 255, 255));
        jLabel5.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel5.setText("Filter Kategori");
        jLabel5.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        panelCari.add(jLabel5);

        txtCari.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                txtCariKeyReleased(evt);
            }
        });
        panelCari.add(txtCari);

        cbFilterKategori.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        cbFilterKategori.addActionListener(this::cbFilterKategoriActionPerformed);
        panelCari.add(cbFilterKategori);

        panelTabel.setBottomLeftRound(false);
        panelTabel.setBottomRightRound(false);
        panelTabel.setcolorEnd(new java.awt.Color(39, 60, 117));
        panelTabel.setcolorStart(new java.awt.Color(39, 60, 117));
        panelTabel.setMinimumSize(new java.awt.Dimension(300, 200));
        panelTabel.setPreferredSize(new java.awt.Dimension(300, 200));
        panelTabel.setTopLeftRound(false);
        panelTabel.setTopRightRound(false);

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

        javax.swing.GroupLayout panelTabelLayout = new javax.swing.GroupLayout(panelTabel);
        panelTabel.setLayout(panelTabelLayout);
        panelTabelLayout.setHorizontalGroup(
            panelTabelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelTabelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 574, Short.MAX_VALUE)
                .addContainerGap())
        );
        panelTabelLayout.setVerticalGroup(
            panelTabelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelTabelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                .addContainerGap())
        );

        panelSEH.setBottomLeftRound(false);
        panelSEH.setBottomRightRound(false);
        panelSEH.setcolorEnd(new java.awt.Color(39, 60, 117));
        panelSEH.setcolorStart(new java.awt.Color(39, 60, 117));
        panelSEH.setTopLeftRound(false);
        panelSEH.setTopRightRound(false);
        panelSEH.setLayout(new java.awt.GridLayout(1, 0, 10, 5));

        panelKategori.setBottomLeftRound(false);
        panelKategori.setBottomRightRound(false);
        panelKategori.setcolorEnd(new java.awt.Color(39, 60, 117));
        panelKategori.setcolorStart(new java.awt.Color(39, 60, 117));
        panelKategori.setPreferredSize(new java.awt.Dimension(170, 33));
        panelKategori.setTopLeftRound(false);
        panelKategori.setTopRightRound(false);

        btnHapusKategori.setFont(new java.awt.Font("Segoe UI Semibold", 0, 18)); // NOI18N
        btnHapusKategori.setText("-");
        btnHapusKategori.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        btnHapusKategori.addActionListener(this::btnHapusKategoriActionPerformed);

        cbKategori.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Makanan", "Minuman", "Sembako" }));
        cbKategori.setPreferredSize(new java.awt.Dimension(85, 22));

        btnTambahKategori.setFont(new java.awt.Font("Segoe UI Semibold", 0, 18)); // NOI18N
        btnTambahKategori.setText("+");
        btnTambahKategori.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        btnTambahKategori.addActionListener(this::btnTambahKategoriActionPerformed);

        javax.swing.GroupLayout panelKategoriLayout = new javax.swing.GroupLayout(panelKategori);
        panelKategori.setLayout(panelKategoriLayout);
        panelKategoriLayout.setHorizontalGroup(
            panelKategoriLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelKategoriLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(btnHapusKategori, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(cbKategori, 0, 161, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(btnTambahKategori, javax.swing.GroupLayout.PREFERRED_SIZE, 43, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
        panelKategoriLayout.setVerticalGroup(
            panelKategoriLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelKategoriLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(panelKategoriLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(panelKategoriLayout.createSequentialGroup()
                        .addComponent(btnTambahKategori)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addComponent(cbKategori, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(btnHapusKategori, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );

        panelSEH.add(panelKategori);

        btnSimpan.setBackground(new java.awt.Color(0, 168, 255));
        btnSimpan.setText("Simpan");
        btnSimpan.addActionListener(this::btnSimpanActionPerformed);
        panelSEH.add(btnSimpan);

        btnEdit.setBackground(new java.awt.Color(251, 197, 49));
        btnEdit.setText("Edit");
        btnEdit.addActionListener(this::btnEditActionPerformed);
        panelSEH.add(btnEdit);

        btnHapus.setBackground(new java.awt.Color(232, 65, 24));
        btnHapus.setText("Hapus");
        btnHapus.addActionListener(this::btnHapusActionPerformed);
        panelSEH.add(btnHapus);

        panelAkunUser.setBottomLeftRound(false);
        panelAkunUser.setBottomRightRound(false);
        panelAkunUser.setcolorEnd(new java.awt.Color(39, 60, 117));
        panelAkunUser.setcolorStart(new java.awt.Color(39, 60, 117));
        panelAkunUser.setTopLeftRound(false);
        panelAkunUser.setTopRightRound(false);
        panelAkunUser.setLayout(new java.awt.BorderLayout(0, 5));

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

        panelAkunUser.add(jScrollPane2, java.awt.BorderLayout.CENTER);

        panelAkun.setBottomLeftRound(false);
        panelAkun.setBottomRightRound(false);
        panelAkun.setcolorEnd(new java.awt.Color(39, 60, 117));
        panelAkun.setcolorStart(new java.awt.Color(39, 60, 117));
        panelAkun.setTopLeftRound(false);
        panelAkun.setTopRightRound(false);
        panelAkun.setLayout(new java.awt.GridLayout(2, 0, 5, 10));

        lblUsername.setFont(new java.awt.Font("Segoe UI Semibold", 0, 14)); // NOI18N
        lblUsername.setForeground(new java.awt.Color(255, 255, 255));
        lblUsername.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblUsername.setText("Username");
        lblUsername.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        panelAkun.add(lblUsername);
        panelAkun.add(txtUsernameUser);

        cbRoleUser.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Admin", "Kasir" }));
        panelAkun.add(cbRoleUser);

        lblMenuKasir.setBackground(new java.awt.Color(68, 189, 50));
        lblMenuKasir.setText("Buka Menu Kasir");
        lblMenuKasir.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        lblMenuKasir.addActionListener(this::lblMenuKasirActionPerformed);
        panelAkun.add(lblMenuKasir);

        lblPassword.setFont(new java.awt.Font("Segoe UI Semibold", 0, 14)); // NOI18N
        lblPassword.setForeground(new java.awt.Color(255, 255, 255));
        lblPassword.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblPassword.setText("Password");
        lblPassword.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        panelAkun.add(lblPassword);
        panelAkun.add(txtPasswordUser);

        btnSimpanUser.setBackground(new java.awt.Color(0, 168, 255));
        btnSimpanUser.setText("Simpan Akun");
        btnSimpanUser.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        btnSimpanUser.addActionListener(this::btnSimpanUserActionPerformed);
        panelAkun.add(btnSimpanUser);

        btnHapusUser.setBackground(new java.awt.Color(232, 65, 24));
        btnHapusUser.setText("Hapus User");
        btnHapusUser.addActionListener(this::btnHapusUserActionPerformed);
        panelAkun.add(btnHapusUser);

        panelAkunUser.add(panelAkun, java.awt.BorderLayout.NORTH);

        javax.swing.GroupLayout PanelBawahLayout = new javax.swing.GroupLayout(PanelBawah);
        PanelBawah.setLayout(PanelBawahLayout);
        PanelBawahLayout.setHorizontalGroup(
            PanelBawahLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, PanelBawahLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(PanelBawahLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(panelInput, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(panelCari, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, PanelBawahLayout.createSequentialGroup()
                        .addComponent(panelTabel, javax.swing.GroupLayout.PREFERRED_SIZE, 586, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(panelAkunUser, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addComponent(panelSEH, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        PanelBawahLayout.setVerticalGroup(
            PanelBawahLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, PanelBawahLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(panelInput, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(panelSEH, javax.swing.GroupLayout.PREFERRED_SIZE, 43, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(panelCari, javax.swing.GroupLayout.PREFERRED_SIZE, 61, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(PanelBawahLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(panelTabel, javax.swing.GroupLayout.DEFAULT_SIZE, 303, Short.MAX_VALUE)
                    .addComponent(panelAkunUser, javax.swing.GroupLayout.DEFAULT_SIZE, 303, Short.MAX_VALUE))
                .addContainerGap())
        );

        javax.swing.GroupLayout customRoundedPanel2Layout = new javax.swing.GroupLayout(customRoundedPanel2);
        customRoundedPanel2.setLayout(customRoundedPanel2Layout);
        customRoundedPanel2Layout.setHorizontalGroup(
            customRoundedPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 1097, Short.MAX_VALUE)
            .addGroup(customRoundedPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addComponent(PanelBawah, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        customRoundedPanel2Layout.setVerticalGroup(
            customRoundedPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 605, Short.MAX_VALUE)
            .addGroup(customRoundedPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(customRoundedPanel2Layout.createSequentialGroup()
                    .addGap(65, 65, 65)
                    .addComponent(PanelBawah, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addContainerGap(41, Short.MAX_VALUE)))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(customRoundedPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(customRoundedPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
        );

        pack();
        setLocationRelativeTo(null);
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

    private void lblMenuKasirActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_lblMenuKasirActionPerformed
// Cek apakah form kasir belum pernah diklik ATAU sudah ditutup (silang) sebelumnya
        if (formKasirAktif == null || !formKasirAktif.isVisible()) {
            // BUKA JENDELA BARU
            formKasirAktif = new KasirFrame();
            formKasirAktif.setVisible(true);

            // (OPSIONAL) Jika Anda ingin MainFrame tertutup saat Kasir terbuka, hapus garis miring di bawah ini:
            // this.dispose(); 
        } else {
            // JIKA SUDAH TERBUKA: Jangan buka baru, cukup tarik jendela yang lama ke depan layar
            formKasirAktif.toFront();
            formKasirAktif.requestFocus();

            // Beri tahu pengguna bahwa jendelanya sudah ada
            JOptionPane.showMessageDialog(this, "Menu Kasir sudah terbuka!", "Informasi", JOptionPane.INFORMATION_MESSAGE);
        }
    }//GEN-LAST:event_lblMenuKasirActionPerformed

    private void btnSimpanUserActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSimpanUserActionPerformed
        String username = txtUsernameUser.getText().trim().toLowerCase(); // Otomatis huruf kecil semua
        String password = new String(txtPasswordUser.getPassword());
        String role = cbRoleUser.getSelectedItem().toString();

        // 1. Validasi input
        if (username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Username dan Password wajib diisi!", "Peringatan", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (username.contains(" ")) {
            JOptionPane.showMessageDialog(this, "Username tidak boleh memakai spasi!", "Peringatan", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String url = "jdbc:sqlite:pos_db.db";
        String sql = "INSERT INTO users (username, password, role) VALUES (?, ?, ?)";

        try (Connection conn = DriverManager.getConnection(url); PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);
            pstmt.setString(2, password);
            pstmt.setString(3, role);
            pstmt.executeUpdate();

            JOptionPane.showMessageDialog(this, "Akun " + role + " dengan username '" + username + "' berhasil dibuat!");

            // Bersihkan kotak isian dan refresh tabel
            txtUsernameUser.setText("");
            txtPasswordUser.setText("");
            txtUsernameUser.requestFocus();
            loadDataUser(); // Panggil fungsi load untuk update tabel di layar

        } catch (Exception e) {
            // Tangani error jika username sudah dipakai
            if (e.getMessage().contains("UNIQUE constraint failed")) {
                JOptionPane.showMessageDialog(this, "Username '" + username + "' sudah dipakai!\nSilakan gunakan nama lain.", "Gagal", JOptionPane.ERROR_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this, "Error Simpan User: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
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
    private toko.aplikasipos.CustomRoundedPanel PanelBawah;
    private javax.swing.JButton btnEdit;
    private javax.swing.JButton btnHapus;
    private javax.swing.JButton btnHapusKategori;
    private javax.swing.JButton btnHapusUser;
    private javax.swing.JButton btnSimpan;
    private javax.swing.JButton btnSimpanUser;
    private javax.swing.JButton btnTambahKategori;
    private javax.swing.JComboBox<String> cbFilterKategori;
    private javax.swing.JComboBox<String> cbKategori;
    private javax.swing.JComboBox<String> cbRoleUser;
    private toko.aplikasipos.CustomRoundedPanel customRoundedPanel2;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JTable jTable1;
    private javax.swing.JButton lblMenuKasir;
    private javax.swing.JLabel lblPassword;
    private javax.swing.JLabel lblUsername;
    private toko.aplikasipos.CustomRoundedPanel panelAkun;
    private toko.aplikasipos.CustomRoundedPanel panelAkunUser;
    private toko.aplikasipos.CustomRoundedPanel panelCari;
    private toko.aplikasipos.CustomRoundedPanel panelInput;
    private toko.aplikasipos.CustomRoundedPanel panelKategori;
    private toko.aplikasipos.CustomRoundedPanel panelSEH;
    private toko.aplikasipos.CustomRoundedPanel panelTabel;
    private javax.swing.JTable tblUser;
    private javax.swing.JTextField txtCari;
    private javax.swing.JTextField txtHarga;
    private javax.swing.JTextField txtNama;
    private javax.swing.JPasswordField txtPasswordUser;
    private javax.swing.JTextField txtStok;
    private javax.swing.JTextField txtUsernameUser;
    // End of variables declaration//GEN-END:variables
}
