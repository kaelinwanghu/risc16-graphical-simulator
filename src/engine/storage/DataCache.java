package engine.storage;

import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;

import engine.Helpers;
import engine.types.Addressable;
import engine.types.DataCacheEntry;
import engine.types.WritePolicy;

public class DataCache implements Addressable {

	private final int lineSize;
	private final int numberOfLines;
	private final int associativity;
	private WritePolicy onHit;
	private WritePolicy onMiss;
	private int accesses;
	private int accessTime;
	private int hits;
	private TreeMap<Integer, DataCacheEntry> cache;
	private Addressable nextLevel;
	
	public DataCache(int lineSize, int numberOfLines, int associativity, int accessTime) {
		if (lineSize < 2)
			throw new IllegalArgumentException("Cache line size must be greater than 1B");
		
		if (!Helpers.isPowerOf2(lineSize))
			throw new IllegalArgumentException("Cache line size (" + lineSize + ") must be a power of 2");
		
		if (numberOfLines < 2)
			throw new IllegalArgumentException("Cache lines must be more than 1");
		
		if (associativity > numberOfLines)
			throw new IllegalArgumentException("Cache associativity (" + associativity + ") cannot be greater than the lines (" + numberOfLines + ")");
		
		this.lineSize = lineSize;
		this.numberOfLines = numberOfLines;
		this.associativity = associativity; 
		this.accessTime = accessTime;
		clear();
	}
	
	public void setWritePolicies(WritePolicy onHit, WritePolicy onMiss) {
		if (onHit != WritePolicy.WRITE_BACK && onHit != WritePolicy.WRITE_THROUGH)
			throw new IllegalArgumentException("Invalid hit write policy");
		
		if (onMiss != WritePolicy.WRITE_ALLOCATE && onMiss != WritePolicy.WRITE_AROUND)
			throw new IllegalArgumentException("Invalid miss write policy");
		
		this.onHit = onHit;
		this.onMiss = onMiss;
	}
	
	public void setNextCacheLevel(Addressable nextCacheLevel) {
		if (nextCacheLevel instanceof DataCache) {
			if (((DataCache) nextCacheLevel).lineSize < lineSize)
				throw new IllegalArgumentException("Cache line sizes must increase or stay the same");
		} else if (((Memory) nextCacheLevel).getSize() < lineSize)
			throw new IllegalArgumentException("Cache line size (" + lineSize + ") must be less than the memory size (" + ((Memory) nextCacheLevel).getSize() + ")");
		
		this.nextLevel = nextCacheLevel;
	}
		
	public byte[] getData(int address, int bytes) {
		accesses++;
		int offset = address % lineSize;
		byte[] data = fetchLine(address).getData();
		return Arrays.copyOfRange(data, offset, offset + bytes);
	}

	public void setData(int address, byte[] data) {
		accesses++;
		if (!isFound(address)){
			writeToMemory(address, data);
			removeLine(address);
			if (onMiss == WritePolicy.WRITE_ALLOCATE)
				fetchLine(address);
			return;
		}
		int offset = address % lineSize;
		DataCacheEntry entry = fetchLine(address);
		byte[] lineData = entry.getData();
		for (int i = 0; i < data.length; i++)
			lineData[offset + i] = data[i];
		
		if (onHit == WritePolicy.WRITE_BACK)
			entry.setDirty();
		else 
			nextLevel.setData(address, data);
	}
		
	private boolean isFound(int address) {
		int tag = address / (lineSize * (numberOfLines / associativity));
		int set = (address / lineSize) % (numberOfLines / associativity);
		DataCacheEntry entry;
		for (int i = 0; i < associativity; i++) {
			entry = cache.get(set * associativity + i);
			if (entry != null && entry.getTag() == tag)
				return true;
		}
		return false;
	}
	
	private void removeLine(int address) {
		int tag = address / (lineSize * (numberOfLines / associativity));
		int set = (address / lineSize) % (numberOfLines / associativity);
		DataCacheEntry entry = null;
		for (int i = 0; i < associativity; i++) {
			entry = cache.get(set * associativity + i);
			if (entry != null && entry.getTag() == tag) {
				cache.put(set * associativity + i, null);
				break;
			}
		}
		if (nextLevel instanceof DataCache)
			((DataCache) nextLevel).removeLine(address);
	}
	
	private DataCacheEntry fetchLine(int address) {
		int tag = address / (lineSize * (numberOfLines / associativity));
		int set = (address / lineSize) % (numberOfLines / associativity);
		int offset = address % lineSize;
		
		int index = 0, oldest = 0;
		DataCacheEntry entry = null;
		for (int i = 0; i < associativity; i++) {
			entry = cache.get(set * associativity + i);
			if (entry == null) {
				index = i;
				break;
			}
			if (entry.getTag() == tag) {
				hits++;
				return entry;
			}
			if (i == 0 || oldest > entry.getAge()) {
				index = i;
				oldest = entry.getAge();
			}
		}
		
		entry = cache.get(set * associativity + index);
		if (onHit == WritePolicy.WRITE_BACK && entry != null && entry.isDirty())
			nextLevel.setData(entry.getDataAddress(), entry.getData());
		
		byte[] data = nextLevel.getData(address - offset, lineSize);
		entry = new DataCacheEntry(tag, data, address - offset, accesses);
		cache.put(set * associativity + index, entry);
		return entry;
	}
		
	private void writeToMemory(int address, byte[] data) {
		if (nextLevel instanceof DataCache)
			((DataCache) nextLevel).writeToMemory(address, data);
		else 
			((Memory) nextLevel).setData(address, data);
	}
	
	public Object[] displayData(boolean hex) {
		String[] headers;
		String[][] data;
		if (onHit == WritePolicy.WRITE_BACK) {
			headers = new String[]{"Index", "Tag", "Dirty", "Data"}; 
			data = new String[cache.size()][4];
		} else {
			headers = new String[]{"Index", "Tag", "Data"}; 
			data = new String[cache.size()][3];
		}
		
		int i = 0;
		for (Map.Entry<Integer, DataCacheEntry> entry : cache.entrySet()) {
			data[i][0] = entry.getKey().toString();
			data[i][1] = entry.getValue().getTag() + "";
			if (onHit == WritePolicy.WRITE_BACK) {
				data[i][2] = entry.getValue().isDirty() + "";
				data[i][3] = bytesToString(entry.getValue().getData(), hex);
			} else {
				data[i][2] = bytesToString(entry.getValue().getData(), hex);
			}
			i++;
		}
		double hitRatio = (accesses == 0)? 0 : (hits * 100.0) / accesses;
		String data2 = String.format("%-10s: %d\n%-10s: %d\n%-10s: %.2f","Accesses", accesses, "Hits", hits, "Hit ratio", hitRatio) + "%";
		return new Object[]{data, headers, data2};
	}
	
	private static String bytesToString(byte[] data, boolean hex) {
		String array = "[";
		for (int i = 0; i < data.length; i++) {
			array += String.format((hex)? "0x%02X" : "%4d", data[i]) + ((i == data.length - 1)? "" : "|");
		}
		return array + "]";
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
		cache = new TreeMap<Integer, DataCacheEntry>();
	}
	
}
