package net.kdt.pojavlaunch.progresskeeper;

import android.util.Log;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class ProgressKeeper {
    private static final Map<String, List<ProgressListener>> sProgressListeners = new ConcurrentHashMap<>();
    private static final Map<String, ProgressState> sProgressStates = new ConcurrentHashMap<>();
    private static final CopyOnWriteArrayList<TaskCountListener> sTaskCountListeners = new CopyOnWriteArrayList<>();
    private static final AtomicInteger sTaskCount = new AtomicInteger(0);
    private static final Object sListenerLock = new Object();

    public static void submitProgress(String progressRecord, int progress, int resid, Object... va) {
        boolean isNewTask = false;
        boolean isFinished = false;

        synchronized (sProgressStates) {
            ProgressState progressState = sProgressStates.get(progressRecord);
            boolean alreadyExists = progressState != null;
            boolean finishRequested = resid == -1 && progress == -1;

            if (finishRequested) {
                if (alreadyExists) {
                    sProgressStates.remove(progressRecord);
                    isFinished = true;
                }
            } else {
                if (!alreadyExists) {
                    progressState = new ProgressState();
                    sProgressStates.put(progressRecord, progressState);
                    isNewTask = true;
                }
                progressState.progress = progress;
                progressState.resid = resid;
                progressState.varArg = va;
            }
        }

        if (isNewTask) {
            updateTaskCount(sTaskCount.incrementAndGet());
        } else if (isFinished) {
            updateTaskCount(sTaskCount.decrementAndGet());
        }

        List<ProgressListener> progressListeners = sProgressListeners.get(progressRecord);
        if (progressListeners != null) {
            for (ProgressListener listener : progressListeners) {
                if (isNewTask) listener.onProgressStarted();
                else if (isFinished) listener.onProgressEnded();
                else listener.onProgressUpdated(progress, resid, va);
            }
        }
    }

    private static void updateTaskCount(int count) {
        sTaskCountListeners.removeIf(listener -> listener.onUpdateTaskCount(count));
    }

    public static boolean hasProgressKey(String key) {
        return sProgressStates.containsKey(key);
    }

    public static void addListener(String progressRecord, ProgressListener listener) {
        ProgressState state;
        synchronized (sProgressStates) {
            state = sProgressStates.get(progressRecord);
        }

        if (state != null && (state.resid != -1 || state.progress != -1)) {
            listener.onProgressStarted();
            listener.onProgressUpdated(state.progress, state.resid, state.varArg);
        } else {
            listener.onProgressEnded();
        }

        List<ProgressListener> listenerList = sProgressListeners.get(progressRecord);
        if (listenerList == null) {
            synchronized (sListenerLock) {
                listenerList = sProgressListeners.get(progressRecord);
                if (listenerList == null) {
                    listenerList = new CopyOnWriteArrayList<>();
                    sProgressListeners.put(progressRecord, listenerList);
                }
            }
        }
        listenerList.add(listener);
    }

    public static void removeListener(String progressRecord, ProgressListener listener) {
        List<ProgressListener> listenerList = sProgressListeners.get(progressRecord);
        if (listenerList != null) listenerList.remove(listener);
    }

    public static void addTaskCountListener(TaskCountListener listener) {
        addTaskCountListener(listener, true);
    }

    public static void addTaskCountListener(TaskCountListener listener, boolean runUpdate) {
        if (runUpdate) {
            if (listener.onUpdateTaskCount(sTaskCount.get())) return;
        }
        sTaskCountListeners.addIfAbsent(listener);
    }

    public static void removeTaskCountListener(TaskCountListener listener) {
        sTaskCountListeners.remove(listener);
    }


    public static void waitUntilDone(final Runnable runnable) {
        if (getTaskCount() == 0) {
            runnable.run();
            return;
        }
        TaskCountListener listener = taskCount -> {
            if (taskCount == 0) {
                runnable.run();
                return true;
            }
            return false;
        };
        addTaskCountListener(listener);
    }

    public static int getTaskCount() {
        int count = sTaskCount.get();
        if (count < 0) {
            Log.w("ProgressKeeper", "Task count is negative: " + count + ". Resetting to 0.");
            sTaskCount.set(0);
            return 0;
        }
        return count;
    }

    public static boolean hasOngoingTasks() {
        return getTaskCount() > 0;
    }
}
