package org.example.tonpad;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.example.tonpad.ui.controllers.MainController;
import org.example.tonpad.core.files.directory.DirectoryServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;

import java.nio.file.Path;
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

        // DirectoryServiceImpl asd = springContext.getBean(DirectoryServiceImpl.class);

        // asd.renameDir(Path.of("src/main/resources/basePath/test"), "test1");

        // asd.createDir(Path.of("src/main/resources/basePath"), "test");
    }

    @Override
    public void start(Stage stage) {

        Scene scene = new Scene(mainController.getMainVBox(), 900, 600);
        scene.getStylesheets().add(
                Objects.requireNonNull(getClass().getResource("/ui/css/styles.css")).toExternalForm()
        );
        stage.setScene(scene);
        stage.show();

        mainController.postInitialize();
    }

    public static void main(String[] args) {
        Application.launch(TonpadApplication.class, args);
    }

}
