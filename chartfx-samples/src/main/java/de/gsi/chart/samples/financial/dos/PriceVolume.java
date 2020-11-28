package de.gsi.chart.samples.financial.dos;

public class PriceVolume {
    public double price;
    public double volumeDown; // bid
    public double volumeUp; // ask

    public PriceVolume(double price, double volumeDown, double volumeUp) {
        this.price = price;
        this.volumeDown = volumeDown; // bid
        this.volumeUp = volumeUp; // ask
    }

    @Override
    public String toString() {
        return "PriceVolume [price=" + price + ", bidVolume=" + volumeDown + ", askVolume=" + volumeUp + "]";
    }
}