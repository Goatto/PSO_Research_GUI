package org.example.projekt_sztucznainteligencja.controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.image.WritableImage;
import javafx.stage.FileChooser;
import javafx.util.StringConverter;
import org.example.projekt_sztucznainteligencja.model.ErrorType;
import org.example.projekt_sztucznainteligencja.model.FitnessFunction;
import org.example.projekt_sztucznainteligencja.model.PSOSolver;
import org.example.projekt_sztucznainteligencja.model.Particle;

import javax.imageio.ImageIO;
import javafx.embed.swing.SwingFXUtils;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

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
    @FXML private NumberAxis xAxis;
    @FXML private NumberAxis yAxis;
    private XYChart.Series<Number, Number> currentSeries;

    @FXML private Button exportButton;
    private Particle[] lastSwarm;

    private final List<List<Double>> runsErrorHistory = new ArrayList<>();
    private List<Double> currentRunErrors;

    final static int savedAttemptLimit = 5;

    @FXML
    public void initialize()
    {
        functionChoice.getItems().addAll(FitnessFunction.values());
        functionChoice.setValue(FitnessFunction.ACKLEY);

        errorChoice.getItems().addAll(ErrorType.values());
        errorChoice.setValue(ErrorType.ABSOLUTE);

        errorChart.setCreateSymbols(false);
        xAxis.setLabel("Epoka");
        yAxis.setLabel("Błąd");

        yAxis.setTickLabelFormatter(createLogScaleFormatter());

        List.of(inertiaField, cognitiveField, socialField, optimumField).forEach(s -> {
            fixSpinnerFormatting(s, Double::parseDouble);
            spinnerAutoCommit(s);
        });

        List.of(particlesAmountField, epochsField, precisionField).forEach(s -> {
            fixSpinnerFormatting(s, Integer::parseInt);
            spinnerAutoCommit(s);
        });

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

            if (runsErrorHistory.isEmpty())
            {
                errorChart.getData().clear();
            }

            // nowa seria dla tego konkretnego uruchomienia
            currentSeries = new XYChart.Series<>();
            currentSeries.setName("Próba " + (runsErrorHistory.size() + 1));
            errorChart.getData().add(currentSeries);

            // zablokowanie przycisku
            startButton.setDisable(true);
            logArea.clear();
            currentRunErrors = new ArrayList<>();

            PSOSolver solver = new PSOSolver(
                    w, c1, c2, optimum, particles, epochs, precision, selectedError,
                    selectedFunc, randomIntertia.isSelected(), randomCognitive.isSelected(),
                    randomSocial.isSelected(), gridDistribution.isSelected()
            );

            solver.setOnLogUpdate(message -> Platform.runLater(() -> logArea.appendText(message)));

            solver.setOnEpochUpdate((epoch, bestError) -> Platform.runLater(() -> {
                currentRunErrors.add(bestError);
                double displayError = bestError > 0 ? Math.log10(bestError) : -15;
                currentSeries.getData().add(new XYChart.Data<>(epoch, displayError));
            }));

            solver.setOnSucceeded(_ -> Platform.runLater(() -> {
                startButton.setDisable(false);
                this.lastSwarm = solver.getSwarm();
                exportButton.setDisable(false);

                // dodawanie biegu do historii
                runsErrorHistory.add(new ArrayList<>(currentRunErrors));

                // sprawdzenie, czy osiągnięto limit 5 prób
                if(runsErrorHistory.size() == savedAttemptLimit)
                {
                    handleExport();
                }
            }));

            solver.setOnFailed(_ -> {
                logArea.appendText("BŁĄD WĄTKU: " + solver.getException().getMessage() + "\n");
                startButton.setDisable(false);
            });

            new Thread(solver).start();

        } catch (Exception e) {
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

    private <T> void fixSpinnerFormatting(Spinner<T> spinner, Function<String, T> parser)
    {
        spinner.getValueFactory().setConverter(new StringConverter<>()
        {
            @Override public String toString(T value)
            {
                return value == null ? "" : value.toString();
            }
            @Override public T fromString(String string)
            {
                try
                {
                    if(string == null || string.isEmpty())
                    {
                        return spinner.getValue();
                    }
                    return parser.apply(string.replace(",", "."));
                }
                catch (NumberFormatException e)
                {
                    return spinner.getValue();
                }
            }
        });
        spinner.getEditor().focusedProperty().addListener((_, _, isFocused) -> {
            if(!isFocused)
            {
                spinner.getEditor().setText(
                        spinner.getValueFactory().getConverter().toString(spinner.getValue())
                );
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
        FileChooser fileChooser = createFileChooser("Zapisz wyniki jako CSV", "Pliki CSV", "csv");
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

    private void handleExport()
    {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Eksport statystyk");
        alert.setHeaderText("Ukończono serię " + savedAttemptLimit + "  prób");
        alert.setContentText("Czy chcesz zapisać wykres odchylenia standardowego do pliku graficznego?");

        Optional<ButtonType> result = alert.showAndWait();
        if(result.isPresent() && result.get() == ButtonType.OK)
        {
            FileChooser fcCombined = createFileChooser("Zapisz wykres zbiorczy błędu", "Obraz PNG", "png");
            fcCombined.setInitialFileName("zbieznosc_zbiorcza.png");
            File fileCombined = fcCombined.showSaveDialog(startButton.getScene().getWindow());
            if(fileCombined != null)
            {
                saveCombinedErrorChartAsImage(fileCombined);
            }

            FileChooser fcSD = createFileChooser("Zapisz wykres odchylenia standardowego", "Obraz PNG", "png");
            fcSD.setInitialFileName("odchylenie_standardowe.png");
            File fileSD = fcSD.showSaveDialog(startButton.getScene().getWindow());
            if(fileSD != null)
            {
                saveSDChartAsImage(fileSD);
            }
        }
        runsErrorHistory.clear();
        errorChart.getData().clear();
        logArea.appendText("Licznik prób zresetowany. Kolejna propozycja zapisu za " + savedAttemptLimit + " uruchomień.\n");
    }

    private void saveCombinedErrorChartAsImage(File file)
    {
        try
        {
            NumberAxis xAxisCombined = new NumberAxis();
            NumberAxis yAxisCombined = new NumberAxis();
            xAxisCombined.setLabel("Epoka");
            yAxisCombined.setLabel("Błąd");
            yAxisCombined.setTickLabelFormatter(createLogScaleFormatter());

            LineChart<Number, Number> ghostChart = new LineChart<>(xAxisCombined, yAxisCombined);
            ghostChart.setAnimated(false);
            ghostChart.setCreateSymbols(false);
            ghostChart.setLegendVisible(true);
            ghostChart.setTitle("Zbieżność algorytmu - " + savedAttemptLimit + " ostatnich prób");

            for(int i = 0; i < runsErrorHistory.size(); i++)
            {
                XYChart.Series<Number, Number> series = new XYChart.Series<>();
                series.setName("Próba " + (i + 1));

                List<Double> runErrors = runsErrorHistory.get(i);
                for(int t = 0; t < runErrors.size(); t++)
                {
                    double rawError = runErrors.get(t);
                    double displayError = rawError > 0 ? Math.log10(rawError) : -15;
                    series.getData().add(new XYChart.Data<>(t, displayError));
                }
                ghostChart.getData().add(series);
            }

            ghostChart.setPrefSize(1000, 700);
            new Scene(ghostChart);

            WritableImage image = ghostChart.snapshot(new SnapshotParameters(), null);
            ImageIO.write(SwingFXUtils.fromFXImage(image, null), "png", file);

            logArea.appendText("ZAPISANO WYKRES ZBIORCZY: " + file.getName() + "\n");
        }
        catch(Exception e)
        {
            logArea.appendText("BŁĄD ZAPISU WYKRESU ZBIORCZEGO: " + e.getMessage() + "\n");
        }
    }

    private void saveSDChartAsImage(File file)
    {
        try
        {
            NumberAxis sdXAxis = new NumberAxis();
            NumberAxis sdYAxis = new NumberAxis();
            sdXAxis.setLabel("Epoka");
            sdYAxis.setLabel("Odchylenie Standardowe");
            sdYAxis.setTickLabelFormatter(createLogScaleFormatter());

            LineChart<Number, Number> ghostChart = new LineChart<>(sdXAxis, sdYAxis);
            ghostChart.setAnimated(false);
            ghostChart.setCreateSymbols(false);
            ghostChart.setLegendVisible(false);
            ghostChart.setTitle("Odchylenie Standardowe ostatnich " + savedAttemptLimit + " prób");

            XYChart.Series<Number, Number> sdSeries = new XYChart.Series<>();

            int maxEpochs = runsErrorHistory.stream().mapToInt(List::size).max().orElse(0);

            for (int index = 0; index < maxEpochs; index++)
            {
                List<Double> errorsAtT = getErrorsAtT(index);

                double mean = errorsAtT.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
                double variance = errorsAtT.stream().mapToDouble(err -> Math.pow(err - mean, 2)).average().orElse(0.0);
                double sd = Math.sqrt(variance);

                double logSD = (sd > 1e-18) ? Math.log10(sd) : -18;
                sdSeries.getData().add(new XYChart.Data<>(index + 1, logSD));
            }

            ghostChart.getData().add(sdSeries);
            ghostChart.setPrefSize(800, 600);

            new Scene(ghostChart);

            WritableImage image = ghostChart.snapshot(new SnapshotParameters(), null);
            ImageIO.write(SwingFXUtils.fromFXImage(image, null), "png", file);

            logArea.appendText("ZAPISANO WYKRES SD: " + file.getName() + "\n");
        }
        catch (Exception e)
        {
            logArea.appendText("BŁĄD GENEROWANIA GRAFIKI SD: " + e.getMessage() + "\n");
        }
    }

    private List<Double> getErrorsAtT(int index)
    {
        List<Double> errorsAtT = new ArrayList<>();

        for(List<Double> run : runsErrorHistory)
        {
            if(index < run.size())
            {
                // próba jeszcze trwa - bierzemy aktualną wartość
                errorsAtT.add(run.get(index));
            }
            else
            {
                // próba już się skończyła - bierzemy jej OSTATNIĄ zapisaną wartość
                errorsAtT.add(run.getLast());
            }
        }
        return errorsAtT;
    }

    private StringConverter<Number> createLogScaleFormatter()
    {
        return new StringConverter<>()
        {
            @Override public String toString(Number value)
            {
                return String.format("10^(%.1f)", value.doubleValue());
            }
            @Override public Number fromString(String string)
            {
                return 0;
            }
        };
    }

    private FileChooser createFileChooser(String title, String description, String extension)
    {
        FileChooser fc = new FileChooser();
        fc.setTitle(title);
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter(description, "*." + extension));
        return fc;
    }
}