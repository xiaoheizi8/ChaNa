package com.chanacode.core.security;

import lombok.extern.slf4j.Slf4j;

import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class AuthManager {

    private final Map<String, User> users;
    private final Map<String, Token> tokens;
    private final Map<String, Permission> permissions;
    private final String secretKey;
    private final ScheduledExecutorService scheduler;
    
    private volatile boolean enabled;
    private static final long TOKEN_EXPIRE_MS = 24 * 60 * 60 * 1000L;

    public AuthManager(String secretKey) {
        this.secretKey = secretKey;
        this.users = new ConcurrentHashMap<>();
        this.tokens = new ConcurrentHashMap<>();
        this.permissions = new ConcurrentHashMap<>();
        this.scheduler = Executors.newScheduledThreadPool(1);
        
        initDefaultUser();
        startTokenCleanup();
    }

    private void initDefaultUser() {
        users.put("admin", new User("admin", hashPassword("admin"), "ADMIN"));
        users.put("guest", new User("guest", hashPassword("guest"), "READ_ONLY"));
        log.info("Default users initialized: admin, guest");
    }

    public String login(String username, String password) {
        User user = users.get(username);
        if (user == null) {
            log.warn("Login failed: user not found - {}", username);
            return null;
        }
        
        if (!verifyPassword(password, user.password)) {
            log.warn("Login failed: invalid password - {}", username);
            return null;
        }
        
        String token = generateToken(username);
        Token tokenObj = new Token(token, username, System.currentTimeMillis() + TOKEN_EXPIRE_MS);
        tokens.put(token, tokenObj);
        
        log.info("User logged in: {}", username);
        return token;
    }

    public boolean logout(String token) {
        Token removed = tokens.remove(token);
        if (removed != null) {
            log.info("User logged out: {}", removed.username);
            return true;
        }
        return false;
    }

    public boolean validateToken(String token) {
        Token tokenObj = tokens.get(token);
        if (tokenObj == null) {
            return false;
        }
        if (System.currentTimeMillis() > tokenObj.expireTime) {
            tokens.remove(token);
            return false;
        }
        return true;
    }

    public String getUsername(String token) {
        Token tokenObj = tokens.get(token);
        return tokenObj != null ? tokenObj.username : null;
    }

    public boolean hasPermission(String token, String permission) {
        Token tokenObj = tokens.get(token);
        if (tokenObj == null) {
            return false;
        }
        
        User user = users.get(tokenObj.username);
        if (user == null) {
            return false;
        }
        
        Permission userPerm = permissions.get(user.role);
        return userPerm != null && userPerm.implies(permission);
    }

    public boolean registerUser(String username, String password, String role) {
        if (users.containsKey(username)) {
            return false;
        }
        users.put(username, new User(username, hashPassword(password), role));
        log.info("User registered: {} with role {}", username, role);
        return true;
    }

    public boolean deleteUser(String username) {
        if ("admin".equals(username)) {
            return false;
        }
        User removed = users.remove(username);
        if (removed != null) {
            tokens.entrySet().removeIf(e -> e.getValue().username.equals(username));
            return true;
        }
        return false;
    }

    public List<String> getUsers() {
        return new ArrayList<>(users.keySet());
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    private String generateToken(String username) {
        String data = username + System.currentTimeMillis() + secretKey;
        return sha256(data);
    }

    private String hashPassword(String password) {
        return sha256(password + secretKey);
    }

    private boolean verifyPassword(String password, String hash) {
        return hashPassword(password).equals(hash);
    }

    private String sha256(String data) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(data.getBytes());
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    private void startTokenCleanup() {
        scheduler.scheduleAtFixedRate(() -> {
            long now = System.currentTimeMillis();
            tokens.entrySet().removeIf(e -> e.getValue().expireTime < now);
        }, 1, 1, TimeUnit.HOURS);
    }

    public void shutdown() {
        scheduler.shutdown();
    }

    static class User {
        final String username;
        final String password;
        final String role;

        User(String username, String password, String role) {
            this.username = username;
            this.password = password;
            this.role = role;
        }
    }

    static class Token {
        final String token;
        final String username;
        final long expireTime;

        Token(String token, String username, long expireTime) {
            this.token = token;
            this.username = username;
            this.expireTime = expireTime;
        }
    }

    static class Permission {
        final String name;
        final Set<String> implies;

        Permission(String name, String... implies) {
            this.name = name;
            this.implies = new HashSet<>(Arrays.asList(implies));
        }

        boolean implies(String permission) {
            return name.equals(permission) || implies.contains(permission);
        }
    }

    public static class Builder {
        private String secretKey = "chana-default-secret-key";
        
        public Builder secretKey(String key) {
            this.secretKey = key;
            return this;
        }
        
        public AuthManager build() {
            return new AuthManager(secretKey);
        }
    }
}
