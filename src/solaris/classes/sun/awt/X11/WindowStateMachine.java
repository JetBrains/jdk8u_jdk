package sun.awt.X11;

import sun.util.logging.PlatformLogger;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

    private Map<WindowInfo, State> waitingWindows = new LinkedHashMap<>();

    class WindowInfo {
        public WindowInfo(long id, long time) {
            this.id = id;
            this.time = time;
        }

        private long id;
        private long time;
    }

    public void waitForNotifyAfterRaise (Long windowId) {
        focusLog.finer("Window: " + Long.toHexString(windowId), new Throwable());
        waitingWindows.put(new WindowInfo(windowId, System.nanoTime()), State.RAISED_AND_WAITING_FOR_MAP_NOTIFY);
    }

    public void waitForNotifyAfterMap (Long windowId) {
        waitingWindows.put(new WindowInfo(windowId, System.nanoTime()), State.MAPPED_AND_WAITING_FOR_MAP_NOTIFY);
    }

    public void notify(Long windowId) {
        List<WindowInfo> windows = waitingWindows.entrySet().stream().
                filter(windowInfoStateEntry -> windowInfoStateEntry.getKey().id == windowId).
                map(Map.Entry::getKey).
                collect(Collectors.toList());
        focusLog.finer("Remove " + windows.size() + "windows");
        windows.forEach(wId -> waitingWindows.remove(wId));
    }

    public boolean isWaitingForWindowShow () {

        double timeout = 3 * 1E9;

        List<WindowInfo> windows2 = waitingWindows.entrySet().stream().
          filter(windowInfoStateEntry -> {
              long timeKept = System.nanoTime() - windowInfoStateEntry.getKey().time;
              focusLog.finer("Window has been kept " + timeKept);
              return timeKept > timeout;
          }).
          map(Map.Entry::getKey).
          collect(Collectors.toList());
        focusLog.finer("Remove " + windows2.size() + "windows by timout");
        windows2.forEach(wId -> waitingWindows.remove(wId));

        return !waitingWindows.isEmpty();
    }

    public void clear() {
        waitingWindows.clear();
    }

    @Override
    public String toString() {
        return "Window state machine: " + waitingWindows.entrySet().stream().
                collect(StringBuilder::new, (sb, e) -> sb.append(Long.toHexString(e.getKey().id)).append(" : ").append(e.getValue()).append("; "), StringBuilder::append);
    }
}
