package engine.execution;

import static org.junit.Assert.*;
import org.junit.Test;
import org.junit.Before;

import engine.assembly.AssemblyResult;
import engine.assembly.assembler.Assembler;
import engine.isa.InstructionEncoder;
import engine.isa.InstructionFormat;
import engine.isa.Opcode;
import engine.memory.Memory;
import engine.metadata.ProgramMetadata;

/**
 * Tests for the ProgramLoader class
 *
 * ProgramLoader is the bridge between assembly and execution:
 * - Takes AssemblyResult (from Assembler)
 * - Writes encoded instructions to Memory
 * - Writes data segments to Memory
 * - Creates ProgramMetadata describing the loaded program
 */
public class ProgramLoaderTest {

    private Memory memory;
    private ProgramLoader loader;

    @Before
    public void setUp() {
        memory = new Memory(1024);
        loader = new ProgramLoader(memory);
    }

    // ========== Assembly-Based Loading Tests ==========

    @Test
    public void testLoadSimpleProgram() {
        String source =
            "add r1, r2, r3\n" +
            "addi r4, r5, 10\n" +
            "lui r6, 100";

        AssemblyResult result = Assembler.assemble(source);
        assertTrue(result.isSuccess());

        ProgramMetadata metadata = loader.load(result);

        // Verify instructions in memory
        for (InstructionFormat instr : result.getInstructions()) {
            short expected = InstructionEncoder.encode(instr);
            short actual = memory.readWord(instr.getAddress());
            assertEquals("Instruction at address " + instr.getAddress(),
                expected, actual);
        }

        // Verify metadata
        assertEquals(0, metadata.getEntryPoint());
        assertEquals(3, metadata.getInstructionCount());
        assertTrue(metadata.isInstruction(0));
        assertTrue(metadata.isInstruction(2));
        assertTrue(metadata.isInstruction(4));
    }

    @Test
    public void testLoadProgramWithData() {
        String source =
            "       lw r1, r0, data\n" +
            "       jalr r0, r0\n" +
            "data:  .fill 42";

        AssemblyResult result = Assembler.assemble(source);
        assertTrue(result.isSuccess());

        ProgramMetadata metadata = loader.load(result);

        // Verify data was loaded
        assertEquals(42, memory.readWord(4));  // data at address 4

        // Verify metadata distinguishes instructions from data
        assertTrue(metadata.isInstruction(0));  // lw
        assertTrue(metadata.isInstruction(2));  // jalr
        assertTrue(metadata.isData(4));         // .fill
        assertFalse(metadata.isInstruction(4)); // data is not instruction
    }

    @Test
    public void testLoadProgramWithLabels() {
        String source =
            "start: add r1, r2, r3\n" +
            "loop:  addi r1, r1, 1\n" +
            "       beq r1, r0, loop\n" +
            "end:   jalr r0, r0";

        AssemblyResult result = Assembler.assemble(source);
        assertTrue(result.isSuccess());

        ProgramMetadata metadata = loader.load(result);

        // Verify labels in metadata
        assertTrue(metadata.hasLabel("start"));
        assertTrue(metadata.hasLabel("loop"));
        assertTrue(metadata.hasLabel("end"));

        assertEquals(Integer.valueOf(0), metadata.getAddress("start"));
        assertEquals(Integer.valueOf(2), metadata.getAddress("loop"));
        assertEquals(Integer.valueOf(6), metadata.getAddress("end"));

        // Reverse lookup
        assertEquals("start", metadata.getLabel(0));
        assertEquals("loop", metadata.getLabel(2));
        assertEquals("end", metadata.getLabel(6));
    }

    @Test
    public void testLoadClearsMemory() {
        // Write some data to memory first
        memory.writeWord(0, (short) 0xFFFF);
        memory.writeWord(2, (short) 0xFFFF);
        memory.writeWord(100, (short) 0xFFFF);

        String source = "add r0, r0, r0";  // NOP at address 0
        AssemblyResult result = Assembler.assemble(source);
        loader.load(result);

        // Memory should be cleared except for loaded instruction
        assertEquals(0x0000, memory.readWord(0) & 0xFFFF);  // NOP = 0
        assertEquals(0, memory.readWord(100));  // Cleared
    }

