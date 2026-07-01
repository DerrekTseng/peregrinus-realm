package org.peregrinusrealm.conn;

import java.util.function.Consumer;

public interface CoreClientSession {

	void setReciver(Consumer<byte[]> reciver);

	void send(byte[] data);

	boolean isConnected();

}
