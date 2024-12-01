package org.ttchampagne.regionplugin;

import org.bukkit.Bukkit;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.ttchampagne.regionplugin.commands.CapitanesCommand;
import org.ttchampagne.regionplugin.commands.ListaCommand;
import org.ttchampagne.regionplugin.commands.TorneoCommand;
import org.ttchampagne.regionplugin.commands.RegionPluginCommand;


import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class RegionPlugin extends JavaPlugin {

    private final Map<String, Region> regions = new HashMap<>(); // Mapa para almacenar las regiones definidas
    public static boolean privadoActivado; // Variable para almacenar el estado del modo privado
    public static String mundoPrivado; // Variable para almacenar el mundo donde se activó el modo privado

    @Override
    public void onEnable() {
        // Cargar el archivo de configuración predeterminado
        saveDefaultConfig();
        // Cargar las regiones definidas en el config.yml
        loadRegions();
        // Crear una instancia del listener que manejará la lógica de protección y comandos
        TorneoCommand listener = new TorneoCommand(this);
        // Registrar los eventos de protección de regiones
        Bukkit.getPluginManager().registerEvents(listener, this);
        // Registrar el comando /torneo
        this.getCommand("torneo").setExecutor(listener);
        this.getCommand("capitanes").setExecutor(new CapitanesCommand());
        this.getCommand("lista").setExecutor(new ListaCommand());
        this.getCommand("RegionPlugin").setExecutor(new RegionPluginCommand(this));
        // Reinicio inicial de variables
        privadoActivado = false;
        mundoPrivado = "";
    }
    // Método para cargar las regiones desde el archivo de configuración
    private void loadRegions() {
        // Obtener el archivo de configuración
        FileConfiguration config = getConfig();
        // Obtener los mundos definidos en el archivo de configuración
        Set<String> worlds = config.getKeys(false);

        // Iterar sobre los mundos y cargar las regiones definidas
        for (String worldName : worlds) {
            String p1String = config.getString(worldName + ".P1"); // Obtener la ubicación del punto 1
            String p2String = config.getString(worldName + ".P2"); // Obtener la ubicación del punto 2

            if (p1String != null && p2String != null) {
                // Convertir las cadenas de ubicación en objetos Location
                Location p1 = parseLocation(worldName, p1String); // Convertir la cadena de ubicación del punto 1
                Location p2 = parseLocation(worldName, p2String); // Convertir la cadena de ubicación del punto 2
                regions.put(worldName, new Region(p1, p2)); // Crear una nueva región y agregarla al mapa de regiones
            }
        }
    }
    // Método para convertir una cadena de ubicación en un objeto Location
    private Location parseLocation(String worldName, String locString) {
        String[] parts = locString.replace("(", "").replace(")", "").split(",");
        World world = Bukkit.getWorld(worldName);
        return new Location(world,
                Double.parseDouble(parts[0]),
                Double.parseDouble(parts[1]),
                Double.parseDouble(parts[2]));
    }
    // Método para obtener el mapa de regiones
    public Map<String, Region> getRegions() {
        return regions;
    }
}
