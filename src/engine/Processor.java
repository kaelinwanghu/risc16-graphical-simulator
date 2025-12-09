package engine;
import java.lang.reflect.Method;

import engine.storage.DataCache;
import engine.storage.InstructionCache;
import engine.storage.Memory;
import engine.types.Addressable;
import engine.types.FunctionType;
import engine.types.Instruction;
import engine.types.WritePolicy;

public class Processor {
	
	private RegisterFile registerFile;
	private Memory memory;
	private DataCache[] dataCache;
	private InstructionCache instructionCache;
	private UnitSet unitSet;
	
	public Processor(int[][] cacheConfig, int[][] unitsConfig) {
		configureStorage(cacheConfig);
		unitSet = new UnitSet(unitsConfig);
		registerFile = new RegisterFile(0);
	}
	
	public void configureStorage(int[][] config) {
		if (config.length < 3)
			throw new IllegalArgumentException("Invalid configuration");
		
		memory = new Memory(config[0][0], config[0][3]);
		instructionCache = new InstructionCache(config[1][0], config[1][1], config[1][2], config[1][3], memory);
		Addressable prev = memory;
		dataCache = new DataCache[config.length - 2];
		for (int i = config.length - 1; i >= 2; i--) {
			dataCache[i - 2] = new DataCache(config[i][0], config[i][1], config[i][2], config[i][3]);
			dataCache[i - 2].setWritePolicies(WritePolicy.values()[config[i][4]], WritePolicy.values()[config[i][5]]);
			dataCache[i - 2].setNextCacheLevel(prev);
			prev = dataCache[i - 2];
		}
	}
			
	public boolean execute(boolean stepped) throws IllegalArgumentException {
		InstructionSet instructionSet = new InstructionSet(this);
		Instruction instruction;
		int oldPc;
		do {
			oldPc = registerFile.getPc();
			instruction = instructionCache.getInstruction(oldPc).clone();
	
			registerFile.incrementPc(2);
			
			Method method = InstructionSet.getMethod(instruction.getOperation());
			Object[] data;
			
			try {
				 data = (Object[])method.invoke(instructionSet, instruction.getOperands());
			} catch (Exception ex) {
				registerFile.setPc(oldPc);
				throw new IllegalArgumentException(ex.getCause().getMessage());
			}
						
			instruction.setFunction((FunctionType)data[0]);
			instruction.setDestination((Integer)data[1]);
			instruction.setEffectiveAddress((Integer)data[2]);
			
			if (data.length == 4)
				instruction.setExecutionTime((Integer)data[3]);
			
			unitSet.addExecutedInstruction(instruction);
			
			if (registerFile.getPc() > memory.getLastInstructionAddress())
				return true;
		
		} while (!stepped);
		
		return false;
	}
		
	public int getDataAccessTime() {
		int accessTime = 0;
		for (int i = 0; i < dataCache.length; i++)
			accessTime += dataCache[i].getAccesses() * dataCache[i].getAccessTime();

		accessTime += memory.getDataAccesses() * memory.getAccessTime();
		return accessTime;
	}
	
	public RegisterFile getRegisterFile() {
		return registerFile;
	}
	
	public Memory getMemory() {
		return memory;
	}
	
	public DataCache getDataCache(int level) {
		if (level > -1 && level < dataCache.length)
			return dataCache[level];
		return null;
	}
	
	public InstructionCache getInstructionCache() {
		return instructionCache;
	}
	
	public UnitSet getUnitSet() {
		return unitSet;
	}
	
	public void clear() {
		registerFile.clear(0);
		unitSet.clear();
		memory.clear();
		instructionCache.clear();
		for (DataCache cache : dataCache)
			cache.clear();
	}
	
}
