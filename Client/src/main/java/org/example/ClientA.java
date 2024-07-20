package org.example;

import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.concurrent.CountDownLatch;

/**
 * 한 라인에 80 바이트인 데이터 백만줄 수신 시간 측정
 */
public class ClientA {

    private static final String SERVER_URI = "ws://localhost:8282/data";
    private static final int EXPECTED_BUNDLES = 20000; // 예상되는 데이터 묶음 수

    public static void main(String[] args) throws Exception {
        CountDownLatch latch = new CountDownLatch(1);

        StandardWebSocketClient client = new StandardWebSocketClient();
        long startTime = System.currentTimeMillis();

        client.doHandshake(new TextWebSocketHandler() {
            private int receivedBundles = 0;
            private long totalBytesReceived = 0;

            @Override
            protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
                receivedBundles++;
                totalBytesReceived += message.getPayloadLength();

                if (receivedBundles >= EXPECTED_BUNDLES) {
                    long endTime = System.currentTimeMillis();
                    long totalTime = endTime - startTime;
                    System.out.println("총 수신 데이터 크기: " + totalBytesReceived + " bytes");
                    System.out.println("총 수신 시간: " + totalTime + " ms");
                    session.close();
                    latch.countDown();
                }
            }

            @Override
            public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
                System.out.println("연결 종료됨");
            }

            @Override
            public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
                System.out.println("연결 오류 발생");
                exception.printStackTrace();
            }

        }, SERVER_URI);

        latch.await(); // latch가 완료될 때까지 기다림
    }
}
