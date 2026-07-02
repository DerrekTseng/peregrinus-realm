package org.peregrinusrealm.utils.marshall;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

public class MarshallBuilder {

	private ByteBuffer buffer;
	private static final int INITIAL_CAPACITY = 256;

	public static MarshallBuilder newInstance() {
		return new MarshallBuilder();
	}

	private MarshallBuilder() {
		this.buffer = ByteBuffer.allocate(INITIAL_CAPACITY).order(ByteOrder.BIG_ENDIAN);
	}

	private void ensureCapacity(int additionalBytes) {
		int required = buffer.position() + additionalBytes;
		if (required > buffer.capacity()) {
			int newCapacity = Math.max(buffer.capacity() * 2, required);
			ByteBuffer newBuffer = ByteBuffer.allocate(newCapacity).order(ByteOrder.BIG_ENDIAN);
			buffer.flip();
			newBuffer.put(buffer);
			buffer = newBuffer;
		}
	}

	public MarshallBuilder add(char[] value) {
		if (value == null) {
			ensureCapacity(Integer.BYTES);
			buffer.putInt(-1);
		} else {
			ensureCapacity(Integer.BYTES + (value.length * Character.BYTES));
			buffer.putInt(value.length);
			for (char c : value) {
				buffer.putChar(c);
			}
		}
		return this;
	}

	public MarshallBuilder add(String value) {
		if (value == null) {
			ensureCapacity(Integer.BYTES);
			buffer.putInt(-1);
		} else {
			byte[] data = value.getBytes(StandardCharsets.UTF_8);
			ensureCapacity(Integer.BYTES + data.length);
			buffer.putInt(data.length);
			buffer.put(data);
		}
		return this;
	}

	public MarshallBuilder add(boolean value) {
		ensureCapacity(1);
		buffer.put((byte) (value ? 1 : 0));
		return this;
	}

	public MarshallBuilder add(int value) {
		ensureCapacity(Integer.BYTES);
		buffer.putInt(value);
		return this;
	}

	public MarshallBuilder add(long value) {
		ensureCapacity(Long.BYTES);
		buffer.putLong(value);
		return this;
	}

	public MarshallBuilder add(byte[] value) {
		if (value == null) {
			ensureCapacity(Integer.BYTES);
			buffer.putInt(-1);
		} else {
			ensureCapacity(Integer.BYTES + value.length);
			buffer.putInt(value.length);
			buffer.put(value);
		}
		return this;
	}

	public byte[] build() {
		byte[] result = new byte[buffer.position()];
		buffer.flip();
		buffer.get(result);
		return result;
	}

}
