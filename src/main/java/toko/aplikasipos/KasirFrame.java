package toko.aplikasipos;

import java.sql.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.util.logging.Logger;

public class KasirFrame extends javax.swing.JFrame {

    private static final String DB_URL = "jdbc:sqlite:pos_db.db";
    private static final Logger logger = Logger.getLogger(KasirFrame.class.getName());
    DefaultTableModel modelKeranjang;
    int totalBelanja = 0;

    public KasirFrame() {
        initComponents();

        initTable();
        loadProfilToko();
        loadKategoriKasir();
        loadBarang();
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

        customRoundedPanel2 = new toko.aplikasipos.CustomRoundedPanel();
        panelBayar = new toko.aplikasipos.CustomRoundedPanel();
        panelKeranjang = new toko.aplikasipos.CustomRoundedPanel();
        btnSimpanTransaksi = new javax.swing.JButton();
        customRoundedPanel5 = new toko.aplikasipos.CustomRoundedPanel();
        cbJenisBayar = new javax.swing.JComboBox<>();
        txtBayar = new javax.swing.JTextField();
        spnDiskon = new javax.swing.JSpinner();
        spnPajak = new javax.swing.JSpinner();
        lblTotalHarga = new javax.swing.JLabel();
        lblKembalian = new javax.swing.JLabel();
        customRoundedPanel6 = new toko.aplikasipos.CustomRoundedPanel();
        txtJenisBayar = new javax.swing.JLabel();
        txtBayar1 = new javax.swing.JLabel();
        txtDiscount = new javax.swing.JLabel();
        txtPPN = new javax.swing.JLabel();
        txtTotal = new javax.swing.JLabel();
        txtKembalian = new javax.swing.JLabel();
        panelBarang = new toko.aplikasipos.CustomRoundedPanel();
        txtKategori = new javax.swing.JLabel();
        txtPilih = new javax.swing.JLabel();
        lblInfoStok = new javax.swing.JLabel();
        cbKategori = new javax.swing.JComboBox<>();
        cbPilihBarang = new javax.swing.JComboBox<>();
        lblInfoHarga = new javax.swing.JLabel();
        customRoundedPanel1 = new toko.aplikasipos.CustomRoundedPanel();
        jLabel1 = new javax.swing.JLabel();
        btnHapusItem = new javax.swing.JButton();
        btnTambahKeranjang = new javax.swing.JButton();
        btnBatalKeranjang = new javax.swing.JButton();
        customRoundedPanel3 = new toko.aplikasipos.CustomRoundedPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        tblKeranjang = new javax.swing.JTable();
        customRoundedPanel4 = new toko.aplikasipos.CustomRoundedPanel();
        customRoundedPanel7 = new toko.aplikasipos.CustomRoundedPanel();
        lblNamaToko = new javax.swing.JLabel();
        lblAlamatToko = new javax.swing.JLabel();
        spnQty = new javax.swing.JSpinner();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setResizable(false);

        customRoundedPanel2.setBottomLeftRound(false);
        customRoundedPanel2.setBottomRightRound(false);
        customRoundedPanel2.setcolorEnd(new java.awt.Color(44, 44, 84));
        customRoundedPanel2.setcolorStart(new java.awt.Color(44, 44, 84));

        panelBayar.setBottomLeftRound(false);
        panelBayar.setBottomRightRound(false);
        panelBayar.setcolorEnd(new java.awt.Color(64, 64, 122));
        panelBayar.setcolorStart(new java.awt.Color(64, 64, 122));
        panelBayar.setTopLeftRound(false);
        panelBayar.setTopRightRound(false);

        panelKeranjang.setBottomLeftRound(false);
        panelKeranjang.setBottomRightRound(false);
        panelKeranjang.setcolorEnd(new java.awt.Color(255, 255, 255));
        panelKeranjang.setcolorStart(new java.awt.Color(255, 255, 255));
        panelKeranjang.setTopLeftRound(false);
        panelKeranjang.setTopRightRound(false);
        panelKeranjang.setLayout(new java.awt.GridLayout(1, 0, 10, 10));

        btnSimpanTransaksi.setBackground(new java.awt.Color(220, 221, 225));
        btnSimpanTransaksi.setText("Simpan Transaksi");
        btnSimpanTransaksi.addActionListener(this::btnSimpanTransaksiActionPerformed);
        panelKeranjang.add(btnSimpanTransaksi);

        customRoundedPanel5.setBottomLeftRound(false);
        customRoundedPanel5.setBottomRightRound(false);
        customRoundedPanel5.setcolorEnd(new java.awt.Color(64, 64, 122));
        customRoundedPanel5.setcolorStart(new java.awt.Color(64, 64, 122));
        customRoundedPanel5.setTopLeftRound(false);
        customRoundedPanel5.setTopRightRound(false);
        customRoundedPanel5.setLayout(new java.awt.GridLayout(6, 0, 0, 5));

        cbJenisBayar.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Tunai", "Debit", "Qris" }));
        customRoundedPanel5.add(cbJenisBayar);

        txtBayar.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                txtBayarKeyReleased(evt);
            }
        });
        customRoundedPanel5.add(txtBayar);

        spnDiskon.setModel(new javax.swing.SpinnerNumberModel(0, 0, 100, 1));
        spnDiskon.addChangeListener(this::spnDiskonStateChanged);
        customRoundedPanel5.add(spnDiskon);

        spnPajak.setModel(new javax.swing.SpinnerNumberModel(0, 0, 100, 1));
        spnPajak.addChangeListener(this::spnPajakStateChanged);
        customRoundedPanel5.add(spnPajak);

        lblTotalHarga.setFont(new java.awt.Font("SimSun-ExtB", 1, 24)); // NOI18N
        lblTotalHarga.setForeground(new java.awt.Color(255, 255, 255));
        lblTotalHarga.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        lblTotalHarga.setText("Rp : ");
        lblTotalHarga.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        customRoundedPanel5.add(lblTotalHarga);

        lblKembalian.setFont(new java.awt.Font("SimSun-ExtB", 1, 24)); // NOI18N
        lblKembalian.setForeground(new java.awt.Color(255, 255, 255));
        lblKembalian.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        lblKembalian.setText("Rp : ");
        lblKembalian.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        customRoundedPanel5.add(lblKembalian);

        customRoundedPanel6.setBottomLeftRound(false);
        customRoundedPanel6.setBottomRightRound(false);
        customRoundedPanel6.setcolorEnd(new java.awt.Color(64, 64, 122));
        customRoundedPanel6.setcolorStart(new java.awt.Color(64, 64, 122));
        customRoundedPanel6.setTopLeftRound(false);
        customRoundedPanel6.setTopRightRound(false);
        customRoundedPanel6.setLayout(new java.awt.GridLayout(6, 0, 0, 5));

        txtJenisBayar.setFont(new java.awt.Font("Yu Gothic UI Semibold", 0, 14)); // NOI18N
        txtJenisBayar.setForeground(new java.awt.Color(255, 255, 255));
        txtJenisBayar.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        txtJenisBayar.setText("Jenis Bayar");
        txtJenisBayar.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
        customRoundedPanel6.add(txtJenisBayar);

        txtBayar1.setFont(new java.awt.Font("Yu Gothic UI Semibold", 0, 14)); // NOI18N
        txtBayar1.setForeground(new java.awt.Color(255, 255, 255));
        txtBayar1.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        txtBayar1.setText("Bayar");
        txtBayar1.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
        customRoundedPanel6.add(txtBayar1);

        txtDiscount.setFont(new java.awt.Font("Yu Gothic UI Semibold", 0, 14)); // NOI18N
        txtDiscount.setForeground(new java.awt.Color(255, 255, 255));
        txtDiscount.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        txtDiscount.setText("Diskon");
        txtDiscount.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
        customRoundedPanel6.add(txtDiscount);

        txtPPN.setFont(new java.awt.Font("Yu Gothic UI Semibold", 0, 14)); // NOI18N
        txtPPN.setForeground(new java.awt.Color(255, 255, 255));
        txtPPN.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        txtPPN.setText("Pajak");
        txtPPN.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
        customRoundedPanel6.add(txtPPN);

        txtTotal.setFont(new java.awt.Font("Yu Gothic UI Semibold", 0, 14)); // NOI18N
        txtTotal.setForeground(new java.awt.Color(255, 255, 255));
        txtTotal.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        txtTotal.setText("Total Akhir");
        txtTotal.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
        customRoundedPanel6.add(txtTotal);

        txtKembalian.setFont(new java.awt.Font("Yu Gothic UI Semibold", 0, 14)); // NOI18N
        txtKembalian.setForeground(new java.awt.Color(255, 255, 255));
        txtKembalian.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        txtKembalian.setText("Kembalian");
        txtKembalian.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
        customRoundedPanel6.add(txtKembalian);

        javax.swing.GroupLayout panelBayarLayout = new javax.swing.GroupLayout(panelBayar);
        panelBayar.setLayout(panelBayarLayout);
        panelBayarLayout.setHorizontalGroup(
            panelBayarLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelBayarLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(panelBayarLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(panelBayarLayout.createSequentialGroup()
                        .addComponent(panelKeranjang, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addContainerGap())
                    .addGroup(panelBayarLayout.createSequentialGroup()
                        .addComponent(customRoundedPanel6, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(customRoundedPanel5, javax.swing.GroupLayout.PREFERRED_SIZE, 295, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 6, Short.MAX_VALUE))))
        );
        panelBayarLayout.setVerticalGroup(
            panelBayarLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelBayarLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(panelBayarLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(customRoundedPanel5, javax.swing.GroupLayout.DEFAULT_SIZE, 235, Short.MAX_VALUE)
                    .addComponent(customRoundedPanel6, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(panelKeranjang, javax.swing.GroupLayout.DEFAULT_SIZE, 78, Short.MAX_VALUE)
                .addContainerGap())
        );

        panelBarang.setBackground(new java.awt.Color(189, 195, 199));
        panelBarang.setBottomLeftRound(false);
        panelBarang.setBottomRightRound(false);
        panelBarang.setcolorEnd(new java.awt.Color(64, 64, 122));
        panelBarang.setcolorStart(new java.awt.Color(64, 64, 122));
        panelBarang.setTopLeftRound(false);
        panelBarang.setTopRightRound(false);
        panelBarang.setLayout(new java.awt.GridLayout(2, 3, 10, 10));

        txtKategori.setFont(new java.awt.Font("Yu Gothic UI Semibold", 0, 12)); // NOI18N
        txtKategori.setForeground(new java.awt.Color(255, 255, 255));
        txtKategori.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        txtKategori.setText("Pilih Kategori");
        txtKategori.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        panelBarang.add(txtKategori);

        txtPilih.setFont(new java.awt.Font("Yu Gothic UI Semibold", 0, 12)); // NOI18N
        txtPilih.setForeground(new java.awt.Color(255, 255, 255));
        txtPilih.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        txtPilih.setText("Pilih Barang");
        txtPilih.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        panelBarang.add(txtPilih);

        lblInfoStok.setFont(new java.awt.Font("Yu Gothic UI Semibold", 0, 14)); // NOI18N
        lblInfoStok.setForeground(new java.awt.Color(255, 255, 255));
        lblInfoStok.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblInfoStok.setText("Sisa Stok: 0");
        lblInfoStok.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        panelBarang.add(lblInfoStok);

        cbKategori.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        cbKategori.addActionListener(this::cbKategoriActionPerformed);
        panelBarang.add(cbKategori);

        cbPilihBarang.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        cbPilihBarang.addActionListener(this::cbPilihBarangActionPerformed);
        panelBarang.add(cbPilihBarang);

        lblInfoHarga.setFont(new java.awt.Font("Yu Gothic UI Semibold", 0, 14)); // NOI18N
        lblInfoHarga.setForeground(new java.awt.Color(255, 255, 255));
        lblInfoHarga.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblInfoHarga.setText("Harga: Rp. 0");
        lblInfoHarga.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        panelBarang.add(lblInfoHarga);

        customRoundedPanel1.setBottomLeftRound(false);
        customRoundedPanel1.setBottomRightRound(false);
        customRoundedPanel1.setcolorEnd(new java.awt.Color(64, 64, 122));
        customRoundedPanel1.setcolorStart(new java.awt.Color(64, 64, 122));
        customRoundedPanel1.setTopLeftRound(false);
        customRoundedPanel1.setTopRightRound(false);
        customRoundedPanel1.setLayout(new java.awt.GridLayout(2, 0, 5, 5));

        jLabel1.setFont(new java.awt.Font("Segoe UI Semibold", 0, 14)); // NOI18N
        jLabel1.setForeground(new java.awt.Color(255, 255, 255));
        jLabel1.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel1.setText("Qty");
        jLabel1.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        customRoundedPanel1.add(jLabel1);

        btnHapusItem.setBackground(new java.awt.Color(220, 221, 225));
        btnHapusItem.setText("Hapus Item");
        btnHapusItem.addActionListener(this::btnHapusItemActionPerformed);
        customRoundedPanel1.add(btnHapusItem);

        btnTambahKeranjang.setBackground(new java.awt.Color(220, 221, 225));
        btnTambahKeranjang.setText("Tambah Keranjang");
        btnTambahKeranjang.addActionListener(this::btnTambahKeranjangActionPerformed);
        customRoundedPanel1.add(btnTambahKeranjang);

        btnBatalKeranjang.setBackground(new java.awt.Color(220, 221, 225));
        btnBatalKeranjang.setText("Batal");
        btnBatalKeranjang.addActionListener(this::btnBatalKeranjangActionPerformed);
        customRoundedPanel1.add(btnBatalKeranjang);

        customRoundedPanel3.setBottomLeftRound(false);
        customRoundedPanel3.setBottomRightRound(false);
        customRoundedPanel3.setcolorEnd(new java.awt.Color(64, 64, 122));
        customRoundedPanel3.setcolorStart(new java.awt.Color(64, 64, 122));
        customRoundedPanel3.setTopLeftRound(false);
        customRoundedPanel3.setTopRightRound(false);

        tblKeranjang.setBackground(new java.awt.Color(220, 221, 225));
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

        javax.swing.GroupLayout customRoundedPanel3Layout = new javax.swing.GroupLayout(customRoundedPanel3);
        customRoundedPanel3.setLayout(customRoundedPanel3Layout);
        customRoundedPanel3Layout.setHorizontalGroup(
            customRoundedPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 0, Short.MAX_VALUE)
            .addGroup(customRoundedPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(customRoundedPanel3Layout.createSequentialGroup()
                    .addContainerGap()
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 500, Short.MAX_VALUE)
                    .addContainerGap()))
        );
        customRoundedPanel3Layout.setVerticalGroup(
            customRoundedPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 0, Short.MAX_VALUE)
            .addGroup(customRoundedPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(customRoundedPanel3Layout.createSequentialGroup()
                    .addContainerGap()
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 289, Short.MAX_VALUE)
                    .addContainerGap()))
        );

        customRoundedPanel4.setBottomLeftRound(false);
        customRoundedPanel4.setBottomRightRound(false);
        customRoundedPanel4.setcolorEnd(new java.awt.Color(22, 160, 133));
        customRoundedPanel4.setcolorStart(new java.awt.Color(22, 160, 133));

        customRoundedPanel7.setBottomLeftRound(false);
        customRoundedPanel7.setBottomRightRound(false);
        customRoundedPanel7.setcolorEnd(new java.awt.Color(22, 160, 133));
        customRoundedPanel7.setcolorStart(new java.awt.Color(22, 160, 133));
        customRoundedPanel7.setTopRightRound(false);
        customRoundedPanel7.setLayout(new java.awt.GridLayout(2, 0));

        lblNamaToko.setFont(new java.awt.Font("Times New Roman", 1, 14)); // NOI18N
        lblNamaToko.setForeground(new java.awt.Color(255, 255, 255));
        lblNamaToko.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblNamaToko.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        customRoundedPanel7.add(lblNamaToko);

        lblAlamatToko.setForeground(new java.awt.Color(255, 255, 255));
        lblAlamatToko.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblAlamatToko.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        customRoundedPanel7.add(lblAlamatToko);

        javax.swing.GroupLayout customRoundedPanel4Layout = new javax.swing.GroupLayout(customRoundedPanel4);
        customRoundedPanel4.setLayout(customRoundedPanel4Layout);
        customRoundedPanel4Layout.setHorizontalGroup(
            customRoundedPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(customRoundedPanel4Layout.createSequentialGroup()
                .addComponent(customRoundedPanel7, javax.swing.GroupLayout.PREFERRED_SIZE, 201, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, Short.MAX_VALUE))
        );
        customRoundedPanel4Layout.setVerticalGroup(
            customRoundedPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, customRoundedPanel4Layout.createSequentialGroup()
                .addGap(0, 22, Short.MAX_VALUE)
                .addComponent(customRoundedPanel7, javax.swing.GroupLayout.PREFERRED_SIZE, 43, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        spnQty.setFont(new java.awt.Font("Segoe UI", 0, 24)); // NOI18N
        spnQty.setModel(new javax.swing.SpinnerNumberModel(1, 1, null, 1));

        javax.swing.GroupLayout customRoundedPanel2Layout = new javax.swing.GroupLayout(customRoundedPanel2);
        customRoundedPanel2.setLayout(customRoundedPanel2Layout);
        customRoundedPanel2Layout.setHorizontalGroup(
            customRoundedPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(customRoundedPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(customRoundedPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(customRoundedPanel2Layout.createSequentialGroup()
                        .addComponent(customRoundedPanel4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addContainerGap())
                    .addGroup(customRoundedPanel2Layout.createSequentialGroup()
                        .addGroup(customRoundedPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(customRoundedPanel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(panelBarang, javax.swing.GroupLayout.DEFAULT_SIZE, 512, Short.MAX_VALUE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(customRoundedPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(customRoundedPanel2Layout.createSequentialGroup()
                                .addComponent(spnQty, javax.swing.GroupLayout.PREFERRED_SIZE, 83, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(customRoundedPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, 292, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(0, 0, Short.MAX_VALUE))
                            .addGroup(customRoundedPanel2Layout.createSequentialGroup()
                                .addComponent(panelBayar, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addContainerGap())))))
        );
        customRoundedPanel2Layout.setVerticalGroup(
            customRoundedPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, customRoundedPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(customRoundedPanel4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(customRoundedPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(panelBarang, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(spnQty, javax.swing.GroupLayout.PREFERRED_SIZE, 53, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(customRoundedPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, 63, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(customRoundedPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(customRoundedPanel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(panelBayar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(37, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(customRoundedPanel2, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(customRoundedPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
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
                String sqlTransaksi = "INSERT INTO transaksi (total_harga, diskon_persen, pajak_persen, bayar, kembalian) VALUES (?, ?, ?, ?, ?)";
                int idTransaksi;
                try (PreparedStatement pstTrans = conn.prepareStatement(sqlTransaksi, Statement.RETURN_GENERATED_KEYS)) {
                    pstTrans.setInt(1, totalBelanja);
                    pstTrans.setInt(2, nilaiDiskon);
                    pstTrans.setInt(3, nilaiPajak);
                    pstTrans.setInt(4, bayar);
                    pstTrans.setInt(5, kembalian);
                    pstTrans.executeUpdate();

                    ResultSet rsKeys = pstTrans.getGeneratedKeys();
                    if (!rsKeys.next()) {
                        throw new SQLException("Gagal membuat ID transaksi.");
                    }
                    idTransaksi = rsKeys.getInt(1);
                }

                String sqlDetail = "INSERT INTO detail_transaksi (id_transaksi, nama_barang, harga, qty, subtotal) VALUES (?, ?, ?, ?, ?)";
                String sqlUpdateStok = "UPDATE barang SET stok = stok - ? WHERE nama_barang = ? AND stok >= ?";

                try (PreparedStatement pstDetail = conn.prepareStatement(sqlDetail); PreparedStatement pstUpdateStok = conn.prepareStatement(sqlUpdateStok)) {
                    int jumlahBaris = modelKeranjang.getRowCount();
                    for (int i = 0; i < jumlahBaris; i++) {
                        String nama = modelKeranjang.getValueAt(i, 0).toString();
                        int harga = Integer.parseInt(modelKeranjang.getValueAt(i, 1).toString());
                        int qty = Integer.parseInt(modelKeranjang.getValueAt(i, 2).toString());
                        int subtotal = Integer.parseInt(modelKeranjang.getValueAt(i, 3).toString());

                        pstDetail.setInt(1, idTransaksi);
                        pstDetail.setString(2, nama);
                        pstDetail.setInt(3, harga);
                        pstDetail.setInt(4, qty);
                        pstDetail.setInt(5, subtotal);
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
    private javax.swing.JButton btnBatalKeranjang;
    private javax.swing.JButton btnHapusItem;
    private javax.swing.JButton btnSimpanTransaksi;
    private javax.swing.JButton btnTambahKeranjang;
    private javax.swing.JComboBox<String> cbJenisBayar;
    private javax.swing.JComboBox<String> cbKategori;
    private javax.swing.JComboBox<String> cbPilihBarang;
    private toko.aplikasipos.CustomRoundedPanel customRoundedPanel1;
    private toko.aplikasipos.CustomRoundedPanel customRoundedPanel2;
    private toko.aplikasipos.CustomRoundedPanel customRoundedPanel3;
    private toko.aplikasipos.CustomRoundedPanel customRoundedPanel4;
    private toko.aplikasipos.CustomRoundedPanel customRoundedPanel5;
    private toko.aplikasipos.CustomRoundedPanel customRoundedPanel6;
    private toko.aplikasipos.CustomRoundedPanel customRoundedPanel7;
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
    private toko.aplikasipos.CustomRoundedPanel panelKeranjang;
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
