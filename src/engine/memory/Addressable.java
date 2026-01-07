package engine.memory;

/**
 * Interface for addressable storage in the memory hierarchy
 * 
 * This interface is implemented by:
 * - Memory (main memory)
 * - DataCache (cache levels)
 * 
 * It allows caches to be chained together and to write through to memory
 * without knowing whether they're writing to another cache or main memory
 */
public interface Addressable {

    /**
     * Reads bytes from this storage level
     * 
     * @param address the byte address to read from
     * @param length the number of bytes to read
     * 
     * @return a byte array containing the data
     * 
     * @throws IllegalArgumentException if address is invalid
     */
    byte[] getData(int address, int length);
    
    /**
     * Writes bytes to this storage level
     * 
     * @param address the byte address to write to
     * @param data the bytes to write
     * 
     * @throws IllegalArgumentException if address is invalid
     */
    void setData(int address, byte[] data);
}