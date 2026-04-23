package toko.aplikasipos;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;

public class TransactionHistoryFrame extends JFrame {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;

    private final JTextField txtAwal = new JTextField(10);
    private final JTextField txtAkhir = new JTextField(10);
    private final DefaultTableModel modelTransaksi = new DefaultTableModel(
            new String[]{"ID", "Tanggal", "Total", "Status", "Kasir", "Catatan"}, 0
    ) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final DefaultTableModel modelDetail = new DefaultTableModel(
            new String[]{"ID Detail", "Nama Barang", "Harga", "Qty", "Subtotal"}, 0
    ) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable tblTransaksi = new JTable(modelTransaksi);
    private final JTable tblDetail = new JTable(modelDetail);

    public TransactionHistoryFrame() {
        setTitle("Riwayat Transaksi / Void / Retur");
        setSize(980, 620);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        initUi();
        setDefaultTanggal();
        loadTransaksi();
    }

    private void initUi() {
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton btnMuat = new JButton("Muat");
        JButton btnVoid = new JButton("Void Transaksi");
        JButton btnRetur = new JButton("Retur Item");

        top.add(new JLabel("Tanggal Awal (YYYY-MM-DD):"));
        top.add(txtAwal);
        top.add(new JLabel("Tanggal Akhir (YYYY-MM-DD):"));
        top.add(txtAkhir);
        top.add(btnMuat);
        top.add(btnVoid);
        top.add(btnRetur);

        JPanel center = new JPanel(new GridLayout(2, 1, 8, 8));
        center.add(new JScrollPane(tblTransaksi));
        center.add(new JScrollPane(tblDetail));

        setLayout(new BorderLayout(8, 8));
        add(top, BorderLayout.NORTH);
        add(center, BorderLayout.CENTER);

        btnMuat.addActionListener(e -> loadTransaksi());
        btnVoid.addActionListener(e -> voidTransaksi());
        btnRetur.addActionListener(e -> returItem());
        tblTransaksi.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                loadDetailBySelectedTransaksi();
            }
        });
    }

    private void setDefaultTanggal() {
        LocalDate today = LocalDate.now();
        txtAwal.setText(today.withDayOfMonth(1).format(DATE_FORMAT));
        txtAkhir.setText(today.format(DATE_FORMAT));
    }

    private void loadTransaksi() {
        LocalDate awal;
        LocalDate akhir;
        try {
            awal = LocalDate.parse(txtAwal.getText().trim(), DATE_FORMAT);
            akhir = LocalDate.parse(txtAkhir.getText().trim(), DATE_FORMAT);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Format tanggal wajib YYYY-MM-DD.");
            return;
        }

        modelTransaksi.setRowCount(0);
        modelDetail.setRowCount(0);
        String sql = """
                SELECT id_transaksi, tanggal, total_harga,
                       COALESCE(status_transaksi, 'NORMAL') AS status_transaksi,
                       COALESCE(updated_by, '-') AS kasir,
                       COALESCE(catatan_status, '-') AS catatan
                FROM transaksi
                WHERE DATE(tanggal) BETWEEN ? AND ?
                ORDER BY id_transaksi DESC
                """;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, awal.toString());
            ps.setString(2, akhir.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    modelTransaksi.addRow(new Object[]{
                        rs.getInt("id_transaksi"),
                        rs.getString("tanggal"),
                        rs.getInt("total_harga"),
                        rs.getString("status_transaksi"),
                        rs.getString("kasir"),
                        rs.getString("catatan")
                    });
                }
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Gagal load riwayat: " + e.getMessage());
        }
    }

    private void loadDetailBySelectedTransaksi() {
        int row = tblTransaksi.getSelectedRow();
        modelDetail.setRowCount(0);
        if (row == -1) {
            return;
        }
        int idTransaksi = Integer.parseInt(String.valueOf(modelTransaksi.getValueAt(row, 0)));
        String sql = "SELECT id_detail, nama_barang, harga, qty, subtotal FROM detail_transaksi WHERE id_transaksi = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idTransaksi);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    modelDetail.addRow(new Object[]{
                        rs.getInt("id_detail"),
                        rs.getString("nama_barang"),
                        rs.getInt("harga"),
                        rs.getInt("qty"),
                        rs.getInt("subtotal")
                    });
                }
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Gagal load detail: " + e.getMessage());
        }
    }

    private boolean authorizeAdminAction() {
        if ("Admin".equalsIgnoreCase(LoginFrame.roleAktif)) {
            return true;
        }

        JTextField txtUser = new JTextField();
        JTextField txtPass = new JTextField();
        Object[] form = {
            "Aksi ini butuh otorisasi Admin.",
            "Username Admin:", txtUser,
            "Password Admin:", txtPass
        };
        int ok = JOptionPane.showConfirmDialog(this, form, "Otorisasi Admin", JOptionPane.OK_CANCEL_OPTION);
        if (ok != JOptionPane.OK_OPTION) {
            return false;
        }

        String sql = "SELECT password FROM users WHERE username = ? AND role = 'Admin'";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, txtUser.getText().trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return PasswordUtil.verify(txtPass.getText(), rs.getString("password"));
                }
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Gagal validasi admin: " + e.getMessage());
        }
        JOptionPane.showMessageDialog(this, "Otorisasi admin gagal.");
        return false;
    }

    private void voidTransaksi() {
        int row = tblTransaksi.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Pilih transaksi dulu.");
            return;
        }
        if (!authorizeAdminAction()) {
            return;
        }

        int idTransaksi = Integer.parseInt(String.valueOf(modelTransaksi.getValueAt(row, 0)));
        String status = String.valueOf(modelTransaksi.getValueAt(row, 3));
        if (!"NORMAL".equalsIgnoreCase(status) && !"RETURN_PARTIAL".equalsIgnoreCase(status)) {
            JOptionPane.showMessageDialog(this, "Transaksi ini tidak bisa di-void karena status: " + status);
            return;
        }

        String alasan = JOptionPane.showInputDialog(this, "Alasan void transaksi:");
        if (alasan == null || alasan.trim().isEmpty()) {
            return;
        }

        try (Connection conn = DatabaseManager.getConnection()) {
            conn.setAutoCommit(false);
            try {
                String sqlDetail = "SELECT nama_barang, qty, harga, subtotal FROM detail_transaksi WHERE id_transaksi = ?";
                int totalVoid = 0;
                try (PreparedStatement ps = conn.prepareStatement(sqlDetail)) {
                    ps.setInt(1, idTransaksi);
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            String nama = rs.getString("nama_barang");
                            int qty = rs.getInt("qty");
                            int sub = rs.getInt("subtotal");
                            totalVoid += sub;

                            try (PreparedStatement up = conn.prepareStatement("UPDATE barang SET stok = stok + ? WHERE nama_barang = ?")) {
                                up.setInt(1, qty);
                                up.setString(2, nama);
                                up.executeUpdate();
                            }
                        }
                    }
                }

                try (PreparedStatement up = conn.prepareStatement(
                        "UPDATE transaksi SET total_harga = 0, status_transaksi = 'VOID', catatan_status = ?, updated_by = ? WHERE id_transaksi = ?")) {
                    up.setString(1, alasan.trim());
                    up.setString(2, LoginFrame.kasirAktif);
                    up.setInt(3, idTransaksi);
                    up.executeUpdate();
                }

                int idRetur;
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO retur_transaksi (id_transaksi, kasir, jenis, alasan, total_retur) VALUES (?, ?, 'VOID', ?, ?)",
                        Statement.RETURN_GENERATED_KEYS)) {
                    ps.setInt(1, idTransaksi);
                    ps.setString(2, LoginFrame.kasirAktif);
                    ps.setString(3, alasan.trim());
                    ps.setInt(4, totalVoid);
                    ps.executeUpdate();
                    try (ResultSet gk = ps.getGeneratedKeys()) {
                        gk.next();
                        idRetur = gk.getInt(1);
                    }
                }

                try (PreparedStatement psD = conn.prepareStatement(
                        "INSERT INTO detail_retur (id_retur, nama_barang, qty, harga, subtotal) " +
                        "SELECT ?, nama_barang, qty, harga, subtotal FROM detail_transaksi WHERE id_transaksi = ?")) {
                    psD.setInt(1, idRetur);
                    psD.setInt(2, idTransaksi);
                    psD.executeUpdate();
                }

                conn.commit();
                JOptionPane.showMessageDialog(this, "Void transaksi berhasil.");
            } catch (Exception e) {
                conn.rollback();
                throw e;
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Gagal void transaksi: " + e.getMessage());
        }
        loadTransaksi();
    }

    private void returItem() {
        int rowTr = tblTransaksi.getSelectedRow();
        int rowDt = tblDetail.getSelectedRow();
        if (rowTr == -1 || rowDt == -1) {
            JOptionPane.showMessageDialog(this, "Pilih transaksi dan item detail terlebih dahulu.");
            return;
        }
        if (!authorizeAdminAction()) {
            return;
        }

        int idTransaksi = Integer.parseInt(String.valueOf(modelTransaksi.getValueAt(rowTr, 0)));
        String status = String.valueOf(modelTransaksi.getValueAt(rowTr, 3));
        if ("VOID".equalsIgnoreCase(status) || "RETURN_FULL".equalsIgnoreCase(status)) {
            JOptionPane.showMessageDialog(this, "Transaksi dengan status " + status + " tidak bisa diretur lagi.");
            return;
        }

        int idDetail = Integer.parseInt(String.valueOf(modelDetail.getValueAt(rowDt, 0)));
        String namaBarang = String.valueOf(modelDetail.getValueAt(rowDt, 1));
        int harga = Integer.parseInt(String.valueOf(modelDetail.getValueAt(rowDt, 2)));
        int qtyAsli = Integer.parseInt(String.valueOf(modelDetail.getValueAt(rowDt, 3)));

        String inputQty = JOptionPane.showInputDialog(this, "Qty retur (maks " + qtyAsli + "):", "1");
        if (inputQty == null) {
            return;
        }
        int qtyRetur;
        try {
            qtyRetur = Integer.parseInt(inputQty.trim());
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Qty retur harus angka.");
            return;
        }
        if (qtyRetur <= 0 || qtyRetur > qtyAsli) {
            JOptionPane.showMessageDialog(this, "Qty retur tidak valid.");
            return;
        }

        String alasan = JOptionPane.showInputDialog(this, "Alasan retur:");
        if (alasan == null || alasan.trim().isEmpty()) {
            return;
        }

        int subtotalRetur = harga * qtyRetur;
        try (Connection conn = DatabaseManager.getConnection()) {
            conn.setAutoCommit(false);
            try {
                int qtySisa = qtyAsli - qtyRetur;
                if (qtySisa == 0) {
                    try (PreparedStatement del = conn.prepareStatement("DELETE FROM detail_transaksi WHERE id_detail = ?")) {
                        del.setInt(1, idDetail);
                        del.executeUpdate();
                    }
                } else {
                    try (PreparedStatement up = conn.prepareStatement(
                            "UPDATE detail_transaksi SET qty = ?, subtotal = ?, laba_kotor = laba_kotor - (? * COALESCE(harga_modal,0)) WHERE id_detail = ?")) {
                        up.setInt(1, qtySisa);
                        up.setInt(2, qtySisa * harga);
                        up.setInt(3, qtyRetur);
                        up.setInt(4, idDetail);
                        up.executeUpdate();
                    }
                }

                try (PreparedStatement up = conn.prepareStatement("UPDATE barang SET stok = stok + ? WHERE nama_barang = ?")) {
                    up.setInt(1, qtyRetur);
                    up.setString(2, namaBarang);
                    up.executeUpdate();
                }

                int totalSisa = 0;
                try (PreparedStatement sum = conn.prepareStatement("SELECT COALESCE(SUM(subtotal),0) FROM detail_transaksi WHERE id_transaksi = ?")) {
                    sum.setInt(1, idTransaksi);
                    try (ResultSet rs = sum.executeQuery()) {
                        if (rs.next()) {
                            totalSisa = rs.getInt(1);
                        }
                    }
                }
                String newStatus = totalSisa == 0 ? "RETURN_FULL" : "RETURN_PARTIAL";
                try (PreparedStatement upTr = conn.prepareStatement(
                        "UPDATE transaksi SET total_harga = ?, status_transaksi = ?, catatan_status = ?, updated_by = ? WHERE id_transaksi = ?")) {
                    upTr.setInt(1, totalSisa);
                    upTr.setString(2, newStatus);
                    upTr.setString(3, alasan.trim());
                    upTr.setString(4, LoginFrame.kasirAktif);
                    upTr.setInt(5, idTransaksi);
                    upTr.executeUpdate();
                }

                int idRetur;
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO retur_transaksi (id_transaksi, kasir, jenis, alasan, total_retur) VALUES (?, ?, 'RETUR', ?, ?)",
                        Statement.RETURN_GENERATED_KEYS)) {
                    ps.setInt(1, idTransaksi);
                    ps.setString(2, LoginFrame.kasirAktif);
                    ps.setString(3, alasan.trim());
                    ps.setInt(4, subtotalRetur);
                    ps.executeUpdate();
                    try (ResultSet gk = ps.getGeneratedKeys()) {
                        gk.next();
                        idRetur = gk.getInt(1);
                    }
                }

                try (PreparedStatement insDet = conn.prepareStatement(
                        "INSERT INTO detail_retur (id_retur, nama_barang, qty, harga, subtotal) VALUES (?, ?, ?, ?, ?)")) {
                    insDet.setInt(1, idRetur);
                    insDet.setString(2, namaBarang);
                    insDet.setInt(3, qtyRetur);
                    insDet.setInt(4, harga);
                    insDet.setInt(5, subtotalRetur);
                    insDet.executeUpdate();
                }

                conn.commit();
                JOptionPane.showMessageDialog(this, "Retur item berhasil.");
            } catch (Exception e) {
                conn.rollback();
                throw e;
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Gagal retur item: " + e.getMessage());
        }
        loadTransaksi();
    }
}

