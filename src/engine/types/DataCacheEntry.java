package engine.types;

public class DataCacheEntry {

	private int tag;
	private int dataAddress;
	private byte[] data;
	private int age;
	private boolean dirty;
	
	public DataCacheEntry(int tag, byte[] data, int dataAddress, int age) {
		this.tag = tag;
		this.data = data.clone();
		this.dataAddress = dataAddress;
		this.age = age;
	}

	public int getTag() {
		return tag;
	}
	
	public byte[] getData() {
		return data;
	}
	
	public void setData(byte[] data) {
		this.data = data;
	}
	
	public int getDataAddress() {
		return dataAddress;
	}
	
	public int getAge() {
		return age;
	}
	
	public boolean isDirty() {
		return dirty;
	}
	
	public void setDirty() {
		this.dirty = true;
	}

}
