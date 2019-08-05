package com.massivecraft.factions.cmd;

import com.massivecraft.factions.SavageFactions;
import com.massivecraft.factions.struct.Permission;
import com.massivecraft.factions.util.fm.enums.TL;

public class CmdBypass extends FCommand {

    public CmdBypass() {
        super();
        this.aliases.add("bypass");

        //this.requiredArgs.add("");
        this.optionalArgs.put("on/off", "flip");

        this.permission = Permission.BYPASS.node;
        this.disableOnLock = false;

        senderMustBePlayer = true;
        senderMustBeMember = false;
        senderMustBeModerator = false;
        senderMustBeColeader = false;
        senderMustBeAdmin = false;
    }

    @Override
    public void perform() {
        fme.setIsAdminBypassing(this.argAsBool(0, !fme.isAdminBypassing()));

        // TODO: Move this to a transient field in the model??
        if (fme.isAdminBypassing()) {
            fme.msg(TL.CMD_BYPASS_ENABLE.toString());
            SavageFactions.plugin.log(fme.getName() + TL.CMD_BYPASS_ENABLE_LOG.toString());
        } else {
            fme.msg(TL.CMD_BYPASS_DISABLE.toString());
            SavageFactions.plugin.log(fme.getName() + TL.CMD_BYPASS_DISABLE_LOG.toString());
        }
    }

    @Override
    public TL getUsageTranslation() {
        return TL.COMMAND_BYPASS_DESCRIPTION;
    }
}