package org.peregrinusrealm.utils.marshall;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * OutputStream-based Marshaller, used to serialize data directly to an output stream.
 */
public class MarshallWriter implements AutoCloseable {

	private final DataOutputStream out;

	public MarshallWriter(OutputStream outputStream) {
		this.out = new DataOutputStream(outputStream);
	}

	public MarshallWriter add(String value) throws IOException {
		if (value == null) {
			out.writeInt(-1);
		} else {
			byte[] data = value.getBytes(StandardCharsets.UTF_8);
			out.writeInt(data.length);
			out.write(data);
		}
		return this;
	}

	public MarshallWriter add(char[] value) throws IOException {
		if (value == null) {
			out.writeInt(-1);
		} else {
			out.writeInt(value.length);
			for (char c : value) {
				out.writeChar(c);
			}
		}
		return this;
	}

	public MarshallWriter add(boolean value) throws IOException {
		out.write(value ? 1 : 0);
		return this;
	}

	public MarshallWriter add(int value) throws IOException {
		out.writeInt(value);
		return this;
	}

	public MarshallWriter add(long value) throws IOException {
		out.writeLong(value);
		return this;
	}

	public MarshallWriter add(byte[] value) throws IOException {
		if (value == null) {
			out.writeInt(-1);
		} else {
			out.writeInt(value.length);
			out.write(value);
		}
		return this;
	}

	/**
	 * Force the buffered data out to the underlying stream.
	 */
	public void flush() throws IOException {
		out.flush();
	}

	@Override
	public void close() throws IOException {
		out.close();
	}
}
