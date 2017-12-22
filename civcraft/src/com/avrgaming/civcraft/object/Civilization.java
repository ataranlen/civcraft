/*************************************************************************
 * 
 * AVRGAMING LLC
 * __________________
 * 
 *  [2013] AVRGAMING LLC
 *  All Rights Reserved.
 * 
 * NOTICE:  All information contained herein is, and remains
 * the property of AVRGAMING LLC and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to AVRGAMING LLC
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from AVRGAMING LLC.
 */
package com.avrgaming.civcraft.object;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.avrgaming.civcraft.camp.WarCamp;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.config.ConfigGovernment;
import com.avrgaming.civcraft.config.ConfigTech;
import com.avrgaming.civcraft.database.SQL;
import com.avrgaming.civcraft.database.SQLUpdate;
import com.avrgaming.civcraft.endgame.EndConditionScience;
import com.avrgaming.civcraft.endgame.EndGameCondition;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.exception.InvalidConfiguration;
import com.avrgaming.civcraft.exception.InvalidNameException;
import com.avrgaming.civcraft.lorestorage.LoreCraftableMaterial;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Relation.Status;
import com.avrgaming.civcraft.permission.PermissionGroup;
import com.avrgaming.civcraft.structure.Capitol;
import com.avrgaming.civcraft.structure.RespawnLocationHolder;
import com.avrgaming.civcraft.structure.Structure;
import com.avrgaming.civcraft.structure.TownHall;
import com.avrgaming.civcraft.threading.TaskMaster;
import com.avrgaming.civcraft.threading.tasks.UpdateTechBar;
import com.avrgaming.civcraft.threading.timers.BeakerTimer;
import com.avrgaming.civcraft.util.BlockCoord;
import com.avrgaming.civcraft.util.ChunkCoord;
import com.avrgaming.civcraft.util.CivColor;
import com.avrgaming.civcraft.util.DateUtil;
import com.avrgaming.civcraft.util.ItemManager;

public class Civilization extends SQLObject {

	private Map<String, ConfigTech> techs = new ConcurrentHashMap<String, ConfigTech>();
	
	private int color;
	private int daysInDebt = 0;
	private int currentEra = 0;
	private double incomeTaxRate;
	private double sciencePercentage;
	private ConfigTech researchTech = null;
	private double researchProgress = 0.0;
		
	private EconObject treasury;
	private PermissionGroup leaderGroup;
	private PermissionGroup adviserGroup;
	
	/* Strings used for reverse lookups. */
	private String leaderName;
	private String leaderGroupName;
	private String advisersGroupName;
	private String capitolName;
	
	private ConcurrentHashMap<String, Town> towns = new ConcurrentHashMap<String, Town>();
	private ConfigGovernment government;

	private double baseBeakers = 1.0;	

	public static final int HEX_COLOR_MAX = 16777215;
	public static final int HEX_COLOR_TOLERANCE = 40;

	/* Store information to display about last upkeep paid. */
	public HashMap<String, Double> lastUpkeepPaidMap = new HashMap<String, Double>();
	
	/* Store information about last tick's taxes */
	public HashMap<String, Double> lastTaxesPaidMap = new HashMap<String, Double>();
	
	/* Used to prevent spam of tech % complete message. */
	private int lastTechPercentage = 0;

	private DiplomacyManager diplomacyManager = new DiplomacyManager(this);

	private boolean adminCiv = false;
	private boolean conquered = false;
	
	private Date conquer_date = null;
	private Date created_date = null;

	public boolean scoutDebug = false;
	public String scoutDebugPlayer = null;
	
	public String messageOfTheDay = "";
	
	public double settlerCost = 25000.0;
	
	private LinkedList<WarCamp> warCamps = new LinkedList<WarCamp>();
	
	public Civilization(String name, String capitolName, Resident leader) throws InvalidNameException {
		this.setName(name);
		this.leaderName = leader.getUUID().toString();
		this.setCapitolName(capitolName);
		
		this.government = CivSettings.governments.get("gov_tribalism");		
		this.color = this.pickCivColor();		
		this.setTreasury(CivGlobal.createEconObject(this));
		this.getTreasury().setBalance(0, false);
		this.created_date = new Date();
		loadSettings();
	}
	
	public Civilization(ResultSet rs) throws SQLException, InvalidNameException {
		this.load(rs);
		loadSettings();
	}
	
	public void loadSettings() {
		try {
			this.baseBeakers = CivSettings.getDouble(CivSettings.civConfig, "civ.base_beaker_rate");
		} catch (InvalidConfiguration e) {
			e.printStackTrace();
		}
	}
	
	public static String TABLE_NAME = "CIVILIZATIONS";
	public static void init() throws SQLException {
		if (!SQL.hasTable(TABLE_NAME)) {
			String table_create = "CREATE TABLE " + SQL.tb_prefix + TABLE_NAME+" (" + 
					"`id` int(11) unsigned NOT NULL auto_increment," +
					"`name` VARCHAR(64) NOT NULL," + 
					"`leaderName` mediumtext," +
					"`capitolName` mediumtext," +
					"`debt` float NOT NULL DEFAULT '0',"+
					"`coins` double DEFAULT 0,"+
					"`daysInDebt` int NOT NULL DEFAULT '0',"+
					"`techs` mediumtext DEFAULT NULL," +
					"`motd` mediumtext DEFAULT NULL,"+
					"`researchTech` mediumtext DEFAULT NULL,"+
					"`researchProgress` float NOT NULL DEFAULT 0,"+
					"`researched` mediumtext DEFAULT NULL, "+
					"`government_id` mediumtext DEFAULT NULL," +
					"`color` int(11) DEFAULT 0," +
					"`income_tax_rate` float NOT NULL DEFAULT 0," +
					"`science_percentage` float NOT NULL DEFAULT 0,"+
					"`leaderGroupName` mediumtext DEFAULT NULL,"+
					"`advisersGroupName` mediumtext DEFAULT NULL,"+
					"`lastUpkeepTick` mediumtext DEFAULT NULL," +
					"`lastTaxesTick` mediumtext DEFAULT NULL," +
					"`adminCiv` boolean DEFAULT false,"+
					"`conquered` boolean DEFAULT false,"+
					"`conquered_date` long,"+
				    "`created_date` long," +
					"UNIQUE KEY (`name`), " +
				"PRIMARY KEY (`id`)" + ")";
			
			SQL.makeTable(table_create);
			CivLog.info("Created "+TABLE_NAME+" table");
		} else {
			CivLog.info(TABLE_NAME+" table OK!");
			SQL.makeCol("conquered", "booelan", TABLE_NAME);
			SQL.makeCol("conquered_date", "long", TABLE_NAME);
			SQL.makeCol("created_date", "long", TABLE_NAME);
			SQL.makeCol("motd", "mediumtext", TABLE_NAME);
			SQL.makeCol("settlerCost", "double", TABLE_NAME);
		}
	}

