package net.tgc.gamingblock_management.punishments;

public class PunishmentResult {
    private Punishment punishment;
    private boolean successful;

    public PunishmentResult(Punishment punishment, boolean successful) {
        this.punishment = punishment;
        this.successful = successful;
    }

    public Punishment getPunishment() {
        return punishment;
    }

    public boolean isSuccessful() {
        return successful;
    }

    public PunishmentResult setSuccessful(boolean successful) {
        this.successful = successful;
        return this;
    }
}
