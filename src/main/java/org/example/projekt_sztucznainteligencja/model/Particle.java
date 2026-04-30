package org.example.projekt_sztucznainteligencja.model;

public class Particle
{
    public double x, y, vx = 0, vy = 0;
    public double[] bestPos = new double[2];
    public double bestValue = Double.MAX_VALUE;

    public Particle(double x, double y)
    {
        this.x = x;
        this.y = y;
    }
}