	@Override
	public void load(ResultSet rs) throws SQLException, InvalidNameException {
		this.setId(rs.getInt("id"));
		this.setName(rs.getString("name"));		
		String resUUID = rs.getString("leaderName");
//		Resident res = CivGlobal.getResidentViaUUID(UUID.fromString(resUUID));
		leaderName = resUUID;
		
		
		capitolName = rs.getString("capitolName");
		setLeaderGroupName(rs.getString("leaderGroupName"));
		setAdvisersGroupName(rs.getString("advisersGroupName"));
		daysInDebt = rs.getInt("daysInDebt");
		this.color = rs.getInt("color");
		this.setResearchTech(CivSettings.techs.get(rs.getString("researchTech")));
		this.setResearchProgress(rs.getDouble("researchProgress"));
		this.setGovernment(rs.getString("government_id"));
		this.loadKeyValueString(rs.getString("lastUpkeepTick"), this.lastUpkeepPaidMap);
		this.loadKeyValueString(rs.getString("lastTaxesTick"), this.lastTaxesPaidMap);
		this.setSciencePercentage(rs.getDouble("science_percentage"));
		
		double taxes = rs.getDouble("income_tax_rate");
		if (taxes > this.government.maximum_tax_rate) {
			taxes = this.government.maximum_tax_rate;
		}
		
		this.setIncomeTaxRate(taxes);
		this.loadResearchedTechs(rs.getString("researched"));
		this.adminCiv = rs.getBoolean("adminCiv");
		this.conquered = rs.getBoolean("conquered");
		Long ctime = rs.getLong("conquered_date");
		if (ctime == null || ctime == 0) {
			this.conquer_date = null;
		} else {
			this.conquer_date = new Date(ctime);
		}
		
		String motd = rs.getString("motd");
		if (motd == null || motd == "") {
			this.messageOfTheDay = null; //Forever in the past.
		} else {
			this.messageOfTheDay = motd;
		}
		
		ctime = rs.getLong("created_date");
		if (ctime == null || ctime == 0) {
			this.created_date = new Date(0); //Forever in the past.
		} else {
			this.created_date = new Date(ctime);
		}
		
		this.setTreasury(CivGlobal.createEconObject(this));
		this.getTreasury().setBalance(rs.getDouble("coins"), false);
		this.getTreasury().setDebt(rs.getDouble("debt"));
		this.settlerCost = rs.getDouble("settlerCost");

		for (ConfigTech tech : this.getTechs())
		{
			if (tech.era > this.getCurrentEra()) {
				this.setCurrentEra(tech.era);
			}
		}
	}

	@Override
	public void save() {
		SQLUpdate.add(this);
	}
	
	@Override
	public void saveNow() throws SQLException {
		HashMap<String, Object> hashmap = new HashMap<String, Object>();
		hashmap.put("name", this.getName());
			hashmap.put("leaderName", this.getLeader().getUUIDString());
		
		hashmap.put("capitolName", this.capitolName);
		hashmap.put("leaderGroupName", this.getLeaderGroupName());
		hashmap.put("advisersGroupName", this.getAdvisersGroupName());
		hashmap.put("debt", this.getTreasury().getDebt());
		hashmap.put("coins", this.getTreasury().getBalance());
		hashmap.put("daysInDebt", this.daysInDebt);
		hashmap.put("income_tax_rate", this.getIncomeTaxRate());
		hashmap.put("science_percentage", this.getSciencePercentage());
		hashmap.put("color", this.getColor());
		if (this.getResearchTech() != null) {
			hashmap.put("researchTech", this.getResearchTech().id);
		} else {
			hashmap.put("researchTech", null);
		}
		hashmap.put("researchProgress", this.getResearchProgress());
		hashmap.put("government_id", this.getGovernment().id);
		hashmap.put("lastUpkeepTick", this.saveKeyValueString(this.lastUpkeepPaidMap));
		hashmap.put("lastTaxesTick", this.saveKeyValueString(this.lastTaxesPaidMap));
		hashmap.put("researched", this.saveResearchedTechs());
		hashmap.put("adminCiv", this.adminCiv);
		hashmap.put("conquered", this.conquered);
		hashmap.put("settlerCost", this.settlerCost);
		
		if (this.conquer_date != null) {
			hashmap.put("conquered_date", this.conquer_date.getTime());
		} else {
			hashmap.put("conquered_date", null);
		}
		
		if (this.messageOfTheDay != null) {
			hashmap.put("motd", this.messageOfTheDay);
		} else {
			hashmap.put("motd", null);
		}
		
		if (this.created_date != null) {
			hashmap.put("created_date", this.created_date.getTime());
		} else {
			hashmap.put("created_date", null);
		}
		
		SQL.updateNamedObject(this, hashmap, TABLE_NAME);
	}
	
	private void loadResearchedTechs(String techstring) {
		if (techstring == null || techstring.equals("")) {
			return;
		}
		
		String[] techs = techstring.split(",");
		
		for (String tech : techs) {
			ConfigTech t = CivSettings.techs.get(tech);
			if (t != null) {
				CivGlobal.researchedTechs.add(t.id.toLowerCase());
				this.techs.put(tech, t);
			}
		}
	}
	
	private Object saveResearchedTechs() {
		String out = "";
		
		for (ConfigTech tech : this.techs.values()) {
			out += tech.id+",";
		}
		
		return out;
	}

	private void loadKeyValueString(String string, HashMap<String, Double> map) {
		
		String[] keyvalues = string.split(";");
		
		for (String keyvalue : keyvalues) {
			try {
				String key = keyvalue.split(":")[0];
				String value = keyvalue.split(":")[1];
				
				map.put(key, Double.valueOf(value));
			} catch (ArrayIndexOutOfBoundsException e) {
				// forget it then.
			}
		}
		
	}
	
	private String saveKeyValueString(HashMap<String, Double> map) {
		String out = "";
		
		for (String key : map.keySet()) {
			double value = map.get(key);
			out += key+":"+value+";";
		}
		return out;
	}

	public boolean hasTechnology(String require_tech) {
		
		if (require_tech != null) {
			String split[] = require_tech.split(":");
			for (String str : split) {
				if (!hasTech(str)) {
					return false;
				}
			}
		}
		
		return true;
	}
	
	private boolean hasTech(String configId) {
		if (configId == null || configId.equals("")) {
			return true;
		}
		
		return techs.containsKey(configId);
	}
	
	public void addTech(ConfigTech t) {
		if (t.era > this.getCurrentEra()) {
			this.setCurrentEra(t.era);
		}
		
		CivGlobal.researchedTechs.add(t.id.toLowerCase());
		techs.put(t.id, t);
		
		for (Town town : this.getTowns()) {
			town.onTechUpdate();
		}
		
	}
	
	public void removeTech(ConfigTech t) {
		removeTech(t.id);
	}
	
	public void removeTech(String configId) {
		techs.remove(configId);
		
		for (Town town : this.getTowns()) {
			town.onTechUpdate();
		}
	}

	public ConfigGovernment getGovernment() {
		return government;
	}

	public void setGovernment(String gov_id) {
		this.government = CivSettings.governments.get(gov_id);
		
		if (this.getSciencePercentage() > this.government.maximum_tax_rate) {
			this.setSciencePercentage(this.government.maximum_tax_rate);
		}
		
	}

	public int getColor() {
		return color;
	}

	public void setColor(int color) {
		this.color = color;
	}
	
	public void setMotd(String message) {
		this.messageOfTheDay = message;
	}
	
	public String MOTD() {
		return this.messageOfTheDay;
	}

	public Resident getLeader() {
		return CivGlobal.getResidentViaUUID(UUID.fromString(leaderName));
	}

	public void setLeader(Resident leader) {
		this.leaderName = leader.getUUID().toString();
	}

	@Override
	public void delete() throws SQLException {
		
		/* First delete all of our groups. */
		if (this.leaderGroup != null) {
			this.leaderGroup.delete();
		}
		
		if (this.adviserGroup != null) {
			this.adviserGroup.delete();
		}
		
		/* Delete all of our towns. */
		for (Town t : getTowns()) {
			t.delete();
		}
		
		/* Delete all relationships with other civs. */
		this.diplomacyManager.deleteAllRelations();
		
		SQL.deleteNamedObject(this, TABLE_NAME);
		CivGlobal.removeCiv(this);
		if (this.isConquered()) {
			CivGlobal.removeConqueredCiv(this);
		}
	}

