package toko.aplikasipos;

import java.awt.Color;
import java.awt.Dimension;
import java.sql.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.util.logging.Logger;
import javax.print.PrintService;
import javax.print.PrintServiceLookup;

public class KasirFrame extends javax.swing.JFrame {

    private static final Logger logger = Logger.getLogger(KasirFrame.class.getName());
    private static final int COL_NAMA = 0;
    private static final int COL_BARCODE = 1;
    private static final int COL_HARGA = 2;
    private static final int COL_QTY = 3;
    private static final int COL_SUBTOTAL = 4;
    DefaultTableModel modelKeranjang;
    private JComboBox<String> cbPilihPrinter;
    private javax.print.PrintService[] availablePrinters;
    int totalBelanja = 0;
    //private int mouseX, mouseY;

    public KasirFrame() {
        initComponents();

        initTable();
        initPrinterList();
        loadProfilToko();
        loadKategoriKasir();
        loadBarang();
        syncKasirIdentity();
        setExtendedState(JFrame.MAXIMIZED_BOTH);

        this.setLocationRelativeTo(null);
        requestBarcodeFocus();
        initIcon();
        mulaiJam();

    }

    private void initPrinterList() {
        availablePrinters = javax.print.PrintServiceLookup.lookupPrintServices(null, null);
        cbPilihPrinter = new JComboBox<>();
        cbPilihPrinter.addItem("System Default");
        for (javax.print.PrintService printer : availablePrinters) {
            cbPilihPrinter.addItem(printer.getName());
        }
        
        // Add to panelBayar (or another appropriate location)
        javax.swing.JPanel p = new JPanel(new java.awt.BorderLayout());
        p.add(new JLabel("Pilih Printer: "), java.awt.BorderLayout.WEST);
        p.add(cbPilihPrinter, java.awt.BorderLayout.CENTER);
        panelBayar.add(p, java.awt.BorderLayout.SOUTH);
        panelBayar.revalidate();
    }

