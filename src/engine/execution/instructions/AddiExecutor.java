package engine.execution.instructions;

import engine.execution.*;
import engine.isa.*;
import engine.memory.Memory;

/**
 * Executor for ADDI instruction (RRI-type)
 * 
 * ADDI regA, regB, immediate
 * Operation: regA = regB + immediate
 * 
 * The immediate is a 7-bit signed value (-64 to 63)
 * 
 * Function type: ADD
 */
public class AddiExecutor implements InstructionExecutor {
    
    @Override
    public ExecutionContext execute(InstructionFormat instruction, ProcessorState state, Memory memory) throws ExecutionException {
        // Extract operands
        int regA = instruction.getRegA();
        int regB = instruction.getRegB();
        int immediate = instruction.getImmediate();
        
        // Get register value
        short valueB = state.getRegister(regB);
        
        // Perform addition with immediate
        short result = (short) (valueB + immediate);
        
        // Build new state
        ProcessorState newState = state.toBuilder()
            .setRegister(regA, result)
            .incrementPC(2)
            .incrementInstructions()
            .build();
        
        // Create execution result
        ExecutionResult executionResult = ExecutionResult.add(regA);
        
        return new ExecutionContext(newState, executionResult);
    }
}