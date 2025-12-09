package engine;


import java.math.BigInteger;

public class Helpers {
	
	public static double log(int number, int base) {
		return Math.log10(number) / Math.log10(base);
	}
	
	public static boolean isPowerOf2(int number) {
		return (number != 0) && ((number & (number - 1)) == 0);
	}
	
	public static byte[] toBytes(short word) {
		return new byte[]{(byte) (word >> 8), (byte) word};
	}
	
	public static short toWord(byte[] bytes) {
		return new BigInteger(bytes).shortValue();
	}
	
}
