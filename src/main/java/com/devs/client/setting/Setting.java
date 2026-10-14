package com.devs.client.setting;

public abstract class Setting<T> {
    private final String name;
    private final T def;
    private T value;
    protected Setting(String name, T value) { this.name = name; this.value = value; this.def = value; }
    public String name() { return name; }
    public T get() { return value; }
    public void set(T value) { this.value = value; }
    /** Restaura el valor inicial (lo usa el boton Reset de la GUI). */
    public void reset() { this.value = def; }
}
