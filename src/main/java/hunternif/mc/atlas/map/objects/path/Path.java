package hunternif.mc.atlas.map.objects.path;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Objects;

public class Path {
    public final int id;
    public final String label;
    public final int color;
    public final int startX, startZ;
    public final short[] segments;

    private transient BlockPos end;

    public Path(int id, String label, int color, int startX, int startZ, short[] segments) {
        this.id = id;
        this.label = label;
        this.color = color;
        this.startX = startX;
        this.startZ = startZ;
        this.segments = segments;
    }

    public Path(NBTTagCompound pathNbt) {
        this(
                pathNbt.getInteger("id"),
                pathNbt.getString("label"),
                pathNbt.getInteger("color"),
                pathNbt.getInteger("x"),
                pathNbt.getInteger("z"),
                loadSegments(pathNbt.getByteArray("segments"))
        );
    }

    public BlockPos getEnd() {
        if (end == null) {
            int x = startX;
            int z = startZ;
            for (short index : segments) {
                Vec3i v = Segment.getVector(index);
                x += v.getX();
                z += v.getZ();
            }
            end = new BlockPos(x, 0, z);
        }
        return end;
    }

    public static short[] loadSegments(byte[] segmentsBytes, short[] segments) {
        ByteBuffer.wrap(segmentsBytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(segments, 0, segmentsBytes.length / 2);
        return segments;
    }

    public static short[] loadSegments(byte[] segmentsBytes) {
        return loadSegments(segmentsBytes, new short[segmentsBytes.length / 2]);
    }

    @Override
    public String toString() {
        return "#" + id + "\"" + label + "\"" + " color=" + color + " from(" + startX + ", " + startZ + ")";
    }

    public NBTTagCompound toNbt() {
        NBTTagCompound r = new NBTTagCompound();
        r.setInteger("id", id);
        r.setString("label", label);
        r.setInteger("color", color);
        r.setInteger("x", startX);
        r.setInteger("z", startZ);
        r.setByteArray("segments", saveSegments(segments));
        return r;
    }

    public static byte[] saveSegments(short[] segments) {
        byte[] segmentsBytes = new byte[segments.length * 2];
        ByteBuffer.wrap(segmentsBytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().put(segments);
        return segmentsBytes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Path path = (Path) o;
        return id == path.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
