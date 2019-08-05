package com.massivecraft.factions.cmd;

import com.massivecraft.factions.*;
import com.massivecraft.factions.event.FactionDisbandEvent.PlayerDisbandReason;
import com.massivecraft.factions.struct.Permission;
import com.massivecraft.factions.struct.Role;
import com.massivecraft.factions.util.fm.FileManager.Files;
import com.massivecraft.factions.util.fm.enums.TL;
import com.massivecraft.factions.zcore.ffly.UtilFly;
import com.massivecraft.factions.zcore.fperms.Access;
import com.massivecraft.factions.zcore.fperms.PermissableAction;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.HashMap;


public class CmdDisband extends FCommand {


	private static HashMap<String, String> disbandMap = new HashMap<>();


	public CmdDisband() {
		super();
		this.aliases.add("disband");

		//this.requiredArgs.add("");
		this.optionalArgs.put("faction tag", "yours");

		this.permission = Permission.DISBAND.node;
		this.disableOnLock = true;


		senderMustBePlayer = false;
		senderMustBeMember = false;
		senderMustBeModerator = false;
		senderMustBeColeader = false;
		senderMustBeAdmin = false;

	}

	@Override
	public void perform() {
		FileConfiguration config = Files.CONFIG.getFile();
		// The faction, default to your own.. but null if console sender.
		Faction faction = this.argAsFaction(0, fme == null ? null : myFaction);
		if (faction == null) {
			return;
		}

		boolean isMyFaction = fme != null && faction == myFaction;

		if (isMyFaction) {
			if (!assertMinRole(Role.LEADER)) {
				return;
			}
		} else {
			if (!Permission.DISBAND_ANY.has(sender, true)) {
				return;
			}
		}


		if (fme != null && !fme.isAdminBypassing()) {
			Access access = faction.getAccess(fme, PermissableAction.DISBAND);
			if (fme.getRole() != Role.LEADER && faction.getFPlayerLeader() != fme && access != Access.ALLOW) {
				fme.msg(TL.CMD_FPERMS_DENY_ACTION, "disband " + faction.getTag());
				return;
			}
		}

		if (!faction.isNormal()) {
			msg(TL.CMD_SYSTEM_FACTION.toString());
			return;
		}
		if (faction.isPermanent()) {
			msg(TL.CMD_PERM_FACTION.toString());
			return;
		}

		// THis means they are a console command sender.
		if (me == null) {
			faction.disband(null, PlayerDisbandReason.PLUGIN);
			return;
		}

		// check for tnt before disbanding.
		if (!disbandMap.containsKey(me.getUniqueId().toString()) && faction.getTnt() > 0) {
			msg(TL.CMD_TNTFILL_DISBAND_CONFIRM.toString().replace("{tnt}", faction.getTnt() + ""));
			disbandMap.put(me.getUniqueId().toString(), faction.getId());
			Bukkit.getScheduler().scheduleSyncDelayedTask(SavageFactions.plugin, () -> disbandMap.remove(me.getUniqueId().toString()), 200L);
		} else if (faction.getId().equals(disbandMap.get(me.getUniqueId().toString())) || faction.getTnt() == 0) {
			if (config.getBoolean("faction-disband-broadcast", true)) {
				for (FPlayer follower : FPlayers.getInstance().getOnlinePlayers()) {
					String amountString = senderIsConsole ? TL.GENERIC_SERVER_ADMIN.toString() : fme.describeTo(follower);
					UtilFly.checkFly(this.fme, Board.getInstance().getFactionAt(new FLocation(follower)));
					if (follower.getFaction() == faction) {
						follower.msg(TL.CMD_YOUR_FACTION, amountString);
					} else {
						follower.msg(TL.CMD_NOT_YOUR_FACTION, amountString, faction.getTag(follower));
					}
				}
				faction.disband(me, PlayerDisbandReason.COMMAND);
			} else {
				faction.disband(me, PlayerDisbandReason.COMMAND);
				me.sendMessage(String.valueOf(TL.CMD_PLAYER_DISBAND));
			}
		}
	}


	@Override
	public TL getUsageTranslation() {
		return TL.COMMAND_DISBAND_DESCRIPTION;
	}
}