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

package quality.text;

import org.junit.Test;
import quality.util.RenderUtil;

import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

public class TextMetricsTest {

    @Test
    public void testTextBounds() throws Exception {
        final Font font = new Font("Menlo", Font.PLAIN, 22);


        BufferedImage bi = RenderUtil.capture(120, 120,
                g2 -> {
                    String s = "A";

                    g2.setFont(font);

                    FontRenderContext frc = g2.getFontRenderContext();
                    Rectangle2D bnd = font.getStringBounds(s, frc);
                    TextLayout textLayout = new TextLayout(s, font, frc);
                    Rectangle2D bnd1 = textLayout.getBounds();
                    GlyphVector gv = font.createGlyphVector(frc, s);
                    Rectangle2D bnd2 = gv.getGlyphVisualBounds(0).getBounds2D();


                    g2.drawString(s, 5, 50);
                    g2.draw(new Path2D.Double(bnd,
                            AffineTransform.getTranslateInstance(5, 50 )));

                    g2.drawString(s, 30, 50);
                    g2.draw(new Path2D.Double(bnd1,
                            AffineTransform.getTranslateInstance(30, 50 )));


                    g2.drawString(s, 50, 50);
                    g2.draw(new Path2D.Double(bnd2,
                            AffineTransform.getTranslateInstance(50, 50 )));

                });

        RenderUtil.checkImage(bi, "text", "bndtext.png");

    }


}
