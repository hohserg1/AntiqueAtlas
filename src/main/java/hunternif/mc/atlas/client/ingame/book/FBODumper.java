package hunternif.mc.atlas.client.ingame.book;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.IntBuffer;

public class FBODumper {

    public static void dump() {
        if (!dumped) {
            dumped = true;

            int w = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_WIDTH);
            int h = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_HEIGHT);

            IntBuffer buf = BufferUtils.createIntBuffer(w * h);

            buf.clear();
            GL11.glGetTexImage(GL11.GL_TEXTURE_2D, 0, GL12.GL_BGRA, GL11.GL_UNSIGNED_BYTE, buf);

            int[] array = new int[w * h];
            buf.get(array);

            File tmp = new File("./test_fbo.png");
            if (tmp.exists())
                tmp.delete();
            BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            img.setRGB(0, 0, w, h, array, 0, w);


            try {
                ImageIO.write(img, "png", tmp);
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

    public static boolean dumped = false;
}
