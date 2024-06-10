package hunternif.mc.atlas.event;

import hunternif.mc.atlas.map.objects.path.Path;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.common.eventhandler.Event;

public class PathHoveredEvent extends Event {
    public EntityPlayer player;
    public Path path;

    public PathHoveredEvent(EntityPlayer player, Path path) {
        this.player = player;
        this.path = path;
    }
}
