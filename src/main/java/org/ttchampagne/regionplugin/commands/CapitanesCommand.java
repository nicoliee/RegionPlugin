package org.ttchampagne.regionplugin.commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class CapitanesCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("capitanes")) {

            // Verificar que el sender sea un jugador
            if (!(sender instanceof Player)) {
                sender.sendMessage("Este comando solo puede ser ejecutado por un jugador.");
                return true;
            }
            Player player = (Player) sender;
            // Verificar permisos
            if (!(player.hasPermission("towers.admin") || player.isOp())) {
                player.sendMessage(ChatColor.RED + "No tienes permiso para usar este comando.");
                return true;
            }
            // Verificar cantidad de argumentos
            if (args.length < 2) {
                player.sendMessage(ChatColor.RED + "Debes especificar al menos dos jugadores.");
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
                player.sendMessage(ChatColor.RED + "No se pudo añadir los capitanes.");
                return true;
            }

            YamlConfiguration config = YamlConfiguration.loadConfiguration(gameSettingsFile);
            config.set("possibleCaptains.activated", "true");
            config.set("possibleCaptains.players", players);

            try {
                config.save(gameSettingsFile);
                player.sendMessage(ChatColor.GREEN + "¡Capitanes actualizados correctamente!");

                // Ejecutar comando para recargar configuración
                String reloadCommand = String.format("tt reloadconfig game_settings %s", worldName);
                player.performCommand(reloadCommand);

            } catch (IOException e) {
                player.sendMessage(ChatColor.RED + "Hubo un error al guardar los capitanes.");
                e.printStackTrace();
            }
            return true;
        }
        return false;
    }
}