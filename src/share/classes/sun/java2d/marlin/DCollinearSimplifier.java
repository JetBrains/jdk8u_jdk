/*
 * Copyright (c) 2015, 2017, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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

package sun.java2d.marlin;


final class DCollinearSimplifier implements DPathConsumer2D {

    private static final int STATE_PREV_LINE = 0;
    private static final int STATE_PREV_POINT = 1;
    private static final int STATE_EMPTY = 2;

    // slope precision threshold
    private static final double EPS = 1e-3d; // aaime proposed 1e-3d

    // members:
    private DPathConsumer2D delegate;
    private int state;
    private double px1, py1;
    private double pdx, pdy;
    private double px2, py2;

    DCollinearSimplifier() {
    }

    public DCollinearSimplifier init(final DPathConsumer2D delegate) {
        this.delegate = delegate;
        this.state = STATE_EMPTY;

        return this; // fluent API
    }

    @Override
    public void pathDone() {
        emitStashedLine();
        delegate.pathDone();
        state = STATE_EMPTY;
    }

    @Override
    public void closePath() {
        emitStashedLine();
        delegate.closePath();
        state = STATE_EMPTY;
    }

    @Override
    public long getNativeConsumer() {
        return 0;
    }

    @Override
    public void quadTo(final double x1, final double y1,
                       final double xe, final double ye)
    {
        emitStashedLine();
        delegate.quadTo(x1, y1, xe, ye);
        // final end point:
        state = STATE_PREV_POINT;
        px1 = xe;
        py1 = ye;
    }

    @Override
    public void curveTo(final double x1, final double y1,
                        final double x2, final double y2,
                        final double xe, final double ye)
    {
        emitStashedLine();
        delegate.curveTo(x1, y1, x2, y2, xe, ye);
        // final end point:
        state = STATE_PREV_POINT;
        px1 = xe;
        py1 = ye;
    }

    @Override
    public void moveTo(final double xe, final double ye) {
        emitStashedLine();
        delegate.moveTo(xe, ye);
        state = STATE_PREV_POINT;
        px1 = xe;
        py1 = ye;
    }

    @Override
    public void lineTo(final double xe, final double ye) {
        // most probable case first:
        if (state == STATE_PREV_LINE) {
            // test for collinearity
            final double dx = (xe - px2);
            final double dy = (ye - py2);

            // perf: avoid slope computation (fdiv) replaced by 3 fmul
            if ((dy == 0.0d && pdy == 0.0d && (pdx * dx) >= 0.0d)
// uncertainty on slope:
//                || (Math.abs(pdx * dy - pdy * dx) < EPS * Math.abs(pdy * dy))) {
// try 0
                || ((pdy * dy) != 0.0d && (pdx * dy - pdy * dx) == 0.0d)) {
                // same horizontal orientation or same slope:
                // TODO: store cumulated error on slope ?
                // merge segments
                px2 = xe;
                py2 = ye;
            } else {
                // emit previous segment
                delegate.lineTo(px2, py2);
                px1 = px2;
                py1 = py2;
                pdx = dx;
                pdy = dy;
                px2 = xe;
                py2 = ye;
            }
        } else if (state == STATE_PREV_POINT) {
            state = STATE_PREV_LINE;
            pdx = (xe - px1);
            pdy = (ye - py1);
            px2 = xe;
            py2 = ye;
        } else if (state == STATE_EMPTY) {
            delegate.lineTo(xe, ye);
            state = STATE_PREV_POINT;
            px1 = xe;
            py1 = ye;
        }
    }

    private void emitStashedLine() {
        if (state == STATE_PREV_LINE) {
            delegate.lineTo(px2, py2);
        }
    }
}
