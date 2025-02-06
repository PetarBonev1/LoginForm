package org.example;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SessionService {
    private static final ConcurrentHashMap<String, String> sessions = new ConcurrentHashMap<>();

    public static String createSession(String email) {
        String token = UUID.randomUUID().toString();
        sessions.put(token, email);
        return token;
    }

    public static String getUserEmail(String token) {
        return sessions.get(token);
    }

    public static void removeSession(String token) {
        sessions.remove(token);
    }
}
