package hunternif.mc.atlas.util;

import org.lwjgl.input.Mouse;

public class CustomMouseHeler {

    public static void grabMouseCursor() {
        Mouse.setGrabbed(true);
    }

    public static void ungrabMouseCursor() {
        int x = Mouse.getX();
        int y = Mouse.getY();
        Mouse.setGrabbed(false);
        Mouse.setCursorPosition(x, y);
    }
}
