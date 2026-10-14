package com.devs.client.setting;
public final class NumberSetting extends Setting<Double> {
    private final double min, max, step;
    public NumberSetting(String name, double value, double min, double max, double step) { super(name, value); this.min=min;this.max=max;this.step=step; }
    public double min(){return min;} public double max(){return max;} public double step(){return step;}
    public void add(double amount) { set(Math.max(min, Math.min(max, get()+amount))); }
}
