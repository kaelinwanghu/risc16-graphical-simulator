package engine.assembly.assembler;

import engine.assembly.AssemblyError;

/**
 * Exception thrown during assembly for fail-fast error handling
 * 
 * This is a RuntimeException that wraps AssemblyError to provide:
 * 1. Fail-fast behavior (stop on first error)
 * 2. Standard Java exception handling (try-catch, throws declarations)
 * 3. Rich error context (from AssemblyError)
 * 
 * The assembler throws this exception on the first error encountered,
 * which is then caught and converted to AssemblyResult.error() at the top/GUI level
 */
public class AssemblyException extends RuntimeException {
    private final transient AssemblyError error;
    
    /**
     * Creates an AssemblyException wrapping an AssemblyError
     * 
     * @param error the AssemblyError
     */
    public AssemblyException(AssemblyError error) {
        super(error.getMessage());
        this.error = error;
    }
    
    /**
     * Creates an AssemblyException from error details
     * 
     * @param lineNumber the line number of the error
     * @param message the error message
     * @param sourceLine the source line that caused the error
     * @param type the error type
     */
    public AssemblyException(int lineNumber, String message, String sourceLine, AssemblyError.ErrorType type) {
        this(new AssemblyError(lineNumber, message, sourceLine, type));
    }
    
    /**
     * Gets the underlying AssemblyError
     */
    public AssemblyError getError() {
        return error;
    }
}