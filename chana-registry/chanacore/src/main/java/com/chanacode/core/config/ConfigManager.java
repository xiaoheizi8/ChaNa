package com.chanacode.core.config;

import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
public class ConfigManager {

    private static ConfigManager instance;

    private final ConcurrentHashMap<String, ConfigInfo> configs;
    private final ConcurrentHashMap<String, List<ConfigListener>> listeners;

    private ConfigManager() {
        this.configs = new ConcurrentHashMap<>();
        this.listeners = new ConcurrentHashMap<>();
    }

    public static synchronized ConfigManager getInstance() {
        if (instance == null) {
            instance = new ConfigManager();
        }
        return instance;
    }

    public String getConfig(String dataId, String group) {
        String key = buildKey(dataId, group);
        ConfigInfo config = configs.get(key);
        return config != null ? config.content : null;
    }

    public boolean publishConfig(String dataId, String group, String content) {
        String key = buildKey(dataId, group);
        ConfigInfo oldConfig = configs.get(key);
        
        ConfigInfo newConfig = new ConfigInfo(dataId, group, content, System.currentTimeMillis());
        configs.put(key, newConfig);
        
        log.info("Config published: {}.{}", dataId, group);
        
        notifyListeners(dataId, group, content);
        
        return true;
    }

    public boolean removeConfig(String dataId, String group) {
        String key = buildKey(dataId, group);
        ConfigInfo removed = configs.remove(key);
        
        if (removed != null) {
            log.info("Config removed: {}.{}", dataId, group);
            return true;
        }
        return false;
    }

    public List<ConfigInfo> getConfigsByGroup(String group) {
        return configs.values().stream()
                .filter(c -> c.group.equals(group))
                .toList();
    }

    public Map<String, Object> getAllConfigs() {
        Map<String, Object> result = new ConcurrentHashMap<>();
        for (ConfigInfo config : configs.values()) {
            String key = config.dataId + "." + config.group;
            result.put(key, config.content);
        }
        return result;
    }

    public void addListener(String dataId, String group, ConfigListener listener) {
        String key = buildKey(dataId, group);
        listeners.computeIfAbsent(key, k -> new CopyOnWriteArrayList<>()).add(listener);
    }

    public void removeListener(String dataId, String group, ConfigListener listener) {
        String key = buildKey(dataId, group);
        List<ConfigListener> list = listeners.get(key);
        if (list != null) {
            list.remove(listener);
        }
    }

    private void notifyListeners(String dataId, String group, String content) {
        String key = buildKey(dataId, group);
        List<ConfigListener> list = listeners.get(key);
        if (list != null) {
            for (ConfigListener listener : list) {
                try {
                    listener.onConfigChanged(dataId, group, content);
                } catch (Exception e) {
                    log.error("Config listener error for {}.{}", dataId, group, e);
                }
            }
        }
    }

    private String buildKey(String dataId, String group) {
        return dataId + "." + group;
    }

    public int getConfigCount() {
        return configs.size();
    }

    public static class ConfigInfo {
        public final String dataId;
        public final String group;
        public final String content;
        public final long lastModified;

        public ConfigInfo(String dataId, String group, String content, long lastModified) {
            this.dataId = dataId;
            this.group = group;
            this.content = content;
            this.lastModified = lastModified;
        }
    }

    public interface ConfigListener {
        void onConfigChanged(String dataId, String group, String content);
    }
}
