package gui.facade;

import engine.Processor;
import engine.assembly.assembler.Assembler;
import engine.assembly.AssemblyResult;
import engine.execution.ExecutionException;
import engine.execution.ProcessorState;
import engine.memory.Memory;
import engine.metadata.ProgramMetadata;

import java.util.ArrayList;
import java.util.List;

/**
 * Facade that wraps the V2 processor engine and provides a clean API for the GUI
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
    private AssemblyResult lastAssembly;
    
    public EngineFacade(int memorySize) {
        this.processor = new Processor(memorySize);
        this.observers = new ArrayList<>();
    }
    
    // Assembly
    
    public AssemblyResult assemble(String sourceCode) {
        AssemblyResult result = Assembler.assemble(sourceCode);
        this.lastAssembly = result;
        
        if (result.isSuccess()) {
            processor.loadProgram(result);
            notifyProgramLoaded(result);
        } else {
            notifyAssemblyError(result);
        }
        
        return result;
    }
    
    // Execution
    
    public void step() throws ExecutionException {
        ProcessorState oldState = processor.getState();
        
        try {
            processor.step();
        } catch (ExecutionException e) {
            notifyExecutionError(e);
            throw e;
        }
        
        ProcessorState newState = processor.getState();
        notifyStateChanged(oldState, newState);
        
        if (newState.isHalted()) {
            notifyHalt();
        }
    }
    
    public void run() throws ExecutionException {
        while (!processor.isHalted()) {
            step();
        }
    }
    
    public void reset() {
        processor.reset();
        notifyStateChanged(null, processor.getState());
    }
    
    public void clear() {
        processor.clear();
        this.lastAssembly = null;
        notifyStateChanged(null, processor.getState());
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
    
    private void notifyStateChanged(ProcessorState oldState, ProcessorState newState) {
        for (EngineObserver obs : observers) {
            obs.onStateChanged(oldState, newState);
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
}