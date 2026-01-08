package engine.execution.instructions;

import engine.execution.*;
import engine.isa.*;
import engine.memory.Memory;

/**
 * Executor for LW instruction (RRI-type)
 * 
 * LW regA, regB, immediate
 * Operation: regA = Memory[regB + immediate]
 * 
 * Loads a 16-bit word from memory at address (regB + immediate).
 * The effective address must be word-aligned (even).
 * 
 * Function type: LOAD
 */
public class LwExecutor implements InstructionExecutor {
    
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
            throw new ExecutionException(String.format("Invalid load address: 0x%04X", effectiveAddress), state.getPC(), instruction.toAssembly());
        }
        
        if (!memory.isWordAligned(effectiveAddress)) {
            throw new ExecutionException(String.format("Unaligned load address: 0x%04X", effectiveAddress), state.getPC(), instruction.toAssembly());
        }
        
        // Load word from memory
        short value;
        try {
            value = memory.readWord(effectiveAddress);
        } catch (IllegalArgumentException e) {
            throw new ExecutionException("Memory access failed: " + e.getMessage(), state.getPC(), instruction.toAssembly());
        }
        
        // Build new state
        ProcessorState newState = state.toBuilder()
            .setRegister(regA, value)
            .incrementPC(2)
            .incrementInstructions()
            .build();
        
        // Create execution result (cache timing would be added by cache system)
        ExecutionResult executionResult = ExecutionResult.load(regA, effectiveAddress);
        
        return new ExecutionContext(newState, executionResult);
    }
}