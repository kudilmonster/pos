package toko.aplikasipos;

import com.formdev.flatlaf.themes.FlatMacDarkLaf;
import com.formdev.flatlaf.themes.FlatMacLightLaf;
import java.awt.Color;
import java.awt.Window;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import javax.swing.table.DefaultTableModel;
import java.sql.PreparedStatement;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public class MainFrame extends javax.swing.JFrame {

    private Connection getConnection() throws Exception {
        return DatabaseManager.getConnection();
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
    private final AdminWorkspaceFrame hostWorkspace;

    private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(MainFrame.class.getName());
    private String idBarangTerpilih = "";

    public MainFrame() {
        this(null);
    }

    public MainFrame(AdminWorkspaceFrame hostWorkspace) {
        this.hostWorkspace = hostWorkspace;
        initComponents();
        syncThemeToggleState();
        //setExtendedState(JFrame.MAXIMIZED_BOTH);
        initDatabase();    // Membuat tabel
        applyRolePermissions();
        loadDataUser();
        loadKategori();    // <- TAMBAHKAN BARIS INI: Mengisi ComboBox
        tampilkanData();   // Menampilkan data di tabel
        initIcons();
        requestBarcodeFocus();
        //setBackground(new Color(0, 0, 0, 0));

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

    }

    private void initDatabase() {
        try {
            DatabaseManager.initializeDatabase();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error DB: " + e.getMessage());
        }
    }

    private DefaultTableModel createReadOnlyTableModel(String... columns) {
        DefaultTableModel model = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        for (String column : columns) {
            model.addColumn(column);
        }
        return model;
    }

    private DefaultTableModel createBarangTableModel() {
        return createReadOnlyTableModel("ID Barang", "Nama Barang", "Barcode", "Kategori", "Harga", "Stok");
    }

    private Object[] mapBarangRow(ResultSet rs) throws Exception {
        String barcode = rs.getString("barcode");
        return new Object[]{
            rs.getInt("id_barang"),
            rs.getString("nama_barang"),
            barcode != null ? barcode : "",
            rs.getString("kategori"),
            rs.getInt("harga"),
            rs.getInt("stok")
        };
    }

    private void refreshBarangTable(String sql, PreparedStatementBinder binder) {
        DefaultTableModel model = createBarangTableModel();
        jTable1.setModel(model);

        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            if (binder != null) {
                binder.bind(pstmt);
            }

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    model.addRow(mapBarangRow(rs));
                }
            }
        } catch (Exception e) {
            System.out.println("Error refresh data barang: " + e.getMessage());
        }
    }

    @FunctionalInterface
    private interface PreparedStatementBinder {

        void bind(PreparedStatement pstmt) throws Exception;
    }

    private void tampilkanData() {
        refreshBarangTable("SELECT * FROM barang", null);
    }

    private void loadDataUser() {
        DefaultTableModel model = createReadOnlyTableModel("ID User", "Username", "Role");

        tblUser.setModel(model);
        try (Connection conn = getConnection(); Statement st = conn.createStatement(); ResultSet rs = st.executeQuery("SELECT id_user, username, role FROM users")) {

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
        String keyword = txtCari.getText();
        String kategoriFilter = "Semua Kategori";
        if (cbFilterKategori.getSelectedItem() != null) {
            kategoriFilter = cbFilterKategori.getSelectedItem().toString();
        }
        final String kategoriFilterFinal = kategoriFilter;
        final boolean hasKategoriFilter = !kategoriFilterFinal.equals("Semua Kategori");

        String sql = "SELECT * FROM barang WHERE (nama_barang LIKE ? OR COALESCE(barcode,'') = ? OR COALESCE(barcode,'') LIKE ? OR CAST(id_barang AS TEXT) = ?)";
        if (hasKategoriFilter) {
            sql += " AND kategori = ?";
        }

        refreshBarangTable(sql, pstmt -> {
            String keywordLike = "%" + keyword + "%";
            pstmt.setString(1, keywordLike);
            pstmt.setString(2, keyword);
            pstmt.setString(3, keywordLike);
            pstmt.setString(4, keyword);
            if (hasKategoriFilter) {
                pstmt.setString(5, kategoriFilterFinal);
            }
        });
    }

    private void loadKategori() {
        cbKategori.removeAllItems();
        cbFilterKategori.removeAllItems();
        cbFilterKategori.addItem("Semua Kategori");

        try (Connection conn = getConnection(); Statement st = conn.createStatement(); ResultSet rs = st.executeQuery("SELECT nama_kategori FROM kategori ORDER BY nama_kategori ASC")) {

            while (rs.next()) {
                String namaKategori = rs.getString("nama_kategori");
                cbKategori.addItem(namaKategori);
                cbFilterKategori.addItem(namaKategori);
            }

        } catch (Exception e) {
            System.out.println("Error Load Kategori: " + e.getMessage());
        }
    }

    private void resetForm() {
        txtNama.setText("");
        txtHarga.setText("");
        txtStok.setText("");
        txtBarcodeBarang.setText(""); // KOSONGKAN BARCODE
        cbKategori.setSelectedIndex(0);
        idBarangTerpilih = "";
        requestBarcodeFocus();
    }

    private boolean isDarkThemeActive() {
        javax.swing.LookAndFeel laf = UIManager.getLookAndFeel();
        if (laf == null) {
            return false;
        }
        String lafName = laf.getClass().getName().toLowerCase();
        return lafName.contains("dark");
    }

    private void updateThemeToggleLabel(boolean darkMode) {
        btnToggleTema.setText(darkMode ? "Dark" : "Light");
        btnToggleTema.setToolTipText(darkMode ? "Ganti ke FlatLaf macOS Light" : "Ganti ke FlatLaf macOS Dark");
    }

    private void syncThemeToggleState() {
        boolean darkMode = isDarkThemeActive();
        btnToggleTema.setSelected(darkMode);
        updateThemeToggleLabel(darkMode);
    }

    private void applyTheme(boolean darkMode) throws Exception {
        if (darkMode) {
            UIManager.setLookAndFeel(new FlatMacDarkLaf());
        } else {
            UIManager.setLookAndFeel(new FlatMacLightLaf());
        }

        for (Window window : Window.getWindows()) {
            SwingUtilities.updateComponentTreeUI(window);
            window.invalidate();
            window.validate();
            window.repaint();
        }
    }

    private void initIcons() {
        AppUtil.setLabelIcon(lblLinkSosmed, "icon/fb.png");
        AppUtil.setLabelIcon(cari, "icon/cari.png");
        AppUtil.setLabelIcon(filter, "icon/filter.png");
        AppUtil.setWindowIcon(this);
        AppUtil.setButtonIcon(btnHapusKategori, "/icon/minus.png");
        AppUtil.setButtonIcon(btnTambahKategori, "/icon/add.png");
        AppUtil.setButtonIcon(btnSimpan, "/icon/add_shopping.png");
        AppUtil.setButtonIcon(btnEdit, "/icon/edit.png");
        AppUtil.setButtonIcon(btnHapus, "/icon/delete.png");
        AppUtil.setButtonIcon(btnSimpanUser, "/icon/person_add.png");
        AppUtil.setButtonIcon(lblMenuKasir, "/icon/monitoring.png");
        AppUtil.setButtonIcon(btnHapusUser, "/icon/person_cancel.png");
    }

    private void requestBarcodeFocus() {
        SwingUtilities.invokeLater(() -> txtBarcodeBarang.requestFocusInWindow());
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        PanelUtama = new toko.aplikasipos.CustomRoundedPanel();
        PanelCenter = new toko.aplikasipos.CustomRoundedPanel();
        panelInputBarang = new toko.aplikasipos.CustomRoundedPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTable1 = new javax.swing.JTable();
        panelSEH = new toko.aplikasipos.CustomRoundedPanel();
        btnHapusKategori = new javax.swing.JButton();
        cbKategori = new javax.swing.JComboBox<>();
        btnTambahKategori = new javax.swing.JButton();
        btnHapus = new javax.swing.JButton();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        btnEdit = new javax.swing.JButton();
        txtNama = new javax.swing.JTextField();
        txtHarga = new javax.swing.JTextField();
        txtStok = new javax.swing.JTextField();
        btnSimpan = new javax.swing.JButton();
        txtBarcodeBarang = new javax.swing.JTextField();
        CariBarang = new toko.aplikasipos.CustomRoundedPanel();
        txtCari = new javax.swing.JTextField();
        cari = new javax.swing.JLabel();
        filter = new javax.swing.JLabel();
        cbFilterKategori = new javax.swing.JComboBox<>();
        panelAkunUser = new toko.aplikasipos.CustomRoundedPanel();
        jScrollPane2 = new javax.swing.JScrollPane();
        tblUser = new javax.swing.JTable();
        panelAkun = new toko.aplikasipos.CustomRoundedPanel();
        lblUsername = new javax.swing.JLabel();
        lblPassword = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        txtUsernameUser = new javax.swing.JTextField();
        txtPasswordUser = new javax.swing.JPasswordField();
        cbRoleUser = new javax.swing.JComboBox<>();
        btnSimpanUser = new javax.swing.JButton();
        btnHapusUser = new javax.swing.JButton();
        lblMenuKasir = new javax.swing.JButton();
        PanelLink = new toko.aplikasipos.CustomRoundedPanel();
        btnToggleTema = new javax.swing.JToggleButton();
        lblLinkSosmed = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        PanelUtama.setBottomLeftRound(false);
        PanelUtama.setBottomRightRound(false);
        PanelUtama.setcolorEnd(new java.awt.Color(72, 126, 176));
        PanelUtama.setcolorStart(new java.awt.Color(72, 126, 176));
        PanelUtama.setTopLeftRound(false);
        PanelUtama.setTopRightRound(false);
        PanelUtama.setLayout(new java.awt.BorderLayout());

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
        panelInputBarang.setMinimumSize(new java.awt.Dimension(557, 500));
        panelInputBarang.setName(""); // NOI18N
        panelInputBarang.setPreferredSize(new java.awt.Dimension(557, 500));
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
        jTable1.setShowGrid(true);
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
        btnHapusKategori.setText("Hapus Kategori");
        btnHapusKategori.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        btnHapusKategori.setHorizontalTextPosition(javax.swing.SwingConstants.LEADING);
        btnHapusKategori.addActionListener(this::btnHapusKategoriActionPerformed);
        panelSEH.add(btnHapusKategori);

        cbKategori.setFont(new java.awt.Font("Segoe UI Semibold", 1, 18)); // NOI18N
        cbKategori.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Makanan", "Minuman", "Sembako" }));
        cbKategori.setPreferredSize(new java.awt.Dimension(85, 22));
        panelSEH.add(cbKategori);

        btnTambahKategori.setFont(new java.awt.Font("Segoe UI Semibold", 0, 12)); // NOI18N
        btnTambahKategori.setText("Tambah Kategori");
        btnTambahKategori.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        btnTambahKategori.addActionListener(this::btnTambahKategoriActionPerformed);
        panelSEH.add(btnTambahKategori);

        btnHapus.setBackground(new java.awt.Color(255, 165, 2));
        btnHapus.setFont(new java.awt.Font("Segoe UI Semibold", 0, 18)); // NOI18N
        btnHapus.setText("Hapus");
        btnHapus.addActionListener(this::btnHapusActionPerformed);
        panelSEH.add(btnHapus);

        jLabel1.setFont(new java.awt.Font("Segoe UI Semibold", 0, 14)); // NOI18N
        jLabel1.setForeground(new java.awt.Color(255, 255, 255));
        jLabel1.setText("Nama Barang");
        jLabel1.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jLabel1.setPreferredSize(new java.awt.Dimension(50, 30));
        panelSEH.add(jLabel1);

        jLabel2.setFont(new java.awt.Font("Segoe UI Semibold", 0, 14)); // NOI18N
        jLabel2.setForeground(new java.awt.Color(255, 255, 255));
        jLabel2.setText("Harga");
        jLabel2.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        panelSEH.add(jLabel2);

        jLabel3.setFont(new java.awt.Font("Segoe UI Semibold", 0, 14)); // NOI18N
        jLabel3.setForeground(new java.awt.Color(255, 255, 255));
        jLabel3.setText("Stok");
        jLabel3.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        panelSEH.add(jLabel3);

        btnEdit.setBackground(new java.awt.Color(30, 144, 255));
        btnEdit.setFont(new java.awt.Font("Segoe UI Semibold", 0, 18)); // NOI18N
        btnEdit.setText("Edit");
        btnEdit.addActionListener(this::btnEditActionPerformed);
        panelSEH.add(btnEdit);

        txtNama.setFont(new java.awt.Font("Segoe UI Semibold", 0, 14)); // NOI18N
        panelSEH.add(txtNama);

        txtHarga.setFont(new java.awt.Font("Segoe UI Semibold", 0, 14)); // NOI18N
        panelSEH.add(txtHarga);

        txtStok.setFont(new java.awt.Font("Segoe UI Semibold", 0, 14)); // NOI18N
        panelSEH.add(txtStok);

        btnSimpan.setBackground(new java.awt.Color(46, 213, 115));
        btnSimpan.setFont(new java.awt.Font("Segoe UI Semibold", 0, 18)); // NOI18N
        btnSimpan.setText("Simpan");
        btnSimpan.addActionListener(this::btnSimpanActionPerformed);
        panelSEH.add(btnSimpan);
        panelSEH.add(txtBarcodeBarang);

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

        cari.setFont(new java.awt.Font("Segoe UI Semibold", 0, 14)); // NOI18N
        cari.setForeground(new java.awt.Color(255, 255, 255));
        cari.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        cari.setText("Cari Barang");

        filter.setFont(new java.awt.Font("Segoe UI Semibold", 0, 14)); // NOI18N
        filter.setForeground(new java.awt.Color(255, 255, 255));
        filter.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        filter.setText("Filter");

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
                        .addComponent(cari, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(CariBarangLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addGroup(CariBarangLayout.createSequentialGroup()
                        .addComponent(filter, javax.swing.GroupLayout.DEFAULT_SIZE, 217, Short.MAX_VALUE)
                        .addContainerGap())
                    .addComponent(cbFilterKategori, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
        );
        CariBarangLayout.setVerticalGroup(
            CariBarangLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(CariBarangLayout.createSequentialGroup()
                .addContainerGap(12, Short.MAX_VALUE)
                .addGroup(CariBarangLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(cari, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(filter, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(CariBarangLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(txtCari, javax.swing.GroupLayout.PREFERRED_SIZE, 28, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(cbFilterKategori, javax.swing.GroupLayout.PREFERRED_SIZE, 28, javax.swing.GroupLayout.PREFERRED_SIZE)))
        );

        javax.swing.GroupLayout panelInputBarangLayout = new javax.swing.GroupLayout(panelInputBarang);
        panelInputBarang.setLayout(panelInputBarangLayout);
        panelInputBarangLayout.setHorizontalGroup(
            panelInputBarangLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelInputBarangLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(panelInputBarangLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(CariBarang, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(panelInputBarangLayout.createSequentialGroup()
                        .addGroup(panelInputBarangLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(panelSEH, javax.swing.GroupLayout.DEFAULT_SIZE, 643, Short.MAX_VALUE)
                            .addComponent(jScrollPane1))
                        .addContainerGap())))
        );
        panelInputBarangLayout.setVerticalGroup(
            panelInputBarangLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, panelInputBarangLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(panelSEH, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(CariBarang, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
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
        lblUsername.setText("Username");
        lblUsername.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        panelAkun.add(lblUsername);

        lblPassword.setFont(new java.awt.Font("Segoe UI Semibold", 0, 14)); // NOI18N
        lblPassword.setForeground(new java.awt.Color(255, 255, 255));
        lblPassword.setText("Password");
        lblPassword.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        panelAkun.add(lblPassword);

        jLabel6.setFont(new java.awt.Font("Segoe UI Semibold", 0, 14)); // NOI18N
        jLabel6.setForeground(new java.awt.Color(255, 255, 255));
        jLabel6.setText("Role");
        jLabel6.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        panelAkun.add(jLabel6);

        txtUsernameUser.setFont(new java.awt.Font("Segoe UI Semibold", 0, 18)); // NOI18N
        panelAkun.add(txtUsernameUser);

        txtPasswordUser.setFont(new java.awt.Font("Segoe UI Semibold", 0, 18)); // NOI18N
        panelAkun.add(txtPasswordUser);

        cbRoleUser.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Admin", "Kasir" }));
        panelAkun.add(cbRoleUser);

        btnSimpanUser.setBackground(new java.awt.Color(0, 168, 255));
        btnSimpanUser.setText("Simpan");
        btnSimpanUser.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        btnSimpanUser.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        btnSimpanUser.addActionListener(this::btnSimpanUserActionPerformed);
        panelAkun.add(btnSimpanUser);

        btnHapusUser.setBackground(new java.awt.Color(232, 65, 24));
        btnHapusUser.setText("Hapus User");
        btnHapusUser.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        btnHapusUser.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        btnHapusUser.addActionListener(this::btnHapusUserActionPerformed);
        panelAkun.add(btnHapusUser);

        lblMenuKasir.setBackground(new java.awt.Color(68, 189, 50));
        lblMenuKasir.setText("Kasir");
        lblMenuKasir.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        lblMenuKasir.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        lblMenuKasir.addActionListener(this::lblMenuKasirActionPerformed);
        panelAkun.add(lblMenuKasir);

        javax.swing.GroupLayout panelAkunUserLayout = new javax.swing.GroupLayout(panelAkunUser);
        panelAkunUser.setLayout(panelAkunUserLayout);
        panelAkunUserLayout.setHorizontalGroup(
            panelAkunUserLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelAkunUserLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(panelAkunUserLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(panelAkun, javax.swing.GroupLayout.DEFAULT_SIZE, 296, Short.MAX_VALUE)
                    .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE))
                .addContainerGap())
        );
        panelAkunUserLayout.setVerticalGroup(
            panelAkunUserLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelAkunUserLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(panelAkun, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                .addContainerGap())
        );

        javax.swing.GroupLayout PanelCenterLayout = new javax.swing.GroupLayout(PanelCenter);
        PanelCenter.setLayout(PanelCenterLayout);
        PanelCenterLayout.setHorizontalGroup(
            PanelCenterLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, PanelCenterLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(panelInputBarang, javax.swing.GroupLayout.DEFAULT_SIZE, 665, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(panelAkunUser, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );
        PanelCenterLayout.setVerticalGroup(
            PanelCenterLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, PanelCenterLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(PanelCenterLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(panelInputBarang, javax.swing.GroupLayout.PREFERRED_SIZE, 478, Short.MAX_VALUE)
                    .addComponent(panelAkunUser, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );

        PanelUtama.add(PanelCenter, java.awt.BorderLayout.CENTER);

        PanelLink.setBottomLeftRound(false);
        PanelLink.setBottomRightRound(false);
        PanelLink.setcolorEnd(new java.awt.Color(39, 60, 117));
        PanelLink.setcolorStart(new java.awt.Color(72, 126, 176));
        PanelLink.setPreferredSize(new java.awt.Dimension(1007, 40));
        PanelLink.setTopLeftRound(false);
        PanelLink.setTopRightRound(false);
        PanelLink.setLayout(new java.awt.GridBagLayout());

        btnToggleTema.setBackground(new java.awt.Color(39, 60, 117));
        btnToggleTema.setForeground(new java.awt.Color(204, 204, 204));
        btnToggleTema.setText("Tema");
        btnToggleTema.addActionListener(this::btnToggleTemaActionPerformed);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 0.1;
        gridBagConstraints.insets = new java.awt.Insets(0, 5, 0, 5);
        PanelLink.add(btnToggleTema, gridBagConstraints);

        lblLinkSosmed.setFont(new java.awt.Font("Times New Roman", 1, 12)); // NOI18N
        lblLinkSosmed.setForeground(new java.awt.Color(204, 204, 204));
        lblLinkSosmed.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblLinkSosmed.setText("sanFk POS");
        lblLinkSosmed.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        lblLinkSosmed.setHorizontalTextPosition(javax.swing.SwingConstants.LEADING);
        lblLinkSosmed.setIconTextGap(10);
        lblLinkSosmed.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                lblLinkSosmedMouseClicked(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.insets = new java.awt.Insets(0, 5, 0, 5);
        PanelLink.add(lblLinkSosmed, gridBagConstraints);

        PanelUtama.add(PanelLink, java.awt.BorderLayout.PAGE_END);

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
        String barcode = txtBarcodeBarang.getText().trim(); // AMBIL TEKS BARCODE
        Object kategoriObj = cbKategori.getSelectedItem();

        if (nama.isEmpty() || hargaStr.isEmpty() || stokStr.isEmpty() || kategoriObj == null) {
            JOptionPane.showMessageDialog(this, "Semua field (kecuali barcode) wajib diisi!");
            return;
        }

        try (Connection conn = getConnection()) {

            int harga = parseInteger(hargaStr, "Harga");
            int stok = parseInteger(stokStr, "Stok");
            validateNonNegative(harga, "Harga");
            validateNonNegative(stok, "Stok");
            String kategori = kategoriObj.toString();

            // CEK DUPLIKAT NAMA
            String cekSql = "SELECT 1 FROM barang WHERE nama_barang = ?";
            try (PreparedStatement cek = conn.prepareStatement(cekSql)) {
                cek.setString(1, nama);
                if (cek.executeQuery().next()) {
                    JOptionPane.showMessageDialog(this, "Barang sudah ada!");
                    return;
                }
            }

            // CEK DUPLIKAT BARCODE (Opsional tapi penting agar scanner tidak bingung)
            if (!barcode.isEmpty()) {
                String cekBc = "SELECT 1 FROM barang WHERE barcode = ?";
                try (PreparedStatement cekB = conn.prepareStatement(cekBc)) {
                    cekB.setString(1, barcode);
                    if (cekB.executeQuery().next()) {
                        JOptionPane.showMessageDialog(this, "Barcode ini sudah dipakai oleh barang lain!");
                        return;
                    }
                }
            }

            // INSERT DENGAN BARCODE
            String sql = "INSERT INTO barang (nama_barang, kategori, harga, stok, barcode) VALUES (?, ?, ?, ?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, nama);
                ps.setString(2, kategori);
                ps.setInt(3, harga);
                ps.setInt(4, stok);
                ps.setString(5, barcode); // MASUKKAN BARCODE KE PARAMETER 5
                ps.executeUpdate();
            }

            JOptionPane.showMessageDialog(this, "Data berhasil disimpan!");
            cariData();
            resetForm();

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error Simpan: " + e.getMessage());
        }
    }//GEN-LAST:event_btnSimpanActionPerformed

    private void jTable1MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jTable1MouseClicked
        int baris = jTable1.rowAtPoint(evt.getPoint());

        if (baris > -1) {
            // Indeks bergeser karena ada kolom barcode (indeks ke-2)
            idBarangTerpilih = jTable1.getValueAt(baris, 0).toString();
            String nama = jTable1.getValueAt(baris, 1).toString();
            String barcode = jTable1.getValueAt(baris, 2).toString(); // AMBIL DARI TABEL
            String kategori = jTable1.getValueAt(baris, 3).toString();
            String harga = jTable1.getValueAt(baris, 4).toString();
            String stok = jTable1.getValueAt(baris, 5).toString();

            txtNama.setText(nama);
            txtBarcodeBarang.setText(barcode); // MASUKKAN KE KOTAK TEKS
            cbKategori.setSelectedItem(kategori);
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
            String barcode = txtBarcodeBarang.getText().trim(); // AMBIL BARCODE UNTUK DIEDIT

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

            // UPDATE DENGAN BARCODE
            String sql = "UPDATE barang SET nama_barang=?, kategori=?, harga=?, stok=?, barcode=? WHERE id_barang=?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, nama);
                ps.setString(2, kategori);
                ps.setInt(3, harga);
                ps.setInt(4, stok);
                ps.setString(5, barcode); // SET BARCODE BARU
                ps.setString(6, idBarangTerpilih);
                ps.executeUpdate();
            }

            JOptionPane.showMessageDialog(this, "Data diupdate!");
            cariData();
            resetForm();

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error Edit: " + e.getMessage());
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
            String sql = "DELETE FROM barang WHERE id_barang = ?";

            try (Connection conn = DatabaseManager.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {

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
                txtBarcodeBarang.setText("");
                idBarangTerpilih = ""; // Reset ID
                requestBarcodeFocus();

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

            String sql = "INSERT INTO kategori (nama_kategori) VALUES (?)";

            try (Connection conn = DatabaseManager.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {

                // 3. Simpan ke database (trim untuk menghapus spasi berlebih)
                pstmt.setString(1, kategoriBaru.trim());
                pstmt.executeUpdate();

                JOptionPane.showMessageDialog(this, "Kategori berhasil ditambahkan!");

                // 4. Refresh daftar di ComboBox
                loadKategori();

                // 5. Otomatis pilih kategori yang baru saja ditambahkan
                cbKategori.setSelectedItem(kategoriBaru.trim());
                requestBarcodeFocus();

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

            try (Connection conn = DatabaseManager.getConnection()) {

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
                    requestBarcodeFocus();
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
            String sql = "DELETE FROM users WHERE id_user = ?";

            try (Connection conn = DatabaseManager.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {

                pstmt.setInt(1, Integer.parseInt(idUser));
                pstmt.executeUpdate();

                JOptionPane.showMessageDialog(this, "Akun berhasil dihapus!");
                loadDataUser(); // Refresh tabel setelah dihapus

            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Error Hapus User: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }//GEN-LAST:event_btnHapusUserActionPerformed

    private void btnToggleTemaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnToggleTemaActionPerformed
        boolean darkMode = btnToggleTema.isSelected();
        try {
            applyTheme(darkMode);
            updateThemeToggleLabel(darkMode);
        } catch (Exception e) {
            btnToggleTema.setSelected(!darkMode);
            updateThemeToggleLabel(!darkMode);
            JOptionPane.showMessageDialog(this, "Gagal mengganti tema: " + e.getMessage(), "Tema", JOptionPane.ERROR_MESSAGE);
        }
    }//GEN-LAST:event_btnToggleTemaActionPerformed

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

    private void openWorkspace(String tabName) {
        if (hostWorkspace != null && hostWorkspace.isDisplayable()) {
            hostWorkspace.openTab(tabName);
            hostWorkspace.toFront();
            hostWorkspace.requestFocus();
            return;
        }

        if (formWorkspaceAktif == null || !formWorkspaceAktif.isVisible()) {
            formWorkspaceAktif = new AdminWorkspaceFrame(false);
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
    private javax.swing.JButton btnSimpan;
    private javax.swing.JButton btnSimpanUser;
    private javax.swing.JButton btnTambahKategori;
    private javax.swing.JToggleButton btnToggleTema;
    private javax.swing.JLabel cari;
    private javax.swing.JComboBox<String> cbFilterKategori;
    private javax.swing.JComboBox<String> cbKategori;
    private javax.swing.JComboBox<String> cbRoleUser;
    private javax.swing.JLabel filter;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel6;
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
    private javax.swing.JTextField txtBarcodeBarang;
    private javax.swing.JTextField txtCari;
    private javax.swing.JTextField txtHarga;
    private javax.swing.JTextField txtNama;
    private javax.swing.JPasswordField txtPasswordUser;
    private javax.swing.JTextField txtStok;
    private javax.swing.JTextField txtUsernameUser;
    // End of variables declaration//GEN-END:variables
}
