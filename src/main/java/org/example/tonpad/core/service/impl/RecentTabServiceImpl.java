package org.example.tonpad.core.service.impl;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.example.tonpad.core.exceptions.FingerPrintException;
import org.example.tonpad.core.files.FileSystemService;
import org.example.tonpad.core.service.RecentTabService;
import org.example.tonpad.core.session.RecentTabsConfig;
import org.example.tonpad.core.session.RecentTabsSession;
import org.example.tonpad.core.session.VaultSession;
import org.example.tonpad.ui.extentions.VaultPathsContainer;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RecentTabServiceImpl implements RecentTabService {
    private final static String RELATIVIZE_ERROR = "%s is not a path for %s";

    private RecentTabsConfig recentTabsConfig = new RecentTabsConfig();

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final VaultSession vaultSession;

    private final VaultPathsContainer vaultPathsContainer;

    private final FileSystemService fileSystemService;

    private String fingerPrint;

    private boolean renaming = false;

    @Override
    public void saveConfig() {
        if (recentTabsConfig.getSessions() == null) {
            recentTabsConfig.setSessions(new HashMap<>());
        }
        String json = toJson();
        fileSystemService.writeFile(getRtConfPath(), json);
    }

    @Override
    public void refreshRtConfig() {
        String json = fileSystemService.readFile(getRtConfPath());
        loadFromJson(json);
        if (recentTabsConfig.getSessions() == null) {
            recentTabsConfig.setSessions(new HashMap<>());
        }
        this.fingerPrint = null;
    }

    @Override
    public void renamePath(Path oldPath, Path newPath) {
        renaming = true;
        Path oldRel = getRelativePath(oldPath);
        Path newRel = getRelativePath(newPath);

        RecentTabsSession session = getSession();

        if (session.getTabs().remove(oldRel.toString())) {
            session.getTabs().add(newRel.toString());
        }

        String last = session.getLastActiveTab();
        if (last != null && last.equals(oldRel.toString())) {
            session.setLastActiveTab(newRel.toString());
        }
        saveConfig();
        renaming = false;
    }

    @Override
    public void renameLastActive(Path oldPath, Path newPath) {
        Path oldRel = getRelativePath(oldPath);
        Path newRel = getRelativePath(newPath);

        RecentTabsSession session = getSession();
        if (oldRel.toString().equals(session.getLastActiveTab())) {
            session.setLastActiveTab(newRel.toString());
            saveConfig();
        }
    }

    @Override
    public Optional<Path> getLastActiveTab() {
        RecentTabsSession session = getSession();
        String last = session.getLastActiveTab();
        if (last == null || last.isBlank()) return Optional.empty();
        return Optional.of(vaultPathsContainer.getVaultPath().resolve(last));
    }

    @Override
    public List<Optional<Path>> getRecentTabs() {
        RecentTabsSession session = getSession();
        return session.getTabs()
                        .stream()
                        .map(t -> {
                            return (t == null || t.isBlank()) 
                                ? Optional.<Path>empty() 
                                : Optional.of(vaultPathsContainer.getVaultPath().resolve(t));
                        }).toList();
    }

    @Override
    public void updateLastActive(Path notePath) {
        Path path = getRelativePath(notePath);
        RecentTabsSession session = getSession();
        session.setLastActiveTab(path.toString());
        saveConfig();
    }

    @Override
    public void clearLastActive() {
        RecentTabsSession session = getSession();
        session.setLastActiveTab(null);
        saveConfig();
    }

    @Override
    public void addOpenedTab(Path notePath) {
        Path path = getRelativePath(notePath);
        RecentTabsSession session = getSession();
        session.getTabs().add(path.toString());
        saveConfig();
    }

    @Override
    public void deleteClosedTab(Path notePath) {
        Path path = getRelativePath(notePath);
        RecentTabsSession session = getSession();
        session.getTabs().remove(path.toString());
        if (!renaming && path.toString().equals(session.getLastActiveTab())) {
            session.setLastActiveTab(null);
        }
        saveConfig();
    }

    @Override
    public boolean isInRecent(Path notePath) {
        Path path = getRelativePath(notePath);
        RecentTabsSession session = getSession();
        if (session.getTabs().contains(path.toString())) return true;
        return false;
    }

    @Override
    public Path getRelativePath(Path notePath) {
        Path vaultPath = vaultPathsContainer.getVaultPath();
        try {
            return vaultPath.relativize(notePath);
        }
        catch (Exception ex) {
            throw new IllegalArgumentException(String.format(RELATIVIZE_ERROR, notePath.toString(), vaultPath.toString()));
        }
    }

    private void loadFromJson(String json) {
        if (json == null || json.isBlank()) {
            this.recentTabsConfig = new RecentTabsConfig();
            return;
        }
        try {
            this.recentTabsConfig = objectMapper.readValue(json, RecentTabsConfig.class);
        } catch (Exception e) {
            this.recentTabsConfig = new RecentTabsConfig();
        }
    }

    private String toJson() {
        try {
            return objectMapper.writeValueAsString(recentTabsConfig);
        } catch (Exception e) {
            return "{}";
        }
    }

    private Path getRtConfPath() {
        return vaultPathsContainer.getVaultPath().resolve(RecentTabsConfig.getRtConfigName());
    }

    private String getFingerPrint() {
        try {
            if (this.fingerPrint == null || this.fingerPrint.isBlank()) {
                this.fingerPrint = vaultSession.getFingerPrint();
            }
            return this.fingerPrint;
        }
        catch (FingerPrintException ex) {
            throw ex;
        }
    }

    @Override
    public void refreshFingerPrint() {
        String oldFp = this.fingerPrint;
        String newFp = vaultSession.getFingerPrint();
        Map<String, RecentTabsSession> sessions = recentTabsConfig.getSessions();
        RecentTabsSession session = sessions.get(oldFp);
        sessions.remove(oldFp);
        sessions.put(newFp, session);
        this.fingerPrint = newFp;
        saveConfig();
    }

    private RecentTabsSession getSession() {
        String fingerprint = getFingerPrint();
        RecentTabsSession session = getSessionByFingerprint(fingerprint);
        if (session == null) {
            session = new RecentTabsSession();
            recentTabsConfig.getSessions().put(fingerprint, session);
        }
        return session;
    }

    private RecentTabsSession getSessionByFingerprint(String fp) {
        return recentTabsConfig.getSessions().get(fp);
    }
}
