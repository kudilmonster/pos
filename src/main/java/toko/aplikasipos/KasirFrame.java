package toko.aplikasipos;

import java.awt.Color;
import java.sql.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.util.logging.Logger;


public class KasirFrame extends javax.swing.JFrame {

    private static final String DB_URL = "jdbc:sqlite:pos_db.db";
    private static final Logger logger = Logger.getLogger(KasirFrame.class.getName());
    DefaultTableModel modelKeranjang;
    int totalBelanja = 0;
    private int mouseX, mouseY;
    
    
    public KasirFrame() {
        initComponents();

        initTable();
        loadProfilToko();
        loadKategoriKasir();
        loadBarang();
        setBackground(new Color(0, 0, 0, 0));
        
    }

    // ================= INIT =================
    private void initTable() {
        modelKeranjang = new DefaultTableModel(
                new String[]{"Nama Barang", "Harga", "Qty", "Subtotal"}, 0
        ) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        tblKeranjang.setModel(modelKeranjang);
    }

    // ================= HELPER =================
    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL);
    }

    private int toInt(Object value) {
        try {
            return Integer.parseInt(value.toString());
        } catch (Exception e) {
            return 0;
        }
    }

    private int getQtyDiKeranjang(String namaBarang) {
        int totalQty = 0;
        for (int i = 0; i < modelKeranjang.getRowCount(); i++) {
            Object nama = modelKeranjang.getValueAt(i, 0);
            if (nama != null && namaBarang.equals(nama.toString())) {
                totalQty += toInt(modelKeranjang.getValueAt(i, 2));
            }
        }
        return totalQty;
    }

    // ================= LOAD DATA =================
    private void loadProfilToko() {
        String sql = "SELECT nama_toko, alamat FROM profil_toko LIMIT 1";

        try (Connection c = getConnection(); Statement s = c.createStatement(); ResultSet r = s.executeQuery(sql)) {

            if (r.next()) {
                lblNamaToko.setText(r.getString("nama_toko"));
                lblAlamatToko.setText(r.getString("alamat"));
            }

        } catch (Exception e) {
            System.out.println("Error Profil: " + e.getMessage());
        }
    }

    private void loadKategoriKasir() {
        cbKategori.removeAllItems();
        cbKategori.addItem("Semua Kategori");

        String sql = "SELECT nama_kategori FROM kategori ORDER BY nama_kategori";

        try (Connection c = getConnection(); Statement s = c.createStatement(); ResultSet r = s.executeQuery(sql)) {

            while (r.next()) {
                cbKategori.addItem(r.getString(1));
            }

        } catch (Exception e) {
            System.out.println("Error kategori: " + e.getMessage());
        }
    }

    private void loadBarang() {
        cbPilihBarang.removeAllItems();

        String kategori = cbKategori.getSelectedItem() == null
                ? "Semua Kategori"
                : cbKategori.getSelectedItem().toString();

        String sql = "SELECT nama_barang FROM barang WHERE stok > 0";

        if (!kategori.equals("Semua Kategori")) {
            sql += " AND kategori = ?";
        }

        try (Connection c = getConnection(); PreparedStatement p = c.prepareStatement(sql)) {

            if (!kategori.equals("Semua Kategori")) {
                p.setString(1, kategori);
            }

            ResultSet r = p.executeQuery();

            while (r.next()) {
                cbPilihBarang.addItem(r.getString(1));
            }

        } catch (Exception e) {
            System.out.println("Error barang: " + e.getMessage());
        }
    }

    // ================= LOGIC =================
    private void hitungTotal() {
        int subtotal = 0;

        for (int i = 0; i < modelKeranjang.getRowCount(); i++) {
            subtotal += toInt(modelKeranjang.getValueAt(i, 3));
        }

        double diskon = ((Number) spnDiskon.getValue()).doubleValue();
        double pajak = ((Number) spnPajak.getValue()).doubleValue();

        double setelahDiskon = subtotal - (subtotal * diskon / 100);
        double total = setelahDiskon + (setelahDiskon * pajak / 100);

        totalBelanja = (int) Math.round(total);

        lblTotalHarga.setText("Rp : " + totalBelanja);
        txtBayarKeyReleased(null);
    }

    // ================= ACTION =================
    private void resetKasir() {
        modelKeranjang.setRowCount(0);
        totalBelanja = 0;

        lblTotalHarga.setText("Rp : ");
        lblKembalian.setText("Rp : ");

        txtBayar.setText("");
        spnDiskon.setValue(0);
        spnPajak.setValue(0);

        loadBarang();
    }

    private void cetakStruk(int total, int diskon, int pajak, int bayar, int kembalian, String kasir, String jenisBayar) {

        // 1. AMBIL PROFIL TOKO
        String namaToko = "NAMA TOKO";
        String alamatToko = "Alamat Toko";
        String url = "jdbc:sqlite:pos_db.db";

        try (Connection conn = DriverManager.getConnection(url); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery("SELECT nama_toko, alamat FROM profil_toko LIMIT 1")) {

            if (rs.next()) {
                namaToko = rs.getString("nama_toko");
                alamatToko = rs.getString("alamat");
            }

        } catch (Exception e) {
            System.out.println("Error load profil toko: " + e.getMessage());
        }

        // 2. SUSUN STRUK
        StringBuilder struk = new StringBuilder();
        int lebarStruk = 36;

        struk.append("====================================\n");
        struk.append(centerText(namaToko, lebarStruk)).append("\n");
        struk.append(centerText(alamatToko, lebarStruk)).append("\n");

        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd-MM-yyyy HH:mm");
        String tanggal = sdf.format(new java.util.Date());

        struk.append("====================================\n");
        struk.append("Kasir : ").append(kasir).append("\n");
        struk.append("Tgl   : ").append(tanggal).append("\n");
        struk.append("------------------------------------\n");

        // LOOP KERANJANG
        for (int i = 0; i < modelKeranjang.getRowCount(); i++) {
            String nama = modelKeranjang.getValueAt(i, 0).toString();
            String harga = modelKeranjang.getValueAt(i, 1).toString();
            String qty = modelKeranjang.getValueAt(i, 2).toString();
            String sub = modelKeranjang.getValueAt(i, 3).toString();

            struk.append(nama).append("\n");
            struk.append(qty).append(" x Rp ").append(harga)
                    .append("        Rp ").append(sub).append("\n");
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

        // 3. TAMPILKAN + OPSI
        JTextArea txtArea = new JTextArea(struk.toString());
        txtArea.setFont(new java.awt.Font("Monospaced", java.awt.Font.PLAIN, 12));

        Object[] options = {"Cetak (Printer)", "Simpan ke PDF", "Tutup"};

        int pilihan = JOptionPane.showOptionDialog(this,
                new JScrollPane(txtArea),
                "Struk Pembayaran",
                JOptionPane.YES_NO_CANCEL_OPTION,
                JOptionPane.INFORMATION_MESSAGE,
                null,
                options,
                options[0]);

        if (pilihan == 0) {
            try {
                txtArea.print();
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Gagal print: " + e.getMessage());
            }
        } else if (pilihan == 1) {
            String timeStamp = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss").format(new java.util.Date());
            simpanKePDF(struk.toString(), "Struk_" + timeStamp);
        }
    }

    private String centerText(String text, int width) {
        if (text.length() >= width) {
            return text;
        }

        int padding = (width - text.length()) / 2;
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < padding; i++) {
            sb.append(" ");
        }

        sb.append(text);
        return sb.toString();
    }

    private void simpanKePDF(String isi, String namaFile) {
        try {
            JFileChooser fc = new JFileChooser();
            fc.setSelectedFile(new java.io.File(namaFile + ".pdf"));

            int pilih = fc.showSaveDialog(this);

            if (pilih == JFileChooser.APPROVE_OPTION) {

                com.lowagie.text.Document doc = new com.lowagie.text.Document();
                com.lowagie.text.pdf.PdfWriter.getInstance(doc,
                        new java.io.FileOutputStream(fc.getSelectedFile().getAbsolutePath()));

                doc.open();

                com.lowagie.text.Font font = new com.lowagie.text.Font(
                        com.lowagie.text.Font.COURIER, 10);

                for (String line : isi.split("\n")) {
                    doc.add(new com.lowagie.text.Paragraph(line, font));
                }

                doc.close();

                JOptionPane.showMessageDialog(this, "PDF berhasil disimpan!");
            }

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error PDF: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        PanelUtamaTransaksi = new toko.aplikasipos.CustomRoundedPanel();
        panelBayar = new toko.aplikasipos.CustomRoundedPanel();
        btnSimpanTransaksi = new javax.swing.JButton();
        PanelProfil = new toko.aplikasipos.CustomRoundedPanel();
        lblNamaToko = new javax.swing.JLabel();
        lblAlamatToko = new javax.swing.JLabel();
        PanelListBayar = new toko.aplikasipos.CustomRoundedPanel();
        txtJenisBayar = new javax.swing.JLabel();
        txtBayar1 = new javax.swing.JLabel();
        txtDiscount = new javax.swing.JLabel();
        txtPPN = new javax.swing.JLabel();
        txtTotal = new javax.swing.JLabel();
        txtKembalian = new javax.swing.JLabel();
        PanelDuit = new toko.aplikasipos.CustomRoundedPanel();
        cbJenisBayar = new javax.swing.JComboBox<>();
        txtBayar = new javax.swing.JTextField();
        spnDiskon = new javax.swing.JSpinner();
        spnPajak = new javax.swing.JSpinner();
        lblTotalHarga = new javax.swing.JLabel();
        lblKembalian = new javax.swing.JLabel();
        PanelTransaksi = new toko.aplikasipos.CustomRoundedPanel();
        panelBarang = new toko.aplikasipos.CustomRoundedPanel();
        txtKategori = new javax.swing.JLabel();
        txtPilih = new javax.swing.JLabel();
        Qty = new javax.swing.JLabel();
        btnBatalKeranjang = new javax.swing.JButton();
        cbKategori = new javax.swing.JComboBox<>();
        cbPilihBarang = new javax.swing.JComboBox<>();
        spnQty = new javax.swing.JSpinner();
        btnHapusItem = new javax.swing.JButton();
        btnTambahKeranjang = new javax.swing.JButton();
        lblInfoHarga = new javax.swing.JLabel();
        lblInfoStok = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        tblKeranjang = new javax.swing.JTable();
        jLabel1 = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setUndecorated(true);
        setResizable(false);

        PanelUtamaTransaksi.setBottomLeftRound(false);
        PanelUtamaTransaksi.setBottomRightRound(false);
        PanelUtamaTransaksi.setcolorEnd(new java.awt.Color(44, 44, 84));
        PanelUtamaTransaksi.setcolorStart(new java.awt.Color(44, 44, 84));
        PanelUtamaTransaksi.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            public void mouseDragged(java.awt.event.MouseEvent evt) {
                PanelUtamaTransaksiMouseDragged(evt);
            }
        });
        PanelUtamaTransaksi.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent evt) {
                PanelUtamaTransaksiMousePressed(evt);
            }
        });

        panelBayar.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Pembayaran", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Segoe UI Black", 1, 18), new java.awt.Color(255, 255, 255))); // NOI18N
        panelBayar.setBottomLeftRound(false);
        panelBayar.setBottomRightRound(false);
        panelBayar.setcolorEnd(new java.awt.Color(64, 64, 122));
        panelBayar.setcolorStart(new java.awt.Color(64, 64, 122));
        panelBayar.setTopLeftRound(false);
        panelBayar.setTopRightRound(false);

        btnSimpanTransaksi.setBackground(new java.awt.Color(5, 196, 107));
        btnSimpanTransaksi.setFont(new java.awt.Font("Segoe UI Black", 1, 18)); // NOI18N
        btnSimpanTransaksi.setForeground(new java.awt.Color(255, 255, 255));
        btnSimpanTransaksi.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icon/pay.png"))); // NOI18N
        btnSimpanTransaksi.setText("BAYAR");
        btnSimpanTransaksi.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        btnSimpanTransaksi.addActionListener(this::btnSimpanTransaksiActionPerformed);

        PanelProfil.setBorder(javax.swing.BorderFactory.createEmptyBorder(10, 10, 10, 10));
        PanelProfil.setBottomLeftRound(false);
        PanelProfil.setBottomRightRound(false);
        PanelProfil.setcolorEnd(new java.awt.Color(64, 64, 122));
        PanelProfil.setcolorStart(new java.awt.Color(27, 20, 100));
        PanelProfil.setTopLeftRound(false);
        PanelProfil.setTopRightRound(false);
        PanelProfil.setLayout(new java.awt.GridLayout(2, 0));

        lblNamaToko.setFont(new java.awt.Font("Times New Roman", 1, 18)); // NOI18N
        lblNamaToko.setForeground(new java.awt.Color(255, 255, 255));
        lblNamaToko.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblNamaToko.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        PanelProfil.add(lblNamaToko);

        lblAlamatToko.setFont(new java.awt.Font("Times New Roman", 0, 12)); // NOI18N
        lblAlamatToko.setForeground(new java.awt.Color(255, 255, 255));
        lblAlamatToko.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblAlamatToko.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        PanelProfil.add(lblAlamatToko);

        PanelListBayar.setBorder(javax.swing.BorderFactory.createEmptyBorder(10, 10, 10, 10));
        PanelListBayar.setBottomLeftRound(false);
        PanelListBayar.setBottomRightRound(false);
        PanelListBayar.setcolorEnd(new java.awt.Color(64, 64, 122));
        PanelListBayar.setcolorStart(new java.awt.Color(64, 64, 122));
        PanelListBayar.setTopLeftRound(false);
        PanelListBayar.setTopRightRound(false);
        PanelListBayar.setLayout(new java.awt.GridLayout(6, 0, 0, 5));

        txtJenisBayar.setFont(new java.awt.Font("Yu Gothic UI Semibold", 0, 14)); // NOI18N
        txtJenisBayar.setForeground(new java.awt.Color(255, 255, 255));
        txtJenisBayar.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        txtJenisBayar.setText("Jenis Bayar");
        txtJenisBayar.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
        PanelListBayar.add(txtJenisBayar);

        txtBayar1.setFont(new java.awt.Font("Yu Gothic UI Semibold", 0, 14)); // NOI18N
        txtBayar1.setForeground(new java.awt.Color(255, 255, 255));
        txtBayar1.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        txtBayar1.setText("Bayar");
        txtBayar1.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
        PanelListBayar.add(txtBayar1);

        txtDiscount.setFont(new java.awt.Font("Yu Gothic UI Semibold", 0, 14)); // NOI18N
        txtDiscount.setForeground(new java.awt.Color(255, 255, 255));
        txtDiscount.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        txtDiscount.setText("Diskon");
        txtDiscount.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
        PanelListBayar.add(txtDiscount);

        txtPPN.setFont(new java.awt.Font("Yu Gothic UI Semibold", 0, 14)); // NOI18N
        txtPPN.setForeground(new java.awt.Color(255, 255, 255));
        txtPPN.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        txtPPN.setText("Pajak");
        txtPPN.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
        PanelListBayar.add(txtPPN);

        txtTotal.setFont(new java.awt.Font("Yu Gothic UI Semibold", 0, 14)); // NOI18N
        txtTotal.setForeground(new java.awt.Color(255, 255, 255));
        txtTotal.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        txtTotal.setText("Total Akhir");
        txtTotal.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
        PanelListBayar.add(txtTotal);

        txtKembalian.setFont(new java.awt.Font("Yu Gothic UI Semibold", 0, 14)); // NOI18N
        txtKembalian.setForeground(new java.awt.Color(255, 255, 255));
        txtKembalian.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        txtKembalian.setText("Kembalian");
        txtKembalian.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
        PanelListBayar.add(txtKembalian);

        PanelDuit.setBorder(javax.swing.BorderFactory.createEmptyBorder(10, 10, 10, 10));
        PanelDuit.setBottomLeftRound(false);
        PanelDuit.setBottomRightRound(false);
        PanelDuit.setcolorEnd(new java.awt.Color(64, 64, 122));
        PanelDuit.setcolorStart(new java.awt.Color(64, 64, 122));
        PanelDuit.setTopLeftRound(false);
        PanelDuit.setTopRightRound(false);
        PanelDuit.setLayout(new java.awt.GridLayout(6, 0, 0, 5));

        cbJenisBayar.setFont(new java.awt.Font("Segoe UI Semibold", 0, 14)); // NOI18N
        cbJenisBayar.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Tunai", "Debit", "Qris" }));
        PanelDuit.add(cbJenisBayar);

        txtBayar.setFont(new java.awt.Font("Segoe UI Semibold", 0, 14)); // NOI18N
        txtBayar.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                txtBayarKeyReleased(evt);
            }
        });
        PanelDuit.add(txtBayar);

        spnDiskon.setFont(new java.awt.Font("Segoe UI Semibold", 0, 14)); // NOI18N
        spnDiskon.setModel(new javax.swing.SpinnerNumberModel(0, 0, 100, 1));
        spnDiskon.addChangeListener(this::spnDiskonStateChanged);
        PanelDuit.add(spnDiskon);

        spnPajak.setFont(new java.awt.Font("Segoe UI Semibold", 0, 14)); // NOI18N
        spnPajak.setModel(new javax.swing.SpinnerNumberModel(0, 0, 100, 1));
        spnPajak.addChangeListener(this::spnPajakStateChanged);
        PanelDuit.add(spnPajak);

        lblTotalHarga.setFont(new java.awt.Font("SimSun-ExtB", 1, 24)); // NOI18N
        lblTotalHarga.setForeground(new java.awt.Color(255, 255, 255));
        lblTotalHarga.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        lblTotalHarga.setText("Rp : ");
        lblTotalHarga.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        PanelDuit.add(lblTotalHarga);

        lblKembalian.setFont(new java.awt.Font("SimSun-ExtB", 1, 24)); // NOI18N
        lblKembalian.setForeground(new java.awt.Color(255, 255, 255));
        lblKembalian.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        lblKembalian.setText("Rp : ");
        lblKembalian.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        PanelDuit.add(lblKembalian);

        javax.swing.GroupLayout panelBayarLayout = new javax.swing.GroupLayout(panelBayar);
        panelBayar.setLayout(panelBayarLayout);
        panelBayarLayout.setHorizontalGroup(
            panelBayarLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelBayarLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(PanelProfil, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, panelBayarLayout.createSequentialGroup()
                .addContainerGap(248, Short.MAX_VALUE)
                .addComponent(btnSimpanTransaksi, javax.swing.GroupLayout.PREFERRED_SIZE, 147, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(22, 22, 22))
            .addGroup(panelBayarLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(panelBayarLayout.createSequentialGroup()
                    .addGap(9, 9, 9)
                    .addComponent(PanelListBayar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGap(18, 18, 18)
                    .addComponent(PanelDuit, javax.swing.GroupLayout.DEFAULT_SIZE, 248, Short.MAX_VALUE)
                    .addContainerGap()))
        );
        panelBayarLayout.setVerticalGroup(
            panelBayarLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelBayarLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(PanelProfil, javax.swing.GroupLayout.PREFERRED_SIZE, 96, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 313, Short.MAX_VALUE)
                .addComponent(btnSimpanTransaksi)
                .addGap(32, 32, 32))
            .addGroup(panelBayarLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(panelBayarLayout.createSequentialGroup()
                    .addGap(111, 111, 111)
                    .addGroup(panelBayarLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(PanelListBayar, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(PanelDuit, javax.swing.GroupLayout.DEFAULT_SIZE, 261, Short.MAX_VALUE))
                    .addGap(112, 112, 112)))
        );

        PanelTransaksi.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Transaksi Penjualan", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Segoe UI Black", 1, 18), new java.awt.Color(255, 255, 255))); // NOI18N
        PanelTransaksi.setBottomLeftRound(false);
        PanelTransaksi.setBottomRightRound(false);
        PanelTransaksi.setcolorEnd(new java.awt.Color(64, 64, 122));
        PanelTransaksi.setcolorStart(new java.awt.Color(64, 64, 122));
        PanelTransaksi.setTopLeftRound(false);
        PanelTransaksi.setTopRightRound(false);

        panelBarang.setBackground(new java.awt.Color(189, 195, 199));
        panelBarang.setBorder(javax.swing.BorderFactory.createEmptyBorder(10, 10, 10, 10));
        panelBarang.setBottomLeftRound(false);
        panelBarang.setBottomRightRound(false);
        panelBarang.setcolorEnd(new java.awt.Color(64, 64, 122));
        panelBarang.setcolorStart(new java.awt.Color(27, 20, 100));
        panelBarang.setTopLeftRound(false);
        panelBarang.setTopRightRound(false);
        panelBarang.setLayout(new java.awt.GridLayout(3, 0, 10, 10));

        txtKategori.setFont(new java.awt.Font("Segoe UI Semibold", 0, 14)); // NOI18N
        txtKategori.setForeground(new java.awt.Color(255, 255, 255));
        txtKategori.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        txtKategori.setText("Pilih Kategori");
        txtKategori.setBorder(new javax.swing.border.LineBorder(new java.awt.Color(255, 255, 255), 2, true));
        txtKategori.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        panelBarang.add(txtKategori);

        txtPilih.setFont(new java.awt.Font("Segoe UI Semibold", 0, 14)); // NOI18N
        txtPilih.setForeground(new java.awt.Color(255, 255, 255));
        txtPilih.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        txtPilih.setText("Pilih Barang");
        txtPilih.setBorder(new javax.swing.border.LineBorder(new java.awt.Color(255, 255, 255), 2, true));
        txtPilih.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        panelBarang.add(txtPilih);

        Qty.setFont(new java.awt.Font("Segoe UI Semibold", 0, 14)); // NOI18N
        Qty.setForeground(new java.awt.Color(255, 255, 255));
        Qty.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        Qty.setText("Qty");
        Qty.setBorder(new javax.swing.border.LineBorder(new java.awt.Color(255, 255, 255), 2, true));
        Qty.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        panelBarang.add(Qty);

        btnBatalKeranjang.setBackground(new java.awt.Color(189, 195, 199));
        btnBatalKeranjang.setFont(new java.awt.Font("Segoe UI Semibold", 0, 12)); // NOI18N
        btnBatalKeranjang.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icon/hapusall.png"))); // NOI18N
        btnBatalKeranjang.setText(" HAPUS ALL");
        btnBatalKeranjang.setActionCommand("Kosongkan Keranjang");
        btnBatalKeranjang.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        btnBatalKeranjang.addActionListener(this::btnBatalKeranjangActionPerformed);
        panelBarang.add(btnBatalKeranjang);

        cbKategori.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        cbKategori.addActionListener(this::cbKategoriActionPerformed);
        panelBarang.add(cbKategori);

        cbPilihBarang.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        cbPilihBarang.addActionListener(this::cbPilihBarangActionPerformed);
        panelBarang.add(cbPilihBarang);

        spnQty.setFont(new java.awt.Font("Segoe UI", 0, 24)); // NOI18N
        spnQty.setModel(new javax.swing.SpinnerNumberModel(1, 1, null, 1));
        panelBarang.add(spnQty);

        btnHapusItem.setBackground(new java.awt.Color(255, 168, 1));
        btnHapusItem.setFont(new java.awt.Font("Segoe UI Semibold", 0, 12)); // NOI18N
        btnHapusItem.setIcon(new javax.swing.ImageIcon(getClass().getResource("/hapus.png"))); // NOI18N
        btnHapusItem.setText("HAPUS ITEM");
        btnHapusItem.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        btnHapusItem.addActionListener(this::btnHapusItemActionPerformed);
        panelBarang.add(btnHapusItem);

        btnTambahKeranjang.setBackground(new java.awt.Color(75, 207, 250));
        btnTambahKeranjang.setFont(new java.awt.Font("Segoe UI Semibold", 0, 12)); // NOI18N
        btnTambahKeranjang.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icon/keranjang.png"))); // NOI18N
        btnTambahKeranjang.setText("TAMBAH");
        btnTambahKeranjang.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        btnTambahKeranjang.addActionListener(this::btnTambahKeranjangActionPerformed);
        panelBarang.add(btnTambahKeranjang);

        lblInfoHarga.setFont(new java.awt.Font("Segoe UI Semibold", 0, 14)); // NOI18N
        lblInfoHarga.setForeground(new java.awt.Color(255, 255, 255));
        lblInfoHarga.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblInfoHarga.setText("Harga: Rp. 0");
        lblInfoHarga.setBorder(new javax.swing.border.LineBorder(new java.awt.Color(255, 255, 255), 2, true));
        lblInfoHarga.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        panelBarang.add(lblInfoHarga);

        lblInfoStok.setFont(new java.awt.Font("Segoe UI Semibold", 0, 14)); // NOI18N
        lblInfoStok.setForeground(new java.awt.Color(255, 255, 255));
        lblInfoStok.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblInfoStok.setText("Sisa Stok: 0");
        lblInfoStok.setBorder(new javax.swing.border.LineBorder(new java.awt.Color(255, 255, 255), 2, true));
        lblInfoStok.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        panelBarang.add(lblInfoStok);

        tblKeranjang.setFont(new java.awt.Font("Segoe UI Semibold", 0, 14)); // NOI18N
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
        tblKeranjang.setGridColor(new java.awt.Color(220, 221, 225));
        tblKeranjang.setRowHeight(30);
        tblKeranjang.setRowMargin(2);
        tblKeranjang.setShowGrid(true);
        jScrollPane1.setViewportView(tblKeranjang);

        javax.swing.GroupLayout PanelTransaksiLayout = new javax.swing.GroupLayout(PanelTransaksi);
        PanelTransaksi.setLayout(PanelTransaksiLayout);
        PanelTransaksiLayout.setHorizontalGroup(
            PanelTransaksiLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, PanelTransaksiLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(PanelTransaksiLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jScrollPane1)
                    .addComponent(panelBarang, javax.swing.GroupLayout.DEFAULT_SIZE, 677, Short.MAX_VALUE))
                .addContainerGap())
        );
        PanelTransaksiLayout.setVerticalGroup(
            PanelTransaksiLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(PanelTransaksiLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(panelBarang, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 311, Short.MAX_VALUE)
                .addContainerGap())
        );

        jLabel1.setFont(new java.awt.Font("Times New Roman", 1, 18)); // NOI18N
        jLabel1.setForeground(new java.awt.Color(204, 204, 204));
        jLabel1.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel1.setText("sanFK POS");
        jLabel1.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        jLabel1.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jLabel1MouseClicked(evt);
            }
        });

        javax.swing.GroupLayout PanelUtamaTransaksiLayout = new javax.swing.GroupLayout(PanelUtamaTransaksi);
        PanelUtamaTransaksi.setLayout(PanelUtamaTransaksiLayout);
        PanelUtamaTransaksiLayout.setHorizontalGroup(
            PanelUtamaTransaksiLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, PanelUtamaTransaksiLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(PanelTransaksi, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(panelBayar, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, PanelUtamaTransaksiLayout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jLabel1)
                .addGap(15, 15, 15))
        );
        PanelUtamaTransaksiLayout.setVerticalGroup(
            PanelUtamaTransaksiLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(PanelUtamaTransaksiLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(PanelUtamaTransaksiLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(PanelTransaksi, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(panelBayar, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(PanelUtamaTransaksi, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(PanelUtamaTransaksi, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
        );

        pack();
        setLocationRelativeTo(null);
    }// </editor-fold>//GEN-END:initComponents

    private void btnTambahKeranjangActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnTambahKeranjangActionPerformed
        if (cbPilihBarang.getSelectedItem() == null) {
            JOptionPane.showMessageDialog(this, "Pilih barang!");
            return;
        }

        String nama = cbPilihBarang.getSelectedItem().toString();
        int qty = (Integer) spnQty.getValue();
        if (qty <= 0) {
            JOptionPane.showMessageDialog(this, "Qty harus lebih dari 0.");
            return;
        }

        String sql = "SELECT harga, stok FROM barang WHERE nama_barang=?";

        try (Connection c = getConnection(); PreparedStatement p = c.prepareStatement(sql)) {

            p.setString(1, nama);
            ResultSet r = p.executeQuery();

            if (r.next()) {
                int harga = r.getInt("harga");
                int stok = r.getInt("stok");
                int qtyDiKeranjang = getQtyDiKeranjang(nama);
                int qtySetelahTambah = qtyDiKeranjang + qty;

                if (qtySetelahTambah > stok) {
                    JOptionPane.showMessageDialog(this,
                            "Stok kurang! Stok tersedia: " + stok + ", sudah di keranjang: " + qtyDiKeranjang);
                    return;
                }

                int subtotal = harga * qty;

                modelKeranjang.addRow(new Object[]{
                    nama, harga, qty, subtotal
                });

                hitungTotal();
                spnQty.setValue(1);
            }

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, e.getMessage());
        }
    }//GEN-LAST:event_btnTambahKeranjangActionPerformed

    private void txtBayarKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_txtBayarKeyReleased
        try {
            // Ambil nominal bayar, ubah ke angka
            String bayarStr = txtBayar.getText();
            if (bayarStr.isEmpty()) {
                lblKembalian.setText("Rp : ");
                return;
            }

            int bayar = Integer.parseInt(bayarStr);
            int kembalian = bayar - totalBelanja;

            // Tampilkan ke label kembalian
            if (kembalian < 0) {
                lblKembalian.setText("Uang Kurang!");
            } else {
                lblKembalian.setText("Rp : " + kembalian);
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

        // Ambil jenis bayar (Tunai, Debit, Qris)
        String jenisBayar = cbJenisBayar.getSelectedItem().toString();

        int bayar = 0;
        int kembalian = 0;

        // --- 2. JALUR PEMBAYARAN QRIS (SEMI-MANUAL) ---
        if (jenisBayar.equalsIgnoreCase("Qris")) {
            // Tampilkan pop-up menunggu pembayaran
            Object[] opsi = {"Konfirmasi Lunas", "Batal"};
            int pilihanQris = JOptionPane.showOptionDialog(this,
                    "Tagihan: Rp " + totalBelanja + "\n\n"
                    + "Silakan arahkan pelanggan untuk scan QRIS di meja kasir.\n"
                    + "Tunggu hingga ada notifikasi uang masuk di HP/Sistem Toko Anda,\n"
                    + "lalu klik 'Konfirmasi Lunas'.",
                    "Menunggu Pembayaran QRIS...",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.INFORMATION_MESSAGE,
                    null, opsi, opsi[0]);

            // Jika kasir klik Batal atau silang (X)
            if (pilihanQris != JOptionPane.YES_OPTION) {
                return; // Batalkan proses simpan transaksi
            }

            // Jika lunas, atur bayar pas dan kembalian 0
            bayar = totalBelanja;
            kembalian = 0;

        } // --- 3. JALUR PEMBAYARAN TUNAI / DEBIT ---
        else {
            String bayarStr = txtBayar.getText().trim();
            if (bayarStr.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Masukkan nominal uang pembayaran!", "Peringatan", JOptionPane.WARNING_MESSAGE);
                return;
            }

            try {
                bayar = Integer.parseInt(bayarStr);
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "Nominal pembayaran harus berupa angka!", "Peringatan", JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (bayar < totalBelanja) {
                JOptionPane.showMessageDialog(this, "Uang pembayaran kurang!", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            kembalian = bayar - totalBelanja;
        }

        // 4. Ambil data tambahan dari komponen GUI Kasir
        int nilaiDiskon = (Integer) spnDiskon.getValue();
        int nilaiPajak = (Integer) spnPajak.getValue();

        // Ambil nama kasir
        String namaKasir = LoginFrame.kasirAktif;
        if (namaKasir.isEmpty()) {
            namaKasir = "Admin Default";
        }

        // 5. PROSES SIMPAN KE DATABASE SECARA ATOMIC
        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            try {
                String sqlTransaksi = "INSERT INTO transaksi (total_harga, diskon_persen, pajak_persen, bayar, kembalian, status_transaksi, updated_by) VALUES (?, ?, ?, ?, ?, 'NORMAL', ?)";
                int idTransaksi;
                try (PreparedStatement pstTrans = conn.prepareStatement(sqlTransaksi, Statement.RETURN_GENERATED_KEYS)) {
                    pstTrans.setInt(1, totalBelanja);
                    pstTrans.setInt(2, nilaiDiskon);
                    pstTrans.setInt(3, nilaiPajak);
                    pstTrans.setInt(4, bayar);
                    pstTrans.setInt(5, kembalian);
                    pstTrans.setString(6, namaKasir);
                    pstTrans.executeUpdate();

                    ResultSet rsKeys = pstTrans.getGeneratedKeys();
                    if (!rsKeys.next()) {
                        throw new SQLException("Gagal membuat ID transaksi.");
                    }
                    idTransaksi = rsKeys.getInt(1);
                }

                String sqlDetail = "INSERT INTO detail_transaksi (id_transaksi, nama_barang, harga, qty, subtotal, harga_modal, laba_kotor) VALUES (?, ?, ?, ?, ?, ?, ?)";
                String sqlUpdateStok = "UPDATE barang SET stok = stok - ? WHERE nama_barang = ? AND stok >= ?";
                String sqlModal = "SELECT COALESCE(harga_modal, 0) FROM barang WHERE nama_barang = ?";

                try (PreparedStatement pstDetail = conn.prepareStatement(sqlDetail);
                     PreparedStatement pstUpdateStok = conn.prepareStatement(sqlUpdateStok);
                     PreparedStatement pstModal = conn.prepareStatement(sqlModal)) {
                    int jumlahBaris = modelKeranjang.getRowCount();
                    for (int i = 0; i < jumlahBaris; i++) {
                        String nama = modelKeranjang.getValueAt(i, 0).toString();
                        int harga = Integer.parseInt(modelKeranjang.getValueAt(i, 1).toString());
                        int qty = Integer.parseInt(modelKeranjang.getValueAt(i, 2).toString());
                        int subtotal = Integer.parseInt(modelKeranjang.getValueAt(i, 3).toString());
                        int hargaModal = 0;
                        pstModal.setString(1, nama);
                        try (ResultSet rsModal = pstModal.executeQuery()) {
                            if (rsModal.next()) {
                                hargaModal = rsModal.getInt(1);
                            }
                        }
                        int labaKotor = subtotal - (qty * hargaModal);

                        pstDetail.setInt(1, idTransaksi);
                        pstDetail.setString(2, nama);
                        pstDetail.setInt(3, harga);
                        pstDetail.setInt(4, qty);
                        pstDetail.setInt(5, subtotal);
                        pstDetail.setInt(6, hargaModal);
                        pstDetail.setInt(7, labaKotor);
                        pstDetail.executeUpdate();

                        pstUpdateStok.setInt(1, qty);
                        pstUpdateStok.setString(2, nama);
                        pstUpdateStok.setInt(3, qty);
                        int updated = pstUpdateStok.executeUpdate();
                        if (updated == 0) {
                            throw new SQLException("Stok barang '" + nama + "' tidak mencukupi saat simpan transaksi.");
                        }
                    }
                }

                conn.commit();

                JOptionPane.showMessageDialog(this, "Transaksi Berhasil!");
                cetakStruk(totalBelanja, nilaiDiskon, nilaiPajak, bayar, kembalian, namaKasir, jenisBayar);
                resetKasir();
            } catch (Exception e) {
                conn.rollback();
                throw e;
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

    private void btnHapusItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnHapusItemActionPerformed
        int row = tblKeranjang.getSelectedRow();

        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Pilih item!");
            return;
        }

        modelKeranjang.removeRow(row);
        hitungTotal();
    }//GEN-LAST:event_btnHapusItemActionPerformed

    private void btnBatalKeranjangActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnBatalKeranjangActionPerformed
// Cek apakah keranjang sudah kosong
        if (modelKeranjang.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this, "Keranjang sudah kosong!");
            return;
        }

        // Minta konfirmasi sebelum menghapus semua
        int konfirmasi = JOptionPane.showConfirmDialog(this, "Yakin ingin membatalkan transaksi dan mengosongkan keranjang?", "Konfirmasi", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);

        if (konfirmasi == JOptionPane.YES_OPTION) {
            // Panggil fungsi resetKasir() yang sudah dibuat sebelumnya! Jauh lebih praktis.
            resetKasir();
        }
    }//GEN-LAST:event_btnBatalKeranjangActionPerformed

    private void PanelUtamaTransaksiMousePressed(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_PanelUtamaTransaksiMousePressed
       mouseX = evt.getX();
        mouseY = evt.getY();
    }//GEN-LAST:event_PanelUtamaTransaksiMousePressed

    private void PanelUtamaTransaksiMouseDragged(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_PanelUtamaTransaksiMouseDragged
        int x = evt.getXOnScreen();
        int y = evt.getYOnScreen();
        setLocation(x - mouseX, y - mouseY);
    }//GEN-LAST:event_PanelUtamaTransaksiMouseDragged

    private void jLabel1MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jLabel1MouseClicked
       System.exit(0);
    }//GEN-LAST:event_jLabel1MouseClicked

    public static void main(String args[]) {

        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(() -> new KasirFrame().setVisible(true));
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private toko.aplikasipos.CustomRoundedPanel PanelDuit;
    private toko.aplikasipos.CustomRoundedPanel PanelListBayar;
    private toko.aplikasipos.CustomRoundedPanel PanelProfil;
    private toko.aplikasipos.CustomRoundedPanel PanelTransaksi;
    private toko.aplikasipos.CustomRoundedPanel PanelUtamaTransaksi;
    private javax.swing.JLabel Qty;
    private javax.swing.JButton btnBatalKeranjang;
    private javax.swing.JButton btnHapusItem;
    private javax.swing.JButton btnSimpanTransaksi;
    private javax.swing.JButton btnTambahKeranjang;
    private javax.swing.JComboBox<String> cbJenisBayar;
    private javax.swing.JComboBox<String> cbKategori;
    private javax.swing.JComboBox<String> cbPilihBarang;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JLabel lblAlamatToko;
    private javax.swing.JLabel lblInfoHarga;
    private javax.swing.JLabel lblInfoStok;
    private javax.swing.JLabel lblKembalian;
    private javax.swing.JLabel lblNamaToko;
    private javax.swing.JLabel lblTotalHarga;
    private toko.aplikasipos.CustomRoundedPanel panelBarang;
    private toko.aplikasipos.CustomRoundedPanel panelBayar;
    private javax.swing.JSpinner spnDiskon;
    private javax.swing.JSpinner spnPajak;
    private javax.swing.JSpinner spnQty;
    private javax.swing.JTable tblKeranjang;
    private javax.swing.JTextField txtBayar;
    private javax.swing.JLabel txtBayar1;
    private javax.swing.JLabel txtDiscount;
    private javax.swing.JLabel txtJenisBayar;
    private javax.swing.JLabel txtKategori;
    private javax.swing.JLabel txtKembalian;
    private javax.swing.JLabel txtPPN;
    private javax.swing.JLabel txtPilih;
    private javax.swing.JLabel txtTotal;
    // End of variables declaration//GEN-END:variables
}
