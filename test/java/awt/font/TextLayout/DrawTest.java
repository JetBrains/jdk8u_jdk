import javax.swing.JFrame;
import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.font.TextLayout;
import java.awt.image.BufferedImage;

/**
 * @test 1.0 2016/08/25
 * @bug 8139176
 * @run main DrawTest
 * @summary java.awt.TextLayout does not handle correctly the bolded logical fonts (Serif)
 */

// The test against java.awt.font.TextLayout, it draws the text "Gerbera" twise
// via the methods Graphics.drawString and TextLayout.draw and then it checks
// that both output have the same width.
// The test does this checking for two styles of the font Serif - PLAIN and
// BOLD in course one by one.
    
public class DrawTest {
    static final Font plain = new Font("Serif", Font.PLAIN, 32);
    static final Font bold = new Font("Serif", Font.BOLD, 32);
    static int testCaseNo = 1;
    static final String txt = "Gerbera";
    static boolean isPassed = true;
    static String errMsg = "";

    public static void main(String[] args) {

        // This happens with jre1.8.0_60-x64
        System.out.println(System.getProperty("java.home"));

        final JFrame frame = new JFrame();
        frame.setSize(116, 97);

        final JPanel panel = new JPanel() {

            private void drawString(Graphics g, Font font) {
                g.setFont(font);
                g.drawString(txt, 0, 32);
                int width = g.getFontMetrics(font).stringWidth(txt);
                g.drawRect(0, 32, width, 0);
            }

            private void drawTextLayout(Graphics g, Font font) {
                TextLayout tl = new TextLayout(txt,
                        font,
                        g.getFontMetrics(font).getFontRenderContext());
                tl.draw((Graphics2D) g, 0, 65);
                int width = (int) tl.getAdvance();
                g.drawRect(0, 65, width, 0);
            }

            /**
             * @see javax.swing.JComponent#paintComponent(java.awt.Graphics)
             */
            @Override
            protected void paintComponent(Graphics g) {
                g.setColor(Color.WHITE);
                g.fillRect(0, 0, getWidth(), getHeight());
                g.setColor(Color.BLACK);

                int width;
                TextLayout tl;
                if (testCaseNo == 1) {
                    // Ok.
                    // For the PLAIN font, the text painted by g.drawString and the text layout are the same.
                    drawString(g, plain);
                    drawTextLayout(g, plain);
                } else {
                    // Not Ok.
                    // For the BOLD font, the text painted by g.drawString and the text layout are NOT the same.
                    drawString(g, bold);
                    drawTextLayout(g, bold);
                }
            }
        };

        frame.getContentPane().add(panel);
        frame.setVisible(true);

        BufferedImage paintImage = getScreenShot(panel);
        int width = paintImage.getWidth();
        int height = paintImage.getHeight();
        int rgb;
        int r, g, b;

        int width1 = charWidth(paintImage, 0, 10, 116, 32);
        int width2 = charWidth(paintImage, 0, 43, 116, 32);
        if (width1 != width2) {
            System.out.println("test case FAILED");
            errMsg = "plained";
            isPassed = false;
        } else
            System.out.println("test case PASSED");

        testCaseNo = 2;
        panel.revalidate();
        panel.repaint();
        paintImage = getScreenShot(panel);

        width1 = charWidth(paintImage, 0, 10, 116, 32);
        width2 = charWidth(paintImage, 0, 43, 116, 32);
        if (width1 != width2) {
            System.out.println("test case FAILED");
            errMsg = "bolded";
            isPassed = false;
        } else
            System.out.println("test case PASSED");

        frame.dispose();
        if (!isPassed) {
            throw new RuntimeException(errMsg + " logical fonts (Serif) was not correctly handled");
        }

    }

    static private BufferedImage getScreenShot(JPanel panel) {
        BufferedImage bi = new BufferedImage(
                panel.getWidth(), panel.getHeight(), BufferedImage.TYPE_INT_ARGB);
        panel.paint(bi.getGraphics());
        return bi;
    }

    private static int charWidth(BufferedImage bufferedImage, int x, int y, int width, int height) {
        int rgb;
        int returnWidth = 0;
        for (int row = y; row < y + height; row++) {
            for (int col = x; col < x + width; col++) {
                // remove transparance
                rgb = bufferedImage.getRGB(col, row) & 0x00FFFFFF;

                int r = rgb >> 16;
                int g = (rgb >> 8) & 0x000000FF;
                int b = rgb & 0x00000FF;
                if (r == g && g == b && b == 255)
                    System.out.print(" .");
                else
                    System.out.print(" X");

                if (rgb != 0xFFFFFF && returnWidth < col)
                    returnWidth = col;
            }
            System.out.println();
        }
        return returnWidth;
    }
}
