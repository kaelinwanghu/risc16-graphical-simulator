package engine.memory;

import static org.junit.Assert.*;
import org.junit.Test;
import org.junit.Before;

import engine.assembly.AssemblyResult;
import engine.assembly.assembler.Assembler;
import engine.isa.InstructionEncoder;
import engine.isa.InstructionFormat;

/**
 * Tests for the RiSC-16 Memory class
 *
 * Memory is the raw byte storage for the RiSC-16 processor.
 * - Byte-addressable with word-aligned access support
 * - Big-endian byte ordering
 * - 16-bit words (2 bytes)
 * - Size must be power of 2 between 128 bytes and 4MB
 */
public class MemoryTest {

    private Memory memory;

    @Before
    public void setUp() {
        memory = new Memory(1024);  // 1KB for testing
    }

    // ========== Assembly-Based Memory Tests ==========

    @Test
    public void testStoreAssembledInstructions() {
        String source =
            "add r1, r2, r3\n" +
            "addi r4, r5, 10\n" +
            "lui r6, 100";

        AssemblyResult result = Assembler.assemble(source);
        assertTrue(result.isSuccess());

        // Store assembled instructions in memory
        int address = 0;
        for (InstructionFormat instr : result.getInstructions()) {
            short encoded = InstructionEncoder.encode(instr);
            memory.writeWord(address, encoded);
            address += 2;
        }

        // Read back and verify
        assertEquals(0x0503, memory.readWord(0) & 0xFFFF);  // ADD r1, r2, r3
    }

    @Test
    public void testLoadAndExecuteProgram() {
        // Assemble a simple program
        String source =
            "       lui r1, 100\n" +
            "       addi r1, r1, 5\n" +
            "       sw r1, r0, 0\n" +
            "       lw r2, r0, 0";

        AssemblyResult result = Assembler.assemble(source);
        assertTrue(result.isSuccess());

        // Load program into memory at address 0
        int programAddr = 0;
        for (InstructionFormat instr : result.getInstructions()) {
            short encoded = InstructionEncoder.encode(instr);
            memory.writeWord(programAddr, encoded);
            programAddr += 2;
        }

        // Verify each instruction can be read back and decoded
        for (int i = 0; i < result.getInstructionCount(); i++) {
            short encoded = memory.readWord(i * 2);
            InstructionFormat decoded = InstructionEncoder.decode(encoded, i * 2);
            assertEquals(result.getInstructions().get(i).getOpcode(), decoded.getOpcode());
        }
    }

    @Test
    public void testStoreDataFromFillDirective() {
        String source =
            "data1: .fill 42\n" +
            "data2: .fill 0xFF\n" +
            "data3: .fill -1";

        AssemblyResult result = Assembler.assemble(source);
        assertTrue(result.isSuccess());

        // Store data segments in memory
        for (AssemblyResult.DataSegment segment : result.getDataSegments()) {
            memory.writeWord(segment.getAddress(), segment.getValue());
        }

        // Read back and verify
        assertEquals(42, memory.readWord(0));
        assertEquals(0xFF, memory.readWord(2) & 0xFFFF);
        assertEquals(-1, memory.readWord(4));
    }

    // ========== Memory Creation Tests ==========

    @Test
    public void testCreateMemory() {
        Memory mem = new Memory(256);
        assertEquals(256, mem.getSize());
    }

    @Test
    public void testCreateMemoryMinSize() {
        Memory mem = new Memory(128);
        assertEquals(128, mem.getSize());
    }