	public EconObject getTreasury() {
		return treasury;
	}

	public void setTreasury(EconObject treasury) {
		this.treasury = treasury;
	}

	public String getLeaderGroupName() {
		return "leaders";
	}

	public void setLeaderGroupName(String leaderGroupName) {
		this.leaderGroupName = "leaders";
	}

	public String getAdvisersGroupName() {
		return "advisers";
	}

	public void setAdvisersGroupName(String advisersGroupName) {
		this.advisersGroupName = "advisers";
	}

	public double getIncomeTaxRate() {
		return incomeTaxRate;
	}

	public void setIncomeTaxRate(double taxRate) {
		this.incomeTaxRate = taxRate;
	}

	public Town getTown(String name) {
		return towns.get(name.toLowerCase());
	}
	
	public void addTown(Town town) {
		towns.put(town.getName().toLowerCase(), town);
	}

	public int getTownCount() {
		return towns.size();
	}

	public String getIncomeTaxRateString() {
		return (this.incomeTaxRate*100)+"%";
	}

	public static void newCiv(String name, String capitolName, Resident resident,
			Player player, Location loc) throws CivException {
		
		ItemStack stack = player.getInventory().getItemInMainHand();
		/*
		 * Verify we have the correct item somewhere in our inventory.
		 */
		LoreCraftableMaterial craftMat = LoreCraftableMaterial.getCraftMaterial(stack);
		if (craftMat == null || !craftMat.hasComponent("FoundCivilization")) {
			throw new CivException(CivSettings.localize.localizedString("civ_found_notItem"));
		}
		
		Civilization existCiv = CivGlobal.getCiv(name);
		if (existCiv != null) {
			throw new CivException(CivSettings.localize.localizedString("var_civ_found_civExists",name));
		}
		
		Town existTown = CivGlobal.getTown(capitolName);
		if (existTown != null) {
			throw new CivException(CivSettings.localize.localizedString("var_civ_found_townExists",capitolName));
		}
		
		if (resident.hasCamp()) {
			throw new CivException(CivSettings.localize.localizedString("civ_found_mustleavecamp"));
		}
		
		//Test that we are not too close to another civ
		try {
			int min_distance = CivSettings.getInteger(CivSettings.civConfig, "civ.min_distance");
			ChunkCoord foundLocation = new ChunkCoord(loc);
			
			for (CultureChunk cc : CivGlobal.getCultureChunks()) {
				double dist = foundLocation.distance(cc.getChunkCoord());
				if (dist <= min_distance) {
					DecimalFormat df = new DecimalFormat();
					throw new CivException(CivSettings.localize.localizedString("var_civ_found_errorTooClose1",cc.getCiv().getName(),df.format(dist),min_distance));
				}
			}	
		} catch (InvalidConfiguration e1) {
			e1.printStackTrace();
			throw new CivException(CivSettings.localize.localizedString("internalException"));
		}
		
		try {
			Civilization civ = new Civilization(name, capitolName, resident);
			try {
				civ.saveNow();
			} catch (SQLException e) {
				CivLog.error("Caught exception:"+e.getMessage()+" error code:"+e.getErrorCode());
				if (e.getMessage().contains("Duplicate entry")) {
					SQL.deleteByName(name, TABLE_NAME);
					throw new CivException(CivSettings.localize.localizedString("civ_found_databaseException"));
				}
			}
			
			// Create permission groups for civs.
			PermissionGroup leadersGroup = new PermissionGroup(civ, "leaders");
			leadersGroup.addMember(resident);
			leadersGroup.saveNow();
			civ.setLeaderGroup(leadersGroup);
		
			PermissionGroup adviserGroup = new PermissionGroup(civ, "advisers");
			adviserGroup.saveNow();
			civ.setAdviserGroup(adviserGroup);
			
			/* Save this civ in the db and hashtable. */
			try {		
				Town.newTown(resident, capitolName, civ, true, true, loc);
			} catch (CivException e) {
				e.printStackTrace();
				civ.delete();
				leadersGroup.delete();
				adviserGroup.delete();
				throw e;
			}
			
			CivGlobal.addCiv(civ);
			ItemStack newStack = new ItemStack(Material.AIR);
			player.getInventory().setItemInMainHand(newStack);
			CivMessage.globalTitle(CivSettings.localize.localizedString("var_civ_found_successTitle",civ.getName()),CivSettings.localize.localizedString("var_civ_found_successSubTitle",civ.getCapitolName(),player.getName()));
			
		} catch (InvalidNameException e) {
			throw new CivException(CivSettings.localize.localizedString("var_civ_found_invalidName",name));
		} catch (SQLException e) {
			e.printStackTrace();
			throw new CivException(CivSettings.localize.localizedString("internalDatabaseException"));
		}
		
	}

	public String getCapitolName() {
		return capitolName;
	}

	public void setCapitolName(String capitolName) {
		this.capitolName = capitolName;
	}

	public void addGroup(PermissionGroup grp) {
		
		if (grp.getName().equalsIgnoreCase(this.leaderGroupName)) {
			this.setLeaderGroup(grp);
		} else if (grp.getName().equalsIgnoreCase(this.advisersGroupName)) {
			this.setAdviserGroup(grp);
		}		
	}

	public PermissionGroup getLeaderGroup() {
		return leaderGroup;
	}

	public void setLeaderGroup(PermissionGroup leaderGroup) {
		this.leaderGroup = leaderGroup;
	}

	public PermissionGroup getAdviserGroup() {
		return adviserGroup;
	}

	public void setAdviserGroup(PermissionGroup adviserGroup) {
		this.adviserGroup = adviserGroup;
	}

	public Collection<Town> getTowns() {
		return this.towns.values();
	}
	
//	public Collection<Town> getEffectiveTowns() {
//		// Gets all towns in this civ, plus any towns from our vassals
//		// or towns in liberation mode.
//		
//		ArrayList<Town> effectiveTowns = new ArrayList<Town>();
//		for (Town t : this.towns.values()) {
//			effectiveTowns.add(t);
//		}
//		
//		for (Relation relation : this.getDiplomacyManager().getRelations()) {
//			if (relation.getStatus().equals(Relation.Status.VASSAL)) {
//				for (Town t : relation.getOtherCiv().getTowns()) {
//					effectiveTowns.add(t);
//				}
//			}
//		}
//		
//		for (Town t : CivGlobal.getTowns()) {
//			if (t.isInLiberationMode() && t.getLiberationCiv() == this) {
//				effectiveTowns.add(t);
//			}
//		}
//		
//		return effectiveTowns;
//	}

