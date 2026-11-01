package net.tgc.gamingblock_management.suspections;

import org.bukkit.entity.Player;

public class Suspection {
    private final Player target;
    private final Player issuer;
    private SuspectionType type;
    private String reason;
    private boolean finished;

    public Suspection(Player target, Player issuer, SuspectionType type, String reason) {
        this.target = target;
        this.issuer = issuer;
        this.type = type;
        this.reason = reason;
        this.finished = false;
    }

    public Player getTarget() {
        return target;
    }

    public Player getIssuer() {
        return issuer;
    }

    public SuspectionType getType() {
        return type;
    }

    public String getReason() {
        return reason;
    }

    public boolean isFinished() {
        return finished;
    }

    public Suspection setType(SuspectionType type) {
        this.type = type;
        return this;
    }

    public Suspection setReason(String reason) {
        this.reason = reason;
        return this;
    }

    public Suspection setFinished(boolean finished) {
        this.finished = finished;
        return this;
    }
}