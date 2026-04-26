package toko.aplikasipos;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;

public class StockAlertFrame extends JFrame {

    
    // 1. TAMBAH KOLOM JUAL DAN MARGIN
    private final DefaultTableModel model = new DefaultTableModel(
            new String[]{"ID", "Nama Barang", "Harga Beli", "Harga Jual", "Margin", "Stok", "Status"}, 0
    ) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    
    private final JTable table = new JTable(model);
    private final JTextField txtMin = new JTextField(5);

    public StockAlertFrame() {
        setTitle("Analisis Stok & Margin Keuntungan");
        setSize(950, 500); 
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        
        AppUtil.setWindowIcon(this);
        initUi();
        loadData();
    }

    private void initUi() {
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.setBackground(new Color(39, 60, 117));
        JButton btnRefresh = new JButton("Refresh Data");
        JButton btnUpdateMin = new JButton("Atur Stok Minimum");
        top.add(new JLabel("Batas Stok Minimum:"));
        top.add(txtMin);
        top.add(btnUpdateMin);
        top.add(btnRefresh);

        setLayout(new BorderLayout(8, 8));
        add(top, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);

        btnRefresh.addActionListener(e -> loadData());
        btnUpdateMin.addActionListener(e -> updateMinimum());
    }

    private void loadData() {
        model.setRowCount(0);
        
        // 2. QUERY SQL: Mengambil harga_modal (Beli) dan harga (Jual)
        String sql = """
                SELECT id_barang, nama_barang, 
                       COALESCE(harga_modal, 0) AS beli, 
                       COALESCE(harga, 0) AS jual, 
                       stok, 
                       COALESCE(stok_minimum, 5) AS min
                FROM barang
                ORDER BY (stok <= COALESCE(stok_minimum, 5)) DESC, nama_barang ASC
                """;
                
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
             
            while (rs.next()) {
                int beli = rs.getInt("beli");
                int jual = rs.getInt("jual");
                int margin = jual - beli;
                int stok = rs.getInt("stok");
                int min = rs.getInt("min");
                
                String status = stok <= min ? "RE-STOCK" : "AMAN";
                
                model.addRow(new Object[]{
                    rs.getInt("id_barang"),
                    rs.getString("nama_barang"),
                    formatRupiah(beli),
                    formatRupiah(jual),
                    formatRupiah(margin),
                    stok,
                    status
                });
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Gagal memuat data: " + e.getMessage());
        }
    }

    private String formatRupiah(int angka) {
        return "Rp " + String.format("%,d", angka).replace(',', '.');
    }

    private void updateMinimum() {
        int row = table.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Pilih barang di tabel terlebih dahulu.");
            return;
        }
        try {
            int minBaru = Integer.parseInt(txtMin.getText().trim());
            int idBarang = (int) model.getValueAt(row, 0);
            
            try (Connection conn = DatabaseManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement("UPDATE barang SET stok_minimum = ? WHERE id_barang = ?")) {
                ps.setInt(1, minBaru);
                ps.setInt(2, idBarang);
                ps.executeUpdate();
                loadData();
                txtMin.setText("");
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Input harus angka valid.");
        }
    }
}
