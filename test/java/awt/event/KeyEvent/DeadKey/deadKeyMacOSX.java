/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

/*
 * @test
 * @bug 7196547
 * @summary Dead Key implementation for KeyEvent on Mac OS X
 * @author alexandr.scherbatiy area=awt.event
 * @run main deadKeyMacOSX
 */

import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Panel;
import java.awt.Robot;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import sun.awt.OSInfo;

public class deadKeyMacOSX {

    private static volatile int state = 0;

    private final static Object keyMainObj = new Object();
    private final static Object keyPressObj = new Object();
    private final static Object keyTypedObj = new Object();


    public static void main(String[] args) throws Exception {

        if (OSInfo.getOSType() != OSInfo.OSType.MACOSX) {
            return;
        }

        Robot robot = new Robot();
        robot.setAutoDelay(50);

        createAndShowGUI();

        // Pressed keys: Alt + E + A
        // Results:  ALT + VK_DEAD_ACUTE + a with accute accent
        keyPress(robot, KeyEvent.VK_ALT);
        keyPress(robot, KeyEvent.VK_E);
        keyRelease(robot, KeyEvent.VK_E);
        keyRelease(robot, KeyEvent.VK_ALT);

        keyPress(robot, KeyEvent.VK_A);
        keyRelease(robot, KeyEvent.VK_A);

        if (state != 3) {
            throw new RuntimeException("Wrong number of key events.");
        }
    }

    private static void keyPress(Robot robot, int keyCode) {
        synchronized (keyMainObj) {
            robot.keyPress(keyCode);
        }
        synchronized (keyPressObj) {
        }
    }

    private static void keyRelease(Robot robot, int keyCode) {
        synchronized (keyMainObj) {
            robot.keyRelease(keyCode);
        }
        synchronized (keyTypedObj) {
        }
    }

    static final Lock lock = new ReentrantLock();
    static final Condition isPainted = lock.newCondition();

    static void createAndShowGUI() {
        Frame frame = new Frame();
        frame.setSize(300, 300);
        Panel panel = new Panel() {
            @Override
            public void paint(Graphics g) {
                super.paint(g);
                lock.lock();
                isPainted.signalAll();
                lock.unlock();
            }
        };
        frame.add(panel);

        lock.lock();

        frame.setVisible(true);
        panel.requestFocusInWindow();

        try {
            isPainted.await();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            lock.unlock();
            panel.addKeyListener(new DeadKeyListener());
        }
    }

    static class DeadKeyListener extends KeyAdapter {

        @Override
        public void keyPressed(KeyEvent e) {
            synchronized (keyPressObj) {
                synchronized (keyMainObj) {
                    int keyCode = e.getKeyCode();

                    switch (state) {
                        case 0:
                            if (keyCode != KeyEvent.VK_ALT) {
                                throw new RuntimeException("Alt is not pressed.");
                            }
                            state++;
                            break;
                        case 1:
                            if (keyCode != KeyEvent.VK_E) {
                                throw new RuntimeException("E is not pressed.");
                            }

                            state++;
                            break;
                        case 2:
                            if (keyCode != KeyEvent.VK_A) {
                                throw new RuntimeException("A is not pressed.");
                            }
                            state++;
                            break;
                        default:
                            throw new RuntimeException("Excessive keyPressed event.");
                    }
                }
            }
        }

        @Override
        public void keyTyped(KeyEvent e) {
            synchronized (keyTypedObj) {
                synchronized (keyMainObj) {
                    int keyCode = e.getKeyCode();
                    char keyChar = e.getKeyChar();

                    if (state == 3) {
                        // Now we send key codes
                        if (keyCode != KeyEvent.VK_A) {
                            throw new RuntimeException("Key code should be undefined.");
                        }
                        // This works for US keyboard only
                        if (keyChar != 0xE1) {
                            throw new RuntimeException("A char does not have ACCUTE accent");
                        }
                    } else {
                        throw new RuntimeException("Wrong number of keyTyped events.");
                    }
                }
            }
        }
    }
}
