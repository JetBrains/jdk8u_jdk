/*
 * Copyright (c) 2005, 2018, Oracle and/or its affiliates. All rights reserved.
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

package sun.java2d.opengl;

import sun.misc.ThreadGroupUtils;
import sun.java2d.pipe.RenderBuffer;
import sun.java2d.pipe.RenderQueue;
import static sun.java2d.pipe.BufferedOpCodes.*;
import java.security.AccessController;
import java.security.PrivilegedAction;

/**
 * OGL-specific implementation of RenderQueue.  This class provides a
 * single (daemon) thread that is responsible for periodically flushing
 * the queue, thus ensuring that only one thread communicates with the native
 * OpenGL libraries for the entire process.
 */
public class OGLRenderQueue extends RenderQueue {

    private static OGLRenderQueue theInstance;
    final QueueFlusher flusher;

    private OGLRenderQueue() {
        super();
        /*
         * The thread must be a member of a thread group
         * which will not get GCed before VM exit.
         */
        flusher = AccessController.doPrivileged((PrivilegedAction<QueueFlusher>) () -> {
            return new QueueFlusher(ThreadGroupUtils.getRootThreadGroup());
        });
    }

    /**
     * Returns the single OGLRenderQueue instance.  If it has not yet been
     * initialized, this method will first construct the single instance
     * before returning it.
     */
    public static synchronized OGLRenderQueue getInstance() {
        if (theInstance == null) {
            theInstance = new OGLRenderQueue();
            theInstance.flusher.start();
        }
        return theInstance;
    }

    /**
     * Flushes the single OGLRenderQueue instance synchronously.  If an
     * OGLRenderQueue has not yet been instantiated, this method is a no-op.
     * This method is useful in the case of Toolkit.sync(), in which we want
     * to flush the OGL pipeline, but only if the OGL pipeline is currently
     * enabled.  Since this class has few external dependencies, callers need
     * not be concerned that calling this method will trigger initialization
     * of the OGL pipeline and related classes.
     */
    public static void sync() {
        if (theInstance != null) {
            theInstance.lock();
            try {
                theInstance.ensureCapacity(4);
                theInstance.getBuffer().putInt(SYNC);
                theInstance.flushNow();
            } finally {
                theInstance.unlock();
            }
        }
    }

    /**
     * Disposes the native memory associated with the given native
     * graphics config info pointer on the single queue flushing thread.
     */
    public static void disposeGraphicsConfig(long pConfigInfo) {
        OGLRenderQueue rq = getInstance();
        rq.lock();
        try {
            // make sure we make the context associated with the given
            // GraphicsConfig current before disposing the native resources
            OGLContext.setScratchSurface(pConfigInfo);

            RenderBuffer buf = rq.getBuffer();
            rq.ensureCapacityAndAlignment(12, 4);
            buf.putInt(DISPOSE_CONFIG);
            buf.putLong(pConfigInfo);

            // this call is expected to complete synchronously, so flush now
            rq.flushNow();
        } finally {
            rq.unlock();
        }
    }

    /**
     * Returns true if the current thread is the OGL QueueFlusher thread.
     */
    public static boolean isQueueFlusherThread() {
        return (Thread.currentThread() == getInstance().flusher);
    }

    @Override
    public void flushNow(boolean sync) {
        // assert lock.isHeldByCurrentThread();
        try {
            flusher.flushNow(sync);
        } catch (Exception e) {
            System.err.println("exception in flushNow:");
            e.printStackTrace();
        }
    }

    @Override
    public void flushAndInvokeNow(Runnable r) {
        // assert lock.isHeldByCurrentThread();
        try {
            flusher.flushAndInvokeNow(r);
        } catch (Exception e) {
            System.err.println("exception in flushAndInvokeNow:");
            e.printStackTrace();
        }
    }

    private native void flushBuffer(long buf, int limit);

    private void flushBuffer() {
        // assert lock.isHeldByCurrentThread();
        int limit = buf.position();
        if (limit > 0) {
            // process the queue
            flushBuffer(buf.getAddress(), limit);
        }
        // reset the queue
        clear();
    }

    private final class QueueFlusher extends Thread {
        private final static long LATENCY = 100;
        private final static long FLUSH_THRESHOLD = 10; // was 4 in jb jdk8u
        private final static long DEFAULT_FLUSH_COUNT = LATENCY / FLUSH_THRESHOLD;

        private boolean needsFlush = false;
        private Runnable task;
        private Error error;

        QueueFlusher(ThreadGroup threadGroup) {
            super(threadGroup, "Java2D Queue Flusher");
            setDaemon(true);
            setPriority(Thread.MAX_PRIORITY);
        }

        public synchronized void flushNow() {
            flushNow(true);
        }

        public synchronized void flushNow(boolean sync) {
            // wake up the flusher
            needsFlush = true;
            if (!sync) {
// TODO: check possible bug who has the AWT lock ?
                return;
            }

            notify();
            // wait for flush to complete
            while (needsFlush) {
                try {
                    wait();
                } catch (InterruptedException e) {
                    // ignored
                }
            }

            // re-throw any error that may have occurred during the flush
            if (error != null) {
                throw error;
            }
        }

        public synchronized void flushAndInvokeNow(Runnable task) {
            this.task = task;
            flushNow();
        }

        @Override
        public synchronized void run() {
            boolean locked = false;
            int count;

            while (true) {
                
                while (!needsFlush) {
                    count = 0;
                    try {
                        /*
                         * Wait FLUSH_THRESHOLD ms then check if needsFlush is set by
                         * flushNow() call or we waited DEFAULT_FLUSH_COUNT times.
                         * If so, flush the queue.
                         */
                        wait(FLUSH_THRESHOLD);
                        /*
                         * We will automatically flush the queue if the
                         * following conditions apply:
                         *   - the wait() timed out DEFAULT_FLUSH_COUNT times
                         *   - we can lock the queue (without blocking)
                         *   - there is something in the queue to flush
                         * Otherwise, just continue (we'll flush eventually).
                         */
                        if (!needsFlush && (count >= DEFAULT_FLUSH_COUNT) &&
                                (locked = tryLock()))
                        {
                            if (buf.position() > 0) {
                                needsFlush = true;
                            } else {
                                locked = false;
                                unlock();
                            }
                        }
                    } catch (InterruptedException e) {
                        // ignored
                    }
                    count++;
                }
                // locked by either this thread (see locked flag) or by waiting thread:
                // TODO: check lock is always acquired when needsFlush = true ?
                try {
                    // reset the throwable state
                    error = null;
                    
                    // flush the buffer now
                    flushBuffer();
                    
                    // if there's a task, invoke that now as well
                    if (task != null) {
                        task.run();
                        task = null;
                    }
                } catch (Error e) {
                    error = e;
                } catch (Exception ex) {
                    System.err.println("exception in QueueFlusher:");
                    ex.printStackTrace();
                } finally {
                    if (locked) {
                        locked = false;
                        unlock();
                    }
                    // allow the waiting thread to continue
                    needsFlush = false;
                    notify();
                }
            }
        }
    }
}
