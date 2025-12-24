package com.wmods.tkkenhancer.xposed.features.general;

import java.util.HashMap;

/**
 * Minimal class to provide shared property maps for TikTok configuration
 * Used by MediaQuality and potentially other features
 */
public class Others {
    public static HashMap<Integer, Boolean> propsBoolean = new HashMap<>();
    public static HashMap<Integer, Integer> propsInteger = new HashMap<>();
}
