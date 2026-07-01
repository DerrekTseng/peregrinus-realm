package org.peregrinusrealm.conn;

import java.net.InetSocketAddress;
import java.util.function.Consumer;

import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.socket.DatagramPacket;

public class CoreClientUdpSession implements CoreClientSession {

	private final Channel rootUdpChannel; // 伺服器唯一共享的 UDP Channel
	private final InetSocketAddress remoteAddress; // 這個 Session 對應的外部 Client 地址
	private Consumer<byte[]> reciver = null;
	private long lastAccessTime; // 用於過期判定 (心跳)

	public CoreClientUdpSession(Channel rootUdpChannel, InetSocketAddress remoteAddress) {
		this.rootUdpChannel = rootUdpChannel;
		this.remoteAddress = remoteAddress;
		this.lastAccessTime = System.currentTimeMillis();
	}

	@Override
	public void setReciver(Consumer<byte[]> reciver) {
		this.reciver = reciver;
	}

	public void handleMessage(byte[] data) {
		this.lastAccessTime = System.currentTimeMillis(); // 刷新活躍時間
		if (this.reciver != null) {
			this.reciver.accept(data);
		}
	}

	@Override
	public void send(byte[] data) {
		if (isConnected()) {
			// 將資料包裝成 DatagramPacket 並指明目的地發送
			DatagramPacket packet = new DatagramPacket(Unpooled.wrappedBuffer(data), remoteAddress);
			rootUdpChannel.writeAndFlush(packet);
		}
	}

	@Override
	public boolean isConnected() {
		// 只要 Root Channel 還活著，且該虛擬連線尚未被清理算活著
		return rootUdpChannel != null && rootUdpChannel.isActive();
	}

	public long getLastAccessTime() {
		return lastAccessTime;
	}

}
