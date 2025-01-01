package org.ttchampagne.regionplugin.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.ttchampagne.regionplugin.RegionPlugin;
import org.ttchampagne.regionplugin.listeners.TorneoListeners;
import org.bukkit.command.TabCompleter;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TorneoCommand implements CommandExecutor, TabCompleter {
    private final RegionPlugin plugin;
    private final TorneoListeners torneoListeners;

    public TorneoCommand(RegionPlugin plugin, TorneoListeners torneoListeners) {
        this.plugin = plugin;
        this.torneoListeners = torneoListeners;
        plugin.getCommand("torneo").setExecutor(this);
    }
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("torneo")) {
            if (sender instanceof Player) {
                Player player = (Player) sender;
    
                // Verificar permiso "towers.admin"
                if (!player.hasPermission("towers.admin")) {
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                            plugin.getMessagesConfig().getString("messages.no_permission")));
                    return true;
                }
    
                String worldName = player.getWorld().getName(); // Obtener el nombre del mundo actual
    
                // Si no se proporcionan argumentos, ejecutar el comando principal
                if (args.length == 0) {
                    executeTournamentCommands(player); // Ejecutar los comandos de torneo
                    return true;
                }
    
                // Manejo de subcomandos
                if (args[0].equalsIgnoreCase("list")) { // /torneo list
                    listTournamentCommands(player); // Mostrar la lista de comandos
                } else if (args[0].equalsIgnoreCase("delete")) {  // /torneo delete {numero}
                    if (args.length == 2) {
                        deleteTournamentCommand(player, args[1]); // Eliminar un comando de la lista
                    } else {
                        player.sendMessage(ChatColor.RED + "/torneo delete {number}");
                    }
                } else if (args[0].equalsIgnoreCase("add")) { // /torneo add {comando}
                    if (args.length >= 2) {
                        addTournamentCommand(player, args); // Añadir un comando a la lista
                    } else {
                        player.sendMessage(ChatColor.RED + "/torneo add {command}");
                    }
                } else if (args[0].equalsIgnoreCase("on")) { // /torneo on
                    handleTournamentOn(player, worldName); // Iniciar la protección de bloques
                } else if (args[0].equalsIgnoreCase("off")) { // /torneo off
                    torneoListeners.stopProtectionTimer(worldName, player); // Detener la protección de bloques
                } else if (args[0].equalsIgnoreCase("help")) { // /torneo help
                    // Mostrar la lista de comandos disponibles
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                            plugin.getMessagesConfig().getString("messages.help_header")));
                    player.sendMessage(" " + ChatColor.YELLOW + "/torneo " + ChatColor.GRAY + "- Ejecuta los comandos de torneo.");
                    player.sendMessage(" " + ChatColor.YELLOW + "/torneo add {comando} " + ChatColor.GRAY + "- Añade un comando a la lista de torneo.");
                    player.sendMessage(" " + ChatColor.YELLOW + "/torneo delete {numero} " + ChatColor.GRAY + "- Elimina un comando de la lista de torneo.");
                    player.sendMessage(" " + ChatColor.YELLOW + "/torneo list " + ChatColor.GRAY + "- Muestra la lista de comandos de torneo.");
                    player.sendMessage(" " + ChatColor.YELLOW + "/torneo off " + ChatColor.GRAY + "- Detiene la protección de bloques en el mundo actual.");
                    player.sendMessage(" " + ChatColor.YELLOW + "/torneo on " + ChatColor.GRAY + "- Inicia la protección de bloques en el mundo actual.");
                } else {
                    // Subcomando no reconocido
                    player.sendMessage(ChatColor.RED + "/torneo <add/delete/list/on/off/help>");
                }
                return true;
            } else {
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                        plugin.getMessagesConfig().getString("messages.no_player")));
                return true;
            }
        }
        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            // Lista de opciones posibles
            List<String> options = Arrays.asList("add", "delete", "list", "off", "on", "help");

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

    
    // Manejar comando /torneo
    private void executeTournamentCommands(Player player) {
        // se revisa en "config.yml" los comandos que debe ejecutar el jugador
        FileConfiguration config = plugin.getConfig();
        List<String> commands = config.getStringList("commands");
        for (String cmd : commands) {
            player.performCommand(cmd);
        }
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                plugin.getMessagesConfig().getString("messages.tournament_rules_activated")));
    }

    // Manejar el comando /torneo list
    private void listTournamentCommands(Player player) {
        FileConfiguration config = plugin.getConfig();
        List<String> commands = config.getStringList("commands");

        if (commands.isEmpty()) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                    plugin.getMessagesConfig().getString("messages.tournament_list_no_commands")));
            return;
        }

        player.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                plugin.getMessagesConfig().getString("messages.tournament_list_commands")));
        for (int i = 0; i < commands.size(); i++) {
            player.sendMessage(" " + ChatColor.GREEN + String.valueOf(i + 1) + ". " + ChatColor.AQUA + commands.get(i));
        }
    }

    // Manejar el comando /torneo delete
    private void deleteTournamentCommand(Player player, String number) {
        try {
            // Convertir el número a un índice
            int index = Integer.parseInt(number) - 1; // Restar 1 para obtener el índice correcto
            FileConfiguration config = plugin.getConfig(); // Cargar el archivo de configuración
            List<String> commands = config.getStringList("commands"); // Obtener la lista de comandos

            if (index < 0 || index >= commands.size()) {
                player.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                        plugin.getMessagesConfig().getString("messages.tournament_delete_invalid_number:")));
                return;
            }

            String removedCommand = commands.remove(index); // Eliminar el comando de la lista
            config.set("commands", commands); // Actualizar la lista de comandos
            plugin.saveConfig(); // Guardar el archivo de configuración

            player.sendMessage(ChatColor.translateAlternateColorCodes('&', 
            plugin.getMessagesConfig().getString("messages.tournament_delete_command_deleted")
            .replace("{command}", removedCommand)));
        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                    plugin.getMessagesConfig().getString("messages.tournament_delete_invalid_number_usage")));
        }
    }

    // Manejar el comando /torneo add
    private void addTournamentCommand(Player player, String[] args) {
        FileConfiguration config = plugin.getConfig();
        List<String> commands = config.getStringList("commands");

        // Unir los argumentos como un comando
        StringBuilder commandBuilder = new StringBuilder();
        for (int i = 1; i < args.length; i++) {
            commandBuilder.append(args[i]).append(" ");
        }
        String newCommand = commandBuilder.toString().trim();

        // Agregar el comando y guardar
        commands.add(newCommand);
        config.set("commands", commands);
        plugin.saveConfig();

        player.sendMessage(ChatColor.translateAlternateColorCodes('&', 
            plugin.getMessagesConfig().getString("messages.tournament_add_command_added")
            .replace("{command}", newCommand)));
    }

    // Manejar el comando /torneo on
    private void handleTournamentOn(Player player, String worldName) {
        torneoListeners.setPrivateMode(worldName, true); // Activar el modo privado
        // Cargar el archivo de configuración
        File configFile = new File(plugin.getDataFolder(), "config.yml");
        FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        // Obtener el tiempo de preparación desde el config.yml
        int preparationTime = 4; // Valor predeterminado de 4 minutos
        int hasteTime = 2; // Valor predeterminado de 2 minutos
        if (config.contains(worldName + ".Timer")) {
            preparationTime = config.getInt(worldName + ".Timer"); // Obtener el tiempo de preparación
        } else {
            String message = plugin.getMessagesConfig().getString("messages.tournament_on_no_preparation_time");
            message = message.replace("{mins}", String.valueOf(preparationTime)); // Reemplazar el marcador {mins}
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
        }
        if (config.contains(worldName + ".Haste")) {
            hasteTime = config.getInt(worldName + ".Haste"); // Obtener el tiempo de haste
        } else {
            String message = plugin.getMessagesConfig().getString("messages.tournament_on_no_haste_time");
            message = message.replace("{mins}", String.valueOf(hasteTime)); // Reemplazar el marcador {mins}
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
        }
        preparationTime *= 60; // Convertir minutos a segundos
        hasteTime *= 60; // Convertir minutos a segundos
        torneoListeners.startProtectionTimer(worldName, player, preparationTime, hasteTime); // Iniciar el temporizador de protección
    }
    
}