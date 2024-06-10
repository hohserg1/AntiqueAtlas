package hunternif.mc.atlas.api;

import hunternif.mc.atlas.map.objects.path.Path;
import net.minecraft.world.World;

import javax.annotation.Nonnull;

public interface PathAPI {
    void deletePath(@Nonnull World world, int atlasID, int pathID);

    Path addPath(@Nonnull World world, int atlasID, String label, int color, int startX, int startZ, short[] segments);
}
