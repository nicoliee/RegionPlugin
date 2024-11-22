package org.ttchampagne.regionplugin;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RegionProtectionListener implements Listener, CommandExecutor {

    private final RegionPlugin plugin;
    private final Map<String, Boolean> worldProtectionStatus = new HashMap<>();
    private final Map<String, BukkitRunnable> protectionTimers = new HashMap<>();
    private final Map<String, Integer> protectionTimeRemaining = new HashMap<>(); // Mapa para almacenar el tiempo restante de protección de bloques
    private final Map<String, Integer> regenerationTimerRemaining = new HashMap<>(); // Mapa para almacenar el tiempo restante de la regeneración
    private final Map<String, Integer> hasteTimerRemaining = new HashMap<>(); // Mapa para guardar el tiempo restante de Haste II


    public RegionProtectionListener(RegionPlugin plugin) {
        this.plugin = plugin;
        plugin.getCommand("torneo").setExecutor(this);
    }
    @EventHandler
    // Logica para la colocación y destrucción de bloques
    public void onBlockPlace(BlockPlaceEvent event) {
        String worldName = event.getBlock().getWorld().getName();
        Boolean isProtectionActive = worldProtectionStatus.get(worldName);

        if (isProtectionActive != null && isProtectionActive) {
            Region region = plugin.getRegions().get(worldName);
            if (region != null && region.isInside(event.getBlock().getLocation())) {
                event.setCancelled(true);
                event.getPlayer().sendMessage(ChatColor.YELLOW + "No puedes construir en tiempo de preparación");
            }
        }
    }

    @EventHandler
    // Logica para la destrucción de bloques
    public void onBlockBreak(BlockBreakEvent event) {
        String worldName = event.getBlock().getWorld().getName();
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
            }.runTaskLater(plugin, 20L);
        }
    }



    @Override
    // Que debe pasar cuando se ejecute el comando principal
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("torneo")) {
            if (sender instanceof Player) {
                Player player = (Player) sender;
                // Verificar permiso "towers.admin"
                if (!player.hasPermission("towers.admin")) {
                    player.sendMessage(ChatColor.RED + "No tienes permiso para usar este comando.");
                    return true;
                }
                String worldName = player.getWorld().getName();
                if (args.length == 0) {
                    executeTournamentCommands(player);
                    return true;
                } else if (args.length == 1 || args.length == 2) {
                    // Si el comando es "/torneo on"
                    if (args[0].equalsIgnoreCase("on")) {
                        int preparationTime = 180; // Valor predeterminado de 3 minutos (180 segundos)

                        // Si hay un segundo argumento, intenta convertirlo a un entero para el tiempo de preparación
                        if (args.length == 2) {
                            try {
                                preparationTime = Integer.parseInt(args[1]);
                            } catch (NumberFormatException e) {
                                player.sendMessage(ChatColor.RED + "El tiempo de preparación debe ser un número en segundos.");
                                return true;
                            }
                        }
                        startProtectionTimer(worldName, player, preparationTime);
                        return true;
                    } else if (args[0].equalsIgnoreCase("off")) {
                        stopProtectionTimer(worldName, player);
                        return true;
                    } else {
                        player.sendMessage(ChatColor.RED + "Uso incorrecto del comando. Usa /torneo on <tiempo_en_segundos> o /torneo off.");
                    }
                } else {
                    player.sendMessage(ChatColor.RED + "Uso incorrecto del comando. Usa /torneo o /torneo on <tiempo_en_segundos>/off.");
                }
            } else {
                sender.sendMessage("Este comando solo puede ser ejecutado por jugadores.");
                return true;
            }
        }
        return false;
    }

    // lo que debe pasar al ejecutar "/torneo"
    private void executeTournamentCommands(Player player) {
        // se revisa en "config.yml" los comandos que debe ejecutar el jugador
        FileConfiguration config = plugin.getConfig();
        List<String> commands = config.getStringList("commands");
        for (String cmd : commands) {
            player.performCommand(cmd);
        }
        player.sendMessage(ChatColor.YELLOW + "Reglas de Torneo Activadas.");
    }

    // lo que debe pasar al ejecutar "/torneo on"
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
        player.sendMessage(ChatColor.YELLOW + "Protección de colocación de bloques iniciada por " + (preparationTime / 60) + " minutos en el mundo " + worldName);
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
    // Que debe pasar cuando se ejecute "/torneo off"
    private void stopProtectionTimer(final String worldName, Player player) {
        if (protectionTimers.containsKey(worldName)) {
            protectionTimers.get(worldName).cancel(); // Se cancela el timer
            protectionTimers.remove(worldName); // Se remueven el timer
            worldProtectionStatus.put(worldName, false); // Estado False (ya no estamos en tiempo de preparación)
            protectionTimeRemaining.remove(worldName); // Limpiar el tiempo restante
            removeRegenerationEffect(worldName); // Eliminar el efecto Regeneración si se detiene el tiempo de preparación
            removeHasteEffect(worldName); // Eliminar el efecto de Haste II
            player.sendMessage(ChatColor.YELLOW + "Protección de colocación de bloques detenida en el mundo " + worldName);
            Bukkit.getLogger().info("La protección de colocación de bloques ha sido detenida en el mundo " + worldName);
        } else {
            player.sendMessage(ChatColor.YELLOW + "No se encontró protección activa en el mundo " + worldName);
        }
    }
}
