package hunternif.mc.atlas.registry;

import net.dries007.tfc.api.types.Ore;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.GameRegistry;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class TFCMarkerTypes {
    public static void init() {
        for (Ore o : GameRegistry.findRegistry(Ore.class).getValuesCollection()) {
            registerGradedMarkerIcon(o, gradePrefix(Ore.Grade.NORMAL));

            if (o.isGraded()) {
                registerGradedMarkerIcon(o, gradePrefix(Ore.Grade.POOR));
                registerGradedMarkerIcon(o, gradePrefix(Ore.Grade.RICH));
            }
        }
    }

    private static void registerGradedMarkerIcon(Ore o, String grade) {
        ResourceLocation textureLoc = new ResourceLocation("tfc", "textures/items/ore/" + grade + o.getRegistryName().getPath() + ".png");

        MarkerType type = new MarkerType(new ResourceLocation(getRegistryName(o, grade)), textureLoc) {
            @Override
            public boolean isVisibleInList() {
                return false;
            }
        };
        type.setSize(1);
        System.out.println("Registering tfc marker " + type.getRegistryName());
        MarkerRegistry.register(type);
    }

    public static String getRegistryName(Ore o, String grade) {
        return "aa_item:" + grade + o.getRegistryName().getPath();
    }

    public static String gradePrefix(Ore.Grade value) {
        switch (value) {
            case NORMAL:
                return "";
            case POOR:
                return "poor/";
            case RICH:
                return "rich/";
            default:
                throw new IllegalArgumentException("unsupported ore grade");
        }
    }
}
