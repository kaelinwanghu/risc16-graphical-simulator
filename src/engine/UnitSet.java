package engine;

import java.util.ArrayList;

import engine.types.FunctionType;
import engine.types.Instruction;

public class UnitSet {
	
	private int[][] configuration;
	private ArrayList<Instruction> executed;
	
	public UnitSet(int[][] configuration) {
		executed = new ArrayList<Instruction>();
		setConfiguration(configuration);
	}
		
	public void addExecutedInstruction(Instruction instruction) {
		if (instruction.getExecutionTime() == -1)
			instruction.setExecutionTime(getExecutionTime(instruction.getFunction()));
		
		executed.add(instruction);
	}
	
	public Object[] displaySchedule() {
		int[][] timings = scheduleInstructions();
		String[] headers = {"Instruction", "Issued", "Executed", "Written", "Committed"}; 
		String[][] data = new String[timings.length][5];
		for (int i = 0; i < timings.length; i++) {
			data[i][0] = executed.get(i) + "";
			data[i][1] = timings[i][0] + "";
			data[i][2] = timings[i][1] + "";
			data[i][3] = timings[i][2] + "";
			data[i][4] = timings[i][3] + "";
		}
		int cycles = timings[timings.length - 1][3];
		String data2 = String.format("Executed : %d Instructions\nDuration : %d Cycles\n", executed.size(), cycles);
		data2 += String.format("IPC      : %.2f", executed.size() * 1.0 / cycles);
		return new Object[]{data, headers, data2};
	}
	
	private int[][] scheduleInstructions() {
		int[][] schedule = new int[executed.size()][4];
		int dependancy;
		boolean reset = false;
		boolean cdbEmpty;
		Instruction instruction;
		for (int i = 0; i < schedule.length; i++) {
			instruction = executed.get(i);
			
			if (i == 0)
				schedule[i][0] = 1;
			else if (reset)
				schedule[i][0] = schedule[i - 1][3] + 1;
			else
				schedule[i][0] = Math.max(getIssueCycle1(schedule, i), getIssueCycle2(schedule, i));
			
			dependancy = getDependancy(i);
			schedule[i][1] = (dependancy == -1)? schedule[i][0] : Math.max(schedule[dependancy][2], schedule[i][0]);
			schedule[i][1] += executed.get(i).getExecutionTime();
			schedule[i][2] = schedule[i][1] + 1;
			
			do {
				cdbEmpty = true;
				for (int j = 0; j < i; j++) {
					if (schedule[i][2] == schedule[j][2]) {
						cdbEmpty = false;
						schedule[i][2]++;
						break;
					}
				}
			} while(!cdbEmpty);
			
			schedule[i][3] = ((i == 0)? schedule[0][2] : Math.max(schedule[i - 1][3], schedule[i][2])) + 1;
			
			reset = false;
			if (instruction.getFunction() == FunctionType.BRANCH) {
				int takenAddress = instruction.getAddress() + (Integer)instruction.getOperands()[2] + 2;
				boolean taken = takenAddress == instruction.getEffectiveAddress();
				boolean prediction = (Integer)instruction.getOperands()[2] >= 0;
				reset = taken != prediction;
			}
		}
		return schedule;
	}
	
	private int getIssueCycle1(int[][] schedule, int instructionNumber) {
		int cycle = schedule[instructionNumber - 1][0] + 1;
		int minCycle = schedule[instructionNumber - 1][3];
		int robEntries = 0;
		for (int j = instructionNumber - 1; j > -1; j--) {
			if (cycle >= schedule[j][0] && cycle < schedule[j][3]) {
				robEntries++;
				if (minCycle > schedule[j][3]) {
					minCycle = schedule[j][3];
				}
			}
		}
		return (robEntries >= configuration[0][0])? minCycle + 1 : cycle;
	}
	
	private int getIssueCycle2(int[][] schedule, int instructionNumber) {
		FunctionType function = executed.get(instructionNumber).getFunction();
		int cycle = schedule[instructionNumber - 1][0] + 1;
		
		if (function.ordinal() >= configuration.length - 1)
			return cycle;
		
		int minCycle = schedule[instructionNumber - 1][3];
		int stations = 0;
		for (int j = instructionNumber - 1; j > -1; j--) {
			if (function == executed.get(j).getFunction() && cycle >= schedule[j][0] && cycle < schedule[j][3]) {
				stations++;
				if (minCycle > schedule[j][3]) {
					minCycle = schedule[j][3];
				}
			}
		}
		int allStations = configuration[function.ordinal() + 1][0] * configuration[function.ordinal() + 1][1];
		return (stations >= allStations)? minCycle : cycle;
	}
	
	private int getDependancy(int instructionNumber) {
		Instruction instruction = executed.get(instructionNumber);
		boolean writes;
		for (int i = instructionNumber - 1; i >= 0; i--) {
			Instruction prev = executed.get(i);
			writes = prev.getFunction() != FunctionType.STORE && 
					prev.getFunction() != FunctionType.BRANCH && 
					prev.getFunction() != FunctionType.JUMP;
			
			if (instruction.getFunction() == FunctionType.LOAD) {
				if (prev.getFunction() == FunctionType.STORE && 
					prev.getEffectiveAddress() == instruction.getEffectiveAddress())
					return i;
			} if (instruction.getFunction() == FunctionType.STORE || instruction.getFunction() == FunctionType.JUMP) {
				if (writes && prev.getDestination() == instruction.getRegisterNumber(0))
					return i;
			} if (instruction.getFunction() == FunctionType.BRANCH) {
				if (writes && (prev.getDestination() == instruction.getRegisterNumber(0) || 
					prev.getDestination() == instruction.getRegisterNumber(1)))
					return i;
			} else {
				if (writes && (prev.getDestination() == instruction.getRegisterNumber(1) || 
					prev.getDestination() == instruction.getRegisterNumber(2)))
					return i;
			}
		}
		return -1;
	}
	
	private int getExecutionTime(FunctionType function) {
		if (function.ordinal() >= configuration.length - 1)
			return 1;
		
		if (configuration[function.ordinal() + 1].length == 3)
			return configuration[function.ordinal() + 1][2];
		
		return -1;
	}

	public void setConfiguration(int[][] configuration) {
		for (int i = 0; i < configuration.length; i++)
			for (int j = 0; j < configuration[i].length; j++)
				if (configuration[i][j] < 1)
					throw new IllegalArgumentException("Invalid units' configuration");

		this.configuration = configuration;
		
		for (Instruction instruction : executed) {
			int time = getExecutionTime(instruction.getFunction());
			if (time != -1)
				instruction.setExecutionTime(time);
		}
	}
	
	public int[][] getConfiguration() {
		return configuration;
	}
	
	public void clear() {
		executed = new ArrayList<Instruction>();
	}
	
}
