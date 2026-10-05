package util;





import javax.imageio.ImageIO;

import java.awt.*;

import java.awt.image.BufferedImage;

import java.io.File;

import java.io.IOException;



/** Resize/crop helpers so every "upload photo" screen in the app behaves the same way. */

public final class ImageUtil {



    private ImageUtil() { }



    /** Like CSS object-fit:cover — fills the target box exactly, cropping any excess. */

    public static BufferedImage coverFit(BufferedImage src, int targetW, int targetH) {

        double srcRatio = (double) src.getWidth() / src.getHeight();

        double targetRatio = (double) targetW / targetH;



        int cropW, cropH, x, y;

        if (srcRatio > targetRatio) {

            cropH = src.getHeight();

            cropW = (int) (cropH * targetRatio);

            x = (src.getWidth() - cropW) / 2;

            y = 0;

        } else {

            cropW = src.getWidth();

            cropH = (int) (cropW / targetRatio);

            x = 0;

            y = (src.getHeight() - cropH) / 2;

        }



        BufferedImage cropped = src.getSubimage(x, y, cropW, cropH);

        return resize(cropped, targetW, targetH);

    }



    /** Shrinks so the longest side is at most maxSide, keeping the shape (no stretching). Smaller images are returned as they are. */

    public static BufferedImage fitWithin(BufferedImage src, int maxSide) {

        int w = src.getWidth(), h = src.getHeight();

        if (Math.max(w, h) <= maxSide) return src;

        double k = (double) maxSide / Math.max(w, h);

        return resize(src, Math.max(1, (int) Math.round(w * k)), Math.max(1, (int) Math.round(h * k)));

    }



    public static BufferedImage resize(BufferedImage src, int w, int h) {

        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);

        Graphics2D g = out.createGraphics();

        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        g.drawImage(src, 0, 0, w, h, null);

        g.dispose();

        return out;

    }



    /** Crops to an arbitrary rectangle the user dragged (see CropDialog). */

    public static BufferedImage crop(BufferedImage src, Rectangle r) {

        Rectangle safe = r.intersection(new Rectangle(0, 0, src.getWidth(), src.getHeight()));

        if (safe.width <= 0 || safe.height <= 0) return src;

        return src.getSubimage(safe.x, safe.y, safe.width, safe.height);

    }



    /** Saves under {appFolder}/VehicleImages/{filename} and returns the filename to store in the DB. */

    public static String saveToDisk(BufferedImage img, String folder, String filename) throws IOException {

        File dir = new File(folder);

        if (!dir.exists()) dir.mkdirs();

        File dest = new File(dir, filename);

        ImageIO.write(img, "png", dest);

        return filename;

    }



    public static BufferedImage loadFromDisk(String folder, String filename) throws IOException {

        File f = new File(folder, filename);

        return f.exists() ? ImageIO.read(f) : null;

    }

}

