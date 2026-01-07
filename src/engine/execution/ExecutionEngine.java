package engine.execution;

import engine.execution.instructions.*;
import engine.isa.*;
import engine.memory.Memory;
import engine.metadata.ProgramMetadata;

import java.util.EnumMap;
import java.util.Map;

/**
 * Core execution engine for RiSC-16 instructions
 * 
 * This engine:
 * - Fetches instructions from memory
 * - Decodes them using InstructionEncoder
 * - Dispatches to appropriate executor
 * - Updates and manages processor state
 * - Enforces instruction limits
 */
public class ExecutionEngine {
    private final Memory memory;
    private final Map<Opcode, InstructionExecutor> executors;
    private ProgramMetadata metadata;
    private int instructionLimit;
    
    /**
     * Creates an execution engine
     * 
     * @param memory the memory to execute from
     * @param metadata program metadata (optional, can be null)
     */
    public ExecutionEngine(Memory memory, ProgramMetadata metadata) {
        if (memory == null) {
            throw new IllegalArgumentException("Memory cannot be null");
        }
        
        this.memory = memory;
        this.metadata = metadata;
        this.instructionLimit = 65535;  // Default limit
        this.executors = new EnumMap<>(Opcode.class);
        
        // Register all instruction executors
        registerExecutors();
    }
    
    /**
     * Registers all instruction executors in the dispatch table
     */
    private void registerExecutors() {
        executors.put(Opcode.ADD, new AddExecutor());
        executors.put(Opcode.ADDI, new AddiExecutor());
        executors.put(Opcode.NAND, new NandExecutor());
        executors.put(Opcode.LUI, new LuiExecutor());
        executors.put(Opcode.LW, new LwExecutor());
        executors.put(Opcode.SW, new SwExecutor());
        executors.put(Opcode.BEQ, new BeqExecutor());
        executors.put(Opcode.JALR, new JalrExecutor());
    }
    
    /**
     * Executes a single instruction
     * 
     * @param state the current processor state
     * 
     * @return the new processor state and execution result
     * 
     * @throws ExecutionException if execution fails
     */
    public InstructionExecutor.ExecutionContext step(ProcessorState state) throws ExecutionException {
        // Check if halted
        if (state.isHalted()) {
            throw new ExecutionException("Processor is halted", state.getPC());
        }
        
        // Check instruction limit
        if (state.getInstructionCount() >= instructionLimit) {
            throw new ExecutionException(
                "Instruction limit of " + instructionLimit + " reached",
                state.getPC()
            );
        }
        
        int pc = state.getPC();
        
        // Validate PC is in bounds
        if (!memory.isValidAddress(pc) || !memory.isValidAddress(pc + 1)) {
            throw new ExecutionException(String.format("PC out of bounds: 0x%04X", pc), pc);
        }
        
        // Validate PC is word-aligned
        if (!memory.isWordAligned(pc)) {
            throw new ExecutionException(String.format("PC not word-aligned: 0x%04X", pc), pc);
        }
        
        // Check if PC points to an instruction (if metadata available)
        if (metadata != null && !metadata.isInstruction(pc)) {
            throw new ExecutionException("PC points to non-instruction memory", pc);
        }
        
        // Fetch and decode instruction
        InstructionFormat instruction;
        try {
            short binary = memory.readWord(pc);
            instruction = InstructionEncoder.decode(binary, pc);
        } catch (IllegalArgumentException e) {
            throw new ExecutionException("Failed to fetch/decode instruction: " + e.getMessage(), pc);
        }
        
        // Get executor for this opcode
        InstructionExecutor executor = executors.get(instruction.getOpcode());
        if (executor == null) {
            throw new ExecutionException("No executor for opcode: " + instruction.getOpcode(), pc, instruction.toAssembly());
        }
        
        // Execute instruction
        return executor.execute(instruction, state, memory);
    }
    
    /**
     * Executes instructions until halt or error
     * 
     * @param initialState the starting processor state
     * 
     * @return the final processor state
     * 
     * @throws ExecutionException if execution fails
     */
    public ProcessorState run(ProcessorState initialState) throws ExecutionException {
        ProcessorState state = initialState;
        
        while (!state.isHalted()) {
            InstructionExecutor.ExecutionContext context = step(state);
            state = context.getNewState();
        }
        
        return state;
    }
    
    /**
     * Sets the instruction limit
     * 
     * @param limit the maximum number of instructions to execute
     */
    public void setInstructionLimit(int limit) {
        if (limit < 1) {
            throw new IllegalArgumentException("Instruction limit must be at least 1");
        }
        this.instructionLimit = limit;
    }
    
    public int getInstructionLimit() {
        return instructionLimit;
    }
    
    public Memory getMemory() {
        return memory;
    }
    
    public ProgramMetadata getMetadata() {
        return metadata;
    }

    /* Mutable metadata for update after new program load */
    public void setMetadata(ProgramMetadata metadata) {
    	this.metadata = metadata;
	}
}