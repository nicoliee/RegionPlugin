package org.ttchampagne.regionplugin.commands;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

public class MapCommand implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 1) {
            return false;
        }
        if (!(sender instanceof Player)) {
            return false;
        }
        String mapName = args[0];
        Player player = (Player) sender;
        executeMapCommand(player, mapName);
        return true;
    }

    public void executeMapCommand(Player player, String mapName) {
        // Obtener la carpeta donde están los mundos, en este caso "AmazingTowers/instances"
        File instancesDirectory = new File("plugins/AmazingTowers/instances");
        
        // Recorrer todas las carpetas dentro de la carpeta "instances"
        for (File worldFolder : instancesDirectory.listFiles()) {
            if (worldFolder.isDirectory()) {
                // Obtener el archivo config.yml de cada carpeta
                File configFile = new File(worldFolder, "config.yml");
                
                if (configFile.exists()) {
                    // Leer el archivo YAML (aquí se usaría un parser YAML como SnakeYAML)
                    YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);
                    
                    // Verificar si existe la clave "name"
                    if (config.contains("name") && config.getString("name").equalsIgnoreCase(mapName)) {
                        // Si encontramos "name: mini", ejecutar el comando /tt tpworld
                        String worldName = worldFolder.getName();
                        String playerName = player.getName(); // Obtener el nombre del jugador que ejecuta el comando
                        player.getServer().dispatchCommand(player.getServer().getConsoleSender(), "tt tpworld " + worldName + " " + playerName);
                        return; // Salir después de ejecutar el comando
                    }
                }
            }
        }
        // Si no se encuentra ninguna instancia con name: mini
        player.sendMessage("No se encontró ninguna instancia con nombre " + mapName);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        List<String> suggestions = new ArrayList<>();
        
        if (args.length == 1) {
            // Obtener la carpeta donde están los mundos, en este caso "AmazingTowers/instances"
            File instancesDirectory = new File("plugins/AmazingTowers/instances");
            
            // Recorrer todas las carpetas dentro de la carpeta "instances"
            for (File worldFolder : instancesDirectory.listFiles()) {
                if (worldFolder.isDirectory()) {
                    // Obtener el archivo config.yml de cada carpeta
                    File configFile = new File(worldFolder, "config.yml");
                    
                    if (configFile.exists()) {
                        // Leer el archivo YAML (aquí se usaría un parser YAML como SnakeYAML)
                        YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);
                        
                        // Verificar si existe la clave "name"
                        if (config.contains("name")) {
                            String mapName = config.getString("name");
                            // Agregar el nombre del mapa a la lista de sugerencias
                            if (mapName != null) {
                                suggestions.add(mapName);
                            }
                        }
                    }
                }
            }
        }
        
        return suggestions;
    }
}
