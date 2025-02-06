package org.example;

import org.mindrot.jbcrypt.BCrypt;

import java.io.*;
import java.sql.*;
import java.util.regex.Pattern;

public class UserService {

    public static void handleRegister(BufferedReader in, BufferedWriter out, String sessionId) throws IOException {
        String requestBody = readRequestBody(in);
        String[] params = requestBody.split("&");

        String email = getParamValue(params, "email");
        String password = getParamValue(params, "password");
        String fullName = getParamValue(params, "fullName");
        String userCaptcha = getParamValue(params, "captcha");

        if (email.isEmpty() || password.isEmpty() || fullName.isEmpty() || userCaptcha.isEmpty()) {
            out.write("HTTP/1.1 400 Bad Request\r\n\r\nAll fields are required, including captcha");
            return;
        }

        if (!isValidEmail(email)) {
            out.write("HTTP/1.1 400 Bad Request\r\n\r\nInvalid email format");
            return;
        }

        if (password.length() < 8) {
            out.write("HTTP/1.1 400 Bad Request\r\n\r\nPassword must be at least 8 characters long");
            return;
        }

        if (!CaptchaService.validateCaptcha(sessionId, userCaptcha)) {
            out.write("HTTP/1.1 400 Bad Request\r\n\r\nInvalid captcha");
            return;
        }

        CaptchaService.removeCaptcha(sessionId);

        String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement("INSERT INTO users (email, password_hash, full_name) VALUES (?, ?, ?)")) {
            stmt.setString(1, email);
            stmt.setString(2, hashedPassword);
            stmt.setString(3, fullName);
            stmt.executeUpdate();
            out.write("HTTP/1.1 201 Created\r\n\r\nUser registered successfully");
        } catch (SQLException e) {
            e.printStackTrace();
            out.write("HTTP/1.1 500 Internal Server Error\r\n\r\nDatabase error");
        }
    }


    private static boolean isValidEmail(String email) {
        String emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$";
        Pattern pattern = Pattern.compile(emailRegex);
        return pattern.matcher(email).matches();
    }


    public static void handleLogin(BufferedReader in, BufferedWriter out) throws IOException {
        String requestBody = readRequestBody(in);
        String[] params = requestBody.split("&");

        String email = getParamValue(params, "email");
        String password = getParamValue(params, "password");

        if (email.isEmpty() || password.isEmpty()) {
            out.write("HTTP/1.1 400 Bad Request\r\n\r\nEmail and password are required");
            return;
        }

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT password_hash FROM users WHERE email = ?")) {
            stmt.setString(1, email);
            ResultSet rs = stmt.executeQuery();

            if (!rs.next()) {
                out.write("HTTP/1.1 401 Unauthorized\r\n\r\nInvalid credentials");
                return;
            }

            String storedHash = rs.getString("password_hash");
            if (!BCrypt.checkpw(password, storedHash)) {
                out.write("HTTP/1.1 401 Unauthorized\r\n\r\nInvalid credentials");
                return;
            }


            String token = SessionService.createSession(email);

            out.write("HTTP/1.1 200 OK\r\n");
            out.write("Set-Cookie: session=" + token + "; Path=/; HttpOnly\r\n");
            out.write("\r\nLogin successful");
        } catch (SQLException e) {
            e.printStackTrace();
            out.write("HTTP/1.1 500 Internal Server Error\r\n\r\nDatabase error");
        }
    }

    public static void handleUpdateProfile(BufferedReader in, BufferedWriter out, String token) throws IOException {
        String email = SessionService.getUserEmail(token);
        if (email == null) {
            out.write("HTTP/1.1 401 Unauthorized\r\n\r\nInvalid session");
            return;
        }

        String requestBody = readRequestBody(in);
        String[] params = requestBody.split("&");

        String newFullName = getParamValue(params, "fullName");
        String newPassword = getParamValue(params, "password");

        if (newFullName.isEmpty() && newPassword.isEmpty()) {
            out.write("HTTP/1.1 400 Bad Request\r\n\r\nNo changes provided");
            return;
        }

        try (Connection conn = Database.getConnection();
             PreparedStatement stmt = conn.prepareStatement("UPDATE users SET full_name = ?, password_hash = ? WHERE email = ?")) {
            stmt.setString(1, newFullName);
            stmt.setString(2, BCrypt.hashpw(newPassword, BCrypt.gensalt()));
            stmt.setString(3, email);
            stmt.executeUpdate();
            out.write("HTTP/1.1 200 OK\r\n\r\nProfile updated successfully");
        } catch (SQLException e) {
            e.printStackTrace();
            out.write("HTTP/1.1 500 Internal Server Error\r\n\r\nDatabase error");
        }
    }


    private static String readRequestBody(BufferedReader in) throws IOException {
        StringBuilder requestBody = new StringBuilder();
        String line;
        while (!(line = in.readLine()).isEmpty()) {
            requestBody.append(line);
        }
        return requestBody.toString();
    }

    private static String getParamValue(String[] params, String key) {
        for (String param : params) {
            String[] pair = param.split("=");
            if (pair.length == 2 && pair[0].equals(key)) {
                return pair[1];
            }
        }
        return "";
    }

    public static void handleLogout(BufferedWriter out, String token) throws IOException {
        SessionService.removeSession(token);
        out.write("HTTP/1.1 200 OK\r\n");
        out.write("Set-Cookie: session=deleted; Path=/; HttpOnly; Max-Age=0\r\n");
        out.write("\r\nLogged out successfully");
    }

}
