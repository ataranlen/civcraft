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

public class Trommel extends Structure {
	public static final int GRAVEL_MAX_CHANCE = CivSettings.getIntegerStructure("trommel_gravel.max"); //100%
	private static final double GRAVEL_REDSTONE_CHANCE = CivSettings.getDoubleStructure("trommel_gravel.redstone_chance"); //3%
	private static final double GRAVEL_IRON_CHANCE = CivSettings.getDoubleStructure("trommel_gravel.iron_chance"); //4%
	private static final double GRAVEL_GOLD_CHANCE = CivSettings.getDoubleStructure("trommel_gravel.gold_chance"); //2%
	private static final double GRAVEL_DIAMOND_CHANCE = CivSettings.getDoubleStructure("trommel_gravel.diamond_chance"); //0.50%
	private static final double GRAVEL_EMERALD_CHANCE = CivSettings.getDoubleStructure("trommel_gravel.emerald_chance"); //0.20%
	private static final double GRAVEL_CHROMIUM_CHANCE = CivSettings.getDoubleStructure("trommel_gravel.chromium_chance");
	
	public static final int GRANITE_MAX_CHANCE = CivSettings.getIntegerStructure("trommel_granite.max");
	private static final double GRANITE_DIRT_RATE = CivSettings.getDoubleStructure("trommel_granite.dirt_rate"); //100%
	private static final double GRANITE_POLISHED_RATE = CivSettings.getDoubleStructure("trommel_granite.polished_rate"); //10%
	private static final double GRANITE_REDSTONE_CHANCE = CivSettings.getDoubleStructure("trommel_granite.redstone_chance");
	private static final double GRANITE_IRON_CHANCE = CivSettings.getDoubleStructure("trommel_granite.iron_chance");
	private static final double GRANITE_GOLD_CHANCE = CivSettings.getDoubleStructure("trommel_granite.gold_chance");
	private static final double GRANITE_TUNGSTEN_CHANCE = CivSettings.getDoubleStructure("trommel_granite.tungsten_chance");
	private static final double GRANITE_DIAMOND_CHANCE = CivSettings.getDoubleStructure("trommel_granite.diamond_chance");
	private static final double GRANITE_EMERALD_CHANCE = CivSettings.getDoubleStructure("trommel_granite.emerald_chance");
	private static final double GRANITE_CHROMIUM_CHANCE = CivSettings.getDoubleStructure("trommel_granite.chromium_chance");
	private static final double GRANITE_CRYSTAL_FRAGMENT_CHANCE = CivSettings.getDoubleStructure("trommel_granite.crystal_fragment_chance");
	private static final double GRANITE_REFINED_CHROMIUM_CHANCE = CivSettings.getDoubleStructure("trommel_granite.refined_chromium_chance");
	private static final double GRANITE_REFINED_TUNGSTEN_CHANCE = CivSettings.getDoubleStructure("trommel_granite.refined_tungsten_chance");
	private static final double GRANITE_CRYSTAL_CHANCE = CivSettings.getDoubleStructure("trommel_granite.crystal_chance");
	
	public static final int DIORITE_MAX_CHANCE = CivSettings.getIntegerStructure("trommel_diorite.max");
	private static final double DIORITE_DIRT_RATE = CivSettings.getDoubleStructure("trommel_diorite.dirt_rate"); //100%
	private static final double DIORITE_POLISHED_RATE = CivSettings.getDoubleStructure("trommel_diorite.polished_rate"); //10%
	private static final double DIORITE_REDSTONE_CHANCE = CivSettings.getDoubleStructure("trommel_diorite.redstone_chance");
	private static final double DIORITE_IRON_CHANCE = CivSettings.getDoubleStructure("trommel_diorite.iron_chance");
	private static final double DIORITE_GOLD_CHANCE = CivSettings.getDoubleStructure("trommel_diorite.gold_chance");
	private static final double DIORITE_TUNGSTEN_CHANCE = CivSettings.getDoubleStructure("trommel_diorite.tungsten_chance");
	private static final double DIORITE_DIAMOND_CHANCE = CivSettings.getDoubleStructure("trommel_diorite.diamond_chance");
	private static final double DIORITE_EMERALD_CHANCE = CivSettings.getDoubleStructure("trommel_diorite.emerald_chance");
	private static final double DIORITE_CHROMIUM_CHANCE = CivSettings.getDoubleStructure("trommel_diorite.chromium_chance");
	private static final double DIORITE_CRYSTAL_FRAGMENT_CHANCE = CivSettings.getDoubleStructure("trommel_diorite.crystal_fragment_chance");
	private static final double DIORITE_REFINED_CHROMIUM_CHANCE = CivSettings.getDoubleStructure("trommel_diorite.refined_chromium_chance");
	private static final double DIORITE_REFINED_TUNGSTEN_CHANCE = CivSettings.getDoubleStructure("trommel_diorite.refined_tungsten_chance");
	private static final double DIORITE_CRYSTAL_CHANCE = CivSettings.getDoubleStructure("trommel_diorite.crystal_chance");
	
