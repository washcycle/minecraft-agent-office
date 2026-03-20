package com.agentoffice.beads;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;

class BeadsCliClientTest {

    private static final Gson GSON = new Gson();

    /** Tests the JSON parsing logic in isolation using reflection to call the private method. */
    @Test
    void parsesJsonArrayCorrectly() throws Exception {
        JsonArray arr = new JsonArray();

        JsonObject t1 = new JsonObject();
        t1.addProperty("id", "proj-abc");
        t1.addProperty("title", "Fix login bug");
        t1.addProperty("description", "Users can't log in after password reset");
        t1.addProperty("assignee", "alice");
        t1.addProperty("status", "in_progress");
        arr.add(t1);

        JsonObject t2 = new JsonObject();
        t2.addProperty("id", "proj-def");
        t2.addProperty("title", "Write docs");
        t2.addProperty("description", "");
        t2.addProperty("status", "open");  // no assignee field
        arr.add(t2);

        String json = GSON.toJson(arr);

        // Expose parseJson via reflection
        var client = new BeadsCliClient("bd", java.util.logging.Logger.getLogger("test"));
        var method = BeadsCliClient.class.getDeclaredMethod("parseJson", String.class);
        method.setAccessible(true);

        @SuppressWarnings("unchecked")
        List<BeadsTask> tasks = (List<BeadsTask>) method.invoke(client, json);

        assertEquals(2, tasks.size());

        BeadsTask task1 = tasks.get(0);
        assertEquals("proj-abc", task1.id());
        assertEquals("Fix login bug", task1.title());
        assertEquals("alice", task1.assignee());
        assertEquals("in_progress", task1.status());

        BeadsTask task2 = tasks.get(1);
        assertEquals("proj-def", task2.id());
        assertNull(task2.assignee());
    }

    @Test
    void parsesEmptyJsonArrayToEmptyList() throws Exception {
        var client = new BeadsCliClient("bd", java.util.logging.Logger.getLogger("test"));
        var method = BeadsCliClient.class.getDeclaredMethod("parseJson", String.class);
        method.setAccessible(true);

        @SuppressWarnings("unchecked")
        List<BeadsTask> tasks = (List<BeadsTask>) method.invoke(client, "[]");
        assertTrue(tasks.isEmpty());
    }

    @Test
    void blankOutputReturnsEmptyList() throws Exception {
        var client = new BeadsCliClient("bd", java.util.logging.Logger.getLogger("test"));
        var method = BeadsCliClient.class.getDeclaredMethod("parseJson", String.class);
        method.setAccessible(true);

        @SuppressWarnings("unchecked")
        List<BeadsTask> tasks = (List<BeadsTask>) method.invoke(client, "");
        assertTrue(tasks.isEmpty());
    }
}
