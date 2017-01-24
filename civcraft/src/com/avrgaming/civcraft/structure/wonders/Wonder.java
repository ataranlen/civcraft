package com.avrgaming.civcraft.structure.wonders;

import com.avrgaming.civcraft.object.Civilization;
import org.bukkit.block.Block;
import com.avrgaming.civcraft.template.Template;
import org.bukkit.entity.Player;
import java.io.IOException;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.NamedObject;
import java.util.Iterator;
import com.avrgaming.civcraft.config.ConfigBuff;
import java.util.HashMap;
import com.avrgaming.civcraft.object.SQLObject;
import com.avrgaming.civcraft.database.SQLUpdate;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.database.SQL;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.util.BlockCoord;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.config.ConfigBuildableInfo;
import com.avrgaming.civcraft.object.Town;
import org.bukkit.Location;
import com.avrgaming.civcraft.exception.CivException;
import java.sql.SQLException;
import java.sql.ResultSet;
import com.avrgaming.civcraft.config.ConfigWonderBuff;
import com.avrgaming.civcraft.structure.Buildable;

public abstract class Wonder extends Buildable
{
    public static String TABLE_NAME;
    private ConfigWonderBuff wonderBuffs;
    
    static {
        Wonder.TABLE_NAME = "WONDERS";
    }
    
    public Wonder(final ResultSet rs) throws SQLException, CivException {
        this.wonderBuffs = null;
        this.load(rs);
        if (this.hitpoints == 0) {
            this.delete();
        }
    }
    
    public Wonder(final Location center, final String id, final Town town) throws CivException {
        this.wonderBuffs = null;
        this.info = CivSettings.wonders.get(id);
        this.setTown(town);
        this.setCorner(new BlockCoord(center));
        this.hitpoints = this.info.max_hitpoints;
        final Wonder wonder = CivGlobal.getWonder(this.getCorner());
        if (wonder != null) {
            throw new CivException(CivSettings.localize.localizedString("wonder_alreadyExistsHere"));
        }
    }
    
    @Override
    public void loadSettings() {
        this.wonderBuffs = CivSettings.wonderBuffs.get(this.getConfigId());
        if (this.isComplete() && this.isActive()) {
            this.addWonderBuffsToTown();
        }
    }
    
    public static void init() throws SQLException {
        if (!SQL.hasTable(Wonder.TABLE_NAME)) {
            final String table_create = "CREATE TABLE " + SQL.tb_prefix + Wonder.TABLE_NAME + " (" + "`id` int(11) unsigned NOT NULL auto_increment," + "`type_id` mediumtext NOT NULL," + "`town_id` int(11) DEFAULT NULL," + "`complete` bool NOT NULL DEFAULT '0'," + "`builtBlockCount` int(11) DEFAULT NULL, " + "`cornerBlockHash` mediumtext DEFAULT NULL," + "`template_name` mediumtext DEFAULT NULL, " + "`template_x` int(11) DEFAULT NULL, " + "`template_y` int(11) DEFAULT NULL, " + "`template_z` int(11) DEFAULT NULL, " + "`hitpoints` int(11) DEFAULT '100'," + "PRIMARY KEY (`id`)" + ")";
            SQL.makeTable(table_create);
            CivLog.info("Created " + Wonder.TABLE_NAME + " table");
        }
        else {
            CivLog.info(String.valueOf(Wonder.TABLE_NAME) + " table OK!");
        }
    }
    
