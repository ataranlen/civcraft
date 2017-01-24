package com.avrgaming.civcraft.object;

import java.util.concurrent.CountedCompleter;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Spliterator;
import java.util.NoSuchElementException;
import java.util.concurrent.locks.LockSupport;
import sun.misc.Contended;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.ToIntFunction;
import java.util.function.ToLongFunction;
import java.util.function.ToDoubleFunction;
import java.util.function.IntBinaryOperator;
import java.util.function.ToIntBiFunction;
import java.util.function.LongBinaryOperator;
import java.util.function.ToLongBiFunction;
import java.util.function.DoubleBinaryOperator;
import java.util.function.ToDoubleBiFunction;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ThreadLocalRandom;
import java.util.Enumeration;
import java.util.function.Function;
import java.util.function.BiFunction;
import java.util.function.BiConsumer;
import java.io.ObjectInputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.Set;
import java.util.Map;
import java.lang.reflect.Type;
import java.lang.reflect.ParameterizedType;
import sun.misc.Unsafe;
import java.io.ObjectStreamField;
import java.io.Serializable;
import java.util.concurrent.ConcurrentMap;
import java.util.AbstractMap;
import com.avrgaming.civcraft.util.DateUtil;
import com.avrgaming.civcraft.interactive.InteractiveResponse;
import com.avrgaming.civcraft.interactive.InteractiveBuildableRefresh;
import com.avrgaming.global.perks.components.CustomTemplate;
import com.avrgaming.global.perks.Perk;
import com.avrgaming.civcraft.util.CivColor;
import com.avrgaming.civcraft.components.AttributeWarUnhappiness;
import com.avrgaming.civcraft.config.ConfigTownHappinessLevel;
import com.avrgaming.civcraft.config.ConfigGovernment;
import java.util.UUID;
import com.avrgaming.civcraft.threading.TaskMaster;
import com.avrgaming.civcraft.threading.sync.SyncUpdateTags;
import com.avrgaming.civcraft.util.ItemFrameStorage;
import com.avrgaming.civcraft.components.AttributeRate;
import com.avrgaming.civcraft.road.Road;
import com.avrgaming.civcraft.structure.Wall;
import com.avrgaming.civcraft.structure.TradeOutpost;
import com.avrgaming.civcraft.template.Template;
import com.avrgaming.civcraft.config.ConfigTownLevel;
import com.avrgaming.civcraft.config.ConfigUnit;
import com.avrgaming.civcraft.structure.TownHall;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Material;
import com.avrgaming.civcraft.config.ConfigBuildableInfo;
import com.avrgaming.civcraft.items.units.Unit;
import java.text.DecimalFormat;
import com.avrgaming.civcraft.war.War;
import org.bukkit.Location;
import com.avrgaming.civcraft.config.ConfigCultureLevel;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.config.ConfigBuff;
import com.avrgaming.civcraft.structure.Temple;
import com.avrgaming.civcraft.components.AttributeBase;
import com.avrgaming.civcraft.components.Component;
import com.avrgaming.civcraft.config.ConfigHappinessState;
import com.avrgaming.civcraft.util.WorldCord;
import com.avrgaming.civcraft.exception.AlreadyRegisteredException;
import java.util.Collection;
import com.avrgaming.civcraft.exception.InvalidConfiguration;
import com.avrgaming.civcraft.config.CivSettings;
import java.util.Iterator;
import com.avrgaming.civcraft.database.SQLUpdate;
import com.avrgaming.civcraft.exception.InvalidNameException;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivGlobal;
import java.sql.ResultSet;
import java.sql.SQLException;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.database.SQL;
import java.util.HashMap;
import java.util.Date;
import com.avrgaming.civcraft.randomevents.RandomEvent;
import java.util.LinkedList;
import com.avrgaming.civcraft.threading.tasks.BuildUndoTask;
import com.avrgaming.civcraft.threading.tasks.BuildAsyncTask;
import com.avrgaming.civcraft.items.BonusGoodie;
import com.avrgaming.civcraft.config.ConfigTownUpgrade;
import java.util.HashSet;
import java.util.ArrayList;
import com.avrgaming.civcraft.permission.PermissionGroup;
import com.avrgaming.civcraft.structure.Buildable;
import com.avrgaming.civcraft.structure.Structure;
import com.avrgaming.civcraft.structure.wonders.Wonder;
import com.avrgaming.civcraft.util.BlockCoord;
import com.avrgaming.civcraft.util.ChunkCoord;
import java.util.concurrent.ConcurrentHashMap;

public class Town extends SQLObject
{
    private ConcurrentHashMap<String, Resident> residents;
    private ConcurrentHashMap<String, Resident> fakeResidents;
    private ConcurrentHashMap<ChunkCoord, TownChunk> townChunks;
    private ConcurrentHashMap<ChunkCoord, TownChunk> outposts;
    private ConcurrentHashMap<ChunkCoord, CultureChunk> cultureChunks;
    private ConcurrentHashMap<BlockCoord, Wonder> wonders;
    private ConcurrentHashMap<BlockCoord, Structure> structures;
    private ConcurrentHashMap<BlockCoord, Buildable> disabledBuildables;
    private int level;
    private double taxRate;
    private double flatTax;
    private Civilization civ;
    private Civilization motherCiv;
    private int daysInDebt;
    private double baseHammers;
    private double extraHammers;
    public Buildable currentStructureInProgress;
    public Buildable currentWonderInProgress;
    private int culture;
    private PermissionGroup defaultGroup;
    private PermissionGroup mayorGroup;
    private PermissionGroup assistantGroup;
    private double unusedBeakers;
    private String defaultGroupName;
    private String mayorGroupName;
    private String assistantGroupName;
    public ArrayList<TownChunk> savedEdgeBlocks;
    public HashSet<Town> townTouchList;
    private ConcurrentHashMap<String, PermissionGroup> groups;
    private EconObject treasury;
    private ConcurrentHashMap<String, ConfigTownUpgrade> upgrades;
    private ConcurrentHashMap<String, BonusGoodie> bonusGoodies;
    private BuffManager buffManager;
    private boolean pvp;
    public ArrayList<BuildAsyncTask> build_tasks;
    public ArrayList<BuildUndoTask> undo_tasks;
    public Buildable lastBuildableBuilt;
    public boolean leaderWantsToDisband;
    public boolean mayorWantsToDisband;
    public HashSet<String> outlaws;
    public boolean claimed;
    public boolean defeated;
    public LinkedList<Buildable> invalidStructures;
    public int saved_bank_level;
    public int saved_trommel_level;
    public int saved_tradeship_upgrade_levels;
    public int saved_quarry_level;
    public int saved_fish_hatchery_level;
    public double saved_bank_interest_amount;
    private double baseHappy;
    private double baseUnhappy;
    private RandomEvent activeEvent;
    private Date lastBuildableRefresh;
    private Date created_date;
    public static final int ATTR_TIMEOUT_SECONDS = 5;
    public HashMap<String, AttrCache> attributeCache;
    private double baseGrowth;
    public static final String TABLE_NAME = "TOWNS";
    private static String lastMessage;
    
    static {
        Town.lastMessage = null;
    }
    
    public static void init() throws SQLException {
        if (!SQL.hasTable("TOWNS")) {
            final String table_create = "CREATE TABLE " + SQL.tb_prefix + "TOWNS" + " (" + "`id` int(11) unsigned NOT NULL auto_increment," + "`name` VARCHAR(64) NOT NULL," + "`civ_id` int(11) NOT NULL DEFAULT 0," + "`master_civ_id` int(11) NOT NULL DEFAULT 0," + "`mother_civ_id` int(11) NOT NULL DEFAULT 0," + "`defaultGroupName` mediumtext DEFAULT NULL," + "`mayorGroupName` mediumtext DEFAULT NULL," + "`assistantGroupName` mediumtext DEFAULT NULL," + "`upgrades` mediumtext DEFAULT NULL," + "`level` int(11) DEFAULT 1," + "`debt` double DEFAULT 0," + "`coins` double DEFAULT 0," + "`daysInDebt` int(11) DEFAULT 0," + "`flat_tax` double NOT NULL DEFAULT '0'," + "`tax_rate` double DEFAULT 0," + "`extra_hammers` double DEFAULT 0," + "`culture` int(11) DEFAULT 0," + "`created_date` long," + "`outlaws` mediumtext DEFAULT NULL," + "`dbg_civ_name` mediumtext DEFAULT NULL," + "UNIQUE KEY (`name`), " + "PRIMARY KEY (`id`)" + ")";
            SQL.makeTable(table_create);
            CivLog.info("Created TOWNS table");
        }
        else {
            CivLog.info("TOWNS table OK!");
            SQL.makeCol("outlaws", "mediumtext", "TOWNS");
            SQL.makeCol("daysInDebt", "int(11)", "TOWNS");
            SQL.makeCol("mother_civ_id", "int(11)", "TOWNS");
            SQL.makeCol("dbg_civ_name", "mediumtext", "TOWNS");
            SQL.makeCol("created_date", "long", "TOWNS");
        }
    }
    
    @Override
    public void load(final ResultSet rs) throws SQLException, InvalidNameException, CivException {
        this.setId(rs.getInt("id"));
        this.setName(rs.getString("name"));
        this.setLevel(rs.getInt("level"));
        this.setCiv(CivGlobal.getCivFromId(rs.getInt("civ_id")));
        final Integer motherCivId = rs.getInt("mother_civ_id");
        if (motherCivId != null && motherCivId != 0) {
            Civilization mother = CivGlobal.getConqueredCivFromId(motherCivId);
            if (mother == null) {
                mother = CivGlobal.getCivFromId(motherCivId);
            }
            if (mother == null) {
                CivLog.warning("Unable to find a mother civ with ID:" + motherCivId + "!");
            }
            else {
                this.setMotherCiv(mother);
            }
        }
        if (this.getCiv() == null) {
            CivLog.error("TOWN:" + this.getName() + " WITHOUT A CIV, id was:" + rs.getInt("civ_id"));
            CivGlobal.orphanTowns.add(this);
            throw new CivException("Failed to load town, bad data.");
        }
        this.setDaysInDebt(rs.getInt("daysInDebt"));
        this.setFlatTax(rs.getDouble("flat_tax"));
        this.setTaxRate(rs.getDouble("tax_rate"));
        this.setUpgradesFromString(rs.getString("upgrades"));
        this.setExtraHammers(rs.getDouble("extra_hammers"));
        this.setAccumulatedCulture(rs.getInt("culture"));
        this.defaultGroupName = "residents";
        this.mayorGroupName = "mayors";
        this.assistantGroupName = "assistants";
        this.setTreasury(new EconObject(this));
        this.getTreasury().setBalance(rs.getDouble("coins"), false);
        this.setDebt(rs.getDouble("debt"));
        final String outlawRaw = rs.getString("outlaws");
        if (outlawRaw != null) {
            final String[] outlaws = outlawRaw.split(",");
            String[] array;
            for (int length = (array = outlaws).length, i = 0; i < length; ++i) {
                final String outlaw = array[i];
                this.outlaws.add(outlaw);
            }
        }
        final Long ctime = rs.getLong("created_date");
        if (ctime == null || ctime == 0L) {
            this.setCreated(new Date(0L));
        }
        else {
            this.setCreated(new Date(ctime));
        }
        this.getCiv().addTown(this);
    }
    
    @Override
    public void save() {
        SQLUpdate.add(this);
    }
    
    @Override
    public void saveNow() throws SQLException {
        final HashMap<String, Object> hashmap = new HashMap<String, Object>();
        hashmap.put("name", this.getName());
        hashmap.put("civ_id", this.getCiv().getId());
        if (this.motherCiv != null) {
            hashmap.put("mother_civ_id", this.motherCiv.getId());
        }
        else {
            hashmap.put("mother_civ_id", 0);
        }
        hashmap.put("defaultGroupName", this.getDefaultGroupName());
        hashmap.put("mayorGroupName", this.getMayorGroupName());
        hashmap.put("assistantGroupName", this.getAssistantGroupName());
        hashmap.put("level", this.getLevel());
        hashmap.put("debt", this.getTreasury().getDebt());
        hashmap.put("daysInDebt", this.getDaysInDebt());
        hashmap.put("flat_tax", this.getFlatTax());
        hashmap.put("tax_rate", this.getTaxRate());
        hashmap.put("extra_hammers", this.getExtraHammers());
        hashmap.put("culture", this.getAccumulatedCulture());
        hashmap.put("upgrades", this.getUpgradesString());
        hashmap.put("coins", this.getTreasury().getBalance());
        hashmap.put("dbg_civ_name", this.getCiv().getName());
        if (this.created_date != null) {
            hashmap.put("created_date", this.created_date.getTime());
        }
        else {
            hashmap.put("created_date", null);
        }
        String outlaws = "";
        for (final String outlaw : this.outlaws) {
            outlaws = String.valueOf(outlaws) + outlaw + ",";
        }
        hashmap.put("outlaws", outlaws);
        SQL.updateNamedObject(this, hashmap, "TOWNS");
    }
    
