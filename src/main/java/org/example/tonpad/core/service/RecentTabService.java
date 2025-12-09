package org.example.tonpad.core.service;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

public interface RecentTabService {
    void addOpenedTab(Path notePath);
    void deleteClosedTab(Path notePath);
    void updateLastActive(Path notePath);
    boolean isInRecent(Path notePath);
    void clearLastActive();
    void renamePath(Path oldPath, Path newPath);
    void renameLastActive(Path oldPath, Path newPath);
    List<Optional<Path>> getRecentTabs();
    Optional<Path> getLastActiveTab();
    Path getRelativePath(Path notePath);
    void refreshRtConfig();
    void saveConfig();
    void refreshFingerPrint();
}
