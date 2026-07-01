package org.peregrinusrealm.conn;

import java.net.InetSocketAddress;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

public class CoreClient {

	private final String host;
	private final int tcpPort;
	private final int udpPort;

	private Consumer<byte[]> tcpReciver = null;
	private Consumer<byte[]> udpReciver = null;

	// 🌟 改為成員變數，控管整個 Client 的生命週期
	private EventLoopGroup group;
	private Channel tcpChannel;
	private Channel udpChannel;

	public CoreClient() {
		this.host = "127.0.0.1";
		this.tcpPort = 25565;
		this.udpPort = 25566;
		init();
	}

	public CoreClient(String host, int tcpPort, int udpPort) {
		this.host = host;
		this.tcpPort = tcpPort;
		this.udpPort = udpPort;
		init();
	}

	private void init() {
		// 🌟 在初始化時就先把管家建立好，讓 TCP 和 UDP 共享
		this.group = new MultiThreadIoEventLoopGroup(NioIoHandler.newFactory());
	}

	public void setTcpReciver(Consumer<byte[]> tcpReciver) {
		this.tcpReciver = tcpReciver;
	}

	public void setUdpReciver(Consumer<byte[]> udpReciver) {
		this.udpReciver = udpReciver;
	}

	public boolean isTcpConnected() {
		// 🌟 檢查 Channel 存在且目前是連線活躍狀態
		return tcpChannel != null && tcpChannel.isActive();
	}

	public boolean isUdpConnected() {
		// 🌟 UDP 雖然無連接，但只要綁定本地 Port 完成，即可視為就緒
		return udpChannel != null && udpChannel.isActive();
	}

	public void connectTcp(BiConsumer<Boolean, Throwable> callback) {
		if (isTcpConnected()) {
			if (callback != null)
				callback.accept(true, null);
			return;
		}

		Bootstrap b = new Bootstrap().group(group).channel(NioSocketChannel.class).handler(new ChannelInitializer<SocketChannel>() {
			@Override
			protected void initChannel(SocketChannel ch) {
				ch.pipeline().addLast(new SimpleChannelInboundHandler<ByteBuf>() {
					@Override
					protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) {
						// 🌟 將 ByteBuf 轉成 byte[] 後，派發給你的接收器
						if (tcpReciver != null) {
							byte[] data = new byte[msg.readableBytes()];
							msg.readBytes(data);
							tcpReciver.accept(data);
						}
					}
				});
			}
		});

		// 🌟 使用非同步連線，不要用 .sync() 擋住 LibGDX 主執行緒
		b.connect(host, tcpPort).addListener((ChannelFutureListener) future -> {
			if (future.isSuccess()) {
				this.tcpChannel = future.channel();
				if (callback != null)
					callback.accept(true, null);
			} else {
				if (callback != null)
					callback.accept(false, future.cause());
			}
		});
	}

	public void connectUdp(BiConsumer<Boolean, Throwable> callback) {
		if (isUdpConnected()) {
			if (callback != null)
				callback.accept(true, null);
			return;
		}

		Bootstrap b = new Bootstrap().group(group).channel(NioDatagramChannel.class).handler(new ChannelInitializer<NioDatagramChannel>() { // 🌟 注意此處對齊新版新命名
			@Override
			protected void initChannel(NioDatagramChannel ch) {
				ch.pipeline().addLast(new SimpleChannelInboundHandler<DatagramPacket>() {
					@Override
					protected void channelRead0(ChannelHandlerContext ctx, DatagramPacket packet) {
						// 🌟 將 UDP 封包內容轉成 byte[] 派發
						if (udpReciver != null) {
							ByteBuf content = packet.content();
							byte[] data = new byte[content.readableBytes()];
							content.readBytes(data);
							udpReciver.accept(data);
						}
					}
				});
			}
		});

		// 🌟 UDP 綁定本地隨機 Port (0)
		b.bind(0).addListener((ChannelFutureListener) future -> {
			if (future.isSuccess()) {
				this.udpChannel = future.channel();
				if (callback != null)
					callback.accept(true, null);
			} else {
				if (callback != null)
					callback.accept(false, future.cause());
			}
		});
	}

	public void tcpSend(byte[] data) {
		if (isTcpConnected()) {
			// 🌟 直接將 byte[] 包裹成 Netty 的 ByteBuf 送出
			tcpChannel.writeAndFlush(Unpooled.wrappedBuffer(data));
		} else {
			System.err.println("[Client Error] TCP 未連線，無法發送資料。");
		}
	}

	public void udpSend(byte[] data) {
		if (isUdpConnected()) {
			// 🌟 UDP 發送必須包裹成 DatagramPacket 並指定 Server 目標位址
			DatagramPacket packet = new DatagramPacket(Unpooled.wrappedBuffer(data), new InetSocketAddress(host, udpPort));
			udpChannel.writeAndFlush(packet);
		} else {
			System.err.println("[Client Error] UDP 未就緒，無法發送資料。");
		}
	}

	public void disconnectTcp() {
		if (tcpChannel != null) {
			tcpChannel.close();
			tcpChannel = null;
			System.out.println("[Client] TCP 已斷開連線。");
		}
	}

	public void disconnectUdp() {
		if (udpChannel != null) {
			udpChannel.close();
			udpChannel = null;
			System.out.println("[Client] UDP 通道已關閉。");
		}
	}

	// 🌟 新增：當遊戲完全關閉時，釋放執行緒池資源
	public void shutdown() {
		disconnectTcp();
		disconnectUdp();
		if (group != null) {
			group.shutdownGracefully();
		}
	}
}