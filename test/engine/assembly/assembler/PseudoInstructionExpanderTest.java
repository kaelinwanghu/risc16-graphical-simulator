package engine.assembly.assembler;

import static org.junit.Assert.*;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * Tests for the PseudoInstructionExpander class
 *
 * Tests expansion of pseudo-instructions (NOP, HALT, LLI, MOVI) into
 * real RiSC-16 instructions.
 */
public class PseudoInstructionExpanderTest {

    // ========== isPseudoInstruction Tests ==========

    @Test
    public void testIsPseudoInstructionTrue() {
        assertTrue(PseudoInstructionExpander.isPseudoInstruction("nop"));
        assertTrue(PseudoInstructionExpander.isPseudoInstruction("halt"));
        assertTrue(PseudoInstructionExpander.isPseudoInstruction("lli"));
        assertTrue(PseudoInstructionExpander.isPseudoInstruction("movi"));
    }

    @Test
    public void testIsPseudoInstructionFalse() {
        assertFalse(PseudoInstructionExpander.isPseudoInstruction("add"));
        assertFalse(PseudoInstructionExpander.isPseudoInstruction("addi"));
        assertFalse(PseudoInstructionExpander.isPseudoInstruction("beq"));
        assertFalse(PseudoInstructionExpander.isPseudoInstruction(".fill"));
    }

    // ========== NOP Expansion Tests ==========

    @Test
    public void testExpandNop() {
        Token nop = new Token(1, null, "nop", new String[]{}, "nop");
        List<Token> input = new ArrayList<>();
        input.add(nop);

        List<Token> expanded = PseudoInstructionExpander.expandAll(input);

        assertEquals(1, expanded.size());
        Token result = expanded.get(0);
        assertEquals("add", result.getOperation());
        assertArrayEquals(new String[]{"r0", "r0", "r0"}, result.getOperands());
    }

    @Test
    public void testExpandNopWithLabel() {
        Token nop = new Token(1, "loop", "nop", new String[]{}, "loop: nop");
        List<Token> input = new ArrayList<>();
        input.add(nop);

        List<Token> expanded = PseudoInstructionExpander.expandAll(input);

        assertEquals(1, expanded.size());
        assertEquals("loop", expanded.get(0).getLabel());
    }

    // ========== HALT Expansion Tests ==========

    @Test
    public void testExpandHalt() {
        Token halt = new Token(1, null, "halt", new String[]{}, "halt");
        List<Token> input = new ArrayList<>();
        input.add(halt);

        List<Token> expanded = PseudoInstructionExpander.expandAll(input);

        assertEquals(1, expanded.size());
        Token result = expanded.get(0);
        assertEquals("jalr", result.getOperation());
        assertArrayEquals(new String[]{"r0", "r0"}, result.getOperands());
    }

    // ========== LLI Expansion Tests ==========

    @Test
    public void testExpandLli() {
        Token lli = new Token(1, null, "lli", new String[]{"r1", "63"}, "lli r1, 63");
        List<Token> input = new ArrayList<>();
        input.add(lli);

        List<Token> expanded = PseudoInstructionExpander.expandAll(input);

        assertEquals(1, expanded.size());
        Token result = expanded.get(0);
        assertEquals("addi", result.getOperation());
        assertEquals("r1", result.getOperands()[0]);
        assertEquals("r1", result.getOperands()[1]);
        assertEquals("63", result.getOperands()[2]);  // 63 & 0x3F = 63
    }

    @Test
    public void testExpandLliMasksValue() {
        // LLI with value > 63 should be masked to lower 6 bits
        Token lli = new Token(1, null, "lli", new String[]{"r1", "65"}, "lli r1, 65");
        List<Token> input = new ArrayList<>();
        input.add(lli);

        List<Token> expanded = PseudoInstructionExpander.expandAll(input);

        assertEquals(1, expanded.size());
        // 65 & 0x3F = 1
        assertEquals("1", expanded.get(0).getOperands()[2]);
    }

    // ========== MOVI Expansion Tests ==========

