package com.avrgaming.civcraft.mobs;

import com.avrgaming.civcraft.mobs.MobSpawner.CustomMobType;
import com.avrgaming.civcraft.mobs.MobSpawner.CustomMobLevel;

public class TypeLevel {
	public CustomMobType type;
	public CustomMobLevel level;

	public TypeLevel(CustomMobType type, CustomMobLevel level) {
		this.type = type;
		this.level = level;
	}
}
