package toko.aplikasipos;

import com.lowagie.text.Document;
import com.lowagie.text.Font;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.text.NumberFormat;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;

public class ReportFrame extends JFrame {

    private static final String DB_URL = "jdbc:sqlite:pos_db.db";
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final NumberFormat IDR_FORMAT = NumberFormat.getNumberInstance(Locale.forLanguageTag("id-ID"));

    private final JTextField txtTanggalAwal = new JTextField(10);
    private final JTextField txtTanggalAkhir = new JTextField(10);
    private final JLabel lblTotalTransaksi = new JLabel("0");
    private final JLabel lblOmzet = new JLabel("Rp 0");
    private final JLabel lblLabaKotor = new JLabel("Rp 0");
    private final JLabel lblRataRata = new JLabel("Rp 0");
    private final JComboBox<String> cbChartMode = new JComboBox<>(new String[]{"Qty Terjual", "Omzet Item"});
    private final List<String> chartLabels = new ArrayList<>();
    private final List<Integer> chartQtyValues = new ArrayList<>();
    private final List<Integer> chartOmzetValues = new ArrayList<>();
    private final List<Integer> chartValues = new ArrayList<>();
    private final DefaultTableModel modelTopItem = new DefaultTableModel(
            new String[]{"Nama Barang", "Qty Terjual", "Omzet Item"}, 0
    ) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };

    private final JTable tblTopItem = new JTable(modelTopItem);
    private final JPanel chartPanel = new TopItemChartPanel();

    public ReportFrame() {
        setTitle("Laporan Penjualan");
        setSize(800, 520);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        initUi();
        setDefaultTanggal();
        loadLaporan();
    }

    private void initUi() {
        JPanel panelFilter = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton btnMuat = new JButton("Muat Laporan");
        JButton btnPreview = new JButton("Preview");
        JButton btnExportCsv = new JButton("Export CSV");
        JButton btnExportPdf = new JButton("Export PDF");

        panelFilter.add(new JLabel("Tanggal Awal (YYYY-MM-DD):"));
        panelFilter.add(txtTanggalAwal);
        panelFilter.add(new JLabel("Tanggal Akhir (YYYY-MM-DD):"));
        panelFilter.add(txtTanggalAkhir);
        panelFilter.add(btnMuat);
        panelFilter.add(btnPreview);
        panelFilter.add(btnExportCsv);
        panelFilter.add(btnExportPdf);
        panelFilter.add(new JLabel("Mode Chart:"));
        panelFilter.add(cbChartMode);

        JPanel panelSummary = new JPanel(new GridLayout(2, 4, 8, 8));
        panelSummary.add(new JLabel("Total Transaksi"));
        panelSummary.add(lblTotalTransaksi);
        panelSummary.add(new JLabel("Omzet"));
        panelSummary.add(lblOmzet);
        panelSummary.add(new JLabel("Laba Kotor"));
        panelSummary.add(lblLabaKotor);
        panelSummary.add(new JLabel("Rata-rata/Transaksi"));
        panelSummary.add(lblRataRata);

        JPanel panelMain = new JPanel(new BorderLayout(8, 8));
        panelMain.add(panelSummary, BorderLayout.NORTH);
        panelMain.add(new JScrollPane(tblTopItem), BorderLayout.CENTER);
        chartPanel.setPreferredSize(new Dimension(760, 200));
        panelMain.add(chartPanel, BorderLayout.SOUTH);

        setLayout(new BorderLayout(8, 8));
        add(panelFilter, BorderLayout.NORTH);
        add(panelMain, BorderLayout.CENTER);

        btnMuat.addActionListener(e -> loadLaporan());
        btnPreview.addActionListener(e -> previewLaporan());
        btnExportCsv.addActionListener(e -> exportCsv());
        btnExportPdf.addActionListener(e -> exportPdf());
        cbChartMode.addActionListener(e -> {
            refreshChartValues();
            chartPanel.repaint();
        });
    }

    private void setDefaultTanggal() {
        LocalDate today = LocalDate.now();
        txtTanggalAwal.setText(today.withDayOfMonth(1).format(DATE_FORMAT));
        txtTanggalAkhir.setText(today.format(DATE_FORMAT));
    }

    private void loadLaporan() {
        LocalDate awal;
        LocalDate akhir;
        try {
            awal = LocalDate.parse(txtTanggalAwal.getText().trim(), DATE_FORMAT);
            akhir = LocalDate.parse(txtTanggalAkhir.getText().trim(), DATE_FORMAT);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Format tanggal wajib YYYY-MM-DD.");
            return;
        }

        if (akhir.isBefore(awal)) {
            JOptionPane.showMessageDialog(this, "Tanggal akhir tidak boleh lebih kecil dari tanggal awal.");
            return;
        }

        String sqlSummary = """
                SELECT
                    COUNT(*) AS total_transaksi,
                    COALESCE(SUM(total_harga), 0) AS omzet
                FROM transaksi
                WHERE DATE(tanggal) BETWEEN ? AND ?
                """;
        String sqlLaba = """
                SELECT COALESCE(SUM(d.laba_kotor), 0) AS laba_kotor
                FROM detail_transaksi d
                JOIN transaksi t ON t.id_transaksi = d.id_transaksi
                WHERE DATE(t.tanggal) BETWEEN ? AND ?
                """;

        String sqlTopItem = """
                SELECT
                    d.nama_barang,
                    COALESCE(SUM(d.qty), 0) AS qty_terjual,
                    COALESCE(SUM(d.subtotal), 0) AS omzet_item
                FROM detail_transaksi d
                JOIN transaksi t ON t.id_transaksi = d.id_transaksi
                WHERE DATE(t.tanggal) BETWEEN ? AND ?
                GROUP BY d.nama_barang
                ORDER BY qty_terjual DESC, omzet_item DESC
                """;

        modelTopItem.setRowCount(0);
        chartLabels.clear();
        chartQtyValues.clear();
        chartOmzetValues.clear();
        chartValues.clear();
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            try (PreparedStatement ps = conn.prepareStatement(sqlSummary)) {
                ps.setString(1, awal.toString());
                ps.setString(2, akhir.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        int totalTransaksi = rs.getInt("total_transaksi");
                        int omzet = rs.getInt("omzet");
                        int labaKotor = 0;
                        try (PreparedStatement psLaba = conn.prepareStatement(sqlLaba)) {
                            psLaba.setString(1, awal.toString());
                            psLaba.setString(2, akhir.toString());
                            try (ResultSet rsLaba = psLaba.executeQuery()) {
                                if (rsLaba.next()) {
                                    labaKotor = rsLaba.getInt("laba_kotor");
                                }
                            }
                        }
                        int rataRata = totalTransaksi == 0 ? 0 : (int) Math.round((double) omzet / totalTransaksi);

                        lblTotalTransaksi.setText(String.valueOf(totalTransaksi));
                        lblOmzet.setText(formatRupiah(omzet));
                        lblLabaKotor.setText(formatRupiah(labaKotor));
                        lblRataRata.setText(formatRupiah(rataRata));
                    }
                }
            }

            try (PreparedStatement ps = conn.prepareStatement(sqlTopItem)) {
                ps.setString(1, awal.toString());
                ps.setString(2, akhir.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    int chartCount = 0;
                    while (rs.next()) {
                        String namaBarang = rs.getString("nama_barang");
                        int qtyTerjual = rs.getInt("qty_terjual");
                        int omzetItem = rs.getInt("omzet_item");

                        modelTopItem.addRow(new Object[]{
                            namaBarang,
                            qtyTerjual,
                            omzetItem
                        });

                        if (chartCount < 5) {
                            chartLabels.add(namaBarang);
                            chartQtyValues.add(qtyTerjual);
                            chartOmzetValues.add(omzetItem);
                            chartCount++;
                        }
                    }
                }
            }
            refreshChartValues();
            chartPanel.repaint();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Gagal memuat laporan: " + e.getMessage());
        }
    }

    private void refreshChartValues() {
        chartValues.clear();
        boolean modeQty = "Qty Terjual".equals(String.valueOf(cbChartMode.getSelectedItem()));
        List<Integer> source = modeQty ? chartQtyValues : chartOmzetValues;
        chartValues.addAll(source);
    }

    private void exportCsv() {
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File("laporan-penjualan.csv"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }

        try (PrintWriter writer = new PrintWriter(chooser.getSelectedFile())) {
            writer.println("Tanggal Awal," + txtTanggalAwal.getText().trim());
            writer.println("Tanggal Akhir," + txtTanggalAkhir.getText().trim());
            writer.println("Total Transaksi," + lblTotalTransaksi.getText());
            writer.println("Omzet," + lblOmzet.getText());
            writer.println("Rata-rata per Transaksi," + lblRataRata.getText());
            writer.println();
            writer.println("Nama Barang,Qty Terjual,Omzet Item");

            for (int i = 0; i < modelTopItem.getRowCount(); i++) {
                writer.printf("%s,%s,%s%n",
                        modelTopItem.getValueAt(i, 0),
                        modelTopItem.getValueAt(i, 1),
                        modelTopItem.getValueAt(i, 2));
            }

            JOptionPane.showMessageDialog(this, "CSV berhasil disimpan.");
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Gagal export CSV: " + e.getMessage());
        }
    }

    private void previewLaporan() {
        JTextArea area = new JTextArea(buildTextReport());
        area.setEditable(false);
        area.setFont(new java.awt.Font("Monospaced", java.awt.Font.PLAIN, 12));
        JOptionPane.showMessageDialog(this, new JScrollPane(area), "Preview Laporan", JOptionPane.INFORMATION_MESSAGE);
    }

    private void exportPdf() {
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File("laporan-penjualan.pdf"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }

        try {
            Document document = new Document();
            PdfWriter.getInstance(document, new FileOutputStream(chooser.getSelectedFile()));
            document.open();
            Font font = new Font(Font.COURIER, 10, Font.NORMAL);

            String[] lines = buildTextReport().split("\n");
            for (String line : lines) {
                document.add(new Paragraph(line, font));
            }

            document.close();
            JOptionPane.showMessageDialog(this, "PDF berhasil disimpan.");
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Gagal export PDF: " + e.getMessage());
        }
    }

    private String buildTextReport() {
        StringBuilder sb = new StringBuilder();
        sb.append("LAPORAN PENJUALAN\n");
        sb.append("Tanggal Awal : ").append(txtTanggalAwal.getText().trim()).append("\n");
        sb.append("Tanggal Akhir: ").append(txtTanggalAkhir.getText().trim()).append("\n");
        sb.append("Total Transaksi : ").append(lblTotalTransaksi.getText()).append("\n");
        sb.append("Omzet           : ").append(lblOmzet.getText()).append("\n");
        sb.append("Laba Kotor      : ").append(lblLabaKotor.getText()).append("\n");
        sb.append("Rata-rata       : ").append(lblRataRata.getText()).append("\n");
        sb.append("----------------------------------------\n");
        sb.append(String.format("%-20s %8s %14s%n", "Nama Barang", "Qty", "Omzet"));
        sb.append("----------------------------------------\n");

        if (modelTopItem.getRowCount() == 0) {
            sb.append("Tidak ada data pada rentang tanggal ini.\n");
            return sb.toString();
        }

        for (int i = 0; i < modelTopItem.getRowCount(); i++) {
            String nama = String.valueOf(modelTopItem.getValueAt(i, 0));
            String qty = String.valueOf(modelTopItem.getValueAt(i, 1));
            int omzetValue = Integer.parseInt(String.valueOf(modelTopItem.getValueAt(i, 2)));
            String omzet = formatRupiah(omzetValue);
            if (nama.length() > 20) {
                nama = nama.substring(0, 20);
            }
            sb.append(String.format("%-20s %8s %14s%n", nama, qty, omzet));
        }

        return sb.toString();
    }

    private class TopItemChartPanel extends JPanel {

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            g.setColor(new Color(39, 60, 117));
            g.fillRect(0, 0, getWidth(), getHeight());

            g.setColor(Color.WHITE);
            String modeLabel = String.valueOf(cbChartMode.getSelectedItem());
            g.drawString("Chart Top 5 Item (" + modeLabel + ")", 12, 20);

            if (chartValues.isEmpty()) {
                g.drawString("Belum ada data untuk ditampilkan.", 12, 45);
                return;
            }

            int left = 20;
            int top = 35;
            int width = getWidth() - 40;
            int height = getHeight() - 65;

            int maxValue = 1;
            for (int v : chartValues) {
                if (v > maxValue) {
                    maxValue = v;
                }
            }

            int itemCount = chartValues.size();
            int gap = 12;
            int barWidth = Math.max(25, (width - ((itemCount + 1) * gap)) / itemCount);
            int x = left + gap;

            for (int i = 0; i < itemCount; i++) {
                int value = chartValues.get(i);
                int barHeight = (int) Math.round((double) value / maxValue * height);
                int y = top + (height - barHeight);

                g.setColor(new Color(0, 168, 255));
                g.fillRect(x, y, barWidth, barHeight);

                g.setColor(Color.WHITE);
                boolean modeQty = "Qty Terjual".equals(String.valueOf(cbChartMode.getSelectedItem()));
                String valueLabel = modeQty ? String.valueOf(value) : formatRupiah(value);
                if (!modeQty && valueLabel.length() > 10) {
                    valueLabel = valueLabel.substring(0, 10) + ".";
                }
                g.drawString(valueLabel, x + 2, y - 5);

                String label = chartLabels.get(i);
                if (label.length() > 10) {
                    label = label.substring(0, 10) + ".";
                }
                g.drawString(label, x, top + height + 15);

                x += barWidth + gap;
            }
        }
    }

    private String formatRupiah(int value) {
        return "Rp " + IDR_FORMAT.format(value);
    }
}
