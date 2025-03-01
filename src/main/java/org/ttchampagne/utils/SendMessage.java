package org.ttchampagne.utils;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.entity.Player;

public class SendMessage {
    public static void sendToPlayer(Player player, String message) {
        if (player == null) {
            return;
        }
        String coloredMessage = ChatColor.translateAlternateColorCodes('&', message);
        player.sendMessage(coloredMessage);
    }
    
    public static void sendToWorld(String worldName, String message) {
        World world = Bukkit.getWorld(worldName);

        if (world == null) {
            System.out.println("El mundo '" + worldName + "' no existe o no está cargado.");
            return;
        }
        
        String coloredMessage = ChatColor.translateAlternateColorCodes('&', message);

        for (Player player : world.getPlayers()) {
            player.sendMessage(coloredMessage);
        }
    }

    public static void sendToAdmins(String message) {
        String coloredMessage = ChatColor.translateAlternateColorCodes('&', message);
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.hasPermission("towers.admin")) {
                player.sendMessage(coloredMessage);
            }
        }
    }

    public static void soundToWorld(String worldName, String sound){
        World world = Bukkit.getWorld(worldName);

        if (world == null) {
            System.out.println("El mundo '" + worldName + "' no existe o no está cargado.");
            return;
        }

        for (Player player : world.getPlayers()) {
            player.playSound(player.getLocation(), sound, 1.0f, 2.0f);
        }
    }

}
