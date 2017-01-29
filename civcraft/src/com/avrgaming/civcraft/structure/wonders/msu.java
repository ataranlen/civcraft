package com.avrgaming.civcraft.structure.wonders;

import java.util.Iterator;
import java.sql.SQLException;
import java.sql.ResultSet;

import org.bukkit.command.CommandSender;
// import org.bukkit.enchantments.Enchantment;
import org.bukkit.Location;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.config.ConfigEnchant;
import com.avrgaming.civcraft.object.StructureSign;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.structure.Library

public class msu extends Wonder
{
    public msu(final Location center, final String id, final Town town) throws CivException {
        super(center, id, town);
    }
    
    public msu(final ResultSet rs) throws SQLException, CivException {
        super(rs);
    }
    
    @Override
    public void onLoad() {
        if (this.isActive()) {
            this.addBuffs();
        }
    }
    
    @Override
    public void onComplete() {
        this.addBuffs();
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        this.removeBuffs();
    }
    
    @Override
    protected void removeBuffs() {
        this.removeBuffFromTown(this.getTown(), "buff_msu_beakers_rate");
        this.removeBuffFromTown(this.getTown(), "buff_msu_profit_sharing");
    }
    
    @Override
    protected void addBuffs() {
        this.addBuffToTown(this.getTown(), "buff_msu_beakers_rate");
        this.addBuffToTown(this.getTown(), "buff_msu_profit_sharing");
    }
    
    @Override
    public void updateSignText() {
        for (final StructureSign sign : this.getSigns()) {
            final String lowerCase;
            switch (lowerCase = sign.getAction().toLowerCase()) {
                case "4": {
                    final ConfigEnchant enchant = CivSettings.enchants.get("unbreaking");
                    sign.setText(String.valueOf(enchant.name) + "\n\n" + "§a" + enchant.cost + " " + CivSettings.CURRENCY_NAME);
                    break;
                }
                case "5": {
                    final ConfigEnchant enchant = CivSettings.enchants.get("feather_falling");
                    sign.setText(String.valueOf(enchant.name) + "\n\n" + "§a" + enchant.cost + " " + CivSettings.CURRENCY_NAME);
                    break;
                }
                case "6": {
                    final ConfigEnchant enchant = CivSettings.enchants.get("knockback");
                    sign.setText(String.valueOf(enchant.name) + "\n\n" + "§a" + enchant.cost + " " + CivSettings.CURRENCY_NAME);
                    break;
                }
                case "7": {
                    final ConfigEnchant enchant = CivSettings.enchants.get("fortune");
                    sign.setText(String.valueOf(enchant.name) + "\n\n" + "§a" + enchant.cost + " " + CivSettings.CURRENCY_NAME);
                    break;
                }
                default:
                    break;
            }
            sign.update();
        }
    }
    
    @Override
    public void processSignAction(final Player player, final StructureSign sign, final PlayerInteractEvent event) {
        final Resident resident = CivGlobal.getResident(player);
        if (resident == null) {
            return;
        }
        if (!resident.hasTown() || resident.getCiv() != this.getCiv()) {
            CivMessage.sendError(player, CivSettings.localize.localizedString("var_msu_nonMember", this.getCiv().getName()));
            return;
        }
        final ItemStack hand = player.getInventory().getItemInMainHand();
        Label_0713: {
            final String action;
            switch (action = sign.getAction()) {
                case "4": {
                    if (!Enchantment.DURABILITY.canEnchantItem(hand)) {
                        CivMessage.sendError(player, CivSettings.localize.localizedString("msu_enchant_cannotEnchant"));
                        return;
                    }
                    final ConfigEnchant configEnchant = CivSettings.enchants.get("unbreaking");
                    if (!resident.getTreasury().hasEnough(configEnchant.cost)) {
                        CivMessage.send(player, "§c" + CivSettings.localize.localizedString("var_msu_enchant_cannotAfford", configEnchant.cost, CivSettings.CURRENCY_NAME));
                        return;
                    }
                    resident.getTreasury().withdraw(configEnchant.cost);
                    hand.addEnchantment(Enchantment.DURABILITY, 3);
                    break Label_0713;
                }
                case "5": {
                    if (!Enchantment.PROTECTION_FALL.canEnchantItem(hand)) {
                        CivMessage.sendError(player, CivSettings.localize.localizedString("msu_enchant_cannotEnchant"));
                        return;
                    }
                    final ConfigEnchant configEnchant = CivSettings.enchants.get("feather_falling");
                    if (!resident.getTreasury().hasEnough(configEnchant.cost)) {
                        CivMessage.send(player, "§c" + CivSettings.localize.localizedString("var_msu_enchant_cannotAfford", configEnchant.cost, CivSettings.CURRENCY_NAME));
                        return;
                    }
                    resident.getTreasury().withdraw(configEnchant.cost);
                    hand.addEnchantment(Enchantment.PROTECTION_FALL, 4);
                    break Label_0713;
                }
                case "6": {
                    if (!Enchantment.KNOCKBACK.canEnchantItem(hand)) {
                        CivMessage.sendError(player, CivSettings.localize.localizedString("msu_enchant_cannotEnchant"));
                        return;
                    }
                    final ConfigEnchant configEnchant = CivSettings.enchants.get("knockback");
                    if (!resident.getTreasury().hasEnough(configEnchant.cost)) {
                        CivMessage.send(player, "§c" + CivSettings.localize.localizedString("var_msu_enchant_cannotAfford", configEnchant.cost, CivSettings.CURRENCY_NAME));
                        return;
                    }
                    resident.getTreasury().withdraw(configEnchant.cost);
                    hand.addEnchantment(Enchantment.KNOCKBACK, 2);
                    break Label_0713;
                }
                case "7": {
                    if (!Enchantment.LOOT_BONUS_BLOCKS.canEnchantItem(hand)) {
                        CivMessage.sendError(player, CivSettings.localize.localizedString("msu_enchant_cannotEnchant"));
                        return;
                    }
                    final ConfigEnchant configEnchant = CivSettings.enchants.get("fortune");
                    if (!resident.getTreasury().hasEnough(configEnchant.cost)) {
                        CivMessage.send(player, "§c" + CivSettings.localize.localizedString("var_msu_enchant_cannotAfford", configEnchant.cost, CivSettings.CURRENCY_NAME));
                        return;
                    }
                    resident.getTreasury().withdraw(configEnchant.cost);
                    hand.addEnchantment(Enchantment.LOOT_BONUS_BLOCKS, 2);
                    break Label_0713;
                }
                default:
                    break;
            }
            CivMessage.sendError(player, CivSettings.localize.localizedString("msu_enchant_cannotEnchant"));
            return;
        }
        CivMessage.sendSuccess((CommandSender)player, CivSettings.localize.localizedString("msu_enchantment_success"));
    }
}
