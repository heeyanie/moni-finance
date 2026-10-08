package app;

import javax.swing.Icon;
import java.awt.*;
import java.awt.geom.*;

/**
 * Small vector icons drawn with Java2D on a 24 x 24 grid.
 *
 * Swing has no built-in icon set, and emoji look different (or show as boxes) on every
 * computer. Drawing the shapes ourselves keeps them sharp at any size and needs no image files.
 */
final class Icons {
    private Icons() {}

    enum Name {
        // navigation (solid)
        HOME, LIST, PIE, PIGGY, BARS, GEAR,
        // cards and buttons (outline)
        WALLET, PIGGY_LINE, SWAP, RECEIPT, SEARCH, CALENDAR, BELL, PLUS, CASH, COINS,
        CHEVRON_DOWN, CHEVRON_LEFT, CHEVRON_RIGHT, INFO, SLIDERS, LOGOUT,
        // spending categories
        UTENSILS, BUS, CAP, GAMEPAD, BAG, BOLT, HEART, TAG,
        // decoration
        SPROUT
    }

    static Icon of(Name name, int size, Color color) {
        return new Glyph(name, size, color);
    }

    /** Icon for a spending category or money source, so every screen uses the same picture. */
    static Name forCategory(String category) {
        String c = category == null ? "" : category.trim().toLowerCase();
        switch (c) {
            case "food": return Name.UTENSILS;
            case "transportation": return Name.BUS;
            case "school": return Name.CAP;
            case "entertainment": return Name.GAMEPAD;
            case "shopping": return Name.BAG;
            case "bills": return Name.BOLT;
            case "health": return Name.HEART;
            case "allowance": return Name.COINS;
            case "other funds": return Name.CASH;
            case "savings": return Name.PIGGY_LINE;
            default: return Name.TAG;
        }
    }

