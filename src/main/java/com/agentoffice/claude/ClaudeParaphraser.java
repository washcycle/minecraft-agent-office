package com.agentoffice.claude;

import okhttp3.*;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * Calls the Anthropic Messages API to produce a ≤10-word, first-person,
 * present-tense paraphrase of a beads task suitable for a floating NPC label.
 *
 * Results are cached by (taskId + titleHash) so repeated calls for unchanged
 * tasks do not make additional HTTP requests.
 */
public class ClaudeParaphraser {

    private static final String API_URL = "https://api.anthropic.com/v1/messages";
    private static final String MODEL = "claude-haiku-4-5-20251001";
    private static final MediaType JSON_MEDIA = MediaType.get("application/json; charset=utf-8");
    private static final int CACHE_MAX = 100;
    private static final int TIMEOUT_SECONDS = 5;

    private final String apiKey;
    private final Logger logger;
    private final OkHttpClient http;
    private final Gson gson = new Gson();

    // LRU cache: key = taskId:titleHash, value = paraphrase
    private final Map<String, String> cache = new LinkedHashMap<>(CACHE_MAX, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, String> eldest) {
            return size() > CACHE_MAX;
        }
    };

    public ClaudeParaphraser(String apiKey, Logger logger) {
        this.apiKey = apiKey;
        this.logger = logger;
        this.http = new OkHttpClient.Builder()
                .callTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .build();
    }

    /**
     * Asynchronously paraphrases the task. Returns a CompletableFuture that
     * resolves to the paraphrase, or the raw title if the API call fails.
     * Never blocks the calling thread.
     */
    public CompletableFuture<String> paraphrase(String taskId, String title, String description) {
        String cacheKey = taskId + ":" + Integer.toHexString(title.hashCode());

        synchronized (cache) {
            String cached = cache.get(cacheKey);
            if (cached != null) {
                return CompletableFuture.completedFuture(cached);
            }
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                String result = callApi(title, description);
                synchronized (cache) {
                    cache.put(cacheKey, result);
                }
                return result;
            } catch (Exception e) {
                logger.warning("[AgentOffice] ClaudeParaphraser failed for task " + taskId
                        + " — using raw title. Error: " + e.getMessage());
                return title;
            }
        });
    }

    private String callApi(String title, String description) throws IOException {
        String prompt = "Summarise this task in ≤10 words, first-person present tense: "
                + title + " — " + (description.isBlank() ? "(no description)" : description);

        JsonObject message = new JsonObject();
        message.addProperty("role", "user");
        message.addProperty("content", prompt);

        JsonArray messages = new JsonArray();
        messages.add(message);

        JsonObject body = new JsonObject();
        body.addProperty("model", MODEL);
        body.addProperty("max_tokens", 30);
        body.add("messages", messages);

        Request request = new Request.Builder()
                .url(API_URL)
                .post(RequestBody.create(gson.toJson(body), JSON_MEDIA))
                .addHeader("x-api-key", apiKey)
                .addHeader("anthropic-version", "2023-06-01")
                .addHeader("content-type", "application/json")
                .build();

        try (Response response = http.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                throw new IOException("API returned HTTP " + response.code());
            }
            String responseBody = response.body().string();
            JsonObject json = gson.fromJson(responseBody, JsonObject.class);
            return json.getAsJsonArray("content")
                    .get(0).getAsJsonObject()
                    .get("text").getAsString()
                    .trim();
        }
    }
}
