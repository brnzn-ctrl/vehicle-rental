package ui;

import javax.swing.Icon;
import java.awt.*;
import java.awt.geom.*;

/** Small line icons drawn with Java2D (no image files needed). Each icon lives in a 24 x 24 box. */
public final class Icons {
    private Icons() { }

    public static void paint(String name, Graphics2D g0, float x, float y, float size, Color color) {
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g.translate(x, y);
        g.scale(size / 24f, size / 24f);
        g.setColor(color);
        g.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        switch (name) {
            case "dashboard":
                g.draw(new RoundRectangle2D.Float(3.5f, 3.5f, 7, 7, 2.5f, 2.5f));
                g.draw(new RoundRectangle2D.Float(13.5f, 3.5f, 7, 7, 2.5f, 2.5f));
                g.draw(new RoundRectangle2D.Float(3.5f, 13.5f, 7, 7, 2.5f, 2.5f));
                g.draw(new RoundRectangle2D.Float(13.5f, 13.5f, 7, 7, 2.5f, 2.5f));
                break;
            case "car":
                Path2D car = new Path2D.Float();
                car.moveTo(3, 15); car.lineTo(3, 12); car.lineTo(5.2f, 7.5f); car.quadTo(5.6f, 6.8f, 6.5f, 6.8f);
                car.lineTo(17.5f, 6.8f); car.quadTo(18.4f, 6.8f, 18.8f, 7.5f); car.lineTo(21, 12); car.lineTo(21, 15);
                car.lineTo(19.5f, 15); car.moveTo(4.5f, 15); car.lineTo(3, 15);
                g.draw(car);
                g.draw(new Line2D.Float(3.2f, 12, 20.8f, 12));
                g.draw(new Line2D.Float(7.5f, 15, 16.5f, 15));
                g.draw(new Ellipse2D.Float(4.6f, 14.2f, 3.4f, 3.4f));
                g.draw(new Ellipse2D.Float(16, 14.2f, 3.4f, 3.4f));
                break;
            case "users":
                g.draw(new Ellipse2D.Float(8, 4, 6, 6));
                g.draw(new Arc2D.Float(4, 13, 14, 12, 0, 180, Arc2D.OPEN));
                g.draw(new Arc2D.Float(14.2f, 4.5f, 5, 5, 270, 180, Arc2D.OPEN));
                g.draw(new Arc2D.Float(15, 13, 6, 8, 0, 90, Arc2D.OPEN));
                break;
            case "calendar":
                g.draw(new RoundRectangle2D.Float(3.5f, 5, 17, 15.5f, 3.5f, 3.5f));
                g.draw(new Line2D.Float(3.5f, 10, 20.5f, 10));
                g.draw(new Line2D.Float(8, 3, 8, 6.5f));
                g.draw(new Line2D.Float(16, 3, 16, 6.5f));
                g.fill(new Ellipse2D.Float(7.3f, 13, 2.2f, 2.2f));
                g.fill(new Ellipse2D.Float(11, 13, 2.2f, 2.2f));
                break;
            case "key":
                g.draw(new Ellipse2D.Float(3.5f, 8.5f, 8, 8));
                g.draw(new Line2D.Float(11, 11.5f, 20.5f, 4));
                g.draw(new Line2D.Float(17, 7, 19.5f, 9.5f));
                g.fill(new Ellipse2D.Float(6.3f, 11.3f, 2.4f, 2.4f));
                break;
            case "return":
                g.draw(new Line2D.Float(9, 6, 4, 11));
                g.draw(new Line2D.Float(4, 11, 9, 16));
                Path2D ret = new Path2D.Float();
                ret.moveTo(4.5f, 11); ret.lineTo(15, 11); ret.quadTo(20, 11, 20, 16); ret.quadTo(20, 20, 15, 20); ret.lineTo(10, 20);
                g.draw(ret);
                break;
            case "card":
                g.draw(new RoundRectangle2D.Float(2.5f, 5, 19, 14, 3.5f, 3.5f));
                g.draw(new Line2D.Float(2.5f, 10, 21.5f, 10));
                g.draw(new Line2D.Float(6, 15, 10, 15));
                break;
            case "box":
                Path2D box = new Path2D.Float();
                box.moveTo(12, 3); box.lineTo(20.5f, 7.5f); box.lineTo(20.5f, 16.5f); box.lineTo(12, 21); box.lineTo(3.5f, 16.5f); box.lineTo(3.5f, 7.5f); box.closePath();
                g.draw(box);
                g.draw(new Line2D.Float(3.8f, 7.7f, 12, 12.2f));
                g.draw(new Line2D.Float(20.2f, 7.7f, 12, 12.2f));
                g.draw(new Line2D.Float(12, 12.2f, 12, 20.8f));
                break;
            case "wallet":
                g.draw(new RoundRectangle2D.Float(3, 6.5f, 18, 13, 3.5f, 3.5f));
                Path2D top = new Path2D.Float();
                top.moveTo(5, 6.5f); top.lineTo(16, 3.8f); top.quadTo(17.5f, 3.5f, 17.8f, 5); top.lineTo(17.8f, 6.5f);
                g.draw(top);
                g.draw(new RoundRectangle2D.Float(14.5f, 11, 6.5f, 4.2f, 2.5f, 2.5f));
                g.fill(new Ellipse2D.Float(16.6f, 12.2f, 1.8f, 1.8f));
                break;
            case "chart":
                g.draw(new Line2D.Float(4, 4, 4, 20));
                g.draw(new Line2D.Float(4, 20, 21, 20));
                g.draw(new RoundRectangle2D.Float(7.5f, 12, 3, 5.5f, 1, 1));
                g.draw(new RoundRectangle2D.Float(12.5f, 8, 3, 9.5f, 1, 1));
                g.draw(new RoundRectangle2D.Float(17.5f, 5, 3, 12.5f, 1, 1));
                break;
            case "tag":
                Path2D tag = new Path2D.Float();
                tag.moveTo(3.5f, 4.5f); tag.lineTo(12, 3.5f); tag.lineTo(20.5f, 12); tag.lineTo(12, 20.5f); tag.lineTo(3.5f, 12); tag.closePath();
                g.draw(tag);
                g.fill(new Ellipse2D.Float(7, 7, 2.6f, 2.6f));
                break;
            case "percent":
                g.draw(new Line2D.Float(19, 5, 5, 19));
                g.draw(new Ellipse2D.Float(4.5f, 4.5f, 5, 5));
                g.draw(new Ellipse2D.Float(14.5f, 14.5f, 5, 5));
                break;
            case "shield":
                Path2D sh = new Path2D.Float();
                sh.moveTo(12, 3); sh.lineTo(19.5f, 6); sh.lineTo(19.5f, 12); sh.quadTo(19.5f, 18, 12, 21); sh.quadTo(4.5f, 18, 4.5f, 12); sh.lineTo(4.5f, 6); sh.closePath();
                g.draw(sh);
                g.draw(new Line2D.Float(8.8f, 12, 11, 14.2f));
                g.draw(new Line2D.Float(11, 14.2f, 15.4f, 9.6f));
                break;
            case "peso":
                g.setStroke(new BasicStroke(1.9f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.draw(new Line2D.Float(8.5f, 4.5f, 8.5f, 19.5f));
                Path2D p = new Path2D.Float();
                p.moveTo(8.5f, 5); p.lineTo(13, 5); p.quadTo(17.5f, 5, 17.5f, 9); p.quadTo(17.5f, 13, 13, 13); p.lineTo(8.5f, 13);
                g.draw(p);
                g.draw(new Line2D.Float(5.5f, 8, 16.5f, 8));
                g.draw(new Line2D.Float(5.5f, 10.8f, 16.5f, 10.8f));
                break;
            case "trend":
                Path2D tr = new Path2D.Float();
                tr.moveTo(3, 17); tr.lineTo(9, 11); tr.lineTo(13, 15); tr.lineTo(21, 6.5f);
                g.draw(tr);
                g.draw(new Line2D.Float(15.5f, 6.5f, 21, 6.5f));
                g.draw(new Line2D.Float(21, 6.5f, 21, 12));
                break;
            case "trend-down":
                Path2D td = new Path2D.Float();
                td.moveTo(3, 7); td.lineTo(9, 13); td.lineTo(13, 9); td.lineTo(21, 17.5f);
                g.draw(td);
                g.draw(new Line2D.Float(15.5f, 17.5f, 21, 17.5f));
                g.draw(new Line2D.Float(21, 17.5f, 21, 12));
                break;
            case "clock":
                g.draw(new Ellipse2D.Float(3.5f, 3.5f, 17, 17));
                g.draw(new Line2D.Float(12, 7.5f, 12, 12));
                g.draw(new Line2D.Float(12, 12, 15.2f, 14));
                break;
            case "alert":
                Path2D al = new Path2D.Float();
                al.moveTo(12, 3.8f); al.lineTo(21, 19.5f); al.lineTo(3, 19.5f); al.closePath();
                g.draw(al);
                g.draw(new Line2D.Float(12, 9.5f, 12, 14));
                g.fill(new Ellipse2D.Float(11.1f, 15.9f, 1.8f, 1.8f));
                break;
            case "wrench":
                g.draw(new Line2D.Float(14, 10, 4.5f, 19.5f));
                g.draw(new Arc2D.Float(10.5f, 3.5f, 9, 9, 120, 290, Arc2D.OPEN));
                break;
            case "check":
                g.draw(new Line2D.Float(5, 12.5f, 10, 17.5f));
                g.draw(new Line2D.Float(10, 17.5f, 19, 7));
                break;
            case "logout":
                Path2D lo = new Path2D.Float();
                lo.moveTo(10, 4); lo.lineTo(6, 4); lo.quadTo(4, 4, 4, 6); lo.lineTo(4, 18); lo.quadTo(4, 20, 6, 20); lo.lineTo(10, 20);
                g.draw(lo);
                g.draw(new Line2D.Float(9.5f, 12, 20, 12));
                g.draw(new Line2D.Float(16, 8, 20, 12));
                g.draw(new Line2D.Float(16, 16, 20, 12));
                break;
            case "power":
                g.draw(new Arc2D.Float(4.5f, 5, 15, 15, 120, 300, Arc2D.OPEN));
                g.draw(new Line2D.Float(12, 3, 12, 11));
                break;
            case "refresh":
                g.draw(new Arc2D.Float(4.5f, 4.5f, 15, 15, 40, 270, Arc2D.OPEN));
                g.draw(new Line2D.Float(17.5f, 3.5f, 18.6f, 7.4f));
                g.draw(new Line2D.Float(18.6f, 7.4f, 14.6f, 8.3f));
                break;
            case "arrow-right":
                g.draw(new Line2D.Float(5, 12, 19, 12));
                g.draw(new Line2D.Float(13, 6, 19, 12));
                g.draw(new Line2D.Float(13, 18, 19, 12));
                break;
            default:
                g.draw(new Ellipse2D.Float(5, 5, 14, 14));
        }
        g.dispose();
    }

    /** An icon centred in a soft round badge, e.g. the circles on the dashboard cards. */
    public static void badge(String name, Graphics2D g0, int x, int y, int d, Color bg, Color fg) {
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(bg);
        g.fillOval(x, y, d, d);
        float s = d * 0.5f;
        paint(name, g, x + (d - s) / 2f, y + (d - s) / 2f, s, fg);
        g.dispose();
    }

    public static Icon icon(String name, int size, Color color) {
        return new Icon() {
            @Override public void paintIcon(Component c, Graphics g, int x, int y) { Icons.paint(name, (Graphics2D) g, x, y, size, color); }
            @Override public int getIconWidth() { return size; }
            @Override public int getIconHeight() { return size; }
        };
    }
}