    @Override
    public void delete() throws SQLException {
        for (final PermissionGroup grp : this.groups.values()) {
            grp.delete();
        }
        for (final Resident resident : this.residents.values()) {
            resident.setTown(null);
            resident.getTreasury().setDebt(0.0);
            resident.saveNow();
        }
        if (this.structures != null) {
            for (final Structure struct : this.structures.values()) {
                struct.delete();
            }
        }
        if (this.getTownChunks() != null) {
            for (final TownChunk tc : this.getTownChunks()) {
                tc.delete();
            }
        }
        if (this.wonders != null) {
            for (final Wonder wonder : this.wonders.values()) {
                wonder.unbindStructureBlocks();
                wonder.fancyDestroyStructureBlocks();
                wonder.delete();
            }
        }
        if (this.cultureChunks != null) {
            for (final CultureChunk cc : this.cultureChunks.values()) {
                CivGlobal.removeCultureChunk(cc);
            }
        }
        this.cultureChunks = null;
        CivGlobal.getSessionDB().deleteAllForTown(this);
        SQL.deleteNamedObject(this, "TOWNS");
        CivGlobal.removeTown(this);
    }
    
    public Town(final String name, final Resident mayor, final Civilization civ) throws InvalidNameException {
        this.residents = new ConcurrentHashMap<String, Resident>();
        this.fakeResidents = new ConcurrentHashMap<String, Resident>();
        this.townChunks = new ConcurrentHashMap<ChunkCoord, TownChunk>();
        this.outposts = new ConcurrentHashMap<ChunkCoord, TownChunk>();
        this.cultureChunks = new ConcurrentHashMap<ChunkCoord, CultureChunk>();
        this.wonders = new ConcurrentHashMap<BlockCoord, Wonder>();
        this.structures = new ConcurrentHashMap<BlockCoord, Structure>();
        this.disabledBuildables = new ConcurrentHashMap<BlockCoord, Buildable>();
        this.baseHammers = 1.0;
        this.savedEdgeBlocks = new ArrayList<TownChunk>();
        this.townTouchList = new HashSet<Town>();
        this.groups = new ConcurrentHashMap<String, PermissionGroup>();
        this.upgrades = new ConcurrentHashMap<String, ConfigTownUpgrade>();
        this.bonusGoodies = new ConcurrentHashMap<String, BonusGoodie>();
        this.buffManager = new BuffManager();
        this.pvp = false;
        this.build_tasks = new ArrayList<BuildAsyncTask>();
        this.undo_tasks = new ArrayList<BuildUndoTask>();
        this.lastBuildableBuilt = null;
        this.leaderWantsToDisband = false;
        this.mayorWantsToDisband = false;
        this.outlaws = new HashSet<String>();
        this.claimed = false;
        this.defeated = false;
        this.invalidStructures = new LinkedList<Buildable>();
        this.saved_bank_level = 1;
        this.saved_trommel_level = 1;
        this.saved_tradeship_upgrade_levels = 1;
        this.saved_quarry_level = 1;
        this.saved_fish_hatchery_level = 1;
        this.saved_bank_interest_amount = 0.0;
        this.baseHappy = 0.0;
        this.baseUnhappy = 0.0;
        this.lastBuildableRefresh = null;
        this.attributeCache = new HashMap<String, AttrCache>();
        this.baseGrowth = 0.0;
        this.setName(name);
        this.setLevel(1);
        this.setTaxRate(0.0);
        this.setFlatTax(0.0);
        this.setCiv(civ);
        this.setDaysInDebt(0);
        this.setHammerRate(1.0);
        this.setExtraHammers(0.0);
        this.setAccumulatedCulture(0);
        this.setTreasury(new EconObject(this));
        this.getTreasury().setBalance(0.0, false);
        this.created_date = new Date();
        this.loadSettings();
    }
    
    public Town(final ResultSet rs) throws SQLException, InvalidNameException, CivException {
        this.residents = new ConcurrentHashMap<String, Resident>();
        this.fakeResidents = new ConcurrentHashMap<String, Resident>();
        this.townChunks = new ConcurrentHashMap<ChunkCoord, TownChunk>();
        this.outposts = new ConcurrentHashMap<ChunkCoord, TownChunk>();
        this.cultureChunks = new ConcurrentHashMap<ChunkCoord, CultureChunk>();
        this.wonders = new ConcurrentHashMap<BlockCoord, Wonder>();
        this.structures = new ConcurrentHashMap<BlockCoord, Structure>();
        this.disabledBuildables = new ConcurrentHashMap<BlockCoord, Buildable>();
        this.baseHammers = 1.0;
        this.savedEdgeBlocks = new ArrayList<TownChunk>();
        this.townTouchList = new HashSet<Town>();
        this.groups = new ConcurrentHashMap<String, PermissionGroup>();
        this.upgrades = new ConcurrentHashMap<String, ConfigTownUpgrade>();
        this.bonusGoodies = new ConcurrentHashMap<String, BonusGoodie>();
        this.buffManager = new BuffManager();
        this.pvp = false;
        this.build_tasks = new ArrayList<BuildAsyncTask>();
        this.undo_tasks = new ArrayList<BuildUndoTask>();
        this.lastBuildableBuilt = null;
        this.leaderWantsToDisband = false;
        this.mayorWantsToDisband = false;
        this.outlaws = new HashSet<String>();
        this.claimed = false;
        this.defeated = false;
        this.invalidStructures = new LinkedList<Buildable>();
        this.saved_bank_level = 1;
        this.saved_trommel_level = 1;
        this.saved_tradeship_upgrade_levels = 1;
        this.saved_quarry_level = 1;
        this.saved_fish_hatchery_level = 1;
        this.saved_bank_interest_amount = 0.0;
        this.baseHappy = 0.0;
        this.baseUnhappy = 0.0;
        this.lastBuildableRefresh = null;
        this.attributeCache = new HashMap<String, AttrCache>();
        this.baseGrowth = 0.0;
        this.load(rs);
        this.loadSettings();
    }
    
    public void loadSettings() {
        try {
            this.baseHammers = CivSettings.getDouble(CivSettings.townConfig, "town.base_hammer_rate");
            this.setBaseGrowth(CivSettings.getDouble(CivSettings.townConfig, "town.base_growth_rate"));
        }
        catch (InvalidConfiguration e) {
            e.printStackTrace();
        }
    }
    
    private void setUpgradesFromString(final String upgradeString) {
        final String[] split = upgradeString.split(",");
        String[] array;
        for (int length = (array = split).length, i = 0; i < length; ++i) {
            final String str = array[i];
            if (str != null) {
                if (!str.equals("")) {
                    final ConfigTownUpgrade upgrade = CivSettings.townUpgrades.get(str);
                    if (upgrade == null) {
                        CivLog.warning("Unknown town upgrade:" + str + " in town " + this.getName());
                    }
                    else {
                        this.upgrades.put(str, upgrade);
                    }
                }
            }
        }
    }
    
    private String getUpgradesString() {
        String out = "";
        for (final ConfigTownUpgrade upgrade : this.upgrades.values()) {
            out = String.valueOf(out) + upgrade.id + ",";
        }
        return out;
    }
    
    public ConfigTownUpgrade getUpgrade(final String id) {
        return this.upgrades.get(id);
    }
    
    public boolean isMayor(final Resident res) {
        return this.getMayorGroup().hasMember(res);
    }
    
    public int getResidentCount() {
        return this.residents.size();
    }
    
    public Collection<Resident> getResidents() {
        return this.residents.values();
    }
    
    public boolean hasResident(final String name) {
        return this.residents.containsKey(name.toLowerCase());
    }
    
    public boolean hasResident(final Resident res) {
        return this.hasResident(res.getName());
    }
    
    public void addResident(final Resident res) throws AlreadyRegisteredException {
        final String key = res.getName().toLowerCase();
        if (this.residents.containsKey(key)) {
            throw new AlreadyRegisteredException(String.valueOf(res.getName()) + " already a member of town " + this.getName());
        }
        res.setTown(this);
        this.residents.put(key, res);
        if (this.defaultGroup != null && !this.defaultGroup.hasMember(res)) {
            this.defaultGroup.addMember(res);
            this.defaultGroup.save();
        }
    }
    
    public void addTownChunk(final TownChunk tc) throws AlreadyRegisteredException {
        if (this.townChunks.containsKey(tc.getChunkCoord())) {
            throw new AlreadyRegisteredException("TownChunk at " + tc.getChunkCoord() + " already registered to town " + this.getName());
        }
        this.townChunks.put(tc.getChunkCoord(), tc);
    }
    
    public Structure findStructureByName(final String name) {
        for (final Structure struct : this.structures.values()) {
            if (struct.getDisplayName().equalsIgnoreCase(name)) {
                return struct;
            }
        }
        return null;
    }
    
    public Structure findStructureByLocation(final WorldCord wc) {
        return this.structures.get(wc);
    }
    
    public int getLevel() {
        return this.level;
    }
    
    public void setLevel(final int level) {
        this.level = level;
    }
    
    public double getTaxRate() {
        return this.taxRate;
    }
    
    public void setTaxRate(final double taxRate) {
        this.taxRate = taxRate;
    }
    
    public String getTaxRateString() {
        final long rounded = Math.round(this.taxRate * 100.0);
        return rounded + "%";
    }
    
    public double getFlatTax() {
        return this.flatTax;
    }
    
    public void setFlatTax(final double flatTax) {
        this.flatTax = flatTax;
    }
    
    public Civilization getCiv() {
        return this.civ;
    }
    
    public void setCiv(final Civilization civ) {
        this.civ = civ;
    }
    
    public int getAccumulatedCulture() {
        return this.culture;
    }
    
    public void setAccumulatedCulture(final int culture) {
        this.culture = culture;
    }
    
    public AttrSource getCultureRate() {
        double rate = 1.0;
        final HashMap<String, Double> rates = new HashMap<String, Double>();
        double newRate = this.getGovernment().culture_rate;
        rates.put("Government", newRate - rate);
        rate = newRate;
        final ConfigHappinessState state = CivSettings.getHappinessState(this.getHappinessPercentage());
        newRate = rate * state.culture_rate;
        rates.put("Happiness", newRate - rate);
        rate = newRate;
        double additional = this.getBuffManager().getEffectiveDouble("buff_fine_art");
        if (this.getBuffManager().hasBuff("buff_pyramid_culture")) {
            additional += this.getBuffManager().getEffectiveDouble("buff_pyramid_culture");
        }
        rates.put("Wonders/Goodies", additional);
        rate += additional;
        return new AttrSource(rates, rate, null);
    }
    
    public AttrSource getCulture() {
        AttrCache cache = this.attributeCache.get("CULTURE");
        if (cache == null) {
            cache = new AttrCache();
            cache.lastUpdate = new Date();
        }
        else {
            final Date now = new Date();
            if (now.getTime() <= cache.lastUpdate.getTime() + 5000L) {
                return cache.sources;
            }
            cache.lastUpdate = now;
        }
        double total = 0.0;
        final HashMap<String, Double> sources = new HashMap<String, Double>();
        final double goodieCulture = this.getBuffManager().getEffectiveInt("buff_doesnotexist");
        sources.put("Goodies", goodieCulture);
        total += goodieCulture;
        double fromStructures = 0.0;
        for (final Structure struct : this.structures.values()) {
            for (final Component comp : struct.attachedComponents) {
                if (comp instanceof AttributeBase) {
                    final AttributeBase as = (AttributeBase)comp;
                    if (!as.getString("attribute").equalsIgnoreCase("CULTURE")) {
                        continue;
                    }
                    fromStructures += as.getGenerated();
                }
            }
            if (struct instanceof Temple) {
                final Temple temple = (Temple)struct;
                fromStructures += temple.getCultureGenerated();
            }
        }
        if (this.getBuffManager().hasBuff("buff_globe_theatre_culture_from_towns")) {
            int townCount = 0;
            for (final Civilization civ : CivGlobal.getCivs()) {
                townCount += civ.getTownCount();
            }
            final double culturePerTown = Double.valueOf(CivSettings.buffs.get("buff_globe_theatre_culture_from_towns").value);
            final double bonus = culturePerTown * townCount;
            CivMessage.sendTown(this, "§a" + CivSettings.localize.localizedString("var_town_GlobeTheatreCulture", "§e" + bonus + "§a", townCount));
            fromStructures += bonus;
        }
        total += fromStructures;
        sources.put("Structures", fromStructures);
        final AttrSource rate = this.getCultureRate();
        total *= rate.total;
        if (total < 0.0) {
            total = 0.0;
        }
        final AttrSource as2 = new AttrSource(sources, total, rate);
        cache.sources = as2;
        this.attributeCache.put("CULTURE", cache);
        return as2;
    }
    
