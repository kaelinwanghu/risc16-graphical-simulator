package engine.execution.instructions;

import engine.execution.*;
import engine.isa.*;
import engine.memory.Memory;

/**
 * Executor for JALR instruction (RRI-type)
 * 
 * JALR regA, regB
 * Operation: regA = PC + 2; PC = regB
 * 
 * Jumps to the address in regB and stores the return address (PC+2) in regA.
 * 
 * Uses:
 * - Function calls: Store return address, jump to function
 * - Function returns: JALR R0, R7 (jump to R7, discard return address)
 * - Indirect jumps: JALR R0, RX (jump to RX, discard return address)
 * - HALT: JALR R0, R0 (jump to 0, which typically contains nothing or halt loop)
 * 
 * Note: The immediate field exists in the encoding but is typically 0.
 * Non-zero immediates can be used for syscalls in extended implementations.
 * 
 * Function type: JUMP_AND_LINK
 * Cycles: 1 (base execution)
 */
public class JalrExecutor implements InstructionExecutor {
    
    @Override
    public ExecutionContext execute(InstructionFormat instruction, ProcessorState state, Memory memory) throws ExecutionException {
        // Extract operands
        int regA = instruction.getRegA();
        int regB = instruction.getRegB();
        
        // Get target address from regB
        short targetAddr = state.getRegister(regB);
        int target = targetAddr & 0xFFFF;  // Treat as unsigned
        
        // Calculate return address (next instruction)
        int returnAddr = state.getPC() + 2;
        
        // Check for HALT pattern (JALR R0, R0)
        boolean isHalt = (regA == 0 && regB == 0);
        
        // Build new state
        ProcessorState.Builder stateBuilder = state.toBuilder()
            .setRegister(regA, (short) returnAddr)  // Store return address
            .setPC(target)                          // Jump to target
            .incrementInstructions()
            .incrementCycles(1);
        
        // If this is HALT, mark processor as halted
        if (isHalt) {
            stateBuilder.setHalted(true);
        }
        
        ProcessorState newState = stateBuilder.build();
        
        // Create execution result
        ExecutionResult execResult = ExecutionResult.jumpAndLink(regA, target);
        
        return new ExecutionContext(newState, execResult);
    }
}