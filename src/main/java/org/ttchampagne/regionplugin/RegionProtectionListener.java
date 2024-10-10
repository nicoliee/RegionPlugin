package org.ttchampagne.regionplugin;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RegionProtectionListener implements Listener, CommandExecutor {

    private final RegionPlugin plugin;
    private final Map<String, Boolean> worldProtectionStatus = new HashMap<>();
    private final Map<String, BukkitRunnable> protectionTimers = new HashMap<>();

    public RegionProtectionListener(RegionPlugin plugin) {
        this.plugin = plugin;
        plugin.getCommand("torneo").setExecutor(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("torneo")) {
            if (sender instanceof Player) {
                Player player = (Player) sender;

                // Verificar si el jugador es OP
                if (!player.isOp()) {
                    player.sendMessage(ChatColor.RED + "No tienes permiso para usar este comando.");
                    return true;  // Se detiene la ejecución si no es OP
                }

                String worldName = player.getWorld().getName();

                if (args.length == 0) {
                    // Ejecutar comandos del torneo desde el archivo de configuración
                    executeTournamentCommands(player);
                    return true;
                } else if (args.length == 1) {
                    // Manejar /torneo on/off para la protección de bloques
                    if (args[0].equalsIgnoreCase("on")) {
                        startProtectionTimer(worldName, player);
                        return true;
                    } else if (args[0].equalsIgnoreCase("off")) {
                        stopProtectionTimer(worldName, player);
                        return true;
                    } else {
                        player.sendMessage(ChatColor.RED + "Uso incorrecto del comando. Usa /torneo on o /torneo off.");
                    }
                } else {
                    player.sendMessage(ChatColor.RED + "Uso incorrecto del comando. Usa /torneo o /torneo on/off.");
                }
            } else {
                sender.sendMessage("Este comando solo puede ser ejecutado por jugadores.");
                return true;
            }
        }
        return false;
    }

    private void executeTournamentCommands(Player player) {
        // Obtiene la lista de comandos desde el archivo de configuración
        FileConfiguration config = plugin.getConfig();
        List<String> commands = config.getStringList("commands");

        // Ejecuta cada comando listado en la configuración desde el contexto del jugador
        for (String cmd : commands) {
            player.performCommand(cmd);
        }

        player.sendMessage(ChatColor.YELLOW + "Reglas de Torneo Activadas.");
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

                // Envía mensajes a todos los jugadores en el mundo
                if (secondsElapsed == 60) {
                    sendMessageToWorldPlayers(worldName, "Quedan 4 minutos hasta que termine el tiempo de preparación.");
                    playSoundToWorldPlayers(worldName);
                } else if (secondsElapsed == 120) {
                    sendMessageToWorldPlayers(worldName, "Quedan 3 minutos hasta que termine el tiempo de preparación.");
                    playSoundToWorldPlayers(worldName);
                } else if (secondsElapsed == 180) {
                    sendMessageToWorldPlayers(worldName, "Quedan 2 minutos hasta que termine el tiempo de preparación.");
                    playSoundToWorldPlayers(worldName);
                } else if (secondsElapsed == 240) {
                    sendMessageToWorldPlayers(worldName, "Queda 1 minuto hasta que termine el tiempo de preparación.");
                    playSoundToWorldPlayers(worldName);
                } else if (secondsElapsed == 290) {
                    sendMessageToWorldPlayers(worldName, "Quedan 10 segundos hasta que termine el tiempo de preparación.");
                    playSoundToWorldPlayers(worldName);
                } else if (secondsElapsed == 295) {
                    sendMessageToWorldPlayers(worldName, "Quedan 5 segundos hasta que termine el tiempo de preparación.");
                    playSoundToWorldPlayers(worldName);
                } else if (secondsElapsed == 296) {
                    sendMessageToWorldPlayers(worldName, "Quedan 4 segundos hasta que termine el tiempo de preparación.");
                    playSoundToWorldPlayers(worldName);
                } else if (secondsElapsed == 297) {
                    sendMessageToWorldPlayers(worldName, "Quedan 3 segundos hasta que termine el tiempo de preparación.");
                    playSoundToWorldPlayers(worldName);
                } else if (secondsElapsed == 298) {
                    sendMessageToWorldPlayers(worldName, "Quedan 2 segundos hasta que termine el tiempo de preparación.");
                    playSoundToWorldPlayers(worldName);
                } else if (secondsElapsed == 299) {
                    sendMessageToWorldPlayers(worldName, "Queda 1 segundo hasta que termine el tiempo de preparación.");
                    playSoundToWorldPlayers(worldName);
                } else if (secondsElapsed == 300) {
                    worldProtectionStatus.put(worldName, false);
                    protectionTimers.remove(worldName);
                    sendMessageToWorldPlayers(worldName, "El tiempo de preparación ha terminado.");
                    playSoundToWorldPlayers(worldName);
                    this.cancel();
                }
            }
        };
        task.runTaskTimer(plugin, 0, 20); // Se ejecuta cada segundo (20 ticks)
        protectionTimers.put(worldName, task);
        player.sendMessage(ChatColor.YELLOW + "Protección de colocación de bloques iniciada por 5 minutos en el mundo " + worldName);
    }

    private void playSoundToWorldPlayers(String worldName) {
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            if (onlinePlayer.getWorld().getName().equals(worldName)) {
                onlinePlayer.playSound(onlinePlayer.getLocation(), "BLOCK_LEVER_CLICK", 1.0f, 1.0f);
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
            player.sendMessage(ChatColor.YELLOW + "Protección de colocación de bloques detenida en el mundo " + worldName);
            Bukkit.getLogger().info("La protección de colocación de bloques ha sido detenida en el mundo " + worldName);
        } else {
            player.sendMessage(ChatColor.YELLOW + "No se encontró protección activa en el mundo " + worldName);
        }
    }
}
