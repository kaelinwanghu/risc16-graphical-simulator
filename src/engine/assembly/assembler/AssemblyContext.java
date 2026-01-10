package engine.assembly.assembler;

import engine.assembly.AssemblyResult;
import engine.assembly.SymbolTable;
import engine.isa.InstructionFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * Assembly context - holds all state during the assembly process
 * 
 * This replaces static variables, making the assembler:
 * - Thread-safe (each assembly gets fresh context)
 * - Testable (state can be inspected)
 * - Reusable (no cleanup needed between assemblies)
 * 
 * Contains:
 * - currentAddress - tracks memory location for assembly
 * - symbolTable - maps labels to addresses
 * - instructions - assembled instructions (output)
 * - dataSegments - data from .fill/.space (output)
 * - unresolvedReferences - symbolic labels to resolve in Pass 2
 */
public final class AssemblyContext {
    private int currentAddress;
    private final SymbolTable symbolTable;
    private final List<InstructionFormat> instructions;
    private final List<AssemblyResult.DataSegment> dataSegments;
    private final List<UnresolvedReference> unresolvedReferences;
    
    /**
     * Creates a new assembly context starting at address 0
     */
    public AssemblyContext() {
        this.currentAddress = 0;
        this.symbolTable = new SymbolTable();
        this.instructions = new ArrayList<>();
        this.dataSegments = new ArrayList<>();
        this.unresolvedReferences = new ArrayList<>();
    }
    
    // Address management
    
    public int getCurrentAddress() {
        return currentAddress;
    }
    
    public void incrementAddress(int bytes) {
        currentAddress += bytes;
    }
    
    // Symbol table
    
    public SymbolTable getSymbolTable() {
        return symbolTable;
    }
    
    // Instructions
    
    public void addInstruction(InstructionFormat instruction) {
        instructions.add(instruction);
        incrementAddress(2);  // Each instruction is 2 bytes
    }
    
    public List<InstructionFormat> getInstructions() {
        return instructions;
    }
    
    public InstructionFormat getInstruction(int index) {
        return instructions.get(index);
    }
    
    public void replaceInstruction(int index, InstructionFormat instruction) {
        instructions.set(index, instruction);
    }
    
    // Data segments
    
    public void addDataSegment(int address, short value) {
        dataSegments.add(new AssemblyResult.DataSegment(address, value));
        incrementAddress(2);  // Each word is 2 bytes
    }
    
    public List<AssemblyResult.DataSegment> getDataSegments() {
        return dataSegments;
    }
    
    public AssemblyResult.DataSegment getDataSegment(int index) {
        return dataSegments.get(index);
    }
    
    public void replaceDataSegment(int index, short value) {
        AssemblyResult.DataSegment old = dataSegments.get(index);
        dataSegments.set(index, new AssemblyResult.DataSegment(old.getAddress(), value));
    }
    
    // Unresolved references
    
    public void addUnresolvedReference(UnresolvedReference ref) {
        unresolvedReferences.add(ref);
    }
    
    public List<UnresolvedReference> getUnresolvedReferences() {
        return unresolvedReferences;
    }
}

/**
 * Represents a symbolic label reference that needs to be resolved in Pass 2.
 * 
 * There are three types:
 * 1. BRANCH - Branch instruction (BEQ), offset relative to PC+1
 * 2. LOAD_STORE - Load/store instruction (LW/SW), offset relative to PC
 * 3. FILL - Data directive (.fill), absolute address
 */
class UnresolvedReference {
    enum Type {
        BRANCH,      // BEQ: offset = target - (PC + 2)
        LOAD_STORE,  // LW/SW: offset = target - PC
        FILL         // .fill: value = target address
    }
    
    private final Type type;
    private final String label;
    private final int instructionIndex;  // Index in instructions list (-1 for data)
    private final int dataIndex;         // Index in dataSegments list (-1 for instructions)
    private final int currentAddress;    // PC value for relative calculations
    private final int lineNumber;        // For error reporting
    private final String originalLine;   // For error reporting
    
    /**
     * Creates an unresolved reference for an instruction
     */
    public static UnresolvedReference instruction(Type type, String label, int instructionIndex, int currentAddress, int lineNumber, String originalLine) {
        return new UnresolvedReference(type, label, instructionIndex, -1, currentAddress, lineNumber, originalLine);
    }
    
    /**
     * Creates an unresolved reference for a data segment (.fill)
     */
    public static UnresolvedReference data(String label, int dataIndex, int lineNumber, String originalLine) {
        return new UnresolvedReference(Type.FILL, label, -1, dataIndex, -1, lineNumber, originalLine);
    }
    
    private UnresolvedReference(Type type, String label, int instructionIndex, int dataIndex, int currentAddress, int lineNumber, String originalLine) {
        this.type = type;
        this.label = label;
        this.instructionIndex = instructionIndex;
        this.dataIndex = dataIndex;
        this.currentAddress = currentAddress;
        this.lineNumber = lineNumber;
        this.originalLine = originalLine;
    }
    
    public Type getType() {
        return type;
    }
    
    public String getLabel() {
        return label;
    }
    
    public int getInstructionIndex() {
        return instructionIndex;
    }
    
    public int getDataIndex() {
        return dataIndex;
    }
    
    public int getCurrentAddress() {
        return currentAddress;
    }
    
    public int getLineNumber() {
        return lineNumber;
    }
    
    public String getOriginalLine() {
        return originalLine;
    }
}