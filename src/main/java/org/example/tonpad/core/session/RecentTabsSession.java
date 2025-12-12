package org.example.tonpad.core.session;

import java.util.LinkedHashSet;
import java.util.Set;

import lombok.Getter;
import lombok.Setter;

public class RecentTabsSession {
    @Getter
    @Setter
    private Set<String> tabs = new LinkedHashSet<>();
    @Getter
    @Setter
    private String lastActiveTab;
}
