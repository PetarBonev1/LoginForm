package org.example;

import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

public class CaptchaService {
    private static final ConcurrentHashMap<String, String> captchaStore = new ConcurrentHashMap<>();
    private static final Random random = new Random();

    public static String generateCaptcha(String sessionId) {
        String captcha = String.format("%04d", random.nextInt(10000));
        captchaStore.put(sessionId, captcha);
        return captcha;
    }

    public static boolean validateCaptcha(String sessionId, String userCaptcha) {
        String correctCaptcha = captchaStore.get(sessionId);
        return correctCaptcha != null && correctCaptcha.equals(userCaptcha);
    }

    public static void removeCaptcha(String sessionId) {
        captchaStore.remove(sessionId);
    }
}