    @Test
    public void testCreateMemoryMaxSize() {
        Memory mem = new Memory(4 * 1024 * 1024);  // 4MB
        assertEquals(4 * 1024 * 1024, mem.getSize());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateMemoryTooSmall() {
        new Memory(64);  // Less than 128
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateMemoryTooLarge() {
        new Memory(8 * 1024 * 1024);  // 8MB > 4MB max
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateMemoryNotPowerOfTwo() {
        new Memory(1000);  // Not a power of 2
    }

    // ========== Byte Read/Write Tests ==========

    @Test
    public void testReadWriteByte() {
        memory.writeByte(0, (byte) 0x42);
        assertEquals((byte) 0x42, memory.readByte(0));
    }

    @Test
    public void testReadWriteByteAtEnd() {
        int lastAddr = memory.getMaxAddress();
        memory.writeByte(lastAddr, (byte) 0xFF);
        assertEquals((byte) 0xFF, memory.readByte(lastAddr));
    }

    @Test
    public void testReadWriteMultipleBytes() {
        memory.writeByte(0, (byte) 0x01);
        memory.writeByte(1, (byte) 0x02);
        memory.writeByte(2, (byte) 0x03);

        assertEquals((byte) 0x01, memory.readByte(0));
        assertEquals((byte) 0x02, memory.readByte(1));
        assertEquals((byte) 0x03, memory.readByte(2));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testReadByteOutOfBounds() {
        memory.readByte(1024);  // Out of bounds
    }

    @Test(expected = IllegalArgumentException.class)
    public void testWriteByteOutOfBounds() {
        memory.writeByte(-1, (byte) 0x00);  // Negative address
    }

    // ========== Word Read/Write Tests (Big-Endian) ==========

    @Test
    public void testReadWriteWord() {
        memory.writeWord(0, (short) 0x1234);
        assertEquals((short) 0x1234, memory.readWord(0));
    }

    @Test
    public void testWordBigEndianOrder() {
        memory.writeWord(0, (short) 0xABCD);

        // Big-endian: high byte first
        assertEquals((byte) 0xAB, memory.readByte(0));  // High byte
        assertEquals((byte) 0xCD, memory.readByte(1));  // Low byte
    }

    @Test
    public void testReadWriteWordNegative() {
        memory.writeWord(0, (short) -1);  // 0xFFFF
        assertEquals((short) -1, memory.readWord(0));
    }

    @Test
    public void testReadWriteWordAtEnd() {
        int lastWordAddr = memory.getMaxWordAddress();
        memory.writeWord(lastWordAddr, (short) 0x5678);
        assertEquals((short) 0x5678, memory.readWord(lastWordAddr));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testReadWordUnaligned() {
        memory.readWord(1);  // Odd address not word-aligned
    }

    @Test(expected = IllegalArgumentException.class)
    public void testWriteWordUnaligned() {
        memory.writeWord(3, (short) 0x1234);  // Odd address
    }

    @Test(expected = IllegalArgumentException.class)
    public void testReadWordOutOfBounds() {
        memory.readWord(1024);  // Out of bounds
    }

    // ========== Bulk Read/Write Tests ==========

    @Test
    public void testReadWriteBytes() {
        byte[] data = {0x01, 0x02, 0x03, 0x04};
        memory.writeBytes(0, data);

        byte[] read = memory.readBytes(0, 4);
        assertArrayEquals(data, read);
    }

    @Test
    public void testReadBytesReturnsNewArray() {
        byte[] data = {0x01, 0x02, 0x03};
        memory.writeBytes(0, data);

        byte[] read1 = memory.readBytes(0, 3);
        byte[] read2 = memory.readBytes(0, 3);

        assertNotSame(read1, read2);  // Different array instances
        assertArrayEquals(read1, read2);  // Same contents
    }

    @Test(expected = IllegalArgumentException.class)
    public void testWriteBytesNull() {
        memory.writeBytes(0, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testReadBytesNegativeLength() {
        memory.readBytes(0, -1);
    }

    // ========== Clear Tests ==========

    @Test
    public void testClear() {
        memory.writeByte(0, (byte) 0xFF);
        memory.writeByte(100, (byte) 0xAB);
        memory.writeWord(200, (short) 0x1234);

        memory.clear();

        assertEquals((byte) 0, memory.readByte(0));
        assertEquals((byte) 0, memory.readByte(100));
        assertEquals((short) 0, memory.readWord(200));
    }

    // ========== Address Validation Tests ==========

    @Test
    public void testIsValidAddress() {
        assertTrue(memory.isValidAddress(0));
        assertTrue(memory.isValidAddress(512));
        assertTrue(memory.isValidAddress(1023));

        assertFalse(memory.isValidAddress(-1));
        assertFalse(memory.isValidAddress(1024));
    }

    @Test
    public void testIsWordAligned() {
        assertTrue(memory.isWordAligned(0));
        assertTrue(memory.isWordAligned(2));
        assertTrue(memory.isWordAligned(100));

        assertFalse(memory.isWordAligned(1));
        assertFalse(memory.isWordAligned(3));
        assertFalse(memory.isWordAligned(101));
    }

    @Test
    public void testGetMaxAddress() {
        assertEquals(1023, memory.getMaxAddress());
    }

    @Test
    public void testGetMaxWordAddress() {
        assertEquals(1022, memory.getMaxWordAddress());
    }

    // ========== Addressable Interface Tests ==========

    @Test
    public void testGetData() {
        memory.writeByte(0, (byte) 0x11);
        memory.writeByte(1, (byte) 0x22);

        byte[] data = memory.getData(0, 2);
        assertEquals((byte) 0x11, data[0]);
        assertEquals((byte) 0x22, data[1]);
    }

    @Test
    public void testSetData() {
        byte[] data = {0x33, 0x44, 0x55};
        memory.setData(10, data);

        assertEquals((byte) 0x33, memory.readByte(10));
        assertEquals((byte) 0x44, memory.readByte(11));
        assertEquals((byte) 0x55, memory.readByte(12));
    }

    // ========== Hex Dump Tests ==========

    @Test
    public void testDumpHex() {
        memory.writeByte(0, (byte) 0x01);
        memory.writeByte(1, (byte) 0x02);
        memory.writeByte(2, (byte) 0x03);

        String dump = memory.dumpHex(0, 3);
        assertTrue(dump.contains("01"));
        assertTrue(dump.contains("02"));
        assertTrue(dump.contains("03"));
    }

    @Test
    public void testDumpHexInvalidRange() {
        String dump = memory.dumpHex(2000, 10);
        assertTrue(dump.contains("Invalid"));
    }

    // ========== ToString Test ==========

    @Test
    public void testToString() {
        String str = memory.toString();
        assertTrue(str.contains("1024"));
        assertTrue(str.contains("Memory"));
    }
}
