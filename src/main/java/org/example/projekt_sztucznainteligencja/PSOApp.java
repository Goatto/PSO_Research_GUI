package org.example.projekt_sztucznainteligencja;

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
        Parent root = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("interface.fxml")));
        Scene scene = new Scene(root, 1024, 576);
        primaryStage.setTitle("Algorytm PSO - GUI");
        primaryStage.setScene(scene);
        primaryStage.show();
    }
}