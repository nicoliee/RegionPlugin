package org.ttchampagne.regionplugin.commands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.configuration.file.FileConfiguration;
import org.ttchampagne.regionplugin.RegionPlugin;
import org.ttchampagne.regionplugin.Region;
public class RegionCommand implements CommandExecutor, TabCompleter{
    private final JavaPlugin plugin;
    public RegionCommand(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    if (!(sender instanceof Player)) {
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    ((RegionPlugin) plugin).getMessagesConfig().getString("messages.no_player")));
        return true;
    }

    Player player = (Player) sender;
    World world = player.getWorld();
    String worldName = world.getName();
    FileConfiguration config = plugin.getConfig();

    if (!player.hasPermission("towers.admin")) {
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    ((RegionPlugin) plugin).getMessagesConfig().getString("messages.no_permission")));
        return true;
    }
    
    if (args.length == 0) {
        player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                ((RegionPlugin) plugin).getMessagesConfig().getString("messages.help_header")));
                player.sendMessage(ChatColor.RED + " /region <add|min|max|delete|timer>");
        return true;
    }

    switch (args[0].toLowerCase()) {
        case "add":
            handleAddCommand(player, config, worldName);
            break;

        case "min":
        if (!config.contains(worldName)) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    ((RegionPlugin) plugin).getMessagesConfig().getString("messages.region_world_not_configured")));
        }
        if (args.length == 4) {
            try {
                int x = Integer.parseInt(args[1]);
                int y = Integer.parseInt(args[2]);
                int z = Integer.parseInt(args[3]);
                setCoordinates(config, player, worldName, "P1", x, y, z);

                // Actualizar la región en memoria
                Region region = ((RegionPlugin) plugin).getRegions().get(worldName); // Obtener la región actual
                if (region != null) {
                    // Actualizar la coordenada mínima de la región en memoria
                    region.setP1(new Location(player.getWorld(), x, y, z));
                }

                plugin.saveConfig(); // Guardar la configuración en el archivo
                player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                        ((RegionPlugin) plugin).getMessagesConfig().getString("messages.region_min_coords_set"))
                        .replace ("{x}", String.valueOf(x))
                        .replace ("{y}", String.valueOf(y))
                        .replace ("{z}", String.valueOf(z)));
            } catch (NumberFormatException e) {
                player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                        ((RegionPlugin) plugin).getMessagesConfig().getString("messages.region_invalid_number")));
            }
        } else {
            player.sendMessage(ChatColor.RED + "/region min <x> <y> <z>");
        }
        break;

    case "max":
        if (!config.contains(worldName)) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    ((RegionPlugin) plugin).getMessagesConfig().getString("messages.region_world_not_configured")));
        }
        if (args.length == 4) {
            try {
                int x = Integer.parseInt(args[1]);
                int y = Integer.parseInt(args[2]);
                int z = Integer.parseInt(args[3]);
                setCoordinates(config, player, worldName, "P2", x, y, z);

                // Actualizar la región en memoria
                Region region = ((RegionPlugin) plugin).getRegions().get(worldName); // Obtener la región actual
                if (region != null) {
                    // Actualizar la coordenada máxima de la región en memoria
                    region.setP2(new Location(player.getWorld(), x, y, z));
                }
                plugin.saveConfig(); // Guardar la configuración en el archivo
                player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                        ((RegionPlugin) plugin).getMessagesConfig().getString("messages.region_max_coords_set"))
                        .replace ("{x}", String.valueOf(x))
                        .replace ("{y}", String.valueOf(y))
                        .replace ("{z}", String.valueOf(z)));
            } catch (NumberFormatException e) {
                player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                        ((RegionPlugin) plugin).getMessagesConfig().getString("messages.region_invalid_number")));
            }
        } else {
            player.sendMessage(ChatColor.RED + "/region max <x> <y> <z>");
        }
        break;

        case "delete":
            handleDeleteCommand(player, config, worldName);
            break;

        case "timer":
            handleTimerCommand(player, config, worldName, args);
            break;

        case "haste":
            handleHasteCommand(player, config, worldName, args);
            break;

        case "list":
            handleListCommand(player, config);
            break;

        case "help":
            handleHelpCommand(player);
            break;

        default:
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    ((RegionPlugin) plugin).getMessagesConfig().getString("messages.help_header")));
                    player.sendMessage(ChatColor.RED + "/region <add|delete|haste|help|list|max|min|timer>");
    }
    return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            // Lista de opciones posibles
            List<String> options = Arrays.asList("add", "delete", "haste", "help", "list", "max", "min", "timer");

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
        return null;
    }

    private void handleAddCommand(Player player, FileConfiguration config, String worldName) {
    if (config.contains(worldName)) {
        player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                ((RegionPlugin) plugin).getMessagesConfig().getString("messages.region_add_world_already_configured")));
        return;
    }

    config.set(worldName + ".P1", "(0,0,0)");
    config.set(worldName + ".P2", "(0,0,0)");
    config.set(worldName + ".Timer", 1);
    config.set(worldName + ".Haste", 1);
    plugin.saveConfig();

    player.sendMessage(ChatColor.translateAlternateColorCodes('&',
            ((RegionPlugin) plugin).getMessagesConfig().getString("messages.region_add_world_added")));
    }

    private void setCoordinates(FileConfiguration config, Player player, String worldName, String key, int x, int y, int z) {
        // Formateamos las coordenadas
        String formattedCoords = "(" + x + "," + y + "," + z + ")";
        // Guardamos las coordenadas en el archivo de configuración
        config.set(worldName + "." + key, formattedCoords);
    }

    private void handleDeleteCommand(Player player, FileConfiguration config, String worldName) {
        if (!config.contains(worldName)) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    ((RegionPlugin) plugin).getMessagesConfig().getString("messages.region_world_not_configured")));
            return;
        }
    
        config.set(worldName, null);
        plugin.saveConfig();
    
        player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                ((RegionPlugin) plugin).getMessagesConfig().getString("messages.region_delete_world_deleted")));
    }

    private void handleTimerCommand(Player player, FileConfiguration config, String worldName, String[] args) {
        if (!config.contains(worldName)) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    ((RegionPlugin) plugin).getMessagesConfig().getString("messages.region_world_not_configured")));
            return;
        }
    
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "/region timer {mins}");
            return;
        }
    
        try {
            int timer = Integer.parseInt(args[1]);
            if (timer <= 0) {
                player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                        ((RegionPlugin) plugin).getMessagesConfig().getString("messages.region_invalid_number")));
                return;
            }
    
            config.set(worldName + ".Timer", timer);
            plugin.saveConfig();
    
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    ((RegionPlugin) plugin).getMessagesConfig().getString("messages.region_timer_set_success").replace("{timer}", String.valueOf(timer))));
        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    ((RegionPlugin) plugin).getMessagesConfig().getString("messages.region_invalid_number")));
        }
    }

    private void handleHasteCommand(Player player, FileConfiguration config, String worldName, String[] args) {
        if (!config.contains(worldName)) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    ((RegionPlugin) plugin).getMessagesConfig().getString("messages.region_world_not_configured")));
            return;
        }
    
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "/region haste {mins}");
            return;
        }
    
        try {
            int haste = Integer.parseInt(args[1]);
            if (haste <= 0) {
                player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                        ((RegionPlugin) plugin).getMessagesConfig().getString("messages.region_invalid_number")));
                return;
            }
    
            config.set(worldName + ".Haste", haste);
            plugin.saveConfig();
    
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    ((RegionPlugin) plugin).getMessagesConfig().getString("messages.region_haste_set_success").replace("{haste}", String.valueOf(haste))));
        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    ((RegionPlugin) plugin).getMessagesConfig().getString("messages.region_invalid_number")));
        }
    }

    private void handleListCommand(Player player, FileConfiguration config) {
        if (config.getKeys(false).isEmpty()) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    ((RegionPlugin) plugin).getMessagesConfig().getString("messages.regionno_configured_worlds")));
            return;
        }
    
        player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    ((RegionPlugin) plugin).getMessagesConfig().getString("messages.region_configured_worlds_header")));
        for (String worldName : config.getKeys(false)) {
            // Ignorar claves que no sean mundos configurados
            if (worldName.equalsIgnoreCase("commands")) {
                continue;
            }
            
            //TODO agregar mensajes a messages.yml
            String p1 = config.getString(worldName + ".P1", "No definido");
            String p2 = config.getString(worldName + ".P2", "No definido");
            int timer = config.getInt(worldName + ".Timer", -1);
            int haste = config.getInt(worldName + ".Haste", -1);
    
            player.sendMessage(ChatColor.GREEN + worldName);
            player.sendMessage(ChatColor.AQUA + "  Coordenadas mínimas: " + ChatColor.WHITE + p1);
            player.sendMessage(ChatColor.AQUA + "  Coordenadas máximas: " + ChatColor.WHITE + p2);
            player.sendMessage(ChatColor.AQUA + "  Temporizador: " + ChatColor.WHITE + (timer > 0 ? timer + " minutos" : "No definido"));
            player.sendMessage(ChatColor.AQUA + "  Temporizador de haste: " + ChatColor.WHITE + (haste > 0 ? haste + " minutos" : "No definido"));
        }
    }
    private void handleHelpCommand(Player player){
        player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    ((RegionPlugin) plugin).getMessagesConfig().getString("messages.help_header")));
        player.sendMessage(" " + ChatColor.YELLOW + "/region add " + ChatColor.GRAY + "- Añadir un mundo al archivo config.yml");
        player.sendMessage(" " + ChatColor.YELLOW + "/region delete " + ChatColor.GRAY + "- Eliminar la configuración de un mundo");
        player.sendMessage(" " + ChatColor.YELLOW + "/region haste <minutos> " + ChatColor.GRAY + "- Configurar el temporizador de haste");
        player.sendMessage(" " + ChatColor.YELLOW + "/region list " + ChatColor.GRAY + "- Listar los mundos configurados");
        player.sendMessage(" " + ChatColor.YELLOW + "/region max " + ChatColor.GRAY + "- Configurar las coordenadas máximas");
        player.sendMessage(" " + ChatColor.YELLOW + "/region min " + ChatColor.GRAY + "- Configurar las coordenadas mínimas");
        player.sendMessage(" " + ChatColor.YELLOW + "/region timer <minutos> " + ChatColor.GRAY + "- Configurar el temporizador de protección");
    }
}
