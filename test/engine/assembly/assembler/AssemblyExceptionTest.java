package engine.assembly.assembler;

import static org.junit.Assert.*;
import org.junit.Test;

import engine.assembly.AssemblyError;
import engine.assembly.AssemblyError.ErrorType;

/**
 * Tests for the AssemblyException class
 *
 * AssemblyException is a RuntimeException that wraps AssemblyError for
 * fail-fast error handling during assembly.
 */
public class AssemblyExceptionTest {

    // ========== Constructor with AssemblyError Tests ==========

    @Test
    public void testConstructorWithError() {
        AssemblyError error = new AssemblyError(5, "Test error", "test source", ErrorType.SYNTAX_ERROR);
        AssemblyException exception = new AssemblyException(error);

        assertEquals(error, exception.getError());
        assertEquals("Test error", exception.getMessage());
    }

    @Test
    public void testConstructorPreservesErrorDetails() {
        AssemblyError error = new AssemblyError(10, 5, "Column error", "source", ErrorType.INVALID_REGISTER);
        AssemblyException exception = new AssemblyException(error);

        AssemblyError retrieved = exception.getError();
        assertEquals(10, retrieved.getLineNumber());
        assertEquals(5, retrieved.getColumnNumber());
        assertEquals("Column error", retrieved.getMessage());
        assertEquals(ErrorType.INVALID_REGISTER, retrieved.getErrorType());
    }

    // ========== Constructor with Parameters Tests ==========

    @Test
    public void testConstructorWithParameters() {
        AssemblyException exception = new AssemblyException(
            3, "Invalid opcode", "xyz r1, r2", ErrorType.INVALID_OPCODE);

        AssemblyError error = exception.getError();
        assertEquals(3, error.getLineNumber());
        assertEquals("Invalid opcode", error.getMessage());
        assertEquals("xyz r1, r2", error.getSourceText());
        assertEquals(ErrorType.INVALID_OPCODE, error.getErrorType());
    }

    @Test
    public void testConstructorMessagePropagated() {
        AssemblyException exception = new AssemblyException(
            1, "My error message", "source", ErrorType.SYNTAX_ERROR);

        assertEquals("My error message", exception.getMessage());
    }

    // ========== Exception Behavior Tests ==========

    @Test
    public void testIsRuntimeException() {
        AssemblyException exception = new AssemblyException(
            1, "Error", "source", ErrorType.SYNTAX_ERROR);

        assertTrue(exception instanceof RuntimeException);
    }

    @Test
    public void testCanBeCaught() {
        boolean caught = false;
        try {
            throw new AssemblyException(1, "Test", "src", ErrorType.SYNTAX_ERROR);
        } catch (AssemblyException e) {
            assertEquals("Test", e.getMessage());
            caught = true;
        }
        assertTrue("Exception should have been caught", caught);
    }

    @Test
    public void testCanBeCaughtAsRuntimeException() {
        boolean caught = false;
        try {
            throw new AssemblyException(1, "Test", "src", ErrorType.SYNTAX_ERROR);
        } catch (RuntimeException e) {
            assertTrue(e instanceof AssemblyException);
            caught = true;
        }
        assertTrue("Exception should have been caught", caught);
    }

    // ========== Error Type Preservation Tests ==========

    @Test
    public void testAllErrorTypesPreserved() {
        for (ErrorType type : ErrorType.values()) {
            AssemblyException exception = new AssemblyException(
                1, "Error for " + type.name(), "source", type);

            assertEquals(type, exception.getError().getErrorType());
        }
    }

    // ========== getError Tests ==========

    @Test
    public void testGetErrorNotNull() {
        AssemblyException exception = new AssemblyException(
            1, "Error", "source", ErrorType.SYNTAX_ERROR);

        assertNotNull(exception.getError());
    }

    @Test
    public void testGetErrorReturnsOriginal() {
        AssemblyError originalError = new AssemblyError(1, "Error", "source", ErrorType.SYNTAX_ERROR);
        AssemblyException exception = new AssemblyException(originalError);

        assertSame(originalError, exception.getError());
    }
}