    /** A letter in a filled circle, used for the profile picture in the top bar. */
    static Icon avatar(String letter, int size, Color fill, Color ink) {
        return new Icon() {
            @Override public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g2.setColor(fill);
                g2.fillOval(x, y, size, size);
                g2.setFont(Theme.font(Font.PLAIN, Math.round(size * 0.46f)));
                FontMetrics fm = g2.getFontMetrics();
                g2.setColor(ink);
                g2.drawString(letter, x + (size - fm.stringWidth(letter)) / 2,
                        y + (size - fm.getAscent() - fm.getDescent()) / 2 + fm.getAscent());
                g2.dispose();
            }
            @Override public int getIconWidth() { return size; }
            @Override public int getIconHeight() { return size; }
        };
    }

    // =====================================================================

    private static final class Glyph implements Icon {
        private final Name name;
        private final int size;
        private final Color color;

        Glyph(Name name, int size, Color color) {
            this.name = name;
            this.size = size;
            this.color = color;
        }

        @Override public int getIconWidth() { return size; }
        @Override public int getIconHeight() { return size; }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            g2.translate(x, y);
            g2.scale(size / 24.0, size / 24.0);
            g2.setColor(color);
            g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            draw(g2);
            g2.dispose();
        }

        private void draw(Graphics2D g) {
            switch (name) {
                case HOME: {
                    Area house = new Area(path(true, 12, 2.8, 21.6, 11.3, 19.4, 11.3, 19.4, 20.6,
                            4.6, 20.6, 4.6, 11.3, 2.4, 11.3));
                    house.subtract(new Area(rr(9.9, 14.2, 4.2, 8, 1.2)));
                    fillSoft(g, house);
                    break;
                }
                case LIST:
                    for (double y : new double[]{6, 12, 18}) {
                        g.fill(circle(4.6, y, 1.4));
                        g.draw(new Line2D.Double(9, y, 20.5, y));
                    }
                    break;
                case PIE:
                    g.fill(new Arc2D.Double(2.6, 5.4, 16, 16, 90, 270, Arc2D.PIE));
                    g.fill(new Arc2D.Double(5.4, 2.6, 16, 16, 0, 90, Arc2D.PIE));
                    break;
                case PIGGY: {
                    Area pig = piggy();
                    pig.subtract(new Area(circle(15.6, 12, 1.05)));
                    fillSoft(g, pig);
                    break;
                }
                case PIGGY_LINE:
                    g.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g.draw(piggy());
                    g.fill(circle(15.6, 12, 1.05));
                    g.draw(circle(12.6, 3.6, 1.9));
                    break;
                case BARS:
                    g.fill(rr(3.8, 13, 4.4, 8, 1.3));
                    g.fill(rr(9.8, 8.4, 4.4, 12.6, 1.3));
                    g.fill(rr(15.8, 3.4, 4.4, 17.6, 1.3));
                    break;
                case GEAR: {
                    Area gear = new Area(circle(12, 12, 7.1));
                    for (int k = 0; k < 8; k++) {
                        Shape tooth = rr(10.15, 1.7, 3.7, 5, 1.1);
                        gear.add(new Area(AffineTransform.getRotateInstance(Math.toRadians(45 * k), 12, 12)
                                .createTransformedShape(tooth)));
                    }
                    gear.subtract(new Area(circle(12, 12, 3.1)));
                    g.fill(gear);
                    break;
                }
                case WALLET:
                    g.draw(path(false, 17.5, 6.2, 17.5, 4.6, 16.4, 3.5, 6.5, 3.5, 4.2, 4.6, 3.5, 6.5));
                    g.draw(rr(3.5, 6.2, 17, 14, 3.2));
                    g.draw(rr(13.8, 10.2, 6.7, 5.6, 2));
                    g.fill(circle(16.8, 13, 1));
                    break;
                case SWAP:
                    g.draw(new Line2D.Double(4, 8, 19.5, 8));
                    g.draw(path(false, 15.5, 4, 19.5, 8, 15.5, 12));
                    g.draw(new Line2D.Double(20, 16, 4.5, 16));
                    g.draw(path(false, 8.5, 12, 4.5, 16, 8.5, 20));
                    break;
                case RECEIPT:
                    g.draw(path(true, 5, 3, 19, 3, 19, 21, 16.67, 19.6, 14.33, 21, 12, 19.6,
                            9.67, 21, 7.33, 19.6, 5, 21));
                    g.draw(new Line2D.Double(8.5, 7.5, 15.5, 7.5));
                    g.draw(new Line2D.Double(8.5, 11, 15.5, 11));
                    g.draw(new Line2D.Double(8.5, 14.5, 12.5, 14.5));
                    break;
                case SEARCH:
                    g.draw(circle(10.8, 10.8, 6.4));
                    g.draw(new Line2D.Double(15.6, 15.6, 20.5, 20.5));
                    break;
                case CALENDAR:
                    g.draw(rr(3.5, 5, 17, 15.5, 2.8));
                    g.draw(new Line2D.Double(3.5, 10, 20.5, 10));
                    g.draw(new Line2D.Double(8, 3, 8, 6.8));
                    g.draw(new Line2D.Double(16, 3, 16, 6.8));
                    for (double[] d : new double[][]{{8, 13.8}, {12, 13.8}, {16, 13.8}, {8, 17.2}, {12, 17.2}}) {
                        g.fill(circle(d[0], d[1], 1));
                    }
                    break;
                case BELL: {
                    Path2D bell = new Path2D.Double();
                    bell.moveTo(6, 16.8);
                    bell.lineTo(6, 11);
                    bell.curveTo(6, 7.4, 8.7, 4.6, 12, 4.6);
                    bell.curveTo(15.3, 4.6, 18, 7.4, 18, 11);
                    bell.lineTo(18, 16.8);
                    bell.lineTo(19.6, 18.4);
                    bell.lineTo(4.4, 18.4);
                    bell.closePath();
                    g.draw(bell);
                    g.draw(new QuadCurve2D.Double(10, 21, 12, 22.6, 14, 21));
                    g.draw(new Line2D.Double(12, 2.4, 12, 4.4));
                    break;
                }
                case PLUS:
                    g.draw(new Line2D.Double(12, 5, 12, 19));
                    g.draw(new Line2D.Double(5, 12, 19, 12));
                    break;
                case CASH:
                    g.draw(rr(2.5, 6, 19, 12, 2.2));
                    g.draw(circle(12, 12, 2.8));
                    g.fill(circle(6.2, 12, 1));
                    g.fill(circle(17.8, 12, 1));
                    break;
                case COINS:
                    g.draw(circle(9.2, 9.2, 5.8));
                    g.draw(new Arc2D.Double(9, 9, 11.8, 11.8, 172, 285, Arc2D.OPEN));
                    g.draw(new Line2D.Double(9.2, 6.8, 9.2, 11.6));
                    break;
                case CHEVRON_DOWN:
                    g.draw(path(false, 6, 9, 12, 15, 18, 9));
                    break;
                case CHEVRON_LEFT:
                    g.draw(path(false, 15, 5.5, 8.5, 12, 15, 18.5));
                    break;
                case CHEVRON_RIGHT:
                    g.draw(path(false, 9, 5.5, 15.5, 12, 9, 18.5));
                    break;
                case INFO:
                    g.draw(circle(12, 12, 9));
                    g.draw(new Line2D.Double(12, 11, 12, 16.5));
                    g.fill(circle(12, 7.6, 1.25));
                    break;
                case SLIDERS:
                    g.draw(new Line2D.Double(4, 6, 20, 6));
                    g.draw(new Line2D.Double(4, 12, 20, 12));
                    g.draw(new Line2D.Double(4, 18, 20, 18));
                    g.fill(circle(9, 6, 2.4));
                    g.fill(circle(15.5, 12, 2.4));
                    g.fill(circle(8, 18, 2.4));
                    break;
                case LOGOUT:
                    g.draw(path(false, 10, 4, 5.5, 4, 4.5, 5, 4.5, 19, 5.5, 20, 10, 20));
                    g.draw(new Line2D.Double(9.5, 12, 20, 12));
                    g.draw(path(false, 16, 8, 20, 12, 16, 16));
                    break;
                case UTENSILS: {
                    g.draw(path(false, 4.3, 3, 4.3, 8.2));
                    g.draw(path(false, 9.3, 3, 9.3, 8.2));
                    g.draw(new Line2D.Double(6.8, 3, 6.8, 21));
                    g.draw(new Arc2D.Double(4.3, 5.7, 5, 5, 180, 180, Arc2D.OPEN));
                    Path2D blade = new Path2D.Double();
                    blade.moveTo(17.2, 13.2);
                    blade.lineTo(17.2, 3);
                    blade.curveTo(20.2, 4.6, 20.8, 9.4, 20.4, 13.2);
                    blade.closePath();
                    g.fill(blade);
                    g.draw(blade);
                    g.draw(new Line2D.Double(17.2, 13, 17.2, 21));
                    break;
                }
                case BUS:
                    g.draw(rr(5, 3, 14, 15.5, 3));
                    g.draw(new Line2D.Double(5, 11, 19, 11));
                    g.draw(new Line2D.Double(9.5, 6.4, 14.5, 6.4));
                    g.fill(circle(8.7, 14.6, 1.15));
                    g.fill(circle(15.3, 14.6, 1.15));
                    g.draw(new Line2D.Double(7.8, 18.5, 7.8, 21));
                    g.draw(new Line2D.Double(16.2, 18.5, 16.2, 21));
                    break;
                case CAP: {
                    g.fill(path(true, 12, 3.6, 22.6, 8.9, 12, 14.2, 1.4, 8.9));
                    Path2D base = new Path2D.Double();
                    base.moveTo(5.6, 11.4);
                    base.lineTo(12, 14.6);
                    base.lineTo(18.4, 11.4);
                    base.lineTo(18.4, 16);
                    base.curveTo(16.4, 18.4, 14.2, 19.5, 12, 19.5);
                    base.curveTo(9.8, 19.5, 7.6, 18.4, 5.6, 16);
                    base.closePath();
                    g.fill(base);
                    g.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g.draw(new Line2D.Double(21.2, 9.4, 21.2, 15.4));
                    g.fill(circle(21.2, 16.2, 1.2));
                    break;
                }
                case GAMEPAD: {
                    Area pad = new Area(new RoundRectangle2D.Double(1.8, 6.4, 20.4, 11.2, 11, 11));
                    pad.add(new Area(new Ellipse2D.Double(1.6, 9.2, 7.4, 10.2)));
                    pad.add(new Area(new Ellipse2D.Double(15, 9.2, 7.4, 10.2)));
                    pad.subtract(new Area(new Rectangle2D.Double(5.3, 11.3, 5, 1.9)));
                    pad.subtract(new Area(new Rectangle2D.Double(6.85, 9.75, 1.9, 5)));
                    pad.subtract(new Area(circle(15.7, 11, 1.2)));
                    pad.subtract(new Area(circle(18, 13.5, 1.2)));
                    g.fill(pad);
                    break;
                }
                case BAG: {
                    g.draw(rr(4.5, 8, 15, 13, 2.5));
                    Path2D handle = new Path2D.Double();
                    handle.moveTo(8.5, 10.5);
                    handle.lineTo(8.5, 7);
                    handle.curveTo(8.5, 4.9, 10, 3.5, 12, 3.5);
                    handle.curveTo(14, 3.5, 15.5, 4.9, 15.5, 7);
                    handle.lineTo(15.5, 10.5);
                    g.draw(handle);
                    break;
                }
                case BOLT:
                    g.draw(path(true, 13, 2.5, 4.5, 13.5, 11.2, 13.5, 10.5, 21.5, 19.5, 10, 12.8, 10));
                    break;
                case HEART: {
                    Path2D h = new Path2D.Double();
                    h.moveTo(12, 20.2);
                    h.curveTo(5, 15.6, 2.5, 12.2, 2.5, 8.6);
                    h.curveTo(2.5, 5.6, 4.8, 3.6, 7.5, 3.6);
                    h.curveTo(9.5, 3.6, 11, 4.6, 12, 6.3);
                    h.curveTo(13, 4.6, 14.5, 3.6, 16.5, 3.6);
                    h.curveTo(19.2, 3.6, 21.5, 5.6, 21.5, 8.6);
                    h.curveTo(21.5, 12.2, 19, 15.6, 12, 20.2);
                    h.closePath();
                    g.draw(h);
                    break;
                }
                case TAG:
                    g.draw(path(true, 3.2, 3.2, 11.2, 3.2, 20.8, 12.8, 12.8, 20.8, 3.2, 11.2));
                    g.fill(circle(7.6, 7.6, 1.35));
                    break;
                case SPROUT:
                    drawSprout(g);
                    break;
                default:
                    break;
            }
        }

        /** Piggy bank body, snout, legs and ear as one outline. */
        private static Area piggy() {
            Area pig = new Area(new Ellipse2D.Double(3.2, 7.6, 16.4, 11.4));
            pig.add(new Area(rr(17.4, 11, 4.1, 4.6, 1.4)));
            pig.add(new Area(rr(6, 16, 3.3, 5, 1.2)));
            pig.add(new Area(rr(13.3, 16, 3.3, 5, 1.2)));
            pig.add(new Area(path(true, 7.1, 9.7, 8.5, 5.2, 11.9, 8.4)));
            return pig;
        }

        /** Two-tone leaves for the sidebar card: the given colour, with darker veins. */
        private void drawSprout(Graphics2D g) {
            Color vein = new Color(Math.max(0, color.getRed() - 70), Math.max(0, color.getGreen() - 80),
                    Math.max(0, color.getBlue() - 90));
            double[][] leaves = {{8.2, 13.6, 5.6, 11.4, -38}, {16.4, 12.6, 5, 9.4, 44}, {12.6, 5.6, 3.8, 7.8, 8}};
            for (double[] l : leaves) {
                Path2D leaf = new Path2D.Double(); // pointed at both ends
                leaf.moveTo(l[0], l[1] - l[3] / 2);
                leaf.quadTo(l[0] + l[2], l[1], l[0], l[1] + l[3] / 2);
                leaf.quadTo(l[0] - l[2], l[1], l[0], l[1] - l[3] / 2);
                leaf.closePath();
                Shape turned = AffineTransform.getRotateInstance(Math.toRadians(l[4]), l[0], l[1])
                        .createTransformedShape(leaf);
                g.setColor(color);
                g.fill(turned);
            }
            g.setColor(vein);
            g.setStroke(new BasicStroke(0.9f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.draw(new CubicCurve2D.Double(12, 23.4, 12.4, 18, 12, 13, 12.6, 3.2));
            g.draw(new QuadCurve2D.Double(11.9, 19.5, 9.5, 15.5, 5.2, 9.2));
            g.draw(new QuadCurve2D.Double(12.1, 18.2, 14.5, 14.5, 19.2, 9.2));
        }

        private static void fillSoft(Graphics2D g, Shape s) {
            g.fill(s);
            g.setStroke(new BasicStroke(1.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.draw(s); // rounds the sharp corners of solid icons slightly
        }

        private static Shape circle(double cx, double cy, double r) {
            return new Ellipse2D.Double(cx - r, cy - r, r * 2, r * 2);
        }

        private static Shape rr(double x, double y, double w, double h, double r) {
            return new RoundRectangle2D.Double(x, y, w, h, r * 2, r * 2);
        }

        private static Path2D path(boolean closed, double... xy) {
            Path2D p = new Path2D.Double();
            p.moveTo(xy[0], xy[1]);
            for (int i = 2; i < xy.length; i += 2) p.lineTo(xy[i], xy[i + 1]);
            if (closed) p.closePath();
            return p;
        }
    }
}