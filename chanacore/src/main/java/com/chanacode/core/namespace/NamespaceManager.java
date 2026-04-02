package com.chanacode.core.namespace;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class NamespaceManager {

    private static NamespaceManager instance;
    private final ConcurrentHashMap<String, Namespace> namespaces;

    private NamespaceManager() {
        this.namespaces = new ConcurrentHashMap<>();
        namespaces.put("public", new Namespace("public", "default"));
    }

    public static synchronized NamespaceManager getInstance() {
        if (instance == null) {
            instance = new NamespaceManager();
        }
        return instance;
    }

    public boolean createNamespace(String name, String description) {
        if (namespaces.containsKey(name)) {
            return false;
        }
        namespaces.put(name, new Namespace(name, description));
        return true;
    }

    public boolean deleteNamespace(String name) {
        if ("public".equals(name)) {
            return false;
        }
        return namespaces.remove(name) != null;
    }

    public Namespace getNamespace(String name) {
        return namespaces.get(name);
    }

    public boolean exists(String name) {
        return namespaces.containsKey(name);
    }

    public java.util.Set<String> getAllNamespaces() {
        return namespaces.keySet();
    }

    public static class Namespace {
        private final String name;
        private final String description;
        private final CopyOnWriteArrayList<String> services;

        public Namespace(String name, String description) {
            this.name = name;
            this.description = description;
            this.services = new CopyOnWriteArrayList<>();
        }

        public String getName() { return name; }
        public String getDescription() { return description; }
        public CopyOnWriteArrayList<String> getServices() { return services; }
    }
}
