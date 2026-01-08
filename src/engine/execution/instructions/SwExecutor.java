package engine.execution.instructions;

import engine.execution.*;
import engine.isa.*;
import engine.memory.Memory;

/**
 * Executor for SW instruction (RRI-type)
 * 
 * SW regA, regB, immediate
 * Operation: Memory[regB + immediate] = regA
 * 
 * Stores a 16-bit word to memory at address (regB + immediate).
 * The effective address must be word-aligned (even).
 * 
 * Function type: STORE
 */
public class SwExecutor implements InstructionExecutor {
    
    @Override
    public ExecutionContext execute(InstructionFormat instruction, ProcessorState state, Memory memory) throws ExecutionException {
        // Extract operands
        int regA = instruction.getRegA();
        int regB = instruction.getRegB();
        int immediate = instruction.getImmediate();
        
        // Calculate effective address
        short baseAddr = state.getRegister(regB);
        int effectiveAddress = (baseAddr & 0xFFFF) + immediate;
        
        // Validate address
        if (!memory.isValidAddress(effectiveAddress)) {
            throw new ExecutionException(String.format("Invalid store address: 0x%04X", effectiveAddress), state.getPC(), instruction.toAssembly());
        }
        
        if (!memory.isWordAligned(effectiveAddress)) {
            throw new ExecutionException(String.format("Unaligned store address: 0x%04X", effectiveAddress), state.getPC(), instruction.toAssembly());
        }
        
        // Get value to store
        short value = state.getRegister(regA);
        
        // Store word to memory
        try {
            memory.writeWord(effectiveAddress, value);
        } catch (IllegalArgumentException e) {
            throw new ExecutionException("Memory access failed: " + e.getMessage(), state.getPC(),instruction.toAssembly());
        }
        
        // Build new state (SW doesn't modify registers)
        ProcessorState newState = state.toBuilder()
            .incrementPC(2)
            .incrementInstructions()
            .build();
        
        // Create execution result
        ExecutionResult executionResult = ExecutionResult.store(effectiveAddress);
        
        return new ExecutionContext(newState, executionResult);
    }
}