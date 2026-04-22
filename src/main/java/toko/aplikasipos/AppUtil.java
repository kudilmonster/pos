package toko.aplikasipos;

import java.net.URL;
import javax.swing.ImageIcon;
import javax.swing.JFrame;

public class AppUtil {
    
    // Tuliskan lokasi gambar logo Anda di sini (Pastikan namanya benar!)
    private static final String LOKASI_LOGO = "/icon/logofk.png";

    // Fungsi statis ini bisa dipanggil dari Frame mana saja
    public static void setWindowIcon(JFrame frame) {
        try {
            // Memuat gambar dari folder resources
            URL urlIcon = AppUtil.class.getResource(LOKASI_LOGO);
            
            if (urlIcon != null) {
                ImageIcon iconAplikasi = new ImageIcon(urlIcon);
                // Memasang gambar ke bingkai dan taskbar
                frame.setIconImage(iconAplikasi.getImage());
            } else {
                System.out.println("Gagal: Gambar ikon tidak ditemukan di " + LOKASI_LOGO);
            }
        } catch (Exception e) {
            System.out.println("Error saat memasang ikon: " + e.getMessage());
        }
    }
    
}