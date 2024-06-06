package hunternif.mc.atlas;

import gloomyfolken.hooklib.api.*;
import io.netty.channel.*;
import net.minecraftforge.fml.common.network.*;
import net.minecraftforge.fml.relauncher.*;
import net.minecraftforge.fml.common.*;

@HookContainer
public class FixFuckingBukkitSide {

    @Hook(targetMethod = "newChannel")
    @OnMethodCall(value = "values",shift = Shift.INSTEAD)
    public static Side[] filterSides(NetworkRegistry networkRegistry, String name, ChannelHandler... handlers) {
        return new Side[]{Side.CLIENT, Side.SERVER};
    }

    @Hook(targetMethod = "newChannel")
    @OnMethodCall(value = "values",shift = Shift.INSTEAD)
    public static Side[] filterSides(NetworkRegistry networkRegistry, ModContainer container, String name, ChannelHandler... handlers) {
        return new Side[]{Side.CLIENT, Side.SERVER};
    }
}
