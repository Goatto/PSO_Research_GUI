package org.example.projekt_sztucznainteligencja.controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.util.StringConverter;
import org.example.projekt_sztucznainteligencja.model.ErrorType;
import org.example.projekt_sztucznainteligencja.model.FitnessFunction;
import org.example.projekt_sztucznainteligencja.model.PSOSolver;
import org.example.projekt_sztucznainteligencja.model.Particle;

import java.util.List;

public class Controller
{
    @FXML private ComboBox<ErrorType> errorChoice;
    @FXML private ComboBox<FitnessFunction> functionChoice;
    @FXML private CheckBox gridDistribution;
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

    @FXML private LineChart<Number, Number> errorChart;
    private XYChart.Series<Number, Number> errorSeries;
    @FXML private NumberAxis xAxis;
    @FXML private NumberAxis yAxis;

    @FXML private Button exportButton;
    private Particle[] lastSwarm;



    @FXML
    public void initialize()
    {
        functionChoice.getItems().addAll(FitnessFunction.values());
        functionChoice.setValue(FitnessFunction.ACKLEY);

        errorChoice.getItems().addAll(ErrorType.values());
        errorChoice.setValue(ErrorType.ABSOLUTE);

        errorChart.setLegendVisible(false);
        xAxis.setLabel("Epoka");
        yAxis.setLabel("Błąd");


        yAxis.setTickLabelFormatter(new StringConverter<>()
        {
            @Override
            public String toString(Number value)
            {
                double exp = value.doubleValue();
                return String.format("10^(%.1f)", exp);
            }

            @Override
            public Number fromString(String string)
            {
                return 0;
            }
        });
        errorSeries = new XYChart.Series<>();
        errorChart.getData().add(errorSeries);

        List.of(inertiaField, cognitiveField, socialField, optimumField)
                .forEach(this::fixSpinnerFormatting);

        List.of(particlesAmountField, epochsField, precisionField)
                .forEach(this::fixIntegerSpinnerFormatting);

        List.of(inertiaField, cognitiveField, socialField, optimumField,
                        particlesAmountField, epochsField, precisionField)
                .forEach(this::spinnerAutoCommit);

        functionChoice.getSelectionModel().selectedItemProperty().addListener((_, _, newValue) -> {
            if(newValue != null)
            {
                optimumField.getValueFactory().setValue(newValue.getTargetOptimum());
            }
        });
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
            ErrorType selectedError = errorChoice.getValue();
            // zablokowanie przycisku
            startButton.setDisable(true);
            logArea.clear();
            errorSeries.getData().clear();


            // inicjalizacja z obecnymi parametrami
            PSOSolver solver = new PSOSolver(
                    w, c1, c2, optimum, particles, epochs, precision, selectedError,
                    selectedFunc, randomIntertia.isSelected(), randomCognitive.isSelected(), randomSocial.isSelected(), gridDistribution.isSelected()
            );

            // modyfikacja elementów JavaFX musi się odbywać tylko w głównym wątku
            solver.setOnLogUpdate(message ->
                    Platform.runLater(() -> logArea.appendText(message))
            );

            solver.setOnEpochUpdate((epoch, bestError) ->
                    Platform.runLater(() -> {
                        // Jeśli błąd jest 0, logarytm wybuchnie, więc dajemy małe zabezpieczenie
                        double displayError = bestError > 0 ? Math.log10(bestError) : -15;
                        errorSeries.getData().add(new XYChart.Data<>(epoch, displayError));
                    })
            );

            solver.setOnSucceeded(_ -> {
                startButton.setDisable(false);
                this.lastSwarm = solver.getSwarm(); // Zakładamy, że dodasz getter do PSOSolver
                exportButton.setDisable(false); // Aktywacja guzika eksportu po sukcesie
            });

            // nie wydaje mi się, że nawet może do tego dojść, ale jest na wszelki wypadek
            solver.setOnFailed(_ -> {
                logArea.appendText("BŁĄD WĄTKU: " + solver.getException().getMessage() + "\n");
                startButton.setDisable(false);
            });

            // wykonywanie obliczeń w nowym wątku chroni nas przed zawieszeniem GUI
            // dopiero po przypięciu wszystkich listenerów rozpoczynamy wątek
            new Thread(solver).start();

        }
        catch(NumberFormatException e)
        {
            logArea.appendText("BŁĄD: Nieprawidłowa wartość liczbowa\n");
        }
        catch(Exception e)
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
            catch(Exception e)
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
        spinner.getValueFactory().setConverter(new javafx.util.StringConverter<>()
        {
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

    @FXML
    void exportResults()
    {
        if(lastSwarm == null)
        {
            return;
        }
        // okno wyboru miejsca zapisu
        javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
        fileChooser.setTitle("Zapisz wyniki jako CSV");
        fileChooser.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("Pliki CSV", "*.csv"));
        java.io.File file = fileChooser.showSaveDialog(startButton.getScene().getWindow());

        if(file != null)
        {
            try(java.io.PrintWriter writer = new java.io.PrintWriter(file))
            {
                writer.println("Nr czasteczki;Pozycja X;Pozycja Y;Najlepszy wynik (pBest)");

                for(int i = 0; i < lastSwarm.length; i++)
                {
                    // Locale.GERMANY wymusza przecinek jako separator
                    writer.println(String.format(java.util.Locale.GERMANY, "%d;%.15f;%.15f;%.15f",
                            i + 1, lastSwarm[i].x, lastSwarm[i].y, lastSwarm[i].bestValue));
                }
                logArea.appendText("WYEKSPORTOWANO DANE: " + file.getName() + "\n");
            }
            catch(Exception e)
            {
                logArea.appendText("BŁĄD ZAPISU: " + e.getMessage() + "\n");
            }
        }
    }
}