package engine.assembly;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Symbol table for managing labels and their addresses
 * 
 * During assembly, labels are defined and then resolved when referenced
 * This class maintains the mapping between label names and their addresses
 * 
 * Labels are case-sensitive and must be unique within a program
 */
public class SymbolTable {
    private final Map<String, Integer> labels;
    
    public SymbolTable() {
        this.labels = new HashMap<>();
    }
    
    /**
     * Defines a label at a specific address
     * 
     * @param label the label name
     * @param address the address this label refers to
     * 
     * @throws IllegalArgumentException if label is already defined
     */
    public void define(String label, int address) {
        if (label == null || label.isEmpty()) {
            throw new IllegalArgumentException("Label cannot be null or empty");
        }
        
        if (labels.containsKey(label)) {
            throw new IllegalArgumentException(String.format("Label '%s' is already defined at address 0x%04X", label, labels.get(label)));
        }
        
        labels.put(label, address);
    }
    
    /**
     * Resolves a label to its address
     * 
     * @param label the label name to resolve
     * 
     * @return the address this label refers to
     * 
     * @throws IllegalArgumentException if label is not defined
     */
    public int resolve(String label) {
        if (!contains(label)) {
            throw new IllegalArgumentException("Undefined label: '" + label + "'");
        }

        return labels.get(label);
    }
    
    /**
     * Checks if a label is defined
     * 
     * @param label the label name to check
     * 
     * @return true if the label exists
     */
    public boolean contains(String label) {
        return labels.containsKey(label);
    }
    
    /**
     * Gets all labels and their addresses (immutable view)
     */
    public Map<String, Integer> getAll() {
        return Collections.unmodifiableMap(labels);
    }
    
    /**
     * Gets all label names (immutable view)
     */
    public Set<String> getLabelNames() {
        return Collections.unmodifiableSet(labels.keySet());
    }
    
    public int size() {
        return labels.size();
    }
    
    public boolean isEmpty() {
        return labels.isEmpty();
    }
    
    /**
     * Clears all labels
     */
    public void clear() {
        labels.clear();
    }
    
    /**
     * Finds the label at a specific address, or null if none
     * 
     * @param address the address to look up
     * 
     * @return the label at that address, or null
     */
    public String getLabelAt(int address) {
        for (Map.Entry<String, Integer> entry : labels.entrySet()) {
            if (entry.getValue() == address) {
                return entry.getKey();
            }
        }
        return null;
    }
    
    /**
     * Creates a formatted listing of all labels
     */
    public String toFormattedString() {
        if (isEmpty()) {
            return "No labels defined";
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("Symbol Table:%n");
        sb.append("-------------%n");
        
        // Sort by address for cleaner output
        labels.entrySet().stream().sorted(Map.Entry.comparingByValue())
            .forEach(entry -> sb.append(String.format("%-20s 0x%04X (%d)%n", entry.getKey(), entry.getValue(), entry.getValue())));
        
        return sb.toString();
    }
    
    @Override
    public String toString() {
        return String.format("SymbolTable[%d labels]", labels.size());
    }
}