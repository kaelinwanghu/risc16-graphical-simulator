package engine.types;

public interface Addressable {

	byte[] getData(int address, int bytes);
	
	void setData(int address, byte[] data);
	
}