	public double getWarUpkeep() {
		double upkeep = 0;
		boolean doublePenalty = false;
		
		/* calculate war upkeep from being an aggressor. */
		for (Relation relation : this.getDiplomacyManager().getRelations()) {
			if (relation.getStatus() == Status.WAR) {
				if (relation.getAggressor() == this) {
					double thisWarUpkeep = 0;
					int ourScore = CivGlobal.getScoreForCiv(this);
					int theirScore = CivGlobal.getScoreForCiv(relation.getOtherCiv());
					int scoreDiff = ourScore - theirScore;
					try {
						thisWarUpkeep += CivSettings.getDouble(CivSettings.warConfig, "war.upkeep_per_war");
					} catch (InvalidConfiguration e) {
						e.printStackTrace();
						return 0;
					}
					if (scoreDiff > 0) {
						double war_penalty;
						try {
							war_penalty = CivSettings.getDouble(CivSettings.warConfig, "war.upkeep_per_war_multiplier");
							
						} catch (InvalidConfiguration e) {
							e.printStackTrace();
							return 0;
						}
						thisWarUpkeep += (scoreDiff)*war_penalty;
					}
					
					/* Try to find notredame in ourenemies buff list or their allies list. */
					ArrayList<Civilization> allies = new ArrayList<Civilization>();
					allies.add(relation.getOtherCiv());
					for (Relation relation2 : relation.getOtherCiv().getDiplomacyManager().getRelations()) {
						if (relation2.getStatus() == Status.ALLY) {
							allies.add(relation2.getOtherCiv());
						}
					}
					
					for (Civilization civ : allies) {
						for (Town t : civ.getTowns()) {
							if(t.getBuffManager().hasBuff("buff_notre_dame_extra_war_penalty")) {
								doublePenalty = true;
								break;
							}
						}
					}
					
					if (doublePenalty) {
						thisWarUpkeep *= 2;
					}
					
					upkeep += thisWarUpkeep;
				}
			}
		}
	
		return upkeep;
	}
	
	public double getWarUnhappiness() {
		double happy = 0;
		
		/* calculate war upkeep from being an aggressor. */
		for (Relation relation : this.getDiplomacyManager().getRelations()) {
			if (relation.getStatus() == Status.WAR) {
				if (relation.getAggressor() == this) {
					double thisWarUpkeep = 0;
					int ourScore = CivGlobal.getScoreForCiv(this);
					int theirScore = CivGlobal.getScoreForCiv(relation.getOtherCiv());
					int scoreDiff = ourScore - theirScore;
					try {
						thisWarUpkeep += CivSettings.getDouble(CivSettings.happinessConfig, "happiness.per_war");
					} catch (InvalidConfiguration e) {
						e.printStackTrace();
						return 0;
					}
					if (scoreDiff > 0) {
						double war_penalty;
						try {
							war_penalty = CivSettings.getDouble(CivSettings.happinessConfig, "happiness.per_war_score");
							double addedFromPoints = (scoreDiff)*war_penalty;
							addedFromPoints = Math.min(CivSettings.getDouble(CivSettings.happinessConfig, "happiness.per_war_score_max"), addedFromPoints);

							thisWarUpkeep += addedFromPoints;
						} catch (InvalidConfiguration e) {
							e.printStackTrace();
							return 0;
						}
					}
					
					happy += thisWarUpkeep;
				}
			}
		}
	
		return happy;
	}
	
	
	public double getDistanceUpkeepAtLocation(Location capitolTownHallLoc, Location townHallLoc, boolean touching) throws InvalidConfiguration {
		double town_distance_base_upkeep = CivSettings.getDoubleCiv("civ.town_distance_base_upkeep");
		double distance_multiplier_touching = CivSettings.getDoubleCiv("civ.town_distance_multiplier");
		double distance_multiplier_not_touching = CivSettings.getDoubleCiv("civ.town_distance_multiplier_outside_culture");
		double maxDistanceUpkeep = CivSettings.getDoubleCiv("civ.town_distance_upkeep_max");
		
		double distance = capitolTownHallLoc.distance(townHallLoc);
		double distanceUpkeep = 0;
		if (touching) {
			 distanceUpkeep = town_distance_base_upkeep*(Math.pow(distance, distance_multiplier_touching));
		} else {
			 distanceUpkeep = town_distance_base_upkeep*(Math.pow(distance, distance_multiplier_not_touching));
		}
		
		if (distanceUpkeep > maxDistanceUpkeep) {
			distanceUpkeep = maxDistanceUpkeep;
		}
		
		distanceUpkeep = Math.round(distanceUpkeep);
		return distanceUpkeep;
	}
	
	public double getDistanceHappiness(Location capitolTownHallLoc, Location townHallLoc, boolean touching) throws InvalidConfiguration {
		double town_distance_base_happy = CivSettings.getDouble(CivSettings.happinessConfig, "happiness.distance_base");
		double distance_multiplier_touching =CivSettings.getDouble(CivSettings.happinessConfig, "happiness.distance_multiplier");
		double distance_multiplier_not_touching = CivSettings.getDouble(CivSettings.happinessConfig, "happiness.distance_multiplier_outside_culture");
		double maxDistanceHappiness = CivSettings.getDouble(CivSettings.happinessConfig, "happiness.distance_max");
		double distance = capitolTownHallLoc.distance(townHallLoc);
		double distance_happy = 0;
		
		if (touching) {
			 distance_happy = town_distance_base_happy*(Math.pow(distance, distance_multiplier_touching));
		} else {
			 distance_happy = town_distance_base_happy*(Math.pow(distance, distance_multiplier_not_touching));
		}
		
		if (distance_happy > maxDistanceHappiness) {
			distance_happy = maxDistanceHappiness;
		}
		
		distance_happy = Math.round(distance_happy);
		return distance_happy;
	}
	
	public Location getCapitolTownHallLocation() {
		Town capitol = this.getTown(capitolName);
		if (capitol == null) {
			return null;
		}
		
		for (Structure struct : capitol.getStructures()) {
			if (struct instanceof Capitol) {
				return struct.getCorner().getLocation();
			}
		}
		
		return null;		
	}
	
	public Capitol getCapitolStructure() {
		Town capitol = this.getTown(capitolName);
		if (capitol == null) {
			return null;
		}
		
		for (Structure struct : capitol.getStructures()) {
			if (struct instanceof Capitol) {
				return (Capitol)struct;
			}
		}
		
		return null;		
	}
	
	public double payUpkeep() throws InvalidConfiguration, CivException {
		double upkeep = 0;
		this.lastUpkeepPaidMap.clear();

		if (this.isAdminCiv())
		{
			return 0;
		}
		Town capitol = this.getTown(capitolName);
		if (capitol == null) {
			throw new CivException("Civilization found with no capitol!");
		}
				
		for (Town t : this.getTowns()) {
			
			/* Calculate upkeep from extra towns, obviously ignore the capitol itself. */
			if (!this.getCapitolName().equals(t.getName())) {
				try {
					/* Base upkeep per town. */
					upkeep += CivSettings.getDoubleCiv("civ.town_upkeep");
					lastUpkeepPaidMap.put(t.getName()+",base", upkeep);
										
				} catch (InvalidConfiguration e) {
					e.printStackTrace();
				}
			}
		}
		
		upkeep += this.getWarUpkeep();
		
		if (this.getTreasury().hasEnough(upkeep)) {
			/* Have plenty on our coffers, pay the lot and clear all of these towns' debt. */
			this.getTreasury().withdraw(upkeep);
		} else {
			/* Doh! We dont have enough money to pay for our upkeep, go into debt. */
			double diff = upkeep - this.getTreasury().getBalance();
			this.getTreasury().setDebt(this.getTreasury().getDebt()+diff);
			this.getTreasury().withdraw(this.getTreasury().getBalance());
		}
		
		return upkeep;
	}


	public int getDaysInDebt() {
		return daysInDebt;
	}

	public void setDaysInDebt(int daysInDebt) {
		this.daysInDebt = daysInDebt;
	}
	
