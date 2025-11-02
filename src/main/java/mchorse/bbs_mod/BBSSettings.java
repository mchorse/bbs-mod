package mchorse.bbs_mod;

import mchorse.bbs_mod.settings.SettingsBuilder;
import mchorse.bbs_mod.settings.values.core.ValueLink;
import mchorse.bbs_mod.settings.values.core.ValueString;
import mchorse.bbs_mod.settings.values.numeric.ValueBoolean;
import mchorse.bbs_mod.settings.values.numeric.ValueFloat;
import mchorse.bbs_mod.settings.values.numeric.ValueInt;
import mchorse.bbs_mod.settings.values.ui.ValueColors;
import mchorse.bbs_mod.settings.values.ui.ValueEditorLayout;
import mchorse.bbs_mod.settings.values.ui.ValueLanguage;
import mchorse.bbs_mod.settings.values.ui.ValueOnionSkin;
import mchorse.bbs_mod.settings.values.ui.ValueStringKeys;
import mchorse.bbs_mod.settings.values.ui.ValueVideoSettings;
import mchorse.bbs_mod.utils.MathUtils;
import mchorse.bbs_mod.utils.colors.Colors;

import java.util.HashSet;

public class BBSSettings
{
    public static ValueColors favoriteColors;
    public static ValueStringKeys disabledSheets;
    public static ValueLanguage language;
    public static ValueInt primaryColor;
    public static ValueBoolean enableTrackpadIncrements;
    public static ValueBoolean enableTrackpadScrolling;
    public static ValueInt userIntefaceScale;
    public static ValueInt tooltipStyle;
    public static ValueFloat fov;
    public static ValueBoolean hsvColorPicker;
    public static ValueBoolean forceQwerty;
    public static ValueBoolean freezeModels;
    public static ValueFloat axesScale;
    public static ValueBoolean uniformScale;
    public static ValueBoolean clickSound;

    public static ValueBoolean enableCursorRendering;
    public static ValueBoolean enableMouseButtonRendering;
    public static ValueBoolean enableKeystrokeRendering;
    public static ValueInt keystrokeOffset;
    public static ValueInt keystrokeMode;

    public static ValueLink backgroundImage;
    public static ValueInt backgroundColor;

    public static ValueBoolean chromaSkyEnabled;
    public static ValueInt chromaSkyColor;
    public static ValueBoolean chromaSkyTerrain;
    public static ValueFloat chromaSkyBillboard;

    public static ValueInt scrollbarShadow;
    public static ValueInt scrollbarWidth;
    public static ValueFloat scrollingSensitivity;
    public static ValueFloat scrollingSensitivityHorizontal;
    public static ValueBoolean scrollingSmoothness;

    public static ValueBoolean multiskinMultiThreaded;

    public static ValueString videoEncoderPath;
    public static ValueBoolean videoEncoderLog;
    public static ValueVideoSettings videoSettings;

    public static ValueFloat editorCameraSpeed;
    public static ValueFloat editorCameraAngleSpeed;
    public static ValueInt duration;
    public static ValueBoolean editorLoop;
    public static ValueInt editorJump;
    public static ValueInt editorGuidesColor;
    public static ValueBoolean editorRuleOfThirds;
    public static ValueBoolean editorCenterLines;
    public static ValueBoolean editorCrosshair;
    public static ValueBoolean editorSeconds;
    public static ValueInt editorPeriodicSave;
    public static ValueBoolean editorHorizontalFlight;
    public static ValueEditorLayout editorLayoutSettings;
    public static ValueOnionSkin editorOnionSkin;
    public static ValueBoolean editorSnapToMarkers;
    public static ValueBoolean editorClipPreview;
    public static ValueBoolean editorRewind;
    public static ValueBoolean editorHorizontalClipEditor;

    public static ValueFloat recordingCountdown;
    public static ValueBoolean recordingSwipeDamage;
    public static ValueBoolean recordingOverlays;
    public static ValueInt recordingPoseTransformOverlays;

