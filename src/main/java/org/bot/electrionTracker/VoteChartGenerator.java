package org.bot.electrionTracker;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import javax.imageio.ImageIO;

/**
 * Renders a small PNG bar chart of candidate votes, styled to roughly match Discord's dark theme,
 * so it can be attached to the /election embed. Pure java.awt - no extra dependency needed.
 */
public final class VoteChartGenerator {

    private static final Color BACKGROUND = new Color(0x2B2D31);
    private static final Color GRID = new Color(0x40444B);
    private static final Color AXIS_TEXT = new Color(0x949BA4);
    private static final Color LABEL_TEXT = new Color(0xDBDEE1);

    private static final Color[] PALETTE = {
            new Color(0xE74C3C), new Color(0x2ECC71), new Color(0x1ABC9C),
            new Color(0xF1C40F), new Color(0xE91E8C), new Color(0x3498DB), new Color(0x9B59B6)
    };

    static {
        // ImageIO defaults to caching to a temp file on disk, which fails with an opaque
        // IOException if the temp directory isn't writable (permissions, AV software, sandboxing,
        // etc). We only ever write a small in-memory PNG here, so force pure in-memory caching.
        ImageIO.setUseCache(false);
    }

    private VoteChartGenerator() {
    }

    public static byte[] renderVotes(List<Candidate> candidates) {
        int width = 520;
        int height = 300;
        int marginLeft = 55;
        int marginBottom = 40;
        int marginTop = 15;
        int marginRight = 15;
        int chartWidth = width - marginLeft - marginRight;
        int chartHeight = height - marginTop - marginBottom;

        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        g.setColor(BACKGROUND);
        g.fillRect(0, 0, width, height);

        long maxVotes = 1;
        for (Candidate c : candidates) {
            maxVotes = Math.max(maxVotes, c.votes);
        }
        long axisMax = niceCeil(maxVotes);

        g.setFont(new Font("SansSerif", Font.PLAIN, 11));
        FontMetrics gridFm = g.getFontMetrics();
        int gridLines = 5;
        for (int i = 0; i <= gridLines; i++) {
            int y = marginTop + chartHeight - (chartHeight * i / gridLines);
            g.setColor(GRID);
            g.drawLine(marginLeft, y, width - marginRight, y);
            String label = formatNumber(axisMax * i / gridLines);
            g.setColor(AXIS_TEXT);
            g.drawString(label, marginLeft - gridFm.stringWidth(label) - 8, y + gridFm.getAscent() / 2 - 1);
        }

        int n = Math.max(candidates.size(), 1);
        int slot = chartWidth / n;
        int barWidth = Math.max((int) (slot * 0.5), 10);
        g.setFont(new Font("SansSerif", Font.BOLD, 12));
        FontMetrics nameFm = g.getFontMetrics();

        for (int i = 0; i < candidates.size(); i++) {
            Candidate c = candidates.get(i);
            double ratio = axisMax == 0 ? 0 : Math.min(1.0, (double) c.votes / axisMax);
            int barHeight = (int) (ratio * chartHeight);
            int x = marginLeft + i * slot + (slot - barWidth) / 2;
            int y = marginTop + chartHeight - barHeight;

            g.setColor(PALETTE[i % PALETTE.length]);
            g.fillRoundRect(x, y, barWidth, Math.max(barHeight, 2), 4, 4);

            String name = c.name;
            int textWidth = nameFm.stringWidth(name);
            g.setColor(LABEL_TEXT);
            g.drawString(name, marginLeft + i * slot + (slot - textWidth) / 2, height - marginBottom + 16);
        }

        g.setColor(new Color(0x5C5F66));
        g.drawLine(marginLeft, marginTop, marginLeft, marginTop + chartHeight);
        g.drawLine(marginLeft, marginTop + chartHeight, width - marginRight, marginTop + chartHeight);

        g.dispose();

        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(img, "png", out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Failed to render vote chart: " + e.getMessage(), e);
        }
    }

    /** Rounds up to a "nice" scale max (e.g. 992_300 -> 1_000_000). */
    private static long niceCeil(long value) {
        if (value <= 0) {
            return 1;
        }
        long magnitude = 1;
        while (magnitude * 10 <= value) {
            magnitude *= 10;
        }
        long steps = (value + magnitude - 1) / magnitude;
        return steps * magnitude;
    }

    private static String formatNumber(long n) {
        if (n >= 1_000_000) {
            return String.format("%.1fM", n / 1_000_000.0);
        }
        if (n >= 1_000) {
            return String.format("%.1fK", n / 1_000.0);
        }
        return String.valueOf(n);
    }
}
