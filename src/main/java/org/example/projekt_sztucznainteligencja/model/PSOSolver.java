package org.example.projekt_sztucznainteligencja.model;

import javafx.concurrent.Task;
import java.util.Random;
import java.util.function.Consumer;

public class PSOSolver extends Task<Void>
{
    private final double baseW, baseC1, baseC2, targetOptimum;
    private final int particlesCount, maxEpochs, precision;
    private final FitnessFunction selectedFunc;
    private final boolean randW, randC1, randC2;
    private final double tolerance;

    private Consumer<String> onLogUpdate;
    private Consumer<Particle[]> onChartUpdate;
    private Consumer<double[]> onBestFound;

    public PSOSolver(double baseW, double baseC1, double baseC2, double targetOptimum,
                     int particlesCount, int maxEpochs, int precision,
                     FitnessFunction selectedFunc, boolean randW, boolean randC1, boolean randC2)
    {
        this.baseW = baseW;
        this.baseC1 = baseC1;
        this.baseC2 = baseC2;
        this.targetOptimum = targetOptimum;
        this.particlesCount = particlesCount;
        this.maxEpochs = maxEpochs;
        this.precision = precision;
        this.selectedFunc = selectedFunc;
        this.randW = randW;
        this.randC1 = randC1;
        this.randC2 = randC2;
        this.tolerance = precision > 0 ? Math.pow(10, -precision) : 1e-15;
    }

    public void setOnLogUpdate(Consumer<String> onLogUpdate) { this.onLogUpdate = onLogUpdate; }
    public void setOnChartUpdate(Consumer<Particle[]> onChartUpdate) { this.onChartUpdate = onChartUpdate; }
    public void setOnBestFound(Consumer<double[]> onBestFound) { this.onBestFound = onBestFound; }

    private void log(String message)
    {
        if (onLogUpdate != null) onLogUpdate.accept(message);
    }

    @Override
    protected Void call() throws Exception
    {
        Random rand = new Random();

        final double currentW = randW ? (0.4 + rand.nextDouble() * 0.5) : baseW;
        final double currentC1 = randC1 ? (0.5 + rand.nextDouble() * 2.0) : baseC1;
        final double currentC2 = randC2 ? (0.5 + rand.nextDouble() * 2.0) : baseC2;

        log(String.format("Parametry: w:%.4f | c1:%.4f | c2:%.4f\n", currentW, currentC1, currentC2));

        Particle[] swarm = new Particle[particlesCount];
        double[] globalBestPos = new double[2];
        double globalBestValue = Double.MAX_VALUE;
        int bestParticleIndex = -1;

        double range = selectedFunc.getDomainRange();
        double vLimit = range * 0.2; // 20% dziedziny jako maksymalna prędkość

        // inicjalizacja
        for(int i = 0; i < particlesCount; i++)
        {
            double startX = -range + (2 * range) * rand.nextDouble();
            double startY = -range + (2 * range) * rand.nextDouble();
            swarm[i] = new Particle(startX, startY);

            double eval = selectedFunc.evaluate(startX, startY, targetOptimum);
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
        }

        if (onChartUpdate != null) onChartUpdate.accept(swarm);

        StringBuilder logBatch = new StringBuilder();

        // epoki
        for(int epoch = 0; epoch < maxEpochs; epoch++)
        {
            for (int i = 0; i < particlesCount; i++)
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

                double eval = selectedFunc.evaluate(p.x, p.y, targetOptimum);
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

            logBatch.append(String.format("Epoka %-3d | Błąd: %." + precision + "f\n", epoch, globalBestValue));
            boolean earlyStop = (globalBestValue <= tolerance);

            if(epoch % 5 == 0 || earlyStop)
            {
                String currentLogs = logBatch.toString();
                logBatch.setLength(0);

                log(currentLogs);
                if (onChartUpdate != null) onChartUpdate.accept(swarm);
            }

            if(earlyStop)
            {
                log("EARLY STOP - Osiągnięto optimum o określonej precyzji\n");
                break;
            }

            Thread.sleep(15);
        }

        // raport końcowy
        StringBuilder report = new StringBuilder("\nOsiągnięto koniec poprzez epoki\n");
        for(int i = 0; i < swarm.length; i++)
        {
            double v = selectedFunc.evaluate(swarm[i].x, swarm[i].y, targetOptimum);
            report.append(String.format("Nr %-3d: (%.3f, %.3f) | Błąd: %.6f\n", i, swarm[i].x, swarm[i].y, v));
        }
        report.append(String.format("ZWYCIĘZCA: Cząsteczka nr %d\n", bestParticleIndex));
        report.append(String.format("NAJLEPSZY WYNIK: %.10f w (%.4f, %.4f)\n", globalBestValue, globalBestPos[0], globalBestPos[1]));

        log(report.toString());

        if (onChartUpdate != null) onChartUpdate.accept(swarm);
        if (onBestFound != null) onBestFound.accept(globalBestPos);

        return null;
    }
}