    private void initTable() {
        modelKeranjang = new DefaultTableModel(
                new String[]{"Nama Barang", "Barcode", "Harga", "Qty", "Subtotal"}, 0
        ) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Agar kasir tidak bisa mengetik manual di tabel
            }
        };

        tblKeranjang.setModel(modelKeranjang);
    }
    private Connection getConnection() throws SQLException {
        return DatabaseManager.getConnection();
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
            Object nama = modelKeranjang.getValueAt(i, COL_NAMA);
            if (nama != null && namaBarang.equals(nama.toString())) {
                totalQty += toInt(modelKeranjang.getValueAt(i, COL_QTY));
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
            logger.severe("Error Profil: " + e.getMessage());
        }
    }

    private void syncKasirIdentity() {
        String namaKasir = (LoginFrame.getKasirAktif() == null || LoginFrame.getKasirAktif().isBlank())
                ? "Kasir"
                : LoginFrame.getKasirAktif();
        String roleKasir = (LoginFrame.getRoleAktif() == null || LoginFrame.getRoleAktif().isBlank())
                ? "Kasir"
                : LoginFrame.getRoleAktif();
        
        txtNamaKasir.setText(namaKasir);
        txtRole.setText(roleKasir);
    }

    private void requestBarcodeFocus() {
        SwingUtilities.invokeLater(() -> txtBarcode.requestFocusInWindow());
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
            logger.severe("Error kategori: " + e.getMessage());
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
            logger.severe("Error barang: " + e.getMessage());
        }
    }

    private void prosesScanBarang(String kode) {
        String sql = "SELECT nama_barang, harga, stok, COALESCE(barcode, '') AS barcode FROM barang WHERE barcode = ? OR id_barang = ? OR nama_barang = ?";

        try (Connection conn = DatabaseManager.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, kode);
            ps.setString(2, kode);
            ps.setString(3, kode);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    updateKeranjang(
                        rs.getString("nama_barang"),
                        rs.getString("barcode"),
                        rs.getInt("harga"),
                        rs.getInt("stok"),
                        1
                    );
                } else {
                    JOptionPane.showMessageDialog(this, "Barang tidak ditemukan!", "Error", JOptionPane.WARNING_MESSAGE);
                }
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error pencarian barang: " + e.getMessage());
        }
    }

    // ================= LOGIC =================
    private void updateKeranjang(String nama, String barcode, int harga, int stok, int qtyTambah) {
        int qtyDiKeranjang = getQtyDiKeranjang(nama);
        if (qtyDiKeranjang + qtyTambah > stok) {
            JOptionPane.showMessageDialog(this, "Stok untuk " + nama + " tidak mencukupi. Stok tersedia: " + stok, "Stok Habis", JOptionPane.WARNING_MESSAGE);
            return;
        }

        boolean sudahAda = false;
        for (int i = 0; i < modelKeranjang.getRowCount(); i++) {
            if (nama.equals(String.valueOf(modelKeranjang.getValueAt(i, COL_NAMA)))) {
                int qtyLama = toInt(modelKeranjang.getValueAt(i, COL_QTY));
                int qtyBaru = qtyLama + qtyTambah;
                int subtotalBaru = harga * qtyBaru;

                modelKeranjang.setValueAt(qtyBaru, i, COL_QTY);
                modelKeranjang.setValueAt(subtotalBaru, i, COL_SUBTOTAL);
                sudahAda = true;
                break;
            }
        }

        if (!sudahAda) {
            modelKeranjang.addRow(new Object[]{nama, barcode, harga, qtyTambah, harga * qtyTambah});
        }

        hitungTotal();
    }

    private void hitungTotal() {
        int subtotal = 0;

        for (int i = 0; i < modelKeranjang.getRowCount(); i++) {
            subtotal += toInt(modelKeranjang.getValueAt(i, COL_SUBTOTAL));
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
        requestBarcodeFocus();
    }

    private boolean cetakStruk(int total, int diskon, int pajak, int bayar, int kembalian, String kasir, String jenisBayar) {

        // 1. AMBIL PROFIL TOKO
        String namaToko = "NAMA TOKO";
        String alamatToko = "Alamat Toko";

        try (Connection conn = DatabaseManager.getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery("SELECT nama_toko, alamat FROM profil_toko LIMIT 1")) {

            if (rs.next()) {
                namaToko = rs.getString("nama_toko");
                alamatToko = rs.getString("alamat");
            }

        } catch (Exception e) {
            logger.severe("Error load profil toko: " + e.getMessage());
        }

        // 2) SUSUN STRUK (loop jika printer tidak ditemukan)
        while (true) {
            StringBuilder struk = new StringBuilder();
            int lebarStruk = 32;

            struk.append("================================\n");
            struk.append(centerText(namaToko, lebarStruk)).append("\n");
            struk.append(centerText(alamatToko, lebarStruk)).append("\n");

            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd-MM-yyyy HH:mm");
            String tanggal = sdf.format(new java.util.Date());

            struk.append("================================\n");
            struk.append("Kasir : ").append(kasir).append("\n");
            struk.append("Tgl   : ").append(tanggal).append("\n");
            struk.append("--------------------------------\n");

            // LOOP KERANJANG
            for (int i = 0; i < modelKeranjang.getRowCount(); i++) {
                String nama = String.valueOf(modelKeranjang.getValueAt(i, COL_NAMA));
                String harga = String.valueOf(modelKeranjang.getValueAt(i, COL_HARGA));
                String qty = String.valueOf(modelKeranjang.getValueAt(i, COL_QTY));
                String sub = String.valueOf(modelKeranjang.getValueAt(i, COL_SUBTOTAL));

                struk.append(nama).append("\n");
                struk.append(qty).append(" x Rp ").append(harga)
                        .append("        Rp ").append(sub).append("\n");
            }

            struk.append("--------------------------------\n");
            struk.append("Diskon      : ").append(diskon).append("%\n");
            struk.append("Pajak (PPN) : ").append(pajak).append("%\n");
            struk.append("Total Akhir : Rp ").append(total).append("\n");
            struk.append("Metode Bayar: ").append(jenisBayar).append("\n");
            struk.append("Bayar       : Rp ").append(bayar).append("\n");
            struk.append("Kembalian   : Rp ").append(kembalian).append("\n");
            struk.append("================================\n");
            struk.append(" TERIMA KASIH ATAS KUNJUNGANNYA   \n");
            struk.append("================================\n");

            // 3) TAMPILKAN + OPSI
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
                    PrintService[] services = PrintServiceLookup.lookupPrintServices(null, null);
                    if (services == null || services.length == 0) {
                        JOptionPane.showMessageDialog(this,
                                "Printer tidak ditemukan.\nSilakan pasang/set default printer dulu, atau pilih Simpan ke PDF.",
                                "Printer Tidak Tersedia",
                                JOptionPane.WARNING_MESSAGE);
                        // Kembali ke dialog awal
                        continue;
                    }

                    // 1) Coba cetak via ESC/POS jika tersedia
                    boolean escPrinted = false;
                    try {
                        byte[] escBytes = toko.aplikasipos.EscPosPrinter.toReceiptBytes(struk.toString());
                        escPrinted = toko.aplikasipos.EscPosPrinter.printBytes(escBytes);
                    } catch (Exception ex) {
                        escPrinted = false;
                    }
                    if (escPrinted) {
                        return true;
                    }

                    // 2) Fall back ke print TextArea
                    boolean printed = txtArea.print(null, null, true, null, null, true);
                    if (!printed) {
                        JOptionPane.showMessageDialog(this,
                                "Cetak dibatalkan atau gagal dikirim ke printer.",
                                "Cetak Tidak Berhasil",
                                JOptionPane.WARNING_MESSAGE);
                        // Kembali ke dialog awal
                        continue;
                    }
                    // Cetak berhasil
                    return true;
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(this, "Gagal print: " + e.getMessage());
                    // Kembali ke dialog awal
                    continue;
                }
            } else if (pilihan == 1) {
                String timeStamp = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss").format(new java.util.Date());
                simpanKePDF(struk.toString(), "Struk_" + timeStamp);
                return true;
            } else {
                // Tutup
                return false;
            }
        } // end while
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

    private void initIcon() {
        AppUtil.setWindowIcon(this);
        AppUtil.setButtonIcon(btnBatalKeranjang, "/icon/shopping_cart_off.png");
        AppUtil.setButtonIcon(btnHapusItem, "/icon/delete.png");
        AppUtil.setButtonIcon(btnTambahKeranjang, "/icon/add_shopping.png");
        AppUtil.setButtonIcon(btnSimpanTransaksi, "/icon/payments.png");
        AppUtil.setLabelIcon(putramas, "/icon/fb.png");
        AppUtil.setButtonIcon(btnScanBarcode, "/icon/barcode.png");
    }

    private void mulaiJam() {
        // Format waktu (Contoh hasil: 14:30:05  |  23-04-2026)
        java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss  |  dd-MM-yyyy");

        // Timer Swing untuk memperbarui waktu setiap 1000 milidetik (1 detik)
        javax.swing.Timer timer = new javax.swing.Timer(1000, e -> {
            // Ambil waktu komputer saat ini
            String waktuSekarang = java.time.LocalDateTime.now().format(formatter);

            // Pasang ke JLabel
            lblJam.setText(waktuSekarang);
        });

        // Pasang waktu langsung saat aplikasi baru dibuka (agar tidak ada delay kosong 1 detik)
        lblJam.setText(java.time.LocalDateTime.now().format(formatter));

        // Jalankan mesin jamnya!
        timer.start();
    }

    private void prosesScanBarang(String barcode, int qtyInput) {
        String sql = "SELECT nama_barang, harga, stok FROM barang WHERE barcode = ?";

        try (Connection conn = DatabaseManager.getConnection(); PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setString(1, barcode);
            ResultSet rs = pst.executeQuery();

            if (!rs.next()) {
                JOptionPane.showMessageDialog(this, "Barang tidak ditemukan!");
                return;
            }

            updateKeranjang(
                rs.getString("nama_barang"),
                barcode,
                rs.getInt("harga"),
                rs.getInt("stok"),
                qtyInput
            );

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        PanelUtamaTransaksi = new toko.aplikasipos.CustomRoundedPanel();
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
        txtBarcode = new javax.swing.JTextField();
        lblInfoHarga = new javax.swing.JLabel();
        lblInfoStok = new javax.swing.JLabel();
        btnTambahKeranjang = new javax.swing.JButton();
        btnScanBarcode = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        tblKeranjang = new javax.swing.JTable();
        panelBayar = new toko.aplikasipos.CustomRoundedPanel();
        PanelProfil = new toko.aplikasipos.CustomRoundedPanel();
        lblNamaToko = new javax.swing.JLabel();
        lblAlamatToko = new javax.swing.JLabel();
        txtRole = new javax.swing.JLabel();
        txtNamaKasir = new javax.swing.JLabel();
        customRoundedPanel2 = new toko.aplikasipos.CustomRoundedPanel();
        btnSimpanTransaksi = new javax.swing.JButton();
        PanelDuit = new toko.aplikasipos.CustomRoundedPanel();
        txtJenisBayar = new javax.swing.JLabel();
        cbJenisBayar = new javax.swing.JComboBox<>();
        txtBayar1 = new javax.swing.JLabel();
        txtBayar = new javax.swing.JTextField();
        txtDiscount = new javax.swing.JLabel();
        spnDiskon = new javax.swing.JSpinner();
        txtPPN = new javax.swing.JLabel();
        spnPajak = new javax.swing.JSpinner();
        txtTotal = new javax.swing.JLabel();
        lblTotalHarga = new javax.swing.JLabel();
        txtKembalian = new javax.swing.JLabel();
        lblKembalian = new javax.swing.JLabel();
        customRoundedPanel1 = new toko.aplikasipos.CustomRoundedPanel();
        putramas = new javax.swing.JLabel();
        lblJam = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setIconImages(null);

        PanelUtamaTransaksi.setBottomLeftRound(false);
        PanelUtamaTransaksi.setBottomRightRound(false);
        PanelUtamaTransaksi.setcolorEnd(new java.awt.Color(72, 126, 176));
        PanelUtamaTransaksi.setcolorStart(new java.awt.Color(72, 126, 176));
        PanelUtamaTransaksi.setMinimumSize(new java.awt.Dimension(1120, 640));
        PanelUtamaTransaksi.setPreferredSize(new java.awt.Dimension(1120, 640));
        PanelUtamaTransaksi.setTopLeftRound(false);
        PanelUtamaTransaksi.setTopRightRound(false);
        PanelUtamaTransaksi.setLayout(new java.awt.BorderLayout(5, 5));

        PanelTransaksi.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Transaksi Penjualan", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Segoe UI Black", 1, 18), new java.awt.Color(255, 255, 255))); // NOI18N
        PanelTransaksi.setBottomLeftRound(false);
        PanelTransaksi.setBottomRightRound(false);
        PanelTransaksi.setcolorEnd(new java.awt.Color(64, 64, 122));
        PanelTransaksi.setcolorStart(new java.awt.Color(64, 64, 122));
        PanelTransaksi.setMinimumSize(new java.awt.Dimension(656, 525));
        PanelTransaksi.setPreferredSize(new java.awt.Dimension(656, 525));
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
        panelBarang.setLayout(new java.awt.GridLayout(0, 4, 10, 10));

        txtKategori.setFont(new java.awt.Font("Segoe UI Semibold", 0, 14)); // NOI18N
        txtKategori.setForeground(new java.awt.Color(255, 255, 255));
        txtKategori.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        txtKategori.setText("Pilih Kategori");
        txtKategori.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        panelBarang.add(txtKategori);

        txtPilih.setFont(new java.awt.Font("Segoe UI Semibold", 0, 14)); // NOI18N
        txtPilih.setForeground(new java.awt.Color(255, 255, 255));
        txtPilih.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        txtPilih.setText("Pilih Barang");
        txtPilih.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        panelBarang.add(txtPilih);

        Qty.setFont(new java.awt.Font("Segoe UI Semibold", 0, 14)); // NOI18N
        Qty.setForeground(new java.awt.Color(255, 255, 255));
        Qty.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        Qty.setText("Qty");
        Qty.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        panelBarang.add(Qty);

        btnBatalKeranjang.setBackground(new java.awt.Color(255, 82, 82));
        btnBatalKeranjang.setFont(new java.awt.Font("Segoe UI Semibold", 0, 12)); // NOI18N
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
        btnHapusItem.setText("HAPUS ITEM");
        btnHapusItem.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        btnHapusItem.addActionListener(this::btnHapusItemActionPerformed);
        panelBarang.add(btnHapusItem);

        txtBarcode.addActionListener(this::txtBarcodeActionPerformed);
        panelBarang.add(txtBarcode);

        lblInfoHarga.setFont(new java.awt.Font("Segoe UI Semibold", 0, 14)); // NOI18N
        lblInfoHarga.setForeground(new java.awt.Color(255, 255, 255));
        lblInfoHarga.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblInfoHarga.setText("Rp. 0");
        lblInfoHarga.setBorder(new javax.swing.border.LineBorder(new java.awt.Color(255, 255, 255), 2, true));
        lblInfoHarga.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        panelBarang.add(lblInfoHarga);

        lblInfoStok.setFont(new java.awt.Font("Segoe UI Semibold", 0, 14)); // NOI18N
        lblInfoStok.setForeground(new java.awt.Color(255, 255, 255));
        lblInfoStok.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblInfoStok.setText("Stok: 0");
        lblInfoStok.setBorder(new javax.swing.border.LineBorder(new java.awt.Color(255, 255, 255), 2, true));
        lblInfoStok.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        panelBarang.add(lblInfoStok);

        btnTambahKeranjang.setBackground(new java.awt.Color(68, 189, 50));
        btnTambahKeranjang.setFont(new java.awt.Font("Segoe UI Semibold", 0, 12)); // NOI18N
        btnTambahKeranjang.setText("TAMBAH");
        btnTambahKeranjang.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        btnTambahKeranjang.addActionListener(this::btnTambahKeranjangActionPerformed);
        panelBarang.add(btnTambahKeranjang);

        btnScanBarcode.setBackground(new java.awt.Color(109, 33, 79));
        btnScanBarcode.setText("Scan Barcode");
        btnScanBarcode.addActionListener(this::btnScanBarcodeActionPerformed);
        panelBarang.add(btnScanBarcode);

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
                .addGroup(PanelTransaksiLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(panelBarang, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jScrollPane1))
                .addContainerGap())
        );
        PanelTransaksiLayout.setVerticalGroup(
            PanelTransaksiLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(PanelTransaksiLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(panelBarang, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 336, Short.MAX_VALUE)
                .addContainerGap())
        );

        PanelUtamaTransaksi.add(PanelTransaksi, java.awt.BorderLayout.CENTER);

        panelBayar.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Pembayaran", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Segoe UI Black", 1, 18), new java.awt.Color(255, 255, 255))); // NOI18N
        panelBayar.setBottomLeftRound(false);
        panelBayar.setBottomRightRound(false);
        panelBayar.setcolorEnd(new java.awt.Color(64, 64, 122));
        panelBayar.setcolorStart(new java.awt.Color(64, 64, 122));
        panelBayar.setMinimumSize(new java.awt.Dimension(400, 300));
        panelBayar.setPreferredSize(new java.awt.Dimension(400, 300));
        panelBayar.setTopLeftRound(false);
        panelBayar.setTopRightRound(false);
        panelBayar.setLayout(new java.awt.BorderLayout(10, 10));

        PanelProfil.setBorder(javax.swing.BorderFactory.createEmptyBorder(10, 10, 10, 10));
        PanelProfil.setBottomLeftRound(false);
        PanelProfil.setBottomRightRound(false);
        PanelProfil.setcolorEnd(new java.awt.Color(64, 64, 122));
        PanelProfil.setcolorStart(new java.awt.Color(27, 20, 100));
        PanelProfil.setTopLeftRound(false);
        PanelProfil.setTopRightRound(false);

        lblNamaToko.setFont(new java.awt.Font("Times New Roman", 1, 18)); // NOI18N
        lblNamaToko.setForeground(new java.awt.Color(255, 255, 255));
        lblNamaToko.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblNamaToko.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);

        lblAlamatToko.setFont(new java.awt.Font("Times New Roman", 0, 12)); // NOI18N
        lblAlamatToko.setForeground(new java.awt.Color(255, 255, 255));
        lblAlamatToko.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblAlamatToko.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);

        txtRole.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        txtRole.setHorizontalTextPosition(javax.swing.SwingConstants.LEFT);

        txtNamaKasir.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        txtNamaKasir.setHorizontalTextPosition(javax.swing.SwingConstants.LEFT);

        javax.swing.GroupLayout PanelProfilLayout = new javax.swing.GroupLayout(PanelProfil);
        PanelProfil.setLayout(PanelProfilLayout);
        PanelProfilLayout.setHorizontalGroup(
            PanelProfilLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(lblNamaToko, javax.swing.GroupLayout.PREFERRED_SIZE, 358, javax.swing.GroupLayout.PREFERRED_SIZE)
            .addComponent(lblAlamatToko, javax.swing.GroupLayout.PREFERRED_SIZE, 358, javax.swing.GroupLayout.PREFERRED_SIZE)
            .addComponent(txtRole, javax.swing.GroupLayout.PREFERRED_SIZE, 358, javax.swing.GroupLayout.PREFERRED_SIZE)
            .addComponent(txtNamaKasir, javax.swing.GroupLayout.PREFERRED_SIZE, 358, javax.swing.GroupLayout.PREFERRED_SIZE)
        );
        PanelProfilLayout.setVerticalGroup(
            PanelProfilLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(PanelProfilLayout.createSequentialGroup()
                .addComponent(lblNamaToko, javax.swing.GroupLayout.PREFERRED_SIZE, 19, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, 0)
                .addComponent(lblAlamatToko, javax.swing.GroupLayout.PREFERRED_SIZE, 19, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, 0)
                .addComponent(txtRole, javax.swing.GroupLayout.PREFERRED_SIZE, 19, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, 0)
                .addComponent(txtNamaKasir, javax.swing.GroupLayout.PREFERRED_SIZE, 19, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        panelBayar.add(PanelProfil, java.awt.BorderLayout.NORTH);

        customRoundedPanel2.setBottomLeftRound(false);
        customRoundedPanel2.setBottomRightRound(false);
        customRoundedPanel2.setcolorEnd(new java.awt.Color(27, 20, 100));
        customRoundedPanel2.setcolorStart(new java.awt.Color(64, 64, 122));
        customRoundedPanel2.setPreferredSize(new java.awt.Dimension(390, 50));
        customRoundedPanel2.setTopLeftRound(false);
        customRoundedPanel2.setTopRightRound(false);
        customRoundedPanel2.setLayout(new java.awt.CardLayout());

        btnSimpanTransaksi.setBackground(new java.awt.Color(5, 196, 107));
        btnSimpanTransaksi.setFont(new java.awt.Font("Segoe UI Black", 1, 18)); // NOI18N
        btnSimpanTransaksi.setForeground(new java.awt.Color(255, 255, 255));
        btnSimpanTransaksi.setText("BAYAR");
        btnSimpanTransaksi.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        btnSimpanTransaksi.addActionListener(this::btnSimpanTransaksiActionPerformed);
        customRoundedPanel2.add(btnSimpanTransaksi, "card2");

        panelBayar.add(customRoundedPanel2, java.awt.BorderLayout.PAGE_END);

        PanelDuit.setBorder(javax.swing.BorderFactory.createEmptyBorder(10, 10, 10, 10));
        PanelDuit.setBottomLeftRound(false);
        PanelDuit.setBottomRightRound(false);
        PanelDuit.setcolorEnd(new java.awt.Color(64, 64, 122));
        PanelDuit.setcolorStart(new java.awt.Color(64, 64, 122));
        PanelDuit.setPreferredSize(new java.awt.Dimension(400, 300));
        PanelDuit.setTopLeftRound(false);
        PanelDuit.setTopRightRound(false);
        PanelDuit.setLayout(new java.awt.GridLayout(12, 0));

        txtJenisBayar.setFont(new java.awt.Font("Segoe UI Semibold", 0, 14)); // NOI18N
        txtJenisBayar.setForeground(new java.awt.Color(255, 255, 255));
        txtJenisBayar.setText("Jenis Bayar");
        txtJenisBayar.setVerticalAlignment(javax.swing.SwingConstants.BOTTOM);
        txtJenisBayar.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        txtJenisBayar.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        PanelDuit.add(txtJenisBayar);

        cbJenisBayar.setFont(new java.awt.Font("Segoe UI Semibold", 0, 14)); // NOI18N
        cbJenisBayar.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Tunai", "Debit", "Qris" }));
        PanelDuit.add(cbJenisBayar);

        txtBayar1.setFont(new java.awt.Font("Segoe UI Semibold", 0, 14)); // NOI18N
        txtBayar1.setForeground(new java.awt.Color(255, 255, 255));
        txtBayar1.setText("Bayar");
        txtBayar1.setVerticalAlignment(javax.swing.SwingConstants.BOTTOM);
        txtBayar1.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        txtBayar1.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        PanelDuit.add(txtBayar1);

        txtBayar.setFont(new java.awt.Font("Segoe UI Semibold", 0, 14)); // NOI18N
        txtBayar.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                txtBayarKeyReleased(evt);
            }
        });
        PanelDuit.add(txtBayar);

        txtDiscount.setFont(new java.awt.Font("Segoe UI Semibold", 0, 14)); // NOI18N
        txtDiscount.setForeground(new java.awt.Color(255, 255, 255));
        txtDiscount.setText("Diskon");
        txtDiscount.setVerticalAlignment(javax.swing.SwingConstants.BOTTOM);
        txtDiscount.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        txtDiscount.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        PanelDuit.add(txtDiscount);

        spnDiskon.setFont(new java.awt.Font("Segoe UI Semibold", 0, 14)); // NOI18N
        spnDiskon.setModel(new javax.swing.SpinnerNumberModel(0, 0, 100, 1));
        spnDiskon.addChangeListener(this::spnDiskonStateChanged);
        PanelDuit.add(spnDiskon);

        txtPPN.setFont(new java.awt.Font("Segoe UI Semibold", 0, 14)); // NOI18N
        txtPPN.setForeground(new java.awt.Color(255, 255, 255));
        txtPPN.setText("Pajak");
        txtPPN.setVerticalAlignment(javax.swing.SwingConstants.BOTTOM);
        txtPPN.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        txtPPN.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        PanelDuit.add(txtPPN);

        spnPajak.setFont(new java.awt.Font("Segoe UI Semibold", 0, 14)); // NOI18N
        spnPajak.setModel(new javax.swing.SpinnerNumberModel(0, 0, 100, 1));
        spnPajak.addChangeListener(this::spnPajakStateChanged);
        PanelDuit.add(spnPajak);

        txtTotal.setFont(new java.awt.Font("Segoe UI Semibold", 0, 14)); // NOI18N
        txtTotal.setForeground(new java.awt.Color(255, 255, 255));
        txtTotal.setText("Total Akhir");
        txtTotal.setVerticalAlignment(javax.swing.SwingConstants.BOTTOM);
        txtTotal.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        txtTotal.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        PanelDuit.add(txtTotal);

        lblTotalHarga.setFont(new java.awt.Font("SimSun-ExtB", 1, 24)); // NOI18N
        lblTotalHarga.setForeground(new java.awt.Color(255, 255, 255));
        lblTotalHarga.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblTotalHarga.setText("Rp : ");
        lblTotalHarga.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        PanelDuit.add(lblTotalHarga);

        txtKembalian.setFont(new java.awt.Font("Segoe UI Semibold", 0, 14)); // NOI18N
        txtKembalian.setForeground(new java.awt.Color(255, 255, 255));
        txtKembalian.setText("Kembalian");
        txtKembalian.setVerticalAlignment(javax.swing.SwingConstants.BOTTOM);
        txtKembalian.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        txtKembalian.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        PanelDuit.add(txtKembalian);

        lblKembalian.setFont(new java.awt.Font("SimSun-ExtB", 1, 24)); // NOI18N
        lblKembalian.setForeground(new java.awt.Color(255, 255, 255));
        lblKembalian.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblKembalian.setText("Rp : ");
        lblKembalian.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        PanelDuit.add(lblKembalian);

        panelBayar.add(PanelDuit, java.awt.BorderLayout.EAST);

        PanelUtamaTransaksi.add(panelBayar, java.awt.BorderLayout.EAST);

        customRoundedPanel1.setBottomLeftRound(false);
        customRoundedPanel1.setBottomRightRound(false);
        customRoundedPanel1.setcolorEnd(new java.awt.Color(27, 20, 100));
        customRoundedPanel1.setcolorStart(new java.awt.Color(64, 64, 122));
        customRoundedPanel1.setPreferredSize(new java.awt.Dimension(1111, 35));
        customRoundedPanel1.setTopLeftRound(false);
        customRoundedPanel1.setTopRightRound(false);

        putramas.setFont(new java.awt.Font("Times New Roman", 1, 18)); // NOI18N
        putramas.setForeground(new java.awt.Color(153, 153, 153));
        putramas.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        putramas.setText("sanFK POS");
        putramas.setHorizontalTextPosition(javax.swing.SwingConstants.LEADING);
        putramas.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                putramasMouseClicked(evt);
            }
        });

        lblJam.setFont(new java.awt.Font("Times New Roman", 0, 18)); // NOI18N
        lblJam.setForeground(new java.awt.Color(153, 153, 153));
        lblJam.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        lblJam.setText("00:00:00");
        lblJam.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);

        javax.swing.GroupLayout customRoundedPanel1Layout = new javax.swing.GroupLayout(customRoundedPanel1);
        customRoundedPanel1.setLayout(customRoundedPanel1Layout);
        customRoundedPanel1Layout.setHorizontalGroup(
            customRoundedPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, customRoundedPanel1Layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(lblJam, javax.swing.GroupLayout.PREFERRED_SIZE, 280, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
            .addGroup(customRoundedPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(customRoundedPanel1Layout.createSequentialGroup()
                    .addGap(0, 0, Short.MAX_VALUE)
                    .addComponent(putramas)
                    .addGap(0, 0, Short.MAX_VALUE)))
        );
        customRoundedPanel1Layout.setVerticalGroup(
            customRoundedPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(customRoundedPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(lblJam)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
            .addGroup(customRoundedPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(customRoundedPanel1Layout.createSequentialGroup()
                    .addGap(0, 0, Short.MAX_VALUE)
                    .addComponent(putramas)
                    .addGap(0, 0, Short.MAX_VALUE)))
        );

        PanelUtamaTransaksi.add(customRoundedPanel1, java.awt.BorderLayout.SOUTH);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(PanelUtamaTransaksi, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(PanelUtamaTransaksi, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
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

        String sql = "SELECT harga, stok, COALESCE(barcode, '') AS barcode FROM barang WHERE nama_barang=?";

        try (Connection c = getConnection(); PreparedStatement p = c.prepareStatement(sql)) {
            p.setString(1, nama);
            ResultSet r = p.executeQuery();

            if (r.next()) {
                updateKeranjang(
                    nama, 
                    r.getString("barcode"), 
                    r.getInt("harga"), 
                    r.getInt("stok"), 
                    qty
                );
                spnQty.setValue(1);
                requestBarcodeFocus();
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
        String namaKasir = LoginFrame.getKasirAktif();
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

                try (PreparedStatement pstDetail = conn.prepareStatement(sqlDetail); PreparedStatement pstUpdateStok = conn.prepareStatement(sqlUpdateStok); PreparedStatement pstModal = conn.prepareStatement(sqlModal)) {
                    int jumlahBaris = modelKeranjang.getRowCount();
                    for (int i = 0; i < jumlahBaris; i++) {
                        String nama = String.valueOf(modelKeranjang.getValueAt(i, COL_NAMA));
                        int harga = Integer.parseInt(String.valueOf(modelKeranjang.getValueAt(i, COL_HARGA)));
                        int qty = Integer.parseInt(String.valueOf(modelKeranjang.getValueAt(i, COL_QTY)));
                        int subtotal = Integer.parseInt(String.valueOf(modelKeranjang.getValueAt(i, COL_SUBTOTAL)));
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
                boolean shouldReset = cetakStruk(totalBelanja, nilaiDiskon, nilaiPajak, bayar, kembalian, namaKasir, jenisBayar);
                if (shouldReset) {
                    resetKasir();
                }
            } catch (Exception e) {
                conn.rollback();
                throw e;
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error Transaksi: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }//GEN-LAST:event_btnSimpanTransaksiActionPerformed

    private void cbPilihBarangActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cbPilihBarangActionPerformed
        if (cbPilihBarang.getSelectedItem() == null) {
            lblInfoHarga.setText("Rp. 0");
            lblInfoStok.setText("Stok: 0");
            return;
        }

        String namaBarang = cbPilihBarang.getSelectedItem().toString();

        try (Connection conn = DatabaseManager.getConnection(); PreparedStatement pstmt = conn.prepareStatement("SELECT harga, stok, COALESCE(barcode, '') AS barcode FROM barang WHERE nama_barang = ?")) {
            pstmt.setString(1, namaBarang);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                int harga = rs.getInt("harga");
                int stok = rs.getInt("stok");

                lblInfoHarga.setText("Rp. " + harga);
                lblInfoStok.setText("Stok: " + stok);
            }
        } catch (Exception e) {
            logger.severe("Error Cek Info Barang: " + e.getMessage());
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
        requestBarcodeFocus();
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
        requestBarcodeFocus();
    }//GEN-LAST:event_btnBatalKeranjangActionPerformed

    private void txtBarcodeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtBarcodeActionPerformed
        String kodeScan = txtBarcode.getText().trim();

        if (!kodeScan.isEmpty()) {
            prosesScanBarang(kodeScan); // <--- Di sini fungsi itu dipanggil
            txtBarcode.setText("");
            txtBarcode.requestFocus();
        }
    }//GEN-LAST:event_txtBarcodeActionPerformed

    private void putramasMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_putramasMouseClicked
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
    }//GEN-LAST:event_putramasMouseClicked

    private void btnScanBarcodeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnScanBarcodeActionPerformed
        KameraScanner[] dialogRef = new KameraScanner[1];
        dialogRef[0] = new KameraScanner(this, barcode -> {
            // Use invokeLater because the callback comes from the scanner thread
            javax.swing.SwingUtilities.invokeLater(() -> {
                if (dialogRef[0] != null) dialogRef[0].setPauseScanning(true);
                
                String input = JOptionPane.showInputDialog(this, "Masukkan Qty:");
                if (input == null) {
                    if (dialogRef[0] != null) dialogRef[0].setPauseScanning(false);
                    return;
                }

                try {
                    int qty = Integer.parseInt(input);
                    if (qty <= 0) {
                        JOptionPane.showMessageDialog(this, "Qty harus > 0!");
                    } else {
                        prosesScanBarang(barcode, qty);
                    }
                } catch (NumberFormatException e) {
                    JOptionPane.showMessageDialog(this, "Qty harus angka!");
                }
                
                if (dialogRef[0] != null) dialogRef[0].setPauseScanning(false);
            });
        });
        dialogRef[0].setVisible(true);
    }//GEN-LAST:event_btnScanBarcodeActionPerformed

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
        System.setProperty("java.awt.headless", "false");
        /* Create and display the form */
        java.awt.EventQueue.invokeLater(() -> new KasirFrame().setVisible(true));
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private toko.aplikasipos.CustomRoundedPanel PanelDuit;
    private toko.aplikasipos.CustomRoundedPanel PanelProfil;
    private toko.aplikasipos.CustomRoundedPanel PanelTransaksi;
    private toko.aplikasipos.CustomRoundedPanel PanelUtamaTransaksi;
    private javax.swing.JLabel Qty;
    private javax.swing.JButton btnBatalKeranjang;
    private javax.swing.JButton btnHapusItem;
    private javax.swing.JButton btnScanBarcode;
    private javax.swing.JButton btnSimpanTransaksi;
    private javax.swing.JButton btnTambahKeranjang;
    private javax.swing.JComboBox<String> cbJenisBayar;
    private javax.swing.JComboBox<String> cbKategori;
    private javax.swing.JComboBox<String> cbPilihBarang;
    private toko.aplikasipos.CustomRoundedPanel customRoundedPanel1;
    private toko.aplikasipos.CustomRoundedPanel customRoundedPanel2;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JLabel lblAlamatToko;
    private javax.swing.JLabel lblInfoHarga;
    private javax.swing.JLabel lblInfoStok;
    private javax.swing.JLabel lblJam;
    private javax.swing.JLabel lblKembalian;
    private javax.swing.JLabel lblNamaToko;
    private javax.swing.JLabel lblTotalHarga;
    private toko.aplikasipos.CustomRoundedPanel panelBarang;
    private toko.aplikasipos.CustomRoundedPanel panelBayar;
    private javax.swing.JLabel putramas;
    private javax.swing.JSpinner spnDiskon;
    private javax.swing.JSpinner spnPajak;
    private javax.swing.JSpinner spnQty;
    private javax.swing.JTable tblKeranjang;
    private javax.swing.JTextField txtBarcode;
    private javax.swing.JTextField txtBayar;
    private javax.swing.JLabel txtBayar1;
    private javax.swing.JLabel txtDiscount;
    private javax.swing.JLabel txtJenisBayar;
    private javax.swing.JLabel txtKategori;
    private javax.swing.JLabel txtKembalian;
    private javax.swing.JLabel txtNamaKasir;
    private javax.swing.JLabel txtPPN;
    private javax.swing.JLabel txtPilih;
    private javax.swing.JLabel txtRole;
    private javax.swing.JLabel txtTotal;
    // End of variables declaration//GEN-END:variables
}
