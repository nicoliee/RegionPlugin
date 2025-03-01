package org.ttchampagne.regionplugin.commands;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.ttchampagne.regionplugin.RegionPlugin;
import org.ttchampagne.regionplugin.listeners.TorneoListeners;
import org.ttchampagne.utils.SendMessage;
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
                    SendMessage.sendToPlayer(player, plugin.getErrorMessage("messages.no_permission"));
                    return true;
                }
    
                String worldName = player.getWorld().getName(); // Obtener el nombre del mundo actual
    
                // Si no se proporcionan argumentos, ejecutar el comando principal
                if (args.length == 0) {
                    checkForStart(player, worldName); // Verificar si el jugador tiene un casco de cuero
                    executeTournamentCommands(player, worldName); // Ejecutar los comandos de torneo
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
                }else if (args[0].equalsIgnoreCase("finishAdd")) { // /torneo finishAdd {comando}
                                    if (args.length >= 2) {
                        handleTournamentFinishAdd(player, args); // Añadir un comando a la lista de finalización
                    } else {
                        player.sendMessage(ChatColor.RED + "/torneo finishAdd {command}");
                    }
                } else if (args[0].equalsIgnoreCase("finishDelete")) { // /torneo finishDelete {numero}
                    if (args.length == 2) {
                        handleTournamentFinishDelete(player, args); // Eliminar un comando de la lista de finalización
                    } else {
                        player.sendMessage(ChatColor.RED + "/torneo finishDelete {number}");
                    }
                } else if (args[0].equalsIgnoreCase("off")) { // /torneo off
                    torneoListeners.stopProtectionTimer(worldName, player); // Detener la protección de bloques

                } else if (args[0].equalsIgnoreCase("help")) { // /torneo help
                    // Mostrar la lista de comandos disponibles
                    player.sendMessage(" " + ChatColor.YELLOW + "/torneo " + ChatColor.GRAY + "- Ejecuta los comandos de torneo.");
                    player.sendMessage(" " + ChatColor.YELLOW + "/torneo add {comando} " + ChatColor.GRAY + "- Añade un comando a la lista de torneo.");
                    player.sendMessage(" " + ChatColor.YELLOW + "/torneo delete {numero} " + ChatColor.GRAY + "- Elimina un comando de la lista de torneo.");
                    player.sendMessage(" " + ChatColor.YELLOW + "/torneo finishAdd {comando} " + ChatColor.GRAY + "- Añade un comando a la lista de finalización.");
                    player.sendMessage(" " + ChatColor.YELLOW + "/torneo finishDelete {numero} " + ChatColor.GRAY + "- Elimina un comando de la lista de finalización.");
                    player.sendMessage(" " + ChatColor.YELLOW + "/torneo list " + ChatColor.GRAY + "- Muestra la lista de comandos de torneo.");
                    player.sendMessage(" " + ChatColor.YELLOW + "/torneo off " + ChatColor.GRAY + "- Detiene la protección de bloques en el mundo actual.");
                } else {
                    // Subcomando no reconocido
                    player.sendMessage(ChatColor.RED + "/torneo <add/delete/list/off/help>");
                }
                return true;
            } else {
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                        plugin.getErrorMessage("errors.no_player")));
                return true;
            }
        }
        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            // Lista de opciones posibles
            List<String> options = Arrays.asList("add", "delete", "finishAdd", "finishDelete", "list", "off", "help");

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
    private void executeTournamentCommands(Player player, String worldName) {
        // se revisa en "config.yml" los comandos que debe ejecutar el jugador
        FileConfiguration config = plugin.getConfig();
        List<String> commands = config.getStringList("commands");
        if (!config.contains(worldName)) {
            SendMessage.sendToPlayer(player, plugin.getErrorMessage("region.worldError"));
            return;
        }
        for (String cmd : commands) {
            player.performCommand(cmd);
        }
        SendMessage.sendToPlayer(player, plugin.getErrorMessage("tournament.start"));
    }

    // Manejar el comando /torneo list
    private void listTournamentCommands(Player player) {
        FileConfiguration config = plugin.getConfig();
        List<String> commands = config.getStringList("commands");
        if (commands.isEmpty()) {
            SendMessage.sendToPlayer(player, plugin.getErrorMessage("tournament.empty"));
            return;
        }

        SendMessage.sendToPlayer(player, plugin.getErrorMessage("tournament.list"));
        for (int i = 0; i < commands.size(); i++) {
            player.sendMessage(" " + ChatColor.GREEN + String.valueOf(i + 1) + ". " + ChatColor.AQUA + commands.get(i));
        }
        List<String> finishCommands = config.getStringList("finishCommands");
        if (finishCommands.isEmpty()) {
            SendMessage.sendToPlayer(player, plugin.getErrorMessage("tournament.empty"));
            return;
        }
        SendMessage.sendToPlayer(player, plugin.getErrorMessage("tournament.finishList"));
        for (int i = 0; i < finishCommands.size(); i++) {
            player.sendMessage(" " + ChatColor.GREEN + String.valueOf(i + 1) + ". " + ChatColor.AQUA + finishCommands.get(i));
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
                SendMessage.sendToPlayer(player, plugin.getErrorMessage("tournament.usage"));
                return;
            }

            String removedCommand = commands.remove(index); // Eliminar el comando de la lista
            config.set("commands", commands); // Actualizar la lista de comandos
            plugin.saveConfig(); // Guardar el archivo de configuración

            SendMessage.sendToPlayer(player, plugin.getErrorMessage("tournament.delete")
                    .replace("{command}", removedCommand));
        } catch (NumberFormatException e) {
            SendMessage.sendToPlayer(player, plugin.getErrorMessage("tournament.usage"));
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

        SendMessage.sendToPlayer(player, plugin.getErrorMessage("tournament.add")
                .replace("{command}", newCommand));
    }

    // Manejar el comando /torneo finishAdd
    private void handleTournamentFinishAdd(Player player, String[] args) {
        FileConfiguration config = plugin.getConfig();
        List<String> commands = config.getStringList("finishCommands");

        // Unir los argumentos como un comando
        StringBuilder commandBuilder = new StringBuilder();
        for (int i = 1; i < args.length; i++) {
            commandBuilder.append(args[i]).append(" ");
        }
        String newCommand = commandBuilder.toString().trim();

        // Agregar el comando y guardar
        commands.add(newCommand);
        config.set("finishCommands", commands);
        plugin.saveConfig();

        SendMessage.sendToPlayer(player, plugin.getErrorMessage("tournament.add")
                .replace("{command}", newCommand));
    }

    // Manejar el comando /torneo finishDelete
    private void handleTournamentFinishDelete(Player player, String[] args) {
        try {
            // Convertir el número a un índice
            int index = Integer.parseInt(args[1]) - 1; // Restar 1 para obtener el índice correcto
            FileConfiguration config = plugin.getConfig(); // Cargar el archivo de configuración
            List<String> commands = config.getStringList("finishCommands"); // Obtener la lista de comandos

            if (index < 0 || index >= commands.size()) {
                SendMessage.sendToPlayer(player, plugin.getErrorMessage("tournament.usage"));
                return;
            }

            String removedCommand = commands.remove(index); // Eliminar el comando de la lista
            config.set("finishCommands", commands); // Actualizar la lista de comandos
            plugin.saveConfig(); // Guardar el archivo de configuración

            SendMessage.sendToPlayer(player, plugin.getErrorMessage("tournament.delete")
                    .replace("{command}", removedCommand));
        } catch (NumberFormatException e) {
            SendMessage.sendToPlayer(player, plugin.getErrorMessage("tournament.usage"));
        }
    }

    private void checkForStart(Player player, String worldName) {
        new BukkitRunnable() {
            private int elapsedTime = 0;
            @Override
            public void run() {
                if (!player.isOnline()) { // Si el jugador se desconecta, detener el Runnable
                    this.cancel();
                    return;
                }

                if (elapsedTime >= 36000) { // 30 minutos (36000 ticks)
                    this.cancel();
                    return;
                }
                ItemStack helmet = player.getInventory().getHelmet();
                if (helmet != null && helmet.getType() == Material.LEATHER_HELMET) {
                    handleTournamentOn(player, worldName);
                    this.cancel(); // Detener la tarea una vez que se ha ejecutado
                }
                elapsedTime += 10; // Incrementar el tiempo transcurrido
            }
        }.runTaskTimer(plugin, 0, 10); // Ejecutar cada 10 ticks (0.5 segundos)
    }    

    // Manejar el comando /torneo on
    private void handleTournamentOn(Player player, String worldName) {
        // Cargar el archivo de configuración
        File configFile = new File(plugin.getDataFolder(), "config.yml");
        FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        
        // Obtener el tiempo de preparación desde el config.yml
        int preparationTime = 4; // Valor predeterminado de 4 minutos
        int hasteTime = 2; // Valor predeterminado de 2 minutos
        // Verificar si el mundo está configurado
        
        torneoListeners.setPrivateMode(worldName, true); // Activar el modo privado
        if (config.contains(worldName + ".Timer")) {
            preparationTime = config.getInt(worldName + ".Timer"); // Obtener el tiempo de preparación
        } else {
            SendMessage.sendToPlayer(player, plugin.getErrorMessage("tournament.defaultTimer"));
        }
        if (config.contains(worldName + ".Haste")) {
            hasteTime = config.getInt(worldName + ".Haste"); // Obtener el tiempo de haste
        } else {
            SendMessage.sendToPlayer(player, plugin.getErrorMessage("tournament.defaultHaste"));
        }
        preparationTime = preparationTime * 60 + 1; // Convertir minutos a segundos y sumar 1 segundo
        hasteTime = hasteTime * 60 + 1; // Convertir minutos a segundos y sumar 1 segundo
        torneoListeners.startProtectionTimer(worldName, player, preparationTime, hasteTime); // Iniciar el temporizador de protección
        player.performCommand("tt timer true");
    }
}