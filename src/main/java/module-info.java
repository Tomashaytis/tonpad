module tonpad {
    requires javafx.controls;
    requires javafx.graphics;
    requires spring.boot.autoconfigure;
    requires spring.boot;
    requires spring.context;

    opens org.example.tonpad to javafx.fxml;
    exports org.example.tonpad;
}