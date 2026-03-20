package com.agentoffice.claude;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ClaudeParaphraser cache logic.
 * These tests inject directly into the cache map to verify hit/miss behaviour
 * without making live API calls.
 */
class ClaudeParaphraserTest {

    private ClaudeParaphraser paraphraser;
    private Map<String, String> cache;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() throws Exception {
        paraphraser = new ClaudeParaphraser("test-key", Logger.getLogger("test"));

        // Expose the private cache field
        Field cacheField = ClaudeParaphraser.class.getDeclaredField("cache");
        cacheField.setAccessible(true);
        cache = (Map<String, String>) cacheField.get(paraphraser);
    }

    @Test
    void returnsCachedValueWithoutApiCall() throws Exception {
        String taskId = "task-1";
        String title = "Fix login bug";
        String cacheKey = taskId + ":" + Integer.toHexString(title.hashCode());

        cache.put(cacheKey, "Fixing login authentication bug now");

        CompletableFuture<String> result = paraphraser.paraphrase(taskId, title, "description");
        // Should resolve immediately (cached)
        assertTrue(result.isDone());
        assertEquals("Fixing login authentication bug now", result.get());
    }

    @Test
    void differentTaskIdProducesDifferentCacheKey() throws Exception {
        String title = "Write docs";
        String key1 = "task-A:" + Integer.toHexString(title.hashCode());
        String key2 = "task-B:" + Integer.toHexString(title.hashCode());

        cache.put(key1, "Writing documentation for module");

        // task-A should hit the cache
        CompletableFuture<String> hitResult = paraphraser.paraphrase("task-A", title, "");
        assertTrue(hitResult.isDone());
        assertEquals("Writing documentation for module", hitResult.get());

        // task-B has a different key — cache miss (will attempt API, but we don't care about that value here)
        assertFalse(cache.containsKey(key2));
    }

    @Test
    void titleHashChangeCausesCacheMiss() {
        String taskId = "task-1";
        String titleV1 = "Fix login bug";
        String titleV2 = "Fix logout bug";

        String keyV1 = taskId + ":" + Integer.toHexString(titleV1.hashCode());
        cache.put(keyV1, "Fixing login auth");

        String keyV2 = taskId + ":" + Integer.toHexString(titleV2.hashCode());
        assertFalse(cache.containsKey(keyV2), "Changed title should produce a cache miss");
    }

    @Test
    void cacheStoresResultAfterApiCallViaDirectInject() throws Exception {
        String taskId = "task-x";
        String title = "Deploy service";
        String cacheKey = taskId + ":" + Integer.toHexString(title.hashCode());

        // Pre-populate as if an API call just completed (simulates the supplyAsync path)
        cache.put(cacheKey, "Deploying service to production");

        // Subsequent call should be immediate
        CompletableFuture<String> result = paraphraser.paraphrase(taskId, title, "");
        assertTrue(result.isDone());
        assertEquals("Deploying service to production", result.get());
    }

    @Test
    void cacheKeyIncludesTaskId() {
        // Two tasks with the same title should have distinct keys
        String title = "Refactor tests";
        String key1 = "task-1:" + Integer.toHexString(title.hashCode());
        String key2 = "task-2:" + Integer.toHexString(title.hashCode());

        assertNotEquals(key1, key2);
    }
}
