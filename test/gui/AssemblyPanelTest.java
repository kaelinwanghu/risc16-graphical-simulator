package gui;

import static org.junit.Assert.*;
import org.junit.Test;
import org.junit.Before;

import engine.assembly.AssemblyResult;
import engine.assembly.assembler.Assembler;
import gui.components.ResizableTable;

import java.lang.reflect.Field;

/**
 * Tests for the AssemblyPanel class.
 *
 * Tests PC-to-row highlighting and hex/decimal format switching.
 * Uses Assembler.assemble() to produce real AssemblyResult objects.
 */
public class AssemblyPanelTest {

    private AssemblyPanel panel;
    private AssemblyResult result;

    private static final String THREE_INSTRUCTION_PROGRAM =
        "addi r1, r0, 5\n" +
        "addi r2, r0, 10\n" +
        "jalr r0, r0";

    @Before
    public void setUp() {
        result = Assembler.assemble(THREE_INSTRUCTION_PROGRAM);
        assertTrue("Assembly should succeed", result.isSuccess());
        // Hex = true (default HEX button state)
        panel = new AssemblyPanel(result, true);
    }

    // ========================================================================
    // Reflection helpers
    // ========================================================================

    private int getHighlightedRow() throws Exception {
        Field field = AssemblyPanel.class.getDeclaredField("highlightedRow");
        field.setAccessible(true);
        return (int) field.get(panel);
    }

    private int[] getAddresses() throws Exception {
        Field field = AssemblyPanel.class.getDeclaredField("addresses");
        field.setAccessible(true);
        return (int[]) field.get(panel);
    }

    private ResizableTable getResizableTable() throws Exception {
        Field field = AssemblyPanel.class.getDeclaredField("resizableTable");
        field.setAccessible(true);
        return (ResizableTable) field.get(panel);
    }

    // ========================================================================
    // Constructor tests
    // ========================================================================

    @Test
    public void testConstructorSetsCorrectRowCount() throws Exception {
        ResizableTable table = getResizableTable();
        assertEquals(3, table.getRowCount());
    }

    @Test
    public void testConstructorPopulatesAddresses() throws Exception {
        int[] addresses = getAddresses();
        assertEquals(3, addresses.length);
        // Byte-addressed: 0, 2, 4
        assertEquals(0, addresses[0]);
        assertEquals(2, addresses[1]);
        assertEquals(4, addresses[2]);
    }

    // ========================================================================
    // highlightInstruction tests
    // ========================================================================

    @Test
    public void testHighlightInstructionSetsCorrectRow() throws Exception {
        panel.highlightInstruction(0);
        assertEquals(0, getHighlightedRow());
    }

    @Test
    public void testHighlightInstructionMiddleInstruction() throws Exception {
        panel.highlightInstruction(2);
        assertEquals(1, getHighlightedRow());
    }

    @Test
    public void testHighlightInstructionLastInstruction() throws Exception {
        panel.highlightInstruction(4);
        assertEquals(2, getHighlightedRow());
    }

    @Test
    public void testHighlightInstructionInvalidPCSetsNegativeOne() throws Exception {
        panel.highlightInstruction(999);
        assertEquals(-1, getHighlightedRow());
    }

    @Test
    public void testHighlightInstructionAfterInvalidResetsRow() throws Exception {
        // Highlight valid, then invalid
        panel.highlightInstruction(2);
        assertEquals(1, getHighlightedRow());

        panel.highlightInstruction(999);
        assertEquals(-1, getHighlightedRow());
    }

    // ========================================================================
    // setFormat tests
    // ========================================================================

    @Test
    public void testSetFormatHexUpdatesAddresses() throws Exception {
        panel.setFormat(true);

        ResizableTable table = getResizableTable();
        String addr0 = table.getValueAt(0, 0).toString();
        assertTrue("Should contain hex prefix, got: " + addr0, addr0.contains("0x"));
    }

    @Test
    public void testSetFormatDecUpdatesAddresses() throws Exception {
        panel.setFormat(false);

        ResizableTable table = getResizableTable();
        String addr0 = table.getValueAt(0, 0).toString().trim();
        // Should be "0" (decimal), not contain "0x"
        assertFalse("Should not contain hex prefix, got: " + addr0, addr0.contains("0x"));
        assertEquals("0", addr0);
    }
}
