package engine.metadata;

import static org.junit.Assert.*;
import org.junit.Test;
import org.junit.Before;

import java.util.Map;
import java.util.Set;

/**
 * Tests for the ProgramMetadata class
 *
 * ProgramMetadata tracks which memory addresses contain instructions,
 * data, labels, and the entry point. Essential for disassembly,
 * debugging, and visualization.
 */
public class ProgramMetadataTest {

    private ProgramMetadata metadata;

    @Before
    public void setUp() {
        metadata = new ProgramMetadata(0);
    }

    // ========== Constructor Tests ==========

    @Test
    public void testDefaultConstructor() {
        ProgramMetadata meta = new ProgramMetadata(0);
        assertEquals(0, meta.getEntryPoint());
        assertEquals(0, meta.getInstructionCount());
        assertEquals(0, meta.getDataCount());
        assertTrue(meta.getAllLabels().isEmpty());
    }

    @Test
    public void testConstructorWithNonZeroEntryPoint() {
        ProgramMetadata meta = new ProgramMetadata(0x100);
        assertEquals(0x100, meta.getEntryPoint());
    }

    // ========== Instruction Marking Tests ==========

    @Test
    public void testMarkInstruction() {
        metadata.markInstruction(0);
        assertTrue(metadata.isInstruction(0));
        assertFalse(metadata.isData(0));
    }

    @Test
    public void testMarkInstructionRemovesData() {
        metadata.markData(0);
        assertTrue(metadata.isData(0));

        metadata.markInstruction(0);
        assertTrue(metadata.isInstruction(0));
        assertFalse(metadata.isData(0));
    }

    @Test
    public void testMarkMultipleInstructions() {
        metadata.markInstruction(0);
        metadata.markInstruction(2);
        metadata.markInstruction(4);

        assertEquals(3, metadata.getInstructionCount());
        assertTrue(metadata.isInstruction(0));
        assertTrue(metadata.isInstruction(2));
        assertTrue(metadata.isInstruction(4));
    }

    // ========== Data Marking Tests ==========

    @Test
    public void testMarkData() {
        metadata.markData(100);
        assertTrue(metadata.isData(100));
        assertFalse(metadata.isInstruction(100));
    }

    @Test
    public void testMarkDataRemovesInstruction() {
        metadata.markInstruction(100);
        assertTrue(metadata.isInstruction(100));

        metadata.markData(100);
        assertTrue(metadata.isData(100));
        assertFalse(metadata.isInstruction(100));
    }

    @Test
    public void testMarkMultipleData() {
        metadata.markData(50);
        metadata.markData(52);
        metadata.markData(54);

        assertEquals(3, metadata.getDataCount());
        assertTrue(metadata.isData(50));
        assertTrue(metadata.isData(52));
        assertTrue(metadata.isData(54));
    }

    // ========== isKnown Tests ==========

    @Test
    public void testIsKnownForInstruction() {
        metadata.markInstruction(10);
        assertTrue(metadata.isKnown(10));
    }

    @Test
    public void testIsKnownForData() {
        metadata.markData(20);
        assertTrue(metadata.isKnown(20));
    }

    @Test
    public void testIsKnownForUnknown() {
        assertFalse(metadata.isKnown(30));
    }

    // ========== Label Tests ==========

    @Test
    public void testAddLabel() {
        metadata.addLabel("main", 0);
        assertTrue(metadata.hasLabel(0));
        assertTrue(metadata.hasLabel("main"));
        assertEquals("main", metadata.getLabel(0));
        assertEquals(Integer.valueOf(0), metadata.getAddress("main"));
    }

    @Test
    public void testAddMultipleLabels() {
        metadata.addLabel("start", 0);
        metadata.addLabel("loop", 10);
        metadata.addLabel("end", 20);

        assertEquals(3, metadata.getAllLabels().size());
        assertEquals("start", metadata.getLabel(0));
        assertEquals("loop", metadata.getLabel(10));
        assertEquals("end", metadata.getLabel(20));
    }

    @Test
    public void testHasLabelByAddress() {
        metadata.addLabel("test", 100);
        assertTrue(metadata.hasLabel(100));
        assertFalse(metadata.hasLabel(200));
    }

    @Test
    public void testHasLabelByName() {
        metadata.addLabel("myLabel", 50);
        assertTrue(metadata.hasLabel("myLabel"));
        assertFalse(metadata.hasLabel("otherLabel"));
    }

    @Test
    public void testGetLabelReturnsNullForUnknown() {
        assertNull(metadata.getLabel(999));
    }

    @Test
    public void testGetAddressReturnsNullForUnknown() {
        assertNull(metadata.getAddress("unknownLabel"));
    }

    // ========== Immutable Collection Tests ==========

