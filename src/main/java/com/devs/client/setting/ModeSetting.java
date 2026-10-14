package com.devs.client.setting;
import java.util.List;
public final class ModeSetting extends Setting<String> {
    private final List<String> modes;
    public ModeSetting(String name, String value, String... modes) { super(name, value); this.modes=List.of(modes); }
    public List<String> modes(){return modes;}
    public void prev(){ set(modes.get((modes.indexOf(get())-1+modes.size())%modes.size())); }
    public void next(){ set(modes.get((modes.indexOf(get())+1)%modes.size())); }
}
