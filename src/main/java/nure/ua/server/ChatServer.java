package nure.ua.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Основний клас сервера, що запускає сокет і приймає підключення клієнтів.
 */
public class ChatServer {
    private static final int PORT = 8000;


    /**
     * Точка входу. Запускає сервер на заданому порту та слухає нові з'єднання.
     *
     * @param args Аргументи командного рядка (не використовуються)
     */
    @SuppressWarnings("CallToPrintStackTrace")
    public static void main(String[] args) {
        try {
            ClientManager.initialize(); 
            System.out.println("Server started...");

            try (ServerSocket serverSocket = new ServerSocket(PORT)) {
                while (true) {
                    System.out.println("Waiting for client...");
                    Socket clientSocket = serverSocket.accept();
                    System.out.println("Client connected: " + clientSocket.getRemoteSocketAddress());
                    new ClientHandler(clientSocket).start();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
}
