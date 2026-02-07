package engine.debug;

import engine.execution.ProcessorState;
import engine.memory.Memory;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Manages debugging features including snapshots, breakpoints, and watchpoints
 * 
 * This is the central hub for all debugging functionality
 */
public class DebugManager {
    private final List<ExecutionSnapshot> snapshots;
    private final Map<Integer, Breakpoint> breakpoints;
    private int snapshotLimit;
    private boolean enabled;

    public DebugManager() {
        this.snapshots = new ArrayList<>();
        this.snapshotLimit = 100; // Default limit
        this.enabled = false;
        this.breakpoints = new HashMap<>();
    }

    // ===== Configuration =====

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (!enabled) {
            clearSnapshots();
            clearBreakpoints();
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

    // ===== Breakpoint Management =====

    /**
     * Sets an unconditional breakpoint at a line number
     */
    public void setBreakpoint(int lineNumber) {
        if (!enabled) {
            return;
        }
        breakpoints.put(lineNumber, new Breakpoint(lineNumber));
    }

    /**
     * Sets a conditional breakpoint
     */
    public void setConditionalBreakpoint(int lineNumber, Breakpoint.WatchType watchType,
            int watchTarget, Breakpoint.Operator operator, int compareValue) {
        if (!enabled) {
            return;
        }
        breakpoints.put(lineNumber, new Breakpoint(lineNumber, watchType, watchTarget, operator, compareValue));
    }

    /**
     * Removes a breakpoint at a line number
     */
    public void removeBreakpoint(int lineNumber) {
        breakpoints.remove(lineNumber);
    }

    /**
     * Checks if a breakpoint exists at a line number
     */
    public boolean hasBreakpoint(int lineNumber) {
        return breakpoints.containsKey(lineNumber);
    }

    /**
     * Gets a specific breakpoint
     */
    public Breakpoint getBreakpoint(int lineNumber) {
        return breakpoints.get(lineNumber);
    }

    /**
     * Gets all breakpoint line numbers
     */
    public Set<Integer> getBreakpointLines() {
        return Collections.unmodifiableSet(breakpoints.keySet());
    }

    /**
     * Gets all breakpoints
     */
    public List<Breakpoint> getAllBreakpoints() {
        return new ArrayList<>(breakpoints.values());
    }

    /**
     * Clears all breakpoints
     */
    public void clearBreakpoints() {
        breakpoints.clear();
    }

    /**
     * Enables or disables a specific breakpoint
     */
    public void setBreakpointEnabled(int lineNumber, boolean enabled) {
        Breakpoint bp = breakpoints.get(lineNumber);
        if (bp != null) {
            bp.setEnabled(enabled);
        }
    }

    /**
     * Checks if execution should break at current state
     * 
     * @param lineNumber the current source line being executed
     * @param state      the current processor state
     * @param memory     the current memory state
     * @return true if a breakpoint condition is met
     */
    public boolean shouldBreak(int lineNumber, ProcessorState state, Memory memory) {
        if (!enabled) {
            return false;
        }

        Breakpoint bp = breakpoints.get(lineNumber);
        if (bp == null || !bp.isEnabled()) {
            return false;
        }

        // Unconditional breakpoint - always break
        if (!bp.isConditional()) {
            return true;
        }

        // Conditional breakpoint - evaluate condition
        return evaluateCondition(bp, state, memory);
    }

    /**
     * Evaluates a breakpoint condition
     */
    private boolean evaluateCondition(Breakpoint bp, ProcessorState state, Memory memory) {
        int actualValue;

        // Get the watched value
        switch (bp.getWatchType()) {
            case REGISTER:
                if (bp.getWatchTarget() < 0 || bp.getWatchTarget() > 7) {
                    return false; // Invalid register
                }
                actualValue = state.getRegister(bp.getWatchTarget()) & 0xFFFF;
                break;

            case MEMORY:
                try {
                    actualValue = memory.readWord(bp.getWatchTarget()) & 0xFFFF;
                } catch (Exception e) {
                    return false; // Invalid address
                }
                break;

            case PC:
                actualValue = state.getPC();
                break;

            case INSTRUCTION_COUNT:
                actualValue = (int) state.getInstructionCount();
                break;

            default:
                return false;
        }

        // Compare using operator
        int compareValue = bp.getCompareValue();

        switch (bp.getOperator()) {
            case EQUALS:
                return actualValue == compareValue;
            case NOT_EQUALS:
                return actualValue != compareValue;
            case LESS_THAN:
                return actualValue < compareValue;
            case LESS_EQUAL:
                return actualValue <= compareValue;
            case GREATER_THAN:
                return actualValue > compareValue;
            case GREATER_EQUAL:
                return actualValue >= compareValue;
            default:
                return false;
        }
    }
}