    @Test
    public void testExpandMovi() {
        // MOVI r1, 1000 -> LUI r1, 15; ADDI r1, r1, 40
        // 1000 = 0x3E8 -> upper 10 bits = 15 (1000 >> 6), lower 6 bits = 40 (1000 & 0x3F)
        Token movi = new Token(1, null, "movi", new String[]{"r1", "1000"}, "movi r1, 1000");
        List<Token> input = new ArrayList<>();
        input.add(movi);

        List<Token> expanded = PseudoInstructionExpander.expandAll(input);

        assertEquals(2, expanded.size());

        // First instruction: LUI
        Token lui = expanded.get(0);
        assertEquals("lui", lui.getOperation());
        assertEquals("r1", lui.getOperands()[0]);
        assertEquals("15", lui.getOperands()[1]);  // 1000 >> 6 = 15

        // Second instruction: ADDI
        Token addi = expanded.get(1);
        assertEquals("addi", addi.getOperation());
        assertEquals("r1", addi.getOperands()[0]);
        assertEquals("r1", addi.getOperands()[1]);
        assertEquals("40", addi.getOperands()[2]);  // 1000 & 0x3F = 40
    }

    @Test
    public void testExpandMoviLabelOnFirstOnly() {
        Token movi = new Token(1, "myLabel", "movi", new String[]{"r1", "100"}, "myLabel: movi r1, 100");
        List<Token> input = new ArrayList<>();
        input.add(movi);

        List<Token> expanded = PseudoInstructionExpander.expandAll(input);

        assertEquals(2, expanded.size());
        assertEquals("myLabel", expanded.get(0).getLabel());  // First instruction gets label
        assertNull(expanded.get(1).getLabel());  // Second instruction has no label
    }

    @Test
    public void testExpandMoviZero() {
        Token movi = new Token(1, null, "movi", new String[]{"r1", "0"}, "movi r1, 0");
        List<Token> input = new ArrayList<>();
        input.add(movi);

        List<Token> expanded = PseudoInstructionExpander.expandAll(input);

        assertEquals(2, expanded.size());
        assertEquals("0", expanded.get(0).getOperands()[1]);  // LUI upper = 0
        assertEquals("0", expanded.get(1).getOperands()[2]);  // ADDI lower = 0
    }

    // ========== Mixed Expansion Tests ==========

    @Test
    public void testExpandMixedTokens() {
        List<Token> input = new ArrayList<>();
        input.add(new Token(1, null, "add", new String[]{"r1", "r2", "r3"}, "add r1, r2, r3"));
        input.add(new Token(2, null, "nop", new String[]{}, "nop"));
        input.add(new Token(3, null, "addi", new String[]{"r4", "r5", "10"}, "addi r4, r5, 10"));

        List<Token> expanded = PseudoInstructionExpander.expandAll(input);

        assertEquals(3, expanded.size());
        assertEquals("add", expanded.get(0).getOperation());
        assertEquals("add", expanded.get(1).getOperation());  // NOP -> ADD r0, r0, r0
        assertEquals("addi", expanded.get(2).getOperation());
    }

    @Test
    public void testExpandPreservesNonPseudo() {
        List<Token> input = new ArrayList<>();
        input.add(new Token(1, "label", "add", new String[]{"r1", "r2", "r3"}, "label: add r1, r2, r3"));

        List<Token> expanded = PseudoInstructionExpander.expandAll(input);

        assertEquals(1, expanded.size());
        assertSame(input.get(0), expanded.get(0));  // Same object - not modified
    }

    // ========== Error Cases ==========

    @Test(expected = AssemblyException.class)
    public void testNopWithOperandsError() {
        Token nop = new Token(1, null, "nop", new String[]{"r1"}, "nop r1");
        List<Token> input = new ArrayList<>();
        input.add(nop);

        PseudoInstructionExpander.expandAll(input);
    }

    @Test(expected = AssemblyException.class)
    public void testLliWithLabelError() {
        Token lli = new Token(1, null, "lli", new String[]{"r1", "label"}, "lli r1, label");
        List<Token> input = new ArrayList<>();
        input.add(lli);

        PseudoInstructionExpander.expandAll(input);
    }

    @Test(expected = AssemblyException.class)
    public void testMoviOutOfRangeError() {
        Token movi = new Token(1, null, "movi", new String[]{"r1", "70000"}, "movi r1, 70000");
        List<Token> input = new ArrayList<>();
        input.add(movi);

        PseudoInstructionExpander.expandAll(input);
    }
}