    public void addAccumulatedCulture(final double generated) {
        final ConfigCultureLevel clc = CivSettings.cultureLevels.get(this.getCultureLevel());
        this.culture += (int)generated;
        this.save();
        if (this.getCultureLevel() != CivSettings.getMaxCultureLevel() && this.culture >= clc.amount) {
            CivGlobal.processCulture();
            CivMessage.sendCiv(this.civ, CivSettings.localize.localizedString("var_town_bordersExpanded", this.getName()));
        }
    }
    
    public double getExtraHammers() {
        return this.extraHammers;
    }
    
    public void setExtraHammers(final double extraHammers) {
        this.extraHammers = extraHammers;
    }
    
    public AttrSource getHammerRate() {
        double rate = 1.0;
        final HashMap<String, Double> rates = new HashMap<String, Double>();
        final ConfigHappinessState state = CivSettings.getHappinessState(this.getHappinessPercentage());
        double newRate = rate * state.hammer_rate;
        rates.put("Happiness", newRate - rate);
        rate = newRate;
        newRate = rate * this.getGovernment().hammer_rate;
        rates.put("Government", newRate - rate);
        rate = newRate;
        final double randomRate = RandomEvent.getHammerRate(this);
        newRate = rate * randomRate;
        rates.put("Random Events", newRate - rate);
        rate = newRate;
        if (this.motherCiv != null) {
            try {
                newRate = rate * CivSettings.getDouble(CivSettings.warConfig, "war.captured_penalty");
                rates.put("Captured Penalty", newRate - rate);
                rate = newRate;
            }
            catch (InvalidConfiguration e) {
                e.printStackTrace();
            }
        }
        return new AttrSource(rates, rate, null);
    }
    
    public AttrSource getHammers() {
        double total = 0.0;
        AttrCache cache = this.attributeCache.get("HAMMERS");
        if (cache == null) {
            cache = new AttrCache();
            cache.lastUpdate = new Date();
        }
        else {
            final Date now = new Date();
            if (now.getTime() <= cache.lastUpdate.getTime() + 5000L) {
                return cache.sources;
            }
            cache.lastUpdate = now;
        }
        final HashMap<String, Double> sources = new HashMap<String, Double>();
        final double wonderGoodies = this.getBuffManager().getEffectiveInt("buff_construction");
        sources.put("Wonders/Goodies", wonderGoodies);
        total += wonderGoodies;
        final double wonderGoodies2 = this.getBuffManager().getEffectiveInt("buff_grand_canyon_construction");
        sources.put("Wonders/Goodies", wonderGoodies2);
        total += wonderGoodies2;
        final double cultureHammers = this.getHammersFromCulture();
        sources.put("Culture Biomes", cultureHammers);
        total += cultureHammers;
        double structures = 0.0;
        for (final Structure struct : this.structures.values()) {
            for (final Component comp : struct.attachedComponents) {
                if (comp instanceof AttributeBase) {
                    final AttributeBase as = (AttributeBase)comp;
                    if (!as.getString("attribute").equalsIgnoreCase("HAMMERS")) {
                        continue;
                    }
                    structures += as.getGenerated();
                }
            }
        }
        total += structures;
        sources.put("Structures", structures);
        sources.put("Base Hammers", this.baseHammers);
        total += this.baseHammers;
        final AttrSource rate = this.getHammerRate();
        total *= rate.total;
        if (total < this.baseHammers) {
            total = this.baseHammers;
        }
        final AttrSource as2 = new AttrSource(sources, total, rate);
        cache.sources = as2;
        this.attributeCache.put("HAMMERS", cache);
        return as2;
    }
    
    public void setHammerRate(final double hammerRate) {
        this.baseHammers = hammerRate;
    }
    
    public static Town newTown(final Resident resident, final String name, final Civilization civ, final boolean free, final boolean capitol, final Location loc) throws CivException {
        try {
            if (War.isWarTime() && !free && civ.getDiplomacyManager().isAtWar()) {
                throw new CivException(CivSettings.localize.localizedString("town_found_errorAtWar"));
            }
            if (civ == null) {
                throw new CivException(CivSettings.localize.localizedString("town_found_errorNotInCiv"));
            }
            if (resident.getTown() != null && resident.getTown().isMayor(resident)) {
                throw new CivException(CivSettings.localize.localizedString("var_town_found_errorIsMayor", resident.getTown().getName()));
            }
            if (resident.hasCamp()) {
                throw new CivException(CivSettings.localize.localizedString("town_found_errorInCamp"));
            }
            final Town existTown = CivGlobal.getTown(name);
            if (existTown != null) {
                throw new CivException(CivSettings.localize.localizedString("var_town_found_errorNameExists", name));
            }
            Town newTown;
            try {
                newTown = new Town(name, resident, civ);
            }
            catch (InvalidNameException e7) {
                throw new CivException(CivSettings.localize.localizedString("var_town_found_errorInvalidName", name));
            }
            final Player player = CivGlobal.getPlayer(resident.getName());
            if (player == null) {
                throw new CivException(CivSettings.localize.localizedString("town_found_errorInvalidResident"));
            }
            if (CivGlobal.getTownChunk(loc) != null) {
                throw new CivException(CivSettings.localize.localizedString("town_found_errorInTownChunk"));
            }
            final CultureChunk cultrueChunk = CivGlobal.getCultureChunk(loc);
            if (cultrueChunk != null && cultrueChunk.getCiv() != resident.getCiv()) {
                throw new CivException(CivSettings.localize.localizedString("town_found_errorCivCulture"));
            }
            double minDistanceFriend;
            double minDistanceEnemy;
            try {
                minDistanceFriend = CivSettings.getDouble(CivSettings.townConfig, "town.min_town_distance");
                minDistanceEnemy = CivSettings.getDouble(CivSettings.townConfig, "town.min_town_distance_enemy");
            }
            catch (InvalidConfiguration e) {
                e.printStackTrace();
                throw new CivException(CivSettings.localize.localizedString("internalException"));
            }
            for (final Town town : CivGlobal.getTowns()) {
                final TownHall townhall = town.getTownHall();
                if (townhall == null) {
                    continue;
                }
                final double dist = townhall.getCenterLocation().distance(new BlockCoord(player.getLocation()));
                double minDistance = minDistanceFriend;
                if (townhall.getCiv().getDiplomacyManager().atWarWith(civ)) {
                    minDistance = minDistanceEnemy;
                }
                if (dist < minDistance) {
                    final DecimalFormat df = new DecimalFormat();
                    throw new CivException(CivSettings.localize.localizedString("var_town_found_errorTooClose", town.getName(), df.format(dist), minDistance));
                }
            }
            try {
                final int min_distance = CivSettings.getInteger(CivSettings.civConfig, "civ.min_distance");
                final ChunkCoord foundLocation = new ChunkCoord(loc);
                for (final TownChunk cc : CivGlobal.getTownChunks()) {
                    if (cc.getTown().getCiv() == newTown.getCiv()) {
                        continue;
                    }
                    final double dist2 = foundLocation.distance(cc.getChunkCoord());
                    if (dist2 <= min_distance) {
                        final DecimalFormat df2 = new DecimalFormat();
                        throw new CivException(CivSettings.localize.localizedString("var_town_found_errorTooClose", cc.getTown().getName(), df2.format(dist2), min_distance));
                    }
                }
            }
            catch (InvalidConfiguration e2) {
                e2.printStackTrace();
                throw new CivException(CivSettings.localize.localizedString("internalException"));
            }
            if (!free) {
                final ConfigUnit unit = Unit.getPlayerUnit(player);
                if (unit == null || !unit.id.equals("u_settler")) {
                    throw new CivException(CivSettings.localize.localizedString("town_found_errorNotSettler"));
                }
            }
            newTown.saveNow();
            CivGlobal.addTown(newTown);
            PermissionGroup residentsGroup;
            try {
                residentsGroup = new PermissionGroup(newTown, "residents");
                residentsGroup.addMember(resident);
                residentsGroup.saveNow();
                newTown.setDefaultGroup(residentsGroup);
                final PermissionGroup mayorGroup = new PermissionGroup(newTown, "mayors");
                mayorGroup.addMember(resident);
                mayorGroup.saveNow();
                newTown.setMayorGroup(mayorGroup);
                final PermissionGroup assistantGroup = new PermissionGroup(newTown, "assistants");
                assistantGroup.saveNow();
                newTown.setAssistantGroup(assistantGroup);
            }
            catch (InvalidNameException e3) {
                e3.printStackTrace();
                throw new CivException(CivSettings.localize.localizedString("internalCommandException"));
            }
            final ChunkCoord cl = new ChunkCoord(loc);
            final TownChunk tc = new TownChunk(newTown, cl);
            tc.perms.addGroup(residentsGroup);
            try {
                newTown.addTownChunk(tc);
            }
            catch (AlreadyRegisteredException e8) {
                throw new CivException(CivSettings.localize.localizedString("town_found_errorTownHasChunk"));
            }
            tc.save();
            CivGlobal.addTownChunk(tc);
            civ.addTown(newTown);
            try {
                if (capitol) {
                    final ConfigBuildableInfo buildableInfo = CivSettings.structures.get("s_capitol");
                    newTown.getTreasury().deposit(buildableInfo.cost);
                    newTown.buildStructure(player, buildableInfo.id, loc, resident.desiredTemplate);
                }
                else {
                    final ConfigBuildableInfo buildableInfo = CivSettings.structures.get("s_townhall");
                    newTown.getTreasury().deposit(buildableInfo.cost);
                    newTown.buildStructure(player, buildableInfo.id, loc, resident.desiredTemplate);
                }
            }
            catch (CivException e4) {
                civ.removeTown(newTown);
                newTown.delete();
                throw e4;
            }
            if (!free) {
                final ItemStack newStack = new ItemStack(Material.AIR);
                player.setItemInHand(newStack);
                Unit.removeUnit(player);
            }
            try {
                if (resident.getTown() != null) {
                    CivMessage.sendTown(resident.getTown(), CivSettings.localize.localizedString("var_town_found_leftTown", resident.getName()));
                    resident.getTown().removeResident(resident);
                }
                newTown.addResident(resident);
            }
            catch (AlreadyRegisteredException e5) {
                e5.printStackTrace();
                throw new CivException(CivSettings.localize.localizedString("town_found_residentError"));
            }
            resident.saveNow();
            CivGlobal.processCulture();
            newTown.saveNow();
            return newTown;
        }
        catch (SQLException e6) {
            e6.printStackTrace();
            throw new CivException(CivSettings.localize.localizedString("internalDatabaseException"));
        }
    }
    
    public PermissionGroup getDefaultGroup() {
        return this.defaultGroup;
    }
    
    public void setDefaultGroup(final PermissionGroup defaultGroup) {
        this.defaultGroup = defaultGroup;
        this.groups.put(defaultGroup.getName(), defaultGroup);
    }
    
    public Collection<PermissionGroup> getGroups() {
        return this.groups.values();
    }
    
    public PermissionGroup getGroup(final String name) {
        return this.groups.get(name);
    }
    
    public PermissionGroup getGroupFromId(final Integer id) {
        for (final PermissionGroup grp : this.groups.values()) {
            if (grp.getId() == id) {
                return grp;
            }
        }
        return null;
    }
    
    public void addGroup(final PermissionGroup grp) {
        if (grp.getName().equalsIgnoreCase(this.defaultGroupName)) {
            this.defaultGroup = grp;
        }
        else if (grp.getName().equalsIgnoreCase(this.mayorGroupName)) {
            this.mayorGroup = grp;
        }
        else if (grp.getName().equalsIgnoreCase(this.assistantGroupName)) {
            this.assistantGroup = grp;
        }
        this.groups.put(grp.getName(), grp);
    }
    
