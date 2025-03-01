package org.ttchampagne.regionplugin.listeners;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.ttchampagne.regionplugin.Region;
import org.ttchampagne.regionplugin.RegionPlugin;
import org.ttchampagne.utils.SendMessage;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TorneoListeners implements Listener{
    private final RegionPlugin plugin;
    private final Map<String, Boolean> worldProtectionStatus;
    private final Map<String, BukkitRunnable> protectionTimers;
    private final Map<String, Integer> protectionTimeRemaining;
    private final Map<String, Integer> regenerationTimerRemaining;
    private final Map<String, Integer> hasteTimerRemaining;
    private final Map<String, Boolean> privateModeMap;

    public TorneoListeners(RegionPlugin plugin,
                       Map<String, Boolean> worldProtectionStatus,
                       Map<String, BukkitRunnable> protectionTimers,
                       Map<String, Integer> protectionTimeRemaining,
                       Map<String, Integer> regenerationTimerRemaining,
                       Map<String, Integer> hasteTimerRemaining,
                       Map<String, Boolean> privateModeMap) {
        this.plugin = plugin;
        this.worldProtectionStatus = worldProtectionStatus != null ? worldProtectionStatus : new HashMap<>();
        this.protectionTimers = protectionTimers != null ? protectionTimers : new HashMap<>();
        this.protectionTimeRemaining = protectionTimeRemaining != null ? protectionTimeRemaining : new HashMap<>();
        this.regenerationTimerRemaining = regenerationTimerRemaining != null ? regenerationTimerRemaining : new HashMap<>();
        this.hasteTimerRemaining = hasteTimerRemaining != null ? hasteTimerRemaining : new HashMap<>();
        this.privateModeMap = privateModeMap != null ? privateModeMap : new HashMap<>();
    }

    @EventHandler
    public void onPrepareItemCraft(PrepareItemCraftEvent e) {
        ItemStack result = e.getInventory().getResult();

        if (result != null && (result.getType() == Material.FISHING_ROD || result.getType() == Material.ARMOR_STAND)) {
            e.getInventory().setResult(null); // Bloquea el crafteo
        }
    }

    @EventHandler
    public void onInventoryClick(final InventoryClickEvent e) {
        ItemStack clickedItem = e.getCurrentItem();

        if (clickedItem != null && (clickedItem.getType() == Material.FISHING_ROD || clickedItem.getType() == Material.ARMOR_STAND)) {
            e.setCancelled(true); // Bloquea la acción
            e.getWhoClicked().closeInventory();
        }
    }
    
    @EventHandler
    // Logica para la protección de bloques
    public void onBlockPlace(BlockPlaceEvent event) {
        String worldName = event.getBlock().getWorld().getName();
        if(!isProtectionActive(worldName)){
            return;
        }
        Region region = plugin.getRegions().get(worldName);
        if (region != null && region.isInside(event.getBlock().getLocation())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(ChatColor.translateAlternateColorCodes('&', 
            plugin.getMessagesConfig().getString("messages.preparation.block_place")));
            event.getPlayer().playSound(event.getPlayer().getLocation(), "tile.piston.out", 1.0f, 1.0f);
        }
    }

    @EventHandler
    // Logica para la destrucción de bloques
    public void onBlockBreak(BlockBreakEvent event) {
        String worldName = event.getBlock().getWorld().getName();
        if(!isProtectionActive(worldName)){
            return;
        }
        Region region = plugin.getRegions().get(worldName);
        if (region != null && region.isInside(event.getBlock().getLocation())) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(ChatColor.translateAlternateColorCodes('&', 
            plugin.getMessagesConfig().getString("messages.preparation.block_break")));
            event.getPlayer().playSound(event.getPlayer().getLocation(), "tile.piston.out", 1.0f, 1.0f);
        }
    }

    @EventHandler
    // Que debe pasar cuando el jugador muera dentro del tiempo de preparación
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        String worldName = player.getWorld().getName();
        // Verifica si hay un valor de protección para el mundo y si está activo
        if (!isProtectionActive(worldName)) {
            return; // Si no hay protección activa, no hacer nada
        }
        int remainingTime = getProtectionTimer(worldName); // Obtener el tiempo restante de preparación
        if (remainingTime > 0) {
            Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
                @Override
                public void run() {
                    applyRegenerationEffect(worldName, remainingTime); // Aplicar Regeneración si queda tiempo de preparación
                    // Verificar si queda tiempo de Haste II
                    int remainingHasteTime = getHasteTimer(worldName);
                    if (remainingHasteTime > 0) {
                        applyHasteEffect(worldName, remainingHasteTime); // Aplicar el tiempo restante de Haste II
                    }
                }
            }, 5L);
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
                        event.getPlayer().sendMessage(ChatColor.translateAlternateColorCodes('&', 
                        plugin.getMessagesConfig().getString("messages.privateMode")));
                        player.performCommand("lista");
                    }
                }
            }, 10L);
        }
    }
    
    @EventHandler
    public void onWorldLoad(WorldLoadEvent event){
        String worldName = event.getWorld().getName();
        privateModeMap.put(worldName, false); // Desactivar el modo privado
        stopProtectionTimer(worldName, null);
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
            SendMessage.sendToPlayer(player, plugin.getErrorMessage("preparation.alreadyStarted"));
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
            String sound = "random.click";
            @Override
            public void run() {
                secondsElapsed++;
                int timeRemaining = preparationTime - secondsElapsed;
                protectionTimeRemaining.put(worldName, timeRemaining); // Actualizar el tiempo restante

                if (timeRemaining > 0) {
                    if (timeRemaining % 60 == 0) {
                        // Si el tiempo es divisible dentro de 60 y es mayor a 0 mostrará un mensaje con el tiempo faltante
                        int minutesRemaining = timeRemaining / 60;
                        SendMessage.sendToWorld(worldName, plugin.getMessagesConfig()
                        .getString("messages.preparation.minutes_left")
                        .replace("{minutes}", String.valueOf(minutesRemaining)));
                        SendMessage.soundToWorld(worldName, sound);
                    } else if (timeRemaining == 30 || timeRemaining == 10 || (timeRemaining <= 5 && timeRemaining >= 1)) {
                        // Si el tiempo restante son 30, 10, 5, 4, 3, 2, 1 segundos mostrará un mensaje
                        SendMessage.sendToWorld(worldName, plugin.getMessagesConfig()
                        .getString("messages.preparation.seconds_left")
                        .replace("{seconds}", String.valueOf(timeRemaining)));
                    } if (timeRemaining <= 30) {
                        // Reproducir un sonido de alerta si el tiempo restante es menor o igual a 30 segundos
                        SendMessage.soundToWorld(worldName, sound);
                    }                              
                } else {
                    // Tiempo de preparación terminado
                    executeFinishCommands(worldName); // Ejecutar comandos al terminar el tiempo de preparación
                    worldProtectionStatus.put(worldName, false);
                    protectionTimers.remove(worldName);
                    protectionTimeRemaining.remove(worldName); // Limpiar el tiempo restante
                    SendMessage.sendToWorld(worldName, plugin.getMessagesConfig()
                    .getString("messages.preparation.ended"));
                    for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                        if (onlinePlayer.getWorld().getName().equals(worldName)) {
                            Sound sound;
                            try {
                                // Intentamos usar el sonido para versiones modernas
                                sound = Sound.valueOf("ENTITY_ENDERDRAGON_GROWL");
                            } catch (IllegalArgumentException e) {
                                // Usamos el sonido antiguo si el moderno no existe
                                sound = Sound.valueOf("ENDERDRAGON_GROWL");
                            }
                    
                            onlinePlayer.playSound(onlinePlayer.getLocation(), sound, 0.5f, 1.0f);
                        }
                    }                    
                    Bukkit.getLogger().info("&8[&7"  + worldName + "&8] " + ChatColor.translateAlternateColorCodes('&',plugin.getMessagesConfig().getString("messages.preparation.ended")));
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
        int preparationTimeinMinutes = preparationTime / 60;
        SendMessage.sendToWorld(worldName, plugin.getMessagesConfig().getString("messages.preparation.start").replace("{minutes}", String.valueOf(preparationTimeinMinutes)));
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
            executeFinishCommands(worldName); // Ejecutar comandos al terminar el tiempo de preparación
            SendMessage.sendToWorld(worldName, plugin.getMessagesConfig()
                    .getString("messages.preparation.cancelled"));
        } else {
            SendMessage.sendToPlayer(player, plugin.getErrorMessage("preparation.notStarted"));
        }
    }

    // Ejecutar comandos al terminar el tiempo de preparación
    public void executeFinishCommands(String worldName) {
        FileConfiguration config = plugin.getConfig();
        List<String> commands = config.getStringList("finishCommands");
        for (String command : commands) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command + " " + worldName);
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
    
    // Método para manejar el modo privado
    public void setPrivateMode(String worldName, boolean status) {
        privateModeMap.put(worldName, status);
    }

    // Método para verificar si el tiempo de preparación está activo
    public boolean isProtectionActive(String worldName) {
        return worldProtectionStatus.getOrDefault(worldName, false);
    }

    public int getProtectionTimer(String worldName) {
        return protectionTimeRemaining.getOrDefault(worldName, 0);
    }

    public int getHasteTimer(String worldName) {
        return hasteTimerRemaining.getOrDefault(worldName, 0);
    }
}