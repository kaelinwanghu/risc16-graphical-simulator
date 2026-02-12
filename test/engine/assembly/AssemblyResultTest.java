package engine.assembly;

import static org.junit.Assert.*;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import engine.isa.InstructionFormat;
import engine.isa.Opcode;
import engine.assembly.AssemblyResult.DataSegment;
import engine.assembly.AssemblyResult.AssemblyWarning;
import engine.assembly.AssemblyError.ErrorType;

/**
 * Tests for the AssemblyResult class
 *
 * AssemblyResult contains the result of assembling a program:
 * - Assembled instructions
 * - Data segments
 * - Symbol table
 * - Errors and warnings
 */
public class AssemblyResultTest {

    // ========== DataSegment Tests ==========

    @Test
    public void testDataSegmentCreation() {
        DataSegment segment = new DataSegment(100, (short) 42);

        assertEquals(100, segment.getAddress());
        assertEquals(42, segment.getValue());
    }

    @Test
    public void testDataSegmentNegativeValue() {
        DataSegment segment = new DataSegment(0, (short) -1);

        assertEquals(0, segment.getAddress());
        assertEquals(-1, segment.getValue());
    }

    @Test
    public void testDataSegmentToString() {
        DataSegment segment = new DataSegment(0x100, (short) 0x1234);

        String str = segment.toString();
        assertTrue(str.contains("0100"));  // Address in hex
        assertTrue(str.contains("1234"));  // Value in hex
    }

    @Test
    public void testDataSegmentToStringNegative() {
        DataSegment segment = new DataSegment(0, (short) -1);

        String str = segment.toString();
        assertTrue(str.contains("FFFF"));  // -1 as unsigned hex
    }

    // ========== AssemblyWarning Tests ==========

    @Test
    public void testAssemblyWarningCreation() {
        AssemblyWarning warning = new AssemblyWarning(5, "Unused label");

        assertEquals(5, warning.getLineNumber());
        assertEquals("Unused label", warning.getMessage());
    }

    @Test
    public void testAssemblyWarningToString() {
        AssemblyWarning warning = new AssemblyWarning(10, "Potential issue");

        String str = warning.toString();
        assertTrue(str.contains("Warning"));
        assertTrue(str.contains("line 10"));
        assertTrue(str.contains("Potential issue"));
    }

    // ========== Builder Basic Tests ==========

    @Test
    public void testBuilderEmpty() {
        AssemblyResult result = AssemblyResult.builder().build();

        assertTrue(result.isSuccess());
        assertTrue(result.getInstructions().isEmpty());
        assertTrue(result.getDataSegments().isEmpty());
        assertTrue(result.getErrors().isEmpty());
        assertTrue(result.getWarnings().isEmpty());
        assertNotNull(result.getSymbolTable());
    }

    @Test
    public void testBuilderWithInstruction() {
        InstructionFormat instr = InstructionFormat.createRRR(Opcode.ADD, 1, 2, 3, 0);

        AssemblyResult result = AssemblyResult.builder()
            .addInstruction(instr)
            .build();

        assertTrue(result.isSuccess());
        assertEquals(1, result.getInstructionCount());
        assertEquals(Opcode.ADD, result.getInstructions().get(0).getOpcode());
    }

    @Test
    public void testBuilderWithMultipleInstructions() {
        InstructionFormat add = InstructionFormat.createRRR(Opcode.ADD, 1, 2, 3, 0);
        InstructionFormat nand = InstructionFormat.createRRR(Opcode.NAND, 4, 5, 6, 2);

        AssemblyResult result = AssemblyResult.builder()
            .addInstruction(add)
            .addInstruction(nand)
            .build();

        assertEquals(2, result.getInstructionCount());
    }

