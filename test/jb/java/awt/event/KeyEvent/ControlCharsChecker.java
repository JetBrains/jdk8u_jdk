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

/* @test
 * @summary it checks KeyPressed and KeyReleased events for control characters
 */

public class ControlCharsChecker {

    static final ControlCharacter[] controlCharArray = {
            new ControlCharacter('@', 0x00),
            new ControlCharacter('A', 0x01),
            new ControlCharacter('B', 0x02),
            new ControlCharacter('C', 0x03),
            new ControlCharacter('D', 0x04),
            new ControlCharacter('E', 0x05),
            new ControlCharacter('F', 0x06),
            new ControlCharacter('G', 0x07),
            new ControlCharacter('H', 0x08),
            new ControlCharacter('I', 0x09),
            new ControlCharacter('J', 0x0A),
            new ControlCharacter('K', 0x0B),
            new ControlCharacter('L', 0x0C),
            new ControlCharacter('M', 0x0D),
            new ControlCharacter('N', 0x0E),
            new ControlCharacter('O', 0x0F),
            new ControlCharacter('P', 0x10),
            new ControlCharacter('Q', 0x11),
            new ControlCharacter('R', 0x12),
            new ControlCharacter('S', 0x13),
            new ControlCharacter('T', 0x14),
            new ControlCharacter('U', 0x15),
            new ControlCharacter('V', 0x16),
            new ControlCharacter('W', 0x17),
            new ControlCharacter('X', 0x18),
            new ControlCharacter('Y', 0x19),
            new ControlCharacter('Z', 0x1A),
            new ControlCharacter('[', 0x1B),
            new ControlCharacter('\\', 0x1C),
            new ControlCharacter(']', 0x1D),
            new ControlCharacter('^', 0x1E),
            new ControlCharacter('_', 0x1F),
            new ControlCharacter('?', 0x7F)
    };

    private static Robot robot;
    private static Frame frame;
    private static volatile int testCase = 0;

    private final static Object keyMainObj = new Object();
    private final static Object keyPressObj = new Object();
    private final static Object keyReleasedObj = new Object();

    private static volatile int keyPressCount;

    public static void main(String[] args) throws Exception {

        if (OSInfo.getOSType() != OSInfo.OSType.MACOSX) {
            return;
        }

        robot = new Robot();
        robot.setAutoDelay(50);

        createAndShowGUI();

        for (int i = 0; i < controlCharArray.length; i++) {
            testCase = i;
            System.out.println("\ntestCase = " + testCase);
            System.out.println("\tkeyPress: <VK_CONTROL> + \'" + controlCharArray[i].controlChar + "\'");
            keyPress(KeyEvent.VK_CONTROL);
            keyPress(controlCharArray[i].controlChar);
            keyRelease(controlCharArray[i].controlChar);
            keyRelease(KeyEvent.VK_CONTROL);
        }

        frame.dispose();
    }

    private static void keyPress(int keyCode) {
        keyPressCount++;
        synchronized (keyMainObj) {
            robot.keyPress(keyCode);
        }
        synchronized (keyPressObj) {
        }
    }

    private static void keyRelease(int keyCode) {
        synchronized (keyMainObj) {
            robot.keyRelease(keyCode);
        }
        synchronized (keyReleasedObj) {
        }
        keyPressCount--;
    }

    static final Lock lock = new ReentrantLock();
    static final Condition isPainted = lock.newCondition();

    static void createAndShowGUI() {
        frame = new Frame();
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
        panel.addKeyListener(new KeyListener());
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
        }
    }

    static String toUnicodeString(char value) {
        return toUnicodeString((int)value);
    }

    static String toUnicodeString(int value) {
        return "\\u" + Integer.toHexString(value | 0x10000).substring(1);
    }

    static class KeyListener extends KeyAdapter {

        @Override
        public synchronized void keyPressed(KeyEvent e) {
            synchronized (keyPressObj) {
                synchronized (keyMainObj) {
                    System.out.println("keyPressed EventHandler:");
                    int keyCode = e.getKeyCode();
                    char keyChar = e.getKeyChar();
                    int intKeyChar = Integer.valueOf(keyChar);
                    System.out.println("\tkeyCode = " + keyCode + " [" + KeyEvent.getKeyText(keyCode) + "]");
                    System.out.println("\tkeyChar = " + toUnicodeString(keyChar));
                    if (keyCode == KeyEvent.VK_CONTROL) {
                        if (keyPressCount != 1)
                            throw new RuntimeException("VK_CONTROL must be the first event");
                    }
                    else {
                        if (keyPressCount != 2)
                            throw new RuntimeException("control character must be the second event");

                        if (intKeyChar != controlCharArray[testCase].keyCode)
                            throw new RuntimeException(
                                    "Expected value: " + toUnicodeString(controlCharArray[testCase].keyCode)
                                            + " actual value: " + toUnicodeString(keyChar));
                    }

                }
            }
        }

        @Override
        public void keyReleased(KeyEvent e) {
            synchronized (keyReleasedObj) {
                synchronized (keyMainObj) {
                    System.out.println("keyReleased EventHandler:");

                    int keyCode = e.getKeyCode();
                    char keyChar = e.getKeyChar();
                    int intKeyChar = Integer.valueOf(keyChar);
                    System.out.println("\tkeyCode = " + keyCode + " [" + KeyEvent.getKeyText(keyCode) + "]");
                    System.out.println("\tkeyChar = " + toUnicodeString(keyChar));

                    if (keyCode == KeyEvent.VK_CONTROL) {
                        if (keyPressCount != 1)
                            throw new RuntimeException("VK_CONTROL must be the first event: " + keyPressCount);
                    }
                    else {
                        if (keyPressCount != 2)
                            throw new RuntimeException("control character must be the second event: " + keyPressCount);

                        if (intKeyChar != controlCharArray[testCase].keyCode)
                            throw new RuntimeException(
                                    "Expected value: " + toUnicodeString(controlCharArray[testCase].keyCode)
                                            + " actual value: " + toUnicodeString(keyChar));
                    }
                }
            }
        }

    }
}

class ControlCharacter {
    char controlChar;
    int keyCode;

    public ControlCharacter(char controlChar, int keyCode) {
        this.controlChar = controlChar;
        this.keyCode = keyCode;
    }
}