package quality.jfx;

import com.sun.glass.ui.Application;
import com.sun.javafx.application.PlatformImpl;
import com.sun.jna.*;
import org.junit.Test;

import java.util.Map;
import java.util.Random;

public class Main {

    private interface GLib extends Library {
        interface GSourceFunc extends Callback {
            boolean invoke(Pointer userData);
        }
        GLib INSTANCE = (GLib)Native.loadLibrary("glib-2.0", GLib.class);
        void g_main_loop_run(Pointer loop);
        int g_idle_add(GSourceFunc function, Pointer data);
        Pointer g_main_loop_new(Pointer context, boolean dummy);
    }

    public static void main(String[] args) {
        tryUseApplication_Run();
    }

    private static void _runPostJFXTestCallbacks() {
        final Runnable jfxCallback = () -> System.out.println("Executed JFX-callback (added via Application.invokeLater), thread: " + Thread.currentThread().getName());
        while (true) {
            final Random rnd = new Random(System.currentTimeMillis());
            try {
                Thread.sleep(rnd.nextInt(5));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            Application.invokeLater(jfxCallback);
        }
    }

    private static void _startThreadToPostGlibTestCallbacks() {
        final Thread helperThread = new Thread(()-> {
            final GLib.GSourceFunc testCallback = (p)-> {
                System.out.println("Executed GSourceFunc (added via g_idle_add), thread: " + Thread.currentThread().getName());
                return false;
            };
            final Random rnd = new Random(System.currentTimeMillis());
            while (true) {
                try {
                    Thread.sleep(rnd.nextInt(5));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                GLib.INSTANCE.g_idle_add(testCallback, Pointer.NULL);
            }
        });
        helperThread.start();
    }

    // creates 2 main-loop threads and crashes when use them
    private static void crashWith2MainLoopThreads() {
        // 1. start main loop (via glib) in separate thread
        final Thread mainThread = new Thread(()->{
            System.out.println("Started Glib main-loop, thread: " + Thread.currentThread().getName());

            final Pointer ctx = GLib.INSTANCE.g_main_loop_new(Pointer.NULL, true);
            GLib.INSTANCE.g_main_loop_run(ctx);
        });
        mainThread.setName("Glib-main-loop");
        mainThread.start();

        // 2. start helper-thread that schedules execution of test-code in glib main loop
        _startThreadToPostGlibTestCallbacks();

        // 3. initialize JFX via PlatformImpl
        PlatformImpl.startup(() -> System.out.println("Started JFX, thread: " + Thread.currentThread().getName()));

        // 4. schedule execution of test-code in JFX main loop
        _runPostJFXTestCallbacks();
    }

    // use PlatformImpl.statup(...) to initialize main-loop (of glib)
    // doesn't creates secondary mail-loop-thread, but creates several unnecessary threads (that can cause low performance)
    private static void noCrashButCreatedUselessThreads() {
        final Map<Thread, StackTraceElement[]> allThreadsBefore = Thread.getAllStackTraces();

        // 1. start main loop (via PlatformImpl)
        PlatformImpl.startup(() -> System.out.println("Started JFX, thread: " + Thread.currentThread().getName()));

        // 2. ensure that several threads were created
        final Map<Thread, StackTraceElement[]> allThreadsAfter = Thread.getAllStackTraces();
        final int threadsDiff = allThreadsAfter.size() - allThreadsBefore.size();
        System.out.println("PlatformImpl.startup(..) has created " + threadsDiff + " threads");

//        Uncomment to ensure that glib main loop is running:
//        final GLib.GSourceFunc testCallback = (p)-> {
//            System.out.println("Executed GSourceFunc (test callback), thread: " + Thread.currentThread().getName());
//            return false;
//        };
//        GLib.INSTANCE.g_idle_add(testCallback, Pointer.NULL);

        // 3. start helper-thread that schedules execution of test-code in glib main loop
        _startThreadToPostGlibTestCallbacks();

        // 4. schedule execution of test-code in JFX main loop
        _runPostJFXTestCallbacks();
    }

    // use Application.run(...) to initialize main-loop (of glib)
    // with original JFX other parts of JFX can't be initialized with PlatformImpl.startup(...) because of IllegalStateException
    // (with fixed JFX everythng is OK)
    @Test
    private static void tryUseApplication_Run() {
        final Map<Thread, StackTraceElement[]> allThreadsBefore = Thread.getAllStackTraces();

        // 1. start main loop (via PlatformImpl)
        Application.run(() -> System.out.println("Started JFX (via Application.run), thread: " + Thread.currentThread().getName()));

        // 2. ensure that only one thread was created
        final Map<Thread, StackTraceElement[]> allThreadsAfter = Thread.getAllStackTraces();
        final int threadsDiff = allThreadsAfter.size() - allThreadsBefore.size();
        System.out.println("Application.run(..) has created " + threadsDiff + " threads");

        // 3. try initialize JFX
        try {
            PlatformImpl.startup(() -> System.out.println("Started JFX, thread: " + Thread.currentThread().getName()));
        } catch (IllegalStateException ise) {
            // IllegalStateException will be thrown with original javaFX
            // No exceptions will be thrown with fixed javaFX
            ise.printStackTrace();
        }
    }

}
