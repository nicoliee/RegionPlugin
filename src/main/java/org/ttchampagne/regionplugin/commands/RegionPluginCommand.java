package org.ttchampagne.regionplugin.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.ttchampagne.regionplugin.update.AutoUpdate;
import org.bukkit.command.CommandExecutor;

public class RegionPluginCommand implements CommandExecutor {
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
            else if (args.length > 0 && args[0].equalsIgnoreCase("update")) {
                // Mostrar la versión actual del plugin
                String currentVersion = plugin.getDescription().getVersion();
                sender.sendMessage("§aLa versión actual de RegionPlugin es: §b" + currentVersion);

                // Iniciar la verificación de actualizaciones
                AutoUpdate updateChecker = new AutoUpdate(plugin); // Si necesita el plugin como parámetro
                updateChecker.checkForUpdates();

                return true;
            }
        }
        return false;
    }
}
