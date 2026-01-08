package engine.execution.instructions;

import engine.execution.*;
import engine.isa.*;
import engine.memory.Memory;

/**
 * Executor for NAND instruction (RRR-type)
 * 
 * NAND regA, regB, regC
 * Operation: regA = ~(regB & regC)
 * 
 * NAND is functionally complete - all other boolean operations can be
 * constructed from NAND. For example:
 * - NOT A = NAND A, A
 * - AND A, B = NOT (NAND A, B)
 * - OR A, B = NAND (NOT A), (NOT B)
 * 
 * Function type: ALU
 */
public class NandExecutor implements InstructionExecutor {
    
    @Override
    public ExecutionContext execute(InstructionFormat instruction, ProcessorState state, Memory memory) throws ExecutionException {
        // Extract operands
        int regA = instruction.getRegA();
        int regB = instruction.getRegB();
        int regC = instruction.getRegC();
        
        // Get register values
        short valueB = state.getRegister(regB);
        short valueC = state.getRegister(regC);
        
        // Perform NAND operation
        short result = (short) (~(valueB & valueC));
        
        // Build new state
        ProcessorState newState = state.toBuilder()
            .setRegister(regA, result)
            .incrementPC(2)
            .incrementInstructions()
            .build();
        
        // Create execution result
        ExecutionResult executionResult = ExecutionResult.alu(regA);
        
        return new ExecutionContext(newState, executionResult);
    }
}