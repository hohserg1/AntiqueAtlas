package hunternif.mc.atlas.util;

import com.google.gson.Gson;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import org.apache.commons.io.IOUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class RenderUtil {
    private static Map<ResourceLocation, DrawFunction> cache = new HashMap<>();

    private static DrawFunction computeDrawFunction(ResourceLocation resourceLocation1) {
        String imagePath = "/assets/" + resourceLocation1.getNamespace() + "/" + resourceLocation1.getPath();
        String specPath = imagePath + ".9grid";
        Spec9Grid spec1;
        try {
            String content = IOUtils.toString(RenderUtil.class.getResourceAsStream(specPath), StandardCharsets.UTF_8);
            Gson gson = new Gson();
            spec1 = gson.fromJson(content, Spec9Grid.class);
        } catch (IOException e) {
            e.printStackTrace();
            spec1 = new Spec9Grid(0, 1, 0, 1);
        }
        final Spec9Grid spec = spec1;

        try {
            BufferedImage image = ImageIO.read(RenderUtil.class.getResourceAsStream(imagePath));
            int imageWidth = image.getWidth();
            int imageHeight = image.getHeight();

            return (x1, y1, w1, h1) -> {

                int secondColumn = (int) (spec.secondColumn * imageWidth) + x1;
                int thirdColumn = w1 - (int) (imageWidth - spec.thirdColumn * imageWidth) + x1;
                int secondLine = (int) (spec.secondLine * imageHeight) + y1;
                int thirdLine = h1 - (int) (imageHeight - spec.thirdLine * imageHeight) + y1;

                //drawTexture(x,y,x+w,y+h,0,0,1,1);

                drawTexture(x1, y1, secondColumn, secondLine, 0, 0, spec.secondColumn, spec.secondLine);
                drawTexture(secondColumn, y1, thirdColumn, secondLine, spec.secondColumn, 0, spec.thirdColumn, spec.secondLine);
                drawTexture(thirdColumn, y1, x1 + w1, secondLine, spec.thirdColumn, 0, 1, spec.secondLine);

                drawTexture(x1, secondLine, secondColumn, thirdLine, 0, spec.secondLine, spec.secondColumn, spec.thirdLine);
                drawTexture(secondColumn, secondLine, thirdColumn, thirdLine, spec.secondColumn, spec.secondLine, spec.thirdColumn, spec.thirdLine);
                drawTexture(thirdColumn, secondLine, x1 + w1, thirdLine, spec.thirdColumn, spec.secondLine, 1, spec.thirdLine);

                drawTexture(x1, thirdLine, secondColumn, y1 + h1, 0, spec.thirdLine, spec.secondColumn, 1);
                drawTexture(secondColumn, thirdLine, thirdColumn, y1 + h1, spec.secondColumn, spec.thirdLine, spec.thirdColumn, 1);
                drawTexture(thirdColumn, thirdLine, x1 + w1, y1 + h1, spec.thirdColumn, spec.thirdLine, 1, 1);
            };
        } catch (IOException e) {
            return (x1, y1, w1, h1) -> e.printStackTrace();
        }
    }

    interface DrawFunction {
        void apply(int x, int y, int w, int h);
    }

    public static void draw9GridScaleTexture(int x, int y, int w, int h, ResourceLocation resourceLocation) {
        Minecraft.getMinecraft().getTextureManager().bindTexture(resourceLocation);
        cache.computeIfAbsent(resourceLocation, RenderUtil::computeDrawFunction).apply(x, y, w, h);
    }

    public static void drawTexture(int x1, int y1, int x2, int y2, float u1, float v1, float u2, float v2) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferbuilder = tessellator.getBuffer();
        bufferbuilder.begin(7, DefaultVertexFormats.POSITION_TEX);
        bufferbuilder.pos(x1, y1, 0).tex(u1, v1).endVertex();
        bufferbuilder.pos(x1, y2, 0).tex(u1, v2).endVertex();
        bufferbuilder.pos(x2, y2, 0).tex(u2, v2).endVertex();
        bufferbuilder.pos(x2, y1, 0).tex(u2, v1).endVertex();
        tessellator.draw();
    }

    public static class Spec9Grid {
        public final float secondColumn, thirdColumn, secondLine, thirdLine;

        public Spec9Grid(float secondColumn, float thirdColumn, float secondLine, float thirdLine) {
            this.secondColumn = secondColumn;
            this.thirdColumn = thirdColumn;
            this.secondLine = secondLine;
            this.thirdLine = thirdLine;
        }
    }
}
