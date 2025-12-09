package org.example.tonpad;

import javafx.application.Application;
import javafx.stage.Stage;
import lombok.RequiredArgsConstructor;
import org.example.tonpad.core.exceptions.handler.GlobalExceptionHandler;
import org.example.tonpad.ui.controllers.tree.FileTreeController;
import org.example.tonpad.ui.controllers.core.MainController;
import org.example.tonpad.ui.controllers.dialog.QuickStartDialogController;
import org.example.tonpad.ui.controllers.core.TabController;
import org.example.tonpad.ui.extentions.VaultPathsContainer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;


@RequiredArgsConstructor
@SpringBootApplication
@EnableConfigurationProperties({TonpadConfig.class})
public class TonpadApplication extends Application {

    @Autowired
    private GlobalExceptionHandler globalExceptionHandler;

    @Autowired
    private MainController mainController;

    @Autowired
    private VaultPathsContainer vaultPathsContainer;

    @Autowired
    private QuickStartDialogController quickStartDialogController;

    @Autowired
    private FileTreeController fileTreeController;

    @Autowired
    private TabController tabController;

    private boolean initialized = false;

    @Override
    public void init() {
        ApplicationContext springContext = new SpringApplicationBuilder(getClass()).run();
        springContext.getAutowireCapableBeanFactory().autowireBean(this);
    }

    @Override
    public void start(Stage primaryStage) {
        Thread.setDefaultUncaughtExceptionHandler(globalExceptionHandler);

        quickStartDialogController.init();
        quickStartDialogController.setCreateVaultHandler(selectedPath -> {
            quickStartDialogController.hide();

            vaultPathsContainer.setVaultPath(selectedPath);

            if (!initialized) {
                mainController.init(primaryStage);
                initialized = true;
            } else {
                tabController.clearAllTabs();
            }

            fileTreeController.refreshTree();

            tabController.refreshRtConfig();
            tabController.restoreRecentTabs();
        });
    }

    public static void main(String[] args) {
        Application.launch(TonpadApplication.class, args);
    }

}
