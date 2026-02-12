package engine.assembly.assembler;

import static org.junit.Assert.*;
import org.junit.Test;

/**
 * Tests for the NumberParser utility class
 *
 * NumberParser handles parsing of decimal, hexadecimal, and octal numbers.
 */
public class NumberParserTest {

    // ========== Decimal Parsing Tests ==========

    @Test
    public void testParseDecimal() {
        assertEquals(Integer.valueOf(42), NumberParser.parse("42"));
        assertEquals(Integer.valueOf(0), NumberParser.parse("0"));
        assertEquals(Integer.valueOf(100), NumberParser.parse("100"));
    }

    @Test
    public void testParseNegativeDecimal() {
        assertEquals(Integer.valueOf(-1), NumberParser.parse("-1"));
        assertEquals(Integer.valueOf(-42), NumberParser.parse("-42"));
        assertEquals(Integer.valueOf(-100), NumberParser.parse("-100"));
    }

    @Test
    public void testParseZero() {
        assertEquals(Integer.valueOf(0), NumberParser.parse("0"));
    }

    // ========== Hexadecimal Parsing Tests ==========

    @Test
    public void testParseHexLowercase() {
        assertEquals(Integer.valueOf(255), NumberParser.parse("0xff"));
        assertEquals(Integer.valueOf(16), NumberParser.parse("0x10"));
        assertEquals(Integer.valueOf(0), NumberParser.parse("0x0"));
    }

    @Test
    public void testParseHexUppercase() {
        assertEquals(Integer.valueOf(255), NumberParser.parse("0xFF"));
        assertEquals(Integer.valueOf(171), NumberParser.parse("0xAB"));
    }

    @Test
    public void testParseHexMixedCase() {
        assertEquals(Integer.valueOf(0xAbCd), NumberParser.parse("0xAbCd"));
    }

    @Test
    public void testParseHexWithUpperX() {
        assertEquals(Integer.valueOf(255), NumberParser.parse("0XFF"));
        assertEquals(Integer.valueOf(16), NumberParser.parse("0X10"));
    }

    // ========== Octal Parsing Tests ==========

    @Test
    public void testParseOctal() {
        assertEquals(Integer.valueOf(8), NumberParser.parse("010"));   // Octal 10 = 8
        assertEquals(Integer.valueOf(63), NumberParser.parse("077"));  // Octal 77 = 63
        assertEquals(Integer.valueOf(1), NumberParser.parse("01"));
    }

    // ========== Non-Number Tests ==========

    @Test
    public void testParseLabel() {
        assertNull(NumberParser.parse("label"));
        assertNull(NumberParser.parse("main"));
        assertNull(NumberParser.parse("loop"));
    }

    @Test
    public void testParseRegister() {
        assertNull(NumberParser.parse("r0"));
        assertNull(NumberParser.parse("r7"));
    }

    @Test
    public void testParseNull() {
        assertNull(NumberParser.parse(null));
    }

    @Test
    public void testParseEmpty() {
        assertNull(NumberParser.parse(""));
    }

    @Test
    public void testParseMixedAlphanumeric() {
        assertNull(NumberParser.parse("123abc"));
        assertNull(NumberParser.parse("abc123"));
    }

    // ========== isNumber Tests ==========

    @Test
    public void testIsNumberTrue() {
        assertTrue(NumberParser.isNumber("42"));
        assertTrue(NumberParser.isNumber("-10"));
        assertTrue(NumberParser.isNumber("0xFF"));
        assertTrue(NumberParser.isNumber("010"));
    }

    @Test
    public void testIsNumberFalse() {
        assertFalse(NumberParser.isNumber("label"));
        assertFalse(NumberParser.isNumber("r0"));
        assertFalse(NumberParser.isNumber(""));
        assertFalse(NumberParser.isNumber(null));
    }

    // ========== Edge Cases ==========

    @Test
    public void testParseLargeNumber() {
        assertEquals(Integer.valueOf(65535), NumberParser.parse("65535"));
        assertEquals(Integer.valueOf(0xFFFF), NumberParser.parse("0xFFFF"));
    }

    @Test
    public void testParseInvalidHex() {
        assertNull(NumberParser.parse("0xGG"));  // G is not valid hex
    }
}