    @Test
    public void testBuilderAddInstructionsList() {
        InstructionFormat add = InstructionFormat.createRRR(Opcode.ADD, 1, 2, 3, 0);
        InstructionFormat nand = InstructionFormat.createRRR(Opcode.NAND, 4, 5, 6, 2);

        List<InstructionFormat> instructions = Arrays.asList(add, nand);

        AssemblyResult result = AssemblyResult.builder()
            .addInstructions(instructions)
            .build();

        assertEquals(2, result.getInstructionCount());
    }

    @Test
    public void testBuilderWithDataSegment() {
        AssemblyResult result = AssemblyResult.builder()
            .addDataSegment(100, (short) 42)
            .build();

        assertEquals(1, result.getDataCount());
        assertEquals(100, result.getDataSegments().get(0).getAddress());
        assertEquals(42, result.getDataSegments().get(0).getValue());
    }

    @Test
    public void testBuilderWithDataSegmentsList() {
        List<DataSegment> segments = Arrays.asList(
            new DataSegment(0, (short) 1),
            new DataSegment(2, (short) 2),
            new DataSegment(4, (short) 3)
        );

        AssemblyResult result = AssemblyResult.builder()
            .addDataSegments(segments)
            .build();

        assertEquals(3, result.getDataCount());
    }

    @Test
    public void testBuilderWithSymbolTable() {
        SymbolTable st = new SymbolTable();
        st.define("main", 0);
        st.define("end", 100);

        AssemblyResult result = AssemblyResult.builder()
            .symbolTable(st)
            .build();

        assertEquals(2, result.getSymbolTable().size());
        assertTrue(result.getSymbolTable().contains("main"));
        assertTrue(result.getSymbolTable().contains("end"));
    }

    // ========== Error Tests ==========

    @Test
    public void testBuilderWithError() {
        AssemblyError error = new AssemblyError(1, "Test error", "source", ErrorType.SYNTAX_ERROR);

        AssemblyResult result = AssemblyResult.builder()
            .addError(error)
            .build();

        assertFalse(result.isSuccess());
        assertEquals(1, result.getErrors().size());
        assertEquals("Test error", result.getErrors().get(0).getMessage());
    }

    @Test
    public void testBuilderWithMultipleErrors() {
        AssemblyError error1 = new AssemblyError(1, "Error 1", "src", ErrorType.SYNTAX_ERROR);
        AssemblyError error2 = new AssemblyError(2, "Error 2", "src", ErrorType.INVALID_REGISTER);

        AssemblyResult result = AssemblyResult.builder()
            .addError(error1)
            .addError(error2)
            .build();

        assertFalse(result.isSuccess());
        assertEquals(2, result.getErrors().size());
    }

    @Test
    public void testBuilderAddErrorsList() {
        List<AssemblyError> errors = Arrays.asList(
            new AssemblyError(1, "Error 1", "src", ErrorType.SYNTAX_ERROR),
            new AssemblyError(2, "Error 2", "src", ErrorType.INVALID_REGISTER)
        );

        AssemblyResult result = AssemblyResult.builder()
            .addErrors(errors)
            .build();

        assertEquals(2, result.getErrors().size());
    }

    // ========== Warning Tests ==========

    @Test
    public void testBuilderWithWarning() {
        AssemblyResult result = AssemblyResult.builder()
            .addWarning(5, "Potential issue")
            .build();

        assertTrue(result.isSuccess());  // Warnings don't cause failure
        assertTrue(result.hasWarnings());
        assertEquals(1, result.getWarnings().size());
        assertEquals("Potential issue", result.getWarnings().get(0).getMessage());
    }

    @Test
    public void testBuilderWithMultipleWarnings() {
        AssemblyResult result = AssemblyResult.builder()
            .addWarning(1, "Warning 1")
            .addWarning(2, "Warning 2")
            .addWarning(3, "Warning 3")
            .build();

        assertTrue(result.hasWarnings());
        assertEquals(3, result.getWarnings().size());
    }

