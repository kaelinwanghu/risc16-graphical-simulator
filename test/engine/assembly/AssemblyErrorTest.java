package engine.assembly;

import static org.junit.Assert.*;
import org.junit.Test;

import engine.assembly.AssemblyError.ErrorType;

/**
 * Tests for the AssemblyError class
 *
 * AssemblyError provides rich error context including line number, column,
 * source text, and descriptive error messages for assembly failures.
 */
public class AssemblyErrorTest {

    // ========== Constructor Tests ==========

    @Test
    public void testFullConstructor() {
        AssemblyError error = new AssemblyError(5, 10, "Invalid register", "add r9, r2, r3", ErrorType.INVALID_REGISTER);

        assertEquals(5, error.getLineNumber());
        assertEquals(10, error.getColumnNumber());
        assertEquals("Invalid register", error.getMessage());
        assertEquals("add r9, r2, r3", error.getSourceText());
        assertEquals(ErrorType.INVALID_REGISTER, error.getErrorType());
    }

    @Test
    public void testConstructorWithoutColumn() {
        AssemblyError error = new AssemblyError(3, "Syntax error", "badline", ErrorType.SYNTAX_ERROR);

        assertEquals(3, error.getLineNumber());
        assertEquals(-1, error.getColumnNumber());
        assertFalse(error.hasColumnNumber());
        assertEquals("Syntax error", error.getMessage());
        assertEquals("badline", error.getSourceText());
        assertEquals(ErrorType.SYNTAX_ERROR, error.getErrorType());
    }

    @Test
    public void testHasColumnNumber() {
        AssemblyError withColumn = new AssemblyError(1, 5, "msg", "src", ErrorType.SYNTAX_ERROR);
        AssemblyError withoutColumn = new AssemblyError(1, "msg", "src", ErrorType.SYNTAX_ERROR);

        assertTrue(withColumn.hasColumnNumber());
        assertFalse(withoutColumn.hasColumnNumber());
    }

    // ========== ErrorType Tests ==========

    @Test
    public void testAllErrorTypes() {
        // Verify all error types exist and can be used
        ErrorType[] types = ErrorType.values();

        assertTrue(types.length >= 11);  // At least 11 error types defined

        // Verify specific types
        assertNotNull(ErrorType.SYNTAX_ERROR);
        assertNotNull(ErrorType.INVALID_OPCODE);
        assertNotNull(ErrorType.INVALID_OPERAND);
        assertNotNull(ErrorType.INVALID_REGISTER);
        assertNotNull(ErrorType.INVALID_IMMEDIATE);
        assertNotNull(ErrorType.UNDEFINED_LABEL);
        assertNotNull(ErrorType.DUPLICATE_LABEL);
        assertNotNull(ErrorType.LABEL_SYNTAX_ERROR);
        assertNotNull(ErrorType.OUT_OF_RANGE);
        assertNotNull(ErrorType.INVALID_DIRECTIVE);
        assertNotNull(ErrorType.MEMORY_OVERFLOW);
        assertNotNull(ErrorType.EMPTY_PROGRAM);
    }

    // ========== Formatted Message Tests ==========

    @Test
    public void testFormattedMessageWithColumn() {
        AssemblyError error = new AssemblyError(5, 5, "Invalid register name", "add r9, r2, r3", ErrorType.INVALID_REGISTER);

        String formatted = error.getFormattedMessage();

        assertTrue(formatted.contains("Error on line 5"));
        assertTrue(formatted.contains("column 5"));
        assertTrue(formatted.contains("Invalid register name"));
        assertTrue(formatted.contains("add r9, r2, r3"));
        assertTrue(formatted.contains("^"));  // Caret indicator
    }

    @Test
    public void testFormattedMessageWithoutColumn() {
        AssemblyError error = new AssemblyError(3, "Unknown instruction", "xyz r1, r2", ErrorType.INVALID_OPCODE);

        String formatted = error.getFormattedMessage();

        assertTrue(formatted.contains("Error on line 3"));
        assertFalse(formatted.contains("column"));
        assertTrue(formatted.contains("Unknown instruction"));
        assertTrue(formatted.contains("xyz r1, r2"));
        assertFalse(formatted.contains("^"));  // No caret without column
    }

    @Test
    public void testFormattedMessageEmptySource() {
        AssemblyError error = new AssemblyError(1, "Program contains no instructions", "", ErrorType.EMPTY_PROGRAM);

        String formatted = error.getFormattedMessage();

        assertTrue(formatted.contains("Error on line 1"));
        assertTrue(formatted.contains("Program contains no instructions"));
    }

    @Test
    public void testFormattedMessageNullSource() {
        AssemblyError error = new AssemblyError(1, 1, "Some error", null, ErrorType.SYNTAX_ERROR);

        String formatted = error.getFormattedMessage();

        assertTrue(formatted.contains("Error on line 1"));
        assertTrue(formatted.contains("Some error"));
    }

    // ========== Compact Message Tests ==========

    @Test
    public void testCompactMessage() {
        AssemblyError error = new AssemblyError(7, 3, "Immediate out of range", "addi r1, r2, 1000", ErrorType.INVALID_IMMEDIATE);

        String compact = error.getCompactMessage();

        assertEquals("Line 7: Immediate out of range", compact);
    }

    @Test
    public void testCompactMessageIgnoresColumn() {
        AssemblyError error = new AssemblyError(10, 5, "Test message", "source", ErrorType.SYNTAX_ERROR);

        String compact = error.getCompactMessage();

        assertFalse(compact.contains("column"));
        assertEquals("Line 10: Test message", compact);
    }

