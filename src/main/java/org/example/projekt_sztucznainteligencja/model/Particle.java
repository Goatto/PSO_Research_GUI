package org.example.projekt_sztucznainteligencja.model;

public class Particle
{
    // obecna pozycja
    public double x;
    public double y;
    // prędkości
    public double vx = 0;
    public double vy = 0;
    public double[] bestPos = new double[2];
    // najlepsza wartość, którą do tej pory osiągnęliśmy
    // maksimum wartości, z racji, że zawsze otrzymamy niższą liczbę, która od razu ją zastąpi
    public double bestValue = Double.MAX_VALUE;

    public Particle(double x, double y)
    {
        this.x = x;
        this.y = y;
    }
}