package engine.assembly;

import engine.isa.InstructionFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Result after assembling a program
 * 
 * This immutable class contains:
 * - The list of assembled instructions
 * - Data segments (.fill directives)
 * - Symbol table (labels and their addresses)
 * - Any errors or warnings encountered
 * 
 * If assembly was successful, errors will be empty and instructions will
 * be populated. If assembly failed, errors will contain at least one error
 * and instructions may be partial or empty.
 */
public class AssemblyResult {
    private final List<InstructionFormat> instructions;
    private final List<DataSegment> dataSegments;
    private final SymbolTable symbolTable;
    private final List<AssemblyError> errors;
    private final List<AssemblyWarning> warnings;
    
    /**
     * Represents a data segment (from .fill or .space directives)
     */
    public static class DataSegment {
        private final int address;
        private final short value;
        
        public DataSegment(int address, short value) {
            this.address = address;
            this.value = value;
        }
        
        public int getAddress() {
            return address;
        }
        
        public short getValue() {
            return value;
        }
        
        @Override
        public String toString() {
            return String.format("Data[0x%04X] = 0x%04X", address, value & 0xFFFF);
        }
    }
    
    /**
     * Represents a warning (non-fatal issue)
     */
    public static class AssemblyWarning {
        private final int lineNumber;
        private final String message;
        
        public AssemblyWarning(int lineNumber, String message) {
            this.lineNumber = lineNumber;
            this.message = message;
        }
        
        public int getLineNumber() {
            return lineNumber;
        }
        
        public String getMessage() {
            return message;
        }
        
        @Override
        public String toString() {
            return "Warning on line " + lineNumber + ": " + message;
        }
    }
    
    private AssemblyResult(Builder builder) {
        this.instructions = new ArrayList<>(builder.instructions);
        this.dataSegments = new ArrayList<>(builder.dataSegments);
        this.symbolTable = builder.symbolTable;
        this.errors = new ArrayList<>(builder.errors);
        this.warnings = new ArrayList<>(builder.warnings);
    }
    
    /**
     * Returns true if assembly was successful (no errors)
     */
    public boolean isSuccess() {
        return errors.isEmpty();
    }
    
    /**
     * Returns true if there are any warnings
     */
    public boolean hasWarnings() {
        return !warnings.isEmpty();
    }
    
    /**
     * Gets the assembled instructions (immutable)
     */
    public List<InstructionFormat> getInstructions() {
        return Collections.unmodifiableList(instructions);
    }
    
    /**
     * Gets the data segments (immutable)
     */
    public List<DataSegment> getDataSegments() {
        return Collections.unmodifiableList(dataSegments);
    }
    
    /**
     * Gets the symbol table
     */
    public SymbolTable getSymbolTable() {
        return symbolTable;
    }
    
    /**
     * Gets all errors (immutable)
     */
    public List<AssemblyError> getErrors() {
        return Collections.unmodifiableList(errors);
    }
    
    /**
     * Gets all warnings (immutable)
     */
    public List<AssemblyWarning> getWarnings() {
        return Collections.unmodifiableList(warnings);
    }
    
    /**
     * Gets an instruction at a specific address, or null if none
     */
    public InstructionFormat getInstructionAt(int address) {
        for (InstructionFormat instr : instructions) {
            if (instr.getAddress() == address) {
                return instr;
            }
        }
        return null;
    }
    
    /**
     * Gets the total number of instructions
     */
    public int getInstructionCount() {
        return instructions.size();
    }
    
    /**
     * Gets the total number of data words
     */
    public int getDataCount() {
        return dataSegments.size();
    }
    
    /**
     * Returns a formatted error report
     */
    public String getErrorReport() {
        if (errors.isEmpty()) {
            return "No errors";
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("Assembly failed with ").append(errors.size()).append(" error(s):\n\n");
        
        for (AssemblyError error : errors) {
            sb.append(error.getFormattedMessage()).append("\n");
        }
        
        return sb.toString();
    }
    
    /**
     * Returns a formatted warning report
     */
    public String getWarningReport() {
        if (warnings.isEmpty()) {
            return "No warnings";
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("Assembly completed with ").append(warnings.size()).append(" warning(s):\n\n");
        
        for (AssemblyWarning warning : warnings) {
            sb.append(warning).append("\n");
        }
        
        return sb.toString();
    }
    
    @Override
    public String toString() {
        if (isSuccess()) {
            return String.format("AssemblyResult[SUCCESS: %d instructions, %d data words]",
                instructions.size(), dataSegments.size());
        } else {
            return String.format("AssemblyResult[FAILURE: %d errors]", errors.size());
        }
    }
    
    // Builder for constructing results
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private final List<InstructionFormat> instructions = new ArrayList<>();
        private final List<DataSegment> dataSegments = new ArrayList<>();
        private SymbolTable symbolTable = new SymbolTable();
        private final List<AssemblyError> errors = new ArrayList<>();
        private final List<AssemblyWarning> warnings = new ArrayList<>();
        
        public Builder addInstruction(InstructionFormat instruction) {
            instructions.add(instruction);
            return this;
        }
        
        public Builder addInstructions(List<InstructionFormat> instructions) {
            this.instructions.addAll(instructions);
            return this;
        }
        
        public Builder addDataSegment(int address, short value) {
            dataSegments.add(new DataSegment(address, value));
            return this;
        }
        
        public Builder addDataSegments(List<DataSegment> segments) {
            this.dataSegments.addAll(segments);
            return this;
        }
        
        public Builder symbolTable(SymbolTable symbolTable) {
            this.symbolTable = symbolTable;
            return this;
        }
        
        public Builder addError(AssemblyError error) {
            errors.add(error);
            return this;
        }
        
        public Builder addErrors(List<AssemblyError> errors) {
            this.errors.addAll(errors);
            return this;
        }
        
        public Builder addWarning(int lineNumber, String message) {
            warnings.add(new AssemblyWarning(lineNumber, message));
            return this;
        }
        
        public Builder addWarnings(List<AssemblyWarning> warnings) {
            this.warnings.addAll(warnings);
            return this;
        }
        
        public AssemblyResult build() {
            return new AssemblyResult(this);
        }
    }
    
    // Factory method for error-only result
    
    /**
     * Creates a failed assembly result with a single error
     */
    public static AssemblyResult error(AssemblyError error) {
        return builder().addError(error).build();
    }
    
    /**
     * Creates a failed assembly result with multiple errors
     */
    public static AssemblyResult errors(List<AssemblyError> errors) {
        return builder().addErrors(errors).build();
    }
}