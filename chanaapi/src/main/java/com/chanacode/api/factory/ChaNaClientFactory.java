package com.chanacode.api.factory;

import com.chanacode.api.client.ChaNaClient;
import java.util.concurrent.ConcurrentHashMap;

public class ChaNaClientFactory {

    private static final ConcurrentHashMap<String, ChaNaClient> clients = new ConcurrentHashMap<>();

    public static ChaNaClient getClient(String host, int port) {
        String key = host + ":" + port;
        return clients.computeIfAbsent(key, k -> new ChaNaClient(host, port));
    }

    public static void removeClient(String host, int port) {
        String key = host + ":" + port;
        ChaNaClient client = clients.remove(key);
        if (client != null) {
            client.close();
        }
    }

    public static void closeAll() {
        for (ChaNaClient client : clients.values()) {
            client.close();
        }
        clients.clear();
    }
}
