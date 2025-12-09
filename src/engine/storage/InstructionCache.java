package engine.storage;

import java.util.Map;
import java.util.TreeMap;

import engine.Helpers;
import engine.types.Instruction;
import engine.types.InstructionCacheEntry;

public class InstructionCache {
	
	private final int lineSize;
	private final int numberOfLines;
	private final int associativity;
	private int accesses;
	private int accessTime;
	private int hits;
	private TreeMap<Integer, InstructionCacheEntry> cache;
	private Memory memory;
	
	public InstructionCache(int lineSize, int numberOfLines, int associativity, int accessTime, Memory memory) {
		if (lineSize < 2)
			throw new IllegalArgumentException("Cache line size must be greater than 1B");
		
		if (!Helpers.isPowerOf2(lineSize))
			throw new IllegalArgumentException("Cache line size (" + lineSize + ") must be a power of 2");
		
		if (numberOfLines < 2)
			throw new IllegalArgumentException("Cache lines must be more than 1");
		
		if (associativity > numberOfLines)
			throw new IllegalArgumentException("Cache associativity (" + associativity + ") cannot be greater than the lines (" + numberOfLines + ")");
		
		if (memory.getSize() < lineSize)
			throw new IllegalArgumentException("Cache line size (" + lineSize + ") must be less than the memory size (" + memory.getSize() + ")");
		
		
		this.lineSize = lineSize;
		this.numberOfLines = numberOfLines;
		this.associativity = associativity;
		this.accessTime = accessTime;
		this.memory = memory;
		clear();
	}
	
	private InstructionCacheEntry fetchLine(int address) {
		accesses++;
		int tag = address / (lineSize * (numberOfLines / associativity));
		int set = (address / lineSize) % (numberOfLines / associativity);
		int offset = address % lineSize;
		
		int index = 0, oldest = 0;
		InstructionCacheEntry entry = null;
		for (int i = 0; i < associativity; i++) {
			entry = cache.get(set * associativity + i);
			if (entry == null) {
				index = i;
				break;
			} else if (entry.getTag() == tag) {
				hits++;
				return entry;
			}
			if (i == 0 || oldest > entry.getAge()) {
				index = i;
				oldest = entry.getAge();
			}
		}
		
		Instruction[] instructions = memory.getInstructions(address - offset, lineSize / 2);
		entry = new InstructionCacheEntry(tag, instructions, accesses);
		cache.put(set * associativity + index, entry);
		return entry;
	}
	
	public Instruction getInstruction(int address) {
		int offset = address % lineSize;
		return fetchLine(address).getInstructions()[offset / 2];
	}
	
	public Object[] displayData() {
		String[] headers = {"Index", "Tag", "Data"}; 
		String[][] data = new String[cache.size()][3];
		int i = 0;
		for (Map.Entry<Integer, InstructionCacheEntry> entry : cache.entrySet()) {
			data[i][0] = entry.getKey().toString();
			data[i][1] = entry.getValue().getTag() + "";
			data[i][2] = instructionsToString(entry.getValue().getInstructions());
			i++;
		}
		double hitRatio = (accesses == 0)? 0 : (hits * 100.0) / accesses;
		String data2 = String.format("%-10s: %d\n%-10s: %d\n%-10s: %.2f","Accesses", accesses, "Hits", hits, "Hit ratio", hitRatio) + "%";
		return new Object[]{data, headers, data2};
	}
	
	private static String instructionsToString(Instruction[] instructions) {
		String array = "";
		for (int i = 0; i < instructions.length; i++) {
			array += "[" + ((instructions[i] == null)? "-" : instructions[i].format(1, 1)) + "]";
		}
		return array;
	}
	
	public int getHits() {
		return hits;
	}
	
	public int getAccesses() {
		return accesses;
	}
	
	public int getAccessTime() {
		return accessTime;
	}
	
	public void clear() {
		accesses = 0;
		hits = 0;
		cache = new TreeMap<Integer, InstructionCacheEntry>();
	}
}
