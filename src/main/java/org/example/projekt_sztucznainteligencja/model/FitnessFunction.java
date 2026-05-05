package org.example.projekt_sztucznainteligencja.model;

public enum FitnessFunction
{
    ACKLEY("Funkcja Ackleya", 15.0, 0d)
            {
                @Override
                public double compute(double x, double y)
                {
                    double p1 = -20 * Math.exp(-0.2 * Math.sqrt(0.5 * (x * x + y * y)));
                    double p2 = Math.exp(0.5 * (Math.cos(2 * Math.PI * x) + Math.cos(2 * Math.PI * y)));
                    return p1 - p2 + Math.E + 20;
                }
            },
    BOOTH("Funkcja Bootha", 10.0, 0d)
            {
                @Override
                public double compute(double x, double y)
                {
                    return Math.pow(x + 2 * y - 7, 2) + Math.pow(2 * x + y - 5, 2);
                }
            },
    CAMEL("Funkcja Wielbłąda Z Trzema Grzbietami", 5.0, 0d)
            {
                @Override
                public double compute(double x, double y)
                {
                    return 2 * x * x - 1.05 * Math.pow(x, 4) + Math.pow(x, 6) / 6.0 + x * y + y * y;
                }
            },
    SIX_HUMP_CAMEL("Funkcja Wielbłąda Z Sześcioma Grzbietami", 5.0, -1.0316)
            {
                @Override
                public double compute(double x, double y)
                {
                    return (4 - 2.1 * x * x + Math.pow(x, 4) / 3.0) * x * x + x * y + (-4 + 4 * y * y) * y * y;
                }
            },
    SPHERE("Funkcja Kuli", 5.0, 0d)
            {
                @Override
                public double compute(double x, double y)
                {
                    return x * x + y * y;
                }
            },
    RASTRIGIN("Funkcja Rastrigina", 5.0, 0d)
            {
                @Override
                public double compute(double x, double y)
                {
                    return 20 + (x * x - 10 * Math.cos(2 * Math.PI * x)) + (y * y - 10 * Math.cos(2 * Math.PI * y));
                }
            };

    private final String displayName;
    private final double domainRange;
    private final double targetOptimum;

    FitnessFunction(String displayName, double domainRange, double targetOptimum)
    {
        this.displayName = displayName;
        this.domainRange = domainRange;
        this.targetOptimum = targetOptimum;
    }

    public double getDomainRange()
    {
        return domainRange;
    }

    public double getTargetOptimum()
    {
        return targetOptimum;
    }

    public abstract double compute(double x, double y);

    public double evaluate(double x, double y, double target, ErrorType errorType)
    {
        double actual = compute(x, y);
        return errorType.compute(actual, target);
    }

    @Override
    public String toString()
    {
        return displayName;
    }
}