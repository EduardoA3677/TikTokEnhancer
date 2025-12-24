package com.wmods.tkkenhancer.xposed.features.general;

import java.util.HashMap;

/**
 * Shared property maps for TikTok configuration
 * Used by various features that need to modify TikTok app properties
 */
public class Others {
    public static HashMap<Integer, Boolean> propsBoolean = new HashMap<>();
    public static HashMap<Integer, Integer> propsInteger = new HashMap<>();
}
