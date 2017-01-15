package com.avrgaming.civcraft.war;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import com.avrgaming.anticheat.ACManager;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.threading.TaskMaster;
import com.avrgaming.civcraft.threading.tasks.PlayerKickBan;
import com.avrgaming.civcraft.util.CivColor;

public class WarAntiCheat {

	
	public static void kickUnvalidatedPlayers() {
		if (CivGlobal.isCasualMode()) {
			return;
		}
		
		if (!ACManager.isEnabled()) {
			return;
		}
		
		for (Player player : Bukkit.getOnlinePlayers()) {
			if (player.isOp()) {
				continue;
			}
			
			if (player.hasPermission("civ.ac_exempt")) {
				continue;
			}
			
// 			Resident resident = CivGlobal.getResident(player);
// 			onWarTimePlayerCheck(resident);
		}
		
// 		CivMessage.global(CivColor.LightGray+CivSettings.localize.localizedString("war_kick_atWarNoAnticheat"));
	}
	
	public static void onWarTimePlayerCheck(Resident resident) {
		if (!resident.hasTown()) {
			return;
		}
		
		if (!resident.getCiv().getDiplomacyManager().isAtWar()) {
			return;
		}
		
// 		try {
// 			if (!resident.isUsesAntiCheat()) {
// 				TaskMaster.syncTask(new PlayerKickBan(resident.getName(), true, false, 
// 						CivSettings.localize.localizedString("war_kick_needAnticheat1")+
// 						CivSettings.localize.localizedString("war_kick_needAntiCheat2")));
// 			}
// 		} catch (CivException e) {
// 		}
	}
	
}
