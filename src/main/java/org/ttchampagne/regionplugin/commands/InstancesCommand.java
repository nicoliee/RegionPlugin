package org.ttchampagne.regionplugin.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.ttchampagne.regionplugin.RegionPlugin;
import org.ttchampagne.utils.SendMessage;

import java.io.File;
import java.io.IOException;

public class InstancesCommand implements CommandExecutor {
    private final RegionPlugin plugin;

    public InstancesCommand(RegionPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("instances")) {
            return false;
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getErrorMessage("errors.no_player"));
            return true;
        }

        Player player = (Player) sender;

        if (!(player.hasPermission("towers.admin") || player.isOp())) {
            SendMessage.sendToPlayer(player, plugin.getErrorMessage("errors.no_permission"));
            return true;
        }

        if (args.length < 1) {
            SendMessage.sendToPlayer(player, plugin.getErrorMessage("instances.usage"));
            return true;
        }

        String input = args[0];
        if (!input.matches("\\d+")) { // Verifica que el argumento sea un número entero positivo
            SendMessage.sendToPlayer(player, plugin.getErrorMessage("instances.usage"));
            return true;
        }

        int number = Integer.parseInt(input);

        // Carga el archivo globalConfig.yml
        File configFile = new File(plugin.getDataFolder().getParentFile(), "AmazingTowers/globalConfig.yml");
        if (!configFile.exists()) {
            SendMessage.sendToPlayer(player, plugin.getErrorMessage("errors.file_not_found"));
            return true;
        }
        FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);

        // Actualiza el valor en el archivo YAML
        config.set("options.instances.amount", number);

        try {
            config.save(configFile); // Guarda los cambios en el archivo
            SendMessage.sendToPlayer(player, plugin.getErrorMessage("instances.set").replace("{instances}", String.valueOf(number)));
        } catch (IOException e) {
            SendMessage.sendToPlayer(player, plugin.getErrorMessage("instances.error"));
            e.printStackTrace();
        }

        return true;
    }
}
