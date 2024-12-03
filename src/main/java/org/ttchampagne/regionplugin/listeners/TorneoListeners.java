package org.ttchampagne.regionplugin.listeners;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.ttchampagne.regionplugin.Region;
import org.ttchampagne.regionplugin.RegionPlugin;

import java.util.HashMap;
import java.util.Map;

public class TorneoListeners implements Listener{
    private final RegionPlugin plugin;
    private final Map<String, Boolean> worldProtectionStatus;
    private final Map<String, Boolean> globalProtectionStatus;
    private final Map<String, BukkitRunnable> protectionTimers;
    private final Map<String, Integer> protectionTimeRemaining;
    private final Map<String, Integer> regenerationTimerRemaining;
    private final Map<String, Integer> hasteTimerRemaining;
    private final Map<String, Boolean> privateModeMap;

    public TorneoListeners(RegionPlugin plugin,
                       Map<String, Boolean> worldProtectionStatus,
                       Map<String, Boolean> globalProtectionStatus,
                       Map<String, BukkitRunnable> protectionTimers,
                       Map<String, Integer> protectionTimeRemaining,
                       Map<String, Integer> regenerationTimerRemaining,
                       Map<String, Integer> hasteTimerRemaining,
                       Map<String, Boolean> privateModeMap) {
        this.plugin = plugin;
        this.worldProtectionStatus = worldProtectionStatus != null ? worldProtectionStatus : new HashMap<>();
        this.globalProtectionStatus = globalProtectionStatus != null ? globalProtectionStatus : new HashMap<>();
        this.protectionTimers = protectionTimers != null ? protectionTimers : new HashMap<>();
        this.protectionTimeRemaining = protectionTimeRemaining != null ? protectionTimeRemaining : new HashMap<>();
        this.regenerationTimerRemaining = regenerationTimerRemaining != null ? regenerationTimerRemaining : new HashMap<>();
        this.hasteTimerRemaining = hasteTimerRemaining != null ? hasteTimerRemaining : new HashMap<>();
        this.privateModeMap = privateModeMap != null ? privateModeMap : new HashMap<>();
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
    @EventHandler
    public void onPlayerChangeWorld(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        String worldName = player.getWorld().getName();

        // Verificamos si el modo privado está activo para el mundo donde el jugador entra
        if (privateModeMap.getOrDefault(worldName, false)) { // Si no hay entrada en el mapa, devuelve 'false' por defecto
            // Verificamos si el jugador tiene armadura y aplicamos el modo espectador si es necesario
            Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
                @Override
                public void run() {
                    // Verificar si el jugador no tiene armadura y ponerlo en modo espectador
                    if (!tieneArmadura(player)) {
                        player.getInventory().clear();
                        player.setGameMode(GameMode.SPECTATOR);
                        player.sendMessage(ChatColor.RED + "Partida iniciada con capitanes, no puedes entrar.");
                        player.performCommand("lista");

                    }
                }
            }, 10L);
        }
    }
    
    @EventHandler
    public void onWorldLoad(WorldLoadEvent event){
        String worldName = event.getWorld().getName();
        privateModeMap.put(worldName, false); // Desactivar el modo privado por defecto
    }

    // Verificar si el jugador tiene armadura de cuero
    private boolean tieneArmadura(Player player) {
        for (ItemStack armorPiece : player.getInventory().getArmorContents()) {
            if (armorPiece != null && (armorPiece.getType() == Material.LEATHER_HELMET
                    || armorPiece.getType() == Material.LEATHER_CHESTPLATE
                    || armorPiece.getType() == Material.LEATHER_LEGGINGS
                    || armorPiece.getType() == Material.LEATHER_BOOTS)) {
                return true;
            }
        }
        return false;
    }

    // Iniciar la protección de bloques
    public void startProtectionTimer(final String worldName, Player player, int preparationTime, int hasteTime) {
        if (protectionTimers.containsKey(worldName)) {
            player.sendMessage(ChatColor.YELLOW + "La protección ya está activa en este mundo.");
            return;
        }
        worldProtectionStatus.put(worldName, true);
        protectionTimeRemaining.put(worldName, preparationTime); // Guardar el tiempo de protección restante
        // Determinar duración para Regeneración y Haste II
        regenerationTimerRemaining.put(worldName, preparationTime); // Tiempo completo para Regeneración
        int hasteDuration = Math.min(hasteTime, preparationTime); // Haste II se aplica solo por la duración de la preparación, si es menos entonces la cantidad será la de config.yml
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
                // Si ya pasaron {haste} segundos, dejar de aplicar Haste II
                if (secondsElapsed >= hasteTime) {
                    removeHasteEffect(worldName); // Eliminar el efecto de Haste II
                } else {
                    hasteTimerRemaining.put(worldName, hasteDuration - secondsElapsed); // Actualizar el tiempo restante de Haste II
                }
            }
        };
        task.runTaskTimer(plugin, 0, 20); // Ejecutar el timer cada segundo
        protectionTimers.put(worldName, task);
        Bukkit.getLogger().info("Protección de colocación de bloques iniciada por " + (preparationTime / 60) + " minutos en el mundo " + worldName);
        sendMessageToWorldPlayers(worldName, "Quedan " + (preparationTime / 60) + " minutos hasta que termine el tiempo de preparación.");
    }

    // Detener la protección de bloques
    public void stopProtectionTimer(final String worldName, Player player) {
        privateModeMap.put(worldName, false); // Desactivar el modo privado
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

    // Aplicar Haste 2
    private void applyHasteEffect(String worldName, int hasteDuration) {
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            if (onlinePlayer.getWorld().getName().equals(worldName) && hasteDuration > 1) {
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

    // Métodos para manejar la protección global
    public void setGlobalProtection(String worldName, boolean status) {
        globalProtectionStatus.put(worldName, status);
    }

    // Métodos para manejar la protección por región
    public void removeGlobalProtection(String worldName) {
        globalProtectionStatus.remove(worldName);
    }
    
    // Métodos para manejar el modo privado
    public void setPrivateMode(String worldName, boolean status) {
        privateModeMap.put(worldName, status);
    }
}