    @Test
    public void testBuilderAddWarningsList() {
        List<AssemblyWarning> warnings = Arrays.asList(
            new AssemblyWarning(1, "W1"),
            new AssemblyWarning(2, "W2")
        );

        AssemblyResult result = AssemblyResult.builder()
            .addWarnings(warnings)
            .build();

        assertEquals(2, result.getWarnings().size());
    }

    @Test
    public void testHasWarningsFalse() {
        AssemblyResult result = AssemblyResult.builder().build();
        assertFalse(result.hasWarnings());
    }

    // ========== isSuccess Tests ==========

    @Test
    public void testIsSuccessTrue() {
        InstructionFormat instr = InstructionFormat.createRRR(Opcode.ADD, 1, 2, 3, 0);

        AssemblyResult result = AssemblyResult.builder()
            .addInstruction(instr)
            .build();

        assertTrue(result.isSuccess());
    }

    @Test
    public void testIsSuccessFalse() {
        AssemblyError error = new AssemblyError(1, "Error", "src", ErrorType.SYNTAX_ERROR);

        AssemblyResult result = AssemblyResult.builder()
            .addError(error)
            .build();

        assertFalse(result.isSuccess());
    }

    @Test
    public void testIsSuccessWithWarningsButNoErrors() {
        AssemblyResult result = AssemblyResult.builder()
            .addWarning(1, "Just a warning")
            .build();

        assertTrue(result.isSuccess());  // Warnings don't fail assembly
    }

    // ========== getInstructionAt Tests ==========

    @Test
    public void testGetInstructionAt() {
        InstructionFormat instr0 = InstructionFormat.createRRR(Opcode.ADD, 1, 2, 3, 0);
        InstructionFormat instr2 = InstructionFormat.createRRR(Opcode.NAND, 4, 5, 6, 2);

        AssemblyResult result = AssemblyResult.builder()
            .addInstruction(instr0)
            .addInstruction(instr2)
            .build();

        assertEquals(Opcode.ADD, result.getInstructionAt(0).getOpcode());
        assertEquals(Opcode.NAND, result.getInstructionAt(2).getOpcode());
    }

    @Test
    public void testGetInstructionAtReturnsNull() {
        InstructionFormat instr = InstructionFormat.createRRR(Opcode.ADD, 1, 2, 3, 0);

        AssemblyResult result = AssemblyResult.builder()
            .addInstruction(instr)
            .build();

        assertNull(result.getInstructionAt(100));  // No instruction at address 100
    }

    // ========== Immutability Tests ==========

