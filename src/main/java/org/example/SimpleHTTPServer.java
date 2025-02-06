package org.example;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.util.Arrays;

public class SimpleHTTPServer {
    private static final int PORT = 8080;

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server started on port " + PORT);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                new Thread(() -> handleClient(clientSocket)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void handleClient(Socket clientSocket) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             OutputStream out = clientSocket.getOutputStream();
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out))) {

            String requestLine = in.readLine();
            if (requestLine == null) return;

            System.out.println("Request: " + requestLine);
            String sessionToken = null;

            String line;
            while (!(line = in.readLine()).isEmpty()) {
                if (line.startsWith("Cookie:")) {
                    sessionToken = extractSessionToken(line);
                }
            }

            if (requestLine.startsWith("GET /")) {
                serveStaticFile("index.html", out);
            } else if (requestLine.startsWith("GET /") && requestLine.contains(".")) {
                serveStaticFile(requestLine.split(" ")[1].substring(1), out);
            } else if (requestLine.startsWith("POST /login")) {
                UserService.handleLogin(in, writer);
            } else if (requestLine.startsWith("POST /logout")) {
                if (sessionToken != null) {
                    UserService.handleLogout(writer, sessionToken);
                } else {
                    writer.write("HTTP/1.1 400 Bad Request\r\n\r\nNo session found");
                }
                writer.flush();
            } else if (requestLine.startsWith("POST /register")) {
                UserService.handleRegister(in, writer, sessionToken);
            } else {
                sendNotFound(out);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void serveStaticFile(String resource, OutputStream out) throws IOException {
        File file = new File("src/main/resources/static/" + resource);
        if (!file.exists()) {
            sendNotFound(out);
            return;
        }

        String contentType = getContentType(resource);
        out.write(("HTTP/1.1 200 OK\r\nContent-Type: " + contentType + "\r\n\r\n").getBytes());
        Files.copy(file.toPath(), out);
    }

    private static void sendNotFound(OutputStream out) throws IOException {
        String response = "HTTP/1.1 404 Not Found\r\nContent-Type: text/html\r\n\r\n" +
                "<html><body><h1>404 Not Found</h1></body></html>";
        out.write(response.getBytes());
    }

    private static String getContentType(String fileName) {
        if (fileName.endsWith(".html")) return "text/html";
        if (fileName.endsWith(".css")) return "text/css";
        if (fileName.endsWith(".js")) return "application/javascript";
        return "text/plain";
    }
    private static String extractSessionToken(String cookieHeader) {
        String[] cookies = cookieHeader.split(";");
        for (String cookie : cookies) {
            String trimmed = cookie.trim();
            if (trimmed.startsWith("session=")) {
                return trimmed.substring(8);
            }
        }
        return null;
    }

}
