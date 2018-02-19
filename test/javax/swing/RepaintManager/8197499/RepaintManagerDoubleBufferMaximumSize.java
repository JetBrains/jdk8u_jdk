/*
 * Copyright 2000-2018 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
