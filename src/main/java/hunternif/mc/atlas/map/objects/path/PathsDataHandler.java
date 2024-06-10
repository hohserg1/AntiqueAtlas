package hunternif.mc.atlas.map.objects.path;

import hunternif.mc.atlas.RegistrarAntiqueAtlas;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PathsDataHandler {
    private static final String PATHS_DATA_PREFIX = "aaPaths_";

    private final Map<Integer, PathsData> pathsDataClientCache = new ConcurrentHashMap<>();

    public PathsData getOrCreate(ItemStack stack, World world) {
        if (stack.getItem() == RegistrarAntiqueAtlas.ATLAS) {
            return getOrCreate(stack.getItemDamage(), world);
        } else {
            return null;
        }
    }

    public PathsData getOrCreate(int atlasID, World world) {
        String key = getPathsDataKey(atlasID);
        PathsData data = null;
        if (world.isRemote) {
            data = pathsDataClientCache.get(atlasID);
        }
        if (data == null) {
            data = (PathsData) world.loadData(PathsData.class, key);
            if (data == null) {
                data = new PathsData(key);
                world.setData(key, data);
            }
            if (world.isRemote) {
                pathsDataClientCache.put(atlasID, data);
            }
        }
        return data;
    }

    public Collection<PathsData> getAllLoadedPathsData() {
        return pathsDataClientCache.values();
    }

    public String getPathsDataKey(int atlasID) {
        return PATHS_DATA_PREFIX + atlasID;
    }

    public int getAtlasId(String wsdKey) {
        return Integer.parseInt(wsdKey.substring(PATHS_DATA_PREFIX.length()));
    }

    @SubscribeEvent
    public void onClientConnectedToServer(FMLNetworkEvent.ClientConnectedToServerEvent event) {
        pathsDataClientCache.clear();
    }
}
