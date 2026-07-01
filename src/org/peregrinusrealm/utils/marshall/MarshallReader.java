package org.peregrinusrealm.utils.marshall;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * InputStream-based Marshaller, used to deserialize data from an input stream.
 */
public class MarshallReader implements AutoCloseable {

	private final DataInputStream in;

	public MarshallReader(InputStream inputStream) {
		this.in = new DataInputStream(inputStream);
	}

	public String getString() throws IOException {
		int length = in.readInt();
		if (length == -1) {
			return null;
		}
		byte[] data = new byte[length];
		in.readFully(data);
		return new String(data, StandardCharsets.UTF_8);
	}

	public boolean getBoolean() throws IOException {
		return in.readByte() == 1;
	}

	public int getInt() throws IOException {
		return in.readInt();
	}

	public long getLong() throws IOException {
		return in.readLong();
	}

	public byte[] getBytes() throws IOException {
		int length = in.readInt();
		if (length == -1) {
			return null;
		}
		byte[] data = new byte[length];
		in.readFully(data);
		return data;
	}

	public char[] getChars() throws IOException {
		int length = in.readInt();
		if (length == -1) {
			return null;
		}
		char[] data = new char[length];
		for (int i = 0; i < data.length; i++) {
			data[i] = in.readChar();
		}
		return data;
	}

	/**
	 * Note: implementing an exact remaining() on an InputStream is difficult. DataInputStream's
	 * available() can only return an estimate of the bytes readable without blocking.
	 */
	public int available() throws IOException {
		return in.available();
	}

	@Override
	public void close() throws IOException {
		in.close();
	}
}
