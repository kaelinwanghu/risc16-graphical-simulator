package engine.isa;

/**
 * Immutable representation of a decoded RiSC-16 instruction
 * 
 * This class represents an immutable instruction in its final decoded form, with all fields
 * extracted and validated.
 */
public final class InstructionFormat {
    private final Opcode opcode;
    private final FormatType format;
    
    // Register fields (0-7, or -1 if unused)
    private final int regA;
    private final int regB;
    private final int regC;
    
    // Immediate field (interpretation depends on format)
    private final int immediate;
    
    // Address of this instruction for debugging
    private final int address;
    private final int sourceLine;
    
    /**
     * Private constructor - use static factory methods to create instances
     */
    private InstructionFormat(Opcode opcode, int regA, int regB, int regC, int immediate, int address, int sourceLine) {
        this.opcode = opcode;
        this.format = opcode.getFormat();
        this.regA = regA;
        this.regB = regB;
        this.regC = regC;
        this.immediate = immediate;
        this.address = address;
        this.sourceLine = sourceLine;
    }
    
    /**
     * Creates an RRR-format instruction
     * 
     * @param opcode the instruction opcode (must be RRR-type)
     * @param regA destination register (0-7)
     * @param regB first source register (0-7)
     * @param regC second source register (0-7)
     * @param address the memory address of this instruction
     * 
     * @return the decoded instruction
     */
    public static InstructionFormat createRRR(Opcode opcode, int regA, int regB, int regC, int address, int sourceLine) {
        if (opcode.getFormat() != FormatType.RRR) {
            throw new IllegalArgumentException(opcode + " is not an RRR instruction");
        }

        validateRegister(regA, "regA");
        validateRegister(regB, "regB");
        validateRegister(regC, "regC");
        
        return new InstructionFormat(opcode, regA, regB, regC, 0, address, sourceLine);
    }
    
    /**
     * Creates an RRI-format instruction
     * 
     * @param opcode the instruction opcode (must be RRI-type)
     * @param regA destination/source register (0-7)
     * @param regB source/base register (0-7)
     * @param immediate 7-bit signed immediate (-64 to 63)
     * @param address the memory address of this instruction
     * 
     * @return the decoded instruction
     */
    public static InstructionFormat createRRI(Opcode opcode, int regA, int regB, int immediate, int address, int sourceLine) {
        if (opcode.getFormat() != FormatType.RRI) {
            throw new IllegalArgumentException(opcode + " is not an RRI instruction");
        }
        validateRegister(regA, "regA");
        validateRegister(regB, "regB");
        validateImmediate(immediate, 7, true);
        
        return new InstructionFormat(opcode, regA, regB, -1, immediate, address, sourceLine);
    }
    
    /**
     * Creates an RI-format instruction
     * 
     * @param opcode the instruction opcode (must be RI-type)
     * @param regA destination register (0-7)
     * @param immediate 10-bit unsigned immediate (0 to 1023)
     * @param address the memory address of this instruction
     * 
     * @return the decoded instruction
     */
    public static InstructionFormat createRI(Opcode opcode, int regA, int immediate, int address, int sourceLine) {
        if (opcode.getFormat() != FormatType.RI) {
            throw new IllegalArgumentException(opcode + " is not an RI instruction");
        }
        validateRegister(regA, "regA");
        validateImmediate(immediate, 10, false);
        
        return new InstructionFormat(opcode, regA, -1, -1, immediate, address, sourceLine);
    }
        
    public Opcode getOpcode() {
        return opcode;
    }
    
    public FormatType getFormat() {
        return format;
    }
    
    public int getRegA() {
        return regA;
    }
    
    public int getRegB() {
        if (format == FormatType.RI) {
            throw new IllegalStateException("RI instructions do not have regB");
        }
        return regB;
    }
    
    public int getRegC() {
        if (format != FormatType.RRR) {
            throw new IllegalStateException("Only RRR instructions have regC");
        }
        return regC;
    }
    
    public int getImmediate() {
        if (format == FormatType.RRR) {
            throw new IllegalStateException("RRR instructions do not have immediate");
        }
        return immediate;
    }
    
    public int getAddress() {
        return address;
    }
    
    public int getSourceLine() {
        return sourceLine;
    }
    
    // Validators
    
    private static void validateRegister(int reg, String name) {
        if (reg < 0 || reg > 7) {
            throw new IllegalArgumentException(name + " must be 0-7, got: " + reg);
        }
    }
    
    private static void validateImmediate(int imm, int bits, boolean signed) {
        // Efficient range calculation for immediate (signed goes from 0 to 2^(bits-1)-1 and unsigned goes from -(2^(bits-1)) to -1)
        int min = signed ? -(1 << (bits - 1)) : 0;
        int max = signed ? (1 << (bits - 1)) - 1 : (1 << bits) - 1;
        if (imm < min || imm > max) {
            throw new IllegalArgumentException("Immediate must be " + bits + "-bit " + (signed ? "signed" : "unsigned") + " with " + min + " to " + max + ", got: " + imm);
        }
    }

    // Utility methods
        
    /**
     * Formats this instruction as assembly code for human readability
     * 
     * @return the assembly representation (e.g., "ADD r1, r2, r3")
     */
    public String toAssembly() {
        StringBuilder sb = new StringBuilder();
        sb.append(opcode.toString());
        sb.append(" ");
        
        switch (format) {
            case RRR:
                sb.append("r").append(regA).append(", ");
                sb.append("r").append(regB).append(", ");
                sb.append("r").append(regC);
                break;
                
            case RRI:
                sb.append("r").append(regA).append(", ");
                sb.append("r").append(regB).append(", ");
                sb.append(immediate);
                break;
                
            case RI:
                sb.append("r").append(regA).append(", ");
                sb.append(immediate);
                break;
        }
        
        return sb.toString();
    }
    
    @Override
    public String toString() {
        return String.format("InstructionFormat[%s at 0x%04X: %s]", 
            opcode, address, toAssembly());
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof InstructionFormat)) return false;
        
        InstructionFormat other = (InstructionFormat) obj;
        return opcode == other.opcode && regA == other.regA && regB == other.regB && regC == other.regC && immediate == other.immediate;
    }
    
    @Override
    public int hashCode() {
        int result = opcode.hashCode();
        result = 31 * result + regA;
        result = 31 * result + regB;
        result = 31 * result + regC;
        result = 31 * result + immediate;
        return result;
    }
}