	public void warnDebt() {
		CivMessage.global(CivSettings.localize.localizedString("var_civ_debtAnnounce",this.getName(),this.getTreasury().getDebt(),CivSettings.CURRENCY_NAME));
	}
	
	
	public void incrementDaysInDebt() {
		daysInDebt++;
		
		if (daysInDebt >= CivSettings.CIV_DEBT_GRACE_DAYS) {
			if (daysInDebt >= CivSettings.CIV_DEBT_SELL_DAYS) {
				if (daysInDebt >= CivSettings.CIV_DEBT_TOWN_SELL_DAYS) {
					CivMessage.global(CivSettings.localize.localizedString("var_civ_fellIntoRuin",this.getName()));
					try {
						this.delete();
						return;
					} catch (SQLException e) {
						e.printStackTrace();
					}
				}
			}
		}
		
		// warn sell..
		CivMessage.global(CivSettings.localize.localizedString("var_civ_debtGlobalAnnounce",this.getName())+" "+getDaysLeftWarning());
		this.save();
		return;
	}

	public String getDaysLeftWarning() {
		
		if (daysInDebt < CivSettings.CIV_DEBT_GRACE_DAYS) {
			return CivSettings.localize.localizedString("var_civ_daystillSaleAnnounce",(CivSettings.CIV_DEBT_GRACE_DAYS-daysInDebt));
		}
		
		if (daysInDebt < CivSettings.CIV_DEBT_SELL_DAYS) {
			return CivSettings.localize.localizedString("var_civ_isForSale1",this.getName(),(CivSettings.CIV_DEBT_SELL_DAYS-daysInDebt));
			
		}
				
		if (daysInDebt < CivSettings.CIV_DEBT_TOWN_SELL_DAYS) {
			return CivSettings.localize.localizedString("var_civ_isForSale2",this.getName(),(CivSettings.CIV_DEBT_TOWN_SELL_DAYS-daysInDebt));		
		}
		
		return "";
	}
	
	private int pickCivColor() {
		int max_retries = 10;
		Random rand = new Random();
		boolean found = false;
		int c = 0;
		//Tries to get CivColor that are not the same.
		for (int i = 0; i < max_retries; i++) {
			c = rand.nextInt(HEX_COLOR_MAX); //Must clip at a 24 bit integer.
			if (testColorForCloseness(c) == false) {
				continue; //reject this color, try again.
			}
			found = true;
			break;
		}
		
		//If we couldn't find a close color withing the max retries, pick any old color as a failsafe.
		if (found == false) {
			c = rand.nextInt();
			System.out.println(CivSettings.localize.localizedString("civ_colorExhaustion"));
		}
		
		return c;
	}
	
	private boolean testColorForCloseness(int c) {
		int tolerance = HEX_COLOR_TOLERANCE; //out of 255 CivColor, 40 is about a 15% difference.

		if (simpleColorDistance(c, 0xFF0000) < tolerance) {
			return false; //never accept pure red, or anything close to it, used for town markers.
		}
		
		if (simpleColorDistance(c, 0xFFFFFF) < tolerance) {
			return false; // not too bright.
		}
		
		if (simpleColorDistance(c, 0x000000) < tolerance) {
			return false; //not too dark/
		}
		
		//Check all the currently held CivColor.
		for (int c2 : CivGlobal.CivColorInUse.keySet()) {
			if (simpleColorDistance(c, c2) < tolerance) {
				return false; //if this color is too close to any other color, reject it.
			}
		}
		return true;
	}
	
	private int simpleColorDistance(int color1, int color2) {
		int red1, red2, blue1, blue2, green1, green2;
		
		red1 = color1 & 0xFF0000;
		red2 = color2 & 0xFF0000;
		green1 = color1 & 0x00FF00;
		green2 = color2 & 0x00FF00;
		blue1 = color1 & 0x0000FF;
		blue2 = color2 & 0x0000FF;
		
		double redPower = Math.pow((red1-red2), 2);
		double greenPower = Math.pow((green1-green2), 2);
		double bluePower = Math.pow((blue1-blue2), 2);
		
		return (int)Math.sqrt(redPower+greenPower+bluePower);
	}

	public String getCultureDescriptionString() {
		String out = "";
			
		out += "<b>"+this.getName()+"</b>";
		
		return out;
	}

	public double getBaseBeakers() {
		return this.baseBeakers;
	}
	
	public double getBeakers() {
		double total = 0;
		
		for (Town town : this.getTowns()) {
			total += town.getBeakers().total;
		}
		
		total += baseBeakers;
		
		return total;
	}

	public void setBaseBeakers(double beakerRate) {
		this.baseBeakers = beakerRate;
	}

	public void addBeakers(double beakers) {
		
		if (beakers == 0) {
			return;
		}
		
		TaskMaster.asyncTask(new UpdateTechBar(this), 0);
		setResearchProgress(getResearchProgress() + beakers);
		
		if (getResearchProgress() >= getResearchTech().getAdjustedBeakerCost(this)) {
			CivMessage.sendCiv(this, CivSettings.localize.localizedString("var_civ_research_Discovery",getResearchTech().name));
			this.addTech(this.getResearchTech());
			this.setResearchProgress(0);
			this.setResearchTech(null);
			
			this.save();
			
			return;
		}
		
		int percentageComplete = (int)((getResearchProgress() / this.getResearchTech().getAdjustedBeakerCost(this))*100);
		if ((percentageComplete % 10) == 0) {
			
			if (percentageComplete != lastTechPercentage) {
				CivMessage.sendCiv(this, CivSettings.localize.localizedString("var_civ_research_currentProgress",getResearchTech().name,percentageComplete));
				lastTechPercentage = percentageComplete;
			}
			
		}
		
		this.save();
	
	}

	public void startTechnologyResearch(ConfigTech tech) throws CivException {		
		if (this.getResearchTech() != null) {
			throw new CivException(CivSettings.localize.localizedString("var_civ_research_switchAlert1",this.getResearchTech().name));
		}
		double cost = tech.getAdjustedTechCost(this);
		
		if (!this.getTreasury().hasEnough(cost)) {
			throw new CivException(CivSettings.localize.localizedString("var_civ_research_notEnoughMoney",cost,CivSettings.CURRENCY_NAME));
		}
		
		if (this.hasTech(tech.id)) {
			throw new CivException(CivSettings.localize.localizedString("civ_research_alreadyDone"));
		}
		
		if (!tech.isAvailable(this)) {
			throw new CivException(CivSettings.localize.localizedString("civ_research_missingRequirements"));
		}
		
		this.setResearchTech(tech);
		this.setResearchProgress(0.0);
	
		this.getTreasury().withdraw(cost);
		TaskMaster.asyncTask(new UpdateTechBar(this),0);
	}

	public ConfigTech getResearchTech() {
		return researchTech;
	}

	public void setResearchTech(ConfigTech researchTech) {
		this.researchTech = researchTech;
	}

	public double getResearchProgress() {
		return researchProgress;
	}

	public void setResearchProgress(double researchProgress) {
		this.researchProgress = researchProgress;
	}

	public void changeGovernment(Civilization civ, ConfigGovernment gov, boolean force) throws CivException {
		if (civ.getGovernment() == gov && !force) {
			throw new CivException(CivSettings.localize.localizedString("var_civ_gov_already",gov.displayName));
		}
		
		if (civ.getGovernment().id.equals("gov_anarchy")) {
			throw new CivException(CivSettings.localize.localizedString("civ_gov_errorAnarchy"));
		}
		
		boolean noanarchy = false;
//		for (Town t : this.getTowns()) {
//			if (t.getBuffManager().hasBuff("buff_noanarchy")) {
//				noanarchy = true;
//				break;
//			}
//		}
		
		if (!noanarchy) {
			String key = "changegov_"+this.getId();
			String value = gov.id;
			
			sessionAdd(key, value);
			
			// Set the town's government to anarchy in the meantime
			civ.setGovernment("gov_anarchy");
			CivMessage.global(CivSettings.localize.localizedString("var_civ_gov_anachyAlert",this.getName()));
		} else {
			civ.setGovernment(gov.id);
			CivMessage.global(CivSettings.localize.localizedString("var_civ_gov_success",civ.getName(),CivSettings.governments.get(gov.id).displayName));
		}
		
		
		civ.save();
		
	}

