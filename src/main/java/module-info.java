module org.example.projekt_sztucznainteligencja {
    requires javafx.controls;
    requires javafx.fxml;


    opens org.example.projekt_sztucznainteligencja to javafx.fxml;
    exports org.example.projekt_sztucznainteligencja;
}