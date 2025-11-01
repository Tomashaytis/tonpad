package org.example.tonpad.core.files;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.io.Reader;
import java.io.Writer;
import java.io.IOException;

import org.example.tonpad.core.files.RecentVaultService;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;

import lombok.extern.slf4j.Slf4j;

import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import lombok.NonNull;

@Slf4j
@Service
public class RecentVaultServiceImpl implements RecentVaultService {
    private final static String RECENTS_YAML_READ_ERROR = "error while reading config with recent vaults";
    private final static String RECENTS_YAML_WRITE_ERROR = "error while writing config with recent vaults";
    private final static String READING_ERROR = "error while reading some file";

    private final Path filePath;
    private final int maxItems;

    private ListChangeListener<String> autosaveListener;

    public RecentVaultServiceImpl() {
        this(defaultConfigPath(), 15);
    }

    public RecentVaultServiceImpl(@NonNull Path filePath, int maxItems) {
        this.filePath = filePath;
        this.maxItems = Math.max(1, maxItems);
    }

    @Override
    public ObservableList<String> load() {
        if(!Files.exists(this.filePath)) return FXCollections.observableArrayList();
        try(Reader r = Files.newBufferedReader(this.filePath, StandardCharsets.UTF_8)) {
            LoaderOptions lo = new LoaderOptions();
            Yaml yaml = new Yaml(lo);
            Object data = yaml.load(r);
            List<String> items = new ArrayList<>();
            if(data instanceof List<?> raw) {
                for(Object o: raw) {
                    if(o != null) items.add(o.toString());
                }
            }
            if(items.size() > maxItems) {
                items = items.subList(0, maxItems);
            }
            return FXCollections.observableArrayList(items);
        }
        catch(IOException e) {
            log.info(RECENTS_YAML_READ_ERROR);
            return FXCollections.observableArrayList();
        }
    }

    @Override
    public void save(@NonNull ObservableList<String> recentVaults) {
        ensureParentDir();
        List<String> toWrite = new ArrayList<>(recentVaults);
        if(toWrite.size() > maxItems) {
            toWrite = toWrite.subList(0, maxItems);
        }
        DumperOptions opts = new DumperOptions();
        opts.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        opts.setPrettyFlow(true);
        Yaml yaml = new Yaml(opts);
        try (Writer w = Files.newBufferedWriter(filePath, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {
            yaml.dump(toWrite, w);
        }
        catch(IOException e) {
            log.info(RECENTS_YAML_WRITE_ERROR);
        } 
    }

    @Override
    public void bindAutoSave(@NonNull ObservableList<String> recentVaults) {
        unbindAutoSave(recentVaults);
        autosaveListener = change -> save(recentVaults);
        recentVaults.addListener(autosaveListener);
    }

    @Override
    public void unbindAutoSave(@NonNull ObservableList<String> recentVaults) {
        if(autosaveListener != null) {
            recentVaults.removeListener(autosaveListener);
            autosaveListener = null;
        }
    }

    @Override
    public boolean setFirstRecent(@NonNull ObservableList<String> recentVaults, @NonNull String path) {
        boolean changed = recentVaults.remove(path);
        recentVaults.add(0, path);
        if(recentVaults.size() > maxItems) {
            recentVaults.remove(maxItems, recentVaults.size());
            changed = true;
        }
        save(recentVaults);
        return changed;
    }

    public static Path defaultConfigPath() {
        String os = System.getProperty("os.name").toLowerCase();
        String home = System.getProperty("user.home");

        if (os.contains("win")) {
            String appData = System.getenv("APPDATA");
            return Path.of(appData != null ? appData : home, "Tonpad", "recent-vaults.yml");
        } else if (os.contains("mac")) {
            return Path.of(home, "Library", "Application Support", "Tonpad", "recent-vaults.yml");
        } else {
            String xdg = System.getenv("XDG_CONFIG_HOME");
            Path base = xdg != null && !xdg.isBlank() ? Path.of(xdg) : Path.of(home, ".config");
            return base.resolve(Path.of("tonpad", "recent-vaults.yml"));
        }
    }

    private void ensureParentDir() {
        try {
            Path dir = filePath.getParent();
            if(dir != null && !Files.exists(dir)) {
                Files.createDirectories(dir);
            }
        }
        catch(IOException ignored) {
            log.info(READING_ERROR);
        }
    }
}
