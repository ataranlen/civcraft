package com.avrgaming.civcraft.structure.wonders;

import com.avrgaming.civcraft.components.AttributeBiomeRadiusPerLevel;
import com.avrgaming.civcraft.object.Town;
import org.bukkit.Location;
import com.avrgaming.civcraft.exception.CivException;
import java.sql.SQLException;
import java.sql.ResultSet;

public class SallingShip extends Wonder
{
    public SallingShip(final ResultSet rs) throws SQLException, CivException {
        super(rs);
    }
    
    public SallingShip(final Location center, final String id, final Town town) throws CivException {
        super(center, id, town);
    }
    
    public double getHammersPerTile() {
        final AttributeBiomeRadiusPerLevel attrBiome = (AttributeBiomeRadiusPerLevel)this.getComponent("AttributeBiomeBase");
        final double base = attrBiome.getBaseValue();
        double rate = 1.0;
        rate += this.getTown().getBuffManager().getEffectiveDouble("buff_advanced_tooling");
        return rate * base;
    }
    
    @Override
    protected void addBuffs() {
        this.addBuffToTown(this.getTown(), "buff_salingship_growth_rate");
		// percent task
        this.addBuffToCiv(this.getCiv(), "buff_salingship_happiness_rate");
		this.addBuufToCiv(this.getCiv(), "buff_salingship_unhappiness_rate");
		// percent task end
    }
    
    @Override
    protected void removeBuffs() {
        this.removeBuffFromTown(this.getTown(), "buff_salingship_growth_rate");
        this.removeBuffFromCiv(this.getCiv(), "buff_salingship_happiness_rate");
		this.removeBuffFromCiv(this.getCiv(), "buff_salingship_unhappiness_rate");
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
}