    @Test
    public void testLoadMultipleDataSegments() {
        String source =
            "       lui r1, 0\n" +
            "val1:  .fill 100\n" +
            "val2:  .fill 200\n" +
            "val3:  .fill -1";

        AssemblyResult result = Assembler.assemble(source);
        assertTrue(result.isSuccess());

        ProgramMetadata metadata = loader.load(result);

        assertEquals(100, memory.readWord(2));
        assertEquals(200, memory.readWord(4));
        assertEquals(-1, memory.readWord(6));

        assertTrue(metadata.isData(2));
        assertTrue(metadata.isData(4));
        assertTrue(metadata.isData(6));
    }

    // ========== Metadata Tests ==========

    @Test
    public void testMetadataEntryPoint() {
        String source = "add r1, r2, r3";
        AssemblyResult result = Assembler.assemble(source);

        ProgramMetadata metadata = loader.load(result);

        // RiSC-16 always starts at address 0
        assertEquals(0, metadata.getEntryPoint());
    }

    @Test
    public void testMetadataInstructionAddresses() {
        String source =
            "add r1, r2, r3\n" +  // 0
            "nand r4, r5, r6\n" + // 2
            "lui r7, 100";        // 4

        AssemblyResult result = Assembler.assemble(source);
        ProgramMetadata metadata = loader.load(result);

        assertEquals(3, metadata.getInstructionAddresses().size());
        assertTrue(metadata.getInstructionAddresses().contains(0));
        assertTrue(metadata.getInstructionAddresses().contains(2));
        assertTrue(metadata.getInstructionAddresses().contains(4));
    }

    @Test
    public void testMetadataDataAddresses() {
        String source =
            "       jalr r0, r0\n" +
            "d1:    .fill 1\n" +
            "d2:    .fill 2";

        AssemblyResult result = Assembler.assemble(source);
        ProgramMetadata metadata = loader.load(result);

        assertEquals(2, metadata.getDataAddresses().size());
        assertTrue(metadata.getDataAddresses().contains(2));
        assertTrue(metadata.getDataAddresses().contains(4));
    }

    // ========== Individual Load Methods ==========

    @Test
    public void testLoadSingleInstruction() {
        ProgramMetadata metadata = new ProgramMetadata(0);
        InstructionFormat instr = InstructionFormat.createRRR(Opcode.ADD, 1, 2, 3, 0x100);

        loader.loadInstruction(instr, metadata);

        short expected = InstructionEncoder.encode(instr);
        assertEquals(expected, memory.readWord(0x100));
        assertTrue(metadata.isInstruction(0x100));
    }

    @Test
    public void testLoadSingleData() {
        ProgramMetadata metadata = new ProgramMetadata(0);

        loader.loadData(0x50, (short) 12345, metadata);

        assertEquals(12345, memory.readWord(0x50));
        assertTrue(metadata.isData(0x50));
    }

    // ========== Error Handling Tests ==========

    @Test(expected = IllegalArgumentException.class)
    public void testLoadNullResult() {
        loader.load(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testLoadFailedAssembly() {
        String source = "invalid instruction here";
        AssemblyResult result = Assembler.assemble(source);
        assertFalse(result.isSuccess());

        loader.load(result);  // Should throw
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorNullMemory() {
        new ProgramLoader(null);
    }

    // ========== Accessor Tests ==========

    @Test
    public void testGetMemory() {
        assertSame(memory, loader.getMemory());
    }

    // ========== Round-Trip Test ==========

    @Test
    public void testLoadAndDecode() {
        String source =
            "add r1, r2, r3\n" +
            "addi r4, r5, -10\n" +
            "lw r6, r7, 5";

        AssemblyResult asmResult = Assembler.assemble(source);
        assertTrue(asmResult.isSuccess());

        loader.load(asmResult);

        // Read back and decode each instruction
        for (int i = 0; i < asmResult.getInstructionCount(); i++) {
            int address = i * 2;
            short binary = memory.readWord(address);
            InstructionFormat decoded = InstructionEncoder.decode(binary, address);

            InstructionFormat original = asmResult.getInstructions().get(i);
            assertEquals(original.getOpcode(), decoded.getOpcode());
            assertEquals(original.getRegA(), decoded.getRegA());
        }
    }
}
