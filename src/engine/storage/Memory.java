package engine.storage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.HashSet;

import engine.Helpers;
import engine.types.Addressable;
import engine.types.Instruction;

public class Memory implements Addressable {
	
	private final int size;
	private int instructionAccesses;
	private int dataAccesses;
	private int accessTime;
	private TreeMap<Integer, Byte> memory;
	private ArrayList<Instruction> instructions;
	private HashSet<Integer> changedAddresses;
	private HashSet<Integer> explicitlySetAddresses;
	
	public Memory(int size, int accessTime) {
		if (size < 128 || size > 4194304)
			throw new IllegalArgumentException("Memory size must be greater than 128B and less than 4MB");
		
		if (!Helpers.isPowerOf2(size))
			throw new IllegalArgumentException("Memory size (" + size + ") must be a power of 2");
		this.size = size;
		this.accessTime = accessTime;
		this.changedAddresses = new HashSet<>();
		this.explicitlySetAddresses = new HashSet<>(); 
		
		clear();
	}

	public void addInstruction(String operation, Object[] operands) {
		int address = instructions.size() * 2;
		if (address >= size)
			throw new IllegalArgumentException("Program too large for memory");
		instructions.add(new Instruction(address, operation, operands));
	}
	
	public Instruction[] getInstructions(int address, int number) {
		if (address % 2 != 0 || address < 0 || address + number * 2 >= size) 
			throw new IllegalArgumentException("Invalid instruction address (" + address + ")");
		
		instructionAccesses++;
		Instruction[] ins = new Instruction[number];
		int index;
		for (int i = 0; i < number; i++) {
			index = address / 2 + i;
			ins[i] = (index < 0 || index >= instructions.size())? null : instructions.get(index);
		}
		return ins;
	}
	
	public void setByte(int address, byte data) {
		if (address < 0 || address >= size)
			throw new IllegalArgumentException("Invalid address (" + address + ")");
			
		memory.put(address, data);
	}
	
	public byte getByte(int address) {
		if (address < 0 || address >= size)
			throw new IllegalArgumentException("Invalid address (" + address + ")");
		
		Byte data = memory.get(address);
		return (data == null) ? 0 : data;
	}
	
	public void setWord(int address, short data) {
		if (address % 2 != 0) 
			throw new IllegalArgumentException("Invalid word address (" + address + ")");
		
		byte[] bytes = Helpers.toBytes(data);
		setByte(address, bytes[0]);
		setByte(address + 1, bytes[1]);

		changedAddresses.add(address);
		explicitlySetAddresses.add(address);
	}
	
	public short getWord(int address) {
		if (address % 2 != 0) 
			throw new IllegalArgumentException("Invalid word address (" + address + ")");
		
		return Helpers.toWord(new byte[]{getByte(address), getByte(address + 1)});
	}
	
	public byte[] getData(int address, int bytes) {
		dataAccesses++;
		byte[] data = new byte[bytes];
		for (int i = 0; i < data.length; i++) {
			data[i] = getByte(address + i);
		}
		return data;
	}
	
	public void setData(int address, byte[] data) {
		dataAccesses++;
		for (int i = 0; i < data.length; i++)
			setByte(address + i, data[i]);
		
        if (address % 2 == 0 && data.length >= 2)
        {
            explicitlySetAddresses.add(address);
            changedAddresses.add(address);
		}
	}
	
	public Object[] displayDataWords(boolean hex) {
		int bits = (int)(Math.ceil(Helpers.log(size, (hex)? 16 : 10)));
		String[] headers = {"Address", "Word"}; 
		
    	ArrayList<Integer> validAddresses = new ArrayList<>();
    
        for (int address : explicitlySetAddresses)
        {
            if (address % 2 == 0)
            {
                validAddresses.add(address);
            }
        }
        
        // Also show any non-zero addresses not already in the list
        for (int address : memory.keySet())
        {
            if (address % 2 == 0 && !explicitlySetAddresses.contains(address))
            {
                short value = getWord(address);
                if (value != 0)
                {
                    validAddresses.add(address);
                }
            }
        }
        
        // Remove duplicates and sort
        TreeSet<Integer> sortedAddresses = new TreeSet<>(validAddresses);

		String[][] data = new String[sortedAddresses.size()][2];
		int i = 0;
		for (int address : sortedAddresses)
		{
			data[i][0] = String.format((hex)? "0x%0" + bits + "X" : "%d", address);
			data[i][1] = String.format((hex)? "0x%04X" : "%d", getWord(address));
			i++;
		}
		String accesses = String.format("%-20s : %d\n%-20s : %d", "Instruction accesses", instructionAccesses, "Data accesses", dataAccesses);
		return new Object[]{data, headers, accesses};
	}
	
	public void clear() {
		instructionAccesses = 0;
		dataAccesses = 0;
		memory = new TreeMap<>();
		instructions = new ArrayList<>();
		changedAddresses = new HashSet<>();
		explicitlySetAddresses = new HashSet<>();
	}
	
	public int getDataAccesses() {
		return dataAccesses;
	}
	
	public int getAccessTime() {
		return accessTime;
	}
	
	public ArrayList<Instruction> getInstructions() {
		 return instructions;
	 }
	
	public int getSize() {
		return size;
	}

	public int getLastInstructionAddress() {
		return instructions.size() * 2 - 2;
	}

	public void markChanged(int address)
	{
		changedAddresses.add(address);
	}

	public boolean hasChanged(int address)
    {
        return changedAddresses.contains(address);
    }

	public void clearChanges()
    {
        changedAddresses.clear();
    }
}
