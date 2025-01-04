package org.ttchampagne.regionplugin.commands;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.bukkit.command.Command;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.ttchampagne.regionplugin.RegionPlugin;
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
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    ((RegionPlugin) plugin).getMessagesConfig().getString("messages.no_permission")));
            return true;
        }

        // Si el comando es "/RegionPlugin"
        if (command.getName().equalsIgnoreCase("RegionPlugin")) {
            // Si no hay argumentos, muestra la versión actual
            if (args.length == 0) {
                String currentVersion = plugin.getDescription().getVersion();
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                        ((RegionPlugin) plugin).getMessagesConfig().getString("messages.current_version")
                        .replace("{version}", currentVersion)));
                return true;
            }
            // Si el comando es "/RegionPlugin update"
            else if (args[0].equalsIgnoreCase("update")) {
                // Mostrar la versión actual del plugin
                String currentVersion = plugin.getDescription().getVersion();
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                        ((RegionPlugin) plugin).getMessagesConfig().getString("messages.current_version")
                        .replace("{version}", currentVersion)));

                // Iniciar la verificación de actualizaciones
                AutoUpdate updateChecker = new AutoUpdate(plugin); // Si necesita el plugin como parámetro
                updateChecker.checkForUpdates();

                return true;
            }
            // Si el comando es "/RegionPlugin reload"
            else if (args[0].equalsIgnoreCase("reload")) {
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                        ((RegionPlugin) plugin).getMessagesConfig().getString("messages.reload_start")));

                // Reiniciar el plugin (deshabilitar y habilitar)
                plugin.getServer().getPluginManager().disablePlugin(plugin);
                plugin.getServer().getPluginManager().enablePlugin(plugin);

                sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                        ((RegionPlugin) plugin).getMessagesConfig().getString("messages.reload_success")));
                return true;
            }
            // Si el comando es "/RegionPlugin configreload"
            else if (args[0].equalsIgnoreCase("configreload")) {
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                        ((RegionPlugin) plugin).getMessagesConfig().getString("messages.config_reload")));

                // Recargar archivos de configuración
                ((RegionPlugin) plugin).loadRegions(); // Recargar regiones desde la configuración
                ((RegionPlugin) plugin).saveDefaultMessages(); // Recargar mensajes

                sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                        ((RegionPlugin) plugin).getMessagesConfig().getString("messages.config_reloaded")));
                return true;
            }
            // Si el comando es "/RegionPlugin messagesreload"
            else if (args[0].equalsIgnoreCase("messagesreload")) {
                File messagesFile = new File(plugin.getDataFolder(), "messages.yml");

                // Notificar al usuario que el proceso ha comenzado
                sender.sendMessage(ChatColor.YELLOW + "Starting the messages reset process...");

                // Si el archivo de mensajes existe, eliminarlo
                if (messagesFile.exists()) {
                    if (!messagesFile.delete()) {
                         sender.sendMessage(ChatColor.RED + "Failed to delete the messages file.");
                        return true;
                    }
                }

                // Guardar el archivo de mensajes predeterminado
                plugin.saveResource("messages.yml", false);

                // Recargar mensajes
                ((RegionPlugin) plugin).saveDefaultMessages();

                // Notificar al usuario que el proceso ha finalizado
                sender.sendMessage(ChatColor.GREEN + "Messages have been successfully reset and reloaded.");
                return true;
            }
        }
        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            // Lista de opciones posibles
            List<String> options = Arrays.asList("configreload", "messagesreload", "reload", "update");

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
