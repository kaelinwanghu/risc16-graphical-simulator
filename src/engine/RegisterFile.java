package engine;

import java.util.ArrayList;
import java.util.HashSet;

import engine.types.Register;

public class RegisterFile {

	private int pc;
	private ArrayList<Register> registers;
	private HashSet<Integer> changedRegisters;
	private boolean r0AttemptedWrite;
	
	public RegisterFile(int instructionsStartAddress) {
		registers = new ArrayList<>(8);
		for (int i = 0; i < 8; i++)
			registers.add(new Register(i, i != 0, this));
		pc = instructionsStartAddress;
		changedRegisters = new HashSet<>();
		r0AttemptedWrite = false;
	}
	
	public Register getRegister(String registerName) {
		for (Register register : registers)
			if (register.getName().equalsIgnoreCase(registerName)) 
				return register;
		
		return null;
	}
	
	public Object[] displayRegisters(boolean hex) {
		String[] headers = {"Register", "Word"}; 
		String[][] data = new String[registers.size()][2];
		for (int i = 0; i < data.length; i++) {
			Register r = registers.get(i);
			data[i][0] = r.getName();
			data[i][1] = String.format((hex)? "0x%04X" : "%d", r.getValue());
		}
		return new Object[]{data, headers, String.format("\nPC : " + ((hex)? "0x%04X" : "%d"), pc), new HashSet<>(changedRegisters), r0AttemptedWrite};
	}
	
	public int getPc() {
		return pc;
	}

	public void setPc(int pc) {
		this.pc = pc;
	}
	
	public void incrementPc(int value) {
		pc += value;
	}
	
	public void clear(int instructionsStartAddress) {
		for (Register register : registers)
			register.clear();
		pc = instructionsStartAddress;
		changedRegisters.clear();
		r0AttemptedWrite = false;
	}
	
	public void markRegisterChanged(int registerNumber)
	{
		if (registerNumber == 0)
		{
			r0AttemptedWrite = true;
		}
		changedRegisters.add(registerNumber);
	}

	public boolean hasChanged(int registerNumber)
    {
        return changedRegisters.contains(registerNumber);
    }

	public boolean r0WasAttemptedWrite()
    {
        return r0AttemptedWrite;
    }

	public void clearChanges()
    {
        changedRegisters.clear();
        r0AttemptedWrite = false;
    }
}