    public static ValueBoolean renderAllModelBlocks;
    public static ValueBoolean clickModelBlocks;
    public static ValueBoolean visualizeStructures;

    public static ValueString entitySelectorsPropertyWhitelist;

    public static ValueBoolean damageControl;

    public static ValueBoolean shaderCurvesEnabled;

    public static ValueBoolean audioWaveformVisible;
    public static ValueInt audioWaveformDensity;
    public static ValueFloat audioWaveformWidth;
    public static ValueInt audioWaveformHeight;
    public static ValueBoolean audioWaveformFilename;
    public static ValueBoolean audioWaveformTime;
    
    /* Pose track bone anchoring */
    public static ValueBoolean boneAnchoringEnabled;
    public static ValueBoolean anchorOverrideEnabled;

    public static int primaryColor()
    {
        return primaryColor(Colors.A50);
    }

    public static int primaryColor(int alpha)
    {
        return primaryColor.get() | alpha;
    }

    public static int getDefaultDuration()
    {
        return duration == null ? 30 : duration.get();
    }

    public static float getFov()
    {
        return BBSSettings.fov == null ? MathUtils.toRad(50) : MathUtils.toRad(BBSSettings.fov.get());
    }

    public static void register(SettingsBuilder builder)
    {
        builder.category("appearance");

        builder.register(language = new ValueLanguage("language"));
        primaryColor = builder.getInt("primary_color", Colors.ACTIVE).color();
        enableTrackpadIncrements = builder.getBoolean("trackpad_increments", true);
        enableTrackpadScrolling = builder.getBoolean("trackpad_scrolling", true);
        userIntefaceScale = builder.getInt("ui_scale", 2, 0, 4);
        tooltipStyle = builder.getInt("tooltip_style", 1);
        fov = builder.getFloat("fov", 40, 0, 180);
        hsvColorPicker = builder.getBoolean("hsv_color_picker", true);
        forceQwerty = builder.getBoolean("force_qwerty", false);
        freezeModels = builder.getBoolean("freeze_models", false);
        axesScale = builder.getFloat("axes_scale", 1F, 0F, 2F);
        uniformScale = builder.getBoolean("uniform_scale", false);
        clickSound = builder.getBoolean("click_sound", false);

        HashSet<String> defaultFilters = new HashSet<>();

        defaultFilters.add("item_off_hand");
        defaultFilters.add("item_head");
        defaultFilters.add("item_chest");
        defaultFilters.add("item_legs");
        defaultFilters.add("item_feet");
        defaultFilters.add("vX");
        defaultFilters.add("vY");
        defaultFilters.add("vZ");
        defaultFilters.add("grounded");
        defaultFilters.add("stick_rx");
        defaultFilters.add("stick_ry");
        defaultFilters.add("trigger_l");
        defaultFilters.add("trigger_r");
        defaultFilters.add("extra1_x");
        defaultFilters.add("extra1_y");
        defaultFilters.add("extra2_x");
        defaultFilters.add("extra2_y");

        favoriteColors = new ValueColors("favorite_colors");
        disabledSheets = new ValueStringKeys("disabled_sheets");
        disabledSheets.set(defaultFilters);
        builder.register(favoriteColors);
        builder.register(disabledSheets);

        enableCursorRendering = builder.category("tutorials").getBoolean("cursor", false);
        enableMouseButtonRendering = builder.getBoolean("mouse_buttons", false);
        enableKeystrokeRendering = builder.getBoolean("keystrokes", false);
        keystrokeOffset = builder.getInt("keystrokes_offset", 10, 0, 20);
        keystrokeMode = builder.getInt("keystrokes_position", 1);

        backgroundImage = builder.category("background").getRL("image", null);
        backgroundColor = builder.getInt("color", Colors.A75).colorAlpha();

        chromaSkyEnabled = builder.category("chroma_sky").getBoolean("enabled", false);
        chromaSkyColor = builder.getInt("color", Colors.A75).color();
        chromaSkyTerrain = builder.getBoolean("terrain", true);
        chromaSkyBillboard = builder.getFloat("billboard", 0F, 0F, 256F);

        scrollbarShadow = builder.category("scrollbars").getInt("shadow", Colors.A50).colorAlpha();
        scrollbarWidth = builder.getInt("width", 4, 2, 10);
        scrollingSensitivity = builder.getFloat("sensitivity", 1F, 0F, 10F);
        scrollingSensitivityHorizontal = builder.getFloat("sensitivity_horizontal", 1F, 0F, 10F);
        scrollingSmoothness = builder.getBoolean("smoothness", true);

        multiskinMultiThreaded = builder.category("multiskin").getBoolean("multithreaded", true);

        videoEncoderPath = builder.category("video").getString("encoder_path", "ffmpeg");
        videoEncoderLog = builder.getBoolean("log", true);
        builder.register(videoSettings = new ValueVideoSettings("settings"));

        /* Camera editor */
        editorCameraSpeed = builder.category("editor").getFloat("speed", 1F, 0.1F, 100F);
        editorCameraAngleSpeed = builder.getFloat("angle_speed", 1F, 0.1F, 100F);
        duration = builder.getInt("duration", 30, 1, 1000);
        editorJump = builder.getInt("jump", 5, 1, 1000);
        editorLoop = builder.getBoolean("loop", false);
        editorGuidesColor = builder.getInt("guides_color", 0xcccc0000).colorAlpha();
        editorRuleOfThirds = builder.getBoolean("rule_of_thirds", false);
        editorCenterLines = builder.getBoolean("center_lines", false);
        editorCrosshair = builder.getBoolean("crosshair", false);
        editorSeconds = builder.getBoolean("seconds", false);
        editorPeriodicSave = builder.getInt("periodic_save", 60, 0, 3600);
        editorHorizontalFlight = builder.getBoolean("horizontal_flight", false);
        builder.register(editorLayoutSettings = new ValueEditorLayout("layout"));
        builder.register(editorOnionSkin = new ValueOnionSkin("onion_skin"));
        editorSnapToMarkers = builder.getBoolean("snap_to_markers", false);
        editorClipPreview = builder.getBoolean("clip_preview", true);
        editorRewind = builder.getBoolean("rewind", true);
        editorHorizontalClipEditor = builder.getBoolean("horizontal_clip_editor", true);

        recordingCountdown = builder.category("recording").getFloat("countdown", 1.5F, 0F, 30F);
        recordingSwipeDamage = builder.getBoolean("swipe_damage", false);
        recordingOverlays = builder.getBoolean("overlays", true);
        recordingPoseTransformOverlays = builder.getInt("pose_transform_overlays", 0, 0, 42);

        renderAllModelBlocks = builder.category("model_blocks").getBoolean("render_all", true);
        clickModelBlocks = builder.getBoolean("click", true);
        visualizeStructures = builder.getBoolean("visualize_structures", false);

        entitySelectorsPropertyWhitelist = builder.category("entity_selectors").getString("whitelist", "CustomName,Name");

        damageControl = builder.category("dc").getBoolean("enabled", true);

        shaderCurvesEnabled = builder.category("shader_curves").getBoolean("enabled", true);

        builder.category("audio");
        audioWaveformVisible = builder.getBoolean("waveform_visible", true);
        audioWaveformDensity = builder.getInt("waveform_density", 20, 10, 100);
        audioWaveformWidth = builder.getFloat("waveform_width", 0.8F, 0F, 1F);
        audioWaveformHeight = builder.getInt("waveform_height", 24, 10, 40);
        audioWaveformFilename = builder.getBoolean("waveform_filename", false);
        audioWaveformTime = builder.getBoolean("waveform_time", false);


        /* Pose track selection: restaurar opciones visibles en el panel */
        builder.category("pose_track_selection");
        boneAnchoringEnabled = builder.getBoolean("bone_anchoring_enabled", false);
        anchorOverrideEnabled = builder.getBoolean("anchor_override_enabled", false);
    }
}