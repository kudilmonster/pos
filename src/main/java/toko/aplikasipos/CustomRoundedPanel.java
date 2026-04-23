package toko.aplikasipos;


import java.awt.*;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import javax.swing.*;

public class CustomRoundedPanel extends JPanel {
// Variabel untuk menyimpan posisi mouse

    private int mouseX, mouseY;
    /*===================================
    mouseX = e.getX();
    mouseY = e.getY();
    =====================================
    int x = e.getXOnScreen();
    int y = e.getYOnScreen();
    setLocation(x - mouseX, y - mouseY);
     ====================================*/
    
    // --- Variabel Warna dan Gradient ---
    private Color colorStart = new Color(106, 17, 203, 180);
    private Color colorEnd = new Color(37, 117, 252, 180);
    private boolean vertical = true;
    private int cornerRadius = 30;

    // --- Variabel Pilihan Sudut ---
    private boolean RoundtopLeft = true;
    private boolean RoundtopRight = true;
    private boolean RoundbottomLeft = true;
    private boolean RoundbottomRight = true;

    // --- Variabel Image, Opacity, dan Aspect Ratio ---
    private Icon image;
    private float imageOpacity = 0.5f;
    private boolean keepAspectRatio = true; // Fitur baru: menjaga proporsi gambar

    public CustomRoundedPanel() {
        setOpaque(false);
    }

    // --- GETTER & SETTER ---
    public Color getcolorStart() {
        return colorStart;
    }

    public void setcolorStart(Color colorStart) {
        this.colorStart = colorStart;
        repaint();
    }

    public Color getcolorEnd() {
        return colorEnd;
    }

    public void setcolorEnd(Color colorEnd) {
        this.colorEnd = colorEnd;
        repaint();
    }

    public boolean isVertical() {
        return vertical;
    }

    public void setVertical(boolean vertical) {
        this.vertical = vertical;
        repaint();
    }

    public int getCornerRadius() {
        return cornerRadius;
    }

    public void setCornerRadius(int radius) {
        this.cornerRadius = radius;
        repaint();
    }

    public boolean isTopLeftRound() {
        return RoundtopLeft;
    }

    public void setTopLeftRound(boolean topLeftRound) {
        this.RoundtopLeft = topLeftRound;
        repaint();
    }

    public boolean isTopRightRound() {
        return RoundtopRight;
    }

    public void setTopRightRound(boolean topRightRound) {
        this.RoundtopRight = topRightRound;
        repaint();
    }

    public boolean isBottomLeftRound() {
        return RoundbottomLeft;
    }

    public void setBottomLeftRound(boolean bottomLeftRound) {
        this.RoundbottomLeft = bottomLeftRound;
        repaint();
    }

    public boolean isBottomRightRound() {
        return RoundbottomRight;
    }

    public void setBottomRightRound(boolean bottomRightRound) {
        this.RoundbottomRight = bottomRightRound;
        repaint();
    }

    public Icon getImage() {
        return image;
    }

    public void setImage(Icon image) {
        this.image = image;
        repaint();
    }

    public float getImageOpacity() {
        return imageOpacity;
    }

    public void setImageOpacity(float imageOpacity) {
        this.imageOpacity = Math.max(0.0f, Math.min(1.0f, imageOpacity));
        repaint();
    }

    public boolean isKeepAspectRatio() {
        return keepAspectRatio;
    }

    public void setKeepAspectRatio(boolean keepAspectRatio) {
        this.keepAspectRatio = keepAspectRatio;
        repaint();
    }

    // Helper untuk mengubah Icon menjadi Image
    private Image toImage(Icon icon) {
        if (icon instanceof ImageIcon) {
            return ((ImageIcon) icon).getImage();
        } else if (icon != null) {
            BufferedImage buf = new BufferedImage(icon.getIconWidth(), icon.getIconHeight(), BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = buf.createGraphics();
            icon.paintIcon(this, g, 0, 0);
            g.dispose();
            return buf;
        }
        return null;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g.create();

        // Rendering Hints untuk kualitas terbaik
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

        int w = getWidth();
        int h = getHeight();
        int r = cornerRadius;

        // 1. Buat Path Rounded
        Path2D.Float path = new Path2D.Float();
        if (RoundtopLeft) {
            path.moveTo(0, r);
            path.quadTo(0, 0, r, 0);
        } else {
            path.moveTo(0, 0);
        }
        if (RoundtopRight) {
            path.lineTo(w - r, 0);
            path.quadTo(w, 0, w, r);
        } else {
            path.lineTo(w, 0);
        }
        if (RoundbottomRight) {
            path.lineTo(w, h - r);
            path.quadTo(w, h, w - r, h);
        } else {
            path.lineTo(w, h);
        }
        if (RoundbottomLeft) {
            path.lineTo(r, h);
            path.quadTo(0, h, 0, h - r);
        } else {
            path.lineTo(0, h);
        }
        path.closePath();

        // 2. Gambar Background Gradient
        GradientPaint gp;
        if (vertical) {
            gp = new GradientPaint(0, 0, colorStart, 0, h, colorEnd);
        } else {
            gp = new GradientPaint(0, 0, colorStart, w, 0, colorEnd);
        }
        g2d.setPaint(gp);
        g2d.fill(path);

        // 3. Gambar Image dengan Clipping, Opacity, dan Aspect Ratio
        if (image != null) {
            Image img = toImage(image);
            if (img != null) {
                g2d.setClip(path);
                g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, imageOpacity));

                int imgW = img.getWidth(null);
                int imgH = img.getHeight(null);

                if (keepAspectRatio && imgW > 0 && imgH > 0) {
                    // Logika Center Crop (Menjaga proporsi)
                    double scale = Math.max((double) w / imgW, (double) h / imgH);
                    int finalW = (int) (imgW * scale);
                    int finalH = (int) (imgH * scale);
                    int x = (w - finalW) / 2;
                    int y = (h - finalH) / 2;
                    g2d.drawImage(img, x, y, finalW, finalH, this);
                } else {
                    // Gambar ditarik paksa memenuhi panel (Gepeng jika ratio beda)
                    g2d.drawImage(img, 0, 0, w, h, this);
                }
            }
        }

        g2d.dispose();
    }
}

