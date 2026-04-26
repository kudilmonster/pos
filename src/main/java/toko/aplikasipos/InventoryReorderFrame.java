package toko.aplikasipos;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.List;

public class InventoryReorderFrame extends JFrame {
    private final DefaultTableModel model = new DefaultTableModel(new Object[]{"ID Barang","Nama Barang","Stok","Reorder Level","Qty To Order"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) { return false; }
    };
    private final JTable tbl = new JTable(model);

    public InventoryReorderFrame() {
        setTitle("Inventory Reorder & Purchase Orders");
        setSize(900, 520);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JScrollPane sp = new JScrollPane(tbl);
        sp.setPreferredSize(new Dimension(860, 420));

        JButton btnCreatePO = new JButton("Buat Purchase Order");
        btnCreatePO.addActionListener(this::onCreatePO);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottom.setBackground(new Color(39, 60, 117));
        bottom.add(btnCreatePO);

        setLayout(new BorderLayout(10, 10));
        add(sp, BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);

        loadReorderList();
    }

    private void loadReorderList() {
        model.setRowCount(0);
        String sql = "SELECT id_barang, nama_barang, stok, stok_minimum, (stok_minimum*2 - stok) AS qty_to_order FROM barang WHERE stok <= stok_minimum";
        try (Connection conn = DatabaseManager.getConnection(); Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                int id = rs.getInt("id_barang");
                String name = rs.getString("nama_barang");
                int stok = rs.getInt("stok");
                int reorder = rs.getInt("stok_minimum");
                int qty = rs.getInt("qty_to_order");
                if (qty <= 0) qty = 1;
                model.addRow(new Object[]{id, name, stok, reorder, qty});
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Gagal load reorder: "+e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void onCreatePO(ActionEvent e) {
        // Load suppliers
        java.util.Map<Integer,String> supplierMap = new java.util.HashMap<>();
        java.util.List<Integer> supplierIds = new java.util.ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection(); Statement st = conn.createStatement(); ResultSet rs = st.executeQuery("SELECT id_supplier, nama_supplier FROM supplier ORDER BY nama_supplier")) {
            while (rs.next()) {
                int id = rs.getInt("id_supplier");
                String name = rs.getString("nama_supplier");
                supplierIds.add(id);
                supplierMap.put(id, name);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Gagal load supplier: "+ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (supplierIds.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Tidak ada supplier terdaftar.", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        // Build supplier selector
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(39, 60, 117));
        panel.add(new JLabel("Pilih Supplier:"), BorderLayout.NORTH);
        JComboBox<String> cb = new JComboBox<>();
        for (int id : supplierIds) {
            cb.addItem(id + " - " + supplierMap.get(id));
        }
        panel.add(cb, BorderLayout.CENTER);
        int res = JOptionPane.showConfirmDialog(this, panel, "Pilih Supplier", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (res != JOptionPane.OK_OPTION) return;
        int supplierId = Integer.parseInt(((String) cb.getSelectedItem()).split(" - ")[0]);
        String kasir = LoginFrame.getKasirAktif();
        
        try (Connection conn = DatabaseManager.getConnection()) {
            conn.setAutoCommit(false);
            int idPembelian;
            try (PreparedStatement ps = conn.prepareStatement("INSERT INTO pembelian (id_supplier, total_beli, dibuat_oleh) VALUES (?, ?, ?)", java.sql.Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, supplierId);
                ps.setInt(2, 0);
                ps.setString(3, kasir);
                ps.executeUpdate();
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    rs.next();
                    idPembelian = rs.getInt(1);
                }
            }
            int totalBeli = 0;
            for (int r = 0; r < model.getRowCount(); r++) {
                int idBarang = (int) model.getValueAt(r, 0);
                String namaBarang = (String) model.getValueAt(r, 1);
                int qtyToOrder = (int) model.getValueAt(r, 4);
                int hargaBeli = 0;
                // fetch harga_modal
                try (Statement s2 = conn.createStatement(); ResultSet rs2 = s2.executeQuery("SELECT harga_modal FROM barang WHERE id_barang = " + idBarang)) {
                    if (rs2.next()) hargaBeli = rs2.getInt("harga_modal");
                }
                int subtotal = qtyToOrder * hargaBeli;
                totalBeli += subtotal;
                try (PreparedStatement ps2 = conn.prepareStatement("INSERT INTO detail_pembelian (id_pembelian, nama_barang, qty, harga_beli, subtotal) VALUES (?, ?, ?, ?, ?)")) {
                    ps2.setInt(1, idPembelian);
                    ps2.setString(2, namaBarang);
                    ps2.setInt(3, qtyToOrder);
                    ps2.setInt(4, hargaBeli);
                    ps2.setInt(5, subtotal);
                    ps2.executeUpdate();
                }
            }
            try (PreparedStatement ps3 = conn.prepareStatement("UPDATE pembelian SET total_beli = ? WHERE id_pembelian = ?")) {
                ps3.setInt(1, totalBeli);
                ps3.setInt(2, idPembelian);
                ps3.executeUpdate();
            }
            conn.commit();
            JOptionPane.showMessageDialog(this, "PO berhasil dibuat. ID: " + idPembelian);
            loadReorderList();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Gagal buat PO: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
