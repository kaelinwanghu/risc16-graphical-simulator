package engine.types;

public class Instruction implements Cloneable {

	private int address;
	private String operation;
	private Object[] operands;
	
	private FunctionType function;
	private int executionTime;
	private int destination;
	private int effectiveAddress;
	
	public Instruction(int address, String operation, Object[] operands) {
		this.address = address;
		this.operation = operation;
		this.operands = operands;
		executionTime = -1;
	}
	
	public int getAddress() {
		return address;
	}
	
	public String getOperation() {
		return operation;
	}
		
	public Object[] getOperands() {
		return operands;
	}
	
	public int getRegisterNumber(int operandNumber) {
		if (operandNumber > -1 && operandNumber < operands.length && operands[operandNumber] instanceof Register)
			return ((Register)operands[operandNumber]).getNumber();
		
		return -1;
	}
	
	public void setFunction(FunctionType function) {
		this.function = function;
	}
	
	public FunctionType getFunction() {
		return function;
	}
	
	public void setExecutionTime(int executionTime) {
		this.executionTime = executionTime;
	}
	
	public int getExecutionTime() {
		return executionTime;
	}
	
	public void setDestination(int destination) {
		this.destination = destination;
	}
		
	public int getDestination() {
		return destination;
	}
	
	public void setEffectiveAddress(int effectiveAddress) {
		this.effectiveAddress = effectiveAddress;
	}
	
	public int getEffectiveAddress() {
		return effectiveAddress;
	}
	
	public String format(int operation, int operand) {
		String instruction = String.format("%-" + operation + "s ", this.operation);
		for (int i = 0; i < operands.length - 1; i++) 
			instruction += String.format("%-" + operand + "s, ", operands[i]);
		
		instruction += operands[operands.length - 1];
		return instruction.toUpperCase();
	}
	
	public Instruction clone() {
		try {
			return (Instruction)super.clone();
		} catch (CloneNotSupportedException e) {
			return null;
		}
	}
	
	public String toString() {
		return format(5, 2);
	}
	
}
