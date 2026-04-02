package com.chanacode.core.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class ConfigManagerTest {

    private ConfigManager configManager;

    @BeforeEach
    void setUp() {
        configManager = ConfigManager.getInstance();
        configManager.getAllConfigs().keySet().forEach(key -> {
            String[] parts = key.split("\\.");
            if (parts.length >= 2) {
                configManager.removeConfig(parts[0], parts[1]);
            }
        });
    }

    @Test
    void testPublishAndGetConfig() {
        String dataId = "test-config";
        String group = "DEFAULT_GROUP";
        String content = "test-content-value";

        boolean result = configManager.publishConfig(dataId, group, content);
        assertTrue(result);

        String retrieved = configManager.getConfig(dataId, group);
        assertEquals(content, retrieved);
    }

    @Test
    void testGetConfigNotFound() {
        String retrieved = configManager.getConfig("non-existent", "DEFAULT_GROUP");
        assertNull(retrieved);
    }

    @Test
    void testUpdateConfig() {
        String dataId = "update-test";
        String group = "DEFAULT_GROUP";

        configManager.publishConfig(dataId, group, "v1");
        configManager.publishConfig(dataId, group, "v2");

        String retrieved = configManager.getConfig(dataId, group);
        assertEquals("v2", retrieved);
    }

    @Test
    void testRemoveConfig() {
        String dataId = "remove-test";
        String group = "DEFAULT_GROUP";

        configManager.publishConfig(dataId, group, "content");
        assertNotNull(configManager.getConfig(dataId, group));

        boolean removed = configManager.removeConfig(dataId, group);
        assertTrue(removed);
        assertNull(configManager.getConfig(dataId, group));
    }

    @Test
    void testRemoveConfigNotFound() {
        boolean removed = configManager.removeConfig("non-existent", "DEFAULT_GROUP");
        assertFalse(removed);
    }

    @Test
    void testGetConfigsByGroup() {
        configManager.publishConfig("config1", "GROUP_A", "content1");
        configManager.publishConfig("config2", "GROUP_A", "content2");
        configManager.publishConfig("config3", "GROUP_B", "content3");

        List<ConfigManager.ConfigInfo> groupAConfigs = configManager.getConfigsByGroup("GROUP_A");
        assertEquals(2, groupAConfigs.size());
    }

    @Test
    void testGetAllConfigs() {
        configManager.publishConfig("c1", "G1", "v1");
        configManager.publishConfig("c2", "G2", "v2");

        Map<String, Object> all = configManager.getAllConfigs();
        assertEquals(2, all.size());
        assertEquals("v1", all.get("c1.G1"));
        assertEquals("v2", all.get("c2.G2"));
    }

    @Test
    void testConfigListener() {
        String dataId = "listener-test";
        String group = "DEFAULT_GROUP";
        AtomicBoolean notified = new AtomicBoolean(false);

        ConfigManager.ConfigListener listener = (d, g, c) -> {
            assertEquals(dataId, d);
            assertEquals(group, g);
            assertEquals("new-content", c);
            notified.set(true);
        };

        configManager.addListener(dataId, group, listener);
        configManager.publishConfig(dataId, group, "new-content");

        assertTrue(notified.get());
    }

    @Test
    void testRemoveListener() {
        String dataId = "remove-listener-test";
        String group = "DEFAULT_GROUP";
        AtomicBoolean notified = new AtomicBoolean(false);

        ConfigManager.ConfigListener listener = (d, g, c) -> notified.set(true);
        configManager.addListener(dataId, group, listener);
        configManager.removeListener(dataId, group, listener);

        configManager.publishConfig(dataId, group, "content");
        assertFalse(notified.get());
    }

    @Test
    void testGetConfigCount() {
        int initialCount = configManager.getConfigCount();

        configManager.publishConfig("count1", "G1", "v1");
        configManager.publishConfig("count2", "G2", "v2");

        assertEquals(initialCount + 2, configManager.getConfigCount());
    }
}
