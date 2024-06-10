package hunternif.mc.atlas.network.server;

import com.google.common.base.Preconditions;
import hunternif.mc.atlas.RegistrarAntiqueAtlas;
import hunternif.mc.atlas.item.ItemAriadneThread;
import hunternif.mc.atlas.map.objects.path.Segment;
import hunternif.mc.atlas.network.AbstractMessage;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.relauncher.Side;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static hunternif.mc.atlas.item.ItemAriadneThread.maxQueueSize;

public class FlushAriadneThreadPoses extends AbstractMessage.AbstractServerMessage<FlushAriadneThreadPoses> {
    private int itemSlot;
    private List<Short> additionalSegments;

    public FlushAriadneThreadPoses() {
    }

    public FlushAriadneThreadPoses(int itemSlot, List<Short> additionalSegments) {
        this.itemSlot = itemSlot;
        this.additionalSegments = additionalSegments;
        Preconditions.checkArgument(additionalSegments.size() <= maxQueueSize, "too many poses at once");
    }

    @Override
    protected void read(PacketBuffer buffer) throws IOException {
        itemSlot = buffer.readByte();
        byte posesCount = buffer.readByte();

        if (posesCount > maxQueueSize)
            throw new IOException("too many poses at once. hacking?");


        additionalSegments = new ArrayList<>(posesCount);
        for (int i = 0; i < posesCount; i++) {
            short segment = buffer.readShort();
            if (0 <= segment && segment <= Segment.maxIndex)
                additionalSegments.add(segment);
            else
                throw new IOException("illegal segment index(" + segment + "). hacking? ");
        }
    }

    @Override
    protected void write(PacketBuffer buffer) throws IOException {
        buffer.writeByte(itemSlot);
        buffer.writeByte(additionalSegments.size());
        additionalSegments.forEach(buffer::writeShort);
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
            ItemAriadneThread.append(stack, additionalSegments);
        }
    }
}
