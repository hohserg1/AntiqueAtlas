package hunternif.mc.atlas.client.gui;

import com.google.common.collect.ImmutableList;
import hunternif.mc.atlas.AntiqueAtlasMod;
import hunternif.mc.atlas.SettingsConfig;
import hunternif.mc.atlas.api.AtlasAPI;
import hunternif.mc.atlas.client.*;
import hunternif.mc.atlas.client.gui.core.*;
import hunternif.mc.atlas.client.gui.core.GuiStates.IState;
import hunternif.mc.atlas.client.gui.core.GuiStates.SimpleState;
import hunternif.mc.atlas.core.DimensionData;
import hunternif.mc.atlas.event.MarkerClickedEvent;
import hunternif.mc.atlas.event.MarkerHoveredEvent;
import hunternif.mc.atlas.marker.DimensionMarkersData;
import hunternif.mc.atlas.marker.Marker;
import hunternif.mc.atlas.marker.MarkersData;
import hunternif.mc.atlas.network.PacketDispatcher;
import hunternif.mc.atlas.network.server.BrowsingPositionPacket;
import hunternif.mc.atlas.registry.MarkerRegistry;
import hunternif.mc.atlas.registry.MarkerRenderInfo;
import hunternif.mc.atlas.registry.MarkerType;
import hunternif.mc.atlas.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraftforge.common.MinecraftForge;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static net.minecraft.util.math.MathHelper.clamp;

public class GuiAtlas extends GuiComponent {
    public static final int WIDTH = 310;
    public static final int HEIGHT = 218;
    protected static final int CONTENT_X = 17;
    protected static final int CONTENT_Y = 11;

    protected static final int MAP_WIDTH = WIDTH - 17 * 2;
    protected static final int MAP_HEIGHT = 194;
    protected static final float PLAYER_ROTATION_STEPS = 16;
    protected static final int PLAYER_ICON_WIDTH = 7;
    protected static final int PLAYER_ICON_HEIGHT = 8;

    public static final int MARKER_SIZE = 32;

    protected static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd_HH.mm.ss");

    /**
     * If the map scale goes below this value, the tiles will not scale down
     * visually, but will instead span greater area.
     */
    protected static final double MIN_SCALE_THRESHOLD = 0.5;

    protected final long[] renderTimes = new long[30];

    protected int renderTimesIndex = 0;

    // States ==================================================================

    protected final GuiStates state = new GuiStates();

    /**
     * If on, navigate the map normally.
     */
    protected final IState NORMAL = new SimpleState();

    /**
     * If on, all markers as well as the player icon are hidden.
     */
    protected final IState HIDING_MARKERS = new IState() {
        @Override
        public void onEnterState() {
            // Set the button as not selected so that it can be clicked again:
            btnShowMarkers.setSelected(false);
            btnShowMarkers.setTitle(I18n.format("gui.antiqueatlas.showMarkers"));
            btnShowMarkers.setIconTexture(Textures.ICON_SHOW_MARKERS);
        }

        @Override
        public void onExitState() {
            btnShowMarkers.setSelected(false);
            btnShowMarkers.setTitle(I18n.format("gui.antiqueatlas.hideMarkers"));
            btnShowMarkers.setIconTexture(Textures.ICON_HIDE_MARKERS);
        }
    };

    protected final IState EDITING_FILTER = new IState() {
        @Override
        public void onEnterState() {
            btnFilterMarkers.show();
        }

        @Override
        public void onExitState() {
            if (btnFilterMarkers.getText().isEmpty())
                btnFilterMarkers.hide();
            else
                btnFilterMarkers.unfocus();
        }
    };

    protected final IState PUBLISHING_MARKER = new IState() {
        @Override
        public void onEnterState() {
            CustomMouseHeler.grabMouseCursor();
            addChild(publisherCursor);
            btnPublishMarker.setSelected(true);
        }

        @Override
        public void onExitState() {
            CustomMouseHeler.ungrabMouseCursor();
            removeChild(publisherCursor);
            btnPublishMarker.setSelected(false);
        }
    };

    protected final GuiCursor publisherCursor = new GuiCursor();

    /**
     * If on, a semi-transparent marker is attached to the cursor, and the
     * player's icon becomes semi-transparent as well.
     */
    protected final IState PLACING_MARKER = new IState() {
        @Override
        public void onEnterState() {
            btnMarker.setSelected(true);
        }

        @Override
        public void onExitState() {
            btnMarker.setSelected(false);
        }
    };

    /**
     * If on, the closest marker will be deleted upon mouseclick.
     */
    protected final IState DELETING_MARKER = new IState() {
        @Override
        public void onEnterState() {
            CustomMouseHeler.grabMouseCursor();
            addChild(eraserCursor);
            btnDelMarker.setSelected(true);
        }

        @Override
        public void onExitState() {
            CustomMouseHeler.ungrabMouseCursor();
            removeChild(eraserCursor);
            btnDelMarker.setSelected(false);
        }
    };
    protected final GuiCursor eraserCursor = new GuiCursor();

    /**
     * If on, the closest marker will be copied upon mouseclick.
     */
    protected final IState COPYING_MARKER = new IState() {

        @Override
        public void onEnterState() {
            CustomMouseHeler.grabMouseCursor();
            addChild(copierCursor);
            btnCopyMarker.setSelected(true);
        }

        @Override
        public void onExitState() {
            CustomMouseHeler.ungrabMouseCursor();
            removeChild(copierCursor);
            btnCopyMarker.setSelected(false);

        }
    };
    protected final GuiCursor copierCursor = new GuiCursor();

    protected final IState EXPORTING_IMAGE = new IState() {
        @Override
        public void onEnterState() {
            btnExportPng.setSelected(true);
        }

        @Override
        public void onExitState() {
            btnExportPng.setSelected(false);
        }
    };

    // Buttons =================================================================

    /**
     * Arrow buttons for navigating the map view via mouse clicks.
     */
    protected final GuiArrowButton btnUp, btnDown, btnLeft, btnRight;

    /**
     * Button for exporting PNG image of the Atlas's contents.
     */
    protected final GuiBookmarkButton btnExportPng;

    /**
     * Button for publishing marker to chat
     */
    protected final GuiBookmarkButton btnPublishMarker;

    /**
     * Button for placing a marker at current position, local to this Atlas instance.
     */
    protected final GuiBookmarkButton btnMarker;

