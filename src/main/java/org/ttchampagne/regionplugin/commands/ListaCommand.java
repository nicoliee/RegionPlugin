package org.ttchampagne.regionplugin.commands;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.command.CommandExecutor;
import org.ttchampagne.regionplugin.RegionPlugin;

public class ListaCommand implements CommandExecutor {
    private final RegionPlugin plugin;
    public ListaCommand(RegionPlugin plugin) {
        this.plugin = plugin;
    }
    @Override
    // si el comando ingresado es /lista
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Verificar que el sender sea un jugador
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                        plugin.getMessagesConfig().getString("messages.no_player")));
                return true;
            }
        if (command.getName().equalsIgnoreCase("lista")) {
            if (sender instanceof Player) {
                Player player = (Player) sender;
                String worldName = player.getWorld().getName();

                // lista de jugadores en los equipos
                List<String> rojo = new ArrayList<>();
                List<String> azul = new ArrayList<>();
                List<String> espectadores = new ArrayList<>();

                // recorre todos los jugadores en línea pero solo los que están en el mundo actual
                for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                    if (!onlinePlayer.getWorld().getName().equals(worldName)) {
                        continue;
                    }
                    // determina el equipo del jugador
                    String equipo = determinarEquipoPorArmadura(onlinePlayer);

                    // agrega el jugador a la lista de su equipo
                    if (equipo.equals("ROJO")) {
                        rojo.add(onlinePlayer.getName());
                    } else if (equipo.equals("AZUL")) {
                        azul.add(onlinePlayer.getName());
                    } else {
                        espectadores.add(onlinePlayer.getName());
                    }
                }
                
                // crea un mensaje con la lista de jugadores en los equipos
                StringBuilder mensaje = new StringBuilder();
                //TODO Agregar "teams" y el color de los equipos a messages.yml
                //TODO agregar la suma de jugadores en cada equipo
                mensaje.append("§7Teams:\n");
                mensaje.append(formatearMensajeEquipo("§4 Red", rojo, "§4", "§8", "§f"));
                mensaje.append(formatearMensajeEquipo("§9 Blue", azul, "§9", "§8", "§f"));
                mensaje.append(formatearMensajeEquipo("§b Observers", espectadores, "§b", "§8", "§f"));
                // envía el mensaje al jugador que ejecutó el comando
                sender.sendMessage(mensaje.toString());
                return true;
            }
        }
        return false;
    }
    // determina el equipo de un jugador basado en su armadura
    private String determinarEquipoPorArmadura(Player player) {
        boolean tieneRojo = false;
        boolean tieneAzul = false;
        
        // recorre todas las piezas de armadura del jugador
        for (ItemStack armorPiece : player.getInventory().getArmorContents()) {
            if (armorPiece != null && (armorPiece.getType() == Material.LEATHER_HELMET
                    || armorPiece.getType() == Material.LEATHER_CHESTPLATE
                    || armorPiece.getType() == Material.LEATHER_LEGGINGS
                    || armorPiece.getType() == Material.LEATHER_BOOTS)) {
                
                // si la armadura es de cuero, obtiene el color
                if (armorPiece.getItemMeta() instanceof LeatherArmorMeta) {
                    LeatherArmorMeta meta = (LeatherArmorMeta) armorPiece.getItemMeta();
                    Color color = meta.getColor();
                    // verifica si el color es rojo o azul
                    if (esColorRojo(color)) {
                        tieneRojo = true;
                    } else if (esColorAzul(color)) {
                        tieneAzul = true;
                    }
                }
            }
        }
        // determina el equipo del jugador
        if (tieneRojo && !tieneAzul) {
            return "ROJO";
        } else if (tieneAzul && !tieneRojo) {
            return "AZUL";
        } else {
            return "ESPECTADOR";
        }
    }
    // verifica si un color es rojo
    private boolean esColorRojo(Color color) {
        return color.getRed() > 200 && color.getGreen() < 100 && color.getBlue() < 100;
    }
    // verifica si un color es azul
    private boolean esColorAzul(Color color) {
        return color.getBlue() > 200 && color.getGreen() < 100 && color.getRed() < 100;
    }
    // formatea un mensaje con la lista de jugadores en un equipo
    // TODO agregar separador "and" en messages.yml
    private String formatearMensajeEquipo(String titulo, List<String> jugadores, String color, String separador, String colorNum) {
        StringBuilder resultado = new StringBuilder();
        resultado.append(titulo).append(": ").append(colorNum).append(jugadores.size()).append("\n");
        if (!jugadores.isEmpty()) {
            for (int i = 0; i < jugadores.size(); i++) {
                // si solo hay un jugador, se agrega el nombre del jugador
                resultado.append(color).append(jugadores.get(i));
                if (i == jugadores.size() - 2) {
                    // si hay dos jugadores, se agrega " and " entre los nombres
                    resultado.append(separador).append(" and ");
                } else if (i < jugadores.size() - 1) {
                    // si hay más de dos jugadores, se agrega ", " entre los nombres
                    resultado.append(separador).append(", ");
                }
            }
            resultado.append("\n");
        }
        return resultado.toString();
    }
}