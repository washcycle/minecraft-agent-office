package com.agentoffice.beads;

import java.util.List;

public interface BeadsClient {
    /** Returns all tasks currently in_progress. */
    List<BeadsTask> listInProgress() throws BeadsException;

    /** Returns all tasks regardless of status. */
    List<BeadsTask> listAll() throws BeadsException;
}
