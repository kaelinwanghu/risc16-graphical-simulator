package engine.assembly.assembler;

import engine.assembly.AssemblyError;
import java.util.List;

/**
 * Pass 1 Parser - builds symbol table and parses instructions/directives
 * 
 * This runs AFTER pseudo-instruction expansion, so all instructions are real
 * 
 * For each token:
 * 1. If it has a label, define in symbol table at current address
 * 2. Parse the instruction or directive
 * 3. Increment address appropriately
 * 
 * Symbolic labels in instructions/directives are tracked as unresolved for Pass 2 to resolve
 */
public final class InitialParser {
    
    // Prevent instantiation
    private InitialParser() {}
    
    /**
     * Executes Pass 1 of assembly
     * 
     * @param tokens the tokens (after pseudo-instruction expansion!)
     * @param context the assembly context
     * 
     * @throws AssemblyException on first error (fail-fast)
     */
    public static void parse(List<Token> tokens, AssemblyContext context) {
        for (Token token : tokens) {
            // Step 1: Define label if present (at CURRENT address, before parsing)
            if (token.hasLabel()) {
                String label = token.getLabel();
                
                // Check for duplicate
                if (context.getSymbolTable().contains(label)) {
                    int firstDefinition = context.getSymbolTable().resolve(label);
                    throw new AssemblyException(token.getLineNumber(), "Label '" + label + "' already defined at address " + firstDefinition, token.getOriginalLine(), AssemblyError.ErrorType.DUPLICATE_LABEL);
                }
                
                // Define at current address
                context.getSymbolTable().define(label, context.getCurrentAddress());
            }
            
            // Step 2: Parse the statement
            String operation = token.getOperation();
            
            if (DirectiveHandler.isDirective(operation)) {
                DirectiveHandler.handleDirective(token, context);
            } else {
                // Must be a real instruction (pseudo-instructions were already expanded)
                InstructionParser.parseInstruction(token, context);
            }
        }
    }
}