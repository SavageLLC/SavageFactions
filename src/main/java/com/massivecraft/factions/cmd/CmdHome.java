package com.massivecraft.factions.cmd;

import com.massivecraft.factions.*;
import com.massivecraft.factions.integration.Essentials;
import com.massivecraft.factions.struct.Permission;
import com.massivecraft.factions.struct.Relation;
import com.massivecraft.factions.struct.Role;
import com.massivecraft.factions.util.WarmUpUtil;
import com.massivecraft.factions.util.fm.enums.TL;
import com.massivecraft.factions.zcore.fperms.Access;
import com.massivecraft.factions.zcore.fperms.PermissableAction;
import com.massivecraft.factions.zcore.util.SmokeUtil;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;


public class CmdHome extends FCommand {

    public CmdHome() {
        super();
        this.aliases.add("home");

        //this.requiredArgs.add("");
        //this.optionalArgs.put("", "");

        this.permission = Permission.HOME.node;
        this.disableOnLock = false;


        senderMustBePlayer = true;
        senderMustBeMember = true;
        senderMustBeModerator = false;
        senderMustBeColeader = false;
        senderMustBeAdmin = false;
    }

    @Override
    public void perform() {
        // TODO: Hide this command on help also.
        if (!Conf.homesEnabled) {
            fme.msg(TL.CMD_FHOME_DISABLED);
            return;
        }

        if (!Conf.homesTeleportCommandEnabled) {
            fme.msg(TL.CMD_TELEPORT_DISABLED);
            return;
        }
        if (!fme.isAdminBypassing()) {
            Access access = myFaction.getAccess(fme, PermissableAction.HOME);
            if (access != Access.ALLOW && fme.getRole() != Role.LEADER) {
                fme.msg(TL.CMD_FPERMS_DENY_ACTION, "teleport home");
                return;
            }
        }


        if (!myFaction.hasHome()) {
            fme.msg(TL.CMD_NO_HOME_F_HOME.toString() + (fme.getRole().value < Role.MODERATOR.value ? TL.GENERIC_ASK_LEADER.toString() : TL.GENERIC_YOU_SHOULD.toString()));
            fme.sendMessage(p.cmdBase.cmdSethome.getUseageTemplate());
            return;
        }

        if (!Conf.homesTeleportAllowedFromEnemyTerritory && fme.isInEnemyTerritory()) {
            fme.msg(TL.CMD_ENEMY_NEAR_F_HOME);
            return;
        }

        if (!Conf.homesTeleportAllowedFromDifferentWorld && me.getWorld().getUID() != myFaction.getHome().getWorld().getUID()) {
            fme.msg(TL.CMD_WRONG_WORLD);
            return;
        }

        Faction faction = Board.getInstance().getFactionAt(new FLocation(me.getLocation()));
        final Location loc = me.getLocation().clone();

        // if player is not in a safe zone or their own faction territory, only allow teleport if no enemies are nearby
        if (Conf.homesTeleportAllowedEnemyDistance > 0 &&
                !faction.isSafeZone() &&
                (!fme.isInOwnTerritory() || (fme.isInOwnTerritory() && !Conf.homesTeleportIgnoreEnemiesIfInOwnTerritory))) {
            World w = loc.getWorld();
            double x = loc.getX();
            double y = loc.getY();
            double z = loc.getZ();

            for (Player p : me.getServer().getOnlinePlayers()) {
                if (p == null || !p.isOnline() || p.isDead() || p == me || p.getWorld() != w) {
                    continue;
                }

                FPlayer fp = FPlayers.getInstance().getByPlayer(p);
                if (fme.getRelationTo(fp) != Relation.ENEMY || fp.isVanished()) {
                    continue;
                }

                Location l = p.getLocation();
                double dx = Math.abs(x - l.getX());
                double dy = Math.abs(y - l.getY());
                double dz = Math.abs(z - l.getZ());
                double max = Conf.homesTeleportAllowedEnemyDistance;

                // box-shaped distance check
                if (dx > max || dy > max || dz > max) {
                    continue;
                }

                fme.msg(TL.CMD_ENEMY_NEAR_F_HOME, String.valueOf(Conf.homesTeleportAllowedEnemyDistance));
                return;
            }
        }

        // if economy is enabled, they're not on the bypass list, and this command has a cost set, make 'em pay
        if (!payForCommand(Conf.econCostHome, TL.COMMAND_HOME_TOTELEPORT.toString(), TL.COMMAND_HOME_FORTELEPORT.toString())) {
            return;
        }

        // if Essentials teleport handling is enabled and available, pass the teleport off to it (for delay and cooldown)
        if (Essentials.handleTeleport(me, myFaction.getHome())) {
            return;
        }

        this.doWarmUp(WarmUpUtil.Warmup.HOME, TL.WARMUP_TELEPORT, "Home", new Runnable() {
            @Override
            public void run() {
                // Create a smoke effect
                if (Conf.homesTeleportCommandSmokeEffectEnabled) {
                    List<Location> smokeLocations = new ArrayList<>();
                    smokeLocations.add(loc);
                    smokeLocations.add(loc.add(0, 1, 0));
                    smokeLocations.add(CmdHome.this.myFaction.getHome());
                    smokeLocations.add(CmdHome.this.myFaction.getHome().clone().add(0, 1, 0));
                    SmokeUtil.spawnCloudRandom(smokeLocations, Conf.homesTeleportCommandSmokeEffectThickness);
                }

                CmdHome.this.me.teleport(CmdHome.this.myFaction.getHome());
            }
        }, this.p.getConfig().getLong("warmups.f-home", 0));
    }

    @Override
    public TL getUsageTranslation() {
        return TL.COMMAND_HOME_DESCRIPTION;
    }
}