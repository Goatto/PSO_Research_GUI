package org.example.projekt_sztucznainteligencja.model;

public enum ErrorType
{
    ABSOLUTE("Błąd bezwzględny")
            {
        @Override
        public double compute(double actual, double target)
        {
            return Math.abs(actual - target);
        }
    },
    SQUARED("Błąd kwadratowy")
            {
        @Override
        public double compute(double actual, double target)
        {
            return Math.pow(actual - target, 2);
        }
    },
    LOG("Błąd logarytmiczny")
            {
        @Override
        public double compute(double actual, double target)
        {
            return Math.log10(1.0 + Math.abs(actual - target));
        }
    };

    private final String displayName;

    ErrorType(String displayName)
    {
        this.displayName = displayName;
    }

    public abstract double compute(double actual, double target);

    @Override
    public String toString() {
        return displayName;
    }
}