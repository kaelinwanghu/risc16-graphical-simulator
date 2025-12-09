package engine;

import engine.types.Register;
import engine.types.Instruction;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.lang.reflect.Method;

public final class Assembler
{
	// Assembler should not be initialized, given that it is essentially a static class
	private Assembler()
	{
		throw new IllegalStateException("Utility class");
	}

	public static void assemble(String program, Processor processor)
	{
		String noInstructionsString = "Please enter one or more instructions";
		if (program.trim().isEmpty())
		{
			throw new IllegalArgumentException(noInstructionsString);
		}
		
		// Resetting static variables
		processor.clear();
		tags.clear();
		fillLabels.clear();
		instructionAddress = 0;
		
		String[] lines = program.toLowerCase().trim().split("\\n+");
		boolean hasContent = false;
		
		// First pass: parse all lines
		for (String line : lines)
		{
			LabeledLine labeled = preprocessLine(line);
			
			if (!labeled.line.isEmpty())
			{
				hasContent = true;
				
				// Store label BEFORE parsing (points to start of this line)
				if (labeled.label != null)
				{
					tags.put(labeled.label, instructionAddress);
				}
				
				// Parse the line (this will increment instructionAddress)
				parseInstructionOrData(labeled.line, processor);
			}
		}
		
		if (!hasContent)
		{
			throw new IllegalArgumentException(noInstructionsString);
		}
		
		// Second pass: resolve all symbolic labels
		resolveSymbolicLabels(processor);
	}

	/**
	 * Preprocesses a line by removing comments and extracting labels
	 * Labels are stored with the CURRENT instructionAddress (before parsing the rest)
	 * @param line the line to process
	 * @return a tuple: [cleaned line without label, extracted label or null]
	 */
	private static LabeledLine preprocessLine(String line)
	{
		// Remove comment first
		String cleanLine = removeComment(line).trim();
		if (cleanLine.isEmpty())
		{
			return new LabeledLine(cleanLine, null);
		}

		// Check if a label is present
		if (cleanLine.contains(":"))
		{
			String[] parts = cleanLine.split(":", 2);
			String label = parts[0].trim();
			
			if (label.isEmpty())
			{
				throw new IllegalArgumentException("Line " + (instructionAddress / 2 + 1) + ": Empty label");
			}
			if (!checkLabelValidity(label))
			{
				throw new IllegalArgumentException("Line " + (instructionAddress / 2 + 1) + ": Labels can only contain alphanumeric symbols and '.' or '_'");
			}
			if (parts[1].trim().isEmpty())
			{
				throw new IllegalArgumentException("Line " + (instructionAddress / 2 + 1) + ": Cannot have a line with just a label");
			}

			cleanLine = parts[1].trim();
			return new LabeledLine(cleanLine, label);
		}
		
		return new LabeledLine(cleanLine, null);
	}
	
	/**
	 * Simple helper class to return both the cleaned line and any extracted label
	 */
	private static class LabeledLine
	{
		String line;
		String label;
		
		LabeledLine(String line, String label)
		{
			this.line = line;
			this.label = label;
		}
	}

	/**
	 * Parses a line that could be an instruction, pseudo-instruction, or data directive
	 */
	private static void parseInstructionOrData(String line, Processor processor)
	{
		String operation;
		String[] operands = {};
		
		// Extract operation and operands
		if (line.indexOf(' ') != -1)
		{
			operation = line.substring(0, line.indexOf(' '));
			operands = line.substring(line.indexOf(' ') + 1).split(",");
		}
		else
		{
			operation = line;
		}
		
		// Trim all operands
		for (int i = 0; i < operands.length; i++)
		{
			operands[i] = operands[i].trim();
		}
		
		// Handle .fill directive
		if (operation.equals(".fill"))
		{
			handleFill(operands, processor);
			return;
		}
		
		// Handle .space directive
		if (operation.equals(".space"))
		{
			handleSpace(operands, processor);
			return;
		}
		
		// Handle pseudo-instructions (nop, halt, lli, movi)
		if (pseudoInstructions.keySet().contains(operation))
		{
			parsePseudoInstruction(operation, operands, processor);
			return;
		}
		
		// Handle regular instructions
		parseInstruction(operation, operands, processor);
	}

	/**
	 * Handles .fill directive - stores a single word in memory
	 */
	private static void handleFill(String[] operands, Processor processor)
	{
		if (operands.length != 1)
		{
			throw new IllegalArgumentException("Line " + (instructionAddress / 2 + 1) + ": .fill requires exactly one operand");
		}
		
		String value = operands[0];
		
		// Try to parse as a number
		Integer numValue = parseIntegerNoThrow(value);
		if (numValue != null)
		{
			// It's a number - store it directly
			processor.getMemory().setWord(instructionAddress, numValue.shortValue());
		}
		else
		{
			// It's a label - defer resolution to second pass
			fillLabels.put(instructionAddress, value);
			processor.getMemory().setWord(instructionAddress, (short)0);  // Placeholder
		}
		
		instructionAddress += 2;
	}

