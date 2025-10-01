package org.example.tonpad;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.example.tonpad.ui.controllers.MainController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;

import java.util.Objects;

@SpringBootApplication
@EnableConfigurationProperties({TonpadConfig.class})
public class TonpadApplication extends Application {

    private ApplicationContext springContext;

    @Autowired
    private MainController mainController;

    @Override
    public void init() {
        springContext = new SpringApplicationBuilder(getClass()).run();
        springContext.getAutowireCapableBeanFactory().autowireBean(this);
    }

    @Override
    public void start(Stage stage) {

        Scene scene = new Scene(mainController.getMainVBox(), 900, 600);
        scene.getStylesheets().add(
                Objects.requireNonNull(getClass().getResource("/ui/css/base.css")).toExternalForm()
        );
        stage.setScene(scene);
        stage.show();

        mainController.init();
    }

    public static void main(String[] args) {
        Application.launch(TonpadApplication.class, args);
    }

}
