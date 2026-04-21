package toko.aplikasipos;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
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

    private static final String DB_URL = "jdbc:sqlite:pos_db.db";
    private final DefaultTableModel model = new DefaultTableModel(
            new String[]{"ID", "Nama Barang", "Stok", "Stok Minimum", "Status"}, 0
    ) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable table = new JTable(model);
    private final JTextField txtMin = new JTextField(5);

    public StockAlertFrame() {
        setTitle("Notifikasi Stok Minimum");
        setSize(760, 480);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        initUi();
        loadData();
    }

    private void initUi() {
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton btnRefresh = new JButton("Refresh");
        JButton btnUpdateMin = new JButton("Set Min untuk Item Terpilih");
        top.add(new JLabel("Min Baru:"));
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
        String sql = """
                SELECT id_barang, nama_barang, stok, COALESCE(stok_minimum, 5) AS stok_minimum
                FROM barang
                ORDER BY (stok <= COALESCE(stok_minimum, 5)) DESC, nama_barang ASC
                """;
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                int stok = rs.getInt("stok");
                int min = rs.getInt("stok_minimum");
                String status = stok <= min ? "KRITIS" : "AMAN";
                model.addRow(new Object[]{
                    rs.getInt("id_barang"),
                    rs.getString("nama_barang"),
                    stok,
                    min,
                    status
                });
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Gagal load stok minimum: " + e.getMessage());
        }
    }

    private void updateMinimum() {
        int row = table.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Pilih barang dulu.");
            return;
        }
        int minBaru;
        try {
            minBaru = Integer.parseInt(txtMin.getText().trim());
            if (minBaru < 0) {
                throw new NumberFormatException();
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Min baru harus angka >= 0.");
            return;
        }

        int idBarang = Integer.parseInt(String.valueOf(model.getValueAt(row, 0)));
        String sql = "UPDATE barang SET stok_minimum = ? WHERE id_barang = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, minBaru);
            ps.setInt(2, idBarang);
            ps.executeUpdate();
            JOptionPane.showMessageDialog(this, "Stok minimum diperbarui.");
            loadData();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Gagal update stok minimum: " + e.getMessage());
        }
    }
}
