package hunternif.mc.atlas.client;

import hunternif.mc.atlas.AntiqueAtlasMod;
import hunternif.mc.atlas.api.AtlasAPI;
import net.minecraft.client.Minecraft;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraft.util.text.event.HoverEvent;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import org.apache.commons.lang3.StringUtils;

import static kenkron.antiqueatlasoverlay.AAORenderEventReceiver.getPlayerAtlas;

@Mod.EventBusSubscriber(modid = AntiqueAtlasMod.ID, value = Side.CLIENT)
public class PublishingMarkerHandler {

    public static final String prefix = "%marker%/";
    public static final String separator = "&&";

    @SubscribeEvent
    public static void onChatMsg(ClientChatReceivedEvent event) {
        String formatted = event.getMessage().getFormattedText();
        String unformatted = event.getMessage().getUnformattedText();

        int startUnformatted = unformatted.indexOf(prefix);

        if (startUnformatted >= 0) {
            String[] markerData = unformatted.substring(startUnformatted + prefix.length()).split(separator);
            if (markerData.length == 4) {
                String label = markerData[0];
                int x = Integer.parseInt(markerData[1]);
                int z = Integer.parseInt(markerData[2]);
                String icon = markerData[3];

                TextComponentString formattedMsg = new TextComponentString(formatted.substring(0, formatted.indexOf(prefix)) + TextFormatting.DARK_GREEN + "[" + StringUtils.abbreviate(label, 20) + "]");
                formattedMsg.setStyle(new Style()
                        .setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TextComponentString(label + "\n" + x + ", " + z+"\nкликни чтобы добавить в свой атлас")))
                        .setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "") {
                            @Override
                            public String getValue() {

                                Integer atlasID = getPlayerAtlas(Minecraft.getMinecraft().player);
                                if (atlasID != null)
                                    AtlasAPI.markers.putMarker(Minecraft.getMinecraft().world, true, atlasID, icon, label, x, z);

                                return super.getValue();
                            }
                        })
                );
                event.setMessage(formattedMsg);
            }
        }
    }
}
