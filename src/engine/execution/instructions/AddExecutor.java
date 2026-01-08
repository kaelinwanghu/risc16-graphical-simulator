package engine.execution.instructions;

import engine.execution.*;
import engine.isa.*;
import engine.memory.Memory;

/**
 * Executor for ADD instruction (RRR-type)
 * 
 * ADD regA, regB, regC
 * Operation: regA = regB + regC
 * 
 * Function type: ADD
 */
public class AddExecutor implements InstructionExecutor {
    
    @Override
    public ExecutionContext execute(InstructionFormat instruction, ProcessorState state, Memory memory) throws ExecutionException {
        // Extract operands
        int regA = instruction.getRegA();
        int regB = instruction.getRegB();
        int regC = instruction.getRegC();
        
        // Get register values
        short valueB = state.getRegister(regB);
        short valueC = state.getRegister(regC);
        
        // Perform addition (wraps on overflow as per RiSC-16 spec)
        short result = (short) (valueB + valueC);
        
        // Build new state with updated register
        ProcessorState newState = state.toBuilder()
            .setRegister(regA, result)
            .incrementPC(2)  // All instructions are 2 bytes
            .incrementInstructions()
            .build();
        
        // Create execution result
        ExecutionResult executionResult = ExecutionResult.add(regA);
        
        return new ExecutionContext(newState, executionResult);
    }
}