package engine.assembly.assembler;

import engine.assembly.AssemblyError;
import engine.isa.InstructionFormat;
import engine.isa.Opcode;
import engine.isa.FormatType;

/**
 * Parses only actual RiSC-16 instructions (not pseudo-instructions or
 * directives)
 * 
 * Handles:
 * - Register parsing (r0-r7)
 * - Immediate parsing (numbers vs. labels)
 * - Operand count validation
 * - Immediate range validation
 * - Creating InstructionFormat objects
 * 
 * For symbolic labels (in branch/load/store), creates unresolved references
 * (resolved in pass 2)
 */
public final class InstructionParser {

    // Prevent instantiation
    private InstructionParser() {
    }

    /**
     * Parses a real instruction token.
     * 
     * @param token   the token to parse
     * @param context the assembly context
     * 
     * @throws AssemblyException on first error (fail-fast)
     */
    public static void parseInstruction(Token token, AssemblyContext context) {
        String operation = token.getOperation();
        Opcode opcode = Opcode.fromMnemonic(operation);

        if (opcode == null) {
            throw new AssemblyException(token.getLineNumber(), "Invalid operation: '" + operation + "'",
                    token.getOriginalLine(), AssemblyError.ErrorType.INVALID_OPCODE);
        }

        String[] operands = token.getOperands();
        FormatType format = opcode.getFormat();

        // Parse based on format
        switch (format) {
            case RRR:
                parseRRR(opcode, operands, token, context);
                break;
            case RRI:
                parseRRI(opcode, operands, token, context);
                break;
            case RI:
                parseRI(opcode, operands, token, context);
                break;
        }
    }

    /**
     * Parses RRR-type instruction (ADD, NAND)
     */
    private static void parseRRR(Opcode opcode, String[] operands, Token token, AssemblyContext context) {
        if (operands.length != 3) {
            throw new AssemblyException(token.getLineNumber(), opcode + " requires 3 operands, got " + operands.length,
                    token.getOriginalLine(), AssemblyError.ErrorType.INVALID_OPERAND);
        }

        int regA = parseRegister(operands[0], token);
        int regB = parseRegister(operands[1], token);
        int regC = parseRegister(operands[2], token);

        InstructionFormat instruction = InstructionFormat.createRRR(opcode, regA, regB, regC,
                context.getCurrentAddress());

        context.addInstruction(instruction);
    }

    /**
     * Parses RRI-type instruction (ADDI, LW, SW, BEQ, JALR)
     */
    private static void parseRRI(Opcode opcode, String[] operands, Token token, AssemblyContext context) {
        int expectedOperands = (opcode == Opcode.JALR) ? 2 : 3;

        if (operands.length != expectedOperands) {
            throw new AssemblyException(token.getLineNumber(),
                    opcode + " requires " + expectedOperands + " operands, got " + operands.length,
                    token.getOriginalLine(), AssemblyError.ErrorType.INVALID_OPERAND);
        }

        int regA = parseRegister(operands[0], token);
        int regB = parseRegister(operands[1], token);

        // JALR has no immediate
        if (opcode == Opcode.JALR) {
            InstructionFormat instruction = InstructionFormat.createRRI(opcode, regA, regB, 0,
                    context.getCurrentAddress());
            context.addInstruction(instruction);
            return;
        }

        // Parse third operand (immediate or label)
        String immStr = operands[2];
        if (immStr.startsWith("__MOVI_LOWER__")) {
            String label = immStr.substring("__MOVI_LOWER__".length());

            // Create placeholder instruction
            InstructionFormat instruction = InstructionFormat.createRRI(opcode, regA, regB, 0,
                    context.getCurrentAddress());
            int instructionIndex = context.getInstructions().size();
            context.addInstruction(instruction);
            
            // Add unresolved reference using existing instruction() method
            context.addUnresolvedReference(
                    UnresolvedReference.instruction(
                            UnresolvedReference.Type.MOVI_LOWER,
                            label,
                            instructionIndex,
                            context.getCurrentAddress() - 2, // Current address after increment
                            token.getLineNumber(),
                            token.getOriginalLine()));
            return;
        }
        Integer immediate = NumberParser.parse(immStr);

        if (immediate != null) {
            // It's a number - validate and use
            if (!ImmediateRanges.isValidRRI(immediate)) {
                throw new AssemblyException(token.getLineNumber(),
                        "Immediate " + immediate + " out of range [" + ImmediateRanges.RRI_MIN + ", "
                                + ImmediateRanges.RRI_MAX + "]",
                        token.getOriginalLine(), AssemblyError.ErrorType.INVALID_IMMEDIATE);
            }

            InstructionFormat instruction = InstructionFormat.createRRI(opcode, regA, regB, immediate,
                    context.getCurrentAddress());
            context.addInstruction(instruction);
        } else {
            // It's a label - create placeholder and unresolved reference
            InstructionFormat instruction = InstructionFormat.createRRI(opcode, regA, regB, 0,
                    context.getCurrentAddress()); // Placeholder immediate

            int instructionIndex = context.getInstructions().size();
            context.addInstruction(instruction);

            // Determine reference type
            UnresolvedReference.Type refType = (opcode == Opcode.BEQ) ? UnresolvedReference.Type.BRANCH
                    : UnresolvedReference.Type.LOAD_STORE;

            context.addUnresolvedReference(UnresolvedReference.instruction(refType, immStr, instructionIndex,
                    context.getCurrentAddress() - 2, token.getLineNumber(), token.getOriginalLine()));
        }
    }

    /**
     * Parses RI-type instruction (LUI)
     */
    private static void parseRI(Opcode opcode, String[] operands, Token token, AssemblyContext context) {
        if (operands.length != 2) {
            throw new AssemblyException(token.getLineNumber(), opcode + " requires 2 operands, got " + operands.length,
                    token.getOriginalLine(), AssemblyError.ErrorType.INVALID_OPERAND);
        }

        int regA = parseRegister(operands[0], token);
        String immStr = operands[1];

        if (immStr.startsWith("__MOVI_UPPER__")) {
            String label = immStr.substring("__MOVI_UPPER__".length());

            // Create placeholder instruction
            InstructionFormat instruction = InstructionFormat.createRI(opcode, regA, 0, context.getCurrentAddress());
            int instructionIndex = context.getInstructions().size();
            context.addInstruction(instruction);

            // Add unresolved reference using existing instruction() method
            context.addUnresolvedReference(
                    UnresolvedReference.instruction(
                            UnresolvedReference.Type.MOVI_UPPER,
                            label,
                            instructionIndex,
                            context.getCurrentAddress() - 2, // Current address after increment
                            token.getLineNumber(),
                            token.getOriginalLine()));
            return;
        }

        // Parse immediate (must be a number for LUI, not a label)
        Integer immediate = NumberParser.parse(operands[1]);
        if (immediate == null) {
            throw new AssemblyException(token.getLineNumber(), "LUI immediate must be a number, not a label",
                    token.getOriginalLine(), AssemblyError.ErrorType.INVALID_OPERAND);
        }

        if (!ImmediateRanges.isValidRI(immediate)) {
            throw new AssemblyException(token.getLineNumber(),
                    "LUI immediate " + immediate + " out of range [" + ImmediateRanges.RI_MIN + ", "
                            + ImmediateRanges.RI_MAX + "]",
                    token.getOriginalLine(), AssemblyError.ErrorType.INVALID_IMMEDIATE);
        }

        InstructionFormat instruction = InstructionFormat.createRI(opcode, regA, immediate,
                context.getCurrentAddress());

        context.addInstruction(instruction);
    }

    /**
     * Parses a register operand (r0-r7)
     * 
     * @return the register number (0-7)
     * @throws AssemblyException if invalid register
     */
    private static int parseRegister(String operand, Token token) {
        operand = operand.toLowerCase();

        if (!operand.startsWith("r")) {
            throw new AssemblyException(token.getLineNumber(), "Expected register (r0-r7), got: " + operand,
                    token.getOriginalLine(), AssemblyError.ErrorType.INVALID_REGISTER);
        }

        try {
            int regNum = Integer.parseInt(operand.substring(1));
            if (regNum < 0 || regNum > 7) {
                throw new AssemblyException(token.getLineNumber(), "Register number must be 0-7, got: " + regNum,
                        token.getOriginalLine(), AssemblyError.ErrorType.INVALID_REGISTER);
            }
            return regNum;
        } catch (NumberFormatException e) {
            throw new AssemblyException(token.getLineNumber(), "Invalid register: " + operand, token.getOriginalLine(),
                    AssemblyError.ErrorType.INVALID_REGISTER);
        }
    }
}