    @Test
    public void testInstructionsImmutable() {
        InstructionFormat instr = InstructionFormat.createRRR(Opcode.ADD, 1, 2, 3, 0);

        AssemblyResult result = AssemblyResult.builder()
            .addInstruction(instr)
            .build();

        try {
            result.getInstructions().clear();
            fail("Should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // Expected
        }
    }

    @Test
    public void testDataSegmentsImmutable() {
        AssemblyResult result = AssemblyResult.builder()
            .addDataSegment(0, (short) 42)
            .build();

        try {
            result.getDataSegments().clear();
            fail("Should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // Expected
        }
    }

    @Test
    public void testErrorsImmutable() {
        AssemblyError error = new AssemblyError(1, "Error", "src", ErrorType.SYNTAX_ERROR);

        AssemblyResult result = AssemblyResult.builder()
            .addError(error)
            .build();

        try {
            result.getErrors().clear();
            fail("Should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // Expected
        }
    }

    @Test
    public void testWarningsImmutable() {
        AssemblyResult result = AssemblyResult.builder()
            .addWarning(1, "Warning")
            .build();

        try {
            result.getWarnings().clear();
            fail("Should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // Expected
        }
    }

    // ========== Report Tests ==========

    @Test
    public void testErrorReportNoErrors() {
        AssemblyResult result = AssemblyResult.builder().build();

        String report = result.getErrorReport();
        assertTrue(report.contains("No errors"));
    }

    @Test
    public void testErrorReportWithErrors() {
        AssemblyError error1 = new AssemblyError(1, "First error", "line1", ErrorType.SYNTAX_ERROR);
        AssemblyError error2 = new AssemblyError(2, "Second error", "line2", ErrorType.INVALID_REGISTER);

        AssemblyResult result = AssemblyResult.builder()
            .addError(error1)
            .addError(error2)
            .build();

        String report = result.getErrorReport();
        assertTrue(report.contains("2 error(s)"));
        assertTrue(report.contains("First error"));
        assertTrue(report.contains("Second error"));
    }

    @Test
    public void testWarningReportNoWarnings() {
        AssemblyResult result = AssemblyResult.builder().build();

        String report = result.getWarningReport();
        assertTrue(report.contains("No warnings"));
    }

    @Test
    public void testWarningReportWithWarnings() {
        AssemblyResult result = AssemblyResult.builder()
            .addWarning(1, "First warning")
            .addWarning(2, "Second warning")
            .build();

        String report = result.getWarningReport();
        assertTrue(report.contains("2 warning(s)"));
        assertTrue(report.contains("First warning"));
        assertTrue(report.contains("Second warning"));
    }

    // ========== Factory Method Tests ==========

    @Test
    public void testErrorFactory() {
        AssemblyError error = new AssemblyError(1, "Single error", "src", ErrorType.SYNTAX_ERROR);

        AssemblyResult result = AssemblyResult.error(error);

        assertFalse(result.isSuccess());
        assertEquals(1, result.getErrors().size());
        assertEquals("Single error", result.getErrors().get(0).getMessage());
    }

    @Test
    public void testErrorsFactory() {
        List<AssemblyError> errors = Arrays.asList(
            new AssemblyError(1, "Error 1", "src", ErrorType.SYNTAX_ERROR),
            new AssemblyError(2, "Error 2", "src", ErrorType.INVALID_REGISTER),
            new AssemblyError(3, "Error 3", "src", ErrorType.UNDEFINED_LABEL)
        );

        AssemblyResult result = AssemblyResult.errors(errors);

        assertFalse(result.isSuccess());
        assertEquals(3, result.getErrors().size());
    }

    // ========== toString Tests ==========

    @Test
    public void testToStringSuccess() {
        InstructionFormat instr = InstructionFormat.createRRR(Opcode.ADD, 1, 2, 3, 0);

        AssemblyResult result = AssemblyResult.builder()
            .addInstruction(instr)
            .addDataSegment(100, (short) 42)
            .build();

        String str = result.toString();
        assertTrue(str.contains("SUCCESS"));
        assertTrue(str.contains("1 instructions"));
        assertTrue(str.contains("1 data"));
    }

    @Test
    public void testToStringFailure() {
        AssemblyError error = new AssemblyError(1, "Error", "src", ErrorType.SYNTAX_ERROR);

        AssemblyResult result = AssemblyResult.builder()
            .addError(error)
            .build();

        String str = result.toString();
        assertTrue(str.contains("FAILURE"));
        assertTrue(str.contains("1 errors"));
    }

    // ========== Count Tests ==========

    @Test
    public void testInstructionCount() {
        InstructionFormat i1 = InstructionFormat.createRRR(Opcode.ADD, 1, 2, 3, 0);
        InstructionFormat i2 = InstructionFormat.createRRR(Opcode.NAND, 1, 2, 3, 2);
        InstructionFormat i3 = InstructionFormat.createRI(Opcode.LUI, 1, 100, 4);

        AssemblyResult result = AssemblyResult.builder()
            .addInstruction(i1)
            .addInstruction(i2)
            .addInstruction(i3)
            .build();

        assertEquals(3, result.getInstructionCount());
    }

    @Test
    public void testDataCount() {
        AssemblyResult result = AssemblyResult.builder()
            .addDataSegment(0, (short) 1)
            .addDataSegment(2, (short) 2)
            .build();

        assertEquals(2, result.getDataCount());
    }
}
