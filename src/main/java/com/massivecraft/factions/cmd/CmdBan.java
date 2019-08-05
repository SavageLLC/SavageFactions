package com.massivecraft.factions.cmd;

import com.massivecraft.factions.FPlayer;
import com.massivecraft.factions.SavageFactions;
import com.massivecraft.factions.event.FPlayerLeaveEvent;
import com.massivecraft.factions.struct.BanInfo;
import com.massivecraft.factions.struct.Permission;
import com.massivecraft.factions.struct.Role;
import com.massivecraft.factions.util.fm.enums.TL;
import com.massivecraft.factions.zcore.fperms.Access;
import com.massivecraft.factions.zcore.fperms.PermissableAction;
import org.bukkit.Bukkit;

import java.util.logging.Level;

public class CmdBan extends FCommand {

    public CmdBan() {
        super();
        this.aliases.add("ban");

        this.requiredArgs.add("target");

        this.permission = Permission.BAN.node;
        this.disableOnLock = true;

        senderMustBePlayer = true;
        senderMustBeMember = false;
        senderMustBeModerator = false;
        senderMustBeColeader = false;
        senderMustBeAdmin = false;
    }

    @Override
    public void perform() {

        // Adds bypass to admins and clean permission check
        if (!fme.isAdminBypassing()) {
            Access access = myFaction.getAccess(fme, PermissableAction.BAN);
            if (access != Access.ALLOW && fme.getRole() != Role.LEADER) {
                fme.msg(TL.CMD_FPERMS_DENY_ACTION.toString(), "ban");
                return;
            }
        }


        // Good on permission checks. Now lets just ban the player.
        FPlayer target = argAsFPlayer(0);
        if (target == null) {
            return; // the above method sends a message if fails to find someone.
        }

        if (fme == target) {
            // You may not ban yourself
            fme.msg(TL.CMD_NO_BAN_SELF.toString());
            return;
        } else if (target.getFaction() == myFaction && target.getRole().value >= fme.getRole().value) {
            // You may not ban someone that has same or higher faction rank
            fme.msg(TL.CMD_INSUFFICIENT_RANK.toString(), target.getName());
            return;
        }

        for (BanInfo banInfo : myFaction.getBannedPlayers()) {
            if (banInfo.getBanned().equals(target.getId())) {
                msg(TL.CMD_ALREADY_BANNED);
                return;
            }
        }


        // Ban the user.
        myFaction.ban(target, fme);
        myFaction.deinvite(target); // can't hurt

        // If in same Faction, lets make sure to kick them and throw an event.
        if (target.getFaction() == myFaction) {

            FPlayerLeaveEvent event = new FPlayerLeaveEvent(target, myFaction, FPlayerLeaveEvent.PlayerLeaveReason.BANNED);
            Bukkit.getServer().getPluginManager().callEvent(event);

            if (event.isCancelled()) {
                // if someone cancels a ban, we'll get people complaining here. So lets log it.
                SavageFactions.plugin.log(Level.WARNING, "Attempted to ban {0} but someone cancelled the kick event. This isn't good.", target.getName());
                return;
            }

            // Didn't get cancelled so remove them and reset their invite.
            myFaction.removeFPlayer(target);
            target.resetFactionData();
        }

        // Lets inform the people!
        target.msg(TL.CMD_BAN_TARGET.toString(), myFaction.getTag(target.getFaction()));
        myFaction.msg(TL.CMD_BANNED_MEMBER.toString(), fme.getName(), target.getName());
    }

    @Override
    public TL getUsageTranslation() {
        return TL.CMD_ADMIN_DESCRIPTION;
    }
}