	/**
	 * Handles .space directive - reserves N words of zero-initialized memory
	 */
	private static void handleSpace(String[] operands, Processor processor)
	{
		if (operands.length != 1)
		{
			throw new IllegalArgumentException("Line " + (instructionAddress / 2 + 1) + ": .space requires exactly one operand");
		}
		
		int count = parseInteger(operands[0]);
		if (count < 1)
		{
			throw new IllegalArgumentException("Line " + (instructionAddress / 2 + 1) + ": .space count must be positive");
		}
		
		// Reserve 'count' words of zeros
		for (int i = 0; i < count; i++)
		{
			processor.getMemory().setWord(instructionAddress, (short)0);
			instructionAddress += 2;
		}
	}

	private static boolean checkLabelValidity(String label)
	{
		int labelLength = label.length();
		for (int i = 0; i < labelLength; ++i)
		{
			char c = label.charAt(i);
			// alphanumeric or . or _ check
			if ((c < 'a' || c > 'z') && (c < 'A' || c > 'Z') && (c < '0' || c > '9') && (c != '.') && (c != '_'))
			{
				return false;
			}
		}
		return true;
	}

	/**
	 * Removes the comment section of a line (anything that goes after the '#' character)
	 * @param line the line string to be parsed
	 * @return the line without the comment section
	 */
	private static String removeComment(String line)
	{
		int commentIndex = line.indexOf('#');
		return (commentIndex >= 0) ? line.substring(0, commentIndex) : line;
	}
	
	/**
	 * Parses a regular (non-pseudo) instruction
	 */
	private static void parseInstruction(String operation, String[] operands, Processor processor)
	{
		Method m = InstructionSet.getMethod(operation);
		if (m == null)
		{
			throw new IllegalArgumentException("Line " + (instructionAddress / 2 + 1) + ": '" + operation + "' is an invalid operation");
		}
		
		Class<?>[] types = m.getParameterTypes();
		if (types.length != operands.length)
		{
			throw new IllegalArgumentException("Line " + (instructionAddress / 2 + 1) + ": Invalid number of operands for '" + operation + "'");
		}
		
		Object[] parameters = new Object[types.length];
		for (int i = 0; i < types.length; i++)
		{
			if (types[i] == Register.class)
			{
				Register r = processor.getRegisterFile().getRegister(operands[i]);
				if (r == null)
				{
					throw new IllegalArgumentException("Line " + (instructionAddress / 2 + 1) + ": '" + operands[i] + "' is an invalid register name");
				}
				parameters[i] = r;
			}
			else if (types[i] == int.class)
			{
				Integer immediate = parseIntegerNoThrow(operands[i]);
				if (immediate != null)
				{
					// Validate immediate ranges
					if (operation.equals("lui") && (immediate < 0 || immediate > 0x3ff))
					{
						throw new IllegalArgumentException("Line " + (instructionAddress / 2 + 1) + ": Upper immediate must be between 0x000 and 0x3ff");
					}
					else if (!operation.equals("lui") && (immediate < -64 || immediate > 63))
					{
						throw new IllegalArgumentException("Line " + (instructionAddress / 2 + 1) + ": Signed immediate must be between -64 and 63");
					}
					parameters[i] = immediate;
				}
				else
				{
					// It's a label (only valid for beq and lw/sw)
					if (operation.equals("beq") || operation.equals("lw") || operation.equals("sw"))
					{
						parameters[i] = operands[i];  // Store label string for later resolution
					}
					else
					{
						throw new IllegalArgumentException("Line " + (instructionAddress / 2 + 1) + ": Invalid immediate operand: " + operands[i]);
					}
				}
			}
		}
		
		processor.getMemory().addInstruction(operation, parameters);
		instructionAddress += 2;
	}

	public static int parseInteger(String number)
	{
		try
		{
			if (number.matches("-?\\d+"))
				return Integer.parseInt(number);
			if (number.matches("0[xX][\\da-fA-F]+"))
				return Integer.parseInt(number.substring(2), 16);
			if (number.matches("0[0-7]+"))
				return Integer.parseInt(number.substring(1), 8);
		}
		catch (Exception e)
		{
			throw new IllegalArgumentException("Failed to parse integer: " + number);
		}
		throw new IllegalArgumentException(number + " is an invalid integer");
	}

	public static Integer parseIntegerNoThrow(String number)
	{
		try
		{
			if (number.matches("-?\\d+"))
				return Integer.parseInt(number);
			if (number.matches("0[xX][\\da-fA-F]+"))
				return Integer.parseInt(number.substring(2), 16);
			if (number.matches("0[0-7]+"))
				return Integer.parseInt(number.substring(1), 8);
		}
		catch (Exception e)
		{
			return null;
		}
		return null;
	}

	public static short parseShort(String number)
	{
		try
		{
			if (number.matches("-?\\d+"))
				return Short.parseShort(number);
			if (number.matches("0[xX][\\da-fA-F]+"))
				return Short.parseShort(number.substring(2), 16);
			if (number.matches("0[0-7]+"))
				return Short.parseShort(number.substring(1), 8);
		}
		catch (Exception e)
		{
			throw new IllegalArgumentException("Failed to parse short: " + number);
		}
		throw new IllegalArgumentException(number + " is an invalid short");
	}

