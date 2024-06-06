package hunternif.mc.atlas.network.server;

import com.google.common.base.Preconditions;
import hunternif.mc.atlas.RegistrarAntiqueAtlas;
import hunternif.mc.atlas.item.ItemAriadneThread;
import hunternif.mc.atlas.network.AbstractMessage;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.relauncher.Side;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static hunternif.mc.atlas.item.ItemAriadneThread.maxQueueSize;

public class FlushAriadneThreadPoses extends AbstractMessage.AbstractServerMessage<FlushAriadneThreadPoses> {
    private int itemSlot;
    private List<BlockPos> poses;

    public FlushAriadneThreadPoses() {
    }

    public FlushAriadneThreadPoses(int itemSlot, List<BlockPos> poses) {
        this.itemSlot = itemSlot;
        this.poses = poses;
        Preconditions.checkArgument(poses.size() <= maxQueueSize, "too many poses at once");
    }

    @Override
    protected void read(PacketBuffer buffer) throws IOException {
        itemSlot = buffer.readByte();
        byte posesCount = buffer.readByte();

        if (posesCount > maxQueueSize)
            throw new IOException("too many poses at once. hacking?");

        poses = new ArrayList<>(posesCount);
        for (int i = 0; i < posesCount; i++) {
            poses.add(BlockPos.fromLong(buffer.readLong()));
        }
    }

    @Override
    protected void write(PacketBuffer buffer) throws IOException {
        buffer.writeByte(itemSlot);
        buffer.writeByte(poses.size());
        for (BlockPos p : poses) {
            buffer.writeLong(p.toLong());
        }
    }

    @Override
    protected void process(EntityPlayer player, Side side) {
        ItemStack stack;
        if (itemSlot == -1)
            stack = player.inventory.getItemStack();
        else if (0 <= itemSlot && itemSlot < player.inventory.getSizeInventory())
            stack = player.inventory.getStackInSlot(itemSlot);
        else
            return;

        if (stack.getItem() == RegistrarAntiqueAtlas.ARIADNE_THREAD) {
            ItemAriadneThread.append(stack, poses);
        }
    }
}
