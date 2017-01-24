package com.avrgaming.civcraft.structure.wonders;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.bukkit.Location;

import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.object.Town;

public class GrandCanyon extends Wonder {

	public GrandCanyon(ResultSet rs) throws SQLException, CivException {
		super(rs);
	}

	public GrandCanyon(Location center, String id, Town town)
			throws CivException {
		super(center, id, town);
	}

	@Override
	protected void addBuffs() {
		addBuffToCiv(this.getCiv(), "buff_grand_canyon_extraction");
		addBuffToCiv(this.getCiv(), "buff_rush");
		addBuffToCiv(this.getCiv(), "buff_grand_canyon_construction");
	}
	
	@Override
	protected void removeBuffs() {
		removeBuffFromCiv(this.getCiv(), "buff_grand_canyon_extraction");
		removeBuffFromCiv(this.getCiv(), "buff_rush");
		removeBuffFromCiv(this.getCiv(), "buff_grand_canyon_construction");
	}
	
	@Override
	public void onLoad() {
		if (this.isActive()) {
			addBuffs();
		}
	}
	
	@Override
	public void onComplete() {
		addBuffs();
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		removeBuffs();
	}
	
}
