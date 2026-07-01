package org.peregrinusrealm.conn;

import java.util.function.Consumer;

import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;

public class CoreClientTcpSession implements CoreClientSession {
	
	private final Channel channel;
	private Consumer<byte[]> reciver = null;

	public CoreClientTcpSession(Channel channel) {
		this.channel = channel;
	}

	@Override
	public void setReciver(Consumer<byte[]> reciver) {
		this.reciver = reciver;
	}

	// 供 Netty 內部收到資料時呼叫，派發給外部商業邏輯
	public void handleMessage(byte[] data) {
		if (this.reciver != null) {
			this.reciver.accept(data);
		}
	}

	@Override
	public void send(byte[] data) {
		if (isConnected()) {
			// Netty 的 writeAndFlush 是執行緒安全的，可由外部執行緒任意呼叫
			channel.writeAndFlush(Unpooled.wrappedBuffer(data));
		}
	}

	@Override
	public boolean isConnected() {
		return channel != null && channel.isActive();
	}

}