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
    private final Map<String, Integer> protectionTimeRemaining = new HashMap<>(); // Mapa para almacenar el tiempo restante de protección
    private final Map<String, Integer> regenerationTimerRemaining = new HashMap<>(); // Mapa para almacenar el tiempo restante de la regeneración
    private final Map<String, Integer> hasteTimerRemaining = new HashMap<>(); // Mapa para guardar el tiempo restante de Haste II


    public RegionProtectionListener(RegionPlugin plugin) {
        this.plugin = plugin;
        plugin.getCommand("torneo").setExecutor(this);
    }

    @EventHandler
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



    // Lógica para Haste y Regeneracion

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        String worldName = player.getWorld().getName();

        if (worldProtectionStatus.get(worldName)) {
            int remainingTime = protectionTimeRemaining.getOrDefault(worldName, 0);
            if (remainingTime > 0) {
                // Retrasar el efecto de Regeneración por 1 segundo después del respawn (20 ticks = 1 segundo)
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        applyPreparationEffects(worldName, remainingTime); // Aplicar Regeneración

                        // Verificar si queda tiempo de Haste II
                        int remainingHasteTime = hasteTimerRemaining.getOrDefault(worldName, 0);
                        if (remainingHasteTime > 0) {
                            applyHasteEffect(worldName, remainingHasteTime); // Aplicar el tiempo restante de Haste II
                        }
                    }
                }.runTaskLater(plugin, 20L); // 20 ticks = 1 segundo
            }
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("torneo")) {
            if (sender instanceof Player) {
                Player player = (Player) sender;

                if (!player.hasPermission("towers.admin")) {
                    player.sendMessage(ChatColor.RED + "No tienes permiso para usar este comando.");
                    return true;
                }

                String worldName = player.getWorld().getName();
                if (args.length == 0) {
                    executeTournamentCommands(player);
                    return true;
                } else if (args.length == 1 || args.length == 2) {
                    if (args[0].equalsIgnoreCase("on")) {
                        int preparationTime = 240; // Valor predeterminado de 4 minutos (240 segundos)

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

    private void executeTournamentCommands(Player player) {
        FileConfiguration config = plugin.getConfig();
        List<String> commands = config.getStringList("commands");

        for (String cmd : commands) {
            player.performCommand(cmd);
        }

        player.sendMessage(ChatColor.YELLOW + "Reglas de Torneo Activadas.");
    }

    private void startProtectionTimer(final String worldName, Player player, int preparationTime) {
        if (protectionTimers.containsKey(worldName)) {
            player.sendMessage(ChatColor.YELLOW + "La protección ya está activa en este mundo.");
            return;
        }

        worldProtectionStatus.put(worldName, true);
        protectionTimeRemaining.put(worldName, preparationTime); // Guardar el tiempo de protección restante
        regenerationTimerRemaining.put(worldName, preparationTime); // Guardar el tiempo restante para Regeneración
        hasteTimerRemaining.put(worldName, Math.min(90, preparationTime)); // Guardar el tiempo restante de Haste II, máximo 90 segundos
        applyPreparationEffects(worldName, preparationTime); // Aplicar la Regeneración a todos los jugadores
        applyHasteEffect(worldName, Math.min(90, preparationTime)); // Aplicar Haste II solo por los primeros 90 segundos

        BukkitRunnable task = new BukkitRunnable() {
            private int secondsElapsed = 0;

            @Override
            public void run() {
                secondsElapsed++;
                int timeRemaining = preparationTime - secondsElapsed;
                protectionTimeRemaining.put(worldName, timeRemaining); // Actualizar el tiempo restante

                if (timeRemaining > 0) {
                    if (timeRemaining % 60 == 0) {
                        int minutesRemaining = timeRemaining / 60;
                        sendMessageToWorldPlayers(worldName, "Quedan " + minutesRemaining + " minutos hasta que termine el tiempo de preparación.");
                    } else if (timeRemaining == 30 || timeRemaining == 10 || (timeRemaining <= 5 && timeRemaining >= 1)) {
                        sendMessageToWorldPlayers(worldName, "Quedan " + timeRemaining + " segundos hasta que termine el tiempo de preparación.");
                    }
                } else {
                    // Tiempo de preparación terminado
                    worldProtectionStatus.put(worldName, false);
                    protectionTimers.remove(worldName);
                    protectionTimeRemaining.remove(worldName); // Limpiar el tiempo restante
                    removePreparationEffects(worldName); // Eliminar el efecto Regeneración al terminar el tiempo de preparación
                    sendMessageToWorldPlayers(worldName, "El tiempo de preparación ha terminado.");
                    this.cancel();
                }

                // Si ya pasaron 90 segundos, dejar de aplicar Haste II
                if (secondsElapsed >= 90) {
                    removeHasteEffect(worldName); // Eliminar el efecto de Haste II después de 90 segundos
                } else {
                    hasteTimerRemaining.put(worldName, 90 - secondsElapsed); // Actualizar el tiempo restante de Haste II
                }
            }
        };

        task.runTaskTimer(plugin, 0, 20);
        protectionTimers.put(worldName, task);
        player.sendMessage(ChatColor.YELLOW + "Protección de colocación de bloques iniciada por " + (preparationTime / 60) + " minutos en el mundo " + worldName);
        sendMessageToWorldPlayers(worldName, "Quedan " + (preparationTime / 60) + " minutos hasta que termine el tiempo de preparación.");
    }

    private void applyHasteEffect(String worldName, int durationInSeconds) {
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            if (onlinePlayer.getWorld().getName().equals(worldName) && durationInSeconds > 0) {
                onlinePlayer.addPotionEffect(new PotionEffect(PotionEffectType.FAST_DIGGING, durationInSeconds * 20, 1, true, false)); // Haste II
            }
        }
    }

    private void removeHasteEffect(String worldName) {
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            if (onlinePlayer.getWorld().getName().equals(worldName)) {
                onlinePlayer.removePotionEffect(PotionEffectType.FAST_DIGGING);
            }
        }
    }

    private void applyPreparationEffects(String worldName, int remainingTime) {
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            if (onlinePlayer.getWorld().getName().equals(worldName)) {
                onlinePlayer.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, remainingTime * 20, 0, true, false));
            }
        }
    }

    private void removePreparationEffects(String worldName) {
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            if (onlinePlayer.getWorld().getName().equals(worldName)) {
                onlinePlayer.removePotionEffect(PotionEffectType.REGENERATION);
            }
        }
    }

    private void sendMessageToWorldPlayers(String worldName, String message) {
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            if (onlinePlayer.getWorld().getName().equals(worldName)) {
                onlinePlayer.sendMessage(ChatColor.YELLOW + message);
            }
        }
    }

    private void stopProtectionTimer(final String worldName, Player player) {
        if (protectionTimers.containsKey(worldName)) {
            protectionTimers.get(worldName).cancel();
            protectionTimers.remove(worldName);
            worldProtectionStatus.put(worldName, false);
            protectionTimeRemaining.remove(worldName); // Limpiar el tiempo restante
            removePreparationEffects(worldName); // Eliminar el efecto Regeneración si se detiene el tiempo de preparación
            removeHasteEffect(worldName); // Eliminar el efecto de Haste II
            player.sendMessage(ChatColor.YELLOW + "Protección de colocación de bloques detenida en el mundo " + worldName);
            Bukkit.getLogger().info("La protección de colocación de bloques ha sido detenida en el mundo " + worldName);
        } else {
            player.sendMessage(ChatColor.YELLOW + "No se encontró protección activa en el mundo " + worldName);
        }
    }
}
