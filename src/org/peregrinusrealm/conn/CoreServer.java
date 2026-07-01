package org.peregrinusrealm.conn;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.AttributeKey;

public class CoreServer {

	// 🌟 Netty 狀態綁定標籤：將 TCP Session 綁定在 Channel 記憶體中
	private static final AttributeKey<CoreClientTcpSession> TCP_SESSION_KEY = AttributeKey.valueOf("TcpSession");

	private final boolean local;
	private final int tcpPort;
	private final int udpPort;

	private Consumer<CoreClientTcpSession> tcpConnectedListener = null;
	private Consumer<CoreClientUdpSession> udpConnectedListener = null;

	// 🌟 UDP 虛擬 Session 管理表（執行緒安全）
	private final Map<InetSocketAddress, CoreClientUdpSession> udpSessions = new ConcurrentHashMap<>();
	private final ScheduledExecutorService udpCleanerExecutor = Executors.newSingleThreadScheduledExecutor();

	public CoreServer() {
		this.local = true;
		this.tcpPort = 25565;
		this.udpPort = 25566;
	}

	public CoreServer(boolean local, int tcpPort, int udpPort) {
		this.local = local;
		this.tcpPort = tcpPort;
		this.udpPort = udpPort;
	}

	public void setTcpConnectedListener(Consumer<CoreClientTcpSession> tcpConnectedListener) {
		this.tcpConnectedListener = tcpConnectedListener;
	}

	public void setUdpConnectedListener(Consumer<CoreClientUdpSession> udpConnectedListener) {
		this.udpConnectedListener = udpConnectedListener;
	}

	public void start() throws InterruptedException {
		EventLoopGroup bossGroup = new MultiThreadIoEventLoopGroup(1, NioIoHandler.newFactory());
		EventLoopGroup workerGroup = new MultiThreadIoEventLoopGroup(NioIoHandler.newFactory());

		// 啟動 UDP 閒置過期檢查器（例如：每 10 秒檢查一次，超過 30 秒沒動靜就淘汰）
		startUdpCleanupTask(30000);

		try {
			// 1. TCP 伺服器配置
			ServerBootstrap tcpBootstrap = new ServerBootstrap().group(bossGroup, workerGroup).channel(NioServerSocketChannel.class).childHandler(new ChannelInitializer<SocketChannel>() {
				@Override
				protected void initChannel(SocketChannel ch) {
					// 處理連線生命週期
					ch.pipeline().addLast(new ChannelInboundHandlerAdapter() {
						@Override
						public void channelActive(ChannelHandlerContext ctx) throws Exception {
							// 連線建立：New 出 Session 並放入 Channel 的屬性中
							CoreClientTcpSession session = new CoreClientTcpSession(ctx.channel());
							ctx.channel().attr(TCP_SESSION_KEY).set(session);

							// 對外拋出新連線通知
							if (tcpConnectedListener != null) {
								tcpConnectedListener.accept(session);
							}
							super.channelActive(ctx);
						}

						@Override
						public void channelInactive(ChannelHandlerContext ctx) throws Exception {
							// 連線中斷：可在此做 Session 的收尾與記憶體釋放
							ctx.channel().attr(TCP_SESSION_KEY).set(null);
							super.channelInactive(ctx);
						}
					});

					// 處理訊息讀取
					ch.pipeline().addLast(new SimpleChannelInboundHandler<ByteBuf>() {
						@Override
						protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) {
							CoreClientTcpSession session = ctx.channel().attr(TCP_SESSION_KEY).get();
							if (session != null) {
								byte[] data = new byte[msg.readableBytes()];
								msg.readBytes(data);
								session.handleMessage(data); // 丟給對應的 Session 處理
							}
						}
					});
				}
			});

			// 2. UDP 伺服器配置
			Bootstrap udpBootstrap = new Bootstrap().group(workerGroup).channel(NioDatagramChannel.class).handler(new SimpleChannelInboundHandler<DatagramPacket>() {
				@Override
				protected void channelRead0(ChannelHandlerContext ctx, DatagramPacket packet) {
					InetSocketAddress sender = packet.sender();
					ByteBuf content = packet.content();
					byte[] data = new byte[content.readableBytes()];
					content.readBytes(data);

					// 檢查此地址是否已經建立了虛擬 Session
					CoreClientUdpSession session = udpSessions.get(sender);
					if (session == null) {
						// 第一次看到這個 IP:Port，視為新連線
						session = new CoreClientUdpSession(ctx.channel(), sender);
						udpSessions.put(sender, session);

						// 對外拋出新連線通知
						if (udpConnectedListener != null) {
							udpConnectedListener.accept(session);
						}
					}

					// 將訊息交給虛擬 Session 派發
					session.handleMessage(data);
				}
			});

			String bindAddress = this.local ? "127.0.0.1" : "0.0.0.0";

			ChannelFuture tcpFuture = tcpBootstrap.bind(new InetSocketAddress(bindAddress, tcpPort)).sync();
			udpBootstrap.bind(new InetSocketAddress(bindAddress, udpPort)).sync();

			System.out.println("====== Netty 雙通道 Session 伺服器啟動 ======");
			tcpFuture.channel().closeFuture().sync();
		} finally {
			udpCleanerExecutor.shutdownNow();
			bossGroup.shutdownGracefully();
			workerGroup.shutdownGracefully();
		}
	}

	// 🌟 定時定點清理太久沒發送 UDP 的 Client (防止 OOM)
	private void startUdpCleanupTask(long timeoutMs) {
		udpCleanerExecutor.scheduleAtFixedRate(() -> {
			long now = System.currentTimeMillis();
			udpSessions.entrySet().removeIf(entry -> {
				boolean isExpired = (now - entry.getValue().getLastAccessTime()) > timeoutMs;
				if (isExpired) {
					System.out.println("[Server UDP] 清除超時的虛擬會話: " + entry.getKey());
				}
				return isExpired;
			});
		}, 10, 10, TimeUnit.SECONDS);
	}
}