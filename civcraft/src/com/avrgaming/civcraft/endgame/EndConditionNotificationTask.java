package com.avrgaming.civcraft.endgame;

import java.util.ArrayList;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Civilization;
import com.avrgaming.civcraft.sessiondb.SessionEntry;
import com.avrgaming.civcraft.util.CivColor;

public class EndConditionNotificationTask implements Runnable {

	@Override
	public void run() {
		
		for (EndGameCondition endCond : EndGameCondition.endConditions) {
			ArrayList<SessionEntry> entries = CivGlobal.getSessionDB().lookup(endCond.getSessionKey());
			if (entries.size() == 0) {
				continue;
			}
			
			for (SessionEntry entry : entries) {
				Civilization civ = EndGameCondition.getCivFromSessionData(entry.value);
				if (civ != null)
				{
					Integer daysLeft = endCond.getDaysToHold() - endCond.getDaysHeldFromSessionData(entry.value);
					if (daysLeft == 0) {
						CivMessage.global(CivSettings.localize.localizedString("var_cmd_civ_info_victory",
								CivColor.Red+CivColor.BOLD+civ.getName()+CivColor.Red, CivColor.Red+CivColor.BOLD+endCond.getVictoryName()+CivColor.Red));
						break;
					} else {
						CivMessage.global(CivSettings.localize.localizedString("var_cmd_civ_info_daysTillVictoryNew",
								CivColor.LightBlue+CivColor.BOLD+civ.getName()+CivColor.White, CivColor.Yellow+CivColor.BOLD+daysLeft+CivColor.White,CivColor.LightPurple+CivColor.BOLD+endCond.getVictoryName()+CivColor.White));
					}
				}
			}
		}
		
	}

}