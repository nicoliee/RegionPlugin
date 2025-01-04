package org.ttchampagne.regionplugin.commands;

import java.io.File;
import java.util.Arrays;
import java.io.IOException;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.ttchampagne.regionplugin.RegionPlugin;

public class TablaCommand implements CommandExecutor {
    private final RegionPlugin plugin;
    public TablaCommand(RegionPlugin plugin) {
        this.plugin = plugin;
    }
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("tabla")) {
            return false;
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    plugin.getMessagesConfig().getString("messages.no_player")));
            return true;
        }

        Player player = (Player) sender;

        if (!(player.hasPermission("towers.admin") || player.isOp())) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    plugin.getMessagesConfig().getString("messages.no_permission")));
            return true;
        }

        if (args.length < 1) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    plugin.getMessagesConfig().getString("messages.tables_specify_table")));
            return true;
        }

        List<String> tables = Arrays.asList(args);

        File globalConfigFile = new File(Bukkit.getServer().getPluginManager()
                    .getPlugin("AmazingTowers") // Nombre del plugin
                    .getDataFolder() + "/globalConfig.yml"); // Ruta al archivo

        if (!globalConfigFile.exists()) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    plugin.getMessagesConfig().getString("messages.tables_file_not_found")));
            return true;
        }

        YamlConfiguration globalConfig = YamlConfiguration.loadConfiguration(globalConfigFile);

        // Accede directamente al nodo y actualiza solo "tableNames"
        if (globalConfig.contains("options.database.tableNames")) {
            globalConfig.set("options.database.tableNames", tables);
        } else {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                plugin.getMessagesConfig().getString("messages.tables_error")));
            return true;
        }

        try {
            globalConfig.save(globalConfigFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Error al guardar el archivo globalConfig.yml: " + e.getMessage());
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                plugin.getMessagesConfig().getString("messages.tables_error")));
            return true;
        }

        player.sendMessage(ChatColor.translateAlternateColorCodes('&',
            plugin.getMessagesConfig().getString("messages.tables_set"))
            .replace("{tables}", String.join(", ", tables))); // Mensaje dinámico con tablas configuradas

        return true;
    }
}
