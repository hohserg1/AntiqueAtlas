package hunternif.mc.atlas.map.objects.path;

import hunternif.mc.atlas.AntiqueAtlasMod;
import hunternif.mc.atlas.network.PacketDispatcher;
import hunternif.mc.atlas.network.client.PathsPacket;
import hunternif.mc.atlas.util.Log;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraft.world.storage.WorldSavedData;
import net.minecraftforge.common.DimensionManager;

import java.util.*;
import java.util.stream.Collectors;

public class PathsData extends WorldSavedData {
    public final int atlasId;
    private Map<Integer, DimensionPathsData> dimensions = new HashMap<>();

    public PathsData(String key) {
        super(key);
        atlasId = AntiqueAtlasMod.pathsData.getAtlasId(key);
        List<Integer> allDims = DimensionManager.getRegisteredDimensions().values().stream().flatMap(Collection::stream).collect(Collectors.toList());
        for (Integer dim : allDims) {
            dimensions.put(dim, new DimensionPathsData(this));
        }
    }

    public DimensionPathsData get(World world) {
        return dimensions.get(world.provider.getDimension());
    }

    public DimensionPathsData get(int dimension) {
        return dimensions.get(dimension);
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        for (Map.Entry<Integer, DimensionPathsData> e : dimensions.entrySet()) {
            e.getValue().readFromNBT(nbt.getCompoundTag("dim_" + e.getKey()));
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        for (Map.Entry<Integer, DimensionPathsData> e : dimensions.entrySet()) {
            nbt.setTag("dim_" + e.getKey(), e.getValue().toNbt());
        }
        return nbt;
    }

    private final Set<EntityPlayer> playersSentTo = Collections.newSetFromMap(new WeakHashMap<>());

    public void syncOnPlayer(EntityPlayer player) {
        for (Map.Entry<Integer, DimensionPathsData> e : dimensions.entrySet()) {
            if (!e.getValue().isEmpty()) {
                int dimension = e.getKey();
                PacketDispatcher.sendTo(new PathsPacket(atlasId, dimension, e.getValue().getAllPaths()), (EntityPlayerMP) player);
            }
        }
        Log.info("Sent paths data #%d to player %s", atlasId, player.getName());
        playersSentTo.add(player);
    }

    public boolean isSyncedOnPlayer(EntityPlayer player) {
        return playersSentTo.contains(player);
    }
}
