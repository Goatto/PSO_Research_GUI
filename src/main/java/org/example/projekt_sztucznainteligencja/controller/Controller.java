package org.example.projekt_sztucznainteligencja.controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.ScatterChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import org.example.projekt_sztucznainteligencja.model.FitnessFunction;
import org.example.projekt_sztucznainteligencja.model.PSOSolver;

import java.util.ArrayList;
import java.util.List;

// TODO KOMENTARZE
public class Controller
{
    @FXML private ComboBox<FitnessFunction> functionChoice;
    @FXML private TextField inertiaField;
    @FXML private TextField cognitiveField;
    @FXML private TextField socialField;
    @FXML private TextField optimumField;
    @FXML private TextField particlesAmountField;
    @FXML private TextField epochsField;
    @FXML private TextField precisionField;

    @FXML private CheckBox randomIntertia;
    @FXML private CheckBox randomCognitive;
    @FXML private CheckBox randomSocial;

    @FXML private Button startButton;
    @FXML private TextArea logArea;

    @FXML private ScatterChart<Number, Number> particleChart;
    @FXML private NumberAxis xAxis;
    @FXML private NumberAxis yAxis;

    private XYChart.Series<Number, Number> particleSeries;
    private XYChart.Series<Number, Number> bestParticleSeries;
    private List<XYChart.Data<Number, Number>> particleDataPoints;

    @FXML
    public void initialize()
    {
        functionChoice.getItems().addAll(FitnessFunction.values());
        functionChoice.setValue(FitnessFunction.ACKLEY);

        particleChart.setLegendVisible(false);
        xAxis.setLabel("Oś X");
        yAxis.setLabel("Oś Y");

        xAxis.setAutoRanging(false);
        xAxis.setLowerBound(-15);
        xAxis.setUpperBound(15);
        xAxis.setTickUnit(5);

        yAxis.setAutoRanging(false);
        yAxis.setLowerBound(-15);
        yAxis.setUpperBound(15);
        yAxis.setTickUnit(5);

        particleSeries = new XYChart.Series<>();
        bestParticleSeries = new XYChart.Series<>();
        particleDataPoints = new ArrayList<>();

        particleChart.getData().add(particleSeries);
        particleChart.getData().add(bestParticleSeries);
    }

    @FXML
    void runPSOAlgorithm()
    {
        try
        {
            double w = Double.parseDouble(inertiaField.getText());
            double c1 = Double.parseDouble(cognitiveField.getText());
            double c2 = Double.parseDouble(socialField.getText());
            double optimum = Double.parseDouble(optimumField.getText());
            int particles = Integer.parseInt(particlesAmountField.getText());
            int epochs = Integer.parseInt(epochsField.getText());
            int precision = Integer.parseInt(precisionField.getText());
            FitnessFunction selectedFunc = functionChoice.getValue();

            if (selectedFunc == null) throw new IllegalArgumentException("Nie wybrano funkcji!");

            startButton.setDisable(true);
            logArea.clear();
            particleSeries.getData().clear();
            bestParticleSeries.getData().clear();
            particleDataPoints.clear();

            // Przygotowanie punktów wykresu z góry
            for(int i = 0; i < particles; i++) {
                particleDataPoints.add(new XYChart.Data<>(0, 0));
            }
            particleSeries.getData().setAll(particleDataPoints);

            PSOSolver solver = new PSOSolver(
                    w, c1, c2, optimum, particles, epochs, precision,
                    selectedFunc, randomIntertia.isSelected(), randomCognitive.isSelected(), randomSocial.isSelected()
            );

            solver.setOnLogUpdate(message ->
                    Platform.runLater(() -> logArea.appendText(message))
            );

            solver.setOnChartUpdate(swarm ->
                    Platform.runLater(() -> {
                        for(int i = 0; i < swarm.length; i++) {
                            particleDataPoints.get(i).setXValue(swarm[i].x);
                            particleDataPoints.get(i).setYValue(swarm[i].y);
                        }
                    })
            );

            solver.setOnBestFound(bestPos ->
                    Platform.runLater(() -> {
                        XYChart.Data<Number, Number> bestNode = new XYChart.Data<>(bestPos[0], bestPos[1]);
                        bestNode.nodeProperty().addListener((obs, oldNode, newNode) -> {
                            if(newNode != null) {
                                newNode.setStyle("-fx-background-color: #0000ff; -fx-background-radius: 10px;");
                                newNode.toFront();
                            }
                        });
                        bestParticleSeries.getData().add(bestNode);
                    })
            );

            solver.setOnSucceeded(e -> startButton.setDisable(false));
            solver.setOnFailed(e -> {
                logArea.appendText("BŁĄD WĄTKU: " + solver.getException().getMessage() + "\n");
                startButton.setDisable(false);
            });

            new Thread(solver).start();

        }
        catch (NumberFormatException e)
        {
            logArea.appendText("BŁĄD: Wprowadzono nieprawidłowe wartości liczbowe!\n");
        }
        catch (Exception e)
        {
            logArea.appendText("BŁĄD: " + e.getMessage() + "\n");
        }
    }
}