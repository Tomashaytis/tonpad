package org.example.tonpad;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.example.tonpad.ui.controllers.MainController;
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

    @Override
    public void init() {
        springContext = new SpringApplicationBuilder(getClass()).headless(false).run();
        springContext.getAutowireCapableBeanFactory().autowireBean(this);
    }

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader mainLoader = loadFxml("/ui/fxml/tonpad-ui.fxml");
        Parent root = mainLoader.load();
        MainController mainController = mainLoader.getController();

        FXMLLoader fileTreeLoader = loadFxml("/ui/fxml/file-tree-panel.fxml");
        VBox fileTreeVBox = fileTreeLoader.load();
        mainController.initializeFileTreePanel(fileTreeVBox);

        FXMLLoader searchInTextLoader = loadFxml("/ui/fxml/search-bar.fxml");
        VBox searchBarVBox = searchInTextLoader.load();
        mainController.initializeSearchInTextPanel(searchBarVBox);

        Scene scene = new Scene(root, 900, 600);
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

    private FXMLLoader loadFxml(String filePath) {
        FXMLLoader loader = new FXMLLoader(
                Objects.requireNonNull(getClass().getResource(filePath))
        );
        loader.setControllerFactory(springContext::getBean);
        return loader;
    }
}
