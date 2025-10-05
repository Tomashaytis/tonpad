package org.example.tonpad;

import javafx.application.Application;
import javafx.stage.Stage;
import org.example.tonpad.ui.controllers.MainController;
import org.example.tonpad.ui.controllers.QuickStartDialogController;
import org.example.tonpad.ui.controllers.TestFieldController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;

import java.io.IOException;


@SpringBootApplication
@EnableConfigurationProperties({TonpadConfig.class})
public class TonpadApplication extends Application {

    private ApplicationContext springContext;

    @Autowired
    private MainController mainController;

    @Autowired
    private QuickStartDialogController quickStartDialogController;

    @Autowired
    private TestFieldController testFieldController;

    @Override
    public void init() {
        springContext = new SpringApplicationBuilder(getClass()).run();
        springContext.getAutowireCapableBeanFactory().autowireBean(this);
    }

    @Override
    public void start(Stage primaryStage) {
        //testFieldController.init(primaryStage);
        quickStartDialogController.init();
        quickStartDialogController.setCreateVaultHandler(selectedPath -> {
            quickStartDialogController.close();

            mainController.setVaultPath(selectedPath);
            mainController.init(primaryStage);
        });
    }

    public static void main(String[] args) {
        Application.launch(TonpadApplication.class, args);
    }

}
