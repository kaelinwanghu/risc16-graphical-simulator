package engine.isa;

/**
 * The three instruction format types in RiSC-16
 * 
 * - RRR: Register-Register-Register (3 registers, e.g., ADD R1, R2, R3)
 * - RRI: Register-Register-Immediate (2 registers + 7-bit signed immediate)
 * - RI: Register-Immediate (1 register + 10-bit unsigned immediate)
 */
public enum FormatType {
    /**
     * RRR-type: 3-bit opcode, 3 3-bit register fields, 4-bit unused
     * Format: [opcode:3][regA:3][regB:3][regC:3][unused:4]
     * Used by: ADD, NAND
     */
    RRR,
    
    /**
     * RRI-type: 3-bit opcode, 2 3-bit register fields, 7-bit signed immediate
     * Format: [opcode:3][regA:3][regB:3][immediate:7]
     * Used by: ADDI, LW, SW, BEQ, JALR
     */
    RRI,
    
    /**
     * RI-type: 3-bit opcode, 1 3-bit register field, 10-bit unsigned immediate
     * Format: [opcode:3][regA:3][immediate:10]
     * Used by: LUI
     */
    RI;
    
    /**
     * Returns a human-readable description of this format
     */
    public String getDescription() {
        switch (this) {
            case RRR:
                return "Register-Register-Register";
            case RRI:
                return "Register-Register-Immediate";
            case RI:
                return "Register-Immediate";
            default:
                return "Unknown";
        }
    }
}