    // ========== toString Tests ==========

    @Test
    public void testToStringReturnsFormattedMessage() {
        AssemblyError error = new AssemblyError(2, "Test error", "test source", ErrorType.SYNTAX_ERROR);

        assertEquals(error.getFormattedMessage(), error.toString());
    }

    // ========== Factory Method Tests ==========

    @Test
    public void testSyntaxErrorFactory() {
        AssemblyError error = AssemblyError.syntaxError(5, "Unexpected token", "add r1, ,");

        assertEquals(5, error.getLineNumber());
        assertEquals("Unexpected token", error.getMessage());
        assertEquals("add r1, ,", error.getSourceText());
        assertEquals(ErrorType.SYNTAX_ERROR, error.getErrorType());
    }

    @Test
    public void testInvalidOpcodeFactory() {
        AssemblyError error = AssemblyError.invalidOpcode(3, "xyz", "xyz r1, r2, r3");

        assertEquals(3, error.getLineNumber());
        assertTrue(error.getMessage().contains("xyz"));
        assertTrue(error.getMessage().contains("Invalid opcode"));
        assertEquals(ErrorType.INVALID_OPCODE, error.getErrorType());
    }

    @Test
    public void testInvalidRegisterFactory() {
        AssemblyError error = AssemblyError.invalidRegister(4, "r9", "add r9, r2, r3");

        assertEquals(4, error.getLineNumber());
        assertTrue(error.getMessage().contains("r9"));
        assertTrue(error.getMessage().contains("r0-r7"));
        assertEquals(ErrorType.INVALID_REGISTER, error.getErrorType());
    }

    @Test
    public void testInvalidImmediateFactory() {
        AssemblyError error = AssemblyError.invalidImmediate(6, "1000", -32, 31, "addi r1, r2, 1000");

        assertEquals(6, error.getLineNumber());
        assertTrue(error.getMessage().contains("1000"));
        assertTrue(error.getMessage().contains("-32"));
        assertTrue(error.getMessage().contains("31"));
        assertEquals(ErrorType.INVALID_IMMEDIATE, error.getErrorType());
    }

    @Test
    public void testUndefinedLabelFactory() {
        AssemblyError error = AssemblyError.undefinedLabel(8, "missing", "beq r0, r0, missing");

        assertEquals(8, error.getLineNumber());
        assertTrue(error.getMessage().contains("missing"));
        assertTrue(error.getMessage().contains("Undefined"));
        assertEquals(ErrorType.UNDEFINED_LABEL, error.getErrorType());
    }

    @Test
    public void testDuplicateLabelFactory() {
        AssemblyError error = AssemblyError.duplicateLabel(10, "loop", 3, "loop: add r1, r2, r3");

        assertEquals(10, error.getLineNumber());
        assertTrue(error.getMessage().contains("loop"));
        assertTrue(error.getMessage().contains("line 3"));
        assertEquals(ErrorType.DUPLICATE_LABEL, error.getErrorType());
    }

    @Test
    public void testWrongOperandCountFactory() {
        AssemblyError error = AssemblyError.wrongOperandCount(2, "add", 3, 2, "add r1, r2");

        assertEquals(2, error.getLineNumber());
        assertTrue(error.getMessage().contains("add"));
        assertTrue(error.getMessage().contains("3"));
        assertTrue(error.getMessage().contains("2"));
        assertEquals(ErrorType.INVALID_OPERAND, error.getErrorType());
    }

    @Test
    public void testBranchOutOfRangeFactory() {
        AssemblyError error = AssemblyError.branchOutOfRange(15, "farLabel", 100, "beq r0, r0, farLabel");

        assertEquals(15, error.getLineNumber());
        assertTrue(error.getMessage().contains("farLabel"));
        assertTrue(error.getMessage().contains("100"));
        assertTrue(error.getMessage().contains("±63"));
        assertEquals(ErrorType.OUT_OF_RANGE, error.getErrorType());
    }

    @Test
    public void testEmptyProgramFactory() {
        AssemblyError error = AssemblyError.emptyProgram();

        assertEquals(1, error.getLineNumber());
        assertTrue(error.getMessage().contains("no instructions"));
        assertEquals(ErrorType.EMPTY_PROGRAM, error.getErrorType());
    }

    // ========== Edge Cases ==========

    @Test
    public void testColumnNumberAtStart() {
        AssemblyError error = new AssemblyError(1, 1, "Error at start", "x", ErrorType.SYNTAX_ERROR);

        String formatted = error.getFormattedMessage();
        assertTrue(formatted.contains("column 1"));
        // Caret should be at position 1 (right at the start after indent)
        assertTrue(formatted.contains("^"));
    }

    @Test
    public void testLineNumberOne() {
        AssemblyError error = new AssemblyError(1, "First line error", "source", ErrorType.SYNTAX_ERROR);

        assertEquals(1, error.getLineNumber());
        assertTrue(error.getFormattedMessage().contains("line 1"));
    }

    @Test
    public void testWhitespaceOnlySource() {
        AssemblyError error = new AssemblyError(1, "Whitespace error", "   ", ErrorType.SYNTAX_ERROR);

        String formatted = error.getFormattedMessage();
        // Source is whitespace only, should not show it
        assertTrue(formatted.contains("Error on line 1"));
    }
}
