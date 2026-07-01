package org.peregrinusrealm;

import org.peregrinusrealm.conn.CoreClient;
import org.peregrinusrealm.conn.CoreServer;

public class Test {

	public static void main(String[] args) throws Exception {
		CoreServer server = new CoreServer();

		// 🌟 1. 必須在 Server 啟動前，先把連線監聽器掛好！
		server.setTcpConnectedListener((tcpSession) -> {
			System.out.println("[Server 偵測] 有新的 TCP Client 連線進來了！");
			// 當有 Client 連上，立刻幫這個 Session 掛上訊息接收器
			tcpSession.setReciver((data) -> {
				System.out.println("Server TCP Received: " + new String(data));
				// 回覆給 Client
				tcpSession.send("Hi TCP Client".getBytes());
			});
		});

		server.setUdpConnectedListener((udpSession) -> {
			System.out.println("[Server 偵測] 有新的 UDP Client 傳送封包（建立虛擬 Session）！");
			// 當有新的 UDP 虛擬連線建立，立刻掛上接收器
			udpSession.setReciver((data) -> {
				System.out.println("Server UDP Received: " + new String(data));
				// 回覆給 Client
				udpSession.send("Hi UDP Client".getBytes());
			});
		});

		// 🌟 2. 用另一個執行緒啟動 Server，絕對不能擋到 main 執行緒
		new Thread(() -> {
			try {
				System.out.println("[Test] 正在啟動伺服器...");
				server.start();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}).start();

		// 給伺服器 1 秒鐘時間初始化完成綁定 Port
		Thread.sleep(1000);

		// 🌟 3. 建立並配置 Client
		CoreClient client = new CoreClient();

		client.setTcpReciver((data) -> {
			System.out.println("Client TCP Received: " + new String(data));
		});

		client.setUdpReciver((data) -> {
			System.out.println("Client UDP Received: " + new String(data));
		});

		// 🌟 4. Client 開始連線並發送資料
		System.out.println("[Test] Client 開始連線 TCP...");
		client.connectTcp((success, throwable) -> {
			System.out.println("Client Connect TCP Result: " + success);
			if (success) {
				// 連線成功，立刻噴一發封包過去
				client.tcpSend("Hello TCP Server".getBytes());
			} else if (throwable != null) {
				throwable.printStackTrace();
			}
		});

		System.out.println("[Test] Client 開始準備 UDP...");
		client.connectUdp((success, throwable) -> {
			System.out.println("Client Connect UDP Result: " + success);
			if (success) {
				// UDP 就緒，噴一發封包，這會觸發 Server 建立虛擬 Session
				client.udpSend("Hello UDP Server".getBytes());
			} else if (throwable != null) {
				throwable.printStackTrace();
			}
		});

		// 讓主執行緒睡著，觀察主控台雙向傳輸的結果
		Thread.sleep(5000);
		System.out.println("[Test] 測試結束。");
	}
}