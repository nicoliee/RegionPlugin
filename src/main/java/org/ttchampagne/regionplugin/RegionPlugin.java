package org.ttchampagne.regionplugin;

import org.bukkit.Bukkit;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.ChatColor;
import org.ttchampagne.regionplugin.commands.CapitanesCommand;
import org.ttchampagne.regionplugin.commands.ListaCommand;
import org.ttchampagne.regionplugin.commands.TorneoCommand;
import org.ttchampagne.regionplugin.commands.TablaCommand;
import org.ttchampagne.regionplugin.commands.RerollCommand;
import org.ttchampagne.regionplugin.commands.InstancesCommand;
import org.ttchampagne.regionplugin.listeners.TorneoListeners;
import org.ttchampagne.regionplugin.commands.RegionPluginCommand;
import org.ttchampagne.regionplugin.commands.RegionCommand;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class RegionPlugin extends JavaPlugin {

    private YamlConfiguration languageConfig; // Configuración de idioma
    private final Map<String, Region> regions = new HashMap<>(); // Mapa para almacenar las regiones definidas
    public static boolean privadoActivado; // Variable para almacenar el estado del modo privado
    public static String mundoPrivado; // Variable para almacenar el mundo donde se activó el modo privado
    private FileConfiguration messagesConfig; // Configuración de mensajes
    // Inicializar estructuras de datos
    Map<String, Boolean> worldProtectionStatus = new HashMap<>();
    Map<String, Boolean> globalProtectionStatus = new HashMap<>();
    Map<String, BukkitRunnable> protectionTimers = new HashMap<>();
    Map<String, Integer> protectionTimeRemaining = new HashMap<>();
    Map<String, Integer> regenerationTimerRemaining = new HashMap<>();
    Map<String, Integer> hasteTimerRemaining = new HashMap<>();
    Map<String, Boolean> privateModeMap = new HashMap<>();

    // Crear instancia de TorneoListeners con dependencias inicializadas
    TorneoListeners torneoListeners = new TorneoListeners(
        this, 
        worldProtectionStatus, 
        protectionTimers, 
        protectionTimeRemaining, 
        regenerationTimerRemaining, 
        hasteTimerRemaining, 
        privateModeMap
    );
    @Override
    public void onEnable() {
        
        // Cargar el archivo de configuración predeterminado
        saveDefaultConfig();
        // Cargar las regiones definidas en el config.yml e idioma
        loadRegions();
        loadLanguage();
        // Crear una instancia del listener que manejará la lógica de protección y comandos
        Bukkit.getPluginManager().registerEvents(torneoListeners, this);
        // Crear instancia del listener
        TorneoListeners torneoListeners = new TorneoListeners(this, null, null, null, null, null, null);
        // Registrar los eventos de protección de regiones
        Bukkit.getPluginManager().registerEvents(torneoListeners, this);
        // Registrar el comando /torneo
        new TorneoCommand(this, torneoListeners);
        getCommand("capitanes").setExecutor(new CapitanesCommand(this));
        getCommand("reroll").setExecutor(new RerollCommand(this));
        getCommand("tabla").setExecutor(new TablaCommand(this));
        getCommand("instances").setExecutor(new InstancesCommand(this));
        getCommand("lista").setExecutor(new ListaCommand(this));
        this.getCommand("region").setExecutor(new RegionCommand(this));
        this.getCommand("RegionPlugin").setExecutor(new RegionPluginCommand(this));
        // Reinicio inicial de variables
        privadoActivado = false;
        mundoPrivado = "";
        // Cargar Messages.yml
        saveDefaultMessages();
    }

    // Método para cargar el archivo de idioma
    private void loadLanguage() {
        InputStream languageStream = getResource("language.yml");
        if (languageStream == null) {
            getLogger().warning("No se pudo cargar el archivo de idioma.");
            return;
        }
        languageConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(languageStream, StandardCharsets.UTF_8));
    }
    // Obtener el idioma global del servidor desde config.yml
    private String getServerLanguage() {
        return getConfig().getString("language", "es"); // Por defecto en español
    }

    // Obtener el mensaje de error desde language.yml en el idioma seleccionado
    public String getErrorMessage(String key) {
        String language = getServerLanguage();  // Obtener el idioma global del servidor
        return ChatColor.translateAlternateColorCodes('&',
                languageConfig.getString("messages." + language + "." + key));
    }

    // Obtener mensajes configurables desde messages.yml
    public String getConfigurableMessage(String key) {
        return ChatColor.translateAlternateColorCodes('&',
                getConfig().getString("messages." + key));
    }

    // Método para cargar las regiones desde el archivo de configuración
    public void loadRegions() {
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

    // Método para guardar el archivo de configuración de mensajes
    public void saveDefaultMessages() {
        File messagesFile = new File(getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            saveResource("messages.yml", false);
        }
        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);
    }

    // Método para obtener el archivo de configuración de mensajes
    public FileConfiguration getMessagesConfig() {
        return messagesConfig;
    }
}