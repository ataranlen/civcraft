package com.avrgaming.civcraft.object;

import java.text.DecimalFormat;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.config.ConfigBuff;

public class Buff
{
    public static final String FINE_ART = "buff_fine_art";
    public static final String CONSTRUCTION = "buff_construction";
    public static final String GROWTH_RATE = "buff_year_of_plenty";
    public static final String TRADE = "buff_monopoly";
    public static final String REDUCE_CONSUME = "buff_preservative";
    public static final String SCIENCE_RATE = "buff_innovation";
    public static final String EXTRA_CULTURE = "buff_doesnotexist";
    public static final String COTTAGE_RATE = "buff_doesnotexist";
    public static final String ADVANCED_TOOLING = "buff_advanced_tooling";
    public static final String BARRICADE = "buff_barricade";
    public static final String BARTER = "buff_barter";
    public static final String EXTRACTION = "buff_extraction";
    public static final String FIRE_BOMB = "buff_fire_bomb";
    public static final String FISHING = "buff_fishing";
    public static final String MEDICINE = "buff_medicine";
    public static final String RUSH = "buff_rush";
    public static final String DEBUFF_PYRAMID_LEECH = "debuff_pyramid_leech";
    private ConfigBuff config;
    private String source;
    private String key;
    
    public Buff(final String buffkey, final String buff_id, final String source) {
        this.config = CivSettings.buffs.get(buff_id);
        this.setKey(buffkey);
        this.source = source;
    }
    
    @Override
    public int hashCode() {
        return this.config.id.toString().hashCode();
    }
    
    @Override
    public boolean equals(final Object other) {
        if (other instanceof Buff) {
            final Buff otherBuff = (Buff)other;
            if (otherBuff.getConfig().id.equals(this.getConfig().id)) {
                return true;
            }
        }
        return false;
    }
    
    public String getSource() {
        return this.source;
    }
    
    public void setSource(final String source) {
        this.source = source;
    }
    
    public ConfigBuff getConfig() {
        return this.config;
    }
    
    public void setConfig(final ConfigBuff config) {
        this.config = config;
    }
    
    public boolean isStackable() {
        return this.config.stackable;
    }
    
    public String getId() {
        return this.config.id;
    }
    
    public Object getParent() {
        return this.config.parent;
    }
    
    public String getValue() {
        return this.config.value;
    }
    
    public String getDisplayDouble() {
        try {
            final double d = Double.valueOf(this.config.value);
            final DecimalFormat df = new DecimalFormat();
            return String.valueOf(df.format(d * 100.0)) + "%";
        }
        catch (NumberFormatException e) {
            return "NAN!";
        }
    }
    
    public String getDisplayInt() {
        try {
            final int i = Integer.valueOf(this.config.value);
            return new StringBuilder().append(i).toString();
        }
        catch (NumberFormatException e) {
            return "NAN!";
        }
    }
    
    public String getDisplayName() {
        return this.config.name;
    }
    
    public String getKey() {
        return this.key;
    }
    
    public void setKey(final String key) {
        this.key = key;
    }
}
