package sun.awt.X11;

import java.util.LinkedHashMap;
import java.util.Map;

public class WindowStateMachine {
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
        System.err.println(windowId + " is waiting for rise");
        waitingWindows.put(windowId, State.RAISED_AND_WAITING_FOR_MAP_NOTIFY);
    }

    public void waitForNotifyAfterMap (Long windowId) {
        System.err.println(windowId + " is waiting for map");
        waitingWindows.put(windowId, State.MAPPED_AND_WAITING_FOR_MAP_NOTIFY);
    }

    public void notify(Long windowId) {
        waitingWindows.remove(windowId);
        System.err.println(windowId + " notified");
    }

    public boolean isWaitingForWindowShow () {
        return !waitingWindows.isEmpty();
    }

}
