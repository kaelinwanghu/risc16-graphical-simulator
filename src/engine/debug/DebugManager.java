package engine.debug;

import engine.execution.ProcessorState;
import engine.memory.Memory;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Manages debugging features including snapshots, breakpoints, and watchpoints
 * 
 * This is the central hub for all debugging functionality
 */
public class DebugManager {
    private final List<ExecutionSnapshot> snapshots;
    private int snapshotLimit;
    private boolean enabled;

    public DebugManager() {
        this.snapshots = new ArrayList<>();
        this.snapshotLimit = 100; // Default limit
        this.enabled = false;
    }

    // ===== Configuration =====

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (!enabled) {
            clearSnapshots();
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setSnapshotLimit(int limit) {
        if (limit < 1) {
            throw new IllegalArgumentException("Snapshot limit must be at least 1");
        }
        this.snapshotLimit = limit;

        // Trim existing snapshots if over new limit
        trimSnapshots();
    }

    public int getSnapshotLimit() {
        return snapshotLimit;
    }

    // ===== Snapshot Management =====

    /**
     * Captures a snapshot of current execution state
     * 
     * @param state       the processor state
     * @param memory      the memory to snapshot
     * @param description human-readable description
     */
    public void captureSnapshot(ProcessorState state, Memory memory, String description) {
        if (!enabled) {
            return; // Don't capture if debugging is disabled
        }

        // Create snapshot with full memory copy
        byte[] memoryData = memory.readBytes(0, memory.getSize());
        ExecutionSnapshot snapshot = new ExecutionSnapshot(state, memoryData, description);

        snapshots.add(snapshot);

        // Enforce limit (FIFO - remove oldest)
        trimSnapshots();
    }

    /**
     * Removes oldest snapshots if over limit
     */
    private void trimSnapshots() {
        while (snapshots.size() > snapshotLimit) {
            snapshots.remove(0); // Remove oldest
        }
    }

    /**
     * Gets all snapshots (immutable view)
     */
    public List<ExecutionSnapshot> getSnapshots() {
        return Collections.unmodifiableList(snapshots);
    }

    /**
     * Gets a specific snapshot by index
     */
    public ExecutionSnapshot getSnapshot(int index) {
        if (index < 0 || index >= snapshots.size()) {
            throw new IllegalArgumentException("Invalid snapshot index: " + index);
        }
        return snapshots.get(index);
    }

    /**
     * Gets the number of snapshots
     */
    public int getSnapshotCount() {
        return snapshots.size();
    }

    /**
     * Clears all snapshots
     */
    public void clearSnapshots() {
        snapshots.clear();
    }

    /**
     * Gets the most recent snapshot, or null if none
     */
    public ExecutionSnapshot getLatestSnapshot() {
        if (snapshots.isEmpty()) {
            return null;
        }
        return snapshots.get(snapshots.size() - 1);
    }

    /**
     * Deletes a specific snapshot by index
     * 
     * @param index the index of the snapshot to delete
     */
    public void deleteSnapshot(int index) {
        if (index < 0 || index >= snapshots.size()) {
            throw new IllegalArgumentException("Invalid snapshot index: " + index);
        }
        snapshots.remove(index);
    }

    // TODO: Add breakpoint management here

    // TODO: Add watchpoint (memory/register watch) management here

    // TODO: Add memory editing validation/tracking here
}