package com.massivecraft.factions.cmd;

import com.massivecraft.factions.Conf;
import com.massivecraft.factions.FPlayer;
import com.massivecraft.factions.Faction;
import com.massivecraft.factions.struct.Permission;
import com.massivecraft.factions.struct.Role;
import com.massivecraft.factions.util.fm.enums.TL;
import mkremins.fanciful.FancyMessage;
import org.bukkit.ChatColor;

public class CmdMod extends FCommand {

    public CmdMod() {
        super();
        this.aliases.add("mod");
        this.aliases.add("setmod");
        this.aliases.add("officer");
        this.aliases.add("setofficer");

        this.optionalArgs.put("player name", "name");
        //this.optionalArgs.put("", "");

        this.permission = Permission.MOD.node;
        this.disableOnLock = true;

        senderMustBePlayer = false;
        senderMustBeMember = true;
        senderMustBeModerator = false;
        senderMustBeColeader = true;
        senderMustBeAdmin = false;
    }

    @Override
    public void perform() {
        FPlayer you = this.argAsBestFPlayerMatch(0);
        if (you == null) {
            FancyMessage msg = new FancyMessage(TL.CMD_MOD_PROMOTED.toString()).color(ChatColor.GOLD);
            for (FPlayer player : myFaction.getFPlayersWhereRole(Role.NORMAL)) {
                String s = player.getName();
                msg.then(s + " ").color(ChatColor.WHITE).tooltip(TL.CMD_CLICK_TO_PROMOTE.toString() + s).command("/" + Conf.baseCommandAliases.get(0) + " mod " + s);
            }

            sendFancyMessage(msg);
            return;
        }

        boolean permAny = Permission.MOD_ANY.has(sender, false);
        Faction targetFaction = you.getFaction();
        if (targetFaction != myFaction && !permAny) {
            msg(TL.CMD_NOT_MEMBER, you.describeTo(fme, true));
            return;
        }

        if (fme != null && fme.getRole() != Role.LEADER && !permAny) {
            msg(TL.CMD_NOT_ADMIN);
            return;
        }

        if (you == fme && !permAny) {
            msg(TL.CMD_NOT_SELF);
            return;
        }

        if (you.getRole() == Role.LEADER) {
            msg(TL.CMD_TARGET_IS_ADMIN);
            return;
        }

        if (you.getRole() == Role.MODERATOR) {
            // Revoke
            you.setRole(Role.NORMAL);
            targetFaction.msg(TL.CMD_REVOKED_MOD, you.describeTo(targetFaction, true));
            msg(TL.CMD_REVOKES_MOD, you.describeTo(fme, true));
        } else {
            // Give
            you.setRole(Role.MODERATOR);
            targetFaction.msg(TL.CMD_MOD_PROMOTED, you.describeTo(targetFaction, true));
            msg(TL.CMD_MOD_PROMOTES, you.describeTo(fme, true));
        }
    }

    @Override
    public TL getUsageTranslation() {
        return TL.COMMAND_MOD_DESCRIPTION;
    }
}