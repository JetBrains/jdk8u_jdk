import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JTextArea;
import javax.swing.RepaintManager;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

/*
 * @test
 * @bug 8197499
 * @summary RepaintManager does not increase double buffer after attaching a display with higher resolution
 * @run main/manual RepaintManagerDoubleBufferMaximumSize
 */

public class RepaintManagerDoubleBufferMaximumSize {

    private static JFrame frame = new JFrame();
    private static JTextArea textArea = new JTextArea();

    private static void createAndShowGUI() {
        textArea = new JTextArea(5, 20);
        textArea.setEditable(false);

        JButton button = new JButton("Attach 4K display and click here to watch DoubleBufferMaximumSize");
        button.addActionListener(e -> doTest());

        frame = new JFrame("DoubleBufferMaximumSize  test");
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setSize(600, 300);
        frame.add("Center", textArea);
        frame.add("South", button);

        frame.setVisible(true);
    }

    private static void doTest() {
        RepaintManager repaintManager = RepaintManager.currentManager(frame);
        textArea.append("DoubleBufferMaximumSize: " + repaintManager.getDoubleBufferMaximumSize() + "\n");
    }

    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeAndWait(RepaintManagerDoubleBufferMaximumSize::createAndShowGUI);
        doTest();
    }
}
