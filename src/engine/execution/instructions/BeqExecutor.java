package engine.execution.instructions;

import engine.execution.*;
import engine.isa.*;
import engine.memory.Memory;

/**
 * Executor for BEQ instruction (RRI-type)
 * 
 * BEQ regA, regB, immediate
 * Operation: if (regA == regB) then PC = PC + 2 + immediate
 * 
 * Branches to (PC + 2 + immediate) if regA equals regB.
 * The immediate is a 7-bit signed offset (-64 to 63) in bytes.
 * 
 * Note: PC is always incremented by 2 first (to point to next instruction),
 * then the offset is added if the branch is taken.
 * 
 * Function type: BRANCH
 * Cycles: 1 (base execution)
 */
public class BeqExecutor implements InstructionExecutor {
    
    @Override
    public ExecutionContext execute(InstructionFormat instruction, ProcessorState state, Memory memory) throws ExecutionException {
        // Extract operands
        int regA = instruction.getRegA();
        int regB = instruction.getRegB();
        int immediate = instruction.getImmediate();
        
        // Get register values
        short valueA = state.getRegister(regA);
        short valueB = state.getRegister(regB);
        
        // Check if branch should be taken
        boolean taken = (valueA == valueB);
        
        // Calculate target address
        // PC is incremented by 2 (next instruction), then offset is added
        int targetPC = state.getPC() + 2 + immediate;
        
        // Build new state
        ProcessorState.Builder stateBuilder = state.toBuilder()
            .incrementInstructions()
            .incrementCycles(1);
        
        if (taken) {
            stateBuilder.setPC(targetPC);
        } else {
            stateBuilder.incrementPC(2);
        }
        
        ProcessorState newState = stateBuilder.build();
        
        // Create execution result
        ExecutionResult execResult = ExecutionResult.branch(taken, targetPC);
        
        return new ExecutionContext(newState, execResult);
    }
}