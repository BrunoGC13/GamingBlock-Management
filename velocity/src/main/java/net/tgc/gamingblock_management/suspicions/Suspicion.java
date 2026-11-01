package net.tgc.gamingblock_management.suspicions;

import java.util.UUID;

public class Suspicion {
    private final UUID targetUuid;
    private final String targetName;
    private final UUID issuerUuid;
    private final String issuerName;

    private SuspicionType type;
    private String reason;
    private boolean finished;

    public Suspicion(
            UUID targetUuid,
            String targetName,
            UUID issuerUuid,
            String issuerName,
            SuspicionType type,
            String reason
    ) {
        this.targetUuid = targetUuid;
        this.targetName = targetName;
        this.issuerUuid = issuerUuid;
        this.issuerName = issuerName;
        this.type = type;
        this.reason = reason;
        this.finished = false;
    }

    public UUID getTargetUuid() {
        return targetUuid;
    }

    public String getTargetName() {
        return targetName;
    }

    public UUID getIssuerUuid() {
        return issuerUuid;
    }

    public String getIssuerName() {
        return issuerName;
    }

    public SuspicionType getType() {
        return type;
    }

    public String getReason() {
        return reason;
    }

    public boolean isFinished() {
        return finished;
    }

    public Suspicion setType(SuspicionType type) {
        this.type = type;
        return this;
    }

    public Suspicion setReason(String reason) {
        this.reason = reason;
        return this;
    }

    public Suspicion setFinished(boolean finished) {
        this.finished = finished;
        return this;
    }
}