package org.example.projekt_sztucznainteligencja;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.ScatterChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Controller
{
    @FXML private ComboBox<String> functionChoice;
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

    @FXML
    public void initialize()
    {
        functionChoice.getItems().addAll(
                "Ackley function", "Booth function", "Three-hump camel function",
                "Sphere function", "Rastrigin function"
        );
        functionChoice.setValue("Ackley function");

        // konfiguracja wyglądu wykresu
        particleChart.setLegendVisible(false);
        xAxis.setLabel("Oś X");
        yAxis.setLabel("Oś Y");

        // od -15 do 15 na obu osiach
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

        particleChart.getData().add(particleSeries);
        particleChart.getData().add(bestParticleSeries);
    }

    @FXML
    void runPSOAlgorithm()
    {
        try
        {
            final double baseW = Double.parseDouble(inertiaField.getText());
            final double baseC1 = Double.parseDouble(cognitiveField.getText());
            final double baseC2 = Double.parseDouble(socialField.getText());
            final double targetOptimum = Double.parseDouble(optimumField.getText());
            final int particlesCount = Integer.parseInt(particlesAmountField.getText());
            final int maxEpochs = Integer.parseInt(epochsField.getText());
            final int precision = Integer.parseInt(precisionField.getText());
            final String selectedFunc = functionChoice.getValue();
            final double tolerance = precision > 0 ? Math.pow(10, -precision) : 1e-15;

            startButton.setDisable(true);
            logArea.clear();
            particleSeries.getData().clear();
            bestParticleSeries.getData().clear();

            Task<Void> psoTask = new Task<>()
            {
                @Override
                protected Void call() throws Exception
                {
                    Random rand = new Random();

                    // losowanie parametrów
                    final double currentW = randomIntertia.isSelected() ? (0.4 + rand.nextDouble() * 0.5) : baseW;
                    final double currentC1 = randomCognitive.isSelected() ? (0.5 + rand.nextDouble() * 2.0) : baseC1;
                    final double currentC2 = randomSocial.isSelected() ? (0.5 + rand.nextDouble() * 2.0) : baseC2;

                    Platform.runLater(() -> {
                        logArea.appendText("=== ROZPOCZĘTO ALGORYTM PSO ===\n");
                        logArea.appendText(String.format("Parametry: w:%.4f | c1:%.4f | c2:%.4f\n",
                                currentW, currentC1, currentC2));
                        logArea.appendText("------------------------------------\n");
                    });

                    Particle[] swarm = new Particle[particlesCount];
                    double[] globalBestPos = new double[2];
                    double globalBestValue = Double.MAX_VALUE;
                    int bestParticleIndex = -1;

                    double range = selectedFunc.contains("Booth") ? 10.0 : 5.0;

                    // inicjalizacja roju cząsteczek
                    List<XYChart.Data<Number, Number>> initialData = new ArrayList<>();
                    for(int i = 0; i < particlesCount; i++)
                    {
                        double startX = -range + (2 * range) * rand.nextDouble();
                        double startY = -range + (2 * range) * rand.nextDouble();
                        swarm[i] = new Particle(startX, startY);

                        double eval = evaluate(startX, startY, targetOptimum, selectedFunc);
                        swarm[i].bestPos[0] = startX;
                        swarm[i].bestPos[1] = startY;
                        swarm[i].bestValue = eval;

                        if(eval < globalBestValue)
                        {
                            globalBestValue = eval;
                            globalBestPos[0] = startX;
                            globalBestPos[1] = startY;
                            bestParticleIndex = i;
                        }
                        initialData.add(new XYChart.Data<>(startX, startY));
                    }
                    Platform.runLater(() -> particleSeries.getData().setAll(initialData));

                    // przechodzenie przez epoki
                    for(int epoch = 0; epoch < maxEpochs; epoch++)
                    {
                        for (int i = 0; i < particlesCount; i++)
                        {
                            Particle p = swarm[i];
                            p.vx = currentW * p.vx + currentC1 * rand.nextDouble() * (p.bestPos[0] - p.x)
                                    + currentC2 * rand.nextDouble() * (globalBestPos[0] - p.x);
                            p.vy = currentW * p.vy + currentC1 * rand.nextDouble() * (p.bestPos[1] - p.y)
                                    + currentC2 * rand.nextDouble() * (globalBestPos[1] - p.y);

                            double vLimit = 1.2;
                            p.vx = Math.max(-vLimit, Math.min(vLimit, p.vx));
                            p.vy = Math.max(-vLimit, Math.min(vLimit, p.vy));

                            p.x += p.vx;
                            p.y += p.vy;

                            double eval = evaluate(p.x, p.y, targetOptimum, selectedFunc);
                            if(eval < p.bestValue)
                            {
                                p.bestValue = eval;
                                p.bestPos[0] = p.x;
                                p.bestPos[1] = p.y;
                            }
                            if(eval < globalBestValue)
                            {
                                globalBestValue = eval;
                                globalBestPos[0] = p.x;
                                globalBestPos[1] = p.y;
                                bestParticleIndex = i;
                            }
                        }

                        final int currentEpoch = epoch;
                        final double currentBest = globalBestValue;

                        Platform.runLater(() -> logArea.appendText(
                                String.format("Epoka %-3d | Błąd: %." + precision + "f\n", currentEpoch, currentBest)));

                        if(globalBestValue <= tolerance)
                        {
                            Platform.runLater(() -> logArea.appendText(
                                    "EARLY STOP - Osiągnięto optimum o określonej precyzji"));
                            break;
                        }

                        if(epoch % 5 == 0)
                        {
                            List<XYChart.Data<Number, Number>> update = new ArrayList<>();
                            for(Particle p : swarm) update.add(new XYChart.Data<>(p.x, p.y));
                            Platform.runLater(() -> particleSeries.getData().setAll(update));
                        }
                        Thread.sleep(15);
                    }

                    // generowanie raportu końcowego
                    final double fX = globalBestPos[0], fY = globalBestPos[1], fVal = globalBestValue;
                    final int winner = bestParticleIndex;

                    StringBuilder report = new StringBuilder("\nOsiągnięto koniec poprzez epoki\n");
                    for(int i = 0; i < swarm.length; i++)
                    {
                        double v = evaluate(swarm[i].x, swarm[i].y, targetOptimum, selectedFunc);
                        report.append(String.format("Nr %-3d: (%.3f, %.3f) | Błąd: %.6f\n", i, swarm[i].x, swarm[i].y, v));
                    }
                    report.append(String.format("ZWYCIĘZCA: Cząsteczka nr %d\n", winner));
                    report.append(String.format("NAJLEPSZY WYNIK: %.10f w (%.4f, %.4f)\n", fVal, fX, fY));

                    Platform.runLater(() -> {
                        logArea.appendText(report.toString());

                        List<XYChart.Data<Number, Number>> finalPoints = new ArrayList<>();
                        for(Particle p : swarm) finalPoints.add(new XYChart.Data<>(p.x, p.y));
                        particleSeries.getData().setAll(finalPoints);

                        XYChart.Data<Number, Number> bestNode = new XYChart.Data<>(fX, fY);
                        bestParticleSeries.getData().add(bestNode);

                        if(bestNode.getNode() != null)
                        {
                            bestNode.getNode().setStyle("-fx-background-color: #0000ff; -fx-background-radius: 10px;");
                            bestNode.getNode().toFront();
                        }
                        startButton.setDisable(false);
                    });
                    return null;
                }
            };
            new Thread(psoTask).start();
        }
        catch(Exception e)
        {
            logArea.appendText("BŁĄD: " + e.getMessage() + "\n");
            startButton.setDisable(false);
        }
    }

    private double evaluate(double x, double y, double target, String functionName)
    {
        double val;
        switch (functionName)
        {
            case String s when s.contains("Ackley") -> {
                double p1 = -20 * Math.exp(-0.2 * Math.sqrt(0.5 * (x * x + y * y)));
                double p2 = Math.exp(0.5 * (Math.cos(2 * Math.PI * x) + Math.cos(2 * Math.PI * y)));
                val = p1 - p2 + Math.E + 20;
            }
            case String s when s.contains("Booth") -> {
                    val = Math.pow(x + 2 * y - 7, 2) + Math.pow(2 * x + y - 5, 2);
            }
            case String s when s.contains("camel") -> {
                    val = 2 * x * x - 1.05 * Math.pow(x, 4) + Math.pow(x, 6) / 6.0 + x * y + y * y;
            }
            case String s when s.contains("Sphere") -> {
                    val = x * x + y * y;
            }
            case String s when s.contains("Rastrigin") -> {
                    val = 20 + (x * x - 10 * Math.cos(2 * Math.PI * x)) + (y * y - 10 * Math.cos(2 * Math.PI * y));
            }
            case null, default -> val = 0d;
        }
        return Math.abs(val - target);
    }

    static class Particle
    {
        double x, y, vx = 0, vy = 0;
        double[] bestPos = new double[2];
        double bestValue = Double.MAX_VALUE;
        Particle(double x, double y)
        {
            this.x = x;
            this.y = y;
        }
    }
}