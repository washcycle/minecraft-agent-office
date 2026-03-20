package com.agentoffice.session;

import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class SessionPollerTest {

    private Plugin mockPlugin;
    private Path tempFile;
    private Method tickMethod;

    @BeforeEach
    void setUp() throws Exception {
        mockPlugin = mock(Plugin.class);
        tempFile = Files.createTempFile("office-sessions-test", ".json");

        tickMethod = SessionPoller.class.getDeclaredMethod("tick");
        tickMethod.setAccessible(true);
    }

    @AfterEach
    void tearDown() throws Exception {
        if (tempFile != null) {
            Files.deleteIfExists(tempFile);
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /** Creates a SessionPoller pointing at tempFile. */
    private SessionPoller poller() {
        return new SessionPoller(mockPlugin, tempFile.toString(), 480, Logger.getLogger("test"));
    }

    /** Creates a SessionPoller pointing at a path that does not exist. */
    private SessionPoller pollerWithMissingFile() throws Exception {
        Path missing = tempFile.resolveSibling("does-not-exist-" + System.nanoTime() + ".json");
        return new SessionPoller(mockPlugin, missing.toString(), 480, Logger.getLogger("test"));
    }

    /** Invokes tick() via reflection, unwrapping any InvocationTargetException. */
    private void invokeTick(SessionPoller p) throws Exception {
        try {
            tickMethod.invoke(p);
        } catch (InvocationTargetException ite) {
            Throwable cause = ite.getCause();
            if (cause instanceof Exception ex) throw ex;
            if (cause instanceof Error err) throw err;
            throw ite;
        }
    }

    /** Reads the private knownProjects field from a SessionPoller instance. */
    @SuppressWarnings("unchecked")
    private Map<String, String> knownProjects(SessionPoller p) throws Exception {
        var field = SessionPoller.class.getDeclaredField("knownProjects");
        field.setAccessible(true);
        return (Map<String, String>) field.get(p);
    }

    /** Builds a JSON sessions payload with a single entry. */
    private String sessionJson(String path, String id, String started) {
        return "{\"sessions\":[{\"path\":\"" + path + "\",\"id\":\"" + id
                + "\",\"started\":\"" + started + "\"}]}";
    }

    // -------------------------------------------------------------------------
    // Tests
    // -------------------------------------------------------------------------

    @Test
    void detectsNewSession() throws Exception {
        String iso = Instant.now().toString();
        Files.writeString(tempFile, sessionJson("/projects/alpha", "sess-1", iso));

        SessionPoller p = poller();
        invokeTick(p);

        Map<String, String> known = knownProjects(p);
        assertTrue(known.containsKey("/projects/alpha"),
                "knownProjects should contain the newly started project");
        assertEquals("sess-1", known.get("/projects/alpha"));
    }

    @Test
    void detectsRemovedSession() throws Exception {
        String iso = Instant.now().toString();
        Files.writeString(tempFile, sessionJson("/projects/alpha", "sess-1", iso));

        SessionPoller p = poller();
        invokeTick(p);
        assertFalse(knownProjects(p).isEmpty(), "Pre-condition: session should be known after first tick");

        // Overwrite file with empty sessions array
        Files.writeString(tempFile, "{\"sessions\":[]}");
        invokeTick(p);

        assertTrue(knownProjects(p).isEmpty(),
                "knownProjects should be empty after the session disappears from the file");
    }

    @Test
    void handlesMissingFile() throws Exception {
        SessionPoller p = pollerWithMissingFile();

        // tick() must not throw even when the file is absent
        assertDoesNotThrow(() -> invokeTick(p));

        assertTrue(knownProjects(p).isEmpty(),
                "knownProjects should be empty when the sessions file does not exist");
    }

    @Test
    void expiresStaleSession() throws Exception {
        // Use a timestamp 9 hours in the past (> 480-minute expiry)
        String staleTimestamp = Instant.now().minus(9, ChronoUnit.HOURS).toString();
        Files.writeString(tempFile, sessionJson("/projects/alpha", "sess-old", staleTimestamp));

        SessionPoller p = poller();
        // First tick: the session is fresh relative to knownProjects (it isn't known yet),
        // but it is stale relative to the expiry cutoff — so it should be treated as stopped
        // and removed from knownProjects rather than added.
        invokeTick(p);

        assertFalse(knownProjects(p).containsKey("/projects/alpha"),
                "Stale session should not appear in knownProjects after tick");
    }
}
