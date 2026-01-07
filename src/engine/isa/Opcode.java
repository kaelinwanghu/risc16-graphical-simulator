package engine.isa;

public enum Opcode {
    ADD(0b000, FormatType.RRR, "add"),
    ADDI(0b001, FormatType.RRI, "addi"),
    NAND(0b010, FormatType.RRR, "nand"),
    LUI(0b011, FormatType.RI, "lui"),
    SW(0b100, FormatType.RRI, "sw"),
    LW(0b101, FormatType.RRI, "lw"),
    BEQ(0b110, FormatType.RRI, "beq"),
    JALR(0b111, FormatType.RRI, "jalr");

    private final int code;
    private final FormatType format;
    private final String mnemonic;

    Opcode(int code, FormatType format, String mnemonic) {
        this.code = code;
        this.format = format;
        this.mnemonic = mnemonic;
    }

    /**
     * Gets the assembly language mnemonic ("add", "lw", "beq", etc.)
     */
    public String getMnemonic() {
        return mnemonic;
    }

    /**
     * Decodes a 3-bit opcode value into an Opcode enum
     * 
     * @param code the 3-bit opcode (0-7)
     * @return the corresponding Opcode
     * @throws IllegalArgumentException if code is not a valid opcode
     */
    public static Opcode fromCode(int code) {
        for (Opcode op : values()) {
            if (op.code == code) {
                return op;
            }
        }
        throw new IllegalArgumentException("Invalid opcode: " + code);
    }

    /**
     * Looks up an opcode by its assembly mnemonic (case-insensitive)
     * 
     * @param mnemonic the instruction name (e.g., "ADD", "lw")
     * @return the corresponding Opcode, or null if not found
     */
    public static Opcode fromMnemonic(String mnemonic) {
        String lower = mnemonic.toLowerCase();
        for (Opcode op : values()) {
            if (op.mnemonic.equals(lower)) {
                return op;
            }
        }
        return null;
    }
    
    public static boolean isValidMnemonic(String mnemonic) {
        return fromMnemonic(mnemonic) != null;
    }

    @Override
    public String toString() {
        return mnemonic.toUpperCase();
    }

    public int getCode() {
        return code;
    }

    public FormatType getFormat() {
        return format;
    }
}
