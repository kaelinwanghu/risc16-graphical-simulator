package engine.assembly.assembler;

import static org.junit.Assert.*;
import org.junit.Test;

/**
 * Tests for the Token class
 *
 * Token represents a tokenized line of assembly code with optional label,
 * operation, operands, and source information.
 */
public class TokenTest {

    // ========== Constructor Tests ==========

    @Test
    public void testTokenWithLabel() {
        Token token = new Token(1, "main", "add", new String[]{"r1", "r2", "r3"}, "main: add r1, r2, r3");

        assertEquals(1, token.getLineNumber());
        assertEquals("main", token.getLabel());
        assertTrue(token.hasLabel());
        assertEquals("add", token.getOperation());
        assertArrayEquals(new String[]{"r1", "r2", "r3"}, token.getOperands());
        assertEquals("main: add r1, r2, r3", token.getOriginalLine());
    }

    @Test
    public void testTokenWithoutLabel() {
        Token token = new Token(5, null, "addi", new String[]{"r4", "r5", "10"}, "addi r4, r5, 10");

        assertEquals(5, token.getLineNumber());
        assertNull(token.getLabel());
        assertFalse(token.hasLabel());
        assertEquals("addi", token.getOperation());
    }

    @Test
    public void testTokenNoOperands() {
        Token token = new Token(1, null, "nop", new String[]{}, "nop");

        assertEquals("nop", token.getOperation());
        assertEquals(0, token.getOperands().length);
    }

    @Test
    public void testTokenSingleOperand() {
        Token token = new Token(1, null, ".fill", new String[]{"42"}, ".fill 42");

        assertEquals(".fill", token.getOperation());
        assertEquals(1, token.getOperands().length);
        assertEquals("42", token.getOperands()[0]);
    }

    // ========== toString Tests ==========

    @Test
    public void testToStringWithLabel() {
        Token token = new Token(1, "loop", "beq", new String[]{"r0", "r0", "loop"}, "loop: beq r0, r0, loop");

        String str = token.toString();
        assertTrue(str.contains("loop:"));
        assertTrue(str.contains("beq"));
        assertTrue(str.contains("r0"));
    }

    @Test
    public void testToStringWithoutLabel() {
        Token token = new Token(1, null, "add", new String[]{"r1", "r2", "r3"}, "add r1, r2, r3");

        String str = token.toString();
        assertFalse(str.contains(":"));
        assertTrue(str.contains("add"));
        assertTrue(str.contains("r1, r2, r3"));
    }

    @Test
    public void testToStringNoOperands() {
        Token token = new Token(1, null, "halt", new String[]{}, "halt");

        String str = token.toString();
        assertEquals("halt", str);
    }

    // ========== Edge Cases ==========

    @Test
    public void testEmptyOperandsArray() {
        Token token = new Token(1, null, "test", new String[]{}, "test");

        assertEquals(0, token.getOperands().length);
    }

    @Test
    public void testLineNumberPreserved() {
        Token token = new Token(100, null, "add", new String[]{"r1", "r2", "r3"}, "add r1, r2, r3");

        assertEquals(100, token.getLineNumber());
    }
}
