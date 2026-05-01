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
    @FXML private Spinner<Double> inertiaField;
    @FXML private Spinner<Double> cognitiveField;
    @FXML private Spinner<Double> socialField;
    @FXML private Spinner<Double> optimumField;
    @FXML private Spinner<Integer> particlesAmountField;
    @FXML private Spinner<Integer> epochsField;
    @FXML private Spinner<Integer> precisionField;

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

        fixSpinnerFormatting(inertiaField);
        fixSpinnerFormatting(cognitiveField);
        fixSpinnerFormatting(socialField);
        fixSpinnerFormatting(optimumField);
        fixIntegerSpinnerFormatting(particlesAmountField);
        fixIntegerSpinnerFormatting(epochsField);
        fixIntegerSpinnerFormatting(precisionField);

        spinnerAutoCommit(inertiaField);
        spinnerAutoCommit(cognitiveField);
        spinnerAutoCommit(socialField);
        spinnerAutoCommit(optimumField);
        spinnerAutoCommit(particlesAmountField);
        spinnerAutoCommit(epochsField);
        spinnerAutoCommit(precisionField);
    }

    @FXML
    void runPSOAlgorithm()
    {
        try
        {
            // zebranie wartości z pól
            double w = inertiaField.getValue();
            double c1 = cognitiveField.getValue();
            double c2 = socialField.getValue();
            double optimum = optimumField.getValue();
            int particles = particlesAmountField.getValue();
            int epochs = epochsField.getValue();
            int precision = precisionField.getValue();
            FitnessFunction selectedFunc = functionChoice.getValue();

            // zablokowanie przycisku
            startButton.setDisable(true);
            logArea.clear();
            // wyczyszczenie starych danych
            particleSeries.getData().clear();
            bestParticleSeries.getData().clear();
            particleDataPoints.clear();

            // z góry tworzymy punkty wykresu, zwykle aktualizujemy ich współrzędne
            // znacznie zmniejsza to ilość tworzenia niepotrzebnych zmiennych do usunięcia przez GC
            for(int i = 0; i < particles; i++)
            {
                particleDataPoints.add(new XYChart.Data<>(0, 0));
            }
            particleSeries.getData().setAll(particleDataPoints);

            // inicjalizacja z obecnymi parametrami
            PSOSolver solver = new PSOSolver(
                    w, c1, c2, optimum, particles, epochs, precision,
                    selectedFunc, randomIntertia.isSelected(), randomCognitive.isSelected(), randomSocial.isSelected()
            );

            // modyfikacja elementów JavaFX musi się odbywać tylko w głównym wątku
            solver.setOnLogUpdate(message ->
                    Platform.runLater(() -> logArea.appendText(message))
            );

            solver.setOnChartUpdate(swarm ->
                    Platform.runLater(() -> {
                        // aktualizacja współrzędnych wcześniej wygenerowanych punktów
                        for(int i = 0; i < swarm.length; i++) {
                            particleDataPoints.get(i).setXValue(swarm[i].x);
                            particleDataPoints.get(i).setYValue(swarm[i].y);
                        }
                    })
            );

            solver.setOnBestFound(bestPos ->
                    Platform.runLater(() -> {
                        // określenie globalnego optimum
                        XYChart.Data<Number, Number> bestNode = new XYChart.Data<>(bestPos[0], bestPos[1]);
                        // zmienienie stylu globalnego optimum by było lepiej widoczne
                        bestNode.nodeProperty().addListener((_, _, newNode) -> {
                            if(newNode != null)
                            {
                                // zmiana koloru
                                newNode.setStyle("-fx-background-color: #0000ff; -fx-background-radius: 10px;");
                                // przeniesienie na pierwszą warstwę
                                newNode.toFront();
                            }
                        });
                        bestParticleSeries.getData().add(bestNode);
                    })
            );

            solver.setOnSucceeded(_ -> startButton.setDisable(false));
            // nie wydaje mi się, że nawet może do tego dojść, ale jest na wszelki wypadek
            solver.setOnFailed(_ -> {
                logArea.appendText("BŁĄD WĄTKU: " + solver.getException().getMessage() + "\n");
                startButton.setDisable(false);
            });

            // wykonywanie obliczeń w nowym wątku chroni nas przed zawieszeniem GUI
            // dopiero po przypięciu wszystkich listenerów rozpoczynamy wątek
            new Thread(solver).start();

        }
        catch (NumberFormatException e)
        {
            logArea.appendText("BŁĄD: Nieprawidłowa wartość liczbowa\n");
        }
        catch (Exception e)
        {
            logArea.appendText("BŁĄD: " + e.getMessage() + "\n");
        }
    }

    private <T> void spinnerAutoCommit(Spinner<T> spinner)
    {
        spinner.getEditor().textProperty().addListener((_, _, newValue) -> {
            try
            {
                T value = spinner.getValueFactory().getConverter().fromString(newValue);

                if(value != null)
                {
                    // dopiero po obsłudze klinkięcia aktualizujemy spinner, inaczej otrzymujemy błędy
                    javafx.application.Platform.runLater(() -> {
                        if(!value.equals(spinner.getValue()))
                        {
                            spinner.getValueFactory().setValue(value);
                        }
                    });
                }
            }
            catch (Exception e)
            {
                // ignorujemy błędy np. gdy ktoś wpisze samą kropkę "0." albo "-"
            }
        });
    }

    private void fixSpinnerFormatting(Spinner<Double> spinner)
    {
        spinner.getValueFactory().setConverter(new javafx.util.StringConverter<>()
        {
            @Override
            public String toString(Double value)
            {
                // nie zaokrąglamy wartości
                return value == null ? "" : value.toString();
            }

            @Override
            public Double fromString(String string)
            {
                try
                {
                    if (string == null || string.isEmpty()) return 0.0;
                    string = string.replace(",", ".");
                    return Double.parseDouble(string);
                }
                catch (NumberFormatException e)
                {
                    // zamiast wyrzucać błąd do konsoli, przywracamy ostatnią dobrą wartość
                    return spinner.getValue();
                }
            }
        });

        spinner.getEditor().focusedProperty().addListener((_, _, isFocused) -> {
            if(!isFocused)
            {
                String safeText = spinner.getValueFactory().getConverter().toString(spinner.getValue());
                spinner.getEditor().setText(safeText);
            }
        });
    }

    private void fixIntegerSpinnerFormatting(Spinner<Integer> spinner)
    {
        spinner.getValueFactory().setConverter(new javafx.util.StringConverter<>() {
            @Override
            public String toString(Integer value)
            {
                return value == null ? "" : value.toString();
            }

            @Override
            public Integer fromString(String string)
            {
                try
                {
                    if (string == null || string.isEmpty()) return 0;
                    return Integer.parseInt(string);
                }
                catch(NumberFormatException e)
                {
                    // zamiast wyrzucać błąd do konsoli, przywracamy ostatnią dobrą wartość
                    return spinner.getValue();
                }
            }
        });

        spinner.getEditor().focusedProperty().addListener((_, _, isFocused) -> {
            if(!isFocused)
            {
                String safeText = spinner.getValueFactory().getConverter().toString(spinner.getValue());
                spinner.getEditor().setText(safeText);
            }
        });
    }
}