	public String getUpkeepPaid(Town town, String type) {
		String out = "";
		
		if (lastUpkeepPaidMap.containsKey(town.getName()+","+type)) {
			out += lastUpkeepPaidMap.get(town.getName()+","+type);
		} else {
			out += "0";
		}
		
		return out;
	}

	public void taxPayment(Town town, double amount) {

		Double townPaid = this.lastTaxesPaidMap.get(town.getName());
		if (townPaid == null) {
			townPaid = amount;
		} else {
			townPaid += amount;
		}
		this.lastTaxesPaidMap.put(town.getName(), townPaid);
		double beakerAmount = amount*this.sciencePercentage;
		amount -= beakerAmount;
		this.getTreasury().deposit(amount);
		this.save();
		
		double coins_per_beaker;
		try {
			coins_per_beaker = CivSettings.getDouble(CivSettings.civConfig, "civ.coins_per_beaker");
			
			for (Town t : this.getTowns()) {
				if (t.getBuffManager().hasBuff("buff_greatlibrary_double_tax_beakers")) {
					coins_per_beaker /= 2;
				}
			}
			
		} catch (InvalidConfiguration e) {
			e.printStackTrace();
			return;
		}
		
		DecimalFormat df = new DecimalFormat("#.#");
		double totalBeakers = Double.valueOf(df.format(beakerAmount/coins_per_beaker));
		if (totalBeakers == 0) {
			return;
		}

		if (this.researchTech != null) {	
			this.addBeakers(totalBeakers);
		} else {
			EndGameCondition scienceVictory = EndGameCondition.getEndCondition("end_science");
			if (scienceVictory == null) {
				CivLog.error("Couldn't find science victory, not configured?");
			} else {
				if (scienceVictory.isActive(this)) {
					/* 
					 * We've got an active science victory, lets add these beakers
					 * to the total stored on "the enlightenment"
					 */
					double beakerTotal = totalBeakers;
					((EndConditionScience)scienceVictory).addExtraBeakersToCiv(this, beakerTotal);
					return;
				}
			}		
		}
	}

	public double getSciencePercentage() {
		return sciencePercentage;
	}

	public void setSciencePercentage(double sciencePercentage) {
		if (sciencePercentage > 1.0) {
			sciencePercentage = 1.0;
		}
		
		this.sciencePercentage = sciencePercentage;
	}

	public Collection<ConfigTech> getTechs() {
		return this.techs.values();
	}

	public void depositFromResident(Resident resident, Double amount) throws CivException, SQLException {

		if (resident.getTreasury().hasEnough(amount) == false) {
			throw new CivException(CivSettings.localize.localizedString("var_civ_deposit_NotEnough",CivSettings.CURRENCY_NAME));
		}
		
		if (this.getTreasury().inDebt()) {
			if (this.getTreasury().getDebt() >= amount) {
				this.getTreasury().setDebt(this.getTreasury().getDebt() - amount);
				resident.getTreasury().withdraw(amount);
			} else if (this.getTreasury().getDebt() < amount) {
				double leftAmount = amount - this.getTreasury().getDebt();
				this.getTreasury().setDebt(0);
				this.getTreasury().deposit(leftAmount);				
				resident.getTreasury().withdraw(amount);
			}
			
			if (this.getTreasury().inDebt() == false) {
				this.daysInDebt = 0;
				CivMessage.global(CivSettings.localize.localizedString("var_civ_deposit_cleardebt",this.getName()));				
			}
		} else {
			this.getTreasury().deposit(amount);
			resident.getTreasury().withdraw(amount);
		}
		this.save();
	}
	
	public void sessionAdd(String key, String value) {
		CivGlobal.getSessionDB().add(key, value, this.getId(), 0, 0);
	}

	public void sessionDeleteAll(String key) {
		CivGlobal.getSessionDB().delete_all(key);
	}
	
	public void sessionUpdateInsert(String key, String value) {
		//CivGlobal.getSessionDB().updateInsert(key, value, this.getId(), 0, 0);
	}

	public DiplomacyManager getDiplomacyManager() {
		return diplomacyManager ;
	}

