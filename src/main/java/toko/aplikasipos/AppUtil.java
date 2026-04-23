package toko.aplikasipos;

import java.net.URL;
import javax.swing.AbstractButton;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;

public class AppUtil {
    
    // Tuliskan lokasi gambar logo Anda di sini (Pastikan namanya benar!)
    private static final String LOKASI_LOGO = "/icon/logofk.png";
    private static final String ICON_PATH_PROPERTY = "__apputil_icon_path";

    private static String normalizeResourcePath(String resourcePath) {
        if (resourcePath == null || resourcePath.isBlank()) {
            return null;
        }
        return resourcePath.startsWith("/") ? resourcePath : "/" + resourcePath;
    }

    public static ImageIcon loadIcon(String resourcePath) {
        String normalized = normalizeResourcePath(resourcePath);
        if (normalized == null) {
            System.out.println("Gagal: path icon kosong.");
            return null;
        }

        try {
            URL urlIcon = AppUtil.class.getResource(normalized);
            if (urlIcon == null) {
                System.out.println("Gagal: Gambar ikon tidak ditemukan di " + normalized);
                return null;
            }
            return new ImageIcon(urlIcon);
        } catch (Exception e) {
            System.out.println("Error saat memasang ikon: " + e.getMessage());
            return null;
        }
    }

    public static void setLabelIcon(JLabel label, String resourcePath) {
        if (label == null) {
            return;
        }
        String normalized = normalizeResourcePath(resourcePath);
        if (normalized == null) {
            return;
        }
        Object currentPath = label.getClientProperty(ICON_PATH_PROPERTY);
        if (normalized.equals(currentPath)) {
            return;
        }

        ImageIcon icon = loadIcon(normalized);
        if (icon != null) {
            label.setIcon(icon);
            label.putClientProperty(ICON_PATH_PROPERTY, normalized);
        }
    }

    public static void setButtonIcon(AbstractButton button, String resourcePath) {
        if (button == null) {
            return;
        }
        String normalized = normalizeResourcePath(resourcePath);
        if (normalized == null) {
            return;
        }
        Object currentPath = button.getClientProperty(ICON_PATH_PROPERTY);
        if (normalized.equals(currentPath)) {
            return;
        }

        ImageIcon icon = loadIcon(normalized);
        if (icon != null) {
            button.setIcon(icon);
            button.putClientProperty(ICON_PATH_PROPERTY, normalized);
        }
    }

    // Fungsi statis ini bisa dipanggil dari Frame mana saja
    public static void setWindowIcon(JFrame frame) {
        if (frame == null) {
            return;
        }
        ImageIcon iconAplikasi = loadIcon(LOKASI_LOGO);
        if (iconAplikasi != null) {
            // Memasang gambar ke bingkai dan taskbar
            frame.setIconImage(iconAplikasi.getImage());
        }
    }
    
}