	/**
	 * Pseudo-instruction parser that expands pseudo-instructions into real instructions
	 * @param operation the pseudo-operation to be performed
	 * @param operands the operands of the pseudo-operation
	 * @param processor the processor
	 */
	private static void parsePseudoInstruction(String operation, String[] operands, Processor processor)
	{
		// Validate operand count
		Integer expectedOperands = pseudoInstructions.get(operation);
		if (expectedOperands == null || expectedOperands != operands.length)
		{
			throw new IllegalArgumentException("Line " + (instructionAddress / 2 + 1) + ": Invalid number of operands for '" + operation + "'");
		}
		
		switch (operation)
		{
			case "nop":
			{
				// nop -> add r0, r0, r0
				parseInstruction("add", new String[]{"r0", "r0", "r0"}, processor);
				break;
			}
			case "halt":
			{
				// halt -> jalr r0, r0
				parseInstruction("jalr", new String[]{"r0", "r0"}, processor);
				break;
			}
			case "lli":
			{
				// lli rA, imm -> addi rA, rA, (imm & 0x3f)
				int immediate = parseInteger(operands[1]) & 0x3f;
				parseInstruction("addi", new String[]{operands[0], operands[0], String.valueOf(immediate)}, processor);
				break;
			}
			case "movi":
			{
				// movi rA, imm -> lui rA, (imm >> 6) + addi rA, rA, (imm & 0x3f)
				int immediate = parseInteger(operands[1]);
				
				if (immediate < 0 || immediate > 0xFFFF)
				{
					throw new IllegalArgumentException("Line " + (instructionAddress / 2 + 1) + ": Word immediate must be between 0x0000 and 0xFFFF");
				}
				
				// First instruction: lui
				parseInstruction("lui", new String[]{operands[0], String.valueOf(immediate >> 6)}, processor);
				
				// Second instruction: addi (lli)
				parseInstruction("addi", new String[]{operands[0], operands[0], String.valueOf(immediate & 0x3f)}, processor);
				break;
			}
			default:
			{
				throw new IllegalArgumentException("Line " + (instructionAddress / 2 + 1) + ": Unknown pseudo-instruction: " + operation);
			}
		}
	}

	/**
	 * Second pass: resolves all symbolic labels in both instructions and .fill directives
	 */
	private static void resolveSymbolicLabels(Processor processor)
	{
		ArrayList<Instruction> instructions = processor.getMemory().getInstructions();
		
		// Resolve labels in branch instructions
		for (int i = 0; i < instructions.size(); i++)
		{
			Instruction instr = instructions.get(i);
			Object[] operands = instr.getOperands();
			int currentAddress = i * 2;
			String operation = instr.getOperation();
			
			for (int j = 0; j < operands.length; j++)
			{
				if (operands[j] instanceof String)
				{
					String label = (String) operands[j];
					
					if (!tags.containsKey(label))
					{
						throw new IllegalArgumentException("Undefined label: '" + label + "'");
					}
					
					int targetAddress = tags.get(label);

					if (operation.equals("beq"))
					{
						// For beq, calculate offset from next instruction
						int offset = (targetAddress - currentAddress - 2);
						
						if (offset < -64 || offset > 63)
						{
							throw new IllegalArgumentException("Branch offset out of range for label '" + label + "' (offset: " + offset + ")");
						}
						
						operands[j] = offset;
						continue;
					}
					else if (operation.equals("lw") || operation.equals("sw"))
					{
						int offset = (targetAddress - currentAddress);
						if (offset < -64 || offset > 63)
						{
							throw new IllegalArgumentException("Load/store offset out of range for label '" + label + "' (offset: " + offset + ")");
						}
						operands[j] = offset;
						continue;
					}
					else
					{
						throw new IllegalArgumentException("Unexpected label operand in instruction: " + operation);
					}
				}
			}
		}
		
		// Resolve labels in .fill directives
		for (Map.Entry<Integer, String> entry : fillLabels.entrySet())
		{
			int address = entry.getKey();
			String label = entry.getValue();
			
			if (!tags.containsKey(label))
			{
				throw new IllegalArgumentException("Undefined label in .fill directive: '" + label + "'");
			}
			
			int targetAddress = tags.get(label);
			processor.getMemory().setWord(address, (short)targetAddress);
		}
		
		// Clear for next assembly
		fillLabels.clear();
	}

	// ==================== Static State ====================
	
	// Current address in memory (incremented as we parse)
	private static int instructionAddress = 0;
	
	// Map of label names to their addresses
	private static HashMap<String, Integer> tags = new HashMap<>();
	
	// Map of .fill directive addresses to label names (for deferred resolution)
	private static HashMap<Integer, String> fillLabels = new HashMap<>();
	
	// Pseudo-instructions and their operand counts
	private static final HashMap<String, Integer> pseudoInstructions = new HashMap<>(
		Map.of(
			"nop", 0,
			"halt", 0,
			"lli", 2,
			"movi", 2,
			".fill", 1,
			".space", 1
		)
	);
}