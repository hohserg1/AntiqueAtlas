package hunternif.mc.atlas.client.waypoint.star;

import java.awt.*;

public class RGB {
    public final float r, g, b;

    public RGB(float r, float g, float b) {
        this.r = r;
        this.g = g;
        this.b = b;
    }

    public static RGB from(Color c) {
        return new RGB( c.getRed() / 255f,  c.getGreen() / 255f,  c.getBlue() / 255f);
    }

    public static final RGB white = new RGB(1, 1, 1);
}
