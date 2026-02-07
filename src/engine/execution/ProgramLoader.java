package engine.execution;

import engine.assembly.AssemblyResult;
import engine.isa.InstructionEncoder;
import engine.isa.InstructionFormat;
import engine.memory.Memory;
import engine.metadata.ProgramMetadata;

/**
 * Loads assembled programs into memory and generates metadata.
 * 
 * This class takes an AssemblyResult (from the assembler) and:
 * 1. Writes instructions to memory as binary
 * 2. Writes data values to memory
 * 3. Creates ProgramMetadata describing the loaded program
 * 
 * It acts as the bridge between stateless assembly and execution, converting the
 * assembled program into the memory and metadata representation needed for execution.
 */
public class ProgramLoader {
    private final Memory memory;
    
    /**
     * Creates a program loader for the given memory
     * 
     * @param memory the memory to load programs into
     */
    public ProgramLoader(Memory memory) {
        if (memory == null) {
            throw new IllegalArgumentException("Memory cannot be null");
        }
        this.memory = memory;
    }
    
    /**
     * Loads an assembled program into memory
     * 
     * @param result the assembly result to load
     * 
     * @return metadata describing the loaded program
     * 
     * @throws IllegalArgumentException if program is too large or has errors
     */
    public ProgramMetadata load(AssemblyResult result) {
        if (result == null) {
            throw new IllegalArgumentException("AssemblyResult cannot be null");
        }
        
        if (!result.isSuccess()) {
            throw new IllegalArgumentException("Cannot load program with assembly errors: " + result.getErrors().get(0).getCompactMessage());
        }
        
        // Clear memory before loading
        memory.clear();
        
        // Build metadata while loading
        ProgramMetadata.Builder metadataBuilder = ProgramMetadata.builder().entryPoint(0); // Entry point is always address 0 for RiSC-16
        
        // Load instructions, encode, then write to memory
        for (InstructionFormat instruction : result.getInstructions()) {
            int address = instruction.getAddress();
            
            short binary = InstructionEncoder.encode(instruction);
            memory.writeWord(address, binary);
            
            // Mark as instruction in metadata
            metadataBuilder.markInstruction(address);
            metadataBuilder.setSourceLine(address, instruction.getSourceLine());
        }
        
        // Load data segments, then write to memory
        for (AssemblyResult.DataSegment segment : result.getDataSegments()) {
            int address = segment.getAddress();
            short value = segment.getValue();
            
            memory.writeWord(address, value);
            
            // Mark as data in metadata
            metadataBuilder.markData(address);
        }
        
        // Add all labels to metadata
        for (String label : result.getSymbolTable().getAll().keySet()) {
            int address = result.getSymbolTable().resolve(label);
            metadataBuilder.addLabel(label, address);
        }
        
        return metadataBuilder.build();
    }
    
    /**
     * Loads a single instruction at a specific address (useful for debugger)
     * 
     * @param instruction the instruction to load
     * @param metadata the metadata to update (marks address as instruction)
     */
    public void loadInstruction(InstructionFormat instruction, ProgramMetadata metadata) {
        int address = instruction.getAddress();
        short binary = InstructionEncoder.encode(instruction);
        memory.writeWord(address, binary);
        metadata.markInstruction(address);
    }
    
    /**
     * Loads a data word at a specific address (useful for debugger)
     * 
     * @param address the word-aligned address to write to
     * @param value the 16-bit value to write
     * @param metadata the metadata to update (marks address as data)
     */
    public void loadData(int address, short value, ProgramMetadata metadata) {
        memory.writeWord(address, value);
        metadata.markData(address);
    }
    
    /**
     * Gets the underlying memory
     */
    public Memory getMemory() {
        return memory;
    }
}