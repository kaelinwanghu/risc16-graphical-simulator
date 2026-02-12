package engine.assembly.assembler;

import static org.junit.Assert.*;
import org.junit.Test;
import org.junit.Before;

import engine.assembly.AssemblyResult.DataSegment;
import engine.isa.InstructionFormat;
import engine.isa.Opcode;

/**
 * Tests for the AssemblyContext class
 *
 * AssemblyContext holds all state during assembly including current address,
 * symbol table, instructions, data segments, and unresolved references.
 */
public class AssemblyContextTest {

    private AssemblyContext context;

    @Before
    public void setUp() {
        context = new AssemblyContext();
    }

    // ========== Initial State Tests ==========

    @Test
    public void testInitialState() {
        assertEquals(0, context.getCurrentAddress());
        assertNotNull(context.getSymbolTable());
        assertTrue(context.getSymbolTable().isEmpty());
        assertTrue(context.getInstructions().isEmpty());
        assertTrue(context.getDataSegments().isEmpty());
        assertTrue(context.getUnresolvedReferences().isEmpty());
    }

    // ========== Address Management Tests ==========

    @Test
    public void testIncrementAddress() {
        assertEquals(0, context.getCurrentAddress());

        context.incrementAddress(2);
        assertEquals(2, context.getCurrentAddress());

        context.incrementAddress(4);
        assertEquals(6, context.getCurrentAddress());
    }

    // ========== Instruction Tests ==========

    @Test
    public void testAddInstruction() {
        InstructionFormat instr = InstructionFormat.createRRR(Opcode.ADD, 1, 2, 3, 0);

        context.addInstruction(instr);

        assertEquals(1, context.getInstructions().size());
        assertEquals(2, context.getCurrentAddress());  // Address incremented by 2
    }

    @Test
    public void testAddMultipleInstructions() {
        InstructionFormat add = InstructionFormat.createRRR(Opcode.ADD, 1, 2, 3, 0);
        InstructionFormat nand = InstructionFormat.createRRR(Opcode.NAND, 4, 5, 6, 2);

        context.addInstruction(add);
        context.addInstruction(nand);

        assertEquals(2, context.getInstructions().size());
        assertEquals(4, context.getCurrentAddress());
    }

    @Test
    public void testGetInstruction() {
        InstructionFormat instr = InstructionFormat.createRRR(Opcode.ADD, 1, 2, 3, 0);
        context.addInstruction(instr);

        assertEquals(Opcode.ADD, context.getInstruction(0).getOpcode());
    }

    @Test
    public void testReplaceInstruction() {
        InstructionFormat old = InstructionFormat.createRRR(Opcode.ADD, 1, 2, 3, 0);
        InstructionFormat replacement = InstructionFormat.createRRR(Opcode.NAND, 4, 5, 6, 0);

        context.addInstruction(old);
        context.replaceInstruction(0, replacement);

        assertEquals(Opcode.NAND, context.getInstruction(0).getOpcode());
    }

    // ========== Data Segment Tests ==========

    @Test
    public void testAddDataSegment() {
        context.addDataSegment(0, (short) 42);

        assertEquals(1, context.getDataSegments().size());
        assertEquals(2, context.getCurrentAddress());  // Address incremented by 2
    }

    @Test
    public void testAddMultipleDataSegments() {
        context.addDataSegment(0, (short) 1);
        context.addDataSegment(2, (short) 2);
        context.addDataSegment(4, (short) 3);

        assertEquals(3, context.getDataSegments().size());
        assertEquals(6, context.getCurrentAddress());
    }

    @Test
    public void testGetDataSegment() {
        context.addDataSegment(0, (short) 100);

        DataSegment segment = context.getDataSegment(0);
        assertEquals(100, segment.getValue());
    }

    @Test
    public void testReplaceDataSegment() {
        context.addDataSegment(10, (short) 50);
        context.replaceDataSegment(0, (short) 999);

        DataSegment segment = context.getDataSegment(0);
        assertEquals(999, segment.getValue());
        assertEquals(10, segment.getAddress());  // Address preserved
    }

    // ========== Symbol Table Tests ==========

    @Test
    public void testSymbolTableAccess() {
        context.getSymbolTable().define("test", 100);

        assertTrue(context.getSymbolTable().contains("test"));
        assertEquals(100, context.getSymbolTable().resolve("test"));
    }

    // ========== Unresolved Reference Tests ==========

    @Test
    public void testAddUnresolvedReference() {
        UnresolvedReference ref = UnresolvedReference.instruction(
            UnresolvedReference.Type.BRANCH, "label", 0, 0, 1, "beq r0, r0, label");

        context.addUnresolvedReference(ref);

        assertEquals(1, context.getUnresolvedReferences().size());
        assertEquals("label", context.getUnresolvedReferences().get(0).getLabel());
    }

    @Test
    public void testMultipleUnresolvedReferences() {
        context.addUnresolvedReference(UnresolvedReference.instruction(
            UnresolvedReference.Type.BRANCH, "label1", 0, 0, 1, ""));
        context.addUnresolvedReference(UnresolvedReference.instruction(
            UnresolvedReference.Type.LOAD_STORE, "label2", 1, 2, 2, ""));
        context.addUnresolvedReference(UnresolvedReference.data(
            "label3", 0, 3, ""));

        assertEquals(3, context.getUnresolvedReferences().size());
    }

    // ========== Mixed Operations Test ==========

    @Test
    public void testMixedInstructionsAndData() {
        InstructionFormat instr1 = InstructionFormat.createRRR(Opcode.ADD, 1, 2, 3, 0);
        context.addInstruction(instr1);  // Address 0-1, now at 2

        context.addDataSegment(context.getCurrentAddress(), (short) 42);  // Address 2-3, now at 4

        InstructionFormat instr2 = InstructionFormat.createRRI(Opcode.ADDI, 1, 2, 5, 4);
        context.addInstruction(instr2);  // Address 4-5, now at 6

        assertEquals(2, context.getInstructions().size());
        assertEquals(1, context.getDataSegments().size());
        assertEquals(6, context.getCurrentAddress());
    }
}
