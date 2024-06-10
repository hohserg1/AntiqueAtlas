package hunternif.mc.atlas.network.client;

import com.google.common.collect.Lists;
import hunternif.mc.atlas.AntiqueAtlasMod;
import hunternif.mc.atlas.map.objects.path.DimensionPathsData;
import hunternif.mc.atlas.map.objects.path.Path;
import hunternif.mc.atlas.network.AbstractMessage;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.relauncher.Side;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

public class PathsPacket extends AbstractMessage.AbstractClientMessage<PathsPacket> {
    private int atlasID;
    private int dimension;
    private Collection<Path> paths;

    public PathsPacket() {
    }

    public PathsPacket(int atlasID, int dimension, Collection<Path> paths) {
        this.atlasID = atlasID;
        this.dimension = dimension;
        this.paths = paths;
    }

    public PathsPacket(int atlasID, int dimension, Path... paths) {
        this(atlasID, dimension, Lists.newArrayList(paths));
    }

    @Override
    protected void read(PacketBuffer buffer) throws IOException {
        atlasID = buffer.readVarInt();
        dimension = buffer.readVarInt();
        int pathCount = buffer.readVarInt();
        paths = new ArrayList<>(pathCount);
        for (int i = 0; i < pathCount; i++) {
            int id = buffer.readVarInt();
            String label = ByteBufUtils.readUTF8String(buffer);
            int color = buffer.readInt();
            int x = buffer.readInt();
            int z = buffer.readInt();

            int segmentCount = buffer.readVarInt();
            short[] segments = new short[segmentCount];
            for (int j = 0; j < segmentCount; j++) {
                segments[j] = buffer.readShort();
            }
            paths.add(new Path(id, label, color, x, z, segments));
        }
    }

    @Override
    protected void write(PacketBuffer buffer) throws IOException {
        buffer.writeVarInt(atlasID);
        buffer.writeVarInt(dimension);
        buffer.writeVarInt(paths.size());
        for (Path path : paths) {
            buffer.writeVarInt(path.id);
            ByteBufUtils.writeUTF8String(buffer, path.label);
            buffer.writeInt(path.color);
            buffer.writeInt(path.startX);
            buffer.writeInt(path.startZ);


            buffer.writeVarInt(path.segments.length);
            for (short segment : path.segments) {
                buffer.writeShort(segment);
            }
        }
    }

    @Override
    protected void process(EntityPlayer player, Side side) {
        DimensionPathsData dimensionPathsData = AntiqueAtlasMod.pathsData.getOrCreate(atlasID, player.getEntityWorld()).get(dimension);
        paths.forEach(dimensionPathsData::loadPath);
    }
}
