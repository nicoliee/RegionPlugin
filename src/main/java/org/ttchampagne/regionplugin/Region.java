package org.ttchampagne.regionplugin;
import org.bukkit.Location;

public class Region {
    // Atributos de la clase
    private Location p1;
    private Location p2;

    public Region(Location p1, Location p2) {
        // Inicialización de los atributos
        this.p1 = p1;
        this.p2 = p2;
    }

    public boolean isInside(Location loc) {
        // Obtener las coordenadas mínimas y máximas de la región
        double x1 = Math.min(p1.getX(), p2.getX()); // Coordenada mínima en X
        double y1 = Math.min(p1.getY(), p2.getY()); // Coordenada mínima en Y
        double z1 = Math.min(p1.getZ(), p2.getZ()); // Coordenada mínima en Z
        double x2 = Math.max(p1.getX(), p2.getX()); // Coordenada máxima en X
        double y2 = Math.max(p1.getY(), p2.getY()); // Coordenada máxima en Y
        double z2 = Math.max(p1.getZ(), p2.getZ()); // Coordenada máxima en Z

        // Verificar si la ubicación está dentro de la región
        return loc.getX() >= x1 && loc.getX() <= x2 &&
                loc.getY() >= y1 && loc.getY() <= y2 &&
                loc.getZ() >= z1 && loc.getZ() <= z2;
    }
    public void setP1(Location p1) {
        this.p1 = p1;
    }
    public void setP2(Location p2) {
        this.p2 = p2;
    }
    
}