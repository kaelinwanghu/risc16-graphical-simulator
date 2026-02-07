package engine.metadata;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Metadata about a loaded program - tracks which memory addresses contain
 * instructions, data, labels, and entry point.
 * 
 * Essential for later features such as
 * - Disassembly (knowing what to decode as instructions)
 * - Debugging (showing labels, distinguishing code from data)
 * - Warnings (detecting data in code section, or vice versa)
 * - Visualization (color-coding instructions vs. data in GUI)
 */
public class ProgramMetadata {
    private final Set<Integer> instructionAddresses;
    private final Set<Integer> dataAddresses;
    private final Map<Integer, String> addressToLabel;
    private final Map<String, Integer> labelToAddress;
    private final int entryPoint;
    private final int lastInstructionAddress;
    private final Map<Integer, Integer> addressToLineNumber;

    /**
     * Creates empty program metadata with a specified entry point
     * 
     * @param entryPoint the address where execution begins (typically 0)
     */
    public ProgramMetadata(int entryPoint) {
        this.instructionAddresses = new HashSet<>();
        this.dataAddresses = new HashSet<>();
        this.addressToLabel = new HashMap<>();
        this.labelToAddress = new HashMap<>();
        this.entryPoint = entryPoint;
        this.lastInstructionAddress = -1;
        this.addressToLineNumber = new HashMap<>();
    }

    /**
     * Creates program metadata using a builder
     */
    private ProgramMetadata(Builder builder) {
        this.instructionAddresses = new HashSet<>(builder.instructionAddresses);
        this.dataAddresses = new HashSet<>(builder.dataAddresses);
        this.addressToLabel = new HashMap<>(builder.addressToLabel);
        this.labelToAddress = new HashMap<>(builder.labelToAddress);
        this.entryPoint = builder.entryPoint;
        this.lastInstructionAddress = builder.lastInstructionAddress;
        this.addressToLineNumber = new HashMap<>(builder.addressToLineNumber);
    }

    // Query methods

    public boolean isInstruction(int address) {
        return instructionAddresses.contains(address);
    }

    public boolean isData(int address) {
        return dataAddresses.contains(address);
    }

    /**
     * Checks if an address has been marked as either instruction or data
     */
    public boolean isKnown(int address) {
        return isInstruction(address) || isData(address);
    }

    /**
     * Gets the label at an address, or null if none
     */
    public String getLabel(int address) {
        return addressToLabel.get(address);
    }

    public boolean hasLabel(int address) {
        return addressToLabel.containsKey(address);
    }

    /**
     * Gets the address of a label, or null if not found
     */
    public Integer getAddress(String label) {
        return labelToAddress.get(label);
    }

    public boolean hasLabel(String label) {
        return labelToAddress.containsKey(label);
    }

    /**
     * Gets the entry point (start address) of the program
     */
    public int getEntryPoint() {
        return entryPoint;
    }

    /**
     * Gets all instruction addresses (immutable view)
     */
    public Set<Integer> getInstructionAddresses() {
        return Collections.unmodifiableSet(instructionAddresses);
    }

    /**
     * Gets all data addresses (immutable view)
     */
    public Set<Integer> getDataAddresses() {
        return Collections.unmodifiableSet(dataAddresses);
    }

    /**
     * Gets all labels and their addresses (immutable view)
     */
    public Map<String, Integer> getAllLabels() {
        return Collections.unmodifiableMap(labelToAddress);
    }

    public int getInstructionCount() {
        return instructionAddresses.size();
    }

    public int getDataCount() {
        return dataAddresses.size();
    }

    public int getLastInstructionAddress() {
        return lastInstructionAddress;
    }

    /**
     * Finds the next instruction address after the given address
     * 
     * @param address the address to search from
     * 
     * @return the next instruction address, or -1 if none found
     */
    public int getNextInstruction(int address) {
        int nextAddr = address + 2; // Instructions are 2 bytes
        while (nextAddr < 65536) { // Max 16-bit address space
            if (isInstruction(nextAddr)) {
                return nextAddr;
            }
            nextAddr += 2;
        }
        return -1;
    }

    /**
     * Finds the previous instruction address before the given address
     * 
     * @param address the address to search from
     * @return the previous instruction address, or -1 if none found
     */
    public int getPreviousInstruction(int address) {
        int prevAddr = address - 2;
        while (prevAddr >= 0) {
            if (isInstruction(prevAddr)) {
                return prevAddr;
            }
            prevAddr -= 2;
        }
        return -1;
    }

    // Modification methods (public - used by ProgramLoader and debugger)

    /**
     * Marks an address as containing an instruction
     * (Package-visible for ProgramLoader, public for debugger)
     */
    public void markInstruction(int address) {
        instructionAddresses.add(address);
        dataAddresses.remove(address); // Can't be both
    }

    /**
     * Marks an address as containing data
     * (Package-visible for ProgramLoader, public for debugger)
     */
    public void markData(int address) {
        dataAddresses.add(address);
        instructionAddresses.remove(address); // Can't be both
    }

    /**
     * Adds a label at an address
     * (Package-visible for ProgramLoader, public for debugger)
     */
    public void addLabel(String label, int address) {
        addressToLabel.put(address, label);
        labelToAddress.put(label, address);
    }

    /**
     * Gets the source line number for a given address
     * 
     * @param address the memory address
     * @return the source line number (1-based), or -1 if not found
     */
    public int getSourceLine(int address) {
        return addressToLineNumber.getOrDefault(address, -1);
    }

    /**
     * Adds source line mapping for an address
     */
    public void setSourceLine(int address, int lineNumber) {
        addressToLineNumber.put(address, lineNumber);
    }

    // Builder for creating metadata

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final Set<Integer> instructionAddresses = new HashSet<>();
        private final Set<Integer> dataAddresses = new HashSet<>();
        private final Map<Integer, String> addressToLabel = new HashMap<>();
        private final Map<String, Integer> labelToAddress = new HashMap<>();
        private final Map<Integer, Integer> addressToLineNumber = new HashMap<>();
        private int lastInstructionAddress = -1;
        private int entryPoint = 0;

        public Builder entryPoint(int address) {
            this.entryPoint = address;
            return this;
        }

        public Builder markInstruction(int address) {
            instructionAddresses.add(address);
            dataAddresses.remove(address);
            if (address > lastInstructionAddress) {
                lastInstructionAddress = address;
            }
            return this;
        }

        public Builder markData(int address) {
            dataAddresses.add(address);
            instructionAddresses.remove(address);
            return this;
        }

        public Builder addLabel(String label, int address) {
            addressToLabel.put(address, label);
            labelToAddress.put(label, address);
            return this;
        }

        public Builder setSourceLine(int address, int lineNumber) {
            addressToLineNumber.put(address, lineNumber);
            return this;
        }

        public ProgramMetadata build() {
            return new ProgramMetadata(this);
        }
    }

    @Override
    public String toString() {
        return String.format("ProgramMetadata[entry=0x%04X, instructions=%d, data=%d, labels=%d]", entryPoint,
                instructionAddresses.size(), dataAddresses.size(), labelToAddress.size());
    }
}