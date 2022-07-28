package hunternif.mc.atlas.client;

import hunternif.mc.atlas.AntiqueAtlasMod;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

@Mod.EventBusSubscriber(modid = AntiqueAtlasMod.ID,value = Side.CLIENT)
public class PublishingMarkerHandler {
    @SubscribeEvent
    public static void onChatMsg(ClientChatReceivedEvent event){
        System.out.println(event.getMessage().getUnformattedText());
    }
}