    public void removeGroup(final PermissionGroup grp) {
        this.groups.remove(grp.getName());
    }
    
    public boolean hasGroupNamed(final String name) {
        for (final PermissionGroup grp : this.groups.values()) {
            if (grp.getName().equalsIgnoreCase(name)) {
                return true;
            }
        }
        return false;
    }
    
    public PermissionGroup getGroupByName(final String name) {
        for (final PermissionGroup grp : this.groups.values()) {
            if (grp.getName().equalsIgnoreCase(name)) {
                return grp;
            }
        }
        return null;
    }
    
    public String getDefaultGroupName() {
        return "residents";
    }
    
    public PermissionGroup getMayorGroup() {
        return this.mayorGroup;
    }
    
    public void setMayorGroup(final PermissionGroup mayorGroup) {
        this.mayorGroup = mayorGroup;
        this.groups.put(mayorGroup.getName(), mayorGroup);
    }
    
    public PermissionGroup getAssistantGroup() {
        return this.assistantGroup;
    }
    
    public void setAssistantGroup(final PermissionGroup assistantGroup) {
        this.assistantGroup = assistantGroup;
        this.groups.put(assistantGroup.getName(), assistantGroup);
    }
    
    public String getMayorGroupName() {
        return "mayors";
    }
    
    public String getAssistantGroupName() {
        return "assistants";
    }
    
    public boolean isProtectedGroup(final PermissionGroup grp) {
        return grp.isProtectedGroup();
    }
    
    public boolean playerIsInGroupName(final String groupName, final Player player) {
        final PermissionGroup grp = this.getGroupByName(groupName);
        if (grp == null) {
            return false;
        }
        final Resident resident = CivGlobal.getResident(player);
        return grp.hasMember(resident);
    }
    
    public EconObject getTreasury() {
        return this.treasury;
    }
    
    public void depositDirect(final double amount) {
        this.treasury.deposit(amount);
    }
    
    public void depositTaxed(double amount) {
        final double taxAmount = amount * this.getDepositCiv().getIncomeTaxRate();
        amount -= taxAmount;
        if (this.getMotherCiv() != null) {
            double capturedPenalty;
            try {
                capturedPenalty = CivSettings.getDouble(CivSettings.warConfig, "war.captured_penalty");
            }
            catch (InvalidConfiguration e) {
                e.printStackTrace();
                return;
            }
            final double capturePayment = amount * capturedPenalty;
            CivMessage.sendTown(this, "§e" + CivSettings.localize.localizedString("var_town_capturePenalty1", amount - capturePayment, CivSettings.CURRENCY_NAME, this.getCiv().getName()));
            amount = capturePayment;
        }
        this.treasury.deposit(amount);
        this.getDepositCiv().taxPayment(this, taxAmount);
    }
    
    public void withdraw(final double amount) {
        this.treasury.withdraw(amount);
    }
    
    public boolean inDebt() {
        return this.treasury.inDebt();
    }
    
    public double getDebt() {
        return this.treasury.getDebt();
    }
    
    public void setDebt(final double amount) {
        this.treasury.setDebt(amount);
    }
    
    public double getBalance() {
        return this.treasury.getBalance();
    }
    
    public boolean hasEnough(final double amount) {
        return this.treasury.hasEnough(amount);
    }
    
    public void setTreasury(final EconObject treasury) {
        this.treasury = treasury;
    }
    
    public String getLevelTitle() {
        final ConfigTownLevel clevel = CivSettings.townLevels.get(this.level);
        if (clevel == null) {
            return "Unknown";
        }
        return clevel.title;
    }
    
    public void purchaseUpgrade(final ConfigTownUpgrade upgrade) throws CivException {
        if (!this.hasUpgrade(upgrade.require_upgrade)) {
            throw new CivException(CivSettings.localize.localizedString("town_missingUpgrades"));
        }
        if (!this.getTreasury().hasEnough(upgrade.cost)) {
            throw new CivException(CivSettings.localize.localizedString("var_town_missingFunds", upgrade.cost, CivSettings.CURRENCY_NAME));
        }
        if (!this.hasStructure(upgrade.require_structure)) {
            throw new CivException(CivSettings.localize.localizedString("town_missingStructures"));
        }
        this.getTreasury().withdraw(upgrade.cost);
        try {
            upgrade.processAction(this);
        }
        catch (CivException e) {
            this.getTreasury().deposit(upgrade.cost);
            throw e;
        }
        this.upgrades.put(upgrade.id, upgrade);
        this.save();
    }
    
    public Structure findStructureByConfigId(final String require_structure) {
        for (final Structure struct : this.structures.values()) {
            if (struct.getConfigId().equals(require_structure)) {
                return struct;
            }
        }
        return null;
    }
    
    public ConcurrentHashMap<String, ConfigTownUpgrade> getUpgrades() {
        return this.upgrades;
    }
    
    public boolean isPvp() {
        return this.pvp;
    }
    
    public void setPvp(final boolean pvp) {
        this.pvp = pvp;
    }
    
    public String getPvpString() {
        if (this.getCiv().getDiplomacyManager().isAtWar()) {
            return "§4[WAR-PvP]";
        }
        if (this.pvp) {
            return "§4[PvP]";
        }
        return "§2[No PvP]";
    }
    
    private void kickResident(final Resident resident) {
        for (final TownChunk tc : this.townChunks.values()) {
            if (tc.perms.getOwner() == resident) {
                tc.perms.setOwner(null);
                tc.perms.replaceGroups(this.defaultGroup);
                tc.perms.resetPerms();
                tc.save();
            }
        }
        resident.getTreasury().setDebt(0.0);
        resident.setDaysTilEvict(0);
        resident.setTown(null);
        resident.setRejoinCooldown(this);
        this.residents.remove(resident.getName().toLowerCase());
        resident.save();
        this.save();
    }
    
    public double collectPlotTax() {
        double total = 0.0;
        for (final Resident resident : this.residents.values()) {
            if (!resident.hasTown()) {
                CivLog.warning("Resident in town list but doesnt have a town! Resident:" + resident.getName() + " town:" + this.getName());
            }
            else {
                if (resident.isTaxExempt()) {
                    continue;
                }
                final double tax = resident.getPropertyTaxOwed();
                final boolean wasInDebt = resident.getTreasury().inDebt();
                total += resident.getTreasury().payToCreditor(this.getTreasury(), tax);
                if (!resident.getTreasury().inDebt() || wasInDebt) {
                    continue;
                }
                resident.onEnterDebt();
            }
        }
        return total;
    }
    
    public double collectFlatTax() {
        double total = 0.0;
        for (final Resident resident : this.residents.values()) {
            if (!resident.hasTown()) {
                CivLog.warning("Resident in town list but doesnt have a town! Resident:" + resident.getName() + " town:" + this.getName());
            }
            else {
                if (resident.isTaxExempt()) {
                    continue;
                }
                final boolean wasInDebt = resident.getTreasury().inDebt();
                total += resident.getTreasury().payToCreditor(this.getTreasury(), this.getFlatTax());
                if (!resident.getTreasury().inDebt() || wasInDebt) {
                    continue;
                }
                resident.onEnterDebt();
            }
        }
        return total;
    }
    
    public Collection<TownChunk> getTownChunks() {
        return this.townChunks.values();
    }
    
    public void quicksave() throws CivException {
        this.save();
    }
    
    public boolean isInGroup(final String name, final Resident resident) {
        final PermissionGroup grp = this.getGroupByName(name);
        return grp != null && grp.hasMember(resident);
    }
    
    public TownHall getTownHall() {
        for (final Structure struct : this.structures.values()) {
            if (struct instanceof TownHall) {
                return (TownHall)struct;
            }
        }
        return null;
    }
    
    public double payUpkeep() throws InvalidConfiguration {
        double upkeep = 0.0;
        if (this.getCiv().isAdminCiv()) {
            return 0.0;
        }
        upkeep += this.getBaseUpkeep();
        upkeep += this.getStructureUpkeep();
        upkeep += this.getOutpostUpkeep();
        upkeep *= this.getGovernment().upkeep_rate;
        if (this.getBuffManager().hasBuff("buff_colossus_reduce_upkeep")) {
            upkeep -= upkeep * this.getBuffManager().getEffectiveDouble("buff_colossus_reduce_upkeep");
        }
        if (this.getBuffManager().hasBuff("debuff_colossus_leech_upkeep")) {
            final double rate = this.getBuffManager().getEffectiveDouble("debuff_colossus_leech_upkeep");
            final double amount = upkeep * rate;
            final Wonder colossus = CivGlobal.getWonderByConfigId("w_colossus");
            if (colossus != null) {
                colossus.getTown().getTreasury().deposit(amount);
            }
            else {
                CivLog.warning("Unable to find Colossus wonder but debuff for leech upkeep was present!");
                this.getBuffManager().removeBuff("debuff_colossus_leech_upkeep");
            }
        }
        if (this.getTreasury().hasEnough(upkeep)) {
            this.getTreasury().withdraw(upkeep);
        }
        else {
            double diff = upkeep - this.getTreasury().getBalance();
            if (this.isCapitol()) {
                if (this.getCiv().getTreasury().hasEnough(diff)) {
                    this.getCiv().getTreasury().withdraw(diff);
                }
                else {
                    diff -= this.getCiv().getTreasury().getBalance();
                    this.getCiv().getTreasury().setBalance(0.0);
                    this.getCiv().getTreasury().setDebt(this.getCiv().getTreasury().getDebt() + diff);
                    this.getCiv().save();
                }
            }
            else {
                this.getTreasury().setDebt(this.getTreasury().getDebt() + diff);
            }
            this.getTreasury().withdraw(this.getTreasury().getBalance());
        }
        return upkeep;
    }
    
    public double getBaseUpkeep() {
        final ConfigTownLevel level = CivSettings.townLevels.get(this.level);
        return level.upkeep;
    }
    
    public double getStructureUpkeep() {
        double upkeep = 0.0;
        for (final Structure struct : this.getStructures()) {
            upkeep += struct.getUpkeepCost();
        }
        return upkeep;
    }
    
    public void removeResident(final Resident resident) {
        this.residents.remove(resident.getName().toLowerCase());
        for (final PermissionGroup group : this.groups.values()) {
            if (group.hasMember(resident)) {
                group.removeMember(resident);
                group.save();
            }
        }
        this.kickResident(resident);
    }
    
    public boolean canClaim() {
        return this.getMaxPlots() > this.townChunks.size();
    }
    
    public int getMaxPlots() {
        final ConfigTownLevel lvl = CivSettings.townLevels.get(this.level);
        return lvl.plots;
    }
    
    public boolean hasUpgrade(final String require_upgrade) {
        return require_upgrade == null || require_upgrade.equals("") || this.upgrades.containsKey(require_upgrade);
    }
    
    public boolean hasTechnology(final String require_tech) {
        return this.getCiv().hasTechnology(require_tech);
    }
    
