package engine;
import java.lang.reflect.Method;

import engine.types.FunctionType;
import engine.types.Register;


public class InstructionSet {
	
	private Processor processor;
	
	public InstructionSet(Processor processor) {
		this.processor = processor;
	}
	
	public Object[] add(Register r1, Register r2, Register r3) {
		r1.setValue((short)(r2.getValue() + r3.getValue()));
		return new Object[]{FunctionType.ADD, r1.getNumber(), -1};
	}
	
	public Object[] addi(Register r1, Register r2, int immediate) {
		r1.setValue((short)(r2.getValue() + immediate));
		return new Object[]{FunctionType.ADD, r1.getNumber(), -1};
	}

	public Object[] nand(Register r1, Register r2, Register r3) {
		r1.setValue((short)(~(r2.getValue() & r3.getValue())));
		return new Object[]{FunctionType.ALU, r1.getNumber(), -1};
	}

	public Object[] lui(Register r, int immediate) {
		r.setValue((short)(immediate << 6));
		return new Object[]{FunctionType.LOAD, r.getNumber(), -1};
	}

	public Object[] sw(Register r1, Register r2, int immediate) {
		int effectiveAddress = r2.getValue() + immediate;
				
		int time1 = processor.getDataAccessTime();
		processor.getDataCache(0).setData(effectiveAddress, Helpers.toBytes(r1.getValue()));
		int time2 = processor.getDataAccessTime();
		return new Object[]{FunctionType.STORE, -1, effectiveAddress, time2 - time1};
	}
		
	public Object[] lw(Register r1, Register r2, int immediate) {
		int effectiveAddress = r2.getValue() + immediate;
		
		int time1 = processor.getDataAccessTime();
		r1.setValue(Helpers.toWord(processor.getDataCache(0).getData(effectiveAddress, 2)));
		int time2 = processor.getDataAccessTime();
		return new Object[]{FunctionType.LOAD, r1.getNumber(), effectiveAddress, time2 - time1};
	}
		
	public Object[] beq(Register r1, Register r2, int immediate) {
		if (r1.getValue() == r2.getValue())
			processor.getRegisterFile().incrementPc(immediate);
		
		return new Object[]{FunctionType.BRANCH, -1, processor.getRegisterFile().getPc()};
	}
	
	public Object[] jalr(Register r1, Register r2) {
		r1.setValue((short) (processor.getRegisterFile().getPc()));
		processor.getRegisterFile().setPc(r2.getValue());
		return new Object[]{FunctionType.JUMP_AND_LINK, r1.getNumber(), (int)r2.getValue()};
	}

	public static Method getMethod(String operation) {
		Method[] methods = InstructionSet.class.getDeclaredMethods();
		for (Method method : methods) {
			if (method.getName().equals(operation))
				return method;
		}
		return null;
	}
	
}
