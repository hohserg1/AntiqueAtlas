package hunternif.mc.atlas.item;

import hunternif.mc.atlas.client.ariadne.thread.RecordingHandler;
import hunternif.mc.atlas.map.objects.path.Path;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;

import javax.annotation.Nullable;
import java.util.List;

import static hunternif.mc.atlas.RegistrarAntiqueAtlas.ARIADNE_THREAD;

public class ItemAriadneThread extends Item {

    {
        setRegistryName("ariadne_thread");
        setTranslationKey("ariadne_thread");
    }

    public static final int maxQueueSize = 10;

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer playerIn, EnumHand hand) {
        ItemStack heldItem = playerIn.getHeldItem(hand);

        if (playerIn.isSneaking()) {
            if (world.isRemote)
                RecordingHandler.stop();

            markActive(heldItem, false, playerIn);

            if (!playerIn.capabilities.isCreativeMode)
                playerIn.getCooldownTracker().setCooldown(this, 20 * 60);

        } else {
            if (heldItem.getCount() == 1) {
                markActive(heldItem, true, playerIn);
            } else {
                ItemStack r = heldItem.copy();
                r.setCount(1);
                markActive(r, true, playerIn);
                if (playerIn.addItemStackToInventory(r)) {
                    heldItem.shrink(1);
                }
            }
            if (world.isRemote)
                RecordingHandler.start(heldItem);
        }

        return new ActionResult<>(EnumActionResult.SUCCESS, heldItem);
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        super.addInformation(stack, worldIn, tooltip, flagIn);
        tooltip.add(I18n.format("item.ariadne_thread.tooltip1"));
        tooltip.add(I18n.format("item.ariadne_thread.tooltip2"));
    }

    @Override
    public boolean hasEffect(ItemStack stack) {
        return isActive(stack);
    }

    public boolean hasColor(ItemStack stack) {
        NBTTagCompound nbttagcompound = stack.getTagCompound();
        return nbttagcompound != null &&
                nbttagcompound.hasKey("display", Constants.NBT.TAG_COMPOUND) &&
                nbttagcompound.getCompoundTag("display").hasKey("color", Constants.NBT.TAG_INT);
    }

    public int getColor(ItemStack stack) {
        if (hasColor(stack))
            return stack.getTagCompound().getCompoundTag("display").getInteger("color");

        return 0xffFFffFF;
    }

    public void removeColor(ItemStack stack) {
        if (stack.hasTagCompound()) {
            stack.getTagCompound().getCompoundTag("display").removeTag("color");
        }
    }

    public void setColor(ItemStack stack, int color) {
        stack.getOrCreateSubCompound("display").setInteger("color", color);
    }

    private static String startKey = "start";
    private static String segmentsKey = "segments";
    private static String activeKey = "active";

    public static boolean isActive(ItemStack stack) {
        return stack.getItem() == ARIADNE_THREAD && RecordingHandler.isActive() && stack.hasTagCompound() && stack.getTagCompound().getBoolean(activeKey);
    }

    public static void markActive(ItemStack stack, boolean active, EntityPlayer player) {
        initNbt(stack);
        if (active)
            if (!stack.getTagCompound().hasKey(segmentsKey))
                stack.getTagCompound().setLong(startKey, posOfPlayer(player).toLong());

        stack.getTagCompound().setBoolean(activeKey, active);
    }

    private static void initNbt(ItemStack stack) {
        if (!stack.hasTagCompound()) {
            stack.setTagCompound(new NBTTagCompound());
        }
    }

    public static BlockPos posOfPlayer(EntityPlayer player) {
        return new BlockPos(player.posX, player.posY + 0.5D, player.posZ);
    }


    public static void append(ItemStack stack, List<Short> addition) {
        initNbt(stack);

        byte[] segmentsBytes = stack.getTagCompound().getByteArray(segmentsKey);
        short[] segments = Path.loadSegments(segmentsBytes, new short[segmentsBytes.length / 2 + addition.size()]);

        for (int i = 0; i < addition.size(); i++) {
            segments[i + segmentsBytes.length / 2] = addition.get(i);
        }
        stack.getTagCompound().setByteArray(segmentsKey, Path.saveSegments(segments));
    }

    public static BlockPos getStart(ItemStack stack) {
        initNbt(stack);
        if (stack.getTagCompound().hasKey(startKey, Constants.NBT.TAG_LONG))
            return BlockPos.fromLong(stack.getTagCompound().getLong(startKey));
        else
            return null;
    }

    public static short[] getPath(ItemStack stack) {
        if (!stack.hasTagCompound())
            return null;

        byte[] segmentsBytes = stack.getTagCompound().getByteArray(segmentsKey);
        if (segmentsBytes.length == 0)
            return null;

        return Path.loadSegments(segmentsBytes);
    }

}
