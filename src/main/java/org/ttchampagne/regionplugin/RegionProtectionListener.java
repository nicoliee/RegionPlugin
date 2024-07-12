package org.ttchampagne.regionplugin;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;

public class RegionProtectionListener implements Listener, CommandExecutor {

    private final RegionPlugin plugin;
    private final Map<String, Boolean> worldProtectionStatus = new HashMap<>();
    private final Map<String, BukkitRunnable> protectionTimers = new HashMap<>();

    public RegionProtectionListener(RegionPlugin plugin) {
        this.plugin = plugin;
        plugin.getCommand("startProtection").setExecutor(this);
        plugin.getCommand("stopProtection").setExecutor(this);
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        String worldName = event.getBlock().getWorld().getName();
        Boolean isProtectionActive = worldProtectionStatus.get(worldName);

        if (isProtectionActive != null && isProtectionActive) {
            Region region = plugin.getRegions().get(worldName);

            if (region != null && region.isInside(event.getBlock().getLocation())) {
                event.setCancelled(true);
                event.getPlayer().sendMessage(ChatColor.YELLOW + "No puedes colocar bloques en esta región.");
            }
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            String worldName = player.getWorld().getName();

            if (label.equalsIgnoreCase("startProtection")) {
                startProtectionTimer(worldName, player);
                return true;
            } else if (label.equalsIgnoreCase("stopProtection")) {
                stopProtectionTimer(worldName, player);
                return true;
            }
        }
        return false;
    }

    private void startProtectionTimer(final String worldName, Player player) {
        if (protectionTimers.containsKey(worldName)) {
            player.sendMessage(ChatColor.YELLOW + "La protección ya está activa en este mundo.");
            return;
        }

        worldProtectionStatus.put(worldName, true);
        BukkitRunnable task = new BukkitRunnable() {
            private int secondsElapsed = 0;

            @Override
            public void run() {
                secondsElapsed++;

                if (secondsElapsed == 60) {
                    Bukkit.broadcastMessage(ChatColor.YELLOW + "Quedan 4 minutos hasta que termine el tiempo de preparación.");
                } else if (secondsElapsed == 120) {
                    Bukkit.broadcastMessage(ChatColor.YELLOW + "Quedan 3 minutos hasta que termine el tiempo de preparación.");
                } else if (secondsElapsed == 180) {
                    Bukkit.broadcastMessage(ChatColor.YELLOW + "Quedan 2 minutos hasta que termine el tiempo de preparación.");
                } else if (secondsElapsed == 240) {
                    Bukkit.broadcastMessage(ChatColor.YELLOW + "Queda 1 minuto hasta que termine el tiempo de preparación.");
                } else if (secondsElapsed == 290) {
                    Bukkit.broadcastMessage(ChatColor.YELLOW + "Quedan 10 segundos hasta que termine el tiempo de preparación.");
                } else if (secondsElapsed == 295) {
                    Bukkit.broadcastMessage(ChatColor.YELLOW + "Quedan 5 segundos hasta que termine el tiempo de preparación.");
                } else if (secondsElapsed == 296) {
                    Bukkit.broadcastMessage(ChatColor.YELLOW + "Quedan 4 segundos hasta que termine el tiempo de preparación.");
                } else if (secondsElapsed == 297) {
                    Bukkit.broadcastMessage(ChatColor.YELLOW + "Quedan 3 segundos hasta que termine el tiempo de preparación.");
                } else if (secondsElapsed == 298) {
                    Bukkit.broadcastMessage(ChatColor.YELLOW + "Quedan 2 segundos hasta que termine el tiempo de preparación.");
                } else if (secondsElapsed == 299) {
                    Bukkit.broadcastMessage(ChatColor.YELLOW + "Queda 1 segundo hasta que termine el tiempo de preparación.");
                } else if (secondsElapsed == 300) {
                    worldProtectionStatus.put(worldName, false);
                    protectionTimers.remove(worldName);
                    Bukkit.broadcastMessage(ChatColor.YELLOW + "El tiempo de preparación ha terminado.");
                    this.cancel();
                }
            }
        };
        task.runTaskTimer(plugin, 0, 20); // Se ejecuta cada segundo (20 ticks)
        protectionTimers.put(worldName, task);
        player.sendMessage(ChatColor.YELLOW + "Protección de colocación de bloques iniciada por 5 minutos en el mundo " + worldName);
    }

    private void stopProtectionTimer(final String worldName, Player player) {
        if (protectionTimers.containsKey(worldName)) {
            protectionTimers.get(worldName).cancel();
            protectionTimers.remove(worldName);
            worldProtectionStatus.put(worldName, false);
            player.sendMessage(ChatColor.YELLOW + "Protección de colocación de bloques detenida en el mundo " + worldName);
            Bukkit.getLogger().info("La protección de colocación de bloques ha sido detenida en el mundo " + worldName);
        } else {
            player.sendMessage(ChatColor.YELLOW + "No se encontró protección activa en el mundo " + worldName);
        }
    }
}