    /**
     * Button for deleting local markers.
     */
    protected final GuiBookmarkButton btnDelMarker;

    /**
     * Button for copying local markers.
     */
    protected final GuiBookmarkButton btnCopyMarker;

    /**
     * Button for showing/hiding all markers.
     */
    protected final GuiBookmarkButton btnShowMarkers;

    /**
     * Button for filtrating markers by name.
     */
    protected final GuiFilterButton btnFilterMarkers;

    /**
     * Button for restoring player's position at the center of the Atlas.
     */
    protected final GuiPositionButton btnPosition;


    // Navigation ==============================================================

    /**
     * Pause between after the arrow button is pressed and continuous
     * navigation starts, in ticks.
     */
    protected static final int BUTTON_PAUSE = 8;

    /**
     * How much the map view is offset, in blocks, per click (or per tick).
     */
    protected static final int navigateStep = 24;

    /**
     * The button which is currently being pressed. Used for continuous
     * navigation using the arrow buttons. Also used to prevent immediate
     * canceling of placing marker.
     */
    protected GuiComponentButton selectedButton = null;

    /**
     * Time in world ticks when the button was pressed. Used to create a pause
     * before continuous navigation using the arrow buttons.
     */
    protected long timeButtonPressed = 0;

    /**
     * Set to true when dragging the map view.
     */
    protected boolean isDragging = false;
    /**
     * The starting cursor position when dragging.
     */
    protected int dragMouseX, dragMouseY;
    /**
     * Map offset at the beginning of drag.
     */
    protected int dragMapOffsetX, dragMapOffsetY;

    /**
     * Offset to the top left corner of the tile at (0, 0) from the center of
     * the map drawing area, in pixels.
     */
    protected int mapOffsetX, mapOffsetY;
    /**
     * If true, the player's icon will be in the center of the GUI, and the
     * offset of the tiles will be calculated accordingly. Otherwise it's the
     * position of the player that will be calculated with respect to the
     * offset.
     */
    protected boolean followPlayer = true;

    protected final GuiScaleBar scaleBar = new GuiScaleBar();

    /**
     * Pixel-to-block ratio.
     */
    protected double mapScale;
    /**
     * The visual size of a tile in pixels.
     */
    protected int tileHalfSize;
    /**
     * The number of chunks a tile spans.
     */
    protected int tile2ChunkScale;


    // Markers =================================================================

    /**
     * Local markers in the current dimension
     */
    protected DimensionMarkersData localMarkersData;
    /**
     * The marker highlighted by the eraser. Even though multiple markers may
     * be highlighted at the same time, only one of them will be deleted.
     */
    protected Marker hoveredMarker;
    protected Marker hoveredOuterMarker;

    protected final GuiMarkerFinalizer markerFinalizer = new GuiMarkerFinalizer();
    /**
     * Displayed where the marker is about to be placed when the Finalizer GUI is on.
     */
    protected final GuiBlinkingMarker blinkingIcon = new GuiBlinkingMarker();

    // Misc stuff ==============================================================

    protected EntityPlayer player;
    protected ItemStack stack;
    protected DimensionData biomeData;

    /**
     * Coordinate scale factor relative to the actual screen size.
     */
    protected int screenScale;

    /**
     * Progress bar for exporting images.
     */
    protected final ProgressBarOverlay progressBar = new ProgressBarOverlay(100, 2);

    protected long lastUpdateMillis = System.currentTimeMillis();
    protected int scaleAlpha = 255;
    protected int scaleClipIndex = 0;
    protected int zoomLevelOne = 8;
    protected int zoomLevel = zoomLevelOne;
    protected String[] zoomNames = new String[]{"256", "128", "64", "32", "16", "8", "4", "2", "1", "1/2", "1/4", "1/8", "1/16", "1/32", "1/64", "1/128", "1/256"};

    protected Thread exportThread;

    @SuppressWarnings("rawtypes")
    public GuiAtlas() {
        setSize(WIDTH, HEIGHT);
        setMapScale(0.5);
        followPlayer = true;
        setInterceptKeyboard(true);

        btnUp = GuiArrowButton.up();
        addChild(btnUp).offsetGuiCoords(148, 10);
        btnDown = GuiArrowButton.down();
        addChild(btnDown).offsetGuiCoords(148, 194);
        btnLeft = GuiArrowButton.left();
        addChild(btnLeft).offsetGuiCoords(15, 100);
        btnRight = GuiArrowButton.right();
        addChild(btnRight).offsetGuiCoords(283, 100);
        btnPosition = new GuiPositionButton();
        btnPosition.setEnabled(!followPlayer);
        addChild(btnPosition).offsetGuiCoords(283, 194);
        IButtonListener positionListener = button -> {
            selectedButton = button;
            if (button.equals(btnPosition)) {
                followPlayer = true;
                btnPosition.setEnabled(false);
            } else {
                // Navigate once, before enabling pause:
                navigateByButton(selectedButton);
                timeButtonPressed = player.getEntityWorld().getTotalWorldTime();
            }
        };
        btnUp.addListener(positionListener);
        btnDown.addListener(positionListener);
        btnLeft.addListener(positionListener);
        btnRight.addListener(positionListener);
        btnPosition.addListener(positionListener);

        btnExportPng = new GuiBookmarkButton(1, Textures.ICON_EXPORT, I18n.format("gui.antiqueatlas.exportImage")) {
            @Override
            public boolean isEnabled() {
                return !ExportImageUtil.isExporting;
            }
        };
        addChild(btnExportPng).offsetGuiCoords(300, 113);
        btnExportPng.addListener(button -> {
            if (stack != null || !SettingsConfig.gameplay.itemNeeded) {
                exportThread = new Thread(() -> exportImage(getAtlasID()), "Atlas file export thread");
                exportThread.start();
            }
        });

        btnPublishMarker = new GuiBookmarkButton(1, Textures.ICON_PUBLISH, I18n.format("gui.antiqueatlas.publishMarker"));
        addChild(btnPublishMarker).offsetGuiCoords(300, 136);
        btnPublishMarker.addListener(button -> {
            if (stack != null || !SettingsConfig.gameplay.itemNeeded) {
                if (state.is(PUBLISHING_MARKER)) {
                    selectedButton = null;
                    state.switchTo(NORMAL);
                } else {
                    selectedButton = button;
                    state.switchTo(PUBLISHING_MARKER);
                }
            }
        });

        btnMarker = new GuiBookmarkButton(0, Textures.ICON_ADD_MARKER, I18n.format("gui.antiqueatlas.addMarker"));
        addChild(btnMarker).offsetGuiCoords(300, 14);
        btnMarker.addListener(button -> {
            if (stack != null || !SettingsConfig.gameplay.itemNeeded) {
                if (state.is(PLACING_MARKER)) {
                    selectedButton = null;
                    state.switchTo(NORMAL);
                } else {
                    selectedButton = button;
                    state.switchTo(PLACING_MARKER);

                    // While holding shift, we create a marker on the player's position
                    if (GuiScreen.isShiftKeyDown()) {
                        markerFinalizer.setMarkerData(player.getEntityWorld(),
                                getAtlasID(), player.dimension,
                                (int) player.posX, (int) player.posZ);
                        addChild(markerFinalizer);

                        blinkingIcon.setTexture(markerFinalizer.selectedType.getIcon(),
                                MARKER_SIZE, MARKER_SIZE);
                        addChildBehind(markerFinalizer, blinkingIcon)
                                .setRelativeCoords(worldXToScreenX((int) player.posX) - getGuiX() - MARKER_SIZE / 2,
                                        worldZToScreenY((int) player.posZ) - getGuiY() - MARKER_SIZE / 2);

                        // Need to intercept keyboard events to type in the label:
                        setInterceptKeyboard(true);

                        // Un-press all keys to prevent player from walking infinitely:
                        KeyBinding.unPressAllKeys();

                        selectedButton = null;
                        state.switchTo(NORMAL);
                    }
                }
            }
        });
        btnDelMarker = new GuiBookmarkButton(2, Textures.ICON_DELETE_MARKER, I18n.format("gui.antiqueatlas.delMarker"));
        addChild(btnDelMarker).offsetGuiCoords(300, 33);
        btnDelMarker.addListener(button -> {
            if (stack != null || !SettingsConfig.gameplay.itemNeeded) {
                if (state.is(DELETING_MARKER)) {
                    selectedButton = null;
                    state.switchTo(NORMAL);
                } else {
                    selectedButton = button;
                    state.switchTo(DELETING_MARKER);
                }
            }
        });
        btnCopyMarker = new GuiBookmarkButton(0, Textures.ICON_COPY_MARKER, I18n.format("gui.antiqueatlas.copyMarker"));
        addChild(btnCopyMarker).offsetGuiCoords(300, 52);
        btnCopyMarker.addListener(button -> {
            if (stack != null || !SettingsConfig.gameplay.itemNeeded) {
                if (copiedMarker == null) {
                    if (state.is(COPYING_MARKER)) {
                        selectedButton = null;
                        state.switchTo(NORMAL);
                    } else {
                        selectedButton = button;
                        state.switchTo(COPYING_MARKER);
                    }
                } else
                    pasteMarker();

            }
        });
        btnShowMarkers = new GuiBookmarkButton(3, Textures.ICON_HIDE_MARKERS, I18n.format("gui.antiqueatlas.hideMarkers"));
        addChild(btnShowMarkers).offsetGuiCoords(300, 71);
        btnShowMarkers.addListener(button -> {
            if (stack != null || !SettingsConfig.gameplay.itemNeeded) {
                selectedButton = null;
                state.switchTo(state.is(HIDING_MARKERS) ? NORMAL : HIDING_MARKERS);
            }
        });

        btnFilterMarkers = new GuiFilterButton();
        addChild(btnFilterMarkers).offsetGuiCoords(300, 90);
        btnFilterMarkers.addListener2(mouseBtn -> {
            if (stack != null || !SettingsConfig.gameplay.itemNeeded) {
                if (mouseBtn == 1/*rmc*/) {
                    btnFilterMarkers.clear();
                    if (!state.is(EDITING_FILTER))
                        state.switchTo(EDITING_FILTER);
                    btnFilterMarkers.setSelected(true);
                    selectedButton = btnFilterMarkers;
                } else { /*lmc*/
                    if (!state.is(EDITING_FILTER))
                        state.switchTo(EDITING_FILTER);
                    btnFilterMarkers.setSelected(true);
                    selectedButton = btnFilterMarkers;
                }
            }
        });

        addChild(scaleBar).offsetGuiCoords(20, 198);
        scaleBar.setMapScale(1);

        markerFinalizer.addListener(blinkingIcon);

        eraserCursor.setTexture(Textures.ERASER, 12, 14, 2, 11);
        copierCursor.setTexture(Textures.COPIER, 12, 14, 2, 11);
        publisherCursor.setTexture(Textures.PUBLISHER, 12, 14, 3, 8);
    }

    public GuiAtlas prepareToOpen(ItemStack stack) {
        this.stack = stack;

        return prepareToOpen();
    }

    public GuiAtlas prepareToOpen() {
        this.player = Minecraft.getMinecraft().player;
        updateAtlasData();
        if (!followPlayer && SettingsConfig.gameplay.doSaveBrowsingPos) {
            loadSavedBrowsingPosition();
        }

        return this;
    }

    public void loadSavedBrowsingPosition() {
        // Apply zoom first, because browsing position depends on it:
        setMapScale(biomeData.getBrowsingZoom());
        mapOffsetX = biomeData.getBrowsingX();
        mapOffsetY = biomeData.getBrowsingY();
        isDragging = false;
    }

    @Override
    public void initGui() {
        super.initGui();
        if (!state.equals(EXPORTING_IMAGE)) {
            state.switchTo(NORMAL); //TODO: his causes the Export PNG progress bar to disappear when resizing game window
        }

        Keyboard.enableRepeatEvents(true);
        screenScale = new ScaledResolution(mc).getScaleFactor();
        setCentered();
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseState) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseState);
        if (state.is(EXPORTING_IMAGE)) {
            return; // Don't remove the progress bar.
        }

        // If clicked on the map, start dragging
        int mapX = (width - MAP_WIDTH) / 2;
        int mapY = (height - MAP_HEIGHT) / 2;
        boolean isMouseOverMap = mouseX >= mapX && mouseX <= mapX + MAP_WIDTH &&
                mouseY >= mapY && mouseY <= mapY + MAP_HEIGHT;

        if (hoveredMarker != null && !isVisibleMarker(hoveredMarker))
            hoveredMarker = null;
        if (hoveredOuterMarker != null && !isVisibleMarker(hoveredOuterMarker))
            hoveredOuterMarker = null;

        if (!state.is(NORMAL) && !state.is(HIDING_MARKERS)) {
            int atlasID = getAtlasID();

            if (state.is(EDITING_FILTER))
                state.switchTo(NORMAL);
            else if (state.is(PLACING_MARKER) // If clicked on the map, place marker:
                    && isMouseOverMap && mouseState == 0 /* left click */) {
                markerFinalizer.setMarkerData(player.getEntityWorld(),
                        atlasID, player.dimension,
                        screenXToWorldX(mouseX), screenYToWorldZ(mouseY));
                addChild(markerFinalizer);

                blinkingIcon.setTexture(markerFinalizer.selectedType.getIcon(),
                        MARKER_SIZE, MARKER_SIZE);
                addChildBehind(markerFinalizer, blinkingIcon)
                        .setRelativeCoords(mouseX - getGuiX() - MARKER_SIZE / 2,
                                mouseY - getGuiY() - MARKER_SIZE / 2);

                // Need to intercept keyboard events to type in the label:
                setInterceptKeyboard(true);

                // Un-press all keys to prevent player from walking infinitely:
                KeyBinding.unPressAllKeys();

            } else if (hoveredMarker != null && !hoveredMarker.isGlobal() && isMouseOverMap && mouseState == 0) {
                if (state.is(DELETING_MARKER))
                    AtlasAPI.markers.deleteMarker(player.getEntityWorld(),
                            atlasID, hoveredMarker.getId());
                else if (state.is(COPYING_MARKER))
                    copyMarker(hoveredMarker);
                else if (state.is(PUBLISHING_MARKER))
                    publishMarker(hoveredMarker);
            }
            state.switchTo(NORMAL);
        } else if (isMouseOverMap && selectedButton == null) {
            if (hoveredMarker == null || !MinecraftForge.EVENT_BUS.post(new MarkerClickedEvent(player, hoveredMarker, mouseState))) {
                isDragging = true;
                dragMouseX = mouseX;
                dragMouseY = mouseY;
                dragMapOffsetX = mapOffsetX;
                dragMapOffsetY = mapOffsetY;
            }
        }

        if (hoveredOuterMarker != null) {
            mapOffsetX = (int) (-hoveredOuterMarker.getX() * mapScale);
            mapOffsetY = (int) (-hoveredOuterMarker.getZ() * mapScale);
            followPlayer = false;
            btnPosition.setEnabled(true);
            hoveredOuterMarker = null;
        }
    }

    private boolean isVisibleMarker(Marker hoveredMarker) {
        return !state.is(HIDING_MARKERS) && isFilteredMarker(hoveredMarker);
    }

    private void publishMarker(Marker marker) {
        selectedButton = null;
        btnPublishMarker.setSelected(false);
        state.switchTo(NORMAL);

        sendChatMessage(PublishingMarkerHandler.prefix +
                marker.getLabel() + PublishingMarkerHandler.separator +
                marker.getX() + PublishingMarkerHandler.separator +
                marker.getZ() + PublishingMarkerHandler.separator +
                marker.getTypeRaw(), false);
    }

    protected static Marker copiedMarker;

    private void copyMarker(Marker marker) {
        copiedMarker = marker;

        selectedButton = null;
        btnCopyMarker.setSelected(false);
        btnCopyMarker.setTitle(I18n.format("gui.antiqueatlas.pasteMarker"));
        btnCopyMarker.setIconTexture(Textures.ICON_PASTE_MARKER);
        state.switchTo(NORMAL);
    }

    private void pasteMarker() {
        if (copiedMarker != null) {
            AtlasAPI.markers.putMarker(player.getEntityWorld(), true, getAtlasID(), copiedMarker.getTypeRaw(), copiedMarker.getLabel(), copiedMarker.getX(), copiedMarker.getZ());

            copiedMarker = null;
        }

        btnCopyMarker.setSelected(false);
        btnCopyMarker.setTitle(I18n.format("gui.antiqueatlas.copyMarker"));
        btnCopyMarker.setIconTexture(Textures.ICON_COPY_MARKER);
        state.switchTo(NORMAL);
    }

    /**
     * Opens a dialog window to select which file to save to, then performs
     * rendering of the map of current dimension into a PNG image.
     */
    private void exportImage(int atlasID) {
        boolean showMarkers = !state.is(HIDING_MARKERS);
        state.switchTo(EXPORTING_IMAGE);
        // Default file name is "Atlas <N>.png"
        ExportImageUtil.isExporting = true;

        File screenshot_folder = new File(Minecraft.getMinecraft().gameDir, "screenshots");
        if (!screenshot_folder.isDirectory()) {
            screenshot_folder.mkdir();
        }

        String outputname = "atlas-" + DATE_FORMAT.format(new Date());

        File file = new File(screenshot_folder, outputname + ".png");
        for (int i = 1; file.exists(); i++) {
            file = new File(screenshot_folder, outputname + "_" + i + ".png");
        }

        if (file != null) {
            try {
                Log.info("Exporting image from Atlas #%d to file %s", atlasID, file.getAbsolutePath());
                ExportImageUtil.exportPngImage(biomeData, localMarkersData, file, showMarkers);
                Log.info("Finished exporting image");
                printSuccessToChat(file);
            } catch (AtlasTooLargeException e) {
                Log.warn(e, "Atlas is too large");
                printLocalizedMessageToChat("gui.antiqueatlas.export.atlasTooLarge");
                ExportImageUtil.isExporting = false;
            } catch (OutOfMemoryError e) {
                Log.warn(e, "Image is too large, trying to export in strips");
                try {
                    ExportImageUtil.exportPngImageTooLarge(biomeData, localMarkersData, file, showMarkers);
                } catch (OutOfMemoryError e2) {
                    int minX = (biomeData.getScope().minX - 1) * ExportImageUtil.TILE_SIZE;
                    int minY = (biomeData.getScope().minY - 1) * ExportImageUtil.TILE_SIZE;
                    int outWidth = (biomeData.getScope().maxX + 2) * ExportImageUtil.TILE_SIZE - minX;
                    int outHeight = (biomeData.getScope().maxY + 2) * ExportImageUtil.TILE_SIZE - minY;

                    Log.error(e2, "Image is STILL too large, how massive is this map?! Answer: (%dx%d)", outWidth, outHeight);

                    printLocalizedMessageToChat("gui.antiqueatlas.export.tooLarge");
                    ExportImageUtil.isExporting = false;
                }
            }
        }
        ExportImageUtil.isExporting = false;
        state.switchTo(showMarkers ? NORMAL : HIDING_MARKERS);
    }

    @Override
    public void handleKeyboardInput() throws IOException {
        super.handleKeyboardInput();
        if (Keyboard.getEventKeyState()) {
            int key = Keyboard.getEventKey();
            if (key == Keyboard.KEY_UP) {
                navigateMap(0, navigateStep);
            } else if (key == Keyboard.KEY_DOWN) {
                navigateMap(0, -navigateStep);
            } else if (key == Keyboard.KEY_LEFT) {
                navigateMap(navigateStep, 0);
            } else if (key == Keyboard.KEY_RIGHT) {
                navigateMap(-navigateStep, 0);
            } else if (key == Keyboard.KEY_ADD || key == Keyboard.KEY_EQUALS) {
                setMapScale(mapScale * 2);
            } else if (key == Keyboard.KEY_SUBTRACT || key == Keyboard.KEY_MINUS) {
                setMapScale(mapScale / 2);
            } else if (key == mc.gameSettings.keyBindInventory.getKeyCode() && !btnFilterMarkers.textFieldFocused) {
                close();
            }
            // Close the GUI if a hotbar key is pressed
            else {
                /*
                KeyBinding[] hotbarKeys = mc.gameSettings.keyBindsHotbar;
                for (KeyBinding bind : hotbarKeys) {
                    if (key == bind.getKeyCode()) {
                        close();
                        break;
                    }
                }*/
            }
        }
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();

        int wheelMove = Mouse.getEventDWheel();
        if (wheelMove != 0) {
            wheelMove = wheelMove > 0 ? 1 : -1;
            if (SettingsConfig.userInterface.doReverseWheelZoom) {
                wheelMove *= -1;
            }

            int mouseOffsetX = mc.displayWidth / screenScale / 2 - getMouseX();
            int mouseOffsetY = mc.displayHeight / screenScale / 2 - getMouseY();
            double newScale = mapScale * Math.pow(2, wheelMove);
            int addOffsetX = 0;
            int addOffsetY = 0;
            if (Math.abs(mouseOffsetX) < MAP_WIDTH / 2 && Math.abs(mouseOffsetY) < MAP_HEIGHT / 2) {
                addOffsetX = mouseOffsetX * wheelMove;
                addOffsetY = mouseOffsetY * wheelMove;

                if (wheelMove > 0) {
                    addOffsetX *= mapScale / newScale;
                    addOffsetY *= mapScale / newScale;
                }
            }

            setMapScale(newScale, addOffsetX, addOffsetY);
        }
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int mouseState) {
        super.mouseReleased(mouseX, mouseY, mouseState);
        if (mouseState != -1) {
            selectedButton = null;
            isDragging = false;
        }
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int lastMouseButton, long timeSinceMouseClick) {
        super.mouseClickMove(mouseX, mouseY, lastMouseButton, timeSinceMouseClick);
        if (isDragging) {
            followPlayer = false;
            btnPosition.setEnabled(true);
            mapOffsetX = dragMapOffsetX + mouseX - dragMouseX;
            mapOffsetY = dragMapOffsetY + mouseY - dragMouseY;
        }
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        if (player == null) return;
        if (followPlayer) {
            mapOffsetX = (int) (-player.posX * mapScale);
            mapOffsetY = (int) (-player.posZ * mapScale);
        }
        if (player.getEntityWorld().getTotalWorldTime() > timeButtonPressed + BUTTON_PAUSE) {
            navigateByButton(selectedButton);
        }

        updateAtlasData();
    }

    /**
     * Update {@link #biomeData}, {@link #localMarkersData}
     */
    private void updateAtlasData() {
        int atlasID = getAtlasID();

        biomeData = AntiqueAtlasMod.atlasData
                .getAtlasData(atlasID, player.getEntityWorld())
                .getDimensionData(player.dimension);
        MarkersData markersData = AntiqueAtlasMod.markersData
                .getMarkersData(atlasID, player.getEntityWorld());
        if (markersData != null) {
            localMarkersData = markersData
                    .getMarkersDataInDimension(player.dimension);
        } else {
            localMarkersData = null;
        }
    }

    /**
     * Offset the map view depending on which button was pressed.
     */
    private void navigateByButton(GuiComponentButton btn) {
        if (btn == null) return;
        if (btn.equals(btnUp)) {
            navigateMap(0, navigateStep);
        } else if (btn.equals(btnDown)) {
            navigateMap(0, -navigateStep);
        } else if (btn.equals(btnLeft)) {
            navigateMap(navigateStep, 0);
        } else if (btn.equals(btnRight)) {
            navigateMap(-navigateStep, 0);
        }
    }

    /**
     * Offset the map view by given values, in blocks.
     */
    private void navigateMap(int dx, int dy) {
        mapOffsetX += dx;
        mapOffsetY += dy;
        followPlayer = false;
        btnPosition.setEnabled(true);
    }

    /**
     * Set the pixel-to-block ratio, maintaining the current center of the screen.
     */
    public void setMapScale(double scale) {
        setMapScale(scale, 0, 0);
    }

    /**
     * Set the pixel-to-block ratio, maintaining the current center of the screen with additional offset.
     */
    protected void setMapScale(double scale, int addOffsetX, int addOffsetY) {
        double oldScale = mapScale;
        mapScale = Math.min(Math.max(scale, SettingsConfig.userInterface.minScale), SettingsConfig.userInterface.maxScale);

        // Scaling not needed
        if (oldScale == mapScale) {
            return;
        }

        if (mapScale >= MIN_SCALE_THRESHOLD) {
            tileHalfSize = (int) Math.round(8 * mapScale);
            tile2ChunkScale = 1;
        } else {
            tileHalfSize = (int) Math.round(8 * MIN_SCALE_THRESHOLD);
            tile2ChunkScale = (int) Math.round(MIN_SCALE_THRESHOLD / mapScale);
        }

        // Times 2 because the contents of the Atlas are rendered at resolution 2 times smaller:
        scaleBar.setMapScale(mapScale * 2);
        mapOffsetX = (int) ((mapOffsetX + addOffsetX) * (mapScale / oldScale));
        mapOffsetY = (int) ((mapOffsetY + addOffsetY) * (mapScale / oldScale));
        dragMapOffsetX *= mapScale / oldScale;
        dragMapOffsetY *= mapScale / oldScale;
        // 2^13 = 8192
        scaleClipIndex = MathHelper.log2((int) (mapScale * 8192)) + 1 - 13;
        zoomLevel = -scaleClipIndex + zoomLevelOne;
        scaleAlpha = 255;

        if (followPlayer && (addOffsetX != 0 || addOffsetY != 0)) {
            followPlayer = false;
            btnPosition.setEnabled(true);
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float par3) {
        long currentMillis = System.currentTimeMillis();
        long deltaMillis = currentMillis - lastUpdateMillis;
        lastUpdateMillis = currentMillis;

        if (SettingsConfig.performance.debugRender) {
            renderTimes[renderTimesIndex++] = System.currentTimeMillis();
            if (renderTimesIndex == renderTimes.length) {
                renderTimesIndex = 0;
                double elapsed = 0;
                for (int i = 0; i < renderTimes.length - 1; i++) {
                    elapsed += renderTimes[i + 1] - renderTimes[i];
                }
                System.out.printf("GuiAtlas avg. render time: %.3f\n", elapsed / renderTimes.length);
            }
        }

        GlStateManager.color(1, 1, 1, 1);
        GlStateManager.alphaFunc(GL11.GL_GREATER, 0); // So light detail on tiles is visible
        AtlasRenderHelper.drawFullTexture(Textures.BOOK, getGuiX(), getGuiY(), WIDTH, HEIGHT);

        if ((stack == null && SettingsConfig.gameplay.itemNeeded) || biomeData == null)
            return;

        if (state.is(DELETING_MARKER)) {
            GlStateManager.color(1, 1, 1, 0.5f);
        }
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor((getGuiX() + CONTENT_X) * screenScale,
                mc.displayHeight - (getGuiY() + CONTENT_Y + MAP_HEIGHT) * screenScale,
                MAP_WIDTH * screenScale, MAP_HEIGHT * screenScale);
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        // Find chunk coordinates of the top left corner of the map.
        // The 'roundToBase' is required so that when the map scales below the
        // threshold the tiles don't change when map position changes slightly.
        // The +-2 at the end provide margin so that tiles at the edges of
        // the page have their stitched texture correct.
        int mapStartX = MathUtil.roundToBase((int) Math.floor(-((double) MAP_WIDTH / 2d + mapOffsetX + 2 * tileHalfSize) / mapScale / 16d), tile2ChunkScale);
        int mapStartZ = MathUtil.roundToBase((int) Math.floor(-((double) MAP_HEIGHT / 2d + mapOffsetY + 2 * tileHalfSize) / mapScale / 16d), tile2ChunkScale);
        int mapEndX = MathUtil.roundToBase((int) Math.ceil(((double) MAP_WIDTH / 2d - mapOffsetX + 2 * tileHalfSize) / mapScale / 16d), tile2ChunkScale);
        int mapEndZ = MathUtil.roundToBase((int) Math.ceil(((double) MAP_HEIGHT / 2d - mapOffsetY + 2 * tileHalfSize) / mapScale / 16d), tile2ChunkScale);
        int mapStartScreenX = getGuiX() + WIDTH / 2 + (int) ((mapStartX << 4) * mapScale) + mapOffsetX;
        int mapStartScreenY = getGuiY() + HEIGHT / 2 + (int) ((mapStartZ << 4) * mapScale) + mapOffsetY;
        TileRenderIterator iter = new TileRenderIterator(biomeData);
        iter.setScope(new Rect().setOrigin(mapStartX, mapStartZ).
                set(mapStartX, mapStartZ, mapEndX, mapEndZ));
        iter.setStep(tile2ChunkScale);
        while (iter.hasNext()) {
            SubTileQuartet subtiles = iter.next();
            for (SubTile subtile : subtiles) {
                if (subtile == null || subtile.tile == null) continue;
                AtlasRenderHelper.drawAutotileCorner(
                        BiomeTextureMap.instance().getTexture(subtile.tile),
                        mapStartScreenX + subtile.x * tileHalfSize,
                        mapStartScreenY + subtile.y * tileHalfSize,
                        subtile.getTextureU(), subtile.getTextureV(), tileHalfSize);
            }
        }

        int markersStartX = MathUtil.roundToBase(mapStartX, MarkersData.CHUNK_STEP) / MarkersData.CHUNK_STEP - 1;
        int markersStartZ = MathUtil.roundToBase(mapStartZ, MarkersData.CHUNK_STEP) / MarkersData.CHUNK_STEP - 1;
        int markersEndX = MathUtil.roundToBase(mapEndX, MarkersData.CHUNK_STEP) / MarkersData.CHUNK_STEP + 1;
        int markersEndZ = MathUtil.roundToBase(mapEndZ, MarkersData.CHUNK_STEP) / MarkersData.CHUNK_STEP + 1;
        double iconScale = getIconScale();

        // Don't draw global markers! They will be added as local markers.

        // Draw local markers:
        if (localMarkersData != null) {
            for (int x = markersStartX; x <= markersEndX; x++) {
                for (int z = markersStartZ; z <= markersEndZ; z++) {
                    List<Marker> markers = localMarkersData.getMarkersAtChunk(x, z);
                    if (markers == null) continue;
                    for (Marker marker : markers) {
                        if (isFilteredMarker(marker))
                            renderMarker(marker, iconScale, worldXToScreenX(marker.getX()), worldZToScreenY(marker.getZ()), false);
                    }
                }
            }
        }

        GL11.glDisable(GL11.GL_SCISSOR_TEST);

        // Overlay the frame so that edges of the map are smooth:
        GlStateManager.color(1, 1, 1, 1);
        AtlasRenderHelper.drawFullTexture(Textures.BOOK_FRAME, getGuiX(), getGuiY(), WIDTH, HEIGHT);

        if (localMarkersData != null) {
            if (state.is(NORMAL) || state.is(EDITING_FILTER))
                if (!btnFilterMarkers.getText().isEmpty()) {
                    for (Marker marker : localMarkersData.getAllMarkers()) {
                        if (isOuterMarker(markersStartX, markersStartZ, markersEndX, markersEndZ, marker)) {
                            if (isFilteredMarker(marker)) {
                                GlStateManager.color(1, 1, 1, 0.3f);
                                renderMarker(marker, iconScale,
                                        clamp(worldXToScreenX(marker.getX()), getGuiX() + CONTENT_X, getGuiX() + CONTENT_X + MAP_WIDTH),
                                        clamp(worldZToScreenY(marker.getZ()), getGuiY() + CONTENT_Y, getGuiY() + CONTENT_Y + MAP_HEIGHT),
                                        true);
                            }
                        }
                    }
                }
        }

        renderScaleOverlay(deltaMillis);
        iconScale = getIconScale();

        // Draw player icon:
        if (!state.is(HIDING_MARKERS)) {
            // How much the player has moved from the top left corner of the map, in pixels:
            int playerOffsetX = (int) (player.posX * mapScale) + mapOffsetX;
            int playerOffsetZ = (int) (player.posZ * mapScale) + mapOffsetY;
            if (playerOffsetX < -MAP_WIDTH / 2) playerOffsetX = -MAP_WIDTH / 2;
            if (playerOffsetX > MAP_WIDTH / 2) playerOffsetX = MAP_WIDTH / 2;
            if (playerOffsetZ < -MAP_HEIGHT / 2) playerOffsetZ = -MAP_HEIGHT / 2;
            if (playerOffsetZ > MAP_HEIGHT / 2 - 2) playerOffsetZ = MAP_HEIGHT / 2 - 2;
            // Draw the icon:
            GlStateManager.color(1, 1, 1, state.is(PLACING_MARKER) ? 0.5f : 1);
            GlStateManager.pushMatrix();
            GlStateManager.translate(getGuiX() + WIDTH / 2 + playerOffsetX, getGuiY() + HEIGHT / 2 + playerOffsetZ, 0);
            float playerRotation = (float) Math.round(player.rotationYaw / 360f * PLAYER_ROTATION_STEPS) / PLAYER_ROTATION_STEPS * 360f;
            GlStateManager.rotate(180 + playerRotation, 0, 0, 1);
            GlStateManager.translate(-PLAYER_ICON_WIDTH / 2 * iconScale, -PLAYER_ICON_HEIGHT / 2 * iconScale, 0);
            AtlasRenderHelper.drawFullTexture(Textures.PLAYER, 0, 0,
                    (int) Math.round(PLAYER_ICON_WIDTH * iconScale), (int) Math.round(PLAYER_ICON_HEIGHT * iconScale));
            GlStateManager.popMatrix();
            GlStateManager.color(1, 1, 1, 1);
        }

        // Draw buttons:
        super.drawScreen(mouseX, mouseY, par3);

        // Draw the semi-transparent marker attached to the cursor when placing a new marker:
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        if (state.is(PLACING_MARKER)) {
            GlStateManager.color(1, 1, 1, 0.5f);
            markerFinalizer.selectedType.calculateMip(iconScale, mapScale, screenScale);
            MarkerRenderInfo renderInfo = markerFinalizer.selectedType.getRenderInfo(iconScale, mapScale, screenScale);
            markerFinalizer.selectedType.resetMip();
            AtlasRenderHelper.drawFullTexture(
                    renderInfo.tex,
                    mouseX + renderInfo.x, mouseY + renderInfo.y,
                    renderInfo.width, renderInfo.height);
            GlStateManager.color(1, 1, 1, 1);

            drawHoveringText(screenXToWorldX(mouseX) + ", " + screenYToWorldZ(mouseY), mouseX, mouseY);
        }

        // Draw progress overlay:
        if (state.is(EXPORTING_IMAGE)) {
            drawDefaultBackground();
            progressBar.draw((width - 100) / 2, height / 2 - 34);
        }
    }

    private boolean isOuterMarker(int markersStartX, int markersStartZ, int markersEndX, int markersEndZ, Marker marker) {
        int largeChunkX = marker.getChunkX() / MarkersData.CHUNK_STEP;
        int largeChunkZ = marker.getChunkZ() / MarkersData.CHUNK_STEP;
        return largeChunkX < markersStartX || largeChunkX > markersEndX || largeChunkZ < markersStartZ || largeChunkZ > markersEndZ;
    }

    private boolean isFilteredMarker(Marker marker) {
        return marker.getLabel().toLowerCase().contains(btnFilterMarkers.getText());
    }

    protected void renderScaleOverlay(long deltaMillis) {
        if (scaleAlpha > 0) {
            GlStateManager.translate(getGuiX() + WIDTH - 13, getGuiY() + 12, 0);

            String text;
            int textWidth, xWidth;

            text = "x";
            xWidth = textWidth = fontRenderer.getStringWidth(text);
            xWidth++;
            fontRenderer.drawString(text, -textWidth, 0, scaleAlpha << 24);

            text = zoomNames[zoomLevel];
            if (text.contains("/")) {

                String[] parts = text.split("/");

                text = parts[0];
                int centerXtranslate = Math.max(fontRenderer.getStringWidth(parts[0]), fontRenderer.getStringWidth(parts[1])) / 2;
                GlStateManager.translate(-xWidth - centerXtranslate, -fontRenderer.FONT_HEIGHT / 2, 0);

                GlStateManager.disableTexture2D();
                Tessellator t = Tessellator.getInstance();
                BufferBuilder vb = t.getBuffer();
                vb.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION);
                vb.pos(centerXtranslate, fontRenderer.FONT_HEIGHT - 1, 0.0D).endVertex();
                vb.pos(-centerXtranslate - 1, fontRenderer.FONT_HEIGHT - 1, 0.0D).endVertex();
                vb.pos(-centerXtranslate - 1, fontRenderer.FONT_HEIGHT, 0.0D).endVertex();
                vb.pos(centerXtranslate, fontRenderer.FONT_HEIGHT, 0.0D).endVertex();
                t.draw();
                GlStateManager.enableTexture2D();
                textWidth = fontRenderer.getStringWidth(text);
                fontRenderer.drawString(text, -textWidth / 2, 0, scaleAlpha << 24);

                text = parts[1];
                GlStateManager.translate(0, fontRenderer.FONT_HEIGHT + 1, 0);

                textWidth = fontRenderer.getStringWidth(text);
                fontRenderer.drawString(text, -textWidth / 2, 0, scaleAlpha << 24);

                GlStateManager.translate(xWidth + centerXtranslate, (-fontRenderer.FONT_HEIGHT / 2) - 2, 0);
            } else {
                textWidth = fontRenderer.getStringWidth(text);
                fontRenderer.drawString(text, -textWidth - xWidth + 1, 1, scaleAlpha << 24);
            }

            GlStateManager.translate(-(getGuiX() + WIDTH - 13), -(getGuiY() + 12), 0);

            int deltaScaleAlpha = (int) (deltaMillis * 0.256);
            // because of some crazy high frame rate
            if (deltaScaleAlpha == 0) {
                deltaScaleAlpha = 1;
            }

            scaleAlpha -= 20 * deltaScaleAlpha;

            if (scaleAlpha < 0)
                scaleAlpha = 0;
        }
    }

    private void renderMarker(Marker marker, double scale, int markerX, int markerY, boolean outer) {

        MarkerType type = MarkerRegistry.find(marker.getTypeForRender());
        if (type == null) {
            return;
        }
        if (type.shouldHide(state.is(HIDING_MARKERS), scaleClipIndex)) {
            return;
        }

        if (!marker.isVisibleAhead() &&
                !biomeData.hasTileAt(marker.getChunkX(), marker.getChunkZ())) {
            return;
        }
        type.calculateMip(scale, mapScale, screenScale);
        MarkerRenderInfo info = type.getRenderInfo(scale, mapScale, screenScale);

        boolean mouseIsOverMarker = type.shouldHover((getMouseX() - (markerX + info.x)) / info.width, (getMouseY() - (markerY + info.y)) / info.height);
        type.resetMip();

        final float outerAlpha = 0.3f;

        if (mouseIsOverMarker) {
            GlStateManager.color(0.5f, 0.5f, 0.5f, 1);
            if (outer)
                hoveredOuterMarker = marker;
            else
                hoveredMarker = marker;
            MinecraftForge.EVENT_BUS.post(new MarkerHoveredEvent(player, marker));
        } else {
            GlStateManager.color(1, 1, 1, outer ? outerAlpha : 1);
            if (hoveredMarker == marker) {
                if (outer)
                    hoveredOuterMarker = null;
                hoveredMarker = null;
            }
        }

        if (state.is(PLACING_MARKER)) {
            GlStateManager.color(1, 1, 1, 0.5f);
        } else if (state.is(DELETING_MARKER) && marker.isGlobal()) {
            GlStateManager.color(1, 1, 1, 0.5f);
        } else {
            GlStateManager.color(1, 1, 1, outer && !mouseIsOverMarker ? outerAlpha : 1);
        }

        if (SettingsConfig.performance.debugRender) {
            System.out.println("Rendering Marker: " + info.tex);
        }

        AtlasRenderHelper.drawFullTexture(
                info.tex,
                markerX + info.x,
                markerY + info.y,
                info.width, info.height);

        if (isMouseOver && mouseIsOverMarker) {
            if (state.is(PLACING_MARKER)) {
                if (marker.getLabel().length() > 0)
                    drawTooltip(Collections.singletonList(marker.getLocalizedLabel()), mc.fontRenderer);

            } else {
                String coordsLabel = marker.getX() + ", " + marker.getZ();
                if (marker.getLabel().isEmpty())
                    drawTooltip(ImmutableList.of(coordsLabel), mc.fontRenderer);
                else
                    drawTooltip(ImmutableList.of(marker.getLocalizedLabel(), coordsLabel), mc.fontRenderer);
            }
        }
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    @Override
    public void onGuiClosed() {
        super.onGuiClosed();
        removeChild(markerFinalizer);
        removeChild(blinkingIcon);
        Keyboard.enableRepeatEvents(false);
        biomeData.setBrowsingPosition(mapOffsetX, mapOffsetY, mapScale);

        PacketDispatcher.sendToServer(new BrowsingPositionPacket(getAtlasID(), player.dimension, mapOffsetX, mapOffsetY, mapScale));
    }

    /**
     * Returns the Y coordinate that the cursor is pointing at.
     */
    protected int screenXToWorldX(int mouseX) {
        return (int) Math.round((double) (mouseX - this.width / 2 - mapOffsetX) / mapScale);
    }

    /**
     * Returns the Y block coordinate that the cursor is pointing at.
     */
    protected int screenYToWorldZ(int mouseY) {
        return (int) Math.round((double) (mouseY - this.height / 2 - mapOffsetY) / mapScale);
    }

    protected int worldXToScreenX(int x) {
        return (int) Math.round((double) x * mapScale + this.width / 2 + mapOffsetX);
    }

    protected int worldZToScreenY(int z) {
        return (int) Math.round((double) z * mapScale + this.height / 2 + mapOffsetY);
    }

    @Override
    protected void onChildClosed(GuiComponent child) {
        if (child.equals(markerFinalizer)) {
            setInterceptKeyboard(true);
            removeChild(blinkingIcon);
        }
    }

    /**
     * Update all text labels to current localization.
     */
    public void updateL18n() {
        btnExportPng.setTitle(I18n.format("gui.antiqueatlas.exportImage"));
        btnMarker.setTitle(I18n.format("gui.antiqueatlas.addMarker"));
    }

    /**
     * Returns the scale of markers and player icon at given mapScale.
     */
    protected double getIconScale() {
        return SettingsConfig.userInterface.doScaleMarkers ? (mapScale < 0.5 ? 0.5 : mapScale > 1 ? 2 : 1) : 1;
    }

    /**
     * Returns atlas id based on "itemNeeded" option
     */
    protected int getAtlasID() {
        return SettingsConfig.gameplay.itemNeeded ? stack.getItemDamage() : player.getUniqueID().hashCode();
    }

    private static void printSuccessToChat(File file) {
        ITextComponent text = new TextComponentString(file.getName());
        String path;
        try {
            path = file.getCanonicalFile().getAbsolutePath();
        } catch (IOException e) {
            path = file.getAbsolutePath();
        }
        text.getStyle().setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_FILE, path));
        text.getStyle().setUnderlined(true);
        TextComponentTranslation post = new TextComponentTranslation("gui.antiqueatlas.export.success", text);
        Minecraft.getMinecraft().ingameGUI.getChatGUI().printChatMessage(post);
    }

    private static void printLocalizedMessageToChat(String translationKey) {
        Minecraft.getMinecraft().ingameGUI.getChatGUI().printChatMessage(new TextComponentTranslation(translationKey));
    }
}
