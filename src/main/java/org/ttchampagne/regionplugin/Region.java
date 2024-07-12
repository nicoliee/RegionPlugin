package org.ttchampagne.regionplugin;

import org.bukkit.Location;

public class Region {

    private final Location p1;
    private final Location p2;

    public Region(Location p1, Location p2) {
        this.p1 = p1;
        this.p2 = p2;
    }

    public boolean isInside(Location loc) {
        double x1 = Math.min(p1.getX(), p2.getX());
        double y1 = Math.min(p1.getY(), p2.getY());
        double z1 = Math.min(p1.getZ(), p2.getZ());
        double x2 = Math.max(p1.getX(), p2.getX());
        double y2 = Math.max(p1.getY(), p2.getY());
        double z2 = Math.max(p1.getZ(), p2.getZ());

        return loc.getX() >= x1 && loc.getX() <= x2 &&
                loc.getY() >= y1 && loc.getY() <= y2 &&
                loc.getZ() >= z1 && loc.getZ() <= z2;
    }
}