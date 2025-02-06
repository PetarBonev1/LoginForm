package org.example;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.io.*;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class UserServiceTest {
    @BeforeEach
    void setUp() {
        UserService.users.clear();
    }

    @Test
    void testSuccessfulRegistration() throws IOException {
        String requestBody = "name=Petar&email=petar@example.com&password=12345";
        BufferedReader in = new BufferedReader(new StringReader(requestBody));
        StringWriter responseWriter = new StringWriter();
        BufferedWriter out = new BufferedWriter(responseWriter);

        UserService.handleRegister(in, out);
        out.flush();

        assertTrue(responseWriter.toString().contains("201 Created"));
        assertTrue(UserService.users.containsKey("petar@example.com"));
    }

    @Test
    void testRegistrationWithExistingEmail() throws IOException {
        UserService.users.put("petar@example.com", "12345");

        String requestBody = "name=Petar&email=petar@example.com&password=54321";
        BufferedReader in = new BufferedReader(new StringReader(requestBody));
        StringWriter responseWriter = new StringWriter();
        BufferedWriter out = new BufferedWriter(responseWriter);

        UserService.handleRegister(in, out);
        out.flush();

        assertTrue(responseWriter.toString().contains("409 Conflict"));
    }

    @Test
    void testInvalidEmailRegistration() throws IOException {
        String requestBody = "name=Petar&email=invalid-email&password=12345";
        BufferedReader in = new BufferedReader(new StringReader(requestBody));
        StringWriter responseWriter = new StringWriter();
        BufferedWriter out = new BufferedWriter(responseWriter);

        UserService.handleRegister(in, out);
        out.flush();

        assertTrue(responseWriter.toString().contains("400 Bad Request"));
    }
}
