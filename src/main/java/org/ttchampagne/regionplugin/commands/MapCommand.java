package org.ttchampagne.regionplugin.commands;

import java.io.File;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.ttchampagne.regionplugin.RegionPlugin;
import org.ttchampagne.utils.SendMessage;

public class MapCommand implements CommandExecutor {
    private final RegionPlugin plugin;

    public MapCommand(RegionPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 1) {
            return false;
        }
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getErrorMessage("errors.no_player"));
            return true;
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
        // Si no se encuentra ninguna instancia con name
        SendMessage.sendToPlayer(player, plugin.getErrorMessage("map.error"));
    }
}