	public static final int ANDESITE_MAX_CHANCE = CivSettings.getIntegerStructure("trommel_andesite.max");
	private static final double ANDESITE_DIRT_RATE = CivSettings.getDoubleStructure("trommel_andesite.dirt_rate"); //100%
	private static final double ANDESITE_POLISHED_RATE = CivSettings.getDoubleStructure("trommel_andesite.polished_rate"); //10%
	private static final double ANDESITE_REDSTONE_CHANCE = CivSettings.getDoubleStructure("trommel_andesite.redstone_chance");
	private static final double ANDESITE_IRON_CHANCE = CivSettings.getDoubleStructure("trommel_andesite.iron_chance");
	private static final double ANDESITE_GOLD_CHANCE = CivSettings.getDoubleStructure("trommel_andesite.gold_chance");
	private static final double ANDESITE_TUNGSTEN_CHANCE = CivSettings.getDoubleStructure("trommel_andesite.tungsten_chance");
	private static final double ANDESITE_DIAMOND_CHANCE = CivSettings.getDoubleStructure("trommel_andesite.diamond_chance");
	private static final double ANDESITE_EMERALD_CHANCE = CivSettings.getDoubleStructure("trommel_andesite.emerald_chance");
	private static final double ANDESITE_CHROMIUM_CHANCE = CivSettings.getDoubleStructure("trommel_andesite.chromium_chance");
	private static final double ANDESITE_CRYSTAL_FRAGMENT_CHANCE = CivSettings.getDoubleStructure("trommel_andesite.crystal_fragment_chance");
	private static final double ANDESITE_REFINED_CHROMIUM_CHANCE = CivSettings.getDoubleStructure("trommel_andesite.refined_chromium_chance");
	private static final double ANDESITE_REFINED_TUNGSTEN_CHANCE = CivSettings.getDoubleStructure("trommel_andesite.refined_tungsten_chance");
	private static final double ANDESITE_CRYSTAL_CHANCE = CivSettings.getDoubleStructure("trommel_andesite.crystal_chance");
	
	private int level = 1;
	public int skippedCounter = 0;
	public ReentrantLock lock = new ReentrantLock();
	
	public enum Mineral {
		CRYSTAL,
		REFINED_TUNGSTEN,
		REFINED_CHROMIUM,
		CRYSTAL_FRAGMENT,
		CHROMIUM,
		EMERALD,
		DIAMOND,
		TUNGSTEN,
		GOLD,
		REDSTONE,
		IRON,
		POLISHED,
		DIRT
	}
	
	protected Trommel(Location center, String id, Town town) throws CivException {
		super(center, id, town);	
	}
	
	public Trommel(ResultSet rs) throws SQLException, CivException {
		super(rs);
	}

	@Override
	public String getDynmapDescription() {
		String out = "<u><b>"+this.getDisplayName()+"</u></b><br/>";
		out += "Level: "+this.level;
		return out;
	}
	
	@Override
	public String getMarkerIconName() {
		return "minecart";
	}
	
	public double getGravelChance(Mineral mineral) {
		double chance = 0;
		switch (mineral) {
		case EMERALD:
			chance = GRAVEL_EMERALD_CHANCE;
			break;
		case DIAMOND:
			chance = GRAVEL_DIAMOND_CHANCE;
			break;
		case GOLD:
			chance = GRAVEL_GOLD_CHANCE;
			break;
		case IRON:
			chance = GRAVEL_IRON_CHANCE;
			break;
		case REDSTONE:
			chance = GRAVEL_REDSTONE_CHANCE;
			break;
		case CHROMIUM:
			chance = GRAVEL_CHROMIUM_CHANCE;
		default:
			break;
		}
		return this.modifyChance(chance);
	}
	
