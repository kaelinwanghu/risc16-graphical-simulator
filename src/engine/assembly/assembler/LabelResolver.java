package engine.assembly.assembler;

import engine.assembly.AssemblyError;
import engine.isa.InstructionFormat;
import java.util.List;

/**
 * Pass 2 - Resolves symbolic labels to concrete offsets/addresses
 * 
 * After Pass 1, we have:
 * - Complete symbol table (all labels → addresses)
 * - List of unresolved references (symbolic labels in instructions/data)
 * 
 * This pass:
 * 1. Looks up each symbolic label in the symbol table
 * 2. Calculates the appropriate offset or address
 * 3. Validates the offset is in range
 * 4. Updates the instruction or data segment
 * 
 * Three types of references:
 * - BRANCH (BEQ): offset = target - (PC + 2)
 * - LOAD_STORE (LW/SW): offset = target - PC
 * - FILL (.fill): value = target address (absolute)
 */
public final class LabelResolver {
    
    // Prevent instantiation
    private LabelResolver() {}
    
    /**
     * Resolves all symbolic labels.
     * 
     * @param context the assembly context
     * 
     * @throws AssemblyException if any label is undefined or offset out of range
     */
    public static void resolveAll(AssemblyContext context) {
        List<UnresolvedReference> unresolvedReferences = context.getUnresolvedReferences();
        
        for (UnresolvedReference ref : unresolvedReferences) {
            resolveOne(ref, context);
        }
    }
    
    /**
     * Resolves a single unresolved reference
     * 
     * @param ref the unresolved reference
     * @param context the assembly context
     * 
     * @throws AssemblyException if the label is undefined or offset out of range
     */
    private static void resolveOne(UnresolvedReference ref, AssemblyContext context) {
        String label = ref.getLabel();
        
        // Look up label in symbol table
        if (!context.getSymbolTable().contains(label)) {
            throw new AssemblyException(ref.getLineNumber(), "Undefined label: '" + label + "'", ref.getOriginalLine(), AssemblyError.ErrorType.UNDEFINED_LABEL);
        }
        
        int targetAddress = context.getSymbolTable().resolve(label);
        
        switch (ref.getType()) {
            case BRANCH:
                resolveBranch(ref, targetAddress, context);
                break;
            case LOAD_STORE:
                resolveLoadStore(ref, targetAddress, context);
                break;
            case FILL:
                resolveFill(ref, targetAddress, context);
                break;
        }
    }
    
    /**
     * Resolves a branch instruction (BEQ)
     * 
     * Branch offset is relative to PC+1:
     *   offset = target - (PC + 2)
     * Where PC is the address of the BEQ instruction
     * 
     */
    private static void resolveBranch(UnresolvedReference ref, int targetAddress, AssemblyContext context) {
        int pc = ref.getCurrentAddress();
        int offset = targetAddress - (pc + 2);
        
        // Validate offset is in range
        if (!ImmediateRanges.isValidRRI(offset)) {
            throw new AssemblyException(ref.getLineNumber(), "Branch to label '" + ref.getLabel() + "' out of range (offset: " + offset + ", max: ±63)", ref.getOriginalLine(), AssemblyError.ErrorType.OUT_OF_RANGE);
        }
        
        // Update instruction
        InstructionFormat old = context.getInstruction(ref.getInstructionIndex());
        InstructionFormat updated = InstructionFormat.createRRI(old.getOpcode(), old.getRegA(), old.getRegB(), offset, old.getAddress());
        
        context.replaceInstruction(ref.getInstructionIndex(), updated);
    }
    
    /**
     * Resolves a load/store instruction (LW/SW)
     * 
     * Load/store offset is relative to PC:
     *   offset = target - PC
     * 
     * Where PC is the address of the LW/SW instruction
     */
    private static void resolveLoadStore(UnresolvedReference ref, int targetAddress, AssemblyContext context) {
        int pc = ref.getCurrentAddress();
        int offset = targetAddress - pc;
        
        // Validate offset is in range
        if (!ImmediateRanges.isValidRRI(offset)) {
            throw new AssemblyException(ref.getLineNumber(), "Load/store offset to label '" + ref.getLabel() + "' out of range (offset: " + offset + ", max: ±63)", ref.getOriginalLine(), AssemblyError.ErrorType.OUT_OF_RANGE);
        }
        
        // Update instruction
        InstructionFormat old = context.getInstruction(ref.getInstructionIndex());
        InstructionFormat updated = InstructionFormat.createRRI(old.getOpcode(), old.getRegA(), old.getRegB(), offset, old.getAddress());
        
        context.replaceInstruction(ref.getInstructionIndex(), updated);
    }
    
    /**
     * Resolves a .fill directive.
     * 
     * .fill with a label stores the label's absolute address.
     */
    private static void resolveFill(UnresolvedReference ref, int targetAddress, AssemblyContext context) {
        // No range check - addresses are 16-bit so always fit
        context.replaceDataSegment(ref.getDataIndex(), (short) targetAddress);
    }
}