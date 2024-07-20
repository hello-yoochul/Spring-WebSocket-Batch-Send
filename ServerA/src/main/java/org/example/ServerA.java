package org.example;//package org.example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 한 라인에 80 byte 정도되는 데이터를 100줄씩 묶음으로 (즉, 한 묶음 8KB) 1000번 전송. 총 백만줄 데이터 전송.
 */
@SpringBootApplication
public class ServerA {
    public static void main(String[] args) {
        SpringApplication.run(ServerA.class, args);
    }

    @Configuration
    @EnableWebSocket
    public class WebSocketConfig implements WebSocketConfigurer {

        @Override
        public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
            registry.addHandler(new DataWebSocketHandler(), "/data").setAllowedOrigins("*");
        }
    }

    public class DataWebSocketHandler extends TextWebSocketHandler {

        private ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();

        @Override
        public void afterConnectionEstablished(WebSocketSession session) throws Exception {
            System.out.println("클라이언트 접속 시작");
            executorService.scheduleAtFixedRate(new DataSender(session), 0, 1, TimeUnit.MILLISECONDS);
        }

        @Override
        public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
            System.out.println("클라이언트 접속 에러");
            session.close(CloseStatus.SERVER_ERROR);
        }

        @Override
        public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
            System.out.println("클라이언트 접속 끊어짐");
            executorService.shutdown();
        }

        private class DataSender implements Runnable {
            private final WebSocketSession session;
            private int bundleCount = 0;
            private final int totalBundles = 20000; // 총 보낼 묶음 (라인당 100개 보내면 100*10000=총 1백만개)
            private final int linesPerBundle = 50;  // 한번 보낼때 보낼 라인 수

            public DataSender(WebSocketSession session) {
                this.session = session;
            }

            @Override
            public void run() {
                try {
                    if (bundleCount < totalBundles) {
                        StringBuilder dataBundle = new StringBuilder();
                        int startLine = bundleCount * linesPerBundle + 1;

                        for (int i = 0; i < linesPerBundle; i++) {
                            dataBundle.append(startLine + i).append(" ")
                                    .append("FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF")
                                    .append("\n");
                        }

                        //System.out.println("송신 데이터 크기: " + dataBundle.length());
                        session.sendMessage(new TextMessage(dataBundle.toString()));
                        bundleCount++;
                    } else {
                        session.close(CloseStatus.NORMAL);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
