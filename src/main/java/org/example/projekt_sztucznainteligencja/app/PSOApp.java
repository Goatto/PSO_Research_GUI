package org.example.projekt_sztucznainteligencja.app;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.Objects;

public class PSOApp extends Application
{
    @Override
    public void start(Stage primaryStage) throws Exception
    {
        Parent root = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("/org/example/projekt_sztucznainteligencja/interface.fxml")));
        // 16:9
        Scene scene = new Scene(root, 1024, 576);
        primaryStage.setTitle("Algorytm PSO - GUI");
        primaryStage.setScene(scene);
        primaryStage.show();
    }
}