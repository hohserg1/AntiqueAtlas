package hunternif.mc.atlas.api.impl;

import hunternif.mc.atlas.AntiqueAtlasMod;
import hunternif.mc.atlas.api.PathAPI;
import hunternif.mc.atlas.map.objects.path.Path;
import hunternif.mc.atlas.network.PacketDispatcher;
import hunternif.mc.atlas.network.bidirectional.DeletePathPacket;
import hunternif.mc.atlas.network.client.PathsPacket;
import net.minecraft.world.World;

import javax.annotation.Nonnull;

public class PathApiImpl implements PathAPI {
    @Override
    public void deletePath(@Nonnull World world, int atlasID, int pathID) {
        DeletePathPacket packet = new DeletePathPacket(atlasID, pathID);
        if (world.isRemote) {
            PacketDispatcher.sendToServer(packet);

        } else {
            AntiqueAtlasMod.pathsData.getOrCreate(atlasID, world).get(world).removePath(pathID);
            PacketDispatcher.sendToAll(packet);
        }
    }

    @Override
    public Path addPath(@Nonnull World world, int atlasID, String label, int color, int startX, int startZ, short[] segments) {
        if (world.isRemote) {
            throw new UnsupportedOperationException("wtf, PathAPI#addPath at client side");

        } else {
            Path path = AntiqueAtlasMod.pathsData.getOrCreate(atlasID, world).get(world).createAndSavePath(label, color, startX, startZ, segments);
            PacketDispatcher.sendToAll(new PathsPacket(atlasID, world.provider.getDimension(), path));
            return path;
        }
    }
}