    @Override
    public void load(final ResultSet rs) throws SQLException, CivException {
        this.setId(rs.getInt("id"));
        this.info = CivSettings.wonders.get(rs.getString("type_id"));
        this.setTown(CivGlobal.getTownFromId(rs.getInt("town_id")));
        if (this.getTown() == null) {
            throw new CivException("Coudln't find town ID:" + rs.getInt("town_id") + " for wonder " + this.getDisplayName() + " ID:" + this.getId());
        }
        this.setCorner(new BlockCoord(rs.getString("cornerBlockHash")));
        this.hitpoints = rs.getInt("hitpoints");
        this.setTemplateName(rs.getString("template_name"));
        this.setTemplateX(rs.getInt("template_x"));
        this.setTemplateY(rs.getInt("template_y"));
        this.setTemplateZ(rs.getInt("template_z"));
        this.setComplete(rs.getBoolean("complete"));
        this.setBuiltBlockCount(rs.getInt("builtBlockCount"));
        this.getTown().addWonder(this);
        this.bindStructureBlocks();
        if (!this.isComplete()) {
            try {
                this.resumeBuildFromTemplate();
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    
    @Override
    public void save() {
        SQLUpdate.add(this);
    }
    
    @Override
    public void saveNow() throws SQLException {
        final HashMap<String, Object> hashmap = new HashMap<String, Object>();
        hashmap.put("type_id", this.getConfigId());
        hashmap.put("town_id", this.getTown().getId());
        hashmap.put("complete", this.isComplete());
        hashmap.put("builtBlockCount", this.getBuiltBlockCount());
        hashmap.put("cornerBlockHash", this.getCorner().toString());
        hashmap.put("hitpoints", this.getHitpoints());
        hashmap.put("template_name", this.getSavedTemplatePath());
        hashmap.put("template_x", this.getTemplateX());
        hashmap.put("template_y", this.getTemplateY());
        hashmap.put("template_z", this.getTemplateZ());
        SQL.updateNamedObject(this, hashmap, Wonder.TABLE_NAME);
    }
    
    @Override
    public void delete() throws SQLException {
        super.delete();
        if (this.wonderBuffs != null) {
            for (final ConfigBuff buff : this.wonderBuffs.buffs) {
                this.getTown().getBuffManager().removeBuff(buff.id);
            }
        }
        SQL.deleteNamedObject(this, Wonder.TABLE_NAME);
        CivGlobal.removeWonder(this);
    }
    
    @Override
    public void updateBuildProgess() {
        if (this.getId() != 0) {
            final HashMap<String, Object> struct_hm = new HashMap<String, Object>();
            struct_hm.put("id", this.getId());
            struct_hm.put("type_id", this.getConfigId());
            struct_hm.put("complete", this.isComplete());
            struct_hm.put("builtBlockCount", this.savedBlockCount);
            try {
                SQL.updateNamedObjectAsync(this, struct_hm, Wonder.TABLE_NAME);
            }
            catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
    
    public static boolean isWonderAvailable(final String configId) {
        if (CivGlobal.isCasualMode()) {
            return true;
        }
        for (final Wonder wonder : CivGlobal.getWonders()) {
            if (wonder.getConfigId().equals(configId)) {
                if (wonder.getConfigId().equals("w_colosseum") || wonder.getConfigId().equals("w_battledome")) {
                    return true;
                }
                if (wonder.isComplete()) {
                    return false;
                }
                continue;
            }
        }
        return true;
    }
    
    @Override
    public void processUndo() throws CivException {
        try {
            this.undoFromTemplate();
        }
        catch (IOException e1) {
            e1.printStackTrace();
            CivMessage.sendTown(this.getTown(), "§c" + CivSettings.localize.localizedString("wonder_undo_error"));
            this.fancyDestroyStructureBlocks();
        }
        CivMessage.global(CivSettings.localize.localizedString("var_wonder_undo_broadcast", "§a" + this.getDisplayName() + "§f", this.getTown().getName(), this.getTown().getCiv().getName()));
        final double refund = this.getCost();
        this.getTown().depositDirect(refund);
        CivMessage.sendTown(this.getTown(), CivSettings.localize.localizedString("var_structure_undo_refund", this.getTown().getName(), refund, CivSettings.CURRENCY_NAME));
        this.unbindStructureBlocks();
        try {
            this.delete();
            this.getTown().removeWonder(this);
        }
        catch (SQLException e2) {
            e2.printStackTrace();
            throw new CivException(CivSettings.localize.localizedString("internalDatabaseException"));
        }
    }
    
    @Override
    public void build(final Player player, Location centerLoc, final Template tpl) throws Exception {
        final Location savedLocation = centerLoc.clone();
        centerLoc = this.repositionCenter(centerLoc, tpl.dir(), tpl.size_x, tpl.size_z);
        final Block centerBlock = centerLoc.getBlock();
        this.setTotalBlockCount(tpl.size_x * tpl.size_y * tpl.size_z);
        this.setTemplateName(tpl.getFilepath());
        this.setTemplateX(tpl.size_x);
        this.setTemplateY(tpl.size_y);
        this.setTemplateZ(tpl.size_z);
        this.setTemplateAABB(new BlockCoord(centerLoc), tpl);
        this.checkBlockPermissionsAndRestrictions(player, centerBlock, tpl.size_x, tpl.size_y, tpl.size_z, savedLocation);
        this.runOnBuild(centerLoc, tpl);
        this.getTown().lastBuildableBuilt = this;
        tpl.saveUndoTemplate(this.getCorner().toString(), this.getTown().getName(), centerLoc);
        tpl.buildScaffolding(centerLoc);
        this.startBuildTask(tpl, centerLoc);
        this.save();
        CivGlobal.addWonder(this);
        CivMessage.global(CivSettings.localize.localizedString("var_wonder_startedByCiv", this.getCiv().getName(), this.getDisplayName(), this.getTown().getName()));
    }
    
    @Override
    public String getDynmapDescription() {
        return null;
    }
    
    @Override
    public String getMarkerIconName() {
        return "beer";
    }
    
    @Override
    protected void runOnBuild(final Location centerLoc, final Template tpl) throws CivException {
    }
    
    @Override
    public void onDestroy() {
        if (!CivGlobal.isCasualMode()) {
            CivMessage.global(CivSettings.localize.localizedString("var_wonder_destroyed", this.getDisplayName(), this.getTown().getName()));
            try {
                this.getTown().removeWonder(this);
                this.fancyDestroyStructureBlocks();
                this.unbindStructureBlocks();
                this.delete();
            }
            catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
    
    public static Wonder newWonder(final Location center, final String id, final Town town) throws CivException {
        try {
            return _newWonder(center, id, town, null);
        }
        catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }
    
    public static Wonder _newWonder(final Location center, final String id, final Town town, final ResultSet rs) throws CivException, SQLException {
        Wonder wonder = null;
        Label_0917: {
            switch (id.hashCode()) {
                case -1847496618: {
                    if (!id.equals("w_greatlibrary")) {
                        throw new CivException(String.valueOf(CivSettings.localize.localizedString("wonder_unknwon_type")) + " " + id);
                    }
                    if (rs == null) {
                        wonder = new GreatLibrary(center, id, town);
                        break Label_0917;
                    }
                    wonder = new GreatLibrary(rs);
                    break Label_0917;
                }
                case -1817858420: {
                    if (!id.equals("w_grandcanyon")) {
                        throw new CivException(String.valueOf(CivSettings.localize.localizedString("wonder_unknwon_type")) + " " + id);
                    }
                    if (rs == null) {
                        wonder = new GrandCanyon(center, id, town);
                        break Label_0917;
                    }
                    wonder = new GrandCanyon(rs);
                    break Label_0917;
                }
                case -1705782403: {
                    if (!id.equals("w_globe_theatre")) {
                        throw new CivException(String.valueOf(CivSettings.localize.localizedString("wonder_unknwon_type")) + " " + id);
                    }
                    if (rs == null) {
                        wonder = new GlobeTheatre(center, id, town);
                        break Label_0917;
                    }
                    wonder = new GlobeTheatre(rs);
                    break Label_0917;
                }
                case -1145911234: {
                    if (!id.equals("w_grand_ship_ingermanland")) {
                        throw new CivException(String.valueOf(CivSettings.localize.localizedString("wonder_unknwon_type")) + " " + id);
                    }
                    if (rs == null) {
                        wonder = new GrandShipIngermanland(center, id, town);
                        break Label_0917;
                    }
                    wonder = new GrandShipIngermanland(rs);
                    break Label_0917;
                }
                case 4653384: {
                    if (!id.equals("w_sallingship")) {
                        throw new CivException(String.valueOf(CivSettings.localize.localizedString("wonder_unknwon_type")) + " " + id);
                    }
                    break;
                }
                case 112837575: {
                    if (!id.equals("w_msu")) {
                        throw new CivException(String.valueOf(CivSettings.localize.localizedString("wonder_unknwon_type")) + " " + id);
                    }
                    if (rs == null) {
                        wonder = new msu(center, id, town);
                        break;
                    }
                    final msu msu = new msu(rs);
                    break;
                }
                case 341317586: {
                    if (!id.equals("w_mother_tree")) {
                        throw new CivException(String.valueOf(CivSettings.localize.localizedString("wonder_unknwon_type")) + " " + id);
                    }
                    if (rs == null) {
                        wonder = new MotherTree(center, id, town);
                        break Label_0917;
                    }
                    wonder = new MotherTree(rs);
                    break Label_0917;
                }
                case 489390797: {
                    if (!id.equals("w_council_of_eight")) {
                        throw new CivException(String.valueOf(CivSettings.localize.localizedString("wonder_unknwon_type")) + " " + id);
                    }
                    if (rs == null) {
                        wonder = new CouncilOfEight(center, id, town);
                        break Label_0917;
                    }
                    wonder = new CouncilOfEight(rs);
                    break Label_0917;
                }
                case 856715638: {
                    if (!id.equals("w_hanginggardens")) {
                        throw new CivException(String.valueOf(CivSettings.localize.localizedString("wonder_unknwon_type")) + " " + id);
                    }
                    if (rs == null) {
                        wonder = new TheHangingGardens(center, id, town);
                        break Label_0917;
                    }
                    wonder = new TheHangingGardens(rs);
                    break Label_0917;
                }
                case 1104194436: {
                    if (!id.equals("w_great_lighthouse")) {
                        throw new CivException(String.valueOf(CivSettings.localize.localizedString("wonder_unknwon_type")) + " " + id);
                    }
                    if (rs == null) {
                        wonder = new GreatLighthouse(center, id, town);
                        break Label_0917;
                    }
                    wonder = new GreatLighthouse(rs);
                    break Label_0917;
                }
                case 1323470051: {
                    if (!id.equals("w_battledome")) {
                        throw new CivException(String.valueOf(CivSettings.localize.localizedString("wonder_unknwon_type")) + " " + id);
                    }
                    if (rs == null) {
                        wonder = new Battledome(center, id, town);
                        break Label_0917;
                    }
                    wonder = new Battledome(rs);
                    break Label_0917;
                }
                case 1398468103: {
                    if (!id.equals("w_chichen_itza")) {
                        throw new CivException(String.valueOf(CivSettings.localize.localizedString("wonder_unknwon_type")) + " " + id);
                    }
                    if (rs == null) {
                        wonder = new ChichenItza(center, id, town);
                        break Label_0917;
                    }
                    wonder = new ChichenItza(rs);
                    break Label_0917;
                }
                case 1405009926: {
                    if (!id.equals("w_colosseum")) {
                        throw new CivException(String.valueOf(CivSettings.localize.localizedString("wonder_unknwon_type")) + " " + id);
                    }
                    if (rs == null) {
                        wonder = new Colosseum(center, id, town);
                        break Label_0917;
                    }
                    wonder = new Colosseum(rs);
                    break Label_0917;
                }
                case 1744750582: {
                    if (!id.equals("w_notre_dame")) {
                        throw new CivException(String.valueOf(CivSettings.localize.localizedString("wonder_unknwon_type")) + " " + id);
                    }
                    if (rs == null) {
                        wonder = new NotreDame(center, id, town);
                        break Label_0917;
                    }
                    wonder = new NotreDame(rs);
                    break Label_0917;
                }
                case 1846438709: {
                    if (!id.equals("w_colossus")) {
                        throw new CivException(String.valueOf(CivSettings.localize.localizedString("wonder_unknwon_type")) + " " + id);
                    }
                    if (rs == null) {
                        wonder = new TheColossus(center, id, town);
                        break Label_0917;
                    }
                    wonder = new TheColossus(rs);
                    break Label_0917;
                }
                case 1913111944: {
                    if (!id.equals("w_pyramid")) {
                        throw new CivException(String.valueOf(CivSettings.localize.localizedString("wonder_unknwon_type")) + " " + id);
                    }
                    if (rs == null) {
                        wonder = new TheGreatPyramid(center, id, town);
                        break Label_0917;
                    }
                    wonder = new TheGreatPyramid(rs);
                    break Label_0917;
                }
            }
            if (rs == null) {
                wonder = new SallingShip(center, id, town);
                break Label_0917;
            }
            wonder = new SallingShip(rs);
            break Label_0917;
        }
        wonder.loadSettings();
        return wonder;
    }
    
    public void addWonderBuffsToTown() {
        if (this.wonderBuffs == null) {
            return;
        }
        for (final ConfigBuff buff : this.wonderBuffs.buffs) {
            try {
                this.getTown().getBuffManager().addBuff("wonder:" + this.getDisplayName() + ":" + this.getCorner() + ":" + buff.id, buff.id, this.getDisplayName());
            }
            catch (CivException e) {
                e.printStackTrace();
            }
        }
    }
    
    @Override
    public void onComplete() {
        this.addWonderBuffsToTown();
    }
    
    public ConfigWonderBuff getWonderBuffs() {
        return this.wonderBuffs;
    }
    
    public void setWonderBuffs(final ConfigWonderBuff wonderBuffs) {
        this.wonderBuffs = wonderBuffs;
    }
    
    public static Wonder newWonder(final ResultSet rs) throws CivException, SQLException {
        return _newWonder(null, rs.getString("type_id"), null, rs);
    }
    
    @Override
    public void onLoad() {
    }
    
    @Override
    public void onUnload() {
    }
    
    protected void addBuffToTown(final Town town, final String id) {
        try {
            town.getBuffManager().addBuff(id, id, String.valueOf(this.getDisplayName()) + " in " + this.getTown().getName());
        }
        catch (CivException e) {
            e.printStackTrace();
        }
    }
    
    protected void addBuffToCiv(final Civilization civ, final String id) {
        for (final Town t : civ.getTowns()) {
            this.addBuffToTown(t, id);
        }
    }
    
    protected void removeBuffFromTown(final Town town, final String id) {
        town.getBuffManager().removeBuff(id);
    }
    
    protected void removeBuffFromCiv(final Civilization civ, final String id) {
        for (final Town t : civ.getTowns()) {
            this.removeBuffFromTown(t, id);
        }
    }
    
    protected abstract void removeBuffs();
    
    protected abstract void addBuffs();
    
    public void processCoinsFromCulture() {
        int cultureCount = 0;
        for (final Town t : this.getCiv().getTowns()) {
            cultureCount += t.getCultureChunks().size();
        }
        final double coinsPerCulture = Double.valueOf(CivSettings.buffs.get("buff_colossus_coins_from_culture").value);
        final double total = coinsPerCulture * cultureCount;
        this.getCiv().getTreasury().deposit(total);
        CivMessage.sendCiv(this.getCiv(), "§a" + CivSettings.localize.localizedString("var_colossus_generatedCoins", "§e" + total + "§a", CivSettings.CURRENCY_NAME, cultureCount));
    }
    
    public void processCoinsFromColosseum() {
        int townCount = 0;
        for (final Civilization civ : CivGlobal.getCivs()) {
            townCount += civ.getTownCount();
        }
        final double coinsPerTown = Double.valueOf(CivSettings.buffs.get("buff_colosseum_coins_from_towns").value);
        final double total = coinsPerTown * townCount;
        this.getCiv().getTreasury().deposit(total);
        CivMessage.sendCiv(this.getCiv(), "§a" + CivSettings.localize.localizedString("var_colosseum_generatedCoins", "§e" + total + "§a", CivSettings.CURRENCY_NAME, townCount));
    }
}
