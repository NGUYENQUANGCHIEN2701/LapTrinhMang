package com.tankzors.client.session;

import com.tankzors.client.gameplay.result.MatchResult;

/** Async account I/O implemented by the browser; core has no HTTP dependency. */
public interface CampaignPersistence {
    interface Callback {
        void saved(int coins, int stars, long revision);
        void failed(String reason);
    }
    void sync(String runId, String operationsJson, Callback callback);
    void submit(MatchResult result, Callback callback);
    boolean isBusy();
    void retry();
}
