package hunternif.mc.atlas.client.gui;

import hunternif.mc.atlas.client.Textures;
import hunternif.mc.atlas.util.AtlasRenderHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.resources.I18n;
import net.minecraft.init.SoundEvents;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import static hunternif.mc.atlas.client.Textures.FILTERING_BOOKMARKS;
import static hunternif.mc.atlas.client.Textures.ICON_FILTER_MARKERS;

public class GuiFilterButton extends GuiBookmarkButton {
    private final List<Consumer<Integer>> listeners = new ArrayList<>();

    GuiFilterButton() {
        super(3, ICON_FILTER_MARKERS, I18n.format("gui.antiqueatlas.filterMarkers"));
    }

    public void addListener2(Consumer<Integer> listener) {
        listeners.add(listener);
    }

    @Override
    protected void mouseClicked(int x, int y, int mouseButton) throws IOException {
        super.mouseClicked(x, y, mouseButton);
        if (isEnabled() && isMouseOver) {

            mc.getSoundHandler().playSound(PositionedSoundRecord.getMasterRecord(SoundEvents.UI_BUTTON_CLICK, 1.0F));

            for (Consumer<Integer> listener : listeners)
                listener.accept(mouseButton);

        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        int backspace = 14;
        int enter = 28;

        if (textFieldFocused) {
            if (keyCode == backspace) {
                if (text.length() > 0)
                    text = text.substring(0, text.length() - 1);
            } else if (keyCode != enter && (Character.isLetterOrDigit(typedChar) || Character.isWhitespace(typedChar)))
                text += Character.toLowerCase(typedChar);
        } else
            super.keyTyped(typedChar, keyCode);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTick) {
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        RenderHelper.disableStandardItemLighting();

        // Render background:
        int u = 3 * WIDTH;
        int v = isMouseOver || isSelected() ? 0 : HEIGHT;
        if (expanded)
            AtlasRenderHelper.drawTexturedRect(FILTERING_BOOKMARKS, getGuiX(), getGuiY(), 0, 0, WIDTH * 4, HEIGHT, 84, 18);
        AtlasRenderHelper.drawTexturedRect(Textures.BOOKMARKS, getGuiX(), getGuiY(), u, v, WIDTH, HEIGHT, IMAGE_WIDTH, IMAGE_HEIGHT);

        // Render the icon:
        GlStateManager.enableBlend();
        AtlasRenderHelper.drawFullTexture(iconTexture,
                getGuiX() + (isMouseOver ? 3 : 2),
                getGuiY() + 1, ICON_WIDTH, ICON_HEIGHT);

        if (isMouseOver) {
            drawTooltip(Collections.singletonList(title), Minecraft.getMinecraft().fontRenderer);
        }

        if (expanded)
            Minecraft.getMinecraft().fontRenderer.drawString(text + (textFieldFocused ? (System.currentTimeMillis() % 1400 < 700 ? "_" : "") : ""), getGuiX() + ICON_WIDTH + 7, getGuiY() + 4, 0xff000000);

        GlStateManager.color(1, 1, 1, 1);
    }

    private boolean expanded = false;
    public boolean textFieldFocused = false;

    @Override
    protected int getWidth() {
        return super.getWidth() + (expanded ? WIDTH * 3 : 0);
    }

    public String getText() {
        return text;
    }

    private String text = "";

    public void clear() {
        text = "";

    }

    public void show() {
        focus();
        expanded = true;

    }

    public void hide() {
        clear();
        unfocus();
        expanded = false;
    }

    public void focus() {
        textFieldFocused = true;
    }

    public void unfocus() {
        textFieldFocused = false;
    }
}
