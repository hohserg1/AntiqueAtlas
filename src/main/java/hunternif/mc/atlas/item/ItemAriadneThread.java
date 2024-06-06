package hunternif.mc.atlas.item;

import com.google.common.collect.ImmutableList;
import hunternif.mc.atlas.client.ariadne.thread.RecordingHandler;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagLong;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;

import javax.annotation.Nullable;
import java.util.LinkedList;
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
            markActive(heldItem, false);

        } else {
            if (world.isRemote)
                RecordingHandler.start(heldItem);
            markActive(heldItem, true);
        }


        if (!playerIn.capabilities.isCreativeMode)
            playerIn.getCooldownTracker().setCooldown(this, 20 * 60);

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

    private static String posesKey = "poses";
    private static String activeKey = "active";

    public static boolean isActive(ItemStack stack) {
        return stack.getItem() == ARIADNE_THREAD && RecordingHandler.isActive() && stack.hasTagCompound() && stack.getTagCompound().getBoolean(activeKey);
    }

    public static void markActive(ItemStack stack, boolean active) {
        if (!stack.hasTagCompound()) {
            stack.setTagCompound(new NBTTagCompound());
        }
        stack.getTagCompound().setBoolean(activeKey, active);
    }


    public static void append(ItemStack stack, List<BlockPos> poses) {
        if (!stack.hasTagCompound()) {
            stack.setTagCompound(new NBTTagCompound());
        }

        NBTTagList posesNbt = stack.getTagCompound().getTagList(posesKey, Constants.NBT.TAG_LONG);
        for (BlockPos p : poses) {
            posesNbt.appendTag(new NBTTagLong(p.toLong()));
        }
        stack.getTagCompound().setTag(posesKey, posesNbt);
    }

    public static List<BlockPos> getPath(ItemStack stack) {
        if (!stack.hasTagCompound())
            return ImmutableList.of();

        NBTTagList posesNbt = stack.getTagCompound().getTagList(posesKey, Constants.NBT.TAG_LONG);
        if (posesNbt.isEmpty())
            return ImmutableList.of();

        List<BlockPos> r = new LinkedList<>();
        for (int i = 0; i < posesNbt.tagCount(); i++) {
            r.add(BlockPos.fromLong(((NBTTagLong) posesNbt.get(i)).getLong()));
        }
        return r;
    }

}
