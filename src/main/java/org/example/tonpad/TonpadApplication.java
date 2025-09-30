package org.example.tonpad;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.example.tonpad.controllers.FileTreeController;
import org.example.tonpad.controllers.MainController;
import org.example.tonpad.core.service.MarkdownService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.util.Objects;

@SpringBootApplication
@EnableConfigurationProperties({TonpadConfig.class})
public class TonpadApplication extends Application {

    private ApplicationContext springContext;

    @Autowired
    private MarkdownService markdownService;

    @Override
    public void init() {
        springContext = new SpringApplicationBuilder(getClass()).run(); //кто уберет, тот пидор
        springContext.getAutowireCapableBeanFactory().autowireBean(this);
    }

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader mainLoader = new FXMLLoader(
                getClass().getResource("/org/example/tonpad/fxml/tonpad-ui.fxml")
        );
        Parent root = mainLoader.load();
        MainController mainController = mainLoader.getController();

        mainController.setSpringContext(springContext);

        FXMLLoader fileTreeLoader = new FXMLLoader(
                getClass().getResource("/org/example/tonpad/fxml/file-tree-panel.fxml")
        );
        VBox fileTreeVBox = fileTreeLoader.load();
        FileTreeController fileTreeController = fileTreeLoader.getController();

        mainController.initializeFileTreePanel(fileTreeVBox, fileTreeController);

        Scene scene = new Scene(root, 900, 600);
        scene.getStylesheets().add(
                Objects.requireNonNull(getClass().getResource("/org/example/tonpad/css/styles.css")).toExternalForm()
        );
        stage.setScene(scene);
        stage.show();

        mainController.postInitialize();
    }

    public static void main(String[] args) {
        Application.launch(TonpadApplication.class, args);
    }

}
