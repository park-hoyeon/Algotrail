package com.algotrail.backend.domain.github.dto;

public record SyncResult(
        int addedCount,
        int skippedCount
) {
    public static SyncResult added() {
        return new SyncResult(1, 0);
    }

    public static SyncResult skipped() {
        return new SyncResult(0, 1);
    }

    public static SyncResult none() {
        return new SyncResult(0, 0);
    }

    public SyncResult plus(SyncResult other) {
        return new SyncResult(
                this.addedCount + other.addedCount,
                this.skippedCount + other.skippedCount
        );
    }
}