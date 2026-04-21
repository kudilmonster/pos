package toko.aplikasipos;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
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

    private static final String DB_URL = "jdbc:sqlite:pos_db.db";

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
        setTitle("Supplier & Pembelian Barang");
        setSize(1000, 580);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        initUi();
        loadSupplier();
        loadBarang();
    }

    private void initUi() {
        JPanel formSupplier = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton btnTambahSupplier = new JButton("Tambah Supplier");
        formSupplier.add(new JLabel("Nama:"));
        formSupplier.add(txtNamaSupplier);
        formSupplier.add(new JLabel("Kontak:"));
        formSupplier.add(txtKontak);
        formSupplier.add(new JLabel("Alamat:"));
        formSupplier.add(txtAlamat);
        formSupplier.add(btnTambahSupplier);

        JTable tblSupplier = new JTable(modelSupplier);
        JScrollPane spSupplier = new JScrollPane(tblSupplier);

        JPanel formPembelian = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton btnSimpanBeli = new JButton("Simpan Pembelian");
        formPembelian.add(new JLabel("Supplier:"));
        formPembelian.add(cbSupplier);
        formPembelian.add(new JLabel("Barang:"));
        formPembelian.add(cbBarang);
        formPembelian.add(new JLabel("Qty:"));
        formPembelian.add(txtQty);
        formPembelian.add(new JLabel("Harga Beli:"));
        formPembelian.add(txtHargaBeli);
        formPembelian.add(btnSimpanBeli);

        JPanel root = new JPanel(new GridLayout(3, 1, 8, 8));
        root.add(formSupplier);
        root.add(spSupplier);
        root.add(formPembelian);

        setLayout(new BorderLayout(8, 8));
        add(root, BorderLayout.CENTER);

        btnTambahSupplier.addActionListener(e -> simpanSupplier());
        btnSimpanBeli.addActionListener(e -> simpanPembelian());
    }

    private void loadSupplier() {
        modelSupplier.setRowCount(0);
        cbSupplier.removeAllItems();
        String sql = "SELECT id_supplier, nama_supplier, COALESCE(kontak,''), COALESCE(alamat,'') FROM supplier ORDER BY nama_supplier";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                int id = rs.getInt("id_supplier");
                String nama = rs.getString("nama_supplier");
                modelSupplier.addRow(new Object[]{id, nama, rs.getString("kontak"), rs.getString("alamat")});
                cbSupplier.addItem(id + " - " + nama);
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Gagal load supplier: " + e.getMessage());
        }
    }

    private void loadBarang() {
        cbBarang.removeAllItems();
        String sql = "SELECT nama_barang FROM barang ORDER BY nama_barang";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                cbBarang.addItem(rs.getString(1));
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Gagal load barang: " + e.getMessage());
        }
    }

    private void simpanSupplier() {
        String nama = txtNamaSupplier.getText().trim();
        if (nama.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Nama supplier wajib diisi.");
            return;
        }
        String sql = "INSERT INTO supplier (nama_supplier, kontak, alamat) VALUES (?, ?, ?)";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nama);
            ps.setString(2, txtKontak.getText().trim());
            ps.setString(3, txtAlamat.getText().trim());
            ps.executeUpdate();
            txtNamaSupplier.setText("");
            txtKontak.setText("");
            txtAlamat.setText("");
            loadSupplier();
            JOptionPane.showMessageDialog(this, "Supplier tersimpan.");
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Gagal simpan supplier: " + e.getMessage());
        }
    }

    private int parseInt(String value, String name) throws Exception {
        try {
            int v = Integer.parseInt(value.trim());
            if (v <= 0) {
                throw new Exception(name + " harus > 0.");
            }
            return v;
        } catch (NumberFormatException e) {
            throw new Exception(name + " harus angka.");
        }
    }

    private void simpanPembelian() {
        if (cbSupplier.getSelectedItem() == null || cbBarang.getSelectedItem() == null) {
            JOptionPane.showMessageDialog(this, "Supplier dan barang wajib dipilih.");
            return;
        }

        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            int qty = parseInt(txtQty.getText(), "Qty");
            int hargaBeli = parseInt(txtHargaBeli.getText(), "Harga Beli");
            int subtotal = qty * hargaBeli;

            String supplierItem = cbSupplier.getSelectedItem().toString();
            int idSupplier = Integer.parseInt(supplierItem.split(" - ")[0]);
            String namaBarang = cbBarang.getSelectedItem().toString();

            conn.setAutoCommit(false);
            try {
                int idPembelian;
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO pembelian (id_supplier, total_beli, dibuat_oleh) VALUES (?, ?, ?)",
                        Statement.RETURN_GENERATED_KEYS)) {
                    ps.setInt(1, idSupplier);
                    ps.setInt(2, subtotal);
                    ps.setString(3, LoginFrame.kasirAktif);
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

                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE barang SET stok = stok + ?, harga_modal = ? WHERE nama_barang = ?")) {
                    ps.setInt(1, qty);
                    ps.setInt(2, hargaBeli);
                    ps.setString(3, namaBarang);
                    ps.executeUpdate();
                }

                conn.commit();
                JOptionPane.showMessageDialog(this, "Pembelian berhasil disimpan.");
                txtQty.setText("1");
                txtHargaBeli.setText("");
            } catch (Exception e) {
                conn.rollback();
                throw e;
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Gagal simpan pembelian: " + e.getMessage());
        }
    }
}
