package toko.aplikasipos;

import java.awt.Color;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Statement;
import javax.swing.JOptionPane;

public class SetupTokoFrame extends javax.swing.JFrame {

    private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(SetupTokoFrame.class.getName());


    public SetupTokoFrame() {
        initComponents();
        this.setLocationRelativeTo(null); // (Opsional) Agar form di tengah layar
        setBackground(new Color(0, 0, 0, 0));
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        customRoundedPanel1 = new toko.aplikasipos.CustomRoundedPanel();
        jLabel1 = new javax.swing.JLabel();
        txtNamaToko = new javax.swing.JTextField();
        jScrollPane1 = new javax.swing.JScrollPane();
        txtAlamat = new javax.swing.JTextArea();
        txtAdmin = new javax.swing.JTextField();
        txtPassword = new javax.swing.JPasswordField();
        btnSimpanSetup = new javax.swing.JButton();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setUndecorated(true);
        setResizable(false);

        customRoundedPanel1.setcolorEnd(new java.awt.Color(25, 42, 86));
        customRoundedPanel1.setcolorStart(new java.awt.Color(0, 168, 255));

        jLabel1.setFont(new java.awt.Font("Segoe UI Black", 0, 24)); // NOI18N
        jLabel1.setForeground(new java.awt.Color(255, 255, 255));
        jLabel1.setText("Setup");

        txtAlamat.setColumns(20);
        txtAlamat.setRows(5);
        jScrollPane1.setViewportView(txtAlamat);

        txtPassword.setText("jPasswordField1");
        txtPassword.addActionListener(this::txtPasswordActionPerformed);

        btnSimpanSetup.setFont(new java.awt.Font("Segoe UI Semibold", 0, 12)); // NOI18N
        btnSimpanSetup.setText("Simpan");
        btnSimpanSetup.addActionListener(this::btnSimpanSetupActionPerformed);

        jLabel2.setForeground(new java.awt.Color(255, 255, 255));
        jLabel2.setText("Nama Toko");

        jLabel3.setForeground(new java.awt.Color(255, 255, 255));
        jLabel3.setText("Alamat Toko");

        jLabel4.setForeground(new java.awt.Color(255, 255, 255));
        jLabel4.setText("Nama Pemilik/Admin");

        jLabel5.setForeground(new java.awt.Color(255, 255, 255));
        jLabel5.setText("Pasword");

        javax.swing.GroupLayout customRoundedPanel1Layout = new javax.swing.GroupLayout(customRoundedPanel1);
        customRoundedPanel1.setLayout(customRoundedPanel1Layout);
        customRoundedPanel1Layout.setHorizontalGroup(
            customRoundedPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(customRoundedPanel1Layout.createSequentialGroup()
                .addGap(68, 68, 68)
                .addGroup(customRoundedPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jLabel2)
                    .addComponent(jLabel3)
                    .addComponent(jLabel4)
                    .addComponent(jLabel5))
                .addGap(32, 32, 32)
                .addGroup(customRoundedPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel1)
                    .addGroup(customRoundedPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                        .addComponent(jScrollPane1)
                        .addComponent(txtNamaToko)
                        .addComponent(txtAdmin)
                        .addComponent(txtPassword, javax.swing.GroupLayout.PREFERRED_SIZE, 234, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(btnSimpanSetup))
                .addContainerGap(93, Short.MAX_VALUE))
        );
        customRoundedPanel1Layout.setVerticalGroup(
            customRoundedPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(customRoundedPanel1Layout.createSequentialGroup()
                .addGap(69, 69, 69)
                .addComponent(jLabel2)
                .addGap(18, 18, 18)
                .addComponent(jLabel3)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 83, Short.MAX_VALUE)
                .addComponent(jLabel4)
                .addGap(18, 18, 18)
                .addComponent(jLabel5)
                .addGap(74, 74, 74))
            .addGroup(customRoundedPanel1Layout.createSequentialGroup()
                .addGap(20, 20, 20)
                .addComponent(jLabel1)
                .addGap(18, 18, 18)
                .addComponent(txtNamaToko, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 86, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(txtAdmin, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(txtPassword, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(btnSimpanSetup)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(customRoundedPanel1, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(customRoundedPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
        );

        pack();
        setLocationRelativeTo(null);
    }// </editor-fold>//GEN-END:initComponents

    private void btnSimpanSetupActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSimpanSetupActionPerformed
        String namaToko = txtNamaToko.getText().trim();
        String alamat = txtAlamat.getText().trim();
        String admin = txtAdmin.getText().trim();
        String password = new String(txtPassword.getPassword());

        if (namaToko.isEmpty() || alamat.isEmpty() || admin.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Semua data wajib diisi!", "Peringatan", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String url = "jdbc:sqlite:pos_db.db";

        try (Connection conn = DriverManager.getConnection(url); Statement stmt = conn.createStatement()) {

            // 1. Buat tabel profil_toko dan simpan data toko
            stmt.execute("CREATE TABLE IF NOT EXISTS profil_toko (id INTEGER PRIMARY KEY, nama_toko TEXT, alamat TEXT)");
            String sqlToko = "INSERT INTO profil_toko (nama_toko, alamat) VALUES (?, ?)";
            try (PreparedStatement pstToko = conn.prepareStatement(sqlToko)) {
                pstToko.setString(1, namaToko);
                pstToko.setString(2, alamat);
                pstToko.executeUpdate();
            }

// 2. Buat tabel users dengan tambahan kolom 'role'
            stmt.execute("CREATE TABLE IF NOT EXISTS users (id_user INTEGER PRIMARY KEY AUTOINCREMENT, username TEXT UNIQUE, password TEXT, role TEXT)");

            // Simpan akun pertama sebagai 'Admin' secara otomatis
            String sqlUser = "INSERT INTO users (username, password, role) VALUES (?, ?, 'Admin')";
            try (PreparedStatement pstUser = conn.prepareStatement(sqlUser)) {
                pstUser.setString(1, admin);
                pstUser.setString(2, PasswordUtil.hashPassword(password));
                pstUser.executeUpdate();
            }

            JOptionPane.showMessageDialog(this, "Setup Toko Berhasil!\nSilakan Login dengan akun yang baru dibuat.");

            // 3. Buka form login dan tutup form setup
            new LoginFrame().setVisible(true);
            this.dispose();

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error Setup: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }//GEN-LAST:event_btnSimpanSetupActionPerformed

    private void txtPasswordActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtPasswordActionPerformed
       btnSimpanSetup.doClick();
    }//GEN-LAST:event_txtPasswordActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnSimpanSetup;
    private toko.aplikasipos.CustomRoundedPanel customRoundedPanel1;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTextField txtAdmin;
    private javax.swing.JTextArea txtAlamat;
    private javax.swing.JTextField txtNamaToko;
    private javax.swing.JPasswordField txtPassword;
    // End of variables declaration//GEN-END:variables
}