    @Test
    public void testGetInstructionAddressesIsImmutable() {
        metadata.markInstruction(0);
        Set<Integer> addresses = metadata.getInstructionAddresses();

        try {
            addresses.add(100);
            fail("Should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // Expected
        }
    }

    @Test
    public void testGetDataAddressesIsImmutable() {
        metadata.markData(0);
        Set<Integer> addresses = metadata.getDataAddresses();

        try {
            addresses.add(100);
            fail("Should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // Expected
        }
    }

    @Test
    public void testGetAllLabelsIsImmutable() {
        metadata.addLabel("test", 0);
        Map<String, Integer> labels = metadata.getAllLabels();

        try {
            labels.put("new", 100);
            fail("Should throw UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // Expected
        }
    }

    // ========== Navigation Tests ==========

    @Test
    public void testGetNextInstruction() {
        metadata.markInstruction(0);
        metadata.markInstruction(4);
        metadata.markInstruction(10);

        assertEquals(4, metadata.getNextInstruction(0));
        assertEquals(10, metadata.getNextInstruction(4));
    }

    @Test
    public void testGetNextInstructionNoMore() {
        metadata.markInstruction(0);
        metadata.markInstruction(4);

        assertEquals(-1, metadata.getNextInstruction(4));
    }

    @Test
    public void testGetPreviousInstruction() {
        metadata.markInstruction(0);
        metadata.markInstruction(4);
        metadata.markInstruction(10);

        assertEquals(4, metadata.getPreviousInstruction(10));
        assertEquals(0, metadata.getPreviousInstruction(4));
    }

    @Test
    public void testGetPreviousInstructionNoMore() {
        metadata.markInstruction(4);
        metadata.markInstruction(10);

        assertEquals(-1, metadata.getPreviousInstruction(4));
    }

    @Test
    public void testGetNextInstructionSkipsNonInstructions() {
        metadata.markInstruction(0);
        metadata.markData(2);  // Data at address 2
        metadata.markInstruction(4);

        assertEquals(4, metadata.getNextInstruction(0));
    }

    // ========== Builder Tests ==========

    @Test
    public void testBuilder() {
        ProgramMetadata built = ProgramMetadata.builder()
            .entryPoint(0x100)
            .markInstruction(0x100)
            .markInstruction(0x102)
            .markData(0x200)
            .addLabel("main", 0x100)
            .build();

        assertEquals(0x100, built.getEntryPoint());
        assertTrue(built.isInstruction(0x100));
        assertTrue(built.isInstruction(0x102));
        assertTrue(built.isData(0x200));
        assertEquals("main", built.getLabel(0x100));
    }

    @Test
    public void testBuilderDefaultEntryPoint() {
        ProgramMetadata built = ProgramMetadata.builder().build();
        assertEquals(0, built.getEntryPoint());
    }

    @Test
    public void testBuilderMarkInstructionRemovesData() {
        ProgramMetadata built = ProgramMetadata.builder()
            .markData(0)
            .markInstruction(0)
            .build();

        assertTrue(built.isInstruction(0));
        assertFalse(built.isData(0));
    }

    @Test
    public void testBuilderMarkDataRemovesInstruction() {
        ProgramMetadata built = ProgramMetadata.builder()
            .markInstruction(0)
            .markData(0)
            .build();

        assertTrue(built.isData(0));
        assertFalse(built.isInstruction(0));
    }

    // ========== Count Tests ==========

    @Test
    public void testInstructionCount() {
        assertEquals(0, metadata.getInstructionCount());

        metadata.markInstruction(0);
        assertEquals(1, metadata.getInstructionCount());

        metadata.markInstruction(2);
        metadata.markInstruction(4);
        assertEquals(3, metadata.getInstructionCount());
    }

    @Test
    public void testDataCount() {
        assertEquals(0, metadata.getDataCount());

        metadata.markData(100);
        assertEquals(1, metadata.getDataCount());

        metadata.markData(102);
        metadata.markData(104);
        assertEquals(3, metadata.getDataCount());
    }

    // ========== ToString Test ==========

    @Test
    public void testToString() {
        metadata.markInstruction(0);
        metadata.markInstruction(2);
        metadata.markData(100);
        metadata.addLabel("main", 0);

        String str = metadata.toString();
        assertTrue(str.contains("ProgramMetadata"));
        assertTrue(str.contains("instructions=2"));
        assertTrue(str.contains("data=1"));
        assertTrue(str.contains("labels=1"));
    }

    @Test
    public void testToStringEntryPoint() {
        ProgramMetadata meta = new ProgramMetadata(0x100);
        String str = meta.toString();
        assertTrue(str.contains("0100"));  // Hex format
    }

    // ========== Edge Cases ==========

    @Test
    public void testSameAddressCanHaveLabelAndInstruction() {
        metadata.markInstruction(0);
        metadata.addLabel("start", 0);

        assertTrue(metadata.isInstruction(0));
        assertTrue(metadata.hasLabel(0));
        assertEquals("start", metadata.getLabel(0));
    }

    @Test
    public void testSameAddressCanHaveLabelAndData() {
        metadata.markData(100);
        metadata.addLabel("myData", 100);

        assertTrue(metadata.isData(100));
        assertTrue(metadata.hasLabel(100));
        assertEquals("myData", metadata.getLabel(100));
    }

    @Test
    public void testLabelOverwrite() {
        // Adding a new label at same address overwrites the old one
        metadata.addLabel("first", 0);
        metadata.addLabel("second", 0);

        assertEquals("second", metadata.getLabel(0));
    }
}
