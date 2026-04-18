package toko.aplikasipos;

import java.awt.Color;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import javax.swing.JOptionPane;

public class LoginFrame extends javax.swing.JFrame {

    private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(LoginFrame.class.getName());

    // Variabel statis ini bisa dipanggil dari MainFrame atau KasirFrame nanti
    public static String kasirAktif = "";

    // VARIABEL BARU: Jatah percobaan login
    private int sisaKesempatan = 3;

    public LoginFrame() {
        initComponents();
        this.setLocationRelativeTo(null); // (Opsional) Agar form di tengah layar
        setBackground(new Color(0, 0, 0, 0));

    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        customRoundedPanel1 = new toko.aplikasipos.CustomRoundedPanel();
        jLabel1 = new javax.swing.JLabel();
        txtUsername = new javax.swing.JTextField();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        btnLogin = new javax.swing.JButton();
        txtPassword = new javax.swing.JPasswordField();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setUndecorated(true);
        setResizable(false);

        customRoundedPanel1.setcolorEnd(new java.awt.Color(25, 42, 86));
        customRoundedPanel1.setcolorStart(new java.awt.Color(0, 168, 255));

        jLabel1.setFont(new java.awt.Font("Segoe UI Semibold", 0, 14)); // NOI18N
        jLabel1.setForeground(new java.awt.Color(255, 255, 255));
        jLabel1.setText("User Name");

        txtUsername.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 1, 1));

        jLabel2.setFont(new java.awt.Font("Segoe UI Semibold", 0, 14)); // NOI18N
        jLabel2.setForeground(new java.awt.Color(255, 255, 255));
        jLabel2.setText("Password");

        jLabel3.setFont(new java.awt.Font("Segoe UI Black", 0, 24)); // NOI18N
        jLabel3.setForeground(new java.awt.Color(255, 255, 255));
        jLabel3.setText("Sign In");

        btnLogin.setText("Login");
        btnLogin.addActionListener(this::btnLoginActionPerformed);

        javax.swing.GroupLayout customRoundedPanel1Layout = new javax.swing.GroupLayout(customRoundedPanel1);
        customRoundedPanel1.setLayout(customRoundedPanel1Layout);
        customRoundedPanel1Layout.setHorizontalGroup(
            customRoundedPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(customRoundedPanel1Layout.createSequentialGroup()
                .addGap(40, 40, 40)
                .addGroup(customRoundedPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel1)
                    .addComponent(jLabel2))
                .addGap(44, 44, 44)
                .addGroup(customRoundedPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel3)
                    .addGroup(customRoundedPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                        .addComponent(txtUsername)
                        .addComponent(txtPassword, javax.swing.GroupLayout.PREFERRED_SIZE, 193, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(btnLogin)))
                .addContainerGap(54, Short.MAX_VALUE))
        );
        customRoundedPanel1Layout.setVerticalGroup(
            customRoundedPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(customRoundedPanel1Layout.createSequentialGroup()
                .addGap(28, 28, 28)
                .addComponent(jLabel3)
                .addGap(42, 42, 42)
                .addGroup(customRoundedPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(txtUsername, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(customRoundedPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jLabel2)
                    .addGroup(customRoundedPanel1Layout.createSequentialGroup()
                        .addComponent(txtPassword, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(2, 2, 2)))
                .addGap(18, 18, 18)
                .addComponent(btnLogin, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(68, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(customRoundedPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(customRoundedPanel1, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        pack();
        setLocationRelativeTo(null);
    }// </editor-fold>//GEN-END:initComponents

    private void btnLoginActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnLoginActionPerformed
        String username = txtUsername.getText();
        String password = new String(txtPassword.getPassword());

        if (username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Username dan Password tidak boleh kosong!", "Peringatan", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String url = "jdbc:sqlite:pos_db.db";
        String sql = "SELECT * FROM users WHERE username = ? AND password = ?";

        boolean isLoginSukses = false;
        String roleAkun = ""; // Variabel baru untuk menyimpan role

        try (Connection conn = DriverManager.getConnection(url); PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);
            pstmt.setString(2, password);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    isLoginSukses = true;
                    kasirAktif = rs.getString("username");
                    roleAkun = rs.getString("role"); // Ambil peran dari database

                    // Jaga-jaga jika role kosong (misal akun lama), anggap saja Kasir
                    if (roleAkun == null) {
                        roleAkun = "Kasir";
                    }
                }
            }

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error Login: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

// --- LOGIKA PEMBAGIAN HAK AKSES ---
        if (isLoginSukses) {
            JOptionPane.showMessageDialog(this, "Selamat Datang, " + kasirAktif + "!\nLogin sebagai: " + roleAkun);

            if (roleAkun.equalsIgnoreCase("Admin")) {
                MainFrame utama = new MainFrame();
                utama.setVisible(true);
            } else {
                KasirFrame kasir = new KasirFrame();
                kasir.setVisible(true);
            }
            this.dispose();

        } else {
            // JIKA LOGIN GAGAL, KURANGI JATAH KESEMPATAN
            sisaKesempatan--;

            if (sisaKesempatan > 0) {
                // Jika masih ada sisa kesempatan, beri peringatan
                JOptionPane.showMessageDialog(this, "Username atau Password salah!\nSisa percobaan Anda: " + sisaKesempatan, "Akses Ditolak", JOptionPane.WARNING_MESSAGE);

                // Kosongkan kotak password agar user mengetik ulang
                txtPassword.setText("");
                txtPassword.requestFocus(); // Pindahkan kursor kedip-kedip ke kotak password

            } else {
                // Jika jatah sudah 0, tutup paksa aplikasi
                JOptionPane.showMessageDialog(this, "Anda telah salah memasukkan data 3 kali berturut-turut.\nDemi keamanan, aplikasi akan ditutup otomatis.", "Sistem Terkunci", JOptionPane.ERROR_MESSAGE);
                System.exit(0); // Perintah Java untuk mematikan seluruh program
            }
        }
    }//GEN-LAST:event_btnLoginActionPerformed

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                boolean tokoSudahSetup = false;
                String url = "jdbc:sqlite:pos_db.db";

                try (java.sql.Connection conn = java.sql.DriverManager.getConnection(url)) {

                    // CARA LEBIH AMAN: Gunakan MetaData bawaan Java untuk mengecek keberadaan tabel
                    java.sql.DatabaseMetaData dbm = conn.getMetaData();
                    try (java.sql.ResultSet tables = dbm.getTables(null, null, "profil_toko", null)) {

                        if (tables.next()) {
                            // Tabel "profil_toko" ditemukan! Sekarang cek apakah ada isinya
                            try (java.sql.Statement stmt = conn.createStatement(); java.sql.ResultSet rsData = stmt.executeQuery("SELECT COUNT(*) AS total FROM profil_toko")) {

                                if (rsData.next() && rsData.getInt("total") > 0) {
                                    tokoSudahSetup = true; // Toko fix sudah di-setup!
                                }
                            }
                        }
                    }

                } catch (Exception e) {
                    // Jika terjadi error, tampilkan di output (bawah) agar kita tahu apa masalahnya
                    System.out.println("Error Gatekeeper: " + e.getMessage());
                }

                // LOGIKA ROUTING:
                if (tokoSudahSetup) {
                    // Jika sudah ada data toko, langsung buka LOGIN
                    new LoginFrame().setVisible(true);
                } else {
                    // Jika masih kosong/baru instal, buka SETUP TOKO
                    new SetupTokoFrame().setVisible(true);
                }
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnLogin;
    private toko.aplikasipos.CustomRoundedPanel customRoundedPanel1;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JPasswordField txtPassword;
    private javax.swing.JTextField txtUsername;
    // End of variables declaration//GEN-END:variables
}
