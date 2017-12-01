/*
 * Copyright 2000-2017 JetBrains s.r.o.
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

import com.apple.laf.AquaProgressBarUI;
import com.intellij.ide.ui.laf.darcula.ui.DarculaProgressBarUI;
import com.intellij.openapi.progress.util.ColorProgressBar;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.ProgressBarUI;
import java.awt.Color;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/* @test
 * @summary the test creates JProgressBar elements with various parameters and compare their progress for
 * DarculaProgressBarUI and AquaProgressBarUI. It fails if DarculaProgressBarUI is slower than AquaProgressBarUI
 * on 20% or more
 */
public class ProgressBarTest extends JDialog {

    private static int ITERATION_NUMBER = 10;

    private static final List<JProgressBar> pbList = new ArrayList<>();
    private static volatile Exception failedException;
    private static JFrame staticFrame;
    private static JPanel panel;
    private static ScheduledExecutorService executor;
    private static Class progressBarUIClass;

    static private JComponent createPanel(boolean indeterminate, Color foreground, boolean modeless)
            throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        String text = (indeterminate ? "indeterminate" : "determinate");
        JLabel label = new JLabel(text);

        JProgressBar progress = new JProgressBar(0, 100);

        Method method = progressBarUIClass.getMethod("createUI", JComponent.class);
        Object retobj = method.invoke(null, new Object[]{null});

        progress.setUI((ProgressBarUI) retobj);
        progress.setBorder(new EmptyBorder(0, 0, 0, 0));

        progress.setIndeterminate(indeterminate);
        progress.setValue(0);
        progress.setForeground(foreground);

        if (modeless) {
            progress.putClientProperty("ProgressBar.stripeWidth", 2);
        }

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.add(label);
        panel.add(progress);
        panel.add(Box.createVerticalStrut(5));

        pbList.add(progress);

        return panel;
    }

    private static void createCenterPanel() throws NoSuchMethodException, IllegalAccessException,
            InvocationTargetException {

        panel.add(createPanel(false, null, false));
        panel.add(createPanel(false, ColorProgressBar.RED, false));
        panel.add(createPanel(false, ColorProgressBar.GREEN, false));
        panel.add(createPanel(false, null, true));
        panel.add(createPanel(false, ColorProgressBar.RED, true));
        panel.add(createPanel(false, ColorProgressBar.GREEN, true));

        panel.add(createPanel(true, null, false));
        panel.add(createPanel(true, null, true));
        panel.add(createPanel(true, ColorProgressBar.RED, false));
        panel.add(createPanel(true, ColorProgressBar.GREEN, false));
        panel.add(createPanel(true, ColorProgressBar.RED, true));
        panel.add(createPanel(true, ColorProgressBar.GREEN, true));

        staticFrame.add(panel);
        staticFrame.pack();

        executor = Executors.newScheduledThreadPool(12);

        for (JProgressBar pb : pbList) {

            if (!pb.isIndeterminate()) {

                Runnable request = () -> {

                    if (pb.getValue() < pb.getMaximum()) {

                        Runnable update = () -> pb.setValue(pb.getValue() + 1);

                        try {
                            SwingUtilities.invokeAndWait(update);
                        } catch (InterruptedException | InvocationTargetException e) {
                            //ignore
                        }
                    }
                };

                executor.scheduleAtFixedRate(request, 0, 10, TimeUnit.MILLISECONDS);
            }
        }
    }

    private static long doTest(Class progressBarUIClass, int iterations) {
        ProgressBarTest.progressBarUIClass = progressBarUIClass;
        System.out.println("Testing : " + ProgressBarTest.progressBarUIClass.getName());
        long started = System.currentTimeMillis();

        for (int i = 0; i < iterations; i++) {
            staticFrame = new JFrame();
            staticFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            panel = new JPanel();
            panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

            staticFrame.setVisible(true);

            try {
                SwingUtilities.invokeAndWait(() -> {
                    try {
                        createCenterPanel();
                    } catch (Exception e) {
                        e.printStackTrace();
                        failedException = e;
                    }
                });

                boolean doContinue;
                do {
                    doContinue = false;
                    for (JProgressBar pb : pbList) {
                        if (!pb.isIndeterminate()) {
                            doContinue = (pb.getValue() + 1) < pb.getMaximum();
                        }
                    }
                } while (doContinue);

            } catch (Exception e) {
                e.printStackTrace();
                failedException = e;
            } finally {
                JFrame frame = staticFrame;
                SwingUtilities.invokeLater(() -> {
                    executor.shutdown();
                    if (frame != null) {
                        frame.setVisible(false);
                        frame.dispose();
                    }
                });
            }
            if (failedException != null) throw new RuntimeException(failedException);
        }
        long execTime = (System.currentTimeMillis() - started);
        System.out.println("\texecution time " + execTime);
        return execTime;
    }

    public static void main(String[] args) {
        if (args.length > 0)
            ITERATION_NUMBER = Integer.parseInt(args[0]);

        long darculaTime = doTest(DarculaProgressBarUI.class, ITERATION_NUMBER);
        long aquaTime = doTest(AquaProgressBarUI.class, ITERATION_NUMBER);
        float diff = ((float) (darculaTime - aquaTime)) / aquaTime * 100;
        System.out.println("diff = " + diff + "%");
        if (diff > 20) {
            throw new RuntimeException("Too big difference between AquaProgressBarUI and DarculaProgressBarUI");
        }
    }
}