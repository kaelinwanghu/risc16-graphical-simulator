package engine.assembly.assembler;

import static org.junit.Assert.*;

import engine.assembly.AssemblyError;
import engine.assembly.AssemblyResult;
import engine.isa.InstructionFormat;
import engine.isa.Opcode;

/**
 * Utility methods for assembler tests
 */
public class AssemblerTestUtils {

    /**
     * Assemble source and assert success
     */
    public static AssemblyResult assembleSuccess(String source) {
        AssemblyResult result = Assembler.assemble(source);
        assertTrue("Assembly should succeed. Errors: " + formatErrors(result), result.isSuccess());
        return result;
    }

    /**
     * Assemble source and assert failure
     */
    public static AssemblyResult assembleFailure(String source) {
        AssemblyResult result = Assembler.assemble(source);
        assertFalse("Assembly should fail", result.isSuccess());
        return result;
    }

    /**
     * Assemble and assert specific error type
     */
    public static AssemblyResult assembleExpectError(String source, AssemblyError.ErrorType expectedType) {
        AssemblyResult result = Assembler.assemble(source);
        assertFalse("Assembly should fail", result.isSuccess());
        assertFalse("Should have at least one error", result.getErrors().isEmpty());

        boolean foundExpectedError = result.getErrors().stream()
            .anyMatch(e -> e.getErrorType() == expectedType);
        assertTrue("Expected error type " + expectedType + " but got: " +
            result.getErrors().get(0).getErrorType(), foundExpectedError);

        return result;
    }

    /**
     * Assert instruction has expected opcode and registers (RRR format)
     */
    public static void assertRRR(InstructionFormat instr, Opcode opcode, int regA, int regB, int regC) {
        assertEquals("Opcode mismatch", opcode, instr.getOpcode());
        assertEquals("RegA mismatch", regA, instr.getRegA());
        assertEquals("RegB mismatch", regB, instr.getRegB());
        assertEquals("RegC mismatch", regC, instr.getRegC());
    }

    /**
     * Assert instruction has expected opcode, registers and immediate (RRI format)
     */
    public static void assertRRI(InstructionFormat instr, Opcode opcode, int regA, int regB, int imm) {
        assertEquals("Opcode mismatch", opcode, instr.getOpcode());
        assertEquals("RegA mismatch", regA, instr.getRegA());
        assertEquals("RegB mismatch", regB, instr.getRegB());
        assertEquals("Immediate mismatch", imm, instr.getImmediate());
    }

    /**
     * Assert instruction has expected opcode, register and immediate (RI format)
     */
    public static void assertRI(InstructionFormat instr, Opcode opcode, int regA, int imm) {
        assertEquals("Opcode mismatch", opcode, instr.getOpcode());
        assertEquals("RegA mismatch", regA, instr.getRegA());
        assertEquals("Immediate mismatch", imm, instr.getImmediate());
    }

    /**
     * Format errors for display in assertion messages
     */
    public static String formatErrors(AssemblyResult result) {
        if (result.isSuccess()) {
            return "(no errors)";
        }
        StringBuilder sb = new StringBuilder();
        for (AssemblyError error : result.getErrors()) {
            sb.append("\n  - ").append(error.getCompactMessage());
        }
        return sb.toString();
    }

    /**
     * Assert that result contains a label at the expected address
     */
    public static void assertLabel(AssemblyResult result, String label, int expectedAddress) {
        assertTrue("Label '" + label + "' should be defined",
            result.getSymbolTable().contains(label));
        int address = result.getSymbolTable().resolve(label);
        assertEquals("Label '" + label + "' address mismatch", expectedAddress, address);
    }

    /**
     * Assert the total number of instructions
     */
    public static void assertInstructionCount(AssemblyResult result, int expected) {
        assertEquals("Instruction count mismatch", expected, result.getInstructionCount());
    }

    /**
     * Assert the total number of data segments
     */
    public static void assertDataCount(AssemblyResult result, int expected) {
        assertEquals("Data segment count mismatch", expected, result.getDataCount());
    }
}
