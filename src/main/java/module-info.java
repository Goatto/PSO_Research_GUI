module org.example.projekt_sztucznainteligencja {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.desktop;
    requires javafx.swing;


    opens org.example.projekt_sztucznainteligencja to javafx.fxml;
    exports org.example.projekt_sztucznainteligencja.app;
    opens org.example.projekt_sztucznainteligencja.app to javafx.fxml;
    exports org.example.projekt_sztucznainteligencja.controller;
    opens org.example.projekt_sztucznainteligencja.controller to javafx.fxml;
}