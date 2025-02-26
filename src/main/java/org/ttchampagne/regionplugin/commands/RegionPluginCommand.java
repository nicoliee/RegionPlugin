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
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.TabCompleter;

public class RegionPluginCommand implements CommandExecutor, TabCompleter {
    private JavaPlugin plugin;
    public RegionPluginCommand(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    // Método que se llama cuando se ejecuta el comando
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Verificar si el usuario tiene permisos
        if (!sender.hasPermission("towers.admin") && !sender.isOp()) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    ((RegionPlugin) plugin).getMessagesConfig().getString("messages.no_permission")));
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
            } else if (args[0].equalsIgnoreCase("update")) {
                String currentVersion = plugin.getDescription().getVersion();
                sender.sendMessage("§8[§bRegionPlugin§8] §7v" + currentVersion);
                AutoUpdate updateChecker = new AutoUpdate(plugin);
                updateChecker.checkForUpdates();
                return true;
            // Si el comando es "/regionplugin reload"
            } else if (args[0].equalsIgnoreCase("reload")) {
                sender.sendMessage("§8[§bRegionPlugin§8] §7Reloading...");
                plugin.getServer().getPluginManager().disablePlugin(plugin);
                plugin.getServer().getPluginManager().enablePlugin(plugin);
                sender.sendMessage("§8[§bRegionPlugin§8] §7Reloaded.");
                return true;
            // Si el comando es "/regionplugin messagesreload"
            } else if (args[0].equalsIgnoreCase("messagesreload")) {
                File messagesFile = new File(plugin.getDataFolder(), "messages.yml");
                sender.sendMessage("§8[§bRegionPlugin§8] §eStarting the messages reset process...");
                if (messagesFile.exists() && !messagesFile.delete()) {
                    sender.sendMessage("§8[§bRegionPlugin§8] §cFailed to delete the messages file.");
                    return true;
                }
                plugin.saveResource("messages.yml", false);
                ((RegionPlugin) plugin).saveDefaultMessages();
                sender.sendMessage("§8[§bRegionPlugin§8] §aMessages have been successfully reset and reloaded.");
                return true;
            // Si el comando es "/regionplugin configreplace"
            } else if (args[0].equalsIgnoreCase("configreplace")) {
                sender.sendMessage("§8[§bRegionPlugin§8] §eDownloading and extracting the configuration files...");
                String zipFileUrl = "https://github.com/nicoliee/configForTowers/archive/refs/heads/main.zip";
                File pluginsFolder = new File("plugins");
                try {
                    downloadFile(zipFileUrl, new File(pluginsFolder, "configForTowers.zip"));
                    unzipFile(new File(pluginsFolder, "configForTowers.zip"), pluginsFolder);
                    new File(pluginsFolder, "configForTowers.zip").delete();
                } catch (IOException e) {
                    e.printStackTrace();
                    sender.sendMessage("§8[§bRegionPlugin§8] §cFailed to download and extract the configuration files.");
                    return true;
                }
                sender.sendMessage("§8[§bRegionPlugin§8] §aThe configuration files have been successfully downloaded and extracted.");
                return true;
            }else if (args[0].equalsIgnoreCase("serverStop")) {
                plugin.getServer().shutdown();
                return true;
            } else {
                sender.sendMessage(ChatColor.RED + "Use /RegionPlugin <configreplace|messagesreload|reload|update>");
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
            List<String> options = Arrays.asList("configReplace", "messagesReload", "reload", "update", "serverStop");

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
