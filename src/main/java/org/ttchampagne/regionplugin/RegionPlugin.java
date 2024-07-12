package org.ttchampagne.regionplugin;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class RegionPlugin extends JavaPlugin {

    private final Map<String, Region> regions = new HashMap<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadRegions();
        RegionProtectionListener listener = new RegionProtectionListener(this);
        Bukkit.getPluginManager().registerEvents(listener, this);
        this.getCommand("startProtection").setExecutor(listener);
    }

    private void loadRegions() {
        FileConfiguration config = getConfig();
        Set<String> worlds = config.getKeys(false);

        for (String worldName : worlds) {
            String p1String = config.getString(worldName + ".P1");
            String p2String = config.getString(worldName + ".P2");

            if (p1String != null && p2String != null) {
                Location p1 = parseLocation(worldName, p1String);
                Location p2 = parseLocation(worldName, p2String);
                regions.put(worldName, new Region(p1, p2));
            }
        }
    }

    private Location parseLocation(String worldName, String locString) {
        String[] parts = locString.replace("(", "").replace(")", "").split(",");
        World world = Bukkit.getWorld(worldName);
        return new Location(world,
                Double.parseDouble(parts[0]),
                Double.parseDouble(parts[1]),
                Double.parseDouble(parts[2]));
    }

    public Map<String, Region> getRegions() {
        return regions;
    }
}