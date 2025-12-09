package engine.types;

public class InstructionCacheEntry {

	private int tag;
	private Instruction[] instructions;
	private int age;
	
	public InstructionCacheEntry(int tag, Instruction[] instructions, int age) {
		this.tag = tag;
		this.instructions = instructions;
		this.age = age;
	}

	public int getTag() {
		return tag;
	}
	
	public Instruction[] getInstructions() {
		return instructions;
	}
	
	public int getAge() {
		return age;
	}
	
}
