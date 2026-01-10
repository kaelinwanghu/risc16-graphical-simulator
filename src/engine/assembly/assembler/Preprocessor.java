package engine.assembly.assembler;

import engine.assembly.AssemblyError;
import java.util.ArrayList;
import java.util.List;

/**
 * Preprocessor for assembly source code
 * 
 * Handles in a SINGLE pass:
 * - Comment removal (# to end of line)
 * - Label extraction (text before ':')
 * - Operand splitting
 * - Blank line filtering
 * 
 * Output: List of Token objects ready for pseudo-instruction expansion and two-pass parsing
 */
public final class Preprocessor {
    
    // Prevent instantiation
    private Preprocessor() {}
    
    /**
     * Preprocesses assembly source code into tokens.
     * 
     * @param sourceCode the raw assembly source
     * 
     * @return list of tokens (one per non-blank line)
     * 
     * @throws AssemblyException on first error (fail-fast)
     */
    public static List<Token> preprocess(String sourceCode) {
        String[] lines = sourceCode.split("\\r?\\n");
        List<Token> tokens = new ArrayList<>();
        
        for (int i = 0; i < lines.length; i++) {
            int lineNumber = i + 1;
            String line = lines[i];
            
            // Remove comment
            String cleanLine = removeComment(line).trim();
            
            if (cleanLine.isEmpty()) {
                continue;
            }
            
            // Tokenize the line
            Token token = tokenizeLine(cleanLine, lineNumber, line);
            tokens.add(token);
        }
        
        return tokens;
    }
    
    /**
     * Tokenizes a single line into a Token.
     * 
     * @param cleanLine the line with comments removed and trimmed
     * @param lineNumber the original source line number
     * @param originalLine the original source line (for error messages)
     * 
     * @return the Token representing this line
     */
    private static Token tokenizeLine(String cleanLine, int lineNumber, String originalLine) {
        String label = null;
        String statement = cleanLine;
        
        // Check for label
        if (cleanLine.contains(":")) {
            int colonIndex = cleanLine.indexOf(':');
            label = cleanLine.substring(0, colonIndex).trim();
            statement = cleanLine.substring(colonIndex + 1).trim();
            
            // Validate label
            if (label.isEmpty()) {
                throw new AssemblyException(lineNumber, "Empty label", originalLine, AssemblyError.ErrorType.LABEL_SYNTAX_ERROR);
            }
            
            if (!isValidLabel(label)) {
                throw new AssemblyException(lineNumber, "Invalid label '" + label + "'. Labels must contain only letters, digits, '.', or '_'", originalLine, AssemblyError.ErrorType.LABEL_SYNTAX_ERROR);
            }
            
            // Label must be followed by a statement
            if (statement.isEmpty()) {
                throw new AssemblyException(lineNumber, "Label '" + label + "' must be followed by an instruction or directive", originalLine, AssemblyError.ErrorType.SYNTAX_ERROR);
            }
        }
        
        // Split statement into operation and operands
        String operation;
        String[] operands;
        
        int firstSpace = statement.indexOf(' ');
        int firstTab = statement.indexOf('\t');
        int splitPos = -1;
        
        if (firstSpace >= 0 && firstTab >= 0) {
            splitPos = Math.min(firstSpace, firstTab);
        } else if (firstSpace >= 0) {
            splitPos = firstSpace;
        } else if (firstTab >= 0) {
            splitPos = firstTab;
        }
        
        if (splitPos >= 0) {
            operation = statement.substring(0, splitPos).toLowerCase();
            String operandStr = statement.substring(splitPos + 1).trim();
            
            // Split operands by comma and/or whitespace
            operands = splitOperands(operandStr);
        } else {
            throw new AssemblyException(lineNumber, "Missing operands in statement", originalLine, AssemblyError.ErrorType.SYNTAX_ERROR);
        }
        
        return new Token(lineNumber, label, operation, operands, originalLine);
    }
    
    /**
     * Splits operand string by commas and/or whitespace.
     * 
     * @param operandStr the operand string to split
     * 
     * @return an array of individual operands
     */
    private static String[] splitOperands(String operandStr) {
        // Split by comma and/or whitespace, then filter empty strings
        String[] parts = operandStr.split("[,\\s]+");
        List<String> operands = new ArrayList<>();
        
        for (String part : parts) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                operands.add(trimmed);
            }
        }
        
        return operands.toArray(new String[0]);
    }
    
    /**
     * Removes comments from a line.
     * Comments start with '#' and continue to end of line.
     * 
     * @param line the original line
     * 
     * @return the line with comments removed
     */
    private static String removeComment(String line) {
        int commentIndex = line.indexOf('#');
        if (commentIndex >= 0) {
            return line.substring(0, commentIndex);
        }
        return line;
    }
    
    /**
     * Validates a label name. Valid labels contain only letters, digits, '.', or '_'.
     * 
     * @param label the label to validate
     * 
     * @return true if valid, false otherwise
     */
    private static boolean isValidLabel(String label) {
        if (label.isEmpty()) {
            return false;
        }
        
        for (char c : label.toCharArray()) {
            if (!Character.isLetterOrDigit(c) && c != '_' && c != '.') {
                return false;
            }
        }
        
        return true;
    }
}