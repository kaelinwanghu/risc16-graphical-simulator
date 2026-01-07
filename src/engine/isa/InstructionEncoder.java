package engine.isa;

/**
 * Encodes and decodes RiSC-16 instructions between InstructionFormat and 16-bit binary in Big Endian
 * 
 * RRR-type: [15:13]=opcode [12:10]=regA [9:7]=regB [6:3]=0000 [2:0]=regC
 * RRI-type: [15:13]=opcode [12:10]=regA [9:7]=regB [6:0]=immediate (7-bit signed)
 * RI-type:  [15:13]=opcode [12:10]=regA [9:0]=immediate (10-bit unsigned)
 * 
 */
public class InstructionEncoder {
    
    // Prevent explicit instantiation
    private InstructionEncoder() {}
    
    /**
     * Encodes an InstructionFormat into a 16-bit binary representation
     * 
     * @param instruction the instruction to encode
     * 
     * @return the 16-bit binary encoding
     */
    public static short encode(InstructionFormat instruction) {
        int encoded = 0;
        
        // Bits [15:13] - Opcode (3 bits)
        encoded |= (instruction.getOpcode().getCode() & 0x7) << 13;
        
        // Bits [12:10] - RegA (3 bits)
        encoded |= (instruction.getRegA() & 0x7) << 10;
        
        switch (instruction.getFormat()) {
            case RRR:
                // Bits [9:7] - RegB (3 bits)
                encoded |= (instruction.getRegB() & 0x7) << 7;
                // Bits [6:3] - Unused (0000)
                // Bits [2:0] - RegC (3 bits)
                encoded |= (instruction.getRegC() & 0x7);
                break;
                
            case RRI:
                // Bits [9:7] - RegB (3 bits)
                encoded |= (instruction.getRegB() & 0x7) << 7;
                // Bits [6:0] - Immediate (7 bits, signed)
                encoded |= instruction.getImmediate() & 0x7F;
                break;
                
            case RI:
                // Bits [9:0] - Immediate (10 bits, unsigned)
                encoded |= instruction.getImmediate() & 0x3FF;
                break;
        }
        
        return (short) encoded;
    }
    
    /**
     * Decodes a 16-bit binary value into an InstructionFormat
     * 
     * @param binary the 16-bit instruction encoding
     * @param address the memory address this instruction came from
     * 
     * @return the decoded instruction
     * 
     * @throws IllegalArgumentException if the opcode is invalid
     */
    public static InstructionFormat decode(short binary, int address) {
        // Convert to unsigned int for bit manipulation
        int bits = binary & 0xFFFF;
        
        // Extract opcode (bits [15:13])
        int opcodeValue = (bits >> 13) & 0x7;
        Opcode opcode = Opcode.fromCode(opcodeValue);
        
        // Extract regA (bits [12:10])
        int regA = (bits >> 10) & 0x7;
        
        switch (opcode.getFormat()) {
            case RRR:
                // Extract regB (bits [9:7])
                int regB = (bits >> 7) & 0x7;
                // Extract regC (bits [2:0])
                int regC = bits & 0x7;
                return InstructionFormat.createRRR(opcode, regA, regB, regC, address);
                
            case RRI:
                // Extract regB (bits [9:7])
                regB = (bits >> 7) & 0x7;
                // Extract 7-bit signed immediate (bits [6:0])
                int imm7 = bits & 0x7F;
                // Sign extend from 7 bits to 32 bits
                if ((imm7 & 0x40) != 0) {
                    imm7 |= 0xFFFFFF80;
                }
                return InstructionFormat.createRRI(opcode, regA, regB, imm7, address);
                
            case RI:
                // Extract 10-bit unsigned immediate (bits [9:0])
                int imm10 = bits & 0x3FF;
                return InstructionFormat.createRI(opcode, regA, imm10, address);
                
            default:
                throw new IllegalStateException("Unknown format type: " + opcode.getFormat());
        }
    }

    /**
     * Decodes an instruction from two bytes (big-endian)
     * 
     * @param highByte the high byte (bits 15:8)
     * @param lowByte the low byte (bits 7:0)
     * @param address the memory address this instruction came from
     * 
     * @return the decoded instruction format
     */
    public static InstructionFormat decode(byte highByte, byte lowByte, int address) {
        // Combine bytes into a 16-bit word (big-endian)
        short binary = (short) (((highByte & 0xFF) << 8) | (lowByte & 0xFF));
        return decode(binary, address);
    }

    /**
     * Decodes an instruction from a byte array
     * 
     * @param bytes the byte array (must have at least 2 bytes at offset)
     * @param offset the offset in the array to start reading
     * @param address the memory address this instruction came from
     * 
     * @return the decoded instruction format
     * @throws IllegalArgumentException if array is too small
     */
    public static InstructionFormat decode(byte[] bytes, int offset, int address) {
        if (bytes == null || bytes.length < offset + 2) {
            throw new IllegalArgumentException("Byte array must have at least 2 bytes at offset " + offset);
        }
        return decode(bytes[offset], bytes[offset + 1], address);
    }
    
    /**
     * Encodes an instruction to its binary representation as a string
     * Useful for debugging and display purposes
     * 
     * @param instruction the instruction to encode
     * @return a 16-character binary string (e.g., "0000010100000011")
     */
    public static String encodeToBinaryString(InstructionFormat instruction) {
        short encoded = encode(instruction);
        return String.format("%16s", Integer.toBinaryString(encoded & 0xFFFF)).replace(' ', '0');
    }
    
    /**
     * Encodes an instruction to its hexadecimal representation
     * 
     * @param instruction the instruction to encode
     * @return a 4-character hex string (e.g., "0x0503")
     */
    public static String encodeToHexString(InstructionFormat instruction) {
        short encoded = encode(instruction);
        return String.format("0x%04X", encoded & 0xFFFF);
    }

        /**
     * Pretty-prints the binary encoding of an instruction with field labels
     * 
     * Example output for ADD R1, R2, R3:
     * "000 001 010 011 0000"
     *  opc  rA  rB  rC unused
     * 
     * @param binary the 16-bit instruction word
     * 
     * @return a formatted string showing the bit fields
     */
    public static String formatBinary(short binary) {
        int bits = binary & 0xFFFF;
        int opcode = (bits >> 13) & 0x7;
        int regA = (bits >> 10) & 0x7;
        
        try {
            Opcode op = Opcode.fromCode(opcode);
            
            switch (op.getFormat()) {
                case RRR:
                    int regB = (bits >> 7) & 0x7;
                    int regC = (bits >> 4) & 0x7;
                    return String.format("%03d %03d %03d %03d 0000",
                        Integer.parseInt(Integer.toBinaryString(opcode)),
                        Integer.parseInt(Integer.toBinaryString(regA)),
                        Integer.parseInt(Integer.toBinaryString(regB)),
                        Integer.parseInt(Integer.toBinaryString(regC)));
                        
                case RRI:
                    regB = (bits >> 7) & 0x7;
                    int imm7 = bits & 0x7F;
                    return String.format("%03d %03d %03d %07d",
                        Integer.parseInt(Integer.toBinaryString(opcode)),
                        Integer.parseInt(Integer.toBinaryString(regA)),
                        Integer.parseInt(Integer.toBinaryString(regB)),
                        Integer.parseInt(Integer.toBinaryString(imm7)));
                        
                case RI:
                    int imm10 = bits & 0x3FF;
                    return String.format("%03d %03d %010d",
                        Integer.parseInt(Integer.toBinaryString(opcode)),
                        Integer.parseInt(Integer.toBinaryString(regA)),
                        Integer.parseInt(Integer.toBinaryString(imm10)));
                        
                default:
                    return String.format("%16s", Integer.toBinaryString(bits)).replace(' ', '0');
            }
        } catch (IllegalArgumentException e) {
            // Invalid opcode, just show raw bits
            return String.format("%16s", Integer.toBinaryString(bits)).replace(' ', '0');
        }
    }
    
    /**
     * Provides a detailed breakdown of an instruction's binary encoding
     * 
     * @param binary the 16-bit instruction word
     * @return a multi-line string with detailed field information
     */
    public static String explainBinary(short binary) {
        StringBuilder sb = new StringBuilder();
        int bits = binary & 0xFFFF;
        
        sb.append(String.format("Binary: %s%n", formatBinary(binary)));
        sb.append(String.format("Hex: 0x%04X%n", bits));
        
        try {
            InstructionFormat instr = decode(binary, 0);
            sb.append(String.format("Decoded: %s%n", instr.toAssembly()));
            sb.append(String.format("Opcode: %s (%d)%n", instr.getOpcode(), instr.getOpcode().getCode()));
            sb.append(String.format("Format: %s%n", instr.getFormat()));
        } catch (IllegalArgumentException e) {
            sb.append("Invalid instruction\n");
        }
        
        return sb.toString();
    }

    /**
     * Checks if a 16-bit value represents a valid instruction
     * 
     * @param binary the 16-bit value to check
     * 
     * @return true if the instruction is valid
     */
    public static boolean isValidInstruction(short binary) {
        try {
            int opcodeValue = ((binary & 0xFFFF) >> 13) & 0x7;
            Opcode opcode = Opcode.fromCode(opcodeValue);
            // For RRR-type instructions, validate padding bits [6:3] are zero
            if (opcode.getFormat() == FormatType.RRR) {
                int paddingBits = (binary & 0x78);
                if (paddingBits != 0) {
                    return false;
                }
            }
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}