    public String getDynmapDescription() {
        String out = "";
        try {
            out = String.valueOf(out) + "<h3><b>" + this.getName() + "</b> (<i>" + this.getCiv().getName() + "</i>)</h3>";
            out = String.valueOf(out) + "<b>" + CivSettings.localize.localizedString("Mayors") + " " + this.getMayorGroup().getMembersString() + "</b>";
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return out;
    }
    
    public void removeCultureChunk(final ChunkCoord coord) {
        this.cultureChunks.remove(coord);
    }
    
    public void removeCultureChunk(final CultureChunk cc) {
        this.cultureChunks.remove(cc.getChunkCoord());
    }
    
    public void addCultureChunk(final CultureChunk cc) {
        this.cultureChunks.put(cc.getChunkCoord(), cc);
    }
    
    public int getCultureLevel() {
        int bestLevel = 0;
        ConfigCultureLevel level = CivSettings.cultureLevels.get(0);
        while (this.culture >= level.amount) {
            level = CivSettings.cultureLevels.get(bestLevel + 1);
            if (level == null) {
                level = CivSettings.cultureLevels.get(bestLevel);
                break;
            }
            ++bestLevel;
        }
        return level.level;
    }
    
    public Collection<CultureChunk> getCultureChunks() {
        return this.cultureChunks.values();
    }
    
    public Object getCultureChunk(final ChunkCoord coord) {
        return this.cultureChunks.get(coord);
    }
    
    public void removeWonder(final Buildable buildable) {
        if (!buildable.isComplete()) {
            this.removeBuildTask(buildable);
        }
        if (this.currentWonderInProgress == buildable) {
            this.currentWonderInProgress = null;
        }
        this.wonders.remove(buildable.getCorner());
    }
    
    public void addWonder(final Buildable buildable) {
        if (buildable instanceof Wonder) {
            this.wonders.put(buildable.getCorner(), (Wonder)buildable);
        }
    }
    
    public int getStructureTypeCount(final String id) {
        int count = 0;
        for (final Structure struct : this.structures.values()) {
            if (struct.getConfigId().equalsIgnoreCase(id)) {
                ++count;
            }
        }
        return count;
    }
    
    public void giveExtraHammers(final double extra) {
        if (this.build_tasks.size() == 0) {
            this.extraHammers = extra;
        }
        else {
            final double hammers_per_task = extra / this.build_tasks.size();
            double leftovers = 0.0;
            for (final BuildAsyncTask task : this.build_tasks) {
                leftovers += task.setExtraHammers(hammers_per_task);
            }
            this.extraHammers = leftovers;
        }
        this.save();
    }
    
    public void buildWonder(final Player player, final String id, final Location center, final Template tpl) throws CivException {
        if (!center.getWorld().getName().equals("world")) {
            throw new CivException(CivSettings.localize.localizedString("town_buildwonder_NotOverworld"));
        }
        final Wonder wonder = Wonder.newWonder(center, id, this);
        if (!this.hasUpgrade(wonder.getRequiredUpgrade())) {
            throw new CivException(CivSettings.localize.localizedString("town_buildwonder_errorMissingUpgrade"));
        }
        if (!this.hasTechnology(wonder.getRequiredTechnology())) {
            throw new CivException(CivSettings.localize.localizedString("town_buildwonder_errorMissingTech"));
        }
        if (!wonder.isAvailable()) {
            throw new CivException(CivSettings.localize.localizedString("town_buildwonder_errorNotAvailable"));
        }
        wonder.canBuildHere(center, 7.0);
        if (!Wonder.isWonderAvailable(id)) {
            throw new CivException(CivSettings.localize.localizedString("town_buildwonder_errorBuiltElsewhere"));
        }
        if (CivGlobal.isCasualMode()) {
            for (final Town town : this.getCiv().getTowns()) {
                for (final Wonder w : town.getWonders()) {
                    if (w.getConfigId().equals(id)) {
                        throw new CivException(CivSettings.localize.localizedString("town_buildwonder_errorLimit1Casual"));
                    }
                }
            }
        }
        final double cost = wonder.getCost();
        if (!this.getTreasury().hasEnough(cost)) {
            throw new CivException(CivSettings.localize.localizedString("var_town_buildwonder_errorTooPoor", wonder.getDisplayName(), cost, CivSettings.CURRENCY_NAME));
        }
        wonder.runCheck(center);
        Buildable inProgress = this.getCurrentStructureInProgress();
        if (inProgress != null) {
            throw new CivException(String.valueOf(CivSettings.localize.localizedString("var_town_buildwonder_errorCurrentlyBuilding", inProgress.getDisplayName())) + " " + CivSettings.localize.localizedString("town_buildwonder_errorOneAtATime"));
        }
        inProgress = this.getCurrentWonderInProgress();
        if (inProgress != null) {
            throw new CivException(String.valueOf(CivSettings.localize.localizedString("var_town_buildwonder_errorCurrentlyBuilding", inProgress.getDisplayName())) + " " + CivSettings.localize.localizedString("town_buildwonder_errorOneWonderAtaTime"));
        }
        try {
            wonder.build(player, center, tpl);
            if (this.getExtraHammers() > 0.0) {
                this.giveExtraHammers(this.getExtraHammers());
            }
        }
        catch (Exception e) {
            if (CivGlobal.testFileFlag("debug")) {
                e.printStackTrace();
            }
            throw new CivException(CivSettings.localize.localizedString("var_town_buildwonder_errorGeneric", e.getMessage()));
        }
        this.wonders.put(wonder.getCorner(), wonder);
        this.getTreasury().withdraw(cost);
        CivMessage.sendTown(this, "§e" + CivSettings.localize.localizedString("var_town_buildwonder_success", wonder.getDisplayName()));
        this.save();
    }
    
    public void buildStructure(final Player player, final String id, final Location center, Template tpl) throws CivException {
        final Structure struct = Structure.newStructure(center, id, this);
        if (!this.hasUpgrade(struct.getRequiredUpgrade())) {
            throw new CivException(CivSettings.localize.localizedString("town_buildwonder_errorMissingUpgrade"));
        }
        if (!this.hasTechnology(struct.getRequiredTechnology())) {
            throw new CivException(CivSettings.localize.localizedString("town_buildwonder_errorMissingTech"));
        }
        if (!struct.isAvailable()) {
            throw new CivException(CivSettings.localize.localizedString("town_structure_errorNotAvaliable"));
        }
        struct.canBuildHere(center, 7.0);
        if (struct.getLimit() != 0 && this.getStructureTypeCount(id) >= struct.getLimit()) {
            throw new CivException(CivSettings.localize.localizedString("var_town_structure_errorLimitMet", struct.getLimit(), struct.getDisplayName()));
        }
        final double cost = struct.getCost();
        if (!this.getTreasury().hasEnough(cost)) {
            throw new CivException(CivSettings.localize.localizedString("var_town_buildwonder_errorTooPoor", struct.getDisplayName(), cost, CivSettings.CURRENCY_NAME));
        }
        struct.runCheck(center);
        final Buildable inProgress = this.getCurrentStructureInProgress();
        if (inProgress != null) {
            throw new CivException(String.valueOf(CivSettings.localize.localizedString("var_town_buildwonder_errorCurrentlyBuilding", inProgress.getDisplayName())) + ". " + CivSettings.localize.localizedString("town_buildwonder_errorOneAtATime"));
        }
        try {
            if (tpl == null) {
                try {
                    tpl = new Template();
                    tpl.initTemplate(center, struct);
                }
                catch (Exception e) {
                    throw e;
                }
            }
            struct.build(player, center, tpl);
            struct.save();
            for (final TownChunk tc : struct.townChunksToSave) {
                tc.save();
            }
            struct.townChunksToSave.clear();
            if (this.getExtraHammers() > 0.0) {
                this.giveExtraHammers(this.getExtraHammers());
            }
        }
        catch (CivException e2) {
            throw new CivException(CivSettings.localize.localizedString("var_town_buildwonder_errorGeneric", e2.getMessage()));
        }
        catch (Exception e) {
            e.printStackTrace();
            throw new CivException(CivSettings.localize.localizedString("internalCommandException"));
        }
        this.getTreasury().withdraw(cost);
        CivMessage.sendTown(this, "§e" + CivSettings.localize.localizedString("var_town_buildwonder_success", struct.getDisplayName()));
        if (struct instanceof TradeOutpost) {
            final TradeOutpost outpost = (TradeOutpost)struct;
            if (outpost.getGood() != null) {
                outpost.getGood().save();
            }
        }
    }
    
    public boolean isStructureAddable(final Structure struct) {
        final int count = this.getStructureTypeCount(struct.getConfigId());
        if (struct.isTileImprovement()) {
            final ConfigTownLevel level = CivSettings.townLevels.get(this.getLevel());
            Integer maxTileImprovements = level.tile_improvements;
            if (this.getBuffManager().hasBuff("buff_mother_tree_tile_improvement_bonus")) {
                maxTileImprovements += maxTileImprovements;
            }
            if (this.getTileImprovementCount() > maxTileImprovements) {
                return false;
            }
        }
        else if (struct.getLimit() != 0 && count > struct.getLimit()) {
            return false;
        }
        return true;
    }
    
    public void addStructure(final Structure struct) {
        this.structures.put(struct.getCorner(), struct);
        if (!this.isStructureAddable(struct)) {
            this.disabledBuildables.put(struct.getCorner(), struct);
            struct.setEnabled(false);
        }
        else {
            this.disabledBuildables.remove(struct.getCorner());
            struct.setEnabled(true);
        }
    }
    
    public Structure getStructureByType(final String id) {
        for (final Structure struct : this.structures.values()) {
            if (struct.getConfigId().equalsIgnoreCase(id)) {
                return struct;
            }
        }
        return null;
    }
    
    public void loadUpgrades() throws CivException {
        for (final ConfigTownUpgrade upgrade : this.upgrades.values()) {
            try {
                upgrade.processAction(this);
            }
            catch (CivException e) {
                CivLog.warning("Loading upgrade generated exception:" + e.getMessage());
            }
        }
    }
    
    public Collection<Structure> getStructures() {
        return this.structures.values();
    }
    
    public void processUndo() throws CivException {
        if (this.lastBuildableBuilt == null) {
            throw new CivException(CivSettings.localize.localizedString("town_undo_cannotFind"));
        }
        if (!(this.lastBuildableBuilt instanceof Wall) && !(this.lastBuildableBuilt instanceof Road)) {
            throw new CivException(CivSettings.localize.localizedString("town_undo_notRoadOrWall"));
        }
        this.lastBuildableBuilt.processUndo();
        this.structures.remove(this.lastBuildableBuilt.getCorner());
        this.removeBuildTask(this.lastBuildableBuilt);
        this.lastBuildableBuilt = null;
    }
    
    private void removeBuildTask(final Buildable lastBuildableBuilt) {
        for (final BuildAsyncTask task : this.build_tasks) {
            if (task.buildable == lastBuildableBuilt) {
                this.build_tasks.remove(task);
                task.abort();
            }
        }
    }
    
    public Structure getStructure(final BlockCoord coord) {
        return this.structures.get(coord);
    }
    
    public void demolish(final Structure struct, final boolean isAdmin) throws CivException {
        if (!struct.allowDemolish() && !isAdmin) {
            throw new CivException(CivSettings.localize.localizedString("town_demolish_Cannot"));
        }
        try {
            struct.onDemolish();
            struct.unbindStructureBlocks();
            this.removeStructure(struct);
            struct.delete();
        }
        catch (SQLException e) {
            e.printStackTrace();
            throw new CivException(CivSettings.localize.localizedString("internalDatabaseException"));
        }
    }
    
    public boolean hasStructure(final String require_structure) {
        if (require_structure == null || require_structure.equals("")) {
            return true;
        }
        final Structure struct = this.findStructureByConfigId(require_structure);
        return struct != null && struct.isActive();
    }
    
    public AttrSource getGrowthRate() {
        double rate = 1.0;
        final HashMap<String, Double> rates = new HashMap<String, Double>();
        final double newRate = rate * this.getGovernment().growth_rate;
        rates.put("Government", newRate - rate);
        rate = newRate;
        double additional = this.getBuffManager().getEffectiveDouble("buff_year_of_plenty");
        additional += this.getBuffManager().getEffectiveDouble("buff_hanging_gardens_growth");
        additional += this.getBuffManager().getEffectiveDouble("buff_mother_tree_growth");
        additional = this.getBuffManager().getEffectiveDouble("buff_salingship_growth_rate");
        final double additionalGrapes = this.getBuffManager().getEffectiveDouble("buff_hanging_gardens_additional_growth");
        int grapeCount = 0;
        for (final BonusGoodie goodie : this.getBonusGoodies()) {
            if (goodie.getDisplayName().equalsIgnoreCase("grapes")) {
                ++grapeCount;
            }
        }
        additional += additionalGrapes * grapeCount;
        rates.put("Wonders/Goodies", additional);
        rate += additional;
        return new AttrSource(rates, rate, null);
    }
    
    public AttrSource getGrowth() {
        AttrCache cache = this.attributeCache.get("GROWTH");
        if (cache == null) {
            cache = new AttrCache();
            cache.lastUpdate = new Date();
        }
        else {
            final Date now = new Date();
            if (now.getTime() <= cache.lastUpdate.getTime() + 5000L) {
                return cache.sources;
            }
            cache.lastUpdate = now;
        }
        double total = 0.0;
        final HashMap<String, Double> sources = new HashMap<String, Double>();
        double cultureSource = 0.0;
        for (final CultureChunk cc : this.cultureChunks.values()) {
            cultureSource += cc.getGrowth();
        }
        sources.put("Culture Biomes", cultureSource);
        total += cultureSource;
        double structures = 0.0;
        for (final Structure struct : this.structures.values()) {
            for (final Component comp : struct.attachedComponents) {
                if (comp instanceof AttributeBase) {
                    final AttributeBase as = (AttributeBase)comp;
                    if (!as.getString("attribute").equalsIgnoreCase("GROWTH")) {
                        continue;
                    }
                    final double h = as.getGenerated();
                    structures += h;
                }
            }
        }
        total += structures;
        sources.put("Structures", structures);
        sources.put("Base Growth", this.baseGrowth);
        total += this.baseGrowth;
        final AttrSource rate = this.getGrowthRate();
        total *= rate.total;
        if (total < 0.0) {
            total = 0.0;
        }
        final AttrSource as2 = new AttrSource(sources, total, rate);
        cache.sources = as2;
        this.attributeCache.put("GROWTH", cache);
        return as2;
    }
    
    public double getCottageRate() {
        double rate = this.getGovernment().cottage_rate;
        final double additional = rate * this.getBuffManager().getEffectiveDouble("buff_doesnotexist");
        rate += additional;
        rate *= this.getHappinessState().coin_rate;
        return rate;
    }
    
    public double getTempleRate() {
        final double rate = 1.0;
        return rate;
    }
    
    public double getSpreadUpkeep() throws InvalidConfiguration {
        double total = 0.0;
        final double grace_distance = CivSettings.getDoubleTown("town.upkeep_town_block_grace_distance");
        final double base = CivSettings.getDoubleTown("town.upkeep_town_block_base");
        final double falloff = CivSettings.getDoubleTown("town.upkeep_town_block_falloff");
        final Structure townHall = this.getTownHall();
        if (townHall == null) {
            CivLog.error("No town hall for " + this.getName() + " while getting spread upkeep.");
            return 0.0;
        }
        final ChunkCoord townHallChunk = new ChunkCoord(townHall.getCorner().getLocation());
        for (final TownChunk tc : this.getTownChunks()) {
            if (tc.isOutpost()) {
                continue;
            }
            if (tc.getChunkCoord().equals(townHallChunk)) {
                continue;
            }
            double distance = tc.getChunkCoord().distance(townHallChunk);
            if (distance <= grace_distance) {
                continue;
            }
            distance -= grace_distance;
            final double upkeep = base * Math.pow(distance, falloff);
            total += upkeep;
        }
        return Math.floor(total);
    }
    
    public double getTotalUpkeep() throws InvalidConfiguration {
        return this.getBaseUpkeep() + this.getStructureUpkeep() + this.getSpreadUpkeep() + this.getOutpostUpkeep();
    }
    
    public double getTradeRate() {
        double rate = this.getGovernment().trade_rate;
        double fromStructures = 0.0;
        for (final Structure struct : this.structures.values()) {
            for (final Component comp : struct.attachedComponents) {
                if (comp instanceof AttributeRate) {
                    final AttributeRate as = (AttributeRate)comp;
                    if (!as.getString("attribute").equalsIgnoreCase("TRADE")) {
                        continue;
                    }
                    fromStructures += as.getGenerated();
                }
            }
        }
        rate += fromStructures;
        final double additional = rate * this.getBuffManager().getEffectiveDouble("buff_monopoly");
        rate += additional;
        rate *= this.getHappinessState().coin_rate;
        return rate;
    }
    
    public int getTileImprovementCount() {
        int count = 0;
        for (final Structure struct : this.getStructures()) {
            if (struct.isTileImprovement()) {
                ++count;
            }
        }
        return count;
    }
    
    public void removeTownChunk(final TownChunk tc) {
        if (tc.isOutpost()) {
            this.outposts.remove(tc.getChunkCoord());
        }
        else {
            this.townChunks.remove(tc.getChunkCoord());
        }
    }
    
    public Double getHammersFromCulture() {
        double hammers = 0.0;
        for (final CultureChunk cc : this.cultureChunks.values()) {
            hammers += cc.getHammers();
        }
        return hammers;
    }
    
    public void setBonusGoodies(final ConcurrentHashMap<String, BonusGoodie> bonusGoodies) {
        this.bonusGoodies = bonusGoodies;
    }
    
    public Collection<BonusGoodie> getBonusGoodies() {
        return this.bonusGoodies.values();
    }
    
    public void removeUpgrade(final ConfigTownUpgrade upgrade) {
        this.upgrades.remove(upgrade.id);
    }
    
    public Structure getNearestStrucutre(final Location location) {
        Structure nearest = null;
        double lowest_distance = Double.MAX_VALUE;
        for (final Structure struct : this.getStructures()) {
            final double distance = struct.getCenterLocation().getLocation().distance(location);
            if (distance < lowest_distance) {
                lowest_distance = distance;
                nearest = struct;
            }
        }
        return nearest;
    }
    
    public Buildable getNearestStrucutreOrWonderInprogress(final Location location) {
        Buildable nearest = null;
        double lowest_distance = Double.MAX_VALUE;
        for (final Structure struct : this.getStructures()) {
            final double distance = struct.getCenterLocation().getLocation().distance(location);
            if (distance < lowest_distance) {
                lowest_distance = distance;
                nearest = struct;
            }
        }
        for (final Wonder wonder : this.getWonders()) {
            if (wonder.isComplete()) {
                continue;
            }
            final double distance = wonder.getCenterLocation().getLocation().distance(location);
            if (distance >= lowest_distance) {
                continue;
            }
            lowest_distance = distance;
            nearest = wonder;
        }
        return nearest;
    }
    
    public void removeStructure(final Structure structure) {
        if (!structure.isComplete()) {
            this.removeBuildTask(structure);
        }
        if (this.currentStructureInProgress == structure) {
            this.currentStructureInProgress = null;
        }
        this.structures.remove(structure.getCorner());
        this.invalidStructures.remove(structure);
        this.disabledBuildables.remove(structure.getCorner());
    }
    
    public BuffManager getBuffManager() {
        return this.buffManager;
    }
    
    public void setBuffManager(final BuffManager buffManager) {
        this.buffManager = buffManager;
    }
    
    public void repairStructure(final Structure struct) throws CivException {
        struct.repairStructure();
    }
    
    public void onDefeat(final Civilization attackingCiv) {
        if (this.getMotherCiv() == null) {
            this.setMotherCiv(this.civ);
        }
        else if (this.getMotherCiv() == attackingCiv) {
            this.setMotherCiv(null);
        }
        this.changeCiv(attackingCiv);
        this.save();
    }
    
    public Civilization getDepositCiv() {
        return this.getCiv();
    }
    
    public Collection<Wonder> getWonders() {
        return this.wonders.values();
    }
    
    public void onGoodiePlaceIntoFrame(final ItemFrameStorage framestore, final BonusGoodie goodie) {
        final TownHall townhall = this.getTownHall();
        if (townhall == null) {
            return;
        }
        for (final ItemFrameStorage fs : townhall.getGoodieFrames()) {
            if (fs == framestore) {
                this.bonusGoodies.put(goodie.getOutpost().getCorner().toString(), goodie);
                for (final ConfigBuff cBuff : goodie.getConfigTradeGood().buffs.values()) {
                    final String key = "tradegood:" + goodie.getOutpost().getCorner() + ":" + cBuff.id;
                    if (this.buffManager.hasBuffKey(key)) {
                        continue;
                    }
                    try {
                        this.buffManager.addBuff(key, cBuff.id, goodie.getDisplayName());
                    }
                    catch (CivException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        for (final Structure struct : this.structures.values()) {
            struct.onGoodieToFrame();
        }
        for (final Wonder wonder : this.wonders.values()) {
            wonder.onGoodieToFrame();
        }
    }
    
    public void loadGoodiePlaceIntoFrame(final TownHall townhall, final BonusGoodie goodie) {
        this.bonusGoodies.put(goodie.getOutpost().getCorner().toString(), goodie);
        for (final ConfigBuff cBuff : goodie.getConfigTradeGood().buffs.values()) {
            final String key = "tradegood:" + goodie.getOutpost().getCorner() + ":" + cBuff.id;
            if (this.buffManager.hasBuffKey(key)) {
                continue;
            }
            try {
                this.buffManager.addBuff(key, cBuff.id, goodie.getDisplayName());
            }
            catch (CivException e) {
                e.printStackTrace();
            }
        }
    }
    
    public void removeGoodie(final BonusGoodie goodie) {
        this.bonusGoodies.remove(goodie.getOutpost().getCorner().toString());
        for (final ConfigBuff cBuff : goodie.getConfigTradeGood().buffs.values()) {
            final String key = "tradegood:" + goodie.getOutpost().getCorner() + ":" + cBuff.id;
            this.buffManager.removeBuff(key);
        }
        if (goodie.getFrame() != null) {
            goodie.getFrame().clearItem();
        }
    }
    
    public void onGoodieRemoveFromFrame(final ItemFrameStorage framestore, final BonusGoodie goodie) {
        final TownHall townhall = this.getTownHall();
        if (townhall == null) {
            return;
        }
        for (final ItemFrameStorage fs : townhall.getGoodieFrames()) {
            if (fs == framestore) {
                this.removeGoodie(goodie);
            }
        }
        for (final Structure struct : this.structures.values()) {
            struct.onGoodieFromFrame();
        }
        for (final Wonder wonder : this.wonders.values()) {
            wonder.onGoodieToFrame();
        }
    }
    
    public int getUnitTypeCount(final String id) {
        return 0;
    }
    
    public ArrayList<ConfigUnit> getAvailableUnits() {
        final ArrayList<ConfigUnit> unitList = new ArrayList<ConfigUnit>();
        for (final ConfigUnit unit : CivSettings.units.values()) {
            if (unit.isAvailable(this)) {
                unitList.add(unit);
            }
        }
        return unitList;
    }
    
    public void onTechUpdate() {
        try {
            for (final Structure struct : this.structures.values()) {
                if (struct.isActive()) {
                    struct.onTechUpdate();
                }
            }
            for (final Wonder wonder : this.wonders.values()) {
                if (wonder.isActive()) {
                    wonder.onTechUpdate();
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public Buildable getNearestBuildable(final Location location) {
        Buildable nearest = null;
        double distance = Double.MAX_VALUE;
        for (final Structure struct : this.structures.values()) {
            final double tmp = location.distance(struct.getCenterLocation().getLocation());
            if (tmp < distance) {
                nearest = struct;
                distance = tmp;
            }
        }
        for (final Wonder wonder : this.wonders.values()) {
            final double tmp = location.distance(wonder.getCenterLocation().getLocation());
            if (tmp < distance) {
                nearest = wonder;
                distance = tmp;
            }
        }
        return nearest;
    }
    
    public boolean isCapitol() {
        return this.getCiv().getCapitolName().equals(this.getName());
    }
    
    public boolean isForSale() {
        return this.getCiv().isTownsForSale() || (this.inDebt() && this.daysInDebt >= 7);
    }
    
    public double getForSalePrice() {
        final int points = this.getScore();
        try {
            final double coins_per_point = CivSettings.getDouble(CivSettings.scoreConfig, "coins_per_point");
            return coins_per_point * points;
        }
        catch (InvalidConfiguration e) {
            e.printStackTrace();
            return 0.0;
        }
    }
    
    public int getScore() {
        int points = 0;
        for (final Structure struct : this.getStructures()) {
            points += struct.getPoints();
        }
        for (final Wonder wonder : this.getWonders()) {
            points += wonder.getPoints();
        }
        try {
            final double perResident = CivSettings.getInteger(CivSettings.scoreConfig, "town_scores.resident");
            points += (int)(perResident * this.getResidents().size());
            final double perTownChunk = CivSettings.getInteger(CivSettings.scoreConfig, "town_scores.town_chunk");
            points += (int)(perTownChunk * this.getTownChunks().size());
            final double perCultureChunk = CivSettings.getInteger(CivSettings.scoreConfig, "town_scores.culture_chunk");
            if (this.cultureChunks != null) {
                points += (int)(perCultureChunk * this.cultureChunks.size());
            }
            else {
                CivLog.warning("Town " + this.getName() + " has no culture chunks??");
            }
            final double coins_per_point = CivSettings.getInteger(CivSettings.scoreConfig, "coins_per_point");
            points += (int)(this.getTreasury().getBalance() / coins_per_point);
        }
        catch (InvalidConfiguration e) {
            e.printStackTrace();
        }
        return points;
    }
    
    public void addOutpostChunk(final TownChunk tc) throws AlreadyRegisteredException {
        if (this.outposts.containsKey(tc.getChunkCoord())) {
            throw new AlreadyRegisteredException(CivSettings.localize.localizedString("var_town_outpost_alreadyRegistered", tc.getChunkCoord(), this.getName()));
        }
        this.outposts.put(tc.getChunkCoord(), tc);
    }
    
    public Collection<TownChunk> getOutpostChunks() {
        return this.outposts.values();
    }
    
    public double getOutpostUpkeep() {
        return 0.0;
    }
    
    public boolean isOutlaw(final String name) {
        final Resident res = CivGlobal.getResident(name);
        return this.outlaws.contains(res.getUUIDString());
    }
    
    public void addOutlaw(final String name) {
        final Resident res = CivGlobal.getResident(name);
        this.outlaws.add(res.getUUIDString());
        TaskMaster.syncTask(new SyncUpdateTags(res.getUUIDString(), this.residents.values()));
    }
    
    public void removeOutlaw(final String name) {
        final Resident res = CivGlobal.getResident(name);
        this.outlaws.remove(res.getUUIDString());
        TaskMaster.syncTask(new SyncUpdateTags(res.getUUIDString(), this.residents.values()));
    }
    
    public void changeCiv(final Civilization newCiv) {
        final Civilization oldCiv = this.civ;
        oldCiv.removeTown(this);
        oldCiv.save();
        newCiv.addTown(this);
        newCiv.save();
        final LinkedList<String> removeUs = new LinkedList<String>();
        for (final String outlaw : this.outlaws) {
            if (outlaw.length() >= 2) {
                final Resident resident = CivGlobal.getResidentViaUUID(UUID.fromString(outlaw));
                if (!newCiv.hasResident(resident)) {
                    continue;
                }
                removeUs.add(outlaw);
            }
        }
        for (final String outlaw : removeUs) {
            this.outlaws.remove(outlaw);
        }
        this.setCiv(newCiv);
        CivGlobal.processCulture();
        this.save();
    }
    
    public void validateResidentSelect(final Resident resident) throws CivException {
        if (this.getMayorGroup() == null || this.getAssistantGroup() == null || this.getDefaultGroup() == null || this.getCiv().getLeaderGroup() == null || this.getAssistantGroup() == null) {
            throw new CivException(CivSettings.localize.localizedString("town_validateSelect_error1"));
        }
        if (!this.getMayorGroup().hasMember(resident) && !this.getAssistantGroup().hasMember(resident) && !this.getDefaultGroup().hasMember(resident) && !this.getCiv().getLeaderGroup().hasMember(resident) && !this.getCiv().getAdviserGroup().hasMember(resident)) {
            throw new CivException(CivSettings.localize.localizedString("town_validateSelect_error2"));
        }
    }
    
    public void disband() {
        this.getCiv().removeTown(this);
        try {
            this.delete();
        }
        catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    public boolean touchesCapitolCulture(final HashSet<Town> closedSet) {
        if (this.isCapitol()) {
            return true;
        }
        closedSet.add(this);
        for (final Town t : this.townTouchList) {
            if (closedSet.contains(t)) {
                continue;
            }
            if (t.getCiv() != this.getCiv()) {
                continue;
            }
            final boolean touches = t.touchesCapitolCulture(closedSet);
            if (touches) {
                return true;
            }
        }
        return false;
    }
    
    public void incrementDaysInDebt() {
        ++this.daysInDebt;
        if (this.daysInDebt >= 7 && this.daysInDebt >= 14) {
            this.disband();
            CivMessage.global(CivSettings.localize.localizedString("var_town_ruin1", this.getName()));
            return;
        }
        CivMessage.global(String.valueOf(CivSettings.localize.localizedString("var_town_inDebt", this.getName())) + this.getDaysLeftWarning());
    }
    
    public String getDaysLeftWarning() {
        if (this.daysInDebt < 7) {
            return " " + CivSettings.localize.localizedString("var_town_inDebt_daysTilSale", 7 - this.daysInDebt);
        }
        if (this.daysInDebt < 14) {
            return " " + CivSettings.localize.localizedString("var_town_inDebt_daysTilDelete", this.getName(), 14 - this.daysInDebt);
        }
        return "";
    }
    
    public int getDaysInDebt() {
        return this.daysInDebt;
    }
    
    public void setDaysInDebt(final int daysInDebt) {
        this.daysInDebt = daysInDebt;
    }
    
    public void depositFromResident(final Double amount, final Resident resident) throws CivException {
        if (!resident.getTreasury().hasEnough(amount)) {
            throw new CivException(CivSettings.localize.localizedString("var_config_marketItem_notEnoughCurrency", amount + " " + CivSettings.CURRENCY_NAME));
        }
        if (this.inDebt()) {
            if (this.getDebt() > amount) {
                this.getTreasury().setDebt(this.getTreasury().getDebt() - amount);
                resident.getTreasury().withdraw(amount);
            }
            else {
                final double leftAmount = amount - this.getTreasury().getDebt();
                this.getTreasury().setDebt(0.0);
                this.getTreasury().deposit(leftAmount);
                resident.getTreasury().withdraw(amount);
            }
            if (!this.getTreasury().inDebt()) {
                this.daysInDebt = 0;
                CivMessage.global(CivSettings.localize.localizedString("town_ruin_nolongerInDebt", this.getName()));
            }
        }
        else {
            this.getTreasury().deposit(amount);
            resident.getTreasury().withdraw(amount);
        }
        this.save();
    }
    
    public Civilization getMotherCiv() {
        return this.motherCiv;
    }
    
    public void setMotherCiv(final Civilization motherCiv) {
        this.motherCiv = motherCiv;
    }
    
    public Collection<Resident> getOnlineResidents() {
        final LinkedList<Resident> residents = new LinkedList<Resident>();
        for (final Resident resident : this.getResidents()) {
            try {
                CivGlobal.getPlayer(resident);
                residents.add(resident);
            }
            catch (CivException ex) {}
        }
        for (final Resident resident : this.fakeResidents.values()) {
            residents.add(resident);
        }
        return residents;
    }
    
    public void capitulate() {
        if (this.getMotherCiv() == null) {
            return;
        }
        if (this.getMotherCiv().getCapitolName().equals(this.getName())) {
            this.getMotherCiv().capitulate();
            return;
        }
        this.setMotherCiv(null);
        this.save();
        CivMessage.global(CivSettings.localize.localizedString("var_town_capitulate1", this.getName(), this.getCiv().getName()));
    }
    
    public ConfigGovernment getGovernment() {
        if (this.getCiv().getGovernment().id.equals("gov_anarchy")) {
            if (this.motherCiv != null && !this.motherCiv.getGovernment().id.equals("gov_anarchy")) {
                return this.motherCiv.getGovernment();
            }
            if (this.motherCiv != null) {
                return CivSettings.governments.get("gov_tribalism");
            }
        }
        return this.getCiv().getGovernment();
    }
    
    public AttrSource getBeakerRate() {
        double rate = 1.0;
        final HashMap<String, Double> rates = new HashMap<String, Double>();
        final ConfigHappinessState state = this.getHappinessState();
        double newRate = rate * state.beaker_rate;
        rates.put("Happiness", newRate - rate);
        rate = newRate;
        newRate = rate * this.getGovernment().beaker_rate;
        rates.put("Government", newRate - rate);
        rate = newRate;
        double additional = rate * this.getBuffManager().getEffectiveDouble("buff_innovation");
        additional += rate * this.getBuffManager().getEffectiveDouble("buff_greatlibrary_extra_beakers");
        additional += rate * this.getBuffManager().getEffectiveDouble("buff_buff_msu_beakers_rate");
        rate += additional;
        rates.put("Goodies/Wonders", additional);
        return new AttrSource(rates, rate, null);
    }
    
    public AttrSource getBeakers() {
        AttrCache cache = this.attributeCache.get("BEAKERS");
        if (cache == null) {
            cache = new AttrCache();
            cache.lastUpdate = new Date();
        }
        else {
            final Date now = new Date();
            if (now.getTime() <= cache.lastUpdate.getTime() + 5000L) {
                return cache.sources;
            }
            cache.lastUpdate = now;
        }
        double beakers = 0.0;
        final HashMap<String, Double> sources = new HashMap<String, Double>();
        double fromCulture = 0.0;
        for (final CultureChunk cc : this.cultureChunks.values()) {
            fromCulture += cc.getBeakers();
        }
        sources.put("Culture Biomes", fromCulture);
        beakers += fromCulture;
        double fromStructures = 0.0;
        for (final Structure struct : this.structures.values()) {
            for (final Component comp : struct.attachedComponents) {
                if (comp instanceof AttributeBase) {
                    final AttributeBase as = (AttributeBase)comp;
                    if (!as.getString("attribute").equalsIgnoreCase("BEAKERS")) {
                        continue;
                    }
                    fromStructures += as.getGenerated();
                }
            }
        }
        beakers += fromStructures;
        sources.put("Structures", fromStructures);
        final double wondersTrade = 0.0;
        beakers += wondersTrade;
        sources.put("Goodies/Wonders", wondersTrade);
        beakers = Math.max(beakers, 0.0);
        final AttrSource rates = this.getBeakerRate();
        beakers *= rates.total;
        if (beakers < 0.0) {
            beakers = 0.0;
        }
        final AttrSource as2 = new AttrSource(sources, beakers, null);
        cache.sources = as2;
        this.attributeCache.put("BEAKERS", cache);
        return as2;
    }
    
    public AttrSource getHappiness() {
        final HashMap<String, Double> sources = new HashMap<String, Double>();
        double total = 0.0;
        AttrCache cache = this.attributeCache.get("HAPPINESS");
        if (cache == null) {
            cache = new AttrCache();
            cache.lastUpdate = new Date();
        }
        else {
            final Date now = new Date();
            if (now.getTime() <= cache.lastUpdate.getTime() + 5000L) {
                return cache.sources;
            }
            cache.lastUpdate = now;
        }
        final double townlevel = CivSettings.townHappinessLevels.get(this.getLevel()).happiness;
        total += townlevel;
        sources.put("Base Happiness", townlevel);
        final double goodiesWonders = this.buffManager.getEffectiveDouble("buff_hedonism");
        sources.put("Goodies/Wonders", goodiesWonders);
        total += goodiesWonders;
        final double goodiesWonders2 = this.buffManager.getEffectiveDouble("buff_salingship_happiness_rate");
		final double goodiesWonders3 = this.buffManager.getEffectiveDouble(buff_salingship_unhappiness_rate);
        sources.put("Goodies/Wonders", goodiesWonders2);
        total += goodiesWonders2;
        final int tradeGoods = this.bonusGoodies.size();
        if (tradeGoods > 0) {
            sources.put("Trade Goods", (double)tradeGoods);
        }
        total += tradeGoods;
        if (this.baseHappy != 0.0) {
            sources.put("Base Happiness", this.baseHappy);
            total += this.baseHappy;
        }
        double fromCulture = 0.0;
        for (final CultureChunk cc : this.cultureChunks.values()) {
            fromCulture += cc.getHappiness();
        }
        sources.put("Culture Biomes", fromCulture);
        total += fromCulture;
        double structures = 0.0;
        for (final Structure struct : this.structures.values()) {
            for (final Component comp : struct.attachedComponents) {
                if (comp instanceof AttributeBase) {
                    final AttributeBase as = (AttributeBase)comp;
                    if (!as.getString("attribute").equalsIgnoreCase("HAPPINESS")) {
                        continue;
                    }
                    final double h = as.getGenerated();
                    if (h <= 0.0) {
                        continue;
                    }
                    structures += h;
                }
            }
        }
        total += structures;
        sources.put("Structures", structures);
        if (total < 0.0) {
            total = 0.0;
        }
        final double randomEvent = RandomEvent.getHappiness(this);
        total += randomEvent;
        sources.put("Random Events", randomEvent);
        final AttrSource as2 = new AttrSource(sources, total, null);
        cache.sources = as2;
        this.attributeCache.put("HAPPINESS", cache);
        return as2;
    }
    
    public AttrSource getUnhappiness() {
        AttrCache cache = this.attributeCache.get("UNHAPPINESS");
        if (cache == null) {
            cache = new AttrCache();
            cache.lastUpdate = new Date();
        }
        else {
            final Date now = new Date();
            if (now.getTime() <= cache.lastUpdate.getTime() + 5000L) {
                return cache.sources;
            }
            cache.lastUpdate = now;
        }
        final HashMap<String, Double> sources = new HashMap<String, Double>();
        double total = this.getCiv().getCivWideUnhappiness(sources);
        double per_resident;
        try {
            per_resident = CivSettings.getDouble(CivSettings.happinessConfig, "happiness.per_resident");
        }
        catch (InvalidConfiguration e) {
            e.printStackTrace();
            return null;
        }
        final HashSet<Resident> UnResidents = new HashSet<Resident>();
        final HashSet<Resident> NonResidents = new HashSet<Resident>();
        for (final PermissionGroup group : this.getGroups()) {
            for (final Resident res : group.getMemberList()) {
                if (res.getCiv() != null) {
                    if (res.getCiv() == this.getCiv()) {
                        continue;
                    }
                    NonResidents.add(res);
                }
                else {
                    UnResidents.add(res);
                }
            }
        }
        final double happy_resident = per_resident * this.getResidents().size();
        final double happy_Nonresident = per_resident * 0.25 * NonResidents.size();
        final double happy_Unresident = per_resident * UnResidents.size();
        sources.put("Residents", happy_resident + happy_Nonresident + happy_Unresident);
        total += happy_resident + happy_Nonresident + happy_Unresident;
        if (sources.containsKey("War")) {
            for (final Structure struct : this.structures.values()) {
                for (final Component comp : struct.attachedComponents) {
                    if (!comp.isActive()) {
                        continue;
                    }
                    if (!(comp instanceof AttributeWarUnhappiness)) {
                        continue;
                    }
                    final AttributeWarUnhappiness warunhappyComp = (AttributeWarUnhappiness)comp;
                    double value = sources.get("War");
                    value += warunhappyComp.value;
                    if (value < 0.0) {
                        value = 0.0;
                    }
                    sources.put("War", value);
                }
            }
        }
        if (this.getMotherCiv() == null && !this.isCapitol()) {
            final double distance_unhappy = this.getCiv().getDistanceHappiness(this);
            total += distance_unhappy;
            sources.put("Distance To Capitol", distance_unhappy);
        }
        if (this.baseUnhappy != 0.0) {
            sources.put("Base Unhappiness", this.baseUnhappy);
            total += this.baseUnhappy;
        }
        double structures = 0.0;
        for (final Structure struct2 : this.structures.values()) {
            for (final Component comp2 : struct2.attachedComponents) {
                if (comp2 instanceof AttributeBase) {
                    final AttributeBase as = (AttributeBase)comp2;
                    if (!as.getString("attribute").equalsIgnoreCase("HAPPINESS")) {
                        continue;
                    }
                    final double h = as.getGenerated();
                    if (h >= 0.0) {
                        continue;
                    }
                    structures += h * -1.0;
                }
            }
        }
        total += structures;
        sources.put("Structures", structures);
        final double randomEvent = RandomEvent.getUnhappiness(this);
        total += randomEvent;
        if (randomEvent > 0.0) {
            sources.put("Random Events", randomEvent);
        }
        if (total < 0.0) {
            total = 0.0;
        }
        final AttrSource as2 = new AttrSource(sources, total, null);
        cache.sources = as2;
        this.attributeCache.put("UNHAPPINESS", cache);
        return as2;
    }
    
    public double getHappinessModifier() {
        return 1.0;
    }
    
    public double getHappinessPercentage() {
        final double total_happiness = this.getHappiness().total;
        final double total_unhappiness = this.getUnhappiness().total;
        final double total = total_happiness + total_unhappiness;
        return total_happiness / total;
    }
    
    public ConfigHappinessState getHappinessState() {
        return CivSettings.getHappinessState(this.getHappinessPercentage());
    }
    
    public void setBaseHappiness(final double happy) {
        this.baseHappy = happy;
    }
    
    public void setBaseUnhappy(final double happy) {
        this.baseUnhappy = happy;
    }
    
    public double getBaseGrowth() {
        return this.baseGrowth;
    }
    
    public void setBaseGrowth(final double baseGrowth) {
        this.baseGrowth = baseGrowth;
    }
    
    public Buildable getCurrentStructureInProgress() {
        return this.currentStructureInProgress;
    }
    
    public void setCurrentStructureInProgress(final Buildable currentStructureInProgress) {
        this.currentStructureInProgress = currentStructureInProgress;
    }
    
    public Buildable getCurrentWonderInProgress() {
        return this.currentWonderInProgress;
    }
    
    public void setCurrentWonderInProgress(final Buildable currentWonderInProgress) {
        this.currentWonderInProgress = currentWonderInProgress;
    }
    
    public void addFakeResident(final Resident fake) {
        this.fakeResidents.put(fake.getName(), fake);
    }
    
    public boolean processSpyExposure(final Resident resident) {
        final double exposure = resident.getSpyExposure();
        final double percent = exposure / Resident.MAX_SPY_EXPOSURE;
        boolean failed = false;
        Player player;
        try {
            player = CivGlobal.getPlayer(resident);
        }
        catch (CivException e1) {
            e1.printStackTrace();
            return failed;
        }
        String message = "";
        try {
            if (percent >= CivSettings.getDouble(CivSettings.espionageConfig, "espionage.town_exposure_failure")) {
                failed = true;
                CivMessage.sendTown(this, "§e" + CivColor.BOLD + CivSettings.localize.localizedString("town_spy_thwarted"));
                return failed;
            }
            if (percent > CivSettings.getDouble(CivSettings.espionageConfig, "espionage.town_exposure_warning")) {
                message = String.valueOf(message) + CivSettings.localize.localizedString("town_spy_currently") + " ";
            }
            if (percent > CivSettings.getDouble(CivSettings.espionageConfig, "espionage.town_exposure_location")) {
                message = String.valueOf(message) + CivSettings.localize.localizedString("var_town_spy_location", String.valueOf(player.getLocation().getBlockX()) + ", " + player.getLocation().getBlockY() + ", " + player.getLocation().getBlockZ()) + " ";
            }
            if (percent > CivSettings.getDouble(CivSettings.espionageConfig, "espionage.town_exposure_name")) {
                message = String.valueOf(message) + CivSettings.localize.localizedString("var_town_spy_perpetrator", resident.getName());
            }
            if (message.length() > 0 && (Town.lastMessage == null || !Town.lastMessage.equals(message))) {
                CivMessage.sendTown(this, "§e" + CivColor.BOLD + message);
                Town.lastMessage = message;
            }
        }
        catch (InvalidConfiguration e2) {
            e2.printStackTrace();
        }
        return failed;
    }
    
    public ArrayList<Perk> getTemplatePerks(final Buildable buildable, final Resident resident, final ConfigBuildableInfo info) {
        final ArrayList<Perk> perks = CustomTemplate.getTemplatePerksForBuildable(this, buildable.getTemplateBaseName());
        for (final Perk perk : resident.getPersonalTemplatePerks(info)) {
            perks.add(perk);
        }
        return perks;
    }
    
    public RandomEvent getActiveEvent() {
        return this.activeEvent;
    }
    
    public void setActiveEvent(final RandomEvent activeEvent) {
        this.activeEvent = activeEvent;
    }
    
    public double getUnusedBeakers() {
        return this.unusedBeakers;
    }
    
    public void setUnusedBeakers(final double unusedBeakers) {
        this.unusedBeakers = unusedBeakers;
    }
    
    public void addUnusedBeakers(final double more) {
        this.unusedBeakers += more;
    }
    
    public void markLastBuildableRefeshAsNow() {
        this.lastBuildableRefresh = new Date();
    }
    
    public void refreshNearestBuildable(final Resident resident) throws CivException {
        if (!this.getMayorGroup().hasMember(resident)) {
            throw new CivException(CivSettings.localize.localizedString("town_refresh_errorNotMayor"));
        }
        if (this.lastBuildableRefresh != null) {
            final Date now = new Date();
            int buildable_refresh_cooldown;
            try {
                buildable_refresh_cooldown = CivSettings.getInteger(CivSettings.townConfig, "town.buildable_refresh_cooldown");
            }
            catch (InvalidConfiguration e) {
                e.printStackTrace();
                throw new CivException(CivSettings.localize.localizedString("internalCommandException"));
            }
            if (now.getTime() < this.lastBuildableRefresh.getTime() + buildable_refresh_cooldown * 60 * 1000) {
                throw new CivException(CivSettings.localize.localizedString("var_town_refresh_wait1", buildable_refresh_cooldown));
            }
        }
        final Player player = CivGlobal.getPlayer(resident);
        final Buildable buildable = CivGlobal.getNearestBuildable(player.getLocation());
        if (buildable == null) {
            throw new CivException(CivSettings.localize.localizedString("town_refresh_couldNotFind"));
        }
        if (!buildable.isActive()) {
            throw new CivException(CivSettings.localize.localizedString("town_refresh_errorInProfress"));
        }
        if (War.isWarTime()) {
            throw new CivException(CivSettings.localize.localizedString("town_refresh_errorWar"));
        }
        if (buildable.getTown() != this) {
            throw new CivException(CivSettings.localize.localizedString("town_refresh_errorWrongTown"));
        }
        resident.setInteractiveMode(new InteractiveBuildableRefresh(buildable, resident.getName()));
    }
    
    public boolean areMayorsInactive() {
        try {
            final int mayor_inactive_days = CivSettings.getInteger(CivSettings.townConfig, "town.mayor_inactive_days");
            for (final Resident resident : this.getMayorGroup().getMemberList()) {
                if (!resident.isInactiveForDays(mayor_inactive_days)) {
                    return false;
                }
            }
        }
        catch (InvalidConfiguration e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }
    
    public void rename(final String name) throws CivException, InvalidNameException {
        final Town other = CivGlobal.getTown(name);
        if (other != null) {
            throw new CivException(CivSettings.localize.localizedString("town_rename_errorExists"));
        }
        if (this.isCapitol()) {
            this.getCiv().setCapitolName(name);
            this.getCiv().save();
        }
        final String oldName = this.getName();
        CivGlobal.removeTown(this);
        this.setName(name);
        this.save();
        CivGlobal.addTown(this);
        CivMessage.global(CivSettings.localize.localizedString("var_town_rename_success1", oldName, this.getName()));
    }
    
    public void trimCultureChunks(final HashSet<ChunkCoord> expanded) {
        final LinkedList<ChunkCoord> removedKeys = new LinkedList<ChunkCoord>();
        for (final ChunkCoord coord : this.cultureChunks.keySet()) {
            if (!expanded.contains(coord)) {
                removedKeys.add(coord);
            }
        }
        for (final ChunkCoord coord : removedKeys) {
            final CultureChunk cc = CivGlobal.getCultureChunk(coord);
            CivGlobal.removeCultureChunk(cc);
            this.cultureChunks.remove(coord);
        }
    }
    
    public ChunkCoord getTownCultureOrigin() {
        final TownHall townhall = this.getTownHall();
        ChunkCoord coord;
        if (townhall == null) {
            coord = this.getTownChunks().iterator().next().getChunkCoord();
        }
        else {
            coord = new ChunkCoord(townhall.getCenterLocation());
        }
        return coord;
    }
    
    public Date getCreated() {
        return this.created_date;
    }
    
    public void setCreated(final Date created_date) {
        this.created_date = created_date;
    }
    
    public void validateGift() throws CivException {
        try {
            final int min_gift_age = CivSettings.getInteger(CivSettings.civConfig, "civ.min_gift_age");
            if (!DateUtil.isAfterDays(this.created_date, min_gift_age)) {
                throw new CivException(CivSettings.localize.localizedString("var_town_gift_errorAge1", this.getName(), min_gift_age));
            }
        }
        catch (InvalidConfiguration e) {
            throw new CivException("Configuration error.");
        }
    }
    
    public int getGiftCost() {
        int gift_cost;
        try {
            gift_cost = CivSettings.getInteger(CivSettings.civConfig, "civ.gift_cost_per_town");
        }
        catch (InvalidConfiguration e) {
            e.printStackTrace();
            return 0;
        }
        return gift_cost;
    }
    
    public void clearBonusGoods() {
        this.bonusGoodies.clear();
    }
    
    public void processStructureFlipping(final HashMap<ChunkCoord, Structure> centerCoords) {
        for (final CultureChunk cc : this.cultureChunks.values()) {
            final Structure struct = centerCoords.get(cc.getChunkCoord());
            if (struct == null) {
                continue;
            }
            if (struct.getCiv() == cc.getCiv()) {
                continue;
            }
            struct.getTown().removeStructure(struct);
            struct.getTown().addStructure(struct);
            struct.setTown(this);
            struct.save();
        }
    }
    
    public boolean hasDisabledStructures() {
        return this.disabledBuildables.size() != 0;
    }
    
    public Collection<Buildable> getDisabledBuildables() {
        return this.disabledBuildables.values();
    }
    
    class AttrCache
    {
        public Date lastUpdate;
        public AttrSource sources;
    }
}
