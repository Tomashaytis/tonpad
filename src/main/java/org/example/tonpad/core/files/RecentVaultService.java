package org.example.tonpad.core.files;

import javafx.collections.ObservableList;
import lombok.NonNull;

public interface RecentVaultService {
    ObservableList<String> load();
    void save(@NonNull ObservableList<String> recentVaults);
    void bindAutoSave(@NonNull ObservableList<String> recentVaults);
    void unbindAutoSave(@NonNull ObservableList<String> recentVaults);
    boolean setFirstRecent(@NonNull ObservableList<String> recentVaults, @NonNull String path);
}