	public double getGraniteChance(Mineral mineral) {
		double chance = 0;
		switch (mineral) {
		case CRYSTAL:
			chance = GRANITE_CRYSTAL_CHANCE;
			break;
		case REFINED_CHROMIUM:
			chance = GRANITE_REFINED_CHROMIUM_CHANCE;
			break;
		case REFINED_TUNGSTEN:
			chance = GRANITE_REFINED_TUNGSTEN_CHANCE;
			break;
		case CRYSTAL_FRAGMENT:
			chance = GRANITE_CRYSTAL_FRAGMENT_CHANCE;
			break;
		case EMERALD:
			chance = GRANITE_EMERALD_CHANCE;
			break;
		case DIAMOND:
			chance = GRANITE_DIAMOND_CHANCE;
			break;
		case TUNGSTEN:
			chance = GRANITE_TUNGSTEN_CHANCE;
			break;
		case GOLD:
			chance = GRANITE_GOLD_CHANCE;
			break;
		case IRON:
			chance = GRANITE_IRON_CHANCE;
			break;
		case REDSTONE:
			chance = GRANITE_REDSTONE_CHANCE;
			break;
		case CHROMIUM:
			chance = GRANITE_CHROMIUM_CHANCE;
			break;
		case POLISHED:
			chance = GRANITE_POLISHED_RATE;
			break;
		case DIRT:
			chance = GRANITE_DIRT_RATE;
			break;
		}
		return this.modifyChance(chance);
	}
	
	public double getDioriteChance(Mineral mineral) {
		double chance = 0;
		switch (mineral) {
		case CRYSTAL:
			chance = DIORITE_CRYSTAL_CHANCE;
			break;
		case REFINED_CHROMIUM:
			chance = DIORITE_REFINED_CHROMIUM_CHANCE;
			break;
		case REFINED_TUNGSTEN:
			chance = DIORITE_REFINED_TUNGSTEN_CHANCE;
			break;
		case CRYSTAL_FRAGMENT:
			chance = DIORITE_CRYSTAL_FRAGMENT_CHANCE;
			break;
		case EMERALD:
			chance = DIORITE_EMERALD_CHANCE;
			break;
		case DIAMOND:
			chance = DIORITE_DIAMOND_CHANCE;
			break;
		case TUNGSTEN:
			chance = DIORITE_TUNGSTEN_CHANCE;
			break;
		case GOLD:
			chance = DIORITE_GOLD_CHANCE;
			break;
		case IRON:
			chance = DIORITE_IRON_CHANCE;
			break;
		case REDSTONE:
			chance = DIORITE_REDSTONE_CHANCE;
			break;
		case CHROMIUM:
			chance = DIORITE_CHROMIUM_CHANCE;
			break;
		case POLISHED:
			chance = DIORITE_POLISHED_RATE;
			break;
		case DIRT:
			chance = DIORITE_DIRT_RATE;
			break;
		}
		return this.modifyChance(chance);
	}

	
	public double getAndesiteChance(Mineral mineral) {
		double chance = 0;
		switch (mineral) {
		case CRYSTAL:
			chance = ANDESITE_CRYSTAL_CHANCE;
			break;
		case REFINED_CHROMIUM:
			chance = ANDESITE_REFINED_CHROMIUM_CHANCE;
			break;
		case REFINED_TUNGSTEN:
			chance = ANDESITE_REFINED_TUNGSTEN_CHANCE;
			break;
		case CRYSTAL_FRAGMENT:
			chance = ANDESITE_CRYSTAL_FRAGMENT_CHANCE;
			break;
		case EMERALD:
			chance = ANDESITE_EMERALD_CHANCE;
			break;
		case DIAMOND:
			chance = ANDESITE_DIAMOND_CHANCE;
			break;
		case TUNGSTEN:
			chance = ANDESITE_TUNGSTEN_CHANCE;
			break;
		case GOLD:
			chance = ANDESITE_GOLD_CHANCE;
			break;
		case IRON:
			chance = ANDESITE_IRON_CHANCE;
			break;
		case REDSTONE:
			chance = ANDESITE_REDSTONE_CHANCE;
			break;
		case CHROMIUM:
			chance = ANDESITE_CHROMIUM_CHANCE;
			break;
		case POLISHED:
			chance = ANDESITE_POLISHED_RATE;
			break;
		case DIRT:
			chance = ANDESITE_DIRT_RATE;
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
				chance *= CivSettings.getDouble(CivSettings.structureConfig, "trommel.despotism_rate");
			} else if (this.getTown().getGovernment().id.equals("gov_theocracy") || this.getTown().getGovernment().id.equals("gov_monarchy")){
				chance *= CivSettings.getDouble(CivSettings.structureConfig, "trommel.penalty_rate");
			}
		} catch (InvalidConfiguration e) {
			e.printStackTrace();
		}
		return chance;
	  }
	}
	
	@Override
	public void onPostBuild(BlockCoord absCoord, SimpleBlock commandBlock) {
		this.level = getTown().saved_trommel_level;
	}

	public int getLevel() {
		return level;
	}

	public void setLevel(int level) {
		this.level = level;
	}

}