package org.example.tonpad.core.dto;

import java.util.HashMap;
import java.util.Map;

import lombok.Getter;
import lombok.Setter;

public class RecentTabsConfig {
    private static final String RECENT_TAB_CONFIG = "rtconfig.json";

    @Getter
    @Setter
    private Map<String, RecentTabsSession> sessions = new HashMap<>();

    public static String getRtConfigName() {return RECENT_TAB_CONFIG;}
}
