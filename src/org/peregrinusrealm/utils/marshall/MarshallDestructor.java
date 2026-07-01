package org.peregrinusrealm.utils.marshall;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

public class MarshallDestructor {

	private final ByteBuffer buffer;

	public MarshallDestructor(byte[] value) {
		this.buffer = ByteBuffer.wrap(value).order(ByteOrder.BIG_ENDIAN);
	}

	public Object getObject(Class<?> type) {
		if (type == String.class) {
			return getString();
		} else if (type == Integer.class || type == int.class) {
			return getInt();
		} else if (type == Long.class || type == long.class) {
			return getLong();
		} else if (type == Boolean.class || type == boolean.class) {
			return getBoolean();
		} else if (type == byte[].class) {
			return getBytes();
		} else if (type == char[].class) {
			return getChars();
		} else {
			throw new RuntimeException("Not accept object type: " + type);
		}
	}

	public String getString() {
		int length = buffer.getInt();
		if (length == -1) {
			return null;
		}
		byte[] data = new byte[length];
		buffer.get(data);
		return new String(data, StandardCharsets.UTF_8);
	}

	public char[] getChars() {
		int length = buffer.getInt();
		if (length == -1) {
			return null;
		}
		char[] data = new char[length];
		for (int i = 0; i < data.length; i++) {
			data[i] = buffer.getChar();
		}
		return data;
	}

	public boolean getBoolean() {
		return buffer.get() == 1;
	}

	public int getInt() {
		return buffer.getInt();
	}

	public long getLong() {
		return buffer.getLong();
	}

	public byte[] getBytes() {
		int length = buffer.getInt();
		if (length == -1) {
			return null;
		}
		byte[] data = new byte[length];
		buffer.get(data);
		return data;
	}

	public int remaining() {
		return buffer.remaining();
	}

}
