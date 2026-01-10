package engine.assembly.assembler;

/**
 * Constants defining immediate value ranges for RiSC-16 instructions
 * 
 * This centralizes all magic numbers related to immediate field constraints,
 * making validation consistent and the codebase more maintainable
 */
public final class ImmediateRanges {
    
    // Prevent instantiation
    private ImmediateRanges() {}
    
    // RRI-type: 7-bit signed immediate
    public static final int RRI_MIN = -64;
    public static final int RRI_MAX = 63;
    public static final int RRI_BITS = 7;
    
    // RI-type: 10-bit unsigned immediate (LUI)
    public static final int RI_MIN = 0;
    public static final int RI_MAX = 1023;  // 0x3FF
    public static final int RI_BITS = 10;
    
    // MOVI pseudo-instruction: 16-bit unsigned
    public static final int MOVI_MIN = 0;
    public static final int MOVI_MAX = 65535;  // 0xFFFF
    
    // Bit manipulation for MOVI and LLI
    public static final int LLI_MASK = 0x3F;       // Lower 6 bits
    public static final int LUI_SHIFT = 6;          // Shift for upper 10 bits
    
    /**
     * Validates a 7-bit signed immediate (for RRI instructions)
     */
    public static boolean isValidRRI(int value) {
        return value >= RRI_MIN && value <= RRI_MAX;
    }
    
    /**
     * Validates a 10-bit unsigned immediate (for LUI)
     */
    public static boolean isValidRI(int value) {
        return value >= RI_MIN && value <= RI_MAX;
    }
    
    /**
     * Validates a 16-bit unsigned immediate (for MOVI)
     */
    public static boolean isValidMOVI(int value) {
        return value >= MOVI_MIN && value <= MOVI_MAX;
    }
}
