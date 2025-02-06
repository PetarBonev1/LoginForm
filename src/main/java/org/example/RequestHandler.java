package org.example;

import java.io.*;
import java.net.Socket;

public class RequestHandler implements Runnable {
    private final Socket clientSocket;

    public RequestHandler(Socket socket) {
        this.clientSocket = socket;
    }


    @Override
    public void run() {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             BufferedWriter out = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()))) {

            String requestLine = in.readLine();
            if (requestLine == null) return;

            String token = extractSessionToken(in);

            if (requestLine.startsWith("POST /login")) {
                UserService.handleLogin(in, out);
            } else if (requestLine.startsWith("POST /logout")) {
                UserService.handleLogout(out, token);
            } else if (requestLine.startsWith("POST /update-profile")) {
                UserService.handleUpdateProfile(in, out, token);
            } else {
                out.write("HTTP/1.1 404 Not Found\r\n\r\n");
            }

            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String extractSessionToken(BufferedReader in) throws IOException {
        String line;
        while (!(line = in.readLine()).isEmpty()) {
            if (line.startsWith("Cookie:")) {
                String[] cookies = line.substring(7).split(";");
                for (String cookie : cookies) {
                    String[] pair = cookie.trim().split("=");
                    if (pair[0].equals("session")) {
                        return pair[1];
                    }
                }
            }
        }
        return null;
    }


}
