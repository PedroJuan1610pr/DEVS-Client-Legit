package com.devs.client.module;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
public final class ModuleManager {
    private final List<Module> modules = new ArrayList<>();
    /** Vista de solo lectura: all() se llama varias veces por frame, antes copiaba la lista cada vez. */
    private final List<Module> view = Collections.unmodifiableList(modules);
    public void register(Module module){modules.add(module);} public List<Module> all(){return view;}
    public Optional<Module> find(String name){return modules.stream().filter(m->m.name().equalsIgnoreCase(name)).findFirst();}
}
