package hunternif.mc.atlas.client.ariadne.thread;

import hunternif.mc.atlas.AntiqueAtlasMod;
import hunternif.mc.atlas.item.ItemAriadneThread;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import org.lwjgl.opengl.GL11;

import java.util.LinkedList;
import java.util.List;

import static hunternif.mc.atlas.RegistrarAntiqueAtlas.ARIADNE_THREAD;
import static org.lwjgl.opengl.GL11.*;

@Mod.EventBusSubscriber(modid = AntiqueAtlasMod.ID, value = Side.CLIENT)
public class RenderHandler {
    private static LinkedList<BlockPos> recordingPath = new LinkedList<>();
    private static List<BlockPos> heldItemPath;

    private static ItemStack lastHoldItem = ItemStack.EMPTY;

    public static void addPos(BlockPos pos) {
        recordingPath.add(pos);
    }

    public static void clear() {
        recordingPath.clear();
    }

    public static void load(ItemStack stack) {
        recordingPath.clear();
        recordingPath.addAll(ItemAriadneThread.getPath(stack));
    }

    @SubscribeEvent
    public static void render(RenderWorldLastEvent event) {
        ItemStack current = getCurrentHeldItem();
        if (current.isEmpty() || ItemAriadneThread.isActive(current)) {
            if (RecordingHandler.isActive()) {
                renderLine(recordingPath, event.getPartialTicks());
            }
        } else {
            if (current != lastHoldItem) {
                lastHoldItem = current;
                heldItemPath = ItemAriadneThread.getPath(current);
            }
            renderLine(heldItemPath, event.getPartialTicks());
        }
    }

    private static ItemStack getCurrentHeldItem() {
        EntityPlayerSP self = Minecraft.getMinecraft().player;

        if (self.getHeldItemMainhand().getItem() == ARIADNE_THREAD)
            return self.getHeldItemMainhand();

        else if (self.getHeldItemOffhand().getItem() == ARIADNE_THREAD)
            return self.getHeldItemOffhand();

        else
            return ItemStack.EMPTY;
    }

    private static void renderLine(List<BlockPos> poses, float partialTicks) {
        BufferBuilder buffer = Tessellator.getInstance().getBuffer();
        buffer.begin(GL11.GL_LINE_STRIP, DefaultVertexFormats.POSITION_COLOR);

        for (BlockPos p : poses) {
            buffer.pos(p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5).color(49/255f, 165/255f, 0, 0.8f).endVertex();
        }

        EntityPlayerSP self = Minecraft.getMinecraft().player;
        double x = self.lastTickPosX + (self.posX - self.lastTickPosX) * partialTicks;
        double y = self.lastTickPosY + (self.posY - self.lastTickPosY) * partialTicks;
        double z = self.lastTickPosZ + (self.posZ - self.lastTickPosZ) * partialTicks;
        setupGL();
        GlStateManager.translate(-x, -y, -z);

        buffer.pos(self.posX, self.posY + 1, self.posZ).color(0, 0.7f, 0, 0.8f).endVertex();

        Tessellator.getInstance().draw();

        resetGL();
    }

    private static void setupGL() {
        GlStateManager.pushMatrix();

        GlStateManager.disableTexture2D();
        GlStateManager.color(1, 1, 1, 1);
        GlStateManager.disableLighting();
        GlStateManager.disableAlpha();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glEnable(GL_LINE_SMOOTH);
        GlStateManager.glLineWidth(2);
    }

    private static void resetGL() {
        GlStateManager.popMatrix();

        GlStateManager.enableTexture2D();
        GlStateManager.enableAlpha();
        GlStateManager.disableBlend();
        glDisable(GL_LINE_SMOOTH);
    }

    public static void clearLast(int amount) {
        for (int i = 0; i < amount; i++) {
            recordingPath.removeLast();
        }
    }
}
