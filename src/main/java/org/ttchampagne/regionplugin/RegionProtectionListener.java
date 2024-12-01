package org.ttchampagne.regionplugin;

import org.ttchampagne.regionplugin.update.AutoUpdate;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RegionProtectionListener implements Listener, CommandExecutor {
    private final RegionPlugin plugin;
    // Mapas para almacenar el estado de protección de bloques por mundo
    private final Map<String, Boolean> worldProtectionStatus = new HashMap<>();
    private final Map<String, BukkitRunnable> protectionTimers = new HashMap<>();
    private final Map<String, Boolean> globalProtectionStatus = new HashMap<>(); // Mapa para la protección global
    private final Map<String, Integer> protectionTimeRemaining = new HashMap<>(); // Mapa para almacenar el tiempo restante de protección de bloques
    // Mapas para almacenar el tiempo restante de los efectos de regeneración y Haste II
    private final Map<String, Integer> regenerationTimerRemaining = new HashMap<>(); // Mapa para almacenar el tiempo restante de la regeneración
    private final Map<String, Integer> hasteTimerRemaining = new HashMap<>(); // Mapa para guardar el tiempo restante de Haste II

    public RegionProtectionListener(RegionPlugin plugin) {
        this.plugin = plugin;
        plugin.getCommand("torneo").setExecutor(this);
    }

    @EventHandler
    // Logica para la protección de bloques
    public void onBlockPlace(BlockPlaceEvent event) {
        String worldName = event.getBlock().getWorld().getName();

        // Verificar si la protección global está activa
        if (globalProtectionStatus.getOrDefault(worldName, false)) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(ChatColor.RED + "No puedes poner bloques hasta que inicie la partida.");
            return;
        }

        // Lógica original de protección por región
        Boolean isProtectionActive = worldProtectionStatus.get(worldName);
        if (isProtectionActive != null && isProtectionActive) {
            Region region = plugin.getRegions().get(worldName);
            if (region != null && region.isInside(event.getBlock().getLocation())) {
                event.setCancelled(true);
                event.getPlayer().sendMessage(ChatColor.YELLOW + "No puedes construir en tiempo de preparación.");
            }
        }
    }

    @EventHandler
    // Logica para la destrucción de bloques
    public void onBlockBreak(BlockBreakEvent event) {
        String worldName = event.getBlock().getWorld().getName();

        // Verificar si la protección global está activa
        if (globalProtectionStatus.getOrDefault(worldName, false)) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(ChatColor.RED + "No puedes romper bloques hasta que inicie la partida.");
            return;
        }

        // Lógica original de protección por región
        Boolean isProtectionActive = worldProtectionStatus.get(worldName);
        if (isProtectionActive != null && isProtectionActive) {
            Region region = plugin.getRegions().get(worldName);
            if (region != null && region.isInside(event.getBlock().getLocation())) {
                event.setCancelled(true);
                event.getPlayer().sendMessage(ChatColor.YELLOW + "No puedes romper bloques en esta región.");
            }
        }
    }

    @EventHandler
    // Logica para la extensión de pistones
    public void onPistonExtend(BlockPistonExtendEvent event) {
        String worldName = event.getBlock().getWorld().getName();
        Boolean isProtectionActive = worldProtectionStatus.get(worldName);

        if (isProtectionActive != null && isProtectionActive) {
            Region region = plugin.getRegions().get(worldName);

            for (org.bukkit.block.Block block : event.getBlocks()) {
                if (region != null && region.isInside(block.getLocation())) {
                    event.setCancelled(true);
                    break;
                }
            }
        }
    }

    @EventHandler
    // Logica para la retracción de pistones
    public void onPistonRetract(BlockPistonRetractEvent event) {
        String worldName = event.getBlock().getWorld().getName();
        Boolean isProtectionActive = worldProtectionStatus.get(worldName);

        if (isProtectionActive != null && isProtectionActive) {
            Region region = plugin.getRegions().get(worldName);

            for (org.bukkit.block.Block block : event.getBlocks()) {
                if (region != null && region.isInside(block.getLocation())) {
                    event.setCancelled(true);
                    break;
                }
            }
        }
    }

    @EventHandler
    // Que debe pasar cuando el jugador muera dentro del tiempo de preparación
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        String worldName = player.getWorld().getName();
        // Verifica si hay un valor de protección para el mundo y si está activo
        Boolean isProtectionActive = worldProtectionStatus.get(worldName);
        if (isProtectionActive == null || !isProtectionActive) {
            return; // Si no hay protección activa, no hacer nada
        }
        int remainingTime = protectionTimeRemaining.getOrDefault(worldName, 0);
        if (remainingTime > 0) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    applyRegenerationEffect(worldName, remainingTime); // Aplicar Regeneración

                    // Verificar si queda tiempo de Haste II
                    int remainingHasteTime = hasteTimerRemaining.getOrDefault(worldName, 0);
                    if (remainingHasteTime > 0) {
                        applyHasteEffect(worldName, remainingHasteTime); // Aplicar el tiempo restante de Haste II
                    }
                }
            }.runTaskLater(plugin, 20L); // Esperar un segundo antes de aplicar los efectos
        }
    }



    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("torneo")) {
            if (sender instanceof Player) {
                Player player = (Player) sender;

                // Verificar permiso "towers.admin"
                if (!player.hasPermission("towers.admin")) {
                    player.sendMessage(ChatColor.RED + "No tienes permiso para usar este comando.");
                    return true;
                }

                String worldName = player.getWorld().getName(); // Obtener el nombre del mundo actual

                // Si no se proporcionan argumentos, ejecutar el comando principal
                if (args.length == 0) {
                    globalProtectionStatus.put(worldName, true); // Activar protección previa al juego
                    executeTournamentCommands(player); // Ejecutar los comandos de torneo
                    return true;
                }

                // Manejo de subcomandos
                if (args[0].equalsIgnoreCase("lista")) { // /torneo lista
                    listTournamentCommands(player); // Mostrar la lista de comandos
                } else if (args[0].equalsIgnoreCase("delete")) {  // /torneo delete {numero}
                    if (args.length == 2) {
                        deleteTournamentCommand(player, args[1]); // Eliminar un comando de la lista
                    } else {
                        player.sendMessage(ChatColor.RED + "Uso incorrecto: /torneo delete {numero}");
                    }
                } else if (args[0].equalsIgnoreCase("add")) { // /torneo add {comando}
                    if (args.length >= 2) {
                        addTournamentCommand(player, args); // Añadir un comando a la lista
                    } else {
                        player.sendMessage(ChatColor.RED + "Uso incorrecto: /torneo add {comando}");
                    }
                } else if (args[0].equalsIgnoreCase("on")) { // /torneo on [tiempo]
                    handleTournamentOn(player, args, worldName); // Iniciar la protección de bloques
                } else if (args[0].equalsIgnoreCase("off")) { // /torneo off
                    stopProtectionTimer(worldName, player); // Detener la protección de bloques
                } else if (args[0].equalsIgnoreCase("timer")) {// /torneo timer
                    if (args.length >= 2) {
                        try {
                            int newPreparationTime = Integer.parseInt(args[1]);
                            updatePreparationTime(newPreparationTime, player, worldName);
                        } catch (NumberFormatException e) {
                            player.sendMessage(ChatColor.RED + "El tiempo de preparación debe ser un número.");
                        }
                    } else {
                        player.sendMessage(ChatColor.RED + "Uso incorrecto: /torneo timer {tiempo}");
                    }
                }else if (args[0].equalsIgnoreCase("update")) { // /torneo update
                    // Mostrar la versión actual del plugin
                    String currentVersion = plugin.getDescription().getVersion();
                    sender.sendMessage("§aLa versión actual de RegionPlugin es: §b" + currentVersion);
                
                    // Iniciar la verificación de actualizaciones
                    AutoUpdate updateChecker = new AutoUpdate(plugin); // Si necesita el plugin como parámetro
                    updateChecker.checkForUpdates();
                }else if (args[0].equalsIgnoreCase("help")) { // /torneo help
                    // Mostrar la lista de comandos disponibles
                    player.sendMessage(ChatColor.YELLOW + "Comandos disponibles:");
                    player.sendMessage(ChatColor.GREEN + "/torneo: Ejecuta los comandos de torneo.");
                    player.sendMessage(ChatColor.GREEN + "/torneo lista: Muestra la lista de comandos de torneo.");
                    player.sendMessage(ChatColor.GREEN + "/torneo add {comando}: Añade un comando a la lista de torneo.");
                    player.sendMessage(ChatColor.GREEN + "/torneo delete {numero}: Elimina un comando de la lista de torneo.");
                    player.sendMessage(ChatColor.GREEN + "/torneo on [tiempo]: Inicia la protección de bloques en el mundo actual.");
                    player.sendMessage(ChatColor.GREEN + "/torneo off: Detiene la protección de bloques en el mundo actual.");
                    player.sendMessage(ChatColor.GREEN + "/torneo timer {tiempo}: Actualiza el tiempo de preparación en el mundo actual.");
                    player.sendMessage(ChatColor.GREEN + "/torneo update: Verifica y descarga una nueva versión del plugin.");
                }else {
                    // Subcomando no reconocido
                    player.sendMessage(ChatColor.RED + "Uso incorrecto del comando. Usa /torneo help para ver los comandos disponibles.");
                }
                return true;
            } else {
                sender.sendMessage("Este comando solo puede ser ejecutado por jugadores.");
                return true;
            }
        }
        return false;
    }

    // Manejar comando /torneo
    private void executeTournamentCommands(Player player) {
        // se revisa en "config.yml" los comandos que debe ejecutar el jugador
        FileConfiguration config = plugin.getConfig();
        List<String> commands = config.getStringList("commands");
        for (String cmd : commands) {
            player.performCommand(cmd);
        }
        player.sendMessage(ChatColor.YELLOW + "Reglas de Torneo Activadas.");
    }

    // Iniciar la protección de bloques
    private void startProtectionTimer(final String worldName, Player player, int preparationTime) {
        if (protectionTimers.containsKey(worldName)) {
            player.sendMessage(ChatColor.YELLOW + "La protección ya está activa en este mundo.");
            return;
        }
        worldProtectionStatus.put(worldName, true);
        protectionTimeRemaining.put(worldName, preparationTime); // Guardar el tiempo de protección restante
        // Determinar duración para Regeneración y Haste II
        regenerationTimerRemaining.put(worldName, preparationTime); // Tiempo completo para Regeneración
        int hasteDuration = Math.min(90, preparationTime); // Haste II se aplica por un máximo de 90 segundos
        hasteTimerRemaining.put(worldName, hasteDuration);
        // Aplicar efectos
        applyRegenerationEffect(worldName, preparationTime); // Aplicar Regeneración a todos los jugadores
        applyHasteEffect(worldName, hasteDuration); // Aplicar Haste II solo por la duración calculada
        BukkitRunnable task = new BukkitRunnable() {
            private int secondsElapsed = 0;
            @Override
            public void run() {
                secondsElapsed++;
                int timeRemaining = preparationTime - secondsElapsed;
                protectionTimeRemaining.put(worldName, timeRemaining); // Actualizar el tiempo restante

                if (timeRemaining > 0) {
                    if (timeRemaining % 60 == 0) {
                        // Si el tiempo es divisible dentro de 60 y es mayor a 0 mostrará un mensaje con el tiempo faltante
                        int minutesRemaining = timeRemaining / 60;
                        sendMessageToWorldPlayers(worldName, "Quedan " + minutesRemaining + " minutos hasta que termine el tiempo de preparación.");
                    } else if (timeRemaining == 30 || timeRemaining == 10 || (timeRemaining <= 5 && timeRemaining >= 1)) {
                        // Si el tiempo restante son 30, 10, 5, 4, 3, 2, 1 segundos mostrará un mensaje
                        sendMessageToWorldPlayers(worldName, "Quedan " + timeRemaining + " segundos hasta que termine el tiempo de preparación.");
                    }
                } else {
                    // Tiempo de preparación terminado
                    worldProtectionStatus.put(worldName, false);
                    protectionTimers.remove(worldName);
                    protectionTimeRemaining.remove(worldName); // Limpiar el tiempo restante
                    removeHasteEffect(worldName);
                    sendMessageToWorldPlayers(worldName, "El tiempo de preparación ha terminado.");
                    Bukkit.getLogger().info("Protección de colocación de bloques terminada en el mundo " + worldName);
                    this.cancel();
                }
                // Si ya pasaron 90 segundos, dejar de aplicar Haste II
                if (secondsElapsed >= 90) {
                    removeHasteEffect(worldName); // Eliminar el efecto de Haste II después de 90 segundos
                } else {
                    hasteTimerRemaining.put(worldName, hasteDuration - secondsElapsed); // Actualizar el tiempo restante de Haste II
                }
            }
        };
        task.runTaskTimer(plugin, 0, 20);
        protectionTimers.put(worldName, task);
        Bukkit.getLogger().info("Protección de colocación de bloques iniciada por " + (preparationTime / 60) + " minutos en el mundo " + worldName);
        sendMessageToWorldPlayers(worldName, "Quedan " + (preparationTime / 60) + " minutos hasta que termine el tiempo de preparación.");
    }

    // Aplicar Haste 2
    private void applyHasteEffect(String worldName, int hasteDuration) {
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            if (onlinePlayer.getWorld().getName().equals(worldName) && hasteDuration > 0) {
                onlinePlayer.addPotionEffect(new PotionEffect(PotionEffectType.FAST_DIGGING, hasteDuration * 20, 1, true, false));
            }
        }
    }

    // Remover Haste 2
    private void removeHasteEffect(String worldName) {
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            if (onlinePlayer.getWorld().getName().equals(worldName)) {
                onlinePlayer.removePotionEffect(PotionEffectType.FAST_DIGGING);
            }
        }
    }

    // Aplicar Regeneración
    private void applyRegenerationEffect(String worldName, int remainingTime) {
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            if (onlinePlayer.getWorld().getName().equals(worldName)) {
                onlinePlayer.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, remainingTime * 20, 0, true, false));
            }
        }
    }

    // Remover Regeneración
    private void removeRegenerationEffect(String worldName) {
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            if (onlinePlayer.getWorld().getName().equals(worldName)) {
                onlinePlayer.removePotionEffect(PotionEffectType.REGENERATION);
            }
        }
    }
    // Función para mandar mensaje a los jugadores
    private void sendMessageToWorldPlayers(String worldName, String message) {
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            if (onlinePlayer.getWorld().getName().equals(worldName)) {
                onlinePlayer.sendMessage(ChatColor.YELLOW + message);
            }
        }
    }

    // Manejar el comando /torneo lista
    private void listTournamentCommands(Player player) {
        FileConfiguration config = plugin.getConfig();
        List<String> commands = config.getStringList("commands");

        if (commands.isEmpty()) {
            player.sendMessage(ChatColor.YELLOW + "No hay comandos configurados.");
            return;
        }

        player.sendMessage(ChatColor.YELLOW + "Comandos configurados:");
        for (int i = 0; i < commands.size(); i++) {
            player.sendMessage(ChatColor.GREEN + String.valueOf(i + 1) + ". " + commands.get(i));
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
                player.sendMessage(ChatColor.RED + "Número inválido. Usa /torneo lista para ver los números válidos.");
                return;
            }

            String removedCommand = commands.remove(index); // Eliminar el comando de la lista
            config.set("commands", commands); // Actualizar la lista de comandos
            plugin.saveConfig(); // Guardar el archivo de configuración

            player.sendMessage(ChatColor.GREEN + "Comando eliminado: " + removedCommand);
        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + "Número inválido. Usa /torneo delete {numero}.");
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

        player.sendMessage(ChatColor.GREEN + "Comando añadido: " + newCommand);
    }

    // Manejar el comando /torneo on
    private void handleTournamentOn(Player player, String[] args, String worldName) {
        globalProtectionStatus.remove(worldName); // Desactivar protección global

        // Verificar si el comando /privado on está registrado
        if (plugin.getServer().getPluginCommand("privado") != null) {
            player.performCommand("privado on"); // Ejecutar el comando "/privado on" solo si está el plugin "captainsForTowers" instalado
        }

        // Cargar el archivo de configuración
        File configFile = new File(plugin.getDataFolder(), "config.yml");
        FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);

        // Obtener el tiempo de preparación desde el config.yml
        int preparationTime = 4; // Valor predeterminado de 4 minutos
        if (config.contains(worldName + ".Timer")) {
            preparationTime = config.getInt(worldName + ".Timer"); // Obtener el tiempo de preparación
        } else {
            player.sendMessage(ChatColor.RED + "No se ha encontrado el tiempo de preparación en el archivo de configuración.");
            return;
        }

        // Si hay un segundo argumento, intentar convertirlo en tiempo de preparación
        if (args.length >= 2) {
            try {
                preparationTime = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                player.sendMessage(ChatColor.RED + "El tiempo de preparación debe ser un número en segundos.");
                return;
            }
        }
        preparationTime *= 60; // Convertir minutos a segundos
        startProtectionTimer(worldName, player, preparationTime);
    }

    // Manejar el comando /torneo off
    private void stopProtectionTimer(final String worldName, Player player) {
        // Verificar si el comando /privado on está registrado
        if (plugin.getServer().getPluginCommand("privado") != null) {
            player.performCommand("privado off"); // Ejecutar el comando "/privado off" solo si está el plugin "captainsForTowers" instalado
        }
        if (protectionTimers.containsKey(worldName)) {
            protectionTimers.get(worldName).cancel(); // Se cancela el timer
            protectionTimers.remove(worldName); // Se remueven el timer
            worldProtectionStatus.put(worldName, false); // Estado False (ya no estamos en tiempo de preparación)
            protectionTimeRemaining.remove(worldName); // Limpiar el tiempo restante
            removeRegenerationEffect(worldName); // Eliminar el efecto Regeneración si se detiene el tiempo de preparación
            removeHasteEffect(worldName); // Eliminar el efecto de Haste II
            sendMessageToWorldPlayers(worldName, "Tiempo de preparación cancelado.");
            Bukkit.getLogger().info("La protección de colocación de bloques ha sido detenida en el mundo " + worldName);
        } else {
            player.sendMessage(ChatColor.YELLOW + "No se encontró protección activa en el mundo " + worldName);
        }
    }

    // Manejar el comadno /torneo timer
    private void updatePreparationTime(int newPreparationTime, Player player, String worldName) {
        // Cargar el archivo de configuración
        File configFile = new File(plugin.getDataFolder(), "config.yml");
        FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
    
        // Verificar si el mundo existe en la configuración
        if (config.contains(worldName)) {
            // Reemplazar el valor del Timer para el mundo dado
            config.set(worldName + ".Timer", newPreparationTime);
    
            // Guardar el archivo de configuración actualizado
            try {
                config.save(configFile);
                player.sendMessage(ChatColor.GREEN + "El tiempo de preparación del mundo " + worldName + " ha sido actualizado a " + newPreparationTime + " minutos.");
            } catch (Exception e) {
                player.sendMessage(ChatColor.RED + "Error al guardar el archivo de configuración.");
                e.printStackTrace();
            }
        } else {
            player.sendMessage(ChatColor.RED + "El mundo " + worldName + " no se encuentra en la configuración.");
        }
    }
}