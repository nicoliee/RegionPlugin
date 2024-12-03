package org.ttchampagne.regionplugin.commands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.ttchampagne.regionplugin.update.AutoUpdate;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.TabCompleter;

public class RegionPluginCommand implements CommandExecutor, TabCompleter {
    private JavaPlugin plugin;
    public RegionPluginCommand(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    // Método que se llama cuando se ejecuta el comando
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Verificar si el usuario tiene permisos
        if (!sender.hasPermission("towers.admin") && !sender.isOp()) {
            sender.sendMessage("§cNo tienes permiso para usar este comando.");
            return true;
        }
        // Si el comando es "/RegionPlugin"
        if (command.getName().equalsIgnoreCase("RegionPlugin")) {
            // Si no hay argumentos, muestra la versión actual
            if (args.length == 0) {
                String currentVersion = plugin.getDescription().getVersion();
                sender.sendMessage("§aLa versión actual de RegionPlugin es: §b" + currentVersion);
                return true;
            }
            // Si el comando es "/RegionPlugin update"
            else if (args[0].equalsIgnoreCase("update")) {
                // Mostrar la versión actual del plugin
                String currentVersion = plugin.getDescription().getVersion();
                sender.sendMessage("§aLa versión actual de RegionPlugin es: §b" + currentVersion);

                // Iniciar la verificación de actualizaciones
                AutoUpdate updateChecker = new AutoUpdate(plugin); // Si necesita el plugin como parámetro
                updateChecker.checkForUpdates();

                return true;
            }
            // Si el comando es "/RegionPlugin reload"
            else if (args[0].equalsIgnoreCase("reload")) {
                sender.sendMessage("§aRecargando el plugin...");
                plugin.getServer().getPluginManager().disablePlugin(plugin);
                plugin.getServer().getPluginManager().enablePlugin(plugin);
                sender.sendMessage("§aEl plugin se ha recargado correctamente.");
                return true;
            }
        }
        return false;
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            // Lista de opciones posibles
            List<String> options = Arrays.asList("reload", "update");

            // Filtrar las opciones que comienzan con el texto ingresado por el usuario
            String input = args[0].toLowerCase();
            List<String> filteredOptions = new ArrayList<>();
            for (String option : options) {
                if (option.toLowerCase().startsWith(input)) {
                    filteredOptions.add(option);
                }
            }
            return filteredOptions;
        }
        return new ArrayList<>();
    }
}
