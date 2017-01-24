package com.avrgaming.civcraft.structure;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.locks.ReentrantLock;

import org.bukkit.Location;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.exception.InvalidConfiguration;
import com.avrgaming.civcraft.object.Buff;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.util.BlockCoord;
import com.avrgaming.civcraft.util.SimpleBlock;

public class Quarry extends Structure {
	public static final int MAX_CHANCE = CivSettings.getIntegerStructure("quarry.max");
	private static final double COBBLESTONE_RATE = CivSettings.getDoubleStructure("quarry.cobblestone_rate"); //100%
	private static final double OTHER_RATE = CivSettings.getDoubleStructure("quarry.other_rate"); //10%
	private static final double COAL_RATE = CivSettings.getDoubleStructure("quarry.coal_rate"); //10%
	private static final double REDSTONE_CHANCE = CivSettings.getDoubleStructure("quarry.redstone_chance");
	private static final double IRON_CHANCE = CivSettings.getDoubleStructure("quarry.iron_chance");
	private static final double GOLD_CHANCE = CivSettings.getDoubleStructure("quarry.gold_chance");
	private static final double TUNGSTEN_CHANCE = CivSettings.getDoubleStructure("quarry.tungsten_chance");
	private static final double RARE_CHANCE = CivSettings.getDoubleStructure("quarry.rare_chance");
	
	private int level = 1;
	public int skippedCounter = 0;
	public ReentrantLock lock = new ReentrantLock();
	
	public enum Mineral {
		RARE,
		TUNGSTEN,
		GOLD,
		REDSTONE,
		IRON,
		COAL,
		OTHER,
		COBBLESTONE
	}
	
	protected Quarry(Location center, String id, Town town) throws CivException {
		super(center, id, town);	
	}
	
	public Quarry(ResultSet rs) throws SQLException, CivException {
		super(rs);
	}

	@Override
	public String getDynmapDescription() {
		String out = "<u><b>"+this.getDisplayName()+"</u></b><br/>";
		out += CivSettings.localize.localizedString("Level")+" "+this.level;
		return out;
	}
	
	@Override
	public String getMarkerIconName() {
		return "minecart";
	}

	public double getChance(Mineral mineral) {
		double chance = 0;
		switch (mineral) {
		case RARE:
			chance = RARE_CHANCE;
			break;
		case TUNGSTEN:
			chance = TUNGSTEN_CHANCE;
			break;
		case GOLD:
			chance = GOLD_CHANCE;
			break;
		case IRON:
			chance = IRON_CHANCE;
			break;
		case REDSTONE:
			chance = REDSTONE_CHANCE;
			break;
		case COAL:
			chance = COAL_RATE;
			break;
		case OTHER:
			chance = OTHER_RATE;
			break;
		case COBBLESTONE:
			chance = COBBLESTONE_RATE;
			break;
		}
		return this.modifyChance(chance);
	}
	
	private double modifyChance(Double chance) {
		double increase = chance*this.getTown().getBuffManager().getEffectiveDouble(Buff.EXTRACTION);
		chance += increase;
	private double modifyChance(Double chance) {
		double increase = chance*this.getTown().getBuffManager().getEffectiveDouble(Buff.EXTRACTIONCANYON);
		chance += increase;			
		
		try {
			if (this.getTown().getGovernment().id.equals("gov_despotism")) {
				chance *= CivSettings.getDouble(CivSettings.structureConfig, "quarry.despotism_rate");
			} else if (this.getTown().getGovernment().id.equals("gov_theocracy") || this.getTown().getGovernment().id.equals("gov_monarchy")){
				chance *= CivSettings.getDouble(CivSettings.structureConfig, "quarry.penalty_rate");
			}
		} catch (InvalidConfiguration e) {
			e.printStackTrace();
		}
		return chance;
	  }
	}
	
	@Override
	public void onPostBuild(BlockCoord absCoord, SimpleBlock commandBlock) {
		this.level = getTown().saved_quarry_level;
	}

	public int getLevel() {
		return level;
	}

	public void setLevel(int level) {
		this.level = level;
	}

}