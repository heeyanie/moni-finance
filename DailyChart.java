package app;

import javax.swing.*;
import java.awt.*;

/** Bar chart of this week's spending, Monday to Sunday, with the daily limit as a dashed line. */
class DailyChart extends JComponent {
    private static final String[] DAY_NAMES = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};

    private double[] values = new double[7];
    private double limit;
    private int todayIndex = -1;

    DailyChart() {
        setPreferredSize(new Dimension(300, 220));
    }

    void setData(double[] dayValues, double dailyLimit, int today) {
        values = dayValues;
        limit = dailyLimit;
        todayIndex = today;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        Theme.smoothText(g2);

        int top = 26;
        int bottom = 28;
        int width = getWidth();
        int chartHeight = Math.max(40, getHeight() - top - bottom);
        int baseY = top + chartHeight;

        // Leave some room above the tallest bar for its label.
        double max = limit;
        for (double v : values) max = Math.max(max, v);
        if (max <= 0) max = 1;
        max *= 1.15;

        Font small = Theme.font(Font.PLAIN, 12);
        Font smallBold = Theme.font(Font.BOLD, 12);
        int slot = width / 7;
        int barWidth = Math.min(38, (int) (slot * 0.55));

        for (int i = 0; i < 7; i++) {
            int x = i * slot + (slot - barWidth) / 2;
            if (values[i] > 0) {
                int barHeight = Math.max(6, (int) Math.round(values[i] / max * chartHeight));
                boolean over = limit > 0 && values[i] > limit;
                g2.setColor(over ? Theme.RED : i == todayIndex ? Theme.ACCENT : Theme.ACCENT_MID);
                g2.fillRoundRect(x, baseY - barHeight, barWidth, barHeight, 10, 10);

                g2.setFont(small);
                g2.setColor(Theme.TEXT_SOFT);
                String label = Theme.peso(values[i]);
                int labelWidth = g2.getFontMetrics().stringWidth(label);
                g2.drawString(label, x + (barWidth - labelWidth) / 2, baseY - barHeight - 6);
            } else {
                g2.setColor(Theme.TRACK);
                g2.fillRoundRect(x, baseY - 4, barWidth, 4, 4, 4);
            }

            boolean isToday = i == todayIndex;
            g2.setFont(isToday ? smallBold : small);
            g2.setColor(isToday ? Theme.TEXT : Theme.MUTED);
            String day = isToday ? "Today" : DAY_NAMES[i];
            int dayWidth = g2.getFontMetrics().stringWidth(day);
            g2.drawString(day, i * slot + (slot - dayWidth) / 2, baseY + 20);
        }

        if (limit > 0) {
            int y = baseY - (int) Math.round(limit / max * chartHeight);
            g2.setColor(Theme.AMBER);
            g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND, 1f,
                    new float[]{6f, 5f}, 0f));
            g2.drawLine(0, y, width, y);
            g2.setFont(smallBold);
            String label = "Daily limit " + Theme.peso(limit);
            g2.drawString(label, width - g2.getFontMetrics().stringWidth(label), y - 6);
        }
        g2.dispose();
    }
}