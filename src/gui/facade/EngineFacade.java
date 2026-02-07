package gui.facade;

import engine.Processor;
import engine.assembly.assembler.Assembler;
import engine.debug.Breakpoint;
import engine.debug.BreakpointException;
import engine.debug.DebugManager;
import engine.debug.ExecutionSnapshot;
import engine.assembly.AssemblyResult;
import engine.execution.ExecutionException;
import engine.execution.ProcessorState;
import engine.memory.Memory;
import engine.metadata.ProgramMetadata;
import engine.execution.ExecutionResult;
import engine.execution.InstructionExecutor;

import java.util.ArrayList;
import java.util.List;

/**
 * Facade that wraps the V2 processor engine and provides a clean API for the
 * GUI
 * 
 * This class:
 * - Manages processor lifecycle
 * - Handles assembly and execution
 * - Notifies observers of state changes
 * - Provides query methods for GUI
 */
public class EngineFacade {
    private final Processor processor;
    private final List<EngineObserver> observers;
    private final DebugManager debugManager;
    private AssemblyResult lastAssembly;

    public EngineFacade(int memorySize) {
        this.processor = new Processor(memorySize);
        this.observers = new ArrayList<>();
        this.debugManager = new DebugManager();
    }

    // Assembly

    public AssemblyResult assemble(String sourceCode) {
        AssemblyResult result = Assembler.assemble(sourceCode);
        this.lastAssembly = result;

        if (result.isSuccess()) {
            processor.clear();
            processor.loadProgram(result);
            notifyProgramLoaded(result);
        } else {
            notifyAssemblyError(result);
        }

        return result;
    }

    // Execution

    public void step() throws ExecutionException, BreakpointException {
        ProcessorState oldState = processor.getState();
        if (debugManager.isEnabled()) {
            String description = String.format("Before instruction at 0x%04X", oldState.getPC());
            debugManager.captureSnapshot(oldState, processor.getMemory(), description);
        }

        InstructionExecutor.ExecutionContext context;

        try {
            context = processor.stepWithContext();
        } catch (ExecutionException e) {
            notifyExecutionError(e);
            throw e;
        }

        ProcessorState newState = processor.getState();
        ExecutionResult result = context.getResult();

        if (shouldBreakAtCurrentState(newState)) {
            int sourceLine = addressToSourceLine(newState.getPC());
            Breakpoint bp = debugManager.getBreakpoint(sourceLine);

            // Notify observers BEFORE throwing
            notifyStateChanged(oldState, newState, result);

            // Throw exception to stop execution
            throw new BreakpointException(newState, sourceLine, bp);
        }
        notifyStateChanged(oldState, newState, result);

        if (newState.isHalted()) {
            notifyHalt();
        }
    }

    public void run() throws ExecutionException, BreakpointException {
        while (!processor.isHalted()) {
            step();
        }
    }

    public void reset() {
        processor.reset();
        notifyStateChanged(null, processor.getState(), null);
    }

    public void clear() {
        processor.clear();
        this.lastAssembly = null;
        notifyStateChanged(null, processor.getState(), null);
    }

    // State queries

    public ProcessorState getState() {
        return processor.getState();
    }

    public short getRegister(int regNum) {
        return processor.getRegister(regNum);
    }

    public int getPC() {
        return processor.getPC();
    }

    public boolean isHalted() {
        return processor.isHalted();
    }

    public Memory getMemory() {
        return processor.getMemory();
    }

    public ProgramMetadata getMetadata() {
        return processor.getMetadata();
    }

    public AssemblyResult getLastAssembly() {
        return lastAssembly;
    }

    public Processor getProcessor() {
        return processor;
    }

    // Configuration

    public void setInstructionLimit(int limit) {
        processor.setInstructionLimit(limit);
    }

    public int getInstructionLimit() {
        return processor.getInstructionLimit();
    }

    // Observer management

    public void addObserver(EngineObserver observer) {
        observers.add(observer);
    }

    public void removeObserver(EngineObserver observer) {
        observers.remove(observer);
    }

    private void notifyStateChanged(ProcessorState oldState, ProcessorState newState, ExecutionResult result) {
        for (EngineObserver obs : observers) {
            obs.onStateChanged(oldState, newState, result);
        }
    }

    private void notifyProgramLoaded(AssemblyResult result) {
        for (EngineObserver obs : observers) {
            obs.onProgramLoaded(result);
        }
    }

    private void notifyAssemblyError(AssemblyResult result) {
        for (EngineObserver obs : observers) {
            obs.onAssemblyError(result);
        }
    }

    private void notifyExecutionError(ExecutionException error) {
        for (EngineObserver obs : observers) {
            obs.onExecutionError(error);
        }
    }

    private void notifyHalt() {
        for (EngineObserver obs : observers) {
            obs.onHalt();
        }
    }

    public DebugManager getDebugManager() {
        return debugManager;
    }

    /**
     * Restores execution state from a snapshot
     */
    public void restoreSnapshot(ExecutionSnapshot snapshot) {
        if (!debugManager.isEnabled()) {
            throw new IllegalStateException("Debugging is not enabled");
        }

        if (processor.isHalted()) {
            throw new IllegalStateException(
                    "Cannot restore snapshot: processor is halted. Reassemble the program first.");
        }

        if (lastAssembly == null || !lastAssembly.isSuccess()) {
            throw new IllegalStateException("Cannot restore snapshot: no program is loaded. Assemble a program first.");
        }
        // Restore memory
        processor.getMemory().writeBytes(0, snapshot.getMemorySnapshot());

        // Restore processor state by building a new one
        // (We can't directly set the state, so we need to use reset + manual setup)
        // This is a limitation we'll need to address

        // For now, we'll need to add a method to Processor to allow state restoration
        processor.restoreState(snapshot.getState());

        // Notify observers
        notifyStateChanged(null, processor.getState(), null);
    }

    /**
     * Maps a PC address to a source line number
     * Returns -1 if no mapping exists
     */
    private int addressToSourceLine(int address) {
        if (lastAssembly == null) {
            return -1;
        }
        return processor.getMetadata().getSourceLine(address);
    }

    /**
     * Checks if execution should break at current state
     */
    private boolean shouldBreakAtCurrentState(ProcessorState state) {
        if (!debugManager.isEnabled()) {
            return false;
        }

        int sourceLine = addressToSourceLine(state.getPC());
        if (sourceLine == -1) {
            return false;
        }

        return debugManager.shouldBreak(sourceLine, state, processor.getMemory());
    }
}