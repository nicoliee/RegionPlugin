package org.ttchampagne.regionplugin.commands;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.net.URL;

import org.bukkit.command.Command;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.ttchampagne.regionplugin.RegionPlugin;
import org.ttchampagne.regionplugin.update.AutoUpdate;
import org.ttchampagne.utils.SendMessage;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

public class RegionPluginCommand implements CommandExecutor, TabCompleter {
    private JavaPlugin plugin;
    public RegionPluginCommand(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    // Método que se llama cuando se ejecuta el comando
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Verificar si el usuario tiene permisos
        if (!sender.hasPermission("towers.admin") && !sender.isOp()) {
            sender.sendMessage(((RegionPlugin) plugin).getErrorMessage("errors.no_player"));
            return true;
        }
        // Si el comando es "/RegionPlugin"
        if (command.getName().equalsIgnoreCase("RegionPlugin")) {
            // Si no hay argumentos, muestra la versión actual
            if (args.length == 0) {
                String currentVersion = plugin.getDescription().getVersion();
                sender.sendMessage("§8[§bRegionPlugin§8] §7v" + currentVersion);
                return true;
            // Si el comando es "/regionplugin update"
            }// Si el comando es "/regionplugin setlang <es/en>"
            else if (args[0].equalsIgnoreCase("setlang")) {
                // Verificar que el idioma sea "es" o "en"
                if (args.length < 2 || (!args[1].equalsIgnoreCase("es") && !args[1].equalsIgnoreCase("en"))) {
                    sender.sendMessage(ChatColor.RED + "/RegionPlugin setlang <es/en>");
                    return true;
                }
                // Establecer el idioma seleccionado en config.yml
                String selectedLang = args[1].toLowerCase();
                plugin.getConfig().set("language", selectedLang); // Actualiza el idioma en config.yml
                plugin.saveConfig(); // Guarda la nueva configuración
                SendMessage.sendToPlayer((Player) sender, ((RegionPlugin) plugin).getErrorMessage("language.set")); // Mensaje de confirmación

                // Recargar el plugin para aplicar el cambio de idioma
                plugin.reloadConfig(); // Recarga el archivo de configuración

                return true;
            } else if (args[0].equalsIgnoreCase("update")) {
                String currentVersion = plugin.getDescription().getVersion();
                SendMessage.sendToPlayer((Player) sender, "§8[§bRegionPlugin§8] §7v" + currentVersion);
                AutoUpdate updateChecker = new AutoUpdate(plugin);
                updateChecker.checkForUpdates();
                return true;
            // Si el comando es "/regionplugin reload"
            } else if (args[0].equalsIgnoreCase("reload")) {
                SendMessage.sendToPlayer((Player) sender, ((RegionPlugin) plugin).getErrorMessage("reload.start"));
                plugin.getServer().getPluginManager().disablePlugin(plugin);
                plugin.getServer().getPluginManager().enablePlugin(plugin);
                SendMessage.sendToPlayer((Player) sender, ((RegionPlugin) plugin).getErrorMessage("reload.success"));
                return true;
            // Si el comando es "/regionplugin messagesreload"
            } else if (args[0].equalsIgnoreCase("messagesreload")) {
                File messagesFile = new File(plugin.getDataFolder(), "messages.yml");
                SendMessage.sendToPlayer((Player) sender, ((RegionPlugin) plugin).getErrorMessage("messages.reloadStart"));
                if (messagesFile.exists() && !messagesFile.delete()) {
                    SendMessage.sendToPlayer((Player) sender, ((RegionPlugin) plugin).getErrorMessage("messages.reloadError"));
                    return true;
                }
                plugin.saveResource("messages.yml", false);
                ((RegionPlugin) plugin).saveDefaultMessages();
                SendMessage.sendToPlayer((Player) sender, ((RegionPlugin) plugin).getErrorMessage("messages.reloadSuccess"));
                return true;
            // Si el comando es "/regionplugin configreplace"
            } else if (args[0].equalsIgnoreCase("configreplace")) {
                SendMessage.sendToPlayer((Player) sender, ((RegionPlugin) plugin).getErrorMessage("configReplace.start"));
                String zipFileUrl = "https://github.com/nicoliee/configForTowers/archive/refs/heads/main.zip";
                File pluginsFolder = new File("plugins");
                try {
                    downloadFile(zipFileUrl, new File(pluginsFolder, "configForTowers.zip"));
                    unzipFile(new File(pluginsFolder, "configForTowers.zip"), pluginsFolder);
                    new File(pluginsFolder, "configForTowers.zip").delete();
                } catch (IOException e) {
                    e.printStackTrace();
                    SendMessage.sendToPlayer((Player) sender, ((RegionPlugin) plugin).getErrorMessage("configReplace.error"));
                    return true;
                }
                SendMessage.sendToPlayer((Player) sender, ((RegionPlugin) plugin).getErrorMessage("configReplace.success"));
                return true;
            }else if (args[0].equalsIgnoreCase("serverStop")) {
                plugin.getServer().shutdown();
                return true;
            } else {
                sender.sendMessage(ChatColor.RED + "/RegionPlugin <configreplace|messagesreload|reload|setlang|update>");
                return true;
            }
        }        
        // Comando no pertenece a "/RegionPlugin"
        return false;
    }
    
    private void downloadFile(String fileUrl, File destinationFile) throws IOException {
        URL url = new URL(fileUrl);
        try (InputStream in = url.openStream(); FileOutputStream out = new FileOutputStream(destinationFile)) {
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
        }
    }

    private void unzipFile(File zipFile, File destinationFolder) throws IOException {
        try (ZipInputStream zipIn = new ZipInputStream(new FileInputStream(zipFile))) {
            ZipEntry entry = zipIn.getNextEntry();
            
            // Si la primera entrada tiene un prefijo, se utiliza para ajustar las rutas
            String mainDirPrefix = entry.getName().split("/")[0] + "/";
            
            // Recorre todas las entradas del ZIP
            while (entry != null) {
                String fileName = entry.getName();
                
                // Elimina el prefijo del nombre del archivo para evitar el directorio principal
                if (fileName.startsWith(mainDirPrefix)) {
                    fileName = fileName.substring(mainDirPrefix.length());
                }
                
                // Crea los archivos o directorios en el destino
                File file = new File(destinationFolder, fileName);
                if (entry.isDirectory()) {
                    file.mkdirs();
                } else {
                    // Asegúrate de crear el directorio si no existe
                    file.getParentFile().mkdirs();
                    
                    try (BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(file))) {
                        byte[] buffer = new byte[1024];
                        int bytesRead;
                        while ((bytesRead = zipIn.read(buffer)) != -1) {
                            out.write(buffer, 0, bytesRead);
                        }
                    }
                }
                zipIn.closeEntry();
                entry = zipIn.getNextEntry();
            }
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            // Lista de opciones posibles
            List<String> options = Arrays.asList("configReplace", "messagesReload", "reload", "serverStop", "setLang", "update");

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
        return new ArrayList<>();
    }
}