	public void onDefeat(Civilization attackingCiv) {
		/* 
		 * The entire civilization has been defeated. We need to give our towns to the attacker.
		 * Meanwhile our civilization will become dormant. We will NOT remember who the attacker
		 * was, if we revolt we will declare war on anyone who owns our remaining towns.
		 * 
		 * We will hand over all of our native towns, as well as any conquered towns we might have.
		 * Those towns when they revolt will revolt against whomever owns them.
		 */
		
		for (Town town : this.getTowns()) {
			town.onDefeat(attackingCiv);
		}
		
		/* Remove any old relationships this civ may have had. */
		LinkedList<Relation> deletedRelations = new LinkedList<Relation>();
		for (Relation relation : this.getDiplomacyManager().getRelations()) {
			deletedRelations.add(relation);
		}
		for (Relation relation : deletedRelations) {
			try {
				if (relation.getStatus() == Relation.Status.WAR) {
					relation.setStatus(Relation.Status.NEUTRAL);
				}
				relation.delete();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		
		
		/* Remove ourselves from the main global civ list and into a special conquered list. */
		CivGlobal.removeCiv(this);
		CivGlobal.addConqueredCiv(this);
		this.conquered = true;
		this.conquer_date = new Date();		
		this.save();
	}

	public boolean isConquered() {
		return conquered;
	}
	
	
	public void regenControlBlocks() {
		for (Town t : getTowns()) {
			t.getTownHall().regenControlBlocks();
		}
	}

	public boolean isAdminCiv() {
		return adminCiv ;
	}

	public void setAdminCiv(boolean bool) {
		adminCiv = bool;
		if (adminCiv) {
			CivGlobal.addAdminCiv(this);
		} else {
			CivGlobal.removeAdminCiv(this);
		}
		this.save();
	}

	public void repositionPlayers(String reason) {
		if (this.getDiplomacyManager().isAtWar() == false) {
			return;
		}
		
		for (Town t : this.getTowns()) {
			TownHall townhall = t.getTownHall();
			if (townhall == null) {
				CivLog.error("Town hall was null for "+t.getName()+" when trying to reposition players.");
				continue;
			}
			
		//	if (!townhall.isComplete()) {
		//		CivLog.error("Town hall was not completed before war time. Unable to reposition players.");
		//		continue;
		//	}
		
			for (Resident resident : t.getResidents()) {
				//if (townhall.isActive()) {
					BlockCoord revive = townhall.getRandomRevivePoint();
	
					try {
						Player player = CivGlobal.getPlayer(resident);
						ChunkCoord coord = new ChunkCoord(player.getLocation());
						CultureChunk cc = CivGlobal.getCultureChunk(coord);
						if (cc != null && cc.getCiv() != this && 
								cc.getCiv().getDiplomacyManager().atWarWith(this)) {
							CivMessage.send(player, CivColor.Purple+reason);
							player.teleport(revive.getLocation());			
						}
						
						
					} catch (CivException e) {
						// player not online....
					}
//				} else {
//					// use player spawn point instead.
//					Player player;
//					try {
//						player = CivGlobal.getPlayer(resident);
//
//						if (player.getBedSpawnLocation() != null) {
//							player.teleport(player.getBedSpawnLocation());
//							CivMessage.send(player, CivColor.Purple+reason);
//						} else {
//							player.gets
//						}
//					} catch (CivException e) {
//						// player not online
//					}
//				}
			}
		}
	}
	
	public boolean isTownsForSale() {
		if (daysInDebt >= CivSettings.CIV_DEBT_SELL_DAYS) {
			return true;
		}
		return false;
	}
	
	public boolean isForSale() {
		if (this.getTownCount() == 0) {
			return false;
		}
		
		if (daysInDebt >= CivSettings.CIV_DEBT_GRACE_DAYS) {
			return true;
		}
		return false;
	}

	public double getForSalePriceFromCivOnly() {
		int effectivePoints = 0;
		effectivePoints = this.getTechScore();
		double coins_per_point;
		try {
			coins_per_point = CivSettings.getDouble(CivSettings.scoreConfig, "coins_per_point");
		} catch (InvalidConfiguration e) {
			e.printStackTrace();
			return 0;
		}
		return coins_per_point*effectivePoints;
	}

	public double getTotalSalePrice() {
		double price = getForSalePriceFromCivOnly();
		for (Town town : this.getTowns()) {
			price += town.getForSalePrice();
		}
		return price;
	}
	
	public void buyCiv(Civilization civ) throws CivException {
		
		if (!this.getTreasury().hasEnough(civ.getTotalSalePrice())) {
			throw new CivException(CivSettings.localize.localizedString("civ_buy_notEnough")+" "+CivSettings.CURRENCY_NAME);
		}
		
		this.getTreasury().withdraw(civ.getTotalSalePrice());
		this.mergeInCiv(civ);
	}
	
	public int getTechScore() {
		int points = 0;
		// Count technologies.
		for (ConfigTech t : this.getTechs()) {
			points += t.points;
		}
		return points;
	}
	
	public int getScore() {
		int points = 0;
		for (Town t : this.getTowns()) {
			points += t.getScore();
		}
		
		points += getTechScore();
		
		return points;
	}

	public boolean hasResident(Resident resident) {
		if (resident == null) {
			return false;
		}
		
		for (Town t : this.getTowns()) {
			if (t.hasResident(resident)) {
				return true;
			}
		}
		
		return false;
	}

	public void removeTown(Town town) {
		this.towns.remove(town.getName().toLowerCase());
	}

	public void mergeInCiv(Civilization oldciv) {
		if (oldciv == this) {
			return;
		}
		
		/* Grab each town underneath and add it to us. */
		for (Town town : oldciv.getTowns()) {
			town.changeCiv(this);
			town.setDebt(0);
			town.setDaysInDebt(0);
			town.save();
		}
		
		if (oldciv.towns.size() > 0) {
			CivLog.error("CIV SOMEHOW STILL HAS TOWNS AFTER WE GAVE THEM ALL AWAY WTFWTFWTFWTF.");
			this.towns.clear();
		}
		
		try {
			oldciv.delete();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		CivGlobal.processCulture();
	}
	
	public void buyTown(Town town) throws CivException {

		if (!this.getTreasury().hasEnough(town.getForSalePrice())) {
			throw new CivException(CivSettings.localize.localizedString("civ_buy_notEnough")+" "+CivSettings.CURRENCY_NAME);
		}
		
		this.getTreasury().withdraw(town.getForSalePrice());
		town.changeCiv(this);
		town.setMotherCiv(null);
		town.setDebt(0);
		town.setDaysInDebt(0);
		town.save();
		CivGlobal.processCulture();
		CivMessage.global(CivSettings.localize.localizedString("var_civ_buyTown_Success1",this.getName(),this.getName()));

	}
	
	public double getRevolutionFee() {
		
		try {
			double base_coins = CivSettings.getDouble(CivSettings.warConfig, "revolution.base_cost");
			double coins_per_town = CivSettings.getDouble(CivSettings.warConfig, "revolution.coins_per_town");
			double coins_per_point = CivSettings.getDouble(CivSettings.warConfig, "revolution.coins_per_point");
			double max_fee = CivSettings.getDouble(CivSettings.warConfig, "revolution.maximum_fee");
			
			double total_coins = base_coins;
			
			double motherCivPoints = this.getTechScore();
			for (Town t : CivGlobal.getTowns()) {
				if (t.getMotherCiv() == this) {
					motherCivPoints += t.getScore();
					total_coins += coins_per_town;
				}
			}
			
			total_coins += motherCivPoints*coins_per_point;
			
			if (total_coins > max_fee) {
				total_coins = max_fee;
			}
			
			return total_coins;
		
		} catch (InvalidConfiguration e) {
			e.printStackTrace();
			return Double.MAX_VALUE;
		}
	}

	public void setConquered(boolean b) {
		this.conquered = b;
	}

	public Collection<Resident> getOnlineResidents() {

		LinkedList<Resident> residents = new LinkedList<Resident>();
		for (Town t : this.getTowns()) {
			residents.addAll(t.getOnlineResidents());
		}
		
		return residents;
	}

	public Date getConqueredDate() {
		return this.conquer_date;
	}

	public void capitulate() {
		for (Town t : CivGlobal.getTowns()) {
			if (t.getMotherCiv() == this) {
				t.setMotherCiv(null);
				t.save();
			}
		}
		
		try {
			this.delete();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		CivMessage.global(CivSettings.localize.localizedString("var_civ_capitulate_Success1",this.getName()));	
	}

	/*
	 * populates the sources with happiness sources.
	 */
	public double getCivWideUnhappiness(HashMap<String, Double> sources) {
		double total = 0;
		
		try {
			/* Get happiness per town. */
			double per_town = CivSettings.getDouble(CivSettings.happinessConfig, "happiness.per_town");
			double per_captured_town = CivSettings.getDouble(CivSettings.happinessConfig, "happiness.per_captured_town");

			double happy_town = 0;
			double happy_captured_town = 0;
			for (Town town : this.getTowns()) {
				if (town.getMotherCiv() == null) {
					if (!town.isCapitol()) {
						happy_town += per_town;
					}
				} else {
					happy_captured_town += per_captured_town;
				}
			}
			
			total += happy_town;
			sources.put("Towns", happy_town);
			
			total += happy_captured_town;
			sources.put("Captured Towns", happy_captured_town);

			/* Get unhappiness from wars. */
			double war_happy = this.getWarUnhappiness();
			total += war_happy;
			sources.put("War", war_happy);
			
		} catch (InvalidConfiguration e) {
			e.printStackTrace();
		}
		
		return total;
	}
	
	/*
	 * Gets distance happiness for a town.
	 */
	 public double getDistanceHappiness(Town town) {
		 Structure capitolTownHall = this.getCapitolStructure();
		 Structure townHall = town.getTownHall();		
		 if (capitolTownHall != null && townHall != null) {
			 Location loc_cap = capitolTownHall.getCorner().getLocation();
			 Location loc_town = townHall.getCorner().getLocation();
			 double distanceHappy;
			 if (town.getMotherCiv() == null || town.getMotherCiv() == this) {
				 try {
					distanceHappy = this.getDistanceHappiness(loc_cap, loc_town, town.touchesCapitolCulture(new HashSet<Town>()));
				} catch (InvalidConfiguration e) {
					e.printStackTrace();
					return 0.0;
				}
			 } else {
				 distanceHappy = 0;
			 }
			 return distanceHappy;
		 }
		 return 0.0;
	 }

	public void declareAsWinner(EndGameCondition end) {
		String out = CivSettings.localize.localizedString("var_civ_victory_end1",this.getName(),end.getVictoryName());
		CivGlobal.getSessionDB().add("endgame:winningCiv", out, 0, 0, 0);
		CivMessage.global(out);
	}

	public void winConditionWarning(EndGameCondition end, int daysLeft) {
		CivMessage.global(CivSettings.localize.localizedString("var_civ_victory_end2",this.getName(),end.getVictoryName(),daysLeft));
	}

	public double getPercentageConquered() {
		
		int totalCivs = CivGlobal.getCivs().size() + CivGlobal.getConqueredCivs().size();		
		int conqueredCivs = 1; /* Your civ already counts */
		
		for (Civilization civ : CivGlobal.getConqueredCivs()) {
			Town capitol = CivGlobal.getTown(civ.getCapitolName());
			if (capitol == null) {
				/* Invalid civ? */
				totalCivs--;
				continue;
			}
			
			if (capitol.getCiv() == this) {
				conqueredCivs++;
			}
		}
		
		double percent = (double)conqueredCivs / (double)totalCivs;
		return percent;
	}

	public void processUnusedBeakers() {
		
		EndGameCondition scienceVictory = EndGameCondition.getEndCondition("end_science");
		if (scienceVictory == null) {
			CivLog.error("Couldn't find science victory, not configured?");
		} else {
			if (scienceVictory.isActive(this)) {
				/* 
				 * We've got an active science victory, lets add these beakers
				 * to the total stored on "the enlightenment"
				 */
				double beakerTotal = this.getBeakers()/BeakerTimer.BEAKER_PERIOD;
				((EndConditionScience)scienceVictory).addExtraBeakersToCiv(this, beakerTotal);
				return;
			}
		}
		
		for (Town town : this.towns.values()) {
			town.addUnusedBeakers(town.getBeakers().total / BeakerTimer.BEAKER_PERIOD);
		}
	}

	public boolean areLeadersInactive() {
		try {
			int leader_inactive_days = CivSettings.getInteger(CivSettings.civConfig, "civ.leader_inactive_days");
			
			for (Resident resident : this.getLeaderGroup().getMemberList()) {
				if (!resident.isInactiveForDays(leader_inactive_days)) {
					return false;
				}
			}
			
		} catch (InvalidConfiguration e) {
			e.printStackTrace();
			return false;
		}
		
		return true;
	}

	public void rename(String name) throws CivException, InvalidNameException {
		
		Civilization other = CivGlobal.getCiv(name);
		if (other != null) {
			throw new CivException(CivSettings.localize.localizedString("civ_rename_errorExists"));
		}
		
		other = CivGlobal.getConqueredCiv(name);
		if (other != null) {
			throw new CivException(CivSettings.localize.localizedString("civ_rename_errorExists"));
		}
		
		if (this.conquered) {
			CivGlobal.removeConqueredCiv(this);
		} else {
			CivGlobal.removeCiv(this);
		}
		
		String oldName = this.getName();
		this.setName(name);
		this.save();
		
		if (this.conquered) {
			CivGlobal.addConqueredCiv(this);
		} else {
			CivGlobal.addCiv(this);
		}
		
		CivMessage.global(CivSettings.localize.localizedString("var_civ_rename_success1",oldName,this.getName()));
	}

	public ArrayList<RespawnLocationHolder> getAvailableRespawnables() {
		ArrayList<RespawnLocationHolder> respawns = new ArrayList<RespawnLocationHolder>();
		
		for (Town town : this.getTowns()) {
			TownHall townhall = town.getTownHall();
			if (townhall != null && townhall.isActive()) {
				if (!townhall.getTown().isCapitol() && town.defeated) {
					/* Do not respawn at defeated towns. */
					continue;
				}

				respawns.add(townhall);
			}
		}
		
		for (WarCamp camp : this.warCamps) {
			respawns.add(camp);
		}
		
		return respawns;
		
	}

	public void addWarCamp(WarCamp camp) {
		this.warCamps.add(camp);
	}

	public LinkedList<WarCamp> getWarCamps() {
		return this.warCamps;
	}

	public void onWarEnd() {
		for (WarCamp camp : this.warCamps) {
			camp.onWarEnd();
		}
		
		for (Town town : towns.values()) {
			TownHall th = town.getTownHall();
			if (th != null) {
				th.setHitpoints(th.getMaxHitPoints());
				th.save();
			}
		}
	}

	public Date getCreated() {
		return created_date;
	}

	public void setCreated(Date created_date) {
		this.created_date = created_date;
	}

	public void validateGift() throws CivException {
		try {
			int min_gift_age = CivSettings.getInteger(CivSettings.civConfig, "civ.min_gift_age");
		
			if (!DateUtil.isAfterDays(created_date, min_gift_age)) {
				throw new CivException(CivSettings.localize.localizedString("var_civ_gift_tooyoung1",this.getName(),min_gift_age));
			}
		} catch (InvalidConfiguration e) {
			throw new CivException(CivSettings.localize.localizedString("internalException"));
		}			
	}

	public void clearAggressiveWars() {
		/* If this civ is the aggressor in any wars. Cancel the, this happens when civs go into debt. */
		LinkedList<Relation> removeUs = new LinkedList<Relation>();
		for (Relation relation : this.getDiplomacyManager().getRelations()) {
			if (relation.getStatus().equals(Relation.Status.WAR)) {
				if (relation.getAggressor() == this) {
					removeUs.add(relation);
				}
			}
		}
		
		for (Relation relation : removeUs) {
			this.getDiplomacyManager().deleteRelation(relation);
			CivMessage.global(CivSettings.localize.localizedString("var_civ_debt_endWar",this.getName(),relation.getOtherCiv().getName()));
		}
		
	}

	public int getMergeCost() {
		int total = 0;
		for (Town town : this.towns.values()) {
			total += town.getGiftCost();
		}
		
		return total;
	}

	public Structure getNearestStructureInTowns(Location loc) {
		Structure nearest = null;
		double lowest_distance = Double.MAX_VALUE;
		
		for (Town town : towns.values()) {
			for (Structure struct : town.getStructures()) {
				double distance = struct.getCenterLocation().getLocation().distance(loc);
				if (distance < lowest_distance ) {
					lowest_distance = distance;
					nearest = struct;
				}
			}
		}
		
		return nearest;
	}
	
	public ItemStack getRandomLeaderSkull(String message) {
		Random rand = new Random();
		int i = rand.nextInt(this.getLeaderGroup().getMemberCount());
		int count = 0;
		Resident resident = CivGlobal.getResident(this.getLeader());
		
		for (Resident res : this.getLeaderGroup().getMemberList()) {
			if (count == i) {
				resident = res;
				break;
			}
		}
		
		String leader = "";
		if (resident != null) {
			leader = resident.getName();
		}
		
		ItemStack stack = ItemManager.spawnPlayerHead(leader, message+" ("+leader+")");
		return stack;
	}

	public int getCurrentEra() {
		return currentEra;
	}

	public void setCurrentEra(int currentEra) {
		this.currentEra = currentEra;
		
		if (this.currentEra > CivGlobal.highestCivEra && !this.isAdminCiv())
		{
			CivGlobal.setCurrentEra(this.currentEra, this);
		}
	}
	
	public double getSettlerCost() {
		return this.settlerCost;
	}
	
	public void incrementSettlerCost(double incremention) {
		this.settlerCost += incremention;
	}
}
