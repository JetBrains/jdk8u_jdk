package performance.text;

import org.junit.Test;
import performance.BaseScrollingPerformanceTest;
import sun.swing.SwingUtilities2;

import javax.swing.*;
import java.awt.*;

public class GreyscaleTextAreaScrollingTest extends BaseScrollingPerformanceTest {
    public GreyscaleTextAreaScrollingTest() {
        super("##teamcity[buildStatisticValue key='GreyscaleTextAreaScrolling' ");
    }

    @Test
    public void testTextAreaScrolling() {
        doTest(this);
    }

    protected JComponent createComponent() {
        JComponent textArea = createTextArea();
        textArea.putClientProperty(SwingUtilities2.AA_TEXT_PROPERTY_KEY, GREYSCALE_HINT);
        return textArea;
    }

    @Override
    protected void configureScrollPane(JScrollPane scrollPane) {
        scrollPane.putClientProperty(SwingUtilities2.AA_TEXT_PROPERTY_KEY, GREYSCALE_HINT);
    }
}
