package engine.assembly;

import static org.junit.Assert.*;
import org.junit.Test;
import org.junit.Before;

import java.util.Map;
import java.util.Set;

/**
 * Tests for the SymbolTable class
 *
 * SymbolTable manages labels and their addresses during assembly.
 * Labels are case-sensitive and must be unique within a program.
 */
public class SymbolTableTest {

    private SymbolTable symbolTable;

    @Before
    public void setUp() {
        symbolTable = new SymbolTable();
    }

    // ========== Basic Define/Resolve Tests ==========

    @Test
    public void testDefineAndResolve() {
        symbolTable.define("main", 0);
        assertEquals(0, symbolTable.resolve("main"));
    }

    @Test
    public void testDefineMultipleLabels() {
        symbolTable.define("start", 0);
        symbolTable.define("loop", 10);
        symbolTable.define("end", 20);

        assertEquals(0, symbolTable.resolve("start"));
        assertEquals(10, symbolTable.resolve("loop"));
        assertEquals(20, symbolTable.resolve("end"));
    }

    @Test
    public void testDefineNonZeroAddress() {
        symbolTable.define("func", 0x1000);
        assertEquals(0x1000, symbolTable.resolve("func"));
    }

    // ========== Contains Tests ==========

    @Test
    public void testContains() {
        symbolTable.define("exists", 50);

        assertTrue(symbolTable.contains("exists"));
        assertFalse(symbolTable.contains("notExists"));
    }

    @Test
    public void testContainsEmptyTable() {
        assertFalse(symbolTable.contains("anything"));
    }

    // ========== Exception Tests ==========

