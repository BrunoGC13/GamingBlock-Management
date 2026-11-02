package net.tgc.gamingblock_management.punishments;

import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.Date;

public class Punishment {
    private OfflinePlayer target;
    private Player operator;
    private PunishmentType type;
    private String reason;
    private PunishmentLayout layout;
    private Date endsOn;

    public Punishment(OfflinePlayer target, Player operator, PunishmentType type, String reason) {
        this.target = target;
        this.operator = operator;
        this.type = type;
        this.reason = reason;
        this.endsOn = null;
        this.layout = null;
    }

    public Punishment(OfflinePlayer target, Player operator, PunishmentType type) {
        this.target = target;
        this.operator = operator;
        this.type = type;
        this.reason = "";
        this.endsOn = null;
        this.layout = null;
    }

    public Punishment(OfflinePlayer target, Player operator, PunishmentType type, String reason, PunishmentLayout layout) {
        this.target = target;
        this.operator = operator;
        this.type = type;
        this.reason = reason;
        this.endsOn = null;
        this.layout = layout;
    }

    public Punishment(OfflinePlayer target, Player operator, PunishmentType type, PunishmentLayout layout) {
        this.target = target;
        this.operator = operator;
        this.type = type;
        this.reason = "";
        this.endsOn = null;
        this.layout = layout;
    }

    public Punishment(OfflinePlayer target, Player operator, PunishmentType type, String reason, Date endsOn) {
        if (type != PunishmentType.TEMPBAN) {
            return;
        }

        this.target = target;
        this.operator = operator;
        this.type = type;
        this.endsOn = endsOn;
        this.reason = reason;
        this.layout = null;
    }

    public Punishment(OfflinePlayer target, Player operator, PunishmentType type, Date endsOn) {
        if (type != PunishmentType.TEMPBAN) {
            return;
        }

        this.target = target;
        this.operator = operator;
        this.type = type;
        this.endsOn = endsOn;
        this.reason = "";
        this.layout = null;
    }

    public Punishment(OfflinePlayer target, Player operator, PunishmentType type, String reason, PunishmentLayout layout, Date endsOn) {
        if (type != PunishmentType.TEMPBAN) {
            return;
        }

        this.target = target;
        this.operator = operator;
        this.type = type;
        this.endsOn = endsOn;
        this.reason = reason;
        this.layout = layout;
    }

    public Punishment(OfflinePlayer target, Player operator, PunishmentType type, PunishmentLayout layout, Date endsOn) {
        if (type != PunishmentType.TEMPBAN) {
            return;
        }

        this.target = target;
        this.operator = operator;
        this.type = type;
        this.endsOn = endsOn;
        this.reason = "";
        this.layout = layout;
    }

    public OfflinePlayer getTarget() {
        return target;
    }

    public Player getOperator() {
        return operator;
    }

    public PunishmentType getType() {
        return type;
    }

    public String getReason() {
        return reason;
    }

    public Date getEndsOn() {
        return endsOn;
    }

    public PunishmentLayout getLayout() {
        return layout;
    }
}
