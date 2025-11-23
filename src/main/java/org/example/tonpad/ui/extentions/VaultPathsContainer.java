package org.example.tonpad.ui.extentions;

import lombok.Getter;
import org.springframework.stereotype.Component;

import java.nio.file.Path;

@Component
@Getter
public class VaultPathsContainer {

    private final static String NOTES_DIR_NAME = "notes";

    private final static String SNIPPETS_DIR_NAME = "snippets";

    private Path vaultPath;

    private Path notesPath;

    private Path snippetsPath;

    public void setVaultPath(Path vaultPath) {
        Path newNotesPath = vaultPath.resolve(NOTES_DIR_NAME);
        Path newSnippetsPath = vaultPath.resolve(SNIPPETS_DIR_NAME);

        this.vaultPath = vaultPath;
        this.notesPath = newNotesPath;
        this.snippetsPath = newSnippetsPath;
    }

    public void setVaultPath(String vaultPath) {
        setVaultPath(Path.of(vaultPath));
    }

}
