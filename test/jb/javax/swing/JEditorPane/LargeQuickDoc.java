/*
 * Copyright 2000-2016 JetBrains s.r.o.
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

import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.text.html.HTMLEditorKit;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/* @test
 * @bug 8151725
 * @summary regression test on JRE-9
 */

public class LargeQuickDoc {
    static private String getText() throws IOException {
        File file = new File(System.getProperty("test.src", "."), "LargeQuickDoc.txt");
        StringBuilder stringBuilder = new StringBuilder();
        InputStream is = null;
        is = new FileInputStream(file);
        BufferedReader buf = new BufferedReader(new InputStreamReader(is));

        String line = null;

        line = buf.readLine();

        while (line != null) {
            stringBuilder.append(line).append("\n");
            line = buf.readLine();
        }
        return stringBuilder.toString();
    }

    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeAndWait(() -> {

            JFrame f = new JFrame();
            JEditorPane editorPane = new javax.swing.JEditorPane("text/html", "");
            editorPane.setEditorKit(new HTMLEditorKit());
            editorPane.setEditable(false);

            JScrollPane scrollPane = new JScrollPane(editorPane);
            scrollPane.setBorder(null);

            try {
                editorPane.setText(getText());
            } catch (IOException | ArrayIndexOutOfBoundsException e) {
                throw new RuntimeException(e);
            }

            f.getContentPane().add(scrollPane);
            f.setSize(500, 500);
            f.setVisible(true);
        });
    }
}