package com.avrgaming.civcraft.config;

import com.avrgaming.civcraft.main.CivMessage;
import java.util.ArrayList;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.object.Civilization;
import java.util.Iterator;
import java.util.List;
import com.avrgaming.civcraft.main.CivLog;
import java.util.Map;
import org.bukkit.configuration.file.FileConfiguration;

public class ConfigTech
{
    public String id;
    public String name;
    public double beaker_cost;
    public double cost;
    public String require_techs;
    public int era;
    public Integer points;
    
    public static void loadConfig(final FileConfiguration cfg, final Map<String, ConfigTech> tech_maps) {
        tech_maps.clear();
        final List<Map<?, ?>> techs = (List<Map<?, ?>>)cfg.getMapList("techs");
        for (final Map<?, ?> confTech : techs) {
            final ConfigTech tech = new ConfigTech();
            tech.id = (String)confTech.get("id");
            tech.name = (String)confTech.get("name");
            tech.beaker_cost = (double)confTech.get("beaker_cost");
            tech.cost = (double)confTech.get("cost");
            tech.era = (int)confTech.get("era");
            tech.require_techs = (String)confTech.get("require_techs");
            tech.points = (Integer)confTech.get("points");
            tech_maps.put(tech.id, tech);
        }
        CivLog.info("Loaded " + tech_maps.size() + " technologies.");
    }
    
    public static double eraRate(final Civilization civ) {
        double rate = 0.0;
        final double era = CivGlobal.highestCivEra - 1 - civ.getCurrentEra();
        if (era > 0.0) {
            rate = era / 10.0;
        }
        return rate;
    }
    
    public double getAdjustedBeakerCost(final Civilization civ) {
        double rate = 1.0;
        rate -= eraRate(civ);
        return Math.floor(this.beaker_cost * Math.max(rate, 0.01));
    }
    
    public double getAdjustedTechCost(final Civilization civ) {
        double rate = 1.0;
        for (final Town town : civ.getTowns()) {
            if (town.getBuffManager().hasBuff("buff_profit_sharing") || town.getBuffManager().hasBuff("buff_msu_profit_sharing")) {
                rate -= town.getBuffManager().getEffectiveDouble("buff_profit_sharing");
				rate -= town.getBuffManager().getEffectiveDouble(buff_msu_profit_sharing);
            }
        }
        rate = Math.max(rate, 0.75);
        rate -= eraRate(civ);
        return Math.floor(this.cost * Math.max(rate, 0.01));
    }
    
    public static ArrayList<ConfigTech> getAvailableTechs(final Civilization civ) {
        final ArrayList<ConfigTech> returnTechs = new ArrayList<ConfigTech>();
        for (final ConfigTech tech : CivSettings.techs.values()) {
            if (!civ.hasTechnology(tech.id) && tech.isAvailable(civ)) {
                returnTechs.add(tech);
            }
        }
        return returnTechs;
    }
    
    public boolean isAvailable(final Civilization civ) {
        if (CivGlobal.testFileFlag("debug-norequire")) {
            CivMessage.global("Ignoring requirements! debug-norequire found.");
            return true;
        }
        if (this.require_techs == null || this.require_techs.equals("")) {
            return true;
        }
        final String[] requireTechs = this.require_techs.split(":");
        String[] array;
        for (int length = (array = requireTechs).length, i = 0; i < length; ++i) {
            final String reqTech = array[i];
            if (!civ.hasTechnology(reqTech)) {
                return false;
            }
        }
        return true;
    }
}
