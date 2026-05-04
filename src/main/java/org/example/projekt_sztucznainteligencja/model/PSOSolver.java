package org.example.projekt_sztucznainteligencja.model;

import javafx.concurrent.Task;
import java.util.Random;
import java.util.function.Consumer;

public class PSOSolver extends Task<Void>
{
    private double globalBestValue = Double.MAX_VALUE;

    private final double baseInertia;
    private final double baseCognitive;
    private final double baseSocial;
    private final double targetOptimum;
    private final int particlesCount;
    private final int maxEpochs;
    private final int precision;
    private final FitnessFunction selectedFitnessFunction;

    // czy określone parametry mają być losowe
    private final boolean IsInertiaRandom;
    private final boolean IsCognitiveRandom;
    private final boolean IsSocialRandom;

    private final boolean useGridDistribution;

    // tolerancja wymagana do uzyskania wczesnego zatrzymania
    private final double earlyStopTolerance;

    // callbacki do asynchronicznego zwracania danych do GUI
    private Consumer<String> onLogUpdate;
    private Consumer<Particle[]> onChartUpdate;
    private Consumer<double[]> onBestFound;

    // dane do wyeksportowania
    private Particle[] swarm;

    public PSOSolver(double baseInertia, double baseCognitive, double baseC2, double targetOptimum,
                     int particlesCount, int maxEpochs, int precision, FitnessFunction selectedFitnessFunction,
                     boolean IsInertiaRandom, boolean IsCognitiveRandom, boolean IsSocialRandom, boolean useGridDistribution)
    {
        this.baseInertia = baseInertia;
        this.baseCognitive = baseCognitive;
        this.baseSocial = baseC2;
        this.targetOptimum = targetOptimum;
        this.particlesCount = particlesCount;
        this.maxEpochs = maxEpochs;
        this.precision = precision;
        this.selectedFitnessFunction = selectedFitnessFunction;
        this.IsInertiaRandom = IsInertiaRandom;
        this.IsCognitiveRandom = IsCognitiveRandom;
        this.IsSocialRandom = IsSocialRandom;
        this.useGridDistribution = useGridDistribution;
        this.earlyStopTolerance = precision > 0 ? Math.pow(10, -precision) : 1e-15;
    }

    public void setOnLogUpdate(Consumer<String> onLogUpdate)
    {
        this.onLogUpdate = onLogUpdate;
    }
    public void setOnChartUpdate(Consumer<Particle[]> onChartUpdate)
    {
        this.onChartUpdate = onChartUpdate;
    }
    public void setOnBestFound(Consumer<double[]> onBestFound)
    {
        this.onBestFound = onBestFound;
    }

    private void log(String message)
    {
        if (onLogUpdate != null) onLogUpdate.accept(message);
    }

    @Override
    protected Void call() throws Exception
    {
        Random rand = new Random();

        final double startingW = IsInertiaRandom ? (0.4 + rand.nextDouble() * 0.5) : baseInertia;
        double targetW = startingW / 3.0;
        final double currentC1 = IsCognitiveRandom ? (0.5 + rand.nextDouble() * 2.0) : baseCognitive;
        final double currentC2 = IsSocialRandom ? (0.5 + rand.nextDouble() * 2.0) : baseSocial;

        log(String.format("Parametry: w:%.4f | c1:%.4f | c2:%.4f\n", startingW, currentC1, currentC2));

        swarm = new Particle[particlesCount];
        double[] globalBestPos = new double[2];
        double globalBestValue = Double.MAX_VALUE;
        int bestParticleIndex = -1;

        double range = selectedFitnessFunction.getDomainRange();
        // nasz clamp na prędkość
        double vLimit = range * 0.2; // 20% dziedziny jako maksymalna prędkość

        // inicjalizacja
        if(useGridDistribution)
        {
            int cols = (int) Math.sqrt(particlesCount);
            int rows = (int) Math.ceil((double) particlesCount / cols);
            double cellWidth = (2 * range) / cols;
            double cellHeight = (2 * range) / rows;

            for(int i = 0; i < particlesCount; i++)
            {
                int r = i / cols;
                int c = i % cols;
                double startX = -range + (c + 0.5) * cellWidth;
                double startY = -range + (r + 0.5) * cellHeight;
                initParticle(i, startX, startY, globalBestPos);
            }
        }
        else
        {
            for (int i = 0; i < particlesCount; i++)
            {
                double startX = -range + (2 * range) * rand.nextDouble();
                double startY = -range + (2 * range) * rand.nextDouble();
                initParticle(i, startX, startY, globalBestPos);
            }
        }

        if(onChartUpdate != null)
        {
            onChartUpdate.accept(swarm);
        }

        StringBuilder logBatch = new StringBuilder();

        // epoki
        double currentW = startingW;
        for(int epoch = 1; epoch <= maxEpochs; epoch++)
        {
            for(int i = 0; i < particlesCount; i++)
            {
                Particle p = swarm[i];
                p.vx = currentW * p.vx + currentC1 * rand.nextDouble() * (p.bestPos[0] - p.x)
                        + currentC2 * rand.nextDouble() * (globalBestPos[0] - p.x);
                p.vy = currentW * p.vy + currentC1 * rand.nextDouble() * (p.bestPos[1] - p.y)
                        + currentC2 * rand.nextDouble() * (globalBestPos[1] - p.y);

                p.vx = Math.max(-vLimit, Math.min(vLimit, p.vx));
                p.vy = Math.max(-vLimit, Math.min(vLimit, p.vy));

                p.x += p.vx;
                p.y += p.vy;

                double eval = selectedFitnessFunction.evaluate(p.x, p.y, targetOptimum);
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
                // zmniejszamy bezwładność do 1/3 oryginalnej wartości w ostatniej epoce
                currentW = startingW - ((double) (epoch - 1) / (maxEpochs - 1)) * (startingW - targetW);
            }

            logBatch.append(String.format("Epoka %-3d | Błąd: "
                    + (globalBestValue > 1e100 ? "Start" : "%."
                    + Math.max(0, precision) + "f") + "\n", epoch, globalBestValue));
            boolean earlyStop = (globalBestValue <= earlyStopTolerance);

            if(epoch == maxEpochs || epoch % 5 == 0 || earlyStop)
            {
                String currentLogs = logBatch.toString();
                logBatch.setLength(0);

                log(currentLogs);
                if (onChartUpdate != null) onChartUpdate.accept(swarm);
            }
            if(earlyStop)
            {
                log("\nEARLY STOP - Osiągnięto optimum o określonej precyzji\n");
                break;
            }

            Thread.sleep(15);
        }

        // raport końcowy
        StringBuilder report = new StringBuilder("\nOsiągnięto koniec poprzez epoki\n");
        for(int i = 0; i < swarm.length; i++)
        {
            double v = selectedFitnessFunction.evaluate(swarm[i].x, swarm[i].y, targetOptimum);
            report.append(String.format("Cząsteczka %-3d: (%.3f, %.3f) | Błąd: %.6f\n", i + 1, swarm[i].x, swarm[i].y, v));
        }
        report.append(String.format("ZWYCIĘZCA: Cząsteczka nr %d, %.10f w (%.4f, %.4f)\n", bestParticleIndex, globalBestValue, globalBestPos[0], globalBestPos[1]));
        log(report.toString());

        if(onChartUpdate != null)
        {
            onChartUpdate.accept(swarm);
        }
        if(onBestFound != null)
        {
            onBestFound.accept(globalBestPos);
        }

        return null;
    }

    public Particle[] getSwarm()
    {
        return swarm;
    }

    private void initParticle(int i, double x, double y, double[] globalBestPos)
    {
        swarm[i] = new Particle(x, y);

        double eval = selectedFitnessFunction.evaluate(x, y, targetOptimum);
        swarm[i].bestPos[0] = x;
        swarm[i].bestPos[1] = y;
        swarm[i].bestValue = eval;

        if(eval < globalBestValue)
        {
            globalBestValue = eval;
            globalBestPos[0] = x;
            globalBestPos[1] = y;
        }
    }
}