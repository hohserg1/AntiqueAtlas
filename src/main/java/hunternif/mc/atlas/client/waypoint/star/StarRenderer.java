package hunternif.mc.atlas.client.waypoint.star;

import hunternif.mc.atlas.AntiqueAtlasMod;
import hunternif.mc.atlas.RegistrarAntiqueAtlas;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import org.lwjgl.opengl.GL11;

import static java.lang.Math.*;

@Mod.EventBusSubscriber(modid = AntiqueAtlasMod.ID, value = Side.CLIENT)
public class StarRenderer {

    public static BlockPos selected = null;
    public static RGB color = RGB.white;

    @SubscribeEvent
    public static void render(RenderWorldLastEvent event) {
        if (selected != null) {
            float partialTicks = event.getPartialTicks();
            Minecraft mc = Minecraft.getMinecraft();
            EntityPlayerSP player = mc.player;
            if (player.getHeldItemMainhand().getItem() == RegistrarAntiqueAtlas.ASTROLABE || player.getHeldItemOffhand().getItem() == RegistrarAntiqueAtlas.ASTROLABE) {
                float rainShade = 1.0F - mc.world.getRainStrength(partialTicks);
                float starBrightness = mc.world.getStarBrightness(partialTicks) * rainShade * 2;

                if (starBrightness > 0.0F) {
                    double px = player.lastTickPosX + (player.posX - player.lastTickPosX) * partialTicks;
                    double py = player.lastTickPosY + (player.posY - player.lastTickPosY) * partialTicks;
                    double pz = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * partialTicks;
                    double dx = px - selected.getX() - 0.5;
                    double dz = pz - selected.getZ() - 0.5;
                    double maxValuableDistance = 20000;
                    double d = min(sqrt(dx * dx + dz * dz), maxValuableDistance);
                    double shortPart = 1400;
                    double longPart = maxValuableDistance - shortPart;
                    double angleV = Math.toRadians(
                            d >= shortPart ?
                                    20d + (longPart - (d - shortPart)) / longPart * (55d - 20d) :
                                    55d + (shortPart - d) / shortPart * (90d - 55d)
                    );
                    float angleH = (float) (atan2(dx, dz) + Math.PI / 2);

                    Vec3d horizontalDirection = new Vec3d(1, 0, 0).rotateYaw(angleH).normalize();

                    Vec3d ort = new Vec3d(-1, 0, horizontalDirection.x / horizontalDirection.z).normalize();
                    double cosV = cos(angleV);
                    double revertCosV = 1 - cosV;
                    double sinV = sin(angleV);
                    Matrix3d m = new Matrix3d(
                            cosV + revertCosV * ort.x * ort.x, revertCosV * ort.x * ort.y - sinV * ort.z, revertCosV * ort.x * ort.z + sinV * ort.y,
                            revertCosV * ort.y * ort.x + sinV * ort.z, cosV + revertCosV * ort.y * ort.y, revertCosV * ort.y * ort.z - sinV * ort.x,
                            revertCosV * ort.z * ort.x - sinV * ort.y, revertCosV * ort.z * ort.y + sinV * ort.x, cosV + revertCosV * ort.z * ort.z
                    );
                    Vec3d direction = m.mul(horizontalDirection).normalize();
                    if (direction.y < 0)
                        direction = new Vec3d(direction.x, -direction.y, direction.z);

                    double distanceToStar = Math.min(200, mc.gameSettings.renderDistanceChunks * 16);
                    Vec3d starPos = direction.scale(distanceToStar);


                    GlStateManager.enableAlpha();
                    GlStateManager.alphaFunc(516, 0);
                    GlStateManager.enableBlend();
                    GlStateManager.disableLighting();

                    GlStateManager.pushMatrix();

                    mc.getTextureManager().bindTexture(new ResourceLocation(AntiqueAtlasMod.ID, "textures/star.png"));

                    BufferBuilder buffer = Tessellator.getInstance().getBuffer();
                    buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);

                    double starScale = distanceToStar / 28;
                    Vec3d look = player.getLook(partialTicks);
                    Vec3d a1;
                    if (look.z > 0.5) {
                        double z = (look.x + look.y) / look.z;
                        a1 = new Vec3d(-1, -1, z).normalize().scale(starScale);
                    } else if (look.x > 0.5) {
                        double x = (look.z + look.y) / look.x;
                        a1 = new Vec3d(x, -1, -1).normalize().scale(starScale);
                    } else {
                        double y = (look.z + look.x) / look.y;
                        a1 = new Vec3d(-1, y, -1).normalize().scale(starScale);
                    }
                    Vec3d a2 = a1.crossProduct(look).normalize().scale(starScale);

                    float r = color.r * starBrightness;
                    float g = color.g * starBrightness;
                    float b = color.b * starBrightness;
                    pos(buffer, starPos.add(a1)).tex(0, 0).color(r, g, b, starBrightness).endVertex();
                    pos(buffer, starPos.add(a2)).tex(1, 0).color(r, g, b, starBrightness).endVertex();
                    pos(buffer, starPos.subtract(a1)).tex(1, 1).color(r, g, b, starBrightness).endVertex();
                    pos(buffer, starPos.subtract(a2)).tex(0, 1).color(r, g, b, starBrightness).endVertex();

                    Tessellator.getInstance().draw();

                    GlStateManager.popMatrix();
                }

            }

        }

    }

    private static BufferBuilder pos(BufferBuilder buffer, Vec3d v) {
        return buffer.pos(v.x, v.y, v.z);
    }
}
