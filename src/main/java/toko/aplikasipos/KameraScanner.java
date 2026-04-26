package toko.aplikasipos;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamPanel;
import com.google.zxing.*;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;

import javax.swing.*;
import java.awt.image.BufferedImage;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Consumer;

public class KameraScanner extends JDialog {

    private Webcam webcam;
    private WebcamPanel panel;
    private volatile boolean isScanning = true;
    private volatile boolean pauseScanning = false;
    private final Consumer<String> onScanSuccess;

    public KameraScanner(JFrame parent, Consumer<String> onScanSuccess) {
        super(parent, "Continuous Scan Barcode", true);
        this.onScanSuccess = onScanSuccess;
        initKamera();
    }

    public void setPauseScanning(boolean pause) {
        this.pauseScanning = pause;
    }

    // ================= INIT =================
    private void initKamera() {
        try {
            for (Webcam w : Webcam.getWebcams()) {
                String name = w.getName().toLowerCase();
                if (name.contains("iriun")) {
                    webcam = w;
                    break;
                }
            }

            if (webcam == null) {
                webcam = Webcam.getDefault();
            }

            if (webcam == null) {
                JOptionPane.showMessageDialog(this, "Kamera tidak ditemukan!");
                dispose();
                return;
            }

            webcam.setViewSize(new java.awt.Dimension(320, 240));
            webcam.open();

            panel = new WebcamPanel(webcam);
            panel.setFPSDisplayed(true);
            panel.setMirrored(false);

            add(panel);
            pack();
            setLocationRelativeTo(null);

            startScannerThread();

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error kamera: " + e.getMessage());
            dispose();
        }
    }

    // ================= SCANNER =================
    private void startScannerThread() {
        Map<DecodeHintType, Object> hints = new EnumMap<>(DecodeHintType.class);
        hints.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);
        hints.put(DecodeHintType.POSSIBLE_FORMATS, java.util.Arrays.asList(
                BarcodeFormat.EAN_13,
                BarcodeFormat.EAN_8,
                BarcodeFormat.CODE_128
        ));

        Thread thread = new Thread(() -> {
            MultiFormatReader reader = new MultiFormatReader();
            reader.setHints(hints);

            while (isScanning) {
                try {
                    Thread.sleep(150);

                    if (pauseScanning) continue;

                    if (webcam == null || !webcam.isOpen()) continue;

                    BufferedImage image = webcam.getImage();
                    if (image == null) continue;

                    BufferedImage cropped = cropCenter(image);
                    BufferedImage enhanced = enhance(cropped);

                    BinaryBitmap bitmap = new BinaryBitmap(
                            new HybridBinarizer(
                                    new BufferedImageLuminanceSource(enhanced)
                            )
                    );

                    try {
                        Result result = reader.decode(bitmap);

                        if (result != null) {
                            String text = result.getText();
                            java.awt.Toolkit.getDefaultToolkit().beep();
                            
                            // Kirim hasil scan ke KasirFrame secara otomatis
                             if (onScanSuccess != null) {
                                 onScanSuccess.accept(text);
                             }
                             
                             // Jeda agar tidak scan berulang kali untuk barang yang sama
                             Thread.sleep(2000); 
                        }

                    } catch (NotFoundException e) {
                        // normal
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        thread.setDaemon(true);
        thread.start();
    }

    private BufferedImage enhance(BufferedImage image) {
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int rgb = image.getRGB(x, y);
                int r = (rgb >> 16) & 0xff;
                int g = (rgb >> 8) & 0xff;
                int b = rgb & 0xff;
                int gray = (r + g + b) / 3;
                gray = gray > 120 ? 255 : 0;
                int newPixel = (gray << 16) | (gray << 8) | gray;
                image.setRGB(x, y, newPixel);
            }
        }
        return image;
    }

    private BufferedImage cropCenter(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        int cropWidth = (int) (width * 0.7);
        int cropHeight = (int) (height * 0.5);
        int x = (width - cropWidth) / 2;
        int y = (height - cropHeight) / 2;
        return image.getSubimage(x, y, cropWidth, cropHeight);
    }

    @Override
    public void dispose() {
        isScanning = false;
        if (webcam != null && webcam.isOpen()) {
            webcam.close();
        }
        super.dispose();
    }
}