package engine.execution.instructions;

import engine.execution.*;
import engine.isa.*;
import engine.memory.Memory;

/**
 * Executor for LUI instruction (RI-type)
 * 
 * LUI regA, immediate
 * Operation: regA = immediate << 6
 * 
 * LUI (Load Upper Immediate) loads a 10-bit unsigned immediate into the
 * upper 10 bits of a register, setting the lower 6 bits to zero.
 * 
 * This is typically used in combination with ADDI (or the LLI pseudo-instruction)
 * to load a full 16-bit constant:
 *   LUI R1, (value >> 6)   # Load upper 10 bits
 *   ADDI R1, R1, (value & 0x3F)  # Add lower 6 bits
 * 
 * Function type: LOAD
 * Cycles: 1 (base execution)
 */
public class LuiExecutor implements InstructionExecutor {
    
    @Override
    public ExecutionContext execute(InstructionFormat instruction, ProcessorState state, Memory memory) throws ExecutionException {
        // Extract operands
        int regA = instruction.getRegA();
        int immediate = instruction.getImmediate();
        
        // Shift immediate left by 6 bits (upper 10 bits, lower 6 bits = 0)
        short result = (short) (immediate << 6);
        
        // Build new state
        ProcessorState newState = state.toBuilder()
            .setRegister(regA, result)
            .incrementPC(2)
            .incrementInstructions()
            .incrementCycles(1)
            .build();
        
        // Create execution result
        ExecutionResult execResult = ExecutionResult.load(regA, -1, 0);
        
        return new ExecutionContext(newState, execResult);
    }
}