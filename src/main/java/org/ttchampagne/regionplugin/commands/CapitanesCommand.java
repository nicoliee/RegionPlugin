package org.ttchampagne.regionplugin.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.ttchampagne.regionplugin.RegionPlugin;
import org.ttchampagne.utils.SendMessage;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class CapitanesCommand implements CommandExecutor {

    private final RegionPlugin plugin;

    public CapitanesCommand(RegionPlugin plugin) {
        this.plugin = plugin;
    }
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("capitanes")) {
            // Verificar que el sender sea un jugador
            if (!(sender instanceof Player)) {
                sender.sendMessage(plugin.getErrorMessage("errors.no_player"));
                return true;
            }

            Player player = (Player) sender;
            // Verificar permisos
            if (!(player.hasPermission("towers.admin") || player.isOp())) {
                SendMessage.sendToPlayer(player, plugin.getErrorMessage("errors.no_permission"));
                return true;
            }

            // Verificar cantidad de argumentos
            if (args.length < 2) {
                SendMessage.sendToPlayer(player, plugin.getErrorMessage("captains.usage"));
                return true;
            }

            List<String> players = Arrays.asList(args); // Obtener lista de jugadores
            String worldName = player.getWorld().getName(); // Obtener mundo del jugador

            // Ruta al archivo gameSettings.yml
            File gameSettingsFile = new File(Bukkit.getServer().getPluginManager()
                    .getPlugin("AmazingTowers") // Nombre del plugin
                    .getDataFolder() + "/instances/" + worldName + "/gameSettings.yml"); // Ruta al archivo

            // Verificar existencia del archivo
            if (!gameSettingsFile.exists()) {
                SendMessage.sendToPlayer(player, plugin.getErrorMessage("captains.no_settings"));
                return true;
            }

            YamlConfiguration config = YamlConfiguration.loadConfiguration(gameSettingsFile);
            config.set("possibleCaptains.activated", "true");
            config.set("possibleCaptains.players", players);

            try {
                config.save(gameSettingsFile);
                SendMessage.sendToPlayer(player, plugin.getErrorMessage("captains.success"));

                // Ejecutar comando para recargar configuración
                String reloadCommand = String.format("tt reloadconfig game_settings %s", worldName);
                player.performCommand(reloadCommand);

            } catch (IOException e) {
                SendMessage.sendToPlayer(player, plugin.getErrorMessage("captains.error"));
                e.printStackTrace();
            }
            return true;
        }
        return false;
    }
}
