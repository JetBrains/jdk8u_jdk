package sun.awt.X11;

import sun.util.logging.PlatformLogger;

import java.util.LinkedHashMap;
import java.util.Map;

public class WindowStateMachine {

    private static final PlatformLogger focusLog = PlatformLogger.getLogger("sun.awt.X11.focus.WindowStateMachine");

    private static WindowStateMachine machine = new WindowStateMachine();

    public static WindowStateMachine get() {
        return machine;
    }

    private enum State {
        RAISED_AND_WAITING_FOR_MAP_NOTIFY,
        MAPPED_AND_WAITING_FOR_MAP_NOTIFY,
    }

    private Map<Long, State> waitingWindows = new LinkedHashMap<>();

    public void waitForNotifyAfterRaise (Long windowId) {
        focusLog.finer("Window: " + Long.toHexString(windowId), new Throwable());
        waitingWindows.put(windowId, State.RAISED_AND_WAITING_FOR_MAP_NOTIFY);
    }

    public void waitForNotifyAfterMap (Long windowId) {
        waitingWindows.put(windowId, State.MAPPED_AND_WAITING_FOR_MAP_NOTIFY);
    }

    public void notify(Long windowId) {
        waitingWindows.remove(windowId);
    }

    public boolean isWaitingForWindowShow () {
        return !waitingWindows.isEmpty();
    }

    public void clear() {
        waitingWindows.clear();
    }

    @Override
    public String toString() {
        return "Window state machine: " + waitingWindows.entrySet().stream().
                collect(StringBuilder::new, (sb, e) -> sb.append(Long.toHexString(e.getKey())).append(" : ").append(e.getValue()).append("; "), StringBuilder::append);
    }
}
