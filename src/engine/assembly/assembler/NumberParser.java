package engine.assembly.assembler;

/**
 * Utility for parsing numbers in different formats
 * 
 * Supports:
 * - Decimal: 42, -17
 * - Hexadecimal: 0x2A, 0xFF
 * - Octal: 052, 0377
 * 
 * Returns null if the string is not a valid number (could be a label)
 */
public final class NumberParser {
    
    // Prevent instantiation
    private NumberParser() {}
    
    /**
     * Attempts to parse a string as an integer
     * 
     * @param str the string to parse
     * 
     * @return the parsed integer, or null if not a number
     */
    public static Integer parse(String str) {
        if (str == null || str.isEmpty()) {
            return null;
        }
        
        try {
            // Hexadecimal: 0x... or 0X...
            if (str.matches("0[xX][0-9a-fA-F]+")) {
                return Integer.parseInt(str.substring(2), 16);
            }
            
            // Octal: 0... (but not just "0")
            if (str.matches("0[0-7]+")) {
                return Integer.parseInt(str.substring(1), 8);
            }
            
            // Decimal: -?\d+
            if (str.matches("-?\\d+")) {
                return Integer.parseInt(str);
            }
            
            // Not a recognized number format
            return null;
            
        } catch (NumberFormatException e) {
            return null;
        }
    }
    
    /**
     * Checks if a string represents a number (not a label)
     * 
     * @param str the string to check
     * 
     * @return true if the string is a valid number
     */
    public static boolean isNumber(String str) {
        return parse(str) != null;
    }
}