package engine.assembly.assembler;

import engine.assembly.AssemblyError;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Expands pseudo-instructions into real RiSC-16 instructions.
 * 
 * This MUST run BEFORE Pass 1 (before labels are stored) to prevent wrong
 * address pointing from expansion
 * 
 * Why? If we expand AFTER storing labels, labels will point to wrong addresses:
 * 
 * Pseudo-instructions:
 * - NOP -> ADD R0, R0, R0
 * - HALT -> JALR R0, R0
 * - LLI regA, imm -> ADDI regA, regA, (imm & 0x3F)
 * - MOVI regA, imm -> LUI regA, (imm >> 6); ADDI regA, regA, (imm & 0x3F)
 */
public final class PseudoInstructionExpander {

    // Prevent instantiation
    private PseudoInstructionExpander() {
    }

    /**
     * Map of pseudo-instructions to their expected operand counts
     */
    private static final Map<String, Integer> PSEUDO_INSTRUCTIONS = Map.of(
            "nop", 0,
            "halt", 0,
            "lli", 2,
            "movi", 2);

    /**
     * Checks if an operation is a pseudo-instruction
     * 
     * @param operation the operation mnemonic
     * 
     * @return true if it's a pseudo-instruction, false otherwise
     */
    public static boolean isPseudoInstruction(String operation) {
        return PSEUDO_INSTRUCTIONS.containsKey(operation);
    }

    /**
     * Expands all pseudo-instructions in a list of tokens.
     * 
     * This is the main entry point - call this IMMEDIATELY after preprocessing,
     * BEFORE Pass 1 begins.
     * 
     * @param tokens the preprocessed tokens
     * 
     * @return new list with all pseudo-instructions expanded
     * 
     * @throws AssemblyException on first error (fail-fast)
     */
    public static List<Token> expandAll(List<Token> tokens) {
        List<Token> expanded = new ArrayList<>();

        for (Token token : tokens) {
            if (isPseudoInstruction(token.getOperation())) {
                expanded.addAll(expandOne(token));
            } else {
                expanded.add(token);
            }
        }

        return expanded;
    }

    /**
     * Expands a single pseudo-instruction token into 1-2 real instruction tokens
     * The label (if any) is preserved on the FIRST expanded instruction only
     * Could be more elegant, but pseudo-instructions are limited in number
     * 
     * @param token the pseudo-instruction token
     * 
     * @return list of expanded instruction tokens
     */
    private static List<Token> expandOne(Token token) {
        String operation = token.getOperation();
        String[] operands = token.getOperands();
        int lineNumber = token.getLineNumber();
        String originalLine = token.getOriginalLine();

        // Validate operand count
        int expected = PSEUDO_INSTRUCTIONS.get(operation);
        if (operands.length != expected) {
            throw new AssemblyException(lineNumber,
                    operation.toUpperCase() + " requires " + expected + " operand(s), got " + operands.length,
                    originalLine, AssemblyError.ErrorType.INVALID_OPERAND);
        }

        List<Token> result = new ArrayList<>();

        switch (operation) {
            case "nop":
                // NOP -> ADD R0, R0, R0
                result.add(new Token(lineNumber, token.getLabel(), "add", new String[] { "r0", "r0", "r0" },
                        originalLine));
                break;

            case "halt":
                // HALT -> JALR R0, R0
                result.add(new Token(lineNumber, token.getLabel(), "jalr", new String[] { "r0", "r0" }, originalLine));
                break;

            case "lli":
                // LLI regA, imm -> ADDI regA, regA, (imm & 0x3F)
                Integer immLLI = NumberParser.parse(operands[1]);
                if (immLLI == null) {
                    throw new AssemblyException(lineNumber, "LLI immediate must be a number, not a label", originalLine,
                            AssemblyError.ErrorType.INVALID_OPERAND);
                }

                result.add(new Token(lineNumber, token.getLabel(), "addi",
                        new String[] { operands[0], operands[0], String.valueOf(immLLI & ImmediateRanges.LLI_MASK) },
                        originalLine));
                break;

            case "movi":
                // MOVI regA, imm -> LUI regA, (imm >> 6); ADDI regA, regA, (imm & 0x3F)
                Integer immMOVI = NumberParser.parse(operands[1]);
                if (immMOVI != null) {
                    // It's a number - expand normally
                    if (!ImmediateRanges.isValidMOVI(immMOVI)) {
                        throw new AssemblyException(lineNumber, "MOVI immediate " + immMOVI + " out of range [" +
                                ImmediateRanges.MOVI_MIN + ", " + ImmediateRanges.MOVI_MAX + "]",
                                originalLine, AssemblyError.ErrorType.INVALID_IMMEDIATE);
                    }

                    int upper10 = immMOVI >> ImmediateRanges.LUI_SHIFT;
                    int lower6 = immMOVI & ImmediateRanges.LLI_MASK;

                    // LUI (gets the label if present on original MOVI)
                    result.add(new Token(lineNumber, token.getLabel(), "lui",
                            new String[] { operands[0], String.valueOf(upper10) }, originalLine));

                    // ADDI (no label)
                    result.add(new Token(lineNumber, null, "addi",
                            new String[] { operands[0], operands[0], String.valueOf(lower6) }, originalLine));
                } else {
                    // It's a label - expand with markers
                    String labelName = operands[1];

                    // LUI with marker (gets the original label)
                    result.add(new Token(lineNumber, token.getLabel(), "lui",
                            new String[] { operands[0], "__MOVI_UPPER__" + labelName }, originalLine));

                    // ADDI with marker (no label)
                    result.add(new Token(lineNumber, null, "addi",
                            new String[] { operands[0], operands[0], "__MOVI_LOWER__" + labelName }, originalLine));
                }
                break;
            default:
                throw new AssemblyException(lineNumber, "Unknown pseudo-instruction: " + operation, originalLine,
                        AssemblyError.ErrorType.INVALID_OPCODE);
        }

        return result;
    }
}