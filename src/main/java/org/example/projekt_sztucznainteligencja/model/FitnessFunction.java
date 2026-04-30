package org.example.projekt_sztucznainteligencja.model;

// TODO KOMENTARZE
public enum FitnessFunction
{
    ACKLEY("Ackley function", 15.0)
            {
                @Override
                public double compute(double x, double y)
                {
                    double p1 = -20 * Math.exp(-0.2 * Math.sqrt(0.5 * (x * x + y * y)));
                    double p2 = Math.exp(0.5 * (Math.cos(2 * Math.PI * x) + Math.cos(2 * Math.PI * y)));
                    return p1 - p2 + Math.E + 20;
                }
            },
    BOOTH("Booth function", 10.0)
            {
                @Override
                public double compute(double x, double y)
                {
                    return Math.pow(x + 2 * y - 7, 2) + Math.pow(2 * x + y - 5, 2);
                }
            },
    CAMEL("Three-hump camel function", 5.0)
            {
                @Override
                public double compute(double x, double y)
                {
                    return 2 * x * x - 1.05 * Math.pow(x, 4) + Math.pow(x, 6) / 6.0 + x * y + y * y;
                }
            },
    SPHERE("Sphere function", 5.0)
            {
                @Override
                public double compute(double x, double y)
                {
                    return x * x + y * y;
                }
            },
    RASTRIGIN("Rastrigin function", 5.0)
            {
                @Override
                public double compute(double x, double y)
                {
                    return 20 + (x * x - 10 * Math.cos(2 * Math.PI * x)) + (y * y - 10 * Math.cos(2 * Math.PI * y));
                }
            };

    private final String displayName;
    private final double domainRange;

    FitnessFunction(String displayName, double domainRange)
    {
        this.displayName = displayName;
        this.domainRange = domainRange;
    }

    public double getDomainRange()
    {
        return domainRange;
    }

    public abstract double compute(double x, double y);

    public double evaluate(double x, double y, double target)
    {
        return Math.abs(compute(x, y) - target);
    }

    @Override
    public String toString()
    {
        return displayName;
    }
}