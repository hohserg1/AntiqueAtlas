package hunternif.mc.atlas.client;

import hunternif.mc.atlas.AntiqueAtlasMod;
import hunternif.mc.atlas.SettingsConfig;
import kenkron.antiqueatlasoverlay.AAOConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.relauncher.Side;

import static hunternif.mc.atlas.RegistrarAntiqueAtlas.ATLAS;
import static net.minecraft.util.math.MathHelper.clamp;

@Mod.EventBusSubscriber(value = Side.CLIENT, modid = AntiqueAtlasMod.ID)
public class IngameScaleToggler {
    public static boolean scaleChanged = false;
    public static int scaleChangeDir = 0;

    private static int prevCurrentItem = -1;

    @SubscribeEvent
    public static void onMouse(InputEvent.MouseInputEvent event) {
        EntityPlayerSP player = Minecraft.getMinecraft().player;
        if (player.isSneaking() && prevCurrentItem != -1) {
            if (player.inventory.getStackInSlot(prevCurrentItem).getItem() == ATLAS || player.getHeldItemOffhand().getItem() == ATLAS) {
                if (player.inventory.currentItem != prevCurrentItem) {
                    int dir;
                    if (prevCurrentItem == 0 && player.inventory.currentItem == 8)
                        dir = 1;
                    else if (prevCurrentItem == 8 && player.inventory.currentItem == 0)
                        dir = -1;
                    else if (prevCurrentItem < player.inventory.currentItem)
                        dir = -1;
                    else
                        dir = 1;

                    dir *= SettingsConfig.userInterface.doReverseWheelZoom ? -1 : 1;

                    player.inventory.currentItem = prevCurrentItem;

                    int currentTileSize = AAOConfig.appearance.tileSize;

                    currentTileSize += dir;
                    currentTileSize = clamp(currentTileSize, 4, 20);

                    if (AAOConfig.appearance.tileSize != currentTileSize) {
                        AAOConfig.appearance.tileSize = currentTileSize;
                        scaleChanged = true;
                        scaleChangeDir = dir;
                    }
                }
            }
        }
        prevCurrentItem = player.inventory.currentItem;
    }
}
