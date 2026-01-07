package engine.memory;

import java.util.Arrays;

/**
 * Raw byte storage for the RiSC-16 processor
 * 
 * This class represents the physical memory of the machine - it knows nothing
 * about instructions vs. data. It simply stores and retrieves bytes at addresses
 * 
 * Memory is byte-addressable, but word-aligned accesses (address % 2 == 0) are
 * more efficient. The RiSC-16 uses 16-bit words (2 bytes), so most accesses
 * should be word-aligned.
 */
public class Memory implements Addressable {
    private final byte[] storage;
    private final int size;
    private static final int MAX_SIZE = 4 * 1024 * 1024; // 4MB max size
    
    /**
     * Creates a new memory of the specified size
     * 
     * @param size the size in bytes (must be a power of 2 between 128 and 4MB)
     * 
     * @throws IllegalArgumentException if size is invalid
     */
    public Memory(int size) {
        if (size < 128 || size > MAX_SIZE) {
            throw new IllegalArgumentException(
                "Memory size must be between 128 bytes and 4MB, got: " + size);
        }
        
        if (!isPowerOfTwo(size)) {
            throw new IllegalArgumentException("Memory size must be a power of 2, got: " + size);
        }
        
        this.size = size;
        this.storage = new byte[size];
    }
    
    /**
     * Gets the total size of memory in bytes
     */
    public int getSize() {
        return size;
    }
    
    /**
     * Reads a single byte from memory
     * 
     * @param address the byte address (0 to size-1)
     * 
     * @return the byte at that address
     * 
     * @throws IllegalArgumentException if address is out of bounds
     */
    public byte readByte(int address) {
        validateAddress(address);
        return storage[address];
    }
    
    /**
     * Writes a single byte to memory
     * 
     * @param address the byte address (0 to size-1)
     * @param value the byte value to 
     * 
     * @throws IllegalArgumentException if address is out of bounds
     */
    public void writeByte(int address, byte value) {
        validateAddress(address);
        storage[address] = value;
    }
    
    /**
     * Reads a 16-bit word from memory (big-endian)
     * 
     * @param address the word-aligned address
     * 
     * @return the 16-bit word as a short
     * 
     * @throws IllegalArgumentException if address is out of bounds or not word-aligned
     */
    public short readWord(int address) {
        validateWordAddress(address);
        
        // Big-endian
        int highByte = storage[address] & 0xFF;
        int lowByte = storage[address + 1] & 0xFF;
        
        return (short) ((highByte << 8) | lowByte);
    }
    
    /**
     * Writes a 16-bit word to memory (big-endian)
     * 
     * @param address the word-aligend address
     * @param value the 16-bit word to write
     * @throws IllegalArgumentException if address is out of bounds or not word-aligned
     */
    public void writeWord(int address, short value) {
        validateWordAddress(address);
        
        // Big-endian: high byte first
        storage[address] = (byte) ((value >> 8) & 0xFF);
        storage[address + 1] = (byte) (value & 0xFF);
    }
    
    /**
     * Reads multiple bytes from memory
     * 
     * @param address the starting byte address
     * @param length the number of bytes to read
     * 
     * @return a new byte array containing the data
     * 
     * @throws IllegalArgumentException if the range is out of bounds
     */
    public byte[] readBytes(int address, int length) {
        if (length < 0) {
            throw new IllegalArgumentException("Length cannot be negative: " + length);
        }
        validateAddress(address);
        validateAddress(address + length - 1);
        
        byte[] result = new byte[length];
        System.arraycopy(storage, address, result, 0, length);
        return result;
    }
    
    /**
     * Writes multiple bytes to memory
     * 
     * @param address the starting byte address
     * @param data the bytes to write
     * 
     * @throws IllegalArgumentException if the range is out of bounds
     */
    public void writeBytes(int address, byte[] data) {
        if (data == null) {
            throw new IllegalArgumentException("Data cannot be null");
        }
        validateAddress(address);
        validateAddress(address + data.length - 1);
        
        System.arraycopy(data, 0, storage, address, data.length);
    }
    
    /**
     * Clears all memory to zero
     */
    public void clear() {
        Arrays.fill(storage, (byte) 0);
    }
    
    /**
     * Checks if an address is valid (in bounds)
     */
    public boolean isValidAddress(int address) {
        return address >= 0 && address < size;
    }
    
    public boolean isWordAligned(int address) {
        return (address & 1) == 0;
    }
    
    public int getMaxAddress() {
        return size - 1;
    }
    
    /**
     * Gets the maximum valid word address
     */
    public int getMaxWordAddress() {
        return size - 2;
    }
    
    /**
     * Creates a snapshot of a memory region for debugging
     * 
     * @param startAddress the starting address
     * @param length the number of bytes to include
     * 
     * @return a hex dump string
     */
    public String dumpHex(int startAddress, int length) {
        if (length <= 0) {
            return "Invalid length: must be positive";
        }
        if (!isValidAddress(startAddress) || !isValidAddress(startAddress + length - 1)) {
            return "Invalid address range";
        }
        
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            if (i % 16 == 0) {
                if (i > 0) sb.append("\n");
                sb.append(String.format("%04X: ", startAddress + i));
            }
            sb.append(String.format("%02X ", storage[startAddress + i] & 0xFF));
        }
        return sb.toString();
    }
    
    // Validation helpers
    
    private void validateAddress(int address) {
        if (address < 0 || address >= size) {
            throw new IllegalArgumentException(String.format("Address 0x%X is out of bounds [0x0000, 0x%04X]",  address, size - 1));
        }
    }
    
    private void validateWordAddress(int address) {
        if ((address & 1) != 0) {
            throw new IllegalArgumentException(String.format("Word address 0x%X must be even", address));
        }
        validateAddress(address);
        validateAddress(address + 1);
    }
    
    private static boolean isPowerOfTwo(int n) {
        return n > 0 && (n & (n - 1)) == 0;
    }
    
    // Addressable interface implementation
    
    /**
     * Reads bytes from memory (Addressable interface)
     * 
     * @param address the starting byte address
     * @param length the number of bytes to read
     * 
     * @return a byte array containing the data
     */
    @Override
    public byte[] getData(int address, int length) {
        return readBytes(address, length);
    }
    
    /**
     * Writes bytes to memory (Addressable interface)
     * 
     * @param address the starting byte address
     * @param data the bytes to write
     */
    @Override
    public void setData(int address, byte[] data) {
        writeBytes(address, data);
    }
    
    @Override
    public String toString() {
        return String.format("Memory[%d bytes (0x%X)]", size, size);
    }
}