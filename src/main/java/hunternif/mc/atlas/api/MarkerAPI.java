package hunternif.mc.atlas.api;

import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * API for putting custom markers to the atlases. Set the textures on the
 * client side, put markers into atlases on the server side. Marker texture
 * has to be square; the center of the texture will point to the marked spot.
 * @author Hunternif
 */
public interface MarkerAPI {
	/** Version of Marker API, meaning this particular class. */
	int getVersion();
	
	/** Assign texture to a marker type. */
	@SideOnly(Side.CLIENT)
	void setTexture(String markerType, ResourceLocation texture);
	
	/** Assign texture to a marker type, if no texture has been assigned to it.
	 * Returns true if the texture was changed for this marker type. */
	@SideOnly(Side.CLIENT)
	boolean setTextureIfNone(String markerType, ResourceLocation texture);
	
	/** Save marker texture config file. You might want to avoid saving if no
	 * texture has actually been changed, so that the config file is not
	 * overwritten too often (that makes it easier to modify manually). */
	@SideOnly(Side.CLIENT)
	void save();
	
	/**
	 * Put a marker in the specified Atlas instance at specified block
	 * coordinates. Call this method per one marker either on the server or
	 * on the client.
	 * @param world
	 * @param visibleAhead	whether the marker should appear visible even if
	 * 						the player hasn't yet discovered that area.
	 * @param atlasID		the ID of the atlas you want to put marker in. Equal
	 * 						to ItemStack damage for ItemAtlas.
	 * @param markerType	name of your custom marker type.
	 * @param label			text label to be displayed on mouseover.
	 * @param x				block coordinate
	 * @param z				block coordinate
	 */
	void putMarker(World world, boolean visibleAhead, int atlasID,
			String markerType, String label, int x, int z);
	
	/**
	 * Put a marker in all atlases in the world at specified block coordinates.
	 * Call this method per one marker, on the server only!
	 * @param world
	 * @param visibleAhead	whether the marker should appear visible even if
	 * 						the player hasn't yet discovered that area.
	 * @param markerType	name of your custom marker type.
	 * @param label			text label to be displayed on mouseover.
	 * @param x				block coordinate
	 * @param z				block coordinate
	 */
	@SideOnly(Side.SERVER)
	void putGlobalMarker(World world, boolean visibleAhead,
			String markerType, String label, int x, int z);
	
	/**
	 * Delete a marker from an atlas.
	 * @param world
	 * @param atlasID
	 * @param markerID
	 */
	void deleteMarker(World world, int atlasID, int markerID);
	
	/**
	 * Delete a global marker from all atlases. Only the server can permit this.
	 * @param world
	 * @param markerID
	 */
	@SideOnly(Side.SERVER)
	void deleteGlobalMarker(World world, int markerID);
}
