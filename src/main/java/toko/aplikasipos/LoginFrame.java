
package toko.aplikasipos;

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
    
public LoginFrame() {
    initComponents();

}



    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jLabel1 = new javax.swing.JLabel();
        txtUsername = new javax.swing.JTextField();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        btnLogin = new javax.swing.JButton();
        txtPassword = new javax.swing.JPasswordField();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        jLabel1.setText("User Name");

        jLabel2.setText("Password");

        jLabel3.setText("LOGIN KASIR");

        btnLogin.setText("Login");
        btnLogin.addActionListener(this::btnLoginActionPerformed);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(37, 37, 37)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel1)
                    .addComponent(jLabel2))
                .addGap(49, 49, 49)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jLabel3)
                    .addComponent(btnLogin, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(txtUsername, javax.swing.GroupLayout.DEFAULT_SIZE, 193, Short.MAX_VALUE)
                    .addComponent(txtPassword))
                .addContainerGap(63, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(24, 24, 24)
                .addComponent(jLabel3)
                .addGap(42, 42, 42)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(txtUsername, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(26, 26, 26)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(txtPassword, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(31, 31, 31)
                .addComponent(btnLogin)
                .addContainerGap(94, Short.MAX_VALUE))
        );

        pack();
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
        
        boolean isLoginSukses = false; // Variabel penanda status login
        
        // BLOK 1: HANYA UNTUK MENGECEK DATABASE (Aman dari SQLite Busy)
        try (Connection conn = DriverManager.getConnection(url);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, username);
            pstmt.setString(2, password);
            
            // Masukkan ResultSet ke dalam try agar langsung ditutup
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    isLoginSukses = true;
                    kasirAktif = rs.getString("username"); // Simpan nama user
                }
            }
            
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error Login: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            return; // Hentikan eksekusi jika database error
        }
        
        // BLOK 2: BUKA FORM BARU (Dijalankan SETELAH koneksi SQLite di atas tertutup!)
        if (isLoginSukses) {
            JOptionPane.showMessageDialog(this, "Selamat Datang, " + kasirAktif + "!");
            
            // Buka MainFrame
            MainFrame utama = new MainFrame();
            utama.setVisible(true);
            
            // Tutup form login
            this.dispose(); 
        } else {
            JOptionPane.showMessageDialog(this, "Username atau Password salah!", "Akses Ditolak", JOptionPane.ERROR_MESSAGE);
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
                            try (java.sql.Statement stmt = conn.createStatement();
                                 java.sql.ResultSet rsData = stmt.executeQuery("SELECT COUNT(*) AS total FROM profil_toko")) {
                                
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
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JPasswordField txtPassword;
    private javax.swing.JTextField txtUsername;
    // End of variables declaration//GEN-END:variables
}
