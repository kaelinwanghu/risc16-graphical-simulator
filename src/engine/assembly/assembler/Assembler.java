package engine.assembly.assembler;

import engine.assembly.AssemblyResult;
import java.util.List;

/**
 * Main entry point for the RiSC-16 assembler.
 * 
 * This assembler uses a clean, modular, two-pass architecture:
 * 
 * PREPROCESSING:
 *   1. Tokenization (Preprocessor) - Remove comments, extract labels, split operands
 *   2. Pseudo-instruction expansion (PseudoInstructionExpander)
 * 
 * PASS 1 (InitialParser):
 *   3. Build symbol table (labels → addresses)
 *   4. Parse instructions and directives
 *   5. Track unresolved symbolic references
 * 
 * PASS 2 (LabelResolver):
 *   6. Resolve all symbolic labels
 *   7. Validate offsets are in range
 * 
 */
public final class Assembler {
    
    // Prevent instantiation
    private Assembler() {}
    
    /**
     * Assembles RiSC-16 assembly source code.
     * 
     * @param sourceCode the assembly source code
     * @return AssemblyResult containing instructions/data or error
     */
    public static AssemblyResult assemble(String sourceCode) {
        try {
            return assembleInternal(sourceCode);
        } catch (AssemblyException e) {
            return AssemblyResult.error(e.getError());
        } catch (Exception e) {
            // Unexpected internal error
            return AssemblyResult.error(new engine.assembly.AssemblyError(
                1, "Internal assembler error: " + e.getMessage(), "",
                engine.assembly.AssemblyError.ErrorType.SYNTAX_ERROR));
        }
    }
    
    /**
     * Internal assembly implementation (throws on error for fail-fast)
     * 
     * @param sourceCode the assembly source 
     * 
     * @return AssemblyResult containing the assembled program and metadata
     */
    private static AssemblyResult assembleInternal(String sourceCode) {
        // Validate input
        if (sourceCode == null || sourceCode.trim().isEmpty()) {
            throw new AssemblyException(engine.assembly.AssemblyError.emptyProgram());
        }
        
        // PREPROCESSING PHASE
        
        // Step 1: Tokenize (remove comments, extract labels, split operands)
        List<Token> tokens = Preprocessor.preprocess(sourceCode);
        
        // Check for empty program after tokenization
        if (tokens.isEmpty()) {
            throw new AssemblyException(engine.assembly.AssemblyError.emptyProgram());
        }
        
        // Step 2: Expand pseudo-instructions
        tokens = PseudoInstructionExpander.expandAll(tokens);
        
        // PASS 1: Build symbol table and parse instructions/data
        
        AssemblyContext context = new AssemblyContext();
        InitialParser.parse(tokens, context);
        
        // Check that something has been assembled
        if (context.getInstructions().isEmpty() && context.getDataSegments().isEmpty()) {
            throw new AssemblyException(engine.assembly.AssemblyError.emptyProgram());
        }
        
        // PASS 2: Resolve symbolic labels
        
        LabelResolver.resolveAll(context);
        
        // BUILD RESULT
        
        return AssemblyResult.builder()
            .addInstructions(context.getInstructions())
            .addDataSegments(context.getDataSegments())
            .symbolTable(context.getSymbolTable())
            .build();
    }
}