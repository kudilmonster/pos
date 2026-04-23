package toko.aplikasipos;

import java.awt.BorderLayout;
import java.awt.Dimension;
import javax.swing.JFrame;
import javax.swing.JTabbedPane;

public class AdminWorkspaceFrame extends javax.swing.JFrame {

    private final JTabbedPane tabs = new JTabbedPane();

    public AdminWorkspaceFrame() {
        this(true);
    }

    public AdminWorkspaceFrame(boolean primaryDashboard) {
        setTitle("Dashboard Admin");
        setSize(new Dimension(1100, 700));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(primaryDashboard ? EXIT_ON_CLOSE : DISPOSE_ON_CLOSE);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        AppUtil.setWindowIcon(this);
        initUi();
    }

    private void initUi() {
        setLayout(new BorderLayout());
        add(tabs, BorderLayout.CENTER);

        tabs.addTab("Input Data", wrapFrame(new MainFrame(this)));
        tabs.addTab("Laporan", wrapFrame(new ReportFrame()));
        tabs.addTab("Riwayat/Void", wrapFrame(new TransactionHistoryFrame()));
        tabs.addTab("Pembelian", wrapFrame(new SupplierPurchaseFrame()));
        tabs.addTab("Stok Minimum", wrapFrame(new StockAlertFrame()));
        tabs.addTab("Backup/Restore", wrapFrame(new BackupRestoreFrame()));
    }

    private javax.swing.JPanel wrapFrame(javax.swing.JFrame frame) {
        java.awt.Container content = frame.getContentPane();
        frame.setContentPane(new javax.swing.JPanel());
        javax.swing.JPanel panel = new javax.swing.JPanel(new BorderLayout());
        panel.add(content, BorderLayout.CENTER);
        return panel;
    }

    public void openTab(String name) {
        for (int i = 0; i < tabs.getTabCount(); i++) {
            if (tabs.getTitleAt(i).equalsIgnoreCase(name)) {
                tabs.setSelectedIndex(i);
                return;
            }
        }
    }
}

