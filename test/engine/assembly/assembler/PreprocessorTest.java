package engine.assembly.assembler;

import static org.junit.Assert.*;
import org.junit.Test;

import java.util.List;

/**
 * Tests for the Preprocessor class
 *
 * Preprocessor handles comment removal, label extraction, and tokenization.
 */
public class PreprocessorTest {

    // ========== Basic Tokenization Tests ==========

    @Test
    public void testSimpleInstruction() {
        List<Token> tokens = Preprocessor.preprocess("add r1, r2, r3");

        assertEquals(1, tokens.size());
        Token token = tokens.get(0);
        assertEquals("add", token.getOperation());
        assertArrayEquals(new String[]{"r1", "r2", "r3"}, token.getOperands());
        assertFalse(token.hasLabel());
    }

    @Test
    public void testMultipleInstructions() {
        String source = "add r1, r2, r3\naddi r4, r5, 10\nnand r6, r7, r0";
        List<Token> tokens = Preprocessor.preprocess(source);

        assertEquals(3, tokens.size());
        assertEquals("add", tokens.get(0).getOperation());
        assertEquals("addi", tokens.get(1).getOperation());
        assertEquals("nand", tokens.get(2).getOperation());
    }

    // ========== Label Tests ==========

    @Test
    public void testLabelExtraction() {
        List<Token> tokens = Preprocessor.preprocess("main: add r1, r2, r3");

        assertEquals(1, tokens.size());
        Token token = tokens.get(0);
        assertTrue(token.hasLabel());
        assertEquals("main", token.getLabel());
        assertEquals("add", token.getOperation());
    }

    @Test
    public void testLabelWithUnderscore() {
        List<Token> tokens = Preprocessor.preprocess("my_label: add r1, r2, r3");

        assertEquals(1, tokens.size());
        assertEquals("my_label", tokens.get(0).getLabel());
    }

    @Test
    public void testLabelWithNumbers() {
        List<Token> tokens = Preprocessor.preprocess("loop1: add r1, r2, r3");

        assertEquals(1, tokens.size());
        assertEquals("loop1", tokens.get(0).getLabel());
    }

    @Test
    public void testLabelWithDot() {
        List<Token> tokens = Preprocessor.preprocess(".start: add r1, r2, r3");

        assertEquals(1, tokens.size());
        assertEquals(".start", tokens.get(0).getLabel());
    }

    // ========== Comment Tests ==========

    @Test
    public void testCommentRemoval() {
        List<Token> tokens = Preprocessor.preprocess("add r1, r2, r3 # this is a comment");

        assertEquals(1, tokens.size());
        assertEquals("add", tokens.get(0).getOperation());
    }

    @Test
    public void testFullLineComment() {
        String source = "# This is a comment\nadd r1, r2, r3";
        List<Token> tokens = Preprocessor.preprocess(source);

        assertEquals(1, tokens.size());
        assertEquals("add", tokens.get(0).getOperation());
    }

    @Test
    public void testMultipleComments() {
        String source = "# Comment 1\nadd r1, r2, r3\n# Comment 2\naddi r4, r5, 1";
        List<Token> tokens = Preprocessor.preprocess(source);

        assertEquals(2, tokens.size());
    }

    // ========== Blank Line Tests ==========

    @Test
    public void testBlankLinesIgnored() {
        String source = "add r1, r2, r3\n\n\naddi r4, r5, 1";
        List<Token> tokens = Preprocessor.preprocess(source);

        assertEquals(2, tokens.size());
    }

    @Test
    public void testWhitespaceOnlyLinesIgnored() {
        String source = "add r1, r2, r3\n   \t  \naddi r4, r5, 1";
        List<Token> tokens = Preprocessor.preprocess(source);

        assertEquals(2, tokens.size());
    }

    // ========== Operand Parsing Tests ==========

    @Test
    public void testCommaDelimitedOperands() {
        List<Token> tokens = Preprocessor.preprocess("add r1,r2,r3");

        String[] operands = tokens.get(0).getOperands();
        assertArrayEquals(new String[]{"r1", "r2", "r3"}, operands);
    }

    @Test
    public void testSpaceDelimitedOperands() {
        List<Token> tokens = Preprocessor.preprocess("add r1 r2 r3");

        String[] operands = tokens.get(0).getOperands();
        assertArrayEquals(new String[]{"r1", "r2", "r3"}, operands);
    }

    @Test
    public void testMixedDelimiters() {
        List<Token> tokens = Preprocessor.preprocess("add r1, r2  r3");

        String[] operands = tokens.get(0).getOperands();
        assertArrayEquals(new String[]{"r1", "r2", "r3"}, operands);
    }

    // ========== Line Number Tests ==========

    @Test
    public void testLineNumbersPreserved() {
        String source = "add r1, r2, r3\n\naddi r4, r5, 1";
        List<Token> tokens = Preprocessor.preprocess(source);

        assertEquals(1, tokens.get(0).getLineNumber());
        assertEquals(3, tokens.get(1).getLineNumber());  // Line 2 is blank
    }

    // ========== Case Handling Tests ==========

    @Test
    public void testOperationLowercased() {
        List<Token> tokens = Preprocessor.preprocess("ADD r1, r2, r3");

        assertEquals("add", tokens.get(0).getOperation());
    }

    // ========== Directive Tests ==========

    @Test
    public void testDirectiveTokenized() {
        List<Token> tokens = Preprocessor.preprocess(".fill 42");

        assertEquals(1, tokens.size());
        assertEquals(".fill", tokens.get(0).getOperation());
        assertArrayEquals(new String[]{"42"}, tokens.get(0).getOperands());
    }

    // ========== Error Cases ==========

    @Test(expected = AssemblyException.class)
    public void testEmptyLabel() {
        Preprocessor.preprocess(": add r1, r2, r3");
    }

    @Test(expected = AssemblyException.class)
    public void testLabelWithoutStatement() {
        Preprocessor.preprocess("label:");
    }

    @Test(expected = AssemblyException.class)
    public void testInvalidLabelCharacter() {
        Preprocessor.preprocess("my-label: add r1, r2, r3");
    }

    // ========== Windows Line Endings ==========

    @Test
    public void testWindowsLineEndings() {
        String source = "add r1, r2, r3\r\naddi r4, r5, 1";
        List<Token> tokens = Preprocessor.preprocess(source);

        assertEquals(2, tokens.size());
    }
}
