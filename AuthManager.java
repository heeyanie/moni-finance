package app;

import java.util.HashMap;
import java.util.Map;

public class AuthManager {
    private static Map<String, User> userDatabase = new HashMap<>();

    static {
        userDatabase.put("2026-1001", new User("2026-1001", "Demo Student", "student@mapua.edu.ph", "pass123"));
    }

    public static boolean register(String studentNumber, String fullName, String email, String password) {
        if (studentNumber.isEmpty() || fullName.isEmpty() || email.isEmpty() || password.isEmpty()) {
            return false;
        }
        if (userDatabase.containsKey(studentNumber)) {
            return false;
        }
        userDatabase.put(studentNumber, new User(studentNumber, fullName, email, password));
        return true;
    }

    public static User login(String studentNumber, String password) {
        User user = userDatabase.get(studentNumber);
        if (user != null && user.getPassword().equals(password)) {
            return user;
        }
        return null;
    }
}