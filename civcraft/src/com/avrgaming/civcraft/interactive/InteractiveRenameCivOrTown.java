package com.avrgaming.civcraft.interactive;

import com.avrgaming.global.perks.components.RenameCivOrTown;
import org.apache.commons.lang.StringUtils;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.main.exception.CivException;
import com.avrgaming.civcraft.main.exception.InvalidNameException;
import com.avrgaming.civcraft.object.Civilization;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.util.CivColor;

public class InteractiveRenameCivOrTown implements InteractiveResponse {
    
    public String selection = null;
    public String oldName = null;
    public String newName = null;
    public Civilization selectedCiv = null;
    public Town selectedTown = null;
    RenameCivOrTown perk;
    
    public InteractiveRenameCivOrTown(Resident resident, RenameCivOrTown perk) {
        displayQuestion(resident);
        this.perk = perk;
    }
    
    public void displayQuestion(Resident resident) {
        CivMessage.send(resident, CivColor.Green + CivSettings.localize.localizedString("interactive_rename_question1"));
        CivMessage.send(resident, CivColor.Gray + CivSettings.localize.localizedString("interactive_rename_question2"));
        return;
    }
    
    @Override
    public void respond(String message, Resident resident) {
        CivMessage.sendHeading(resident, CivSettings.localize.localizedString("interactive_rename_heading"););
        
        try {
            if (selection == null) {
                if (message.equalsIgnoreCase("town")) {
                    CivMessage.send(resident, CivColor.Green + CivSettings.localize.localizedString("interactive_rename_townPrompt"));
                    selection = "town";
                } else if (message.equalsIgnoreCase("civ")) {
                    CivMessage.send(resident, CivColor.Green + CivSettings.localize.localizedString("interactive_rename_civPrompt"));
                    selection = "civ";
                } else {
                    throw new CivException(CivSettings.localize.localizedString("interactive_rename_cancel"));
                }
            } else if (oldName == null) {
                oldName = message;
                if (selection.equals("town")) {
                    
                    Town town = CivGlobal.getTown(oldName);
                    if (town == null) {
                        throw new CivException(CivSettings.localize.localizedString("var_interactive_rename_townNoTown", oldName));
                    }
                    
                    if (!town.getMayorGroup().hasMember(resident) && !town.getCiv().getLeaderGroup().hasMember(resident)) {
                        throw new CivException(CivSettings.localize.localizedString("interactive_rename_noPerms"));
                    }
                    
                    selectedTown = town;
                    CivMessage.send(resident, CivColor.Green + CivSettings.localize.localizedString("interactive_rename_newtownPrompt"));
                } else if (selection.equals("civ")) {
                    Civilization civ = CivGlobal.getCiv(oldName);
                    
                    if (civ == null) {
                        civ = CivGlobal.getConqueredCiv(oldName);
                        if (civ == null) {
                            throw new CivException(CivSettings.localize.localizedString("var_interactive_rename_civNone", oldName));
                        }
                    }
                    
                    if (!civ.getLeaderGroup().hasMember(resident)) {
                        throw new CivException(CivSettings.localize.localizedString("interactive_rename_civnoPerms"));
                    }
                    
                    selectedCiv = civ;
                    CivMessage.send(resident, CivColor.Green + CivSettings.localize.localizedString("interactive_rename_newcivPrompt"));
                }
            } else if (newName == null) {
                newName = message.replace(" ", "_");
                if (selectedCiv != null) {
                    if (!StringUtils.isAlpha(newName) || !StringUtils.isAsciiPrintable(newName)) {
                        throw new CivException(CivColor.RoseBold + CivSettings.localize.localizedString("interactive_civ_invalid"));
                    }
                    
                    try {
                        CivMessage.global(CivSettings.localize.localizedString("var_interactive_rename_successCiv", resident.getName(), selectedCiv.getName(), newName));
                        selectedCiv.rename(newName);
                        perk.markAsUsed(resident);
                    } catch (InvalidNameException e) {
                        throw new CivException(CivSettings.localize.localizedString("interactive_rename_invalidName"));
                    }
                } else if (selectedTown != null) {
                    if (!StringUtils.isAlpha(newName) || !StringUtils.isAsciiPrintable(newName)) {
                        throw new CivException(CivColor.RoseBold + CivSettings.localize.localizedString("interactive_capitol_invalidname"));
                    }
                    
                    try {
                        CivMessage.global(CivSettings.localize.localizedString("var_interactive_rename_successTown", resident.getName(), selectedTown.getName(), newName));
                        selectedTown.rename(newName);
                        perk.markAsUsed(resident);
                    } catch (InvalidNameException e) {
                        throw new CivException(CivSettings.localize.localizedString("interactive_rename_invalidName"));
                    }
                }
            } else {
                throw new CivException(CivSettings.localize.localizedString("interactive_rename_missingInfo"));
            }
        } catch (CivException e) {
            CivMessage.sendError(resident, e.getMessage());
            resident.clearInteractiveMode();
            return;
        }
        
        
    }
    
}

