package hunternif.mc.atlas.core;

import blusunrize.immersiveengineering.common.IEContent;
import blusunrize.immersiveengineering.common.util.ItemNBTHelper;
import hunternif.mc.atlas.AntiqueAtlasMod;
import hunternif.mc.atlas.network.PacketDispatcher;
import hunternif.mc.atlas.network.client.PickupSamplePacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;

import static hunternif.mc.atlas.RegistrarAntiqueAtlas.ATLAS;

@Mod.EventBusSubscriber(modid = AntiqueAtlasMod.ID)
public class ImmersiveCoreDrillHandler {

    @SubscribeEvent
    public static void onPickupSample(PlayerEvent.ItemPickupEvent event) {
        EntityPlayer player = event.player;
        World world = player.world;
        if (!world.isRemote) {
            ItemStack heldItemOffhand = player.getHeldItemOffhand();
            ItemStack heldItemMainhand = player.getHeldItemMainhand();
            int atlas =
                    heldItemOffhand.getItem() == ATLAS ? heldItemOffhand.getItemDamage() :
                            heldItemMainhand.getItem() == ATLAS ? heldItemMainhand.getItemDamage() :
                                    -1;
            if (atlas != -1) {
                ItemStack stack = event.getStack();
                if (stack.getItem() == IEContent.itemCoresample) {
                    if (ItemNBTHelper.hasKey(stack, "coords")) {
                        int[] coords = ItemNBTHelper.getIntArray(stack, "coords");
                        int dim = coords[0];
                        int x = coords[1];
                        int z = coords[2];

                        if (world.provider.getDimension() == dim) {

                            long timestamp = ItemNBTHelper.getLong(stack, "timestamp");
                            String mineral = ItemNBTHelper.getString(stack, "mineral");

                            boolean infinite = ItemNBTHelper.getBoolean(stack, "infinite");
                            int depletion = ItemNBTHelper.getInt(stack, "depletion");

                            int oil = ItemNBTHelper.getInt(stack, "oil");
                            String resType = ItemNBTHelper.getString(stack, "resType");


                            PacketDispatcher.sendTo(new PickupSamplePacket(timestamp, mineral, infinite, depletion, oil, resType, x, z), (EntityPlayerMP) player);
                        }
                    }
                }
            }
        }
    }

}
