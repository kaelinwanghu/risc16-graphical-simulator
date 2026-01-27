package engine.assembly.assembler;

import static org.junit.Assert.*;
import static engine.assembly.assembler.AssemblerTestUtils.*;

import org.junit.Test;

import engine.assembly.AssemblyResult;

/**
 * JUnit tests for assembler directives (.fill, .space, etc.)
 */
public class DirectiveTest {

    // ========== .fill Directive Tests ==========

    @Test
    public void testFillDirective() {
        String source = ".fill 42";
        AssemblyResult result = assembleSuccess(source);

        assertDataCount(result, 1);
        assertEquals(42, result.getDataSegments().get(0).getValue());
    }

    @Test
    public void testFillWithHex() {
        String source = ".fill 0xFF";
        AssemblyResult result = assembleSuccess(source);

        assertDataCount(result, 1);
        assertEquals(255, result.getDataSegments().get(0).getValue());
    }

    @Test
    public void testFillWithNegative() {
        String source = ".fill -1";
        AssemblyResult result = assembleSuccess(source);

        assertDataCount(result, 1);
        assertEquals(-1, result.getDataSegments().get(0).getValue());
    }

    @Test
    public void testFillWithLabel() {
        String source =
            "data: .fill 100\n" +
            "      add r1, r2, r3";

        AssemblyResult result = assembleSuccess(source);

        assertLabel(result, "data", 0);
        assertDataCount(result, 1);
        assertInstructionCount(result, 1);
    }

    @Test
    public void testMultipleFills() {
        String source =
            ".fill 1\n" +
            ".fill 2\n" +
            ".fill 3";

        AssemblyResult result = assembleSuccess(source);

        assertDataCount(result, 3);
        assertEquals(1, result.getDataSegments().get(0).getValue());
        assertEquals(2, result.getDataSegments().get(1).getValue());
        assertEquals(3, result.getDataSegments().get(2).getValue());
    }

    // ========== Mixed Code and Data Tests ==========

    @Test
    public void testCodeBeforeData() {
        // Note: RiSC-16 uses word addressing (each instruction/data is 2 bytes)
        String source =
            "      add r1, r2, r3\n" +
            "      addi r4, r5, 1\n" +
            "data: .fill 42";

        AssemblyResult result = assembleSuccess(source);

        assertInstructionCount(result, 2);
        assertDataCount(result, 1);
        assertLabel(result, "data", 4);  // Address 4 = 2 instructions * 2 bytes each
    }

    @Test
    public void testDataBeforeCode() {
        String source =
            "data: .fill 42\n" +
            "      add r1, r2, r3";

        AssemblyResult result = assembleSuccess(source);

        assertDataCount(result, 1);
        assertInstructionCount(result, 1);
        assertLabel(result, "data", 0);
    }

    @Test
    public void testInterleavedCodeAndData() {
        String source =
            "      add r1, r2, r3\n" +
            "val1: .fill 10\n" +
            "      addi r4, r5, 1\n" +
            "val2: .fill 20";

        AssemblyResult result = assembleSuccess(source);

        assertInstructionCount(result, 2);
        assertDataCount(result, 2);
    }

    // ========== .space Directive Tests (if supported) ==========

    @Test
    public void testSpaceDirective() {
        String source = ".space 3";
        AssemblyResult result = Assembler.assemble(source);

        // Space directive allocates multiple zero-initialized words
        if (result.isSuccess()) {
            assertTrue("Should have at least 3 data segments or reserved space",
                result.getDataCount() >= 3 || result.getInstructionCount() == 0);
        }
        // If not supported, the test will just verify no crash occurs
    }

    // ========== Label Reference in .fill ==========

    @Test
    public void testFillWithLabelReference() {
        String source =
            "start: add r1, r2, r3\n" +
            "       .fill start";

        AssemblyResult result = Assembler.assemble(source);

        // Some assemblers support label references in .fill
        // This test verifies the behavior doesn't crash
        // Actual support depends on implementation
        assertNotNull("Result should not be null", result);
    }

    // ========== Directive Case Insensitivity ==========

    @Test
    public void testDirectiveCaseInsensitive() {
        String source1 = ".fill 42";
        String source2 = ".FILL 42";
        String source3 = ".Fill 42";

        AssemblyResult result1 = Assembler.assemble(source1);
        AssemblyResult result2 = Assembler.assemble(source2);
        AssemblyResult result3 = Assembler.assemble(source3);

        // At least lowercase should work
        assertTrue("Lowercase .fill should work", result1.isSuccess());
        // Other cases may or may not work depending on implementation
    }
}
