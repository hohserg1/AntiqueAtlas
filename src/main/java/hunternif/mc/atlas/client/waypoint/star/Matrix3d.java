package hunternif.mc.atlas.client.waypoint.star;

import net.minecraft.util.math.Vec3d;

public class Matrix3d {

    public double m00;
    public double m01;
    public double m02;
    public double m10;
    public double m11;
    public double m12;
    public double m20;
    public double m21;
    public double m22;

    public Matrix3d(double m00, double m01, double m02,
                    double m10, double m11, double m12,
                    double m20, double m21, double m22) {
        this.m00 = m00;
        this.m01 = m01;
        this.m02 = m02;

        this.m10 = m10;
        this.m11 = m11;
        this.m12 = m12;

        this.m20 = m20;
        this.m21 = m21;
        this.m22 = m22;

    }

    public Vec3d mul(Vec3d t) {
        double x = m00 * t.x + m01 * t.y + m02 * t.z;
        double y = m10 * t.x + m11 * t.y + m12 * t.z;
        double z = m20 * t.x + m21 * t.y + m22 * t.z;
        return new Vec3d(x, y, z);
    }
}
