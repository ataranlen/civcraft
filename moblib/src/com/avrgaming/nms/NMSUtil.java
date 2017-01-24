package com.avrgaming.nms;

import java.lang.reflect.Field;

import net.minecraft.server.v1_8_R3.PathfinderGoalSelector;

import org.bukkit.craftbukkit.v1_8_R3.util.UnsafeList;

public class NMSUtil {

	@SuppressWarnings("rawtypes")
	public static void clearPathfinderGoals(PathfinderGoalSelector selector) {

        Field gsa;
		try {
			gsa = net.minecraft.server.v1_8_R3.PathfinderGoalSelector.class.getDeclaredField("b");
			gsa.setAccessible(true);

			gsa.set(selector, new UnsafeList());
		    
			gsa = net.minecraft.server.v1_8_R3.PathfinderGoalSelector.class.getDeclaredField("c");
			gsa.setAccessible(true);

			gsa.set(selector, new UnsafeList());
		    
		} catch (NoSuchFieldException e) {
			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}
		
	}
	
}