    @Test(expected = IllegalArgumentException.class)
    public void testDefineNullLabel() {
        symbolTable.define(null, 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDefineEmptyLabel() {
        symbolTable.define("", 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDefineDuplicateLabel() {
        symbolTable.define("duplicate", 0);
        symbolTable.define("duplicate", 10);  // Should throw
    }

    @Test
    public void testDefineDuplicateLabelErrorMessage() {
        symbolTable.define("myLabel", 0x100);

        try {
            symbolTable.define("myLabel", 0x200);
            fail("Should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("myLabel"));
            assertTrue(e.getMessage().contains("already defined"));
            assertTrue(e.getMessage().contains("0100"));  // Original address in hex
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testResolveUndefinedLabel() {
        symbolTable.resolve("undefined");
    }

    @Test
    public void testResolveUndefinedLabelErrorMessage() {
        try {
            symbolTable.resolve("missingLabel");
            fail("Should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("missingLabel"));
            assertTrue(e.getMessage().contains("Undefined"));
        }
    }

    // ========== Case Sensitivity Tests ==========

    @Test
    public void testCaseSensitive() {
        symbolTable.define("Label", 0);
        symbolTable.define("label", 10);
        symbolTable.define("LABEL", 20);

        assertEquals(0, symbolTable.resolve("Label"));
        assertEquals(10, symbolTable.resolve("label"));
        assertEquals(20, symbolTable.resolve("LABEL"));
        assertEquals(3, symbolTable.size());
    }

    // ========== Size and Empty Tests ==========

    @Test
    public void testIsEmptyTrue() {
        assertTrue(symbolTable.isEmpty());
    }

    @Test
    public void testIsEmptyFalse() {
        symbolTable.define("label", 0);
        assertFalse(symbolTable.isEmpty());
    }

    @Test
    public void testSizeEmpty() {
        assertEquals(0, symbolTable.size());
    }

    @Test
    public void testSizeAfterDefine() {
        symbolTable.define("a", 0);
        assertEquals(1, symbolTable.size());

        symbolTable.define("b", 2);
        assertEquals(2, symbolTable.size());

        symbolTable.define("c", 4);
        assertEquals(3, symbolTable.size());
    }

    // ========== Clear Tests ==========

    @Test
    public void testClear() {
        symbolTable.define("label1", 0);
        symbolTable.define("label2", 10);
        symbolTable.define("label3", 20);

        assertEquals(3, symbolTable.size());

        symbolTable.clear();

        assertEquals(0, symbolTable.size());
        assertTrue(symbolTable.isEmpty());
        assertFalse(symbolTable.contains("label1"));
    }

    @Test
    public void testClearEmptyTable() {
        symbolTable.clear();  // Should not throw
        assertTrue(symbolTable.isEmpty());
    }

    // ========== GetAll Tests ==========

    @Test
    public void testGetAll() {
        symbolTable.define("a", 0);
        symbolTable.define("b", 10);
        symbolTable.define("c", 20);

        Map<String, Integer> all = symbolTable.getAll();

        assertEquals(3, all.size());
        assertEquals(Integer.valueOf(0), all.get("a"));
        assertEquals(Integer.valueOf(10), all.get("b"));
        assertEquals(Integer.valueOf(20), all.get("c"));
    }

    @Test
    public void testGetAllIsImmutable() {
        symbolTable.define("test", 0);
        Map<String, Integer> all = symbolTable.getAll();

        try {
            all.put("new", 100);
            fail("Should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // Expected
        }
    }

    @Test
    public void testGetAllEmpty() {
        Map<String, Integer> all = symbolTable.getAll();
        assertTrue(all.isEmpty());
    }

    // ========== GetLabelNames Tests ==========

    @Test
    public void testGetLabelNames() {
        symbolTable.define("alpha", 0);
        symbolTable.define("beta", 10);
        symbolTable.define("gamma", 20);

        Set<String> names = symbolTable.getLabelNames();

        assertEquals(3, names.size());
        assertTrue(names.contains("alpha"));
        assertTrue(names.contains("beta"));
        assertTrue(names.contains("gamma"));
    }

    @Test
    public void testGetLabelNamesIsImmutable() {
        symbolTable.define("test", 0);
        Set<String> names = symbolTable.getLabelNames();

        try {
            names.add("new");
            fail("Should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // Expected
        }
    }

    // ========== GetLabelAt Tests ==========

    @Test
    public void testGetLabelAt() {
        symbolTable.define("myLabel", 100);

        assertEquals("myLabel", symbolTable.getLabelAt(100));
    }

    @Test
    public void testGetLabelAtReturnsNullForUnknown() {
        symbolTable.define("somewhere", 50);

        assertNull(symbolTable.getLabelAt(100));
    }

    @Test
    public void testGetLabelAtEmptyTable() {
        assertNull(symbolTable.getLabelAt(0));
    }

    @Test
    public void testGetLabelAtMultipleLabels() {
        symbolTable.define("first", 0);
        symbolTable.define("second", 10);
        symbolTable.define("third", 20);

        assertEquals("first", symbolTable.getLabelAt(0));
        assertEquals("second", symbolTable.getLabelAt(10));
        assertEquals("third", symbolTable.getLabelAt(20));
        assertNull(symbolTable.getLabelAt(5));  // No label at address 5
    }

    // ========== toFormattedString Tests ==========

    @Test
    public void testToFormattedStringEmpty() {
        String formatted = symbolTable.toFormattedString();
        assertTrue(formatted.contains("No labels defined"));
    }

    @Test
    public void testToFormattedString() {
        symbolTable.define("main", 0);
        symbolTable.define("helper", 0x100);

        String formatted = symbolTable.toFormattedString();

        assertTrue(formatted.contains("Symbol Table"));
        assertTrue(formatted.contains("main"));
        assertTrue(formatted.contains("helper"));
        assertTrue(formatted.contains("0000"));  // Hex address for main
        assertTrue(formatted.contains("0100"));  // Hex address for helper
    }

    // ========== toString Tests ==========

    @Test
    public void testToStringEmpty() {
        String str = symbolTable.toString();
        assertTrue(str.contains("0 labels"));
    }

    @Test
    public void testToString() {
        symbolTable.define("a", 0);
        symbolTable.define("b", 10);

        String str = symbolTable.toString();
        assertTrue(str.contains("2 labels"));
        assertTrue(str.contains("SymbolTable"));
    }

    // ========== Edge Cases ==========

    @Test
    public void testLabelWithNumbers() {
        symbolTable.define("label123", 0);
        symbolTable.define("123label", 10);
        symbolTable.define("lab456el", 20);

        assertEquals(0, symbolTable.resolve("label123"));
        assertEquals(10, symbolTable.resolve("123label"));
        assertEquals(20, symbolTable.resolve("lab456el"));
    }

    @Test
    public void testLabelWithUnderscore() {
        symbolTable.define("my_label", 0);
        symbolTable.define("_private", 10);
        symbolTable.define("end_", 20);

        assertEquals(0, symbolTable.resolve("my_label"));
        assertEquals(10, symbolTable.resolve("_private"));
        assertEquals(20, symbolTable.resolve("end_"));
    }

    @Test
    public void testMaxAddress() {
        symbolTable.define("maxAddr", 0xFFFF);
        assertEquals(0xFFFF, symbolTable.resolve("maxAddr"));
    }

    @Test
    public void testDefineAfterClear() {
        symbolTable.define("original", 0);
        symbolTable.clear();
        symbolTable.define("original", 100);  // Should work after clear

        assertEquals(100, symbolTable.resolve("original"));
    }
}
