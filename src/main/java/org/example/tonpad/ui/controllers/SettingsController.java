package org.example.tonpad.ui.controllers;

import java.nio.file.Path;
import java.util.function.Consumer;

import org.example.tonpad.core.exceptions.CustomIOException;
import org.example.tonpad.core.files.CryptoFileService;
import org.example.tonpad.core.files.FileSystemService;
import org.example.tonpad.core.service.crypto.DerivationService;
import org.example.tonpad.core.service.crypto.exception.DerivationException;
import org.example.tonpad.core.session.VaultSession;
import org.example.tonpad.ui.extentions.VaultPath;
import org.example.tonpad.ui.service.ThemeService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import javafx.fxml.FXML;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.Window;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;

@Component
@Slf4j
@RequiredArgsConstructor
public class SettingsController extends AbstractController {

    private final ObjectProvider<ResetPasswordController> resetPasswordProvider;

    @FXML
    private VBox settingsPanel;

    @FXML
    private Button closeButton;

    @FXML
    private ChoiceBox<String> themeChoice;

    @FXML
    private Button changePasswordButton;

    private final ThemeService themeService;

    private final CryptoFileService cryptoFileService;
    private final DerivationService derivationService;
    private final VaultSession vaultSession;
    private final VaultPath vaultPath;

    private static final double OFFSET = 12.0;

    private AnchorPane host;
    private boolean isShowing = false;

    public void init(AnchorPane hostPane) {
            this.host = hostPane;
        ensureLoaded();

        settingsPanel.setManaged(false);
        settingsPanel.setVisible(false);
        settingsPanel.setTranslateX(offscreenX());

        closeButton.setOnAction(e -> hide());

        setupThemeChoice();

        if(changePasswordButton != null) changePasswordButton.setOnAction(e -> onChangePassword());
    }

    private void onChangePassword() {
        Stage stage = findOwnerStage();
        ResetPasswordController dlg = resetPasswordProvider.getObject();
        dlg.showModal(stage,
        pwd -> { // смена пароля. char[] pwd
            // все .enc перешифрует. Либо все файлы зашифрует в файлы .enc
            try {
                final boolean wasNoPwd = vaultSession.isOpendWithNoPassword();
                final boolean wasWithKey = vaultSession.isProtectionEnabled();

                log.info("[SET-PWD] session state before: unlocked={}, withKey={}, noPwd={}",
                        vaultSession.isUnlocked(), wasWithKey, wasNoPwd);

                byte[] oldKeyOrNull = vaultSession.getKeyIfPresent()
                        .map(k -> k.getEncoded())
                        .orElse(null);

                byte[] newKey = derivationService.deriveAuthHash(pwd);
                log.info("[SET-PWD] derive newKey ok, oldKeyPresent={}, root='{}'",
                        oldKeyOrNull != null, vaultPath.getVaultPath());

                cryptoFileService.reEncryptFiles(oldKeyOrNull, newKey, Path.of(vaultPath.getVaultPath()));

                // Переводим сессию в режим с паролем:
                vaultSession.lock();
                vaultSession.unlock(pwd);
                log.info("[SET-PWD] session state after: unlocked={}, withKey={}, noPwd={}",
                        vaultSession.isUnlocked(), vaultSession.isProtectionEnabled(), vaultSession.isOpendWithNoPassword());
            }
            catch (DerivationException e) {
                log.info("[SET-PWD] derivation failed: {}", e.toString());
            }
            catch (Exception e) {
                log.info("[SET-PWD] unexpected error: {}", e.toString());
            }
        },
        () -> { // сброс пароля. Все расшифровать. Все файлы с .enc перейдут в .dec
            try {
                final boolean wasNoPwd = vaultSession.isOpendWithNoPassword();
                final boolean wasWithKey = vaultSession.isProtectionEnabled();
                log.info("[RESET-PWD] session state before: unlocked={}, withKey={}, noPwd={}",
                        vaultSession.isUnlocked(), wasWithKey, wasNoPwd);

                byte[] oldKey = vaultSession.getKeyIfPresent()
                        .map(k -> k.getEncoded())
                        .orElse(null);

                if (oldKey == null) {
                    log.info("[RESET-PWD] no key present -> nothing to decrypt");
                } else {
                    cryptoFileService.decryptFiles(oldKey, Path.of(vaultPath.getVaultPath()));
                }

                vaultSession.lock();
                vaultSession.openWithoutPassword();
                log.info("[RESET-PWD] session state after: unlocked={}, withKey={}, noPwd={}",
                        vaultSession.isUnlocked(), vaultSession.isProtectionEnabled(), vaultSession.isOpendWithNoPassword());
            }
            catch (Exception e) {
                log.info("[RESET-PWD] unexpected error: {}", e.toString());
            }
        });
    }

    private Stage findOwnerStage() {
        Scene scene = settingsPanel.getScene();
        if(scene != null) {
            Window window = scene.getWindow();
            if(window instanceof Stage stage) return stage;
        }
        if(host != null && host.getScene() != null) {
            Window window = host.getScene().getWindow();
            if(window instanceof Stage stage) return stage;
        }
        return null;
    }

    private void setupThemeChoice() {
        themeChoice.getItems().setAll("Light", "Dark");
        if(themeChoice.getValue() == null) themeChoice.setValue("Light");
        themeChoice.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
            if(newV == null) return;
            Scene scene = settingsPanel.getScene();
            if(scene == null) return;
            if("Light".equalsIgnoreCase(newV)) {
                themeService.apply(scene, ThemeService.Theme.LIGHT);
            }
            else themeService.apply(scene, ThemeService.Theme.DARK);
        });

        Scene scene = settingsPanel.getScene();
        if(scene != null) themeService.apply(scene, ThemeService.Theme.LIGHT);
    }

    public void toggle() { if (isShowing) hide(); else show(); }

    public void show() {
        if (isShowing) return;
        ensureLoaded();
        settingsPanel.setTranslateX(0);
        settingsPanel.setVisible(true);
        settingsPanel.setManaged(true);
        isShowing = true;
    }

    public void hide() {
        if (!isShowing) return;
        ensureLoaded();
        settingsPanel.setTranslateX(offscreenX());
        settingsPanel.setVisible(false);
        settingsPanel.setManaged(false);
        isShowing = false;
    }

    private double offscreenX() {
        return getPanelWidth() + OFFSET;
    }

    private double getPanelWidth() {
        double w = settingsPanel.getWidth();
        if (w <= 0) w = settingsPanel.getPrefWidth() > 0 ? settingsPanel.getPrefWidth() : 360.0;
        return w;
    }

    private void ensureLoaded() {
        if (settingsPanel == null) {
            throw new CustomIOException("settings panel is not loaded (FXML)");
        }
        if (settingsPanel.getParent() == null) {
            AnchorPane.setTopAnchor(settingsPanel, 0.0);
            AnchorPane.setRightAnchor(settingsPanel, 0.0);
            AnchorPane.setBottomAnchor(settingsPanel, 0.0);
            host.getChildren().add(settingsPanel);
        }
    }

    @Override
    protected String getFxmlSource() {
        return "/ui/fxml/settings-panel.fxml";
    }
}
