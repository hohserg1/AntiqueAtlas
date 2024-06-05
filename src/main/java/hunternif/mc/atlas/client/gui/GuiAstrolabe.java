package hunternif.mc.atlas.client.gui;

import hunternif.mc.atlas.client.Textures;
import hunternif.mc.atlas.client.gui.core.GuiCursor;
import hunternif.mc.atlas.client.waypoint.star.RGB;
import hunternif.mc.atlas.client.waypoint.star.StarRenderer;
import hunternif.mc.atlas.util.CustomMouseHeler;
import net.minecraft.client.Minecraft;
import net.minecraft.util.math.BlockPos;

import java.awt.*;
import java.io.IOException;
import java.util.Random;

public class GuiAstrolabe extends GuiAtlas {
    public GuiAstrolabe() {
        removeChild(btnExportPng);
        removeChild(btnPublishMarker);
        removeChild(btnMarker);
        removeChild(btnDelMarker);
        removeChild(btnCopyMarker);
        removeChild(btnShowMarkers);
        selectionCursor.setTexture(Textures.ASTROLABE, 12, 14, 3, 8);
    }

    @Override
    public void setWorldAndResolution(Minecraft mc, int width, int height) {
        super.setWorldAndResolution(mc, width, height);
        CustomMouseHeler.grabMouseCursor();
        addChild(selectionCursor);
    }

    @Override
    public void onGuiClosed() {
        super.onGuiClosed();
        CustomMouseHeler.ungrabMouseCursor();
        removeChild(selectionCursor);
    }

    protected final GuiCursor selectionCursor = new GuiCursor();

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseState) throws IOException {
        // If clicked on the map, start dragging
        int mapX = (width - MAP_WIDTH) / 2;
        int mapY = (height - MAP_HEIGHT) / 2;
        boolean isMouseOverMap = mouseX >= mapX && mouseX <= mapX + MAP_WIDTH && mouseY >= mapY && mouseY <= mapY + MAP_HEIGHT;

        if (state.is(EDITING_FILTER))
            state.switchTo(NORMAL);

        if (isMouseOverMap) {
            if (hoveredMarker == null) {
                isDragging = true;
                dragMouseX = mouseX;
                dragMouseY = mouseY;
                dragMapOffsetX = mapOffsetX;
                dragMapOffsetY = mapOffsetY;
            } else {
                StarRenderer.selected = new BlockPos(hoveredMarker.getX(), 0, hoveredMarker.getZ());
                rand.setSeed(StarRenderer.selected.toLong());
                StarRenderer.color = RGB.from(new Color(rand.nextFloat(), rand.nextFloat(), rand.nextFloat()).brighter());
                close();
            }
        }
    }

    private Random rand = new Random();
}
