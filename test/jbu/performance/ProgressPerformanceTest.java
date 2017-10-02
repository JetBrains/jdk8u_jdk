/*
 * Copyright 2017 JetBrains s.r.o.
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

package performance;

import org.junit.Test;
import performance.ui.JBProgressBar;

import javax.swing.*;
import java.awt.*;
import java.util.concurrent.Semaphore;

public class ProgressPerformanceTest extends JFrame {

    @SuppressWarnings("FieldCanBeLocal")
    private static int N0 = 200;
    private static int N1 = 10000;

    private static volatile int count = 0;
    private static volatile long overallTime = 0;
    private static volatile long time = 0;

    private static Semaphore s = new Semaphore(1);

    public ProgressPerformanceTest() {
        setLayout(new FlowLayout());
        add(createPanel(true, true));
        add(createPanel(true, true));
        add(createPanel(true, true));
        add(createPanel(true, true));
        add(createPanel(true, true));
        setPreferredSize(new Dimension(320, 240));
        pack();
    }

    private static JComponent createPanel(boolean indeterminate, boolean opaque) {
        String text = (indeterminate ? "indeterminate" : "determinate") +
                (opaque ? "; opaque" : "; non opaque");
        JLabel label = new JLabel(text);

        JBProgressBar progress = new MyProgressBar();
        progress.setIndeterminate(indeterminate);
        progress.setValue(30);
        progress.setOpaque(opaque);

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.add(progress);
        wrapper.setBackground(Color.BLUE);

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.add(label);
        panel.add(wrapper);
        panel.add(Box.createVerticalStrut(5));

        return panel;
    }

    public static void doInitCounts(int nWarmUp, int nMeasure) {
        N0 = nWarmUp;
        N1 = nMeasure;
        count = 0;
    }

    public static void doBeginPaint() {
        if (count >= N0) {
            time = System.currentTimeMillis();
        }
    }

    public static void doEndPaint() {
        count++;
        if (count > N0) {
            overallTime += System.currentTimeMillis() - time;
            if (count >= N1 + N0) {
                System.err.println("##teamcity[buildStatisticValue key='ProgressPerformanceTest' value='" + (double) overallTime / N1 + "']");
                System.out.println((double) overallTime / N1);
                count = 0;
                s.release();
                overallTime = 0;
            }
        }

    }

    @Test
    public void testProgress() {
        try {
            s.acquire();
        } catch (InterruptedException e) {
            throw new RuntimeException("Cannot start test");
        }
        SwingUtilities.invokeLater(() -> {
            (new ProgressPerformanceTest()).setVisible(true);
        });

        try {
            s.acquire();
        } catch (InterruptedException e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        } finally {
            s.release();
        }
    }



    static class MyProgressBar extends JBProgressBar {
        @Override
        public void paint(Graphics g) {
            doBeginPaint();
            super.paint(g);
            doEndPaint();
        }
    }

}
