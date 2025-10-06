package org.example.tonpad;

import javafx.application.Application;
import javafx.stage.Stage;
import lombok.RequiredArgsConstructor;
import org.example.tonpad.ui.controllers.MainController;
import org.example.tonpad.ui.controllers.QuickStartDialogController;
import org.example.tonpad.ui.extentions.VaultPath;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;

@RequiredArgsConstructor
@SpringBootApplication
@EnableConfigurationProperties({TonpadConfig.class})
public class TonpadApplication extends Application {

    private ApplicationContext springContext;

    @Autowired
    private MainController mainController;

    @Autowired
    private VaultPath vaultPath;

    @Autowired
    private QuickStartDialogController quickStartDialogController;

    @Override
    public void init() {
        springContext = new SpringApplicationBuilder(getClass()).run();
        springContext.getAutowireCapableBeanFactory().autowireBean(this);
    }

    @Override
    public void start(Stage primaryStage) {
        quickStartDialogController.init();
        quickStartDialogController.setCreateVaultHandler(selectedPath -> {
            quickStartDialogController.close();

            vaultPath.setVaultPath(selectedPath);
            mainController.init(primaryStage);
        });
    }

    public static void main(String[] args) {
        Application.launch(TonpadApplication.class, args);
    }

}
