package hunternif.mc.atlas.client.ariadne.thread;

import hunternif.mc.atlas.AntiqueAtlasMod;
import hunternif.mc.atlas.item.ItemAriadneThread;
import hunternif.mc.atlas.network.PacketDispatcher;
import hunternif.mc.atlas.network.server.FlushAriadneThreadPoses;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.ArrayList;
import java.util.List;

import static hunternif.mc.atlas.item.ItemAriadneThread.maxQueueSize;
import static net.minecraft.util.math.Vec3d.ZERO;

@Mod.EventBusSubscriber(modid = AntiqueAtlasMod.ID, value = Side.CLIENT)
public class RecordingHandler {
    public static final long maxFlushPeriodMS = 1000 * 60;
    public static final long maxPeriodWithoutAddingMS = 1000 * 60 * 5;
    public static final double directionAngleValueableDifference = Math.toRadians(45 / 2);
    private static long lastFlushTime = 0;
    private static long lastAddTime = 0;
    private static Vec3d direction;
    private static int ticks = 0;
    private static boolean recording = false;
    private static List<BlockPos> sendQueue = new ArrayList<>();

    public static boolean isActive() {
        return recording;
    }

    public static void start(ItemStack heldItem) {
        if (!recording) {
            recording = true;
            RenderHandler.load(heldItem);
            addPos();
        }
    }

    public static void stop() {
        if (recording) {
            addPos();
            flush();
            RenderHandler.clear();
            recording = false;
        }
    }


    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (recording) {
            ticks++;
            EntityPlayerSP player = Minecraft.getMinecraft().player;
            if (player == null)
                return;

            int activeItem = getActiveItem(player);
            if (activeItem == -2) {
                recording = false;
                return;
            }

            if (direction == null)
                direction = getCurrentDirection();

            if (diff(direction, getCurrentDirection()) > directionAngleValueableDifference) {
                direction = getCurrentDirection();
                addPos();

            } else {
                long currentTimeMillis = System.currentTimeMillis();
                if (currentTimeMillis - lastAddTime > maxPeriodWithoutAddingMS) {
                    addPos();
                }
            }

            long currentTimeMillis = System.currentTimeMillis();
            if (currentTimeMillis - lastFlushTime >= maxFlushPeriodMS) {
                lastFlushTime = currentTimeMillis;
                flush();
            }
        }
    }

    private static double diff(Vec3d a, Vec3d b) {
        if (a == ZERO || b == ZERO)
            return directionAngleValueableDifference + 1;

        return Math.acos((a.x * b.x + a.y * b.y + a.z * b.z) / (a.length() * b.length()));
    }

    private static Vec3d getCurrentDirection() {
        EntityPlayerSP player = Minecraft.getMinecraft().player;
        return new Vec3d(player.motionX, player.motionY, player.motionZ).normalize();
    }

    public static int getActiveItem(EntityPlayer player) {
        if (ItemAriadneThread.isActive(player.getHeldItemMainhand()))
            return player.inventory.currentItem;

        if (ItemAriadneThread.isActive(player.getHeldItemOffhand()))
            return 40;

        for (int i = 0; i < player.inventory.getSizeInventory(); i++) {
            if (ItemAriadneThread.isActive(player.inventory.getStackInSlot(i))) {
                return i;
            }
        }

        if (ItemAriadneThread.isActive(player.inventory.getItemStack()))
            return -1;

        return -2;
    }

    public static void addPos() {
        EntityPlayerSP player = Minecraft.getMinecraft().player;
        BlockPos pos = new BlockPos(player.posX, player.posY + 0.5D, player.posZ);
        if (!sendQueue.isEmpty() && sendQueue.get(sendQueue.size() - 1).distanceSq(pos) < 9)
            return;

        RenderHandler.addPos(pos);
        sendQueue.add(pos);
        lastAddTime = System.currentTimeMillis();
    }

    public static void flush() {

        if (sendQueue.isEmpty())
            return;

        if (sendQueue.size() > maxQueueSize) {
            sendQueue.subList(maxQueueSize - 1, sendQueue.size()).clear();
            RenderHandler.clearLast(sendQueue.size() - maxQueueSize + 1);
            addPos();
        }
        int activeItem = getActiveItem(Minecraft.getMinecraft().player);
        if (activeItem >= -1) {
            PacketDispatcher.sendToServer(new FlushAriadneThreadPoses(activeItem, sendQueue));
            sendQueue = new ArrayList<>();
        }
    }
}
