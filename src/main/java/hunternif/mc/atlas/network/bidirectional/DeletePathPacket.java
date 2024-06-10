package hunternif.mc.atlas.network.bidirectional;

import hunternif.mc.atlas.AntiqueAtlasMod;
import hunternif.mc.atlas.RegistrarAntiqueAtlas;
import hunternif.mc.atlas.SettingsConfig;
import hunternif.mc.atlas.api.AtlasAPI;
import hunternif.mc.atlas.network.AbstractMessage;
import hunternif.mc.atlas.util.Log;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.relauncher.Side;

import java.io.IOException;

public class DeletePathPacket extends AbstractMessage<DeletePathPacket> {
    private int atlasID;
    private int pathID;

    public DeletePathPacket() {
    }

    public DeletePathPacket(int atlasID, int pathID) {
        this.atlasID = atlasID;
        this.pathID = pathID;
    }

    @Override
    protected void read(PacketBuffer buffer) throws IOException {
        atlasID = buffer.readVarInt();
        pathID = buffer.readVarInt();

    }

    @Override
    protected void write(PacketBuffer buffer) throws IOException {
        buffer.writeVarInt(atlasID);
        buffer.writeVarInt(pathID);

    }

    @Override
    protected void process(EntityPlayer player, Side side) {
        if (side.isServer()) {
            // Make sure it's this player's atlas :^)
            if (SettingsConfig.gameplay.itemNeeded && !player.inventory.hasItemStack(new ItemStack(RegistrarAntiqueAtlas.ATLAS, 1, atlasID))) {
                Log.warn("Player %s attempted to delete path from someone else's Atlas #%d", player.getGameProfile().getName(), atlasID);
                return;
            }
            AtlasAPI.paths.deletePath(player.world,atlasID,pathID);

        } else {
            AntiqueAtlasMod.pathsData.getOrCreate(atlasID, player.world).get(player.world).removePath(pathID);
        }

    }
}
