package org.ttchampagne.regionplugin.commands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.configuration.file.FileConfiguration;

public class RegionCommand implements CommandExecutor, TabCompleter{
    private final JavaPlugin plugin;

    public RegionCommand(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Este comando solo puede ser ejecutado por jugadores.");
            return true;
        }

        Player player = (Player) sender;
        World world = player.getWorld();
        String worldName = world.getName();
        FileConfiguration config = plugin.getConfig();

        // Verificar permiso "towers.admin"
        if (!player.hasPermission("towers.admin")) {
            player.sendMessage(ChatColor.RED + "No tienes permiso para usar este comando.");
            return true;
        }
        
        if (args.length == 0) {
            player.sendMessage(ChatColor.RED + "Uso: /region <add|min|max|delete|timer>");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "add":
                handleAddCommand(player, config, worldName);
                break;

                case "min":
                    if (args.length == 4) {
                        try {
                            // Parseamos las coordenadas proporcionadas
                            int x = Integer.parseInt(args[1]);
                            int y = Integer.parseInt(args[2]);
                            int z = Integer.parseInt(args[3]);
                            
                            // Llamamos a la función para configurar las coordenadas
                            setCoordinates(config, player, worldName, "P1", x, y, z);
                            plugin.saveConfig();
                            sender.sendMessage(ChatColor.GREEN + "Coordenadas mínimas configuradas.");
                        } catch (NumberFormatException e) {
                            sender.sendMessage(ChatColor.RED + "Las coordenadas deben ser números válidos.");
                        }
                    } else {
                        sender.sendMessage(ChatColor.RED + "Uso: /region min <x> <y> <z>");
                    }
                    break;
                
                case "max":
                    if (args.length == 4) {
                        try {
                            // Parseamos las coordenadas proporcionadas
                            int x = Integer.parseInt(args[1]);
                            int y = Integer.parseInt(args[2]);
                            int z = Integer.parseInt(args[3]);
                            
                            // Llamamos a la función para configurar las coordenadas
                            setCoordinates(config, player, worldName, "P2", x, y, z);
                            plugin.saveConfig();
                            sender.sendMessage(ChatColor.GREEN + "Coordenadas máximas configuradas.");
                        } catch (NumberFormatException e) {
                            sender.sendMessage(ChatColor.RED + "Las coordenadas deben ser números válidos.");
                        }
                    } else {
                        sender.sendMessage(ChatColor.RED + "Uso: /region max <x> <y> <z>");
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
                player.sendMessage(ChatColor.RED + "Comando no reconocido. Uso: /region <add|delete|haste|help|list|max|min|timer>");
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
            player.sendMessage(ChatColor.GREEN + "El mundo ya está configurado en el archivo config.yml.");
            return;
        }

        config.set(worldName + ".P1", "(0,0,0)");
        config.set(worldName + ".P2", "(0,0,0)");
        config.set(worldName + ".Timer", 1);
        config.set(worldName + ".Haste", 1);
        plugin.saveConfig();

        player.sendMessage(ChatColor.GREEN + "Se ha añadido el mundo al archivo config.yml con valores por defecto.");
    }

    private void setCoordinates(FileConfiguration config, Player player, String worldName, String key, int x, int y, int z) {
        // Formateamos las coordenadas
        String formattedCoords = "(" + x + "," + y + "," + z + ")";
        // Guardamos las coordenadas en el archivo de configuración
        config.set(worldName + "." + key, formattedCoords);
    }

    private void handleDeleteCommand(Player player, FileConfiguration config, String worldName) {
        if (!config.contains(worldName)) {
            player.sendMessage(ChatColor.RED + "Este mundo no está configurado.");
            return;
        }

        config.set(worldName, null);
        plugin.saveConfig();

        player.sendMessage(ChatColor.GREEN + "La configuración del mundo ha sido eliminada del archivo config.yml.");
    }

    private void handleTimerCommand(Player player, FileConfiguration config, String worldName, String[] args) {
        if (!config.contains(worldName)) {
            player.sendMessage(ChatColor.RED + "Este mundo no está configurado. Usa /region add primero.");
            return;
        }

        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Uso: /region timer <minutos>");
            return;
        }

        try {
            int timer = Integer.parseInt(args[1]);
            if (timer <= 0) {
                player.sendMessage(ChatColor.RED + "El temporizador debe ser un número positivo.");
                return;
            }

            config.set(worldName + ".Timer", timer);
            plugin.saveConfig();

            player.sendMessage(ChatColor.GREEN + "El temporizador para este mundo ha sido configurado a " + timer + " minutos.");
        } catch (NumberFormatException e) {
            player.sendMessage("Por favor, introduce un número válido.");
        }
    }

    private void handleHasteCommand(Player player, FileConfiguration config, String worldName, String[] args){
        if (!config.contains(worldName)) {
            player.sendMessage(ChatColor.RED + "Este mundo no está configurado. Usa /region add primero.");
            return;
        }

        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Uso: /region haste <minutos>");
            return;
        }

        try {
            int haste = Integer.parseInt(args[1]);
            if (haste <= 0) {
                player.sendMessage(ChatColor.RED + "El temporizador de haste debe ser un número positivo.");
                return;
            }

            config.set(worldName + ".Haste", haste);
            plugin.saveConfig();

            player.sendMessage(ChatColor.GREEN + "El temporizador de haste para este mundo ha sido configurado a " + haste + " minutos.");
        } catch (NumberFormatException e) {
            player.sendMessage("Por favor, introduce un número válido.");
        }
    }

    private void handleListCommand(Player player, FileConfiguration config) {
        if (config.getKeys(false).isEmpty()) {
            player.sendMessage(ChatColor.RED + "No hay mundos configurados en el archivo config.yml.");
            return;
        }
    
        player.sendMessage(ChatColor.YELLOW + "Mundos configurados y sus detalles:");
        for (String worldName : config.getKeys(false)) {
            // Ignorar claves que no sean mundos configurados
            if (worldName.equalsIgnoreCase("commands")) {
                continue;
            }
    
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
        player.sendMessage(ChatColor.GREEN + "Comandos disponibles:");
        player.sendMessage(" " + ChatColor.YELLOW + "/region add " + ChatColor.GRAY + "- Añadir un mundo al archivo config.yml");
        player.sendMessage(" " + ChatColor.YELLOW + "/region delete " + ChatColor.GRAY + "- Eliminar la configuración de un mundo");
        player.sendMessage(" " + ChatColor.YELLOW + "/region haste <minutos> " + ChatColor.GRAY + "- Configurar el temporizador de haste");
        player.sendMessage(" " + ChatColor.YELLOW + "/region list " + ChatColor.GRAY + "- Listar los mundos configurados");
        player.sendMessage(" " + ChatColor.YELLOW + "/region max " + ChatColor.GRAY + "- Configurar las coordenadas máximas");
        player.sendMessage(" " + ChatColor.YELLOW + "/region min " + ChatColor.GRAY + "- Configurar las coordenadas mínimas");
        player.sendMessage(" " + ChatColor.YELLOW + "/region timer <minutos> " + ChatColor.GRAY + "- Configurar el temporizador de protección");
    }
}
