package mchorse.bbs_mod.ui;

import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.utils.keys.KeyCombo;
import org.lwjgl.glfw.GLFW;

/**
 * IF THE KEYS DON'T APPEAR IN THE CONFIGURATION MENU, you used wrong constructor!
 * Use {@link KeyCombo#KeyCombo(String, IKey, int...)} intead of {@link KeyCombo#KeyCombo(IKey, int...)}!
 */
public class Keys
{
    /* General */
    public static final KeyCombo DESELECT = new KeyCombo("deselect", UIKeys.CAMERA_EDITOR_KEYS_CLIPS_DESELECT, GLFW.GLFW_KEY_D, GLFW.GLFW_KEY_LEFT_CONTROL);
    public static final KeyCombo KEYBINDS = new KeyCombo("keybinds", UIKeys.KEYS_LIST, GLFW.GLFW_KEY_F9);
    public static final KeyCombo NEXT = new KeyCombo("next", UIKeys.CAMERA_EDITOR_KEYS_EDITOR_NEXT, GLFW.GLFW_KEY_RIGHT).repeatable();
    public static final KeyCombo PLAUSE = new KeyCombo("plause", UIKeys.CAMERA_EDITOR_KEYS_EDITOR_PLAUSE, GLFW.GLFW_KEY_SPACE);
    public static final KeyCombo PREV = new KeyCombo("prev", UIKeys.CAMERA_EDITOR_KEYS_EDITOR_PREV, GLFW.GLFW_KEY_LEFT).repeatable();
    public static final KeyCombo REDO = new KeyCombo("redo", UIKeys.CAMERA_EDITOR_KEYS_EDITOR_REDO, GLFW.GLFW_KEY_Y, GLFW.GLFW_KEY_LEFT_CONTROL);
    public static final KeyCombo UNDO = new KeyCombo("undo", UIKeys.CAMERA_EDITOR_KEYS_EDITOR_UNDO, GLFW.GLFW_KEY_Z, GLFW.GLFW_KEY_LEFT_CONTROL);
    public static final KeyCombo COPY = new KeyCombo("copy", UIKeys.GENERAL_COPY, GLFW.GLFW_KEY_C, GLFW.GLFW_KEY_LEFT_CONTROL);
    public static final KeyCombo PASTE = new KeyCombo("paste", UIKeys.GENERAL_PASTE, GLFW.GLFW_KEY_V, GLFW.GLFW_KEY_LEFT_CONTROL);
    public static final KeyCombo PRESETS = new KeyCombo("presets", UIKeys.GENERAL_PRESETS, GLFW.GLFW_KEY_V, GLFW.GLFW_KEY_LEFT_SHIFT, GLFW.GLFW_KEY_LEFT_CONTROL);
    public static final KeyCombo SAVE = new KeyCombo("save", UIKeys.GENERAL_SAVE, GLFW.GLFW_KEY_S, GLFW.GLFW_KEY_LEFT_CONTROL);
    public static final KeyCombo DELETE = new KeyCombo("delete", UIKeys.GENERAL_REMOVE, GLFW.GLFW_KEY_DELETE);
    public static final KeyCombo CONFIRM = new KeyCombo("confirm", UIKeys.GENERAL_CONFIRM, GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_LEFT_CONTROL);
    public static final KeyCombo CLOSE = new KeyCombo("close", UIKeys.GENERAL_CLOSE, GLFW.GLFW_KEY_ESCAPE);

    /* Camera editor */
    public static final KeyCombo ADD_AT_CURSOR = new KeyCombo("add_at_cursor", UIKeys.CAMERA_TIMELINE_CONTEXT_ADD_AT_CURSOR, GLFW.GLFW_KEY_A, GLFW.GLFW_KEY_LEFT_SHIFT).categoryKey("camera");
    public static final KeyCombo ADD_AT_TICK = new KeyCombo("add_at_tick", UIKeys.CAMERA_TIMELINE_CONTEXT_ADD_AT_TICK, GLFW.GLFW_KEY_A, GLFW.GLFW_KEY_LEFT_ALT).categoryKey("camera");
    public static final KeyCombo ADD_ON_TOP = new KeyCombo("add_on_top", UIKeys.CAMERA_TIMELINE_CONTEXT_ADD_ON_TOP, GLFW.GLFW_KEY_A, GLFW.GLFW_KEY_LEFT_SHIFT, GLFW.GLFW_KEY_LEFT_ALT).categoryKey("camera");
    public static final KeyCombo CLIP_CUT = new KeyCombo("clip_cut", UIKeys.CAMERA_TIMELINE_CONTEXT_CUT, GLFW.GLFW_KEY_C, GLFW.GLFW_KEY_LEFT_ALT).categoryKey("camera");
    public static final KeyCombo CLIP_DURATION = new KeyCombo("clip_duration", UIKeys.CAMERA_TIMELINE_CONTEXT_SHIFT_DURATION, GLFW.GLFW_KEY_M).categoryKey("camera");
    public static final KeyCombo CLIP_ENABLE = new KeyCombo("clip_enable", UIKeys.CAMERA_TIMELINE_KEYS_ENABLED, GLFW.GLFW_KEY_J).categoryKey("camera");
    public static final KeyCombo CLIP_SHIFT = new KeyCombo("clip_shift", UIKeys.CAMERA_TIMELINE_CONTEXT_SHIFT, GLFW.GLFW_KEY_S, GLFW.GLFW_KEY_LEFT_SHIFT).categoryKey("camera");
    public static final KeyCombo CLIP_SELECT_AFTER = new KeyCombo("clip_select_after", UIKeys.CAMERA_EDITOR_KEYS_EDITOR_SELECT_AFTER, GLFW.GLFW_KEY_PERIOD, GLFW.GLFW_KEY_LEFT_CONTROL).categoryKey("camera");
    public static final KeyCombo CLIP_SELECT_BEFORE = new KeyCombo("clip_select_before", UIKeys.CAMERA_EDITOR_KEYS_EDITOR_SELECT_BEFORE, GLFW.GLFW_KEY_COMMA, GLFW.GLFW_KEY_LEFT_CONTROL).categoryKey("camera");
    public static final KeyCombo FLIGHT = new KeyCombo("flight", UIKeys.CAMERA_EDITOR_KEYS_MODES_FLIGHT, GLFW.GLFW_KEY_F).categoryKey("camera");
    public static final KeyCombo LOOPING = new KeyCombo("looping", UIKeys.CAMERA_EDITOR_KEYS_MODES_LOOPING, GLFW.GLFW_KEY_L).categoryKey("camera");
    public static final KeyCombo LOOPING_SET_MAX = new KeyCombo("looping_set_max", UIKeys.CAMERA_EDITOR_KEYS_LOOPING_SET_MAX, GLFW.GLFW_KEY_RIGHT_BRACKET).categoryKey("camera");
    public static final KeyCombo LOOPING_SET_MIN = new KeyCombo("looping_set_min", UIKeys.CAMERA_EDITOR_KEYS_LOOPING_SET_MIN, GLFW.GLFW_KEY_LEFT_BRACKET).categoryKey("camera");
    public static final KeyCombo NEXT_CLIP = new KeyCombo("next_clip", UIKeys.CAMERA_EDITOR_KEYS_EDITOR_NEXT_CLIP, GLFW.GLFW_KEY_RIGHT, GLFW.GLFW_KEY_LEFT_SHIFT).repeatable().categoryKey("camera");
    public static final KeyCombo PREV_CLIP = new KeyCombo("prev_clip", UIKeys.CAMERA_EDITOR_KEYS_EDITOR_PREV_CLIP, GLFW.GLFW_KEY_LEFT, GLFW.GLFW_KEY_LEFT_SHIFT).repeatable().categoryKey("camera");
    public static final KeyCombo JUMP_FORWARD = new KeyCombo("jump_forward", UIKeys.CAMERA_EDITOR_KEYS_EDITOR_JUMP_FORWARD, GLFW.GLFW_KEY_UP).repeatable().categoryKey("camera");
    public static final KeyCombo JUMP_BACKWARD = new KeyCombo("jump_backward", UIKeys.CAMERA_EDITOR_KEYS_EDITOR_JUMP_BACKWARD, GLFW.GLFW_KEY_DOWN).repeatable().categoryKey("camera");
    public static final KeyCombo FADE_IN = new KeyCombo("fade_in", UIKeys.CAMERA_EDITOR_KEYS_EDITOR_FADE_IN, GLFW.GLFW_KEY_COMMA).categoryKey("camera");
    public static final KeyCombo FADE_OUT = new KeyCombo("fade_out", UIKeys.CAMERA_EDITOR_KEYS_EDITOR_FADE_OUT, GLFW.GLFW_KEY_PERIOD).categoryKey("camera");

    /* Flight mode keybinds */
    public static final KeyCombo FLIGHT_FORWARD = new KeyCombo("flight_forward", UIKeys.CAMERA_FLIGHT_FLIGHT_FORWARD, GLFW.GLFW_KEY_W).categoryKey("flight");
    public static final KeyCombo FLIGHT_BACKWARD = new KeyCombo("flight_backward", UIKeys.CAMERA_FLIGHT_FLIGHT_BACKWARD, GLFW.GLFW_KEY_S).categoryKey("flight");
    public static final KeyCombo FLIGHT_LEFT = new KeyCombo("flight_left", UIKeys.CAMERA_FLIGHT_FLIGHT_LEFT, GLFW.GLFW_KEY_A).categoryKey("flight");
    public static final KeyCombo FLIGHT_RIGHT = new KeyCombo("flight_right", UIKeys.CAMERA_FLIGHT_FLIGHT_RIGHT, GLFW.GLFW_KEY_D).categoryKey("flight");
    public static final KeyCombo FLIGHT_UP = new KeyCombo("flight_up", UIKeys.CAMERA_FLIGHT_FLIGHT_UP, GLFW.GLFW_KEY_SPACE).categoryKey("flight");
    public static final KeyCombo FLIGHT_DOWN = new KeyCombo("flight_down", UIKeys.CAMERA_FLIGHT_FLIGHT_DOWN, GLFW.GLFW_KEY_LEFT_SHIFT).categoryKey("flight");
    public static final KeyCombo FLIGHT_TILT_UP = new KeyCombo("flight_tilt_up", UIKeys.CAMERA_FLIGHT_FLIGHT_TILT_UP, GLFW.GLFW_KEY_UP).categoryKey("flight");
    public static final KeyCombo FLIGHT_TILT_DOWN = new KeyCombo("flight_tilt_down", UIKeys.CAMERA_FLIGHT_FLIGHT_TILT_DOWN, GLFW.GLFW_KEY_DOWN).categoryKey("flight");
    public static final KeyCombo FLIGHT_PAN_LEFT = new KeyCombo("flight_pan_left", UIKeys.CAMERA_FLIGHT_FLIGHT_PAN_LEFT, GLFW.GLFW_KEY_LEFT).categoryKey("flight");
    public static final KeyCombo FLIGHT_PAN_RIGHT = new KeyCombo("flight_pan_right", UIKeys.CAMERA_FLIGHT_FLIGHT_PAN_RIGHT, GLFW.GLFW_KEY_RIGHT).categoryKey("flight");
    public static final KeyCombo FLIGHT_ORBIT = new KeyCombo("flight_orbit", UIKeys.CAMERA_FLIGHT_FLIGHT_ORBIT, GLFW.GLFW_KEY_Z).categoryKey("flight");

    /* Dashboard */
    public static final KeyCombo OPEN_UTILITY_PANEL = new KeyCombo("utility_panel", UIKeys.UTILITY_TITLE, GLFW.GLFW_KEY_F6).categoryKey("dashboard");
    public static final KeyCombo OPEN_DATA_MANAGER = new KeyCombo("data_manager", UIKeys.PANELS_KEYS_OPEN_DATA_MANAGER, GLFW.GLFW_KEY_N).categoryKey("dashboard");
    public static final KeyCombo TOGGLE_VISIBILITY = new KeyCombo("toggle", UIKeys.DASHBOARD_CONTEXT_TOGGLE_VISIBILITY, GLFW.GLFW_KEY_F1).categoryKey("dashboard");

    /* Forms */
    public static final KeyCombo FORMS_FOCUS = new KeyCombo("focus", UIKeys.FORMS_LIST_CONTEXT_FOCUS, GLFW.GLFW_KEY_F, GLFW.GLFW_KEY_LEFT_CONTROL).categoryKey("forms");
    public static final KeyCombo FORMS_PICK = new KeyCombo("pick", UIKeys.GENERAL_PICK, GLFW.GLFW_KEY_P).categoryKey("forms");
    public static final KeyCombo FORMS_EDIT = new KeyCombo("edit", UIKeys.GENERAL_EDIT, GLFW.GLFW_KEY_E).categoryKey("forms");
    public static final KeyCombo FORMS_PICK_ALT = new KeyCombo("pick_alt", UIKeys.GENERAL_PICK, GLFW.GLFW_KEY_P, GLFW.GLFW_KEY_LEFT_SHIFT).categoryKey("forms");
    public static final KeyCombo FORMS_EDIT_ALT = new KeyCombo("edit_alt", UIKeys.GENERAL_EDIT, GLFW.GLFW_KEY_E, GLFW.GLFW_KEY_LEFT_SHIFT).categoryKey("forms");
    public static final KeyCombo FORMS_PICK_TEXTURE = new KeyCombo("pick_texture", UIKeys.FORMS_EDITOR_MODEL_PICK_TEXTURE, GLFW.GLFW_KEY_P, GLFW.GLFW_KEY_LEFT_SHIFT).categoryKey("forms");
    public static final KeyCombo FORMS_OPEN_STATES_EDITOR = new KeyCombo("open_states_editor", UIKeys.FORMS_EDITOR_STATES_OPEN, GLFW.GLFW_KEY_BACKSLASH).categoryKey("forms");

    /* Pixel editor */
    public static final KeyCombo PIXEL_SWAP = new KeyCombo("swap", UIKeys.TEXTURES_KEYS_SWAP, GLFW.GLFW_KEY_X).categoryKey("pixels");
    public static final KeyCombo PIXEL_PICK = new KeyCombo("pick", UIKeys.TEXTURES_KEYS_PICK, GLFW.GLFW_KEY_R).categoryKey("pixels");
    public static final KeyCombo PIXEL_FILL = new KeyCombo("fill", UIKeys.TEXTURES_KEYS_FILL, GLFW.GLFW_KEY_B, GLFW.GLFW_KEY_LEFT_CONTROL).categoryKey("pixels");

    /* Keyframes */
    public static final KeyCombo KEYFRAMES_MAXIMIZE = new KeyCombo("maximize", UIKeys.KEYFRAMES_CONTEXT_MAXIMIZE, GLFW.GLFW_KEY_HOME).categoryKey("keyframes");
    public static final KeyCombo KEYFRAMES_SELECT_ALL = new KeyCombo("select_all", UIKeys.KEYFRAMES_CONTEXT_SELECT_ALL, GLFW.GLFW_KEY_A, GLFW.GLFW_KEY_LEFT_CONTROL).categoryKey("keyframes");
    public static final KeyCombo KEYFRAMES_INTERP = new KeyCombo("interp", UIKeys.KEYFRAMES_KEYS_TOGGLE_INTERP, GLFW.GLFW_KEY_T, GLFW.GLFW_KEY_LEFT_SHIFT).categoryKey("keyframes");
    public static final KeyCombo KEYFRAMES_SELECT_LEFT = new KeyCombo("select_left", UIKeys.KEYFRAMES_KEYS_SELECT_LEFT, GLFW.GLFW_KEY_COMMA, GLFW.GLFW_KEY_LEFT_CONTROL).categoryKey("keyframes");
    public static final KeyCombo KEYFRAMES_SELECT_RIGHT = new KeyCombo("select_right", UIKeys.KEYFRAMES_KEYS_SELECT_RIGHT, GLFW.GLFW_KEY_PERIOD, GLFW.GLFW_KEY_LEFT_CONTROL).categoryKey("keyframes");
    public static final KeyCombo KEYFRAMES_SELECT_SAME = new KeyCombo("select_same", UIKeys.KEYFRAMES_KEYS_SELECT_SAME, GLFW.GLFW_KEY_L).categoryKey("keyframes");
    public static final KeyCombo KEYFRAMES_SCALE_TIME = new KeyCombo("scale_time", UIKeys.KEYFRAMES_KEYS_SCALE_TIME, GLFW.GLFW_KEY_V).categoryKey("keyframes");
    public static final KeyCombo KEYFRAMES_STACK_KEYFRAMES = new KeyCombo("stack_keyframes", UIKeys.KEYFRAMES_KEYS_STACK_KEYFRAMES, GLFW.GLFW_KEY_B).categoryKey("keyframes");
    public static final KeyCombo KEYFRAMES_SELECT_PREV = new KeyCombo("select_prev", UIKeys.KEYFRAMES_KEYS_SELECT_PREV, GLFW.GLFW_KEY_LEFT_BRACKET).repeatable().categoryKey("keyframes");
    public static final KeyCombo KEYFRAMES_SELECT_NEXT = new KeyCombo("select_next", UIKeys.KEYFRAMES_KEYS_SELECT_NEXT, GLFW.GLFW_KEY_RIGHT_BRACKET).repeatable().categoryKey("keyframes");
    public static final KeyCombo KEYFRAMES_SPREAD = new KeyCombo("spread", UIKeys.KEYFRAMES_CONTEXT_SPREAD, GLFW.GLFW_KEY_B, GLFW.GLFW_KEY_LEFT_ALT).categoryKey("keyframes");
    public static final KeyCombo KEYFRAMES_ADJUST_VALUES = new KeyCombo("adjust_values", UIKeys.KEYFRAMES_CONTEXT_ADJUST_VALUES, GLFW.GLFW_KEY_N, GLFW.GLFW_KEY_LEFT_SHIFT).categoryKey("keyframes");

    /* World menu */
    public static final KeyCombo CYCLE_PANELS = new KeyCombo("cycle_panels", UIKeys.WORLD_KEYS_CYCLE_PANELS, GLFW.GLFW_KEY_TAB).categoryKey("world");

    /* Transformations */
    public static final KeyCombo TRANSFORMATIONS_TRANSLATE = new KeyCombo("translate", UIKeys.TRANSFORMS_TRANSLATE, GLFW.GLFW_KEY_G).categoryKey("transformations");
    public static final KeyCombo TRANSFORMATIONS_SCALE = new KeyCombo("scale", UIKeys.TRANSFORMS_SCALE, GLFW.GLFW_KEY_S).categoryKey("transformations");
    public static final KeyCombo TRANSFORMATIONS_ROTATE = new KeyCombo("rotate", UIKeys.TRANSFORMS_ROTATE, GLFW.GLFW_KEY_R).categoryKey("transformations");
    public static final KeyCombo TRANSFORMATIONS_X = new KeyCombo("x", UIKeys.GENERAL_X, GLFW.GLFW_KEY_X).categoryKey("transformations");
    public static final KeyCombo TRANSFORMATIONS_Y = new KeyCombo("y", UIKeys.GENERAL_Y, GLFW.GLFW_KEY_Y).categoryKey("transformations");
    public static final KeyCombo TRANSFORMATIONS_Z = new KeyCombo("z", UIKeys.GENERAL_Z, GLFW.GLFW_KEY_Z).categoryKey("transformations");
    public static final KeyCombo TRANSFORMATIONS_TOGGLE_AXES = new KeyCombo("toggle_axes", UIKeys.TRANSFORMS_KEYS_TOGGLE_AXES, GLFW.GLFW_KEY_F8).categoryKey("transformations");

    /* Film controller */
    public static final KeyCombo FILM_CONTROLLER_START_RECORDING = new KeyCombo("start_recording", UIKeys.FILM_CONTROLLER_KEYS_START_RECORDING, GLFW.GLFW_KEY_R, GLFW.GLFW_KEY_LEFT_CONTROL).categoryKey("film_controller");
    public static final KeyCombo FILM_CONTROLLER_INSERT_FRAME = new KeyCombo("insert_frame", UIKeys.FILM_CONTROLLER_KEYS_INSERT_FRAME, GLFW.GLFW_KEY_I).categoryKey("film_controller");
    public static final KeyCombo FILM_CONTROLLER_TOGGLE_CONTROL = new KeyCombo("toggle_control", UIKeys.FILM_CONTROLLER_KEYS_TOGGLE_CONTROL, GLFW.GLFW_KEY_H).categoryKey("film_controller");
    public static final KeyCombo FILM_CONTROLLER_TOGGLE_ORBIT_MODE = new KeyCombo("toggle_orbit_mode", UIKeys.FILM_CONTROLLER_KEYS_CHANGE_CAMERA_MODE, GLFW.GLFW_KEY_F3).categoryKey("film_controller");
    public static final KeyCombo FILM_CONTROLLER_MOVE_REPLAY_TO_CURSOR = new KeyCombo("move_replay_to_cursor", UIKeys.FILM_CONTROLLER_KEYS_MOVE_REPLAY_TO_CURSOR, GLFW.GLFW_KEY_G, GLFW.GLFW_KEY_LEFT_CONTROL).categoryKey("film_controller");
    public static final KeyCombo FILM_CONTROLLER_RESTART_ACTIONS = new KeyCombo("restart_actions", UIKeys.FILM_CONTROLLER_KEYS_RESTART_ACTIONS, GLFW.GLFW_KEY_R, GLFW.GLFW_KEY_LEFT_ALT).categoryKey("film_controller");
    public static final KeyCombo FILM_CONTROLLER_TOGGLE_ONION_SKIN = new KeyCombo("toggle_onion_skin", UIKeys.FILM_CONTROLLER_ONION_SKIN_KEYS_TOGGLE, GLFW.GLFW_KEY_O).categoryKey("film_controller");
    public static final KeyCombo FILM_CONTROLLER_OPEN_REPLAYS = new KeyCombo("toggle_replays", UIKeys.FILM_CONTROLLER_KEYS_OPEN_REPLAYS, GLFW.GLFW_KEY_RIGHT_SHIFT).categoryKey("film_controller");
    public static final KeyCombo FILM_CONTROLLER_CYCLE_EDITORS = new KeyCombo("cycle_editors", UIKeys.FILM_CONTROLLER_KEYS_CYCLE_EDITORS, GLFW.GLFW_KEY_GRAVE_ACCENT).categoryKey("film_controller");
    public static final KeyCombo FILM_CONTROLLER_PREV_REPLAY = new KeyCombo("prev_replay", UIKeys.FILM_CONTROLLER_KEYS_PREV_REPLAY, GLFW.GLFW_KEY_PAGE_UP).categoryKey("film_controller");
    public static final KeyCombo FILM_CONTROLLER_NEXT_REPLAY = new KeyCombo("next_replay", UIKeys.FILM_CONTROLLER_KEYS_NEXT_REPLAY, GLFW.GLFW_KEY_PAGE_DOWN).categoryKey("film_controller");

    /* Recording groups */
    public static final KeyCombo RECORDING_GROUP_ALL = new KeyCombo("all", UIKeys.FILM_GROUPS_ALL, GLFW.GLFW_KEY_1).categoryKey("recording_groups");
    public static final KeyCombo RECORDING_GROUP_LEFT_STICK = new KeyCombo("left_stick", UIKeys.FILM_GROUPS_LEFT_STICK, GLFW.GLFW_KEY_2).categoryKey("recording_groups");
    public static final KeyCombo RECORDING_GROUP_RIGHT_STICK = new KeyCombo("right_stick", UIKeys.FILM_GROUPS_RIGHT_STICK, GLFW.GLFW_KEY_3).categoryKey("recording_groups");
    public static final KeyCombo RECORDING_GROUP_TRIGGERS = new KeyCombo("triggers", UIKeys.FILM_GROUPS_TRIGGERS, GLFW.GLFW_KEY_4).categoryKey("recording_groups");
    public static final KeyCombo RECORDING_GROUP_EXTRA_1 = new KeyCombo("extra_1", UIKeys.FILM_GROUPS_EXTRA_1, GLFW.GLFW_KEY_5).categoryKey("recording_groups");
    public static final KeyCombo RECORDING_GROUP_EXTRA_2 = new KeyCombo("extra_2", UIKeys.FILM_GROUPS_EXTRA_2, GLFW.GLFW_KEY_6).categoryKey("recording_groups");
    public static final KeyCombo RECORDING_GROUP_ONLY_POSITION = new KeyCombo("only_position", UIKeys.FILM_GROUPS_ONLY_POSITION, GLFW.GLFW_KEY_7).categoryKey("recording_groups");
    public static final KeyCombo RECORDING_GROUP_ONLY_ROTATION = new KeyCombo("only_rotation", UIKeys.FILM_GROUPS_ONLY_ROTATION, GLFW.GLFW_KEY_8).categoryKey("recording_groups");
    public static final KeyCombo RECORDING_GROUP_POS_ROT = new KeyCombo("pos_rot", UIKeys.FILM_GROUPS_ONLY_POS_ROT, GLFW.GLFW_KEY_9).categoryKey("recording_groups");
    public static final KeyCombo RECORDING_GROUP_OUTSIDE = new KeyCombo("outside", UIKeys.FILM_GROUPS_OUTSIDE, GLFW.GLFW_KEY_R).categoryKey("recording_groups");

    /* Model block editor */
    public static final KeyCombo MODEL_BLOCKS_MOVE_TO = new KeyCombo("move_to", UIKeys.MODEL_BLOCKS_KEYS_MOVE_TO, GLFW.GLFW_KEY_G, GLFW.GLFW_KEY_LEFT_CONTROL).categoryKey("model_blocks");
    public static final KeyCombo MODEL_BLOCKS_TOGGLE_RENDERING = new KeyCombo("toggle_rendering", UIKeys.MODEL_BLOCKS_KEYS_TOGGLE_RENDERING, GLFW.GLFW_KEY_F7).categoryKey("model_blocks");
    public static final KeyCombo MODEL_BLOCKS_TELEPORT = new KeyCombo("teleport", UIKeys.MODEL_BLOCKS_KEYS_TELEPORT, GLFW.GLFW_KEY_T, GLFW.GLFW_KEY_LEFT_CONTROL).categoryKey("model_blocks");

    /* Texture picker */
    public static final KeyCombo TEXTURE_PICKER_FIND = new KeyCombo("find", UIKeys.TEXTURE_KEYS_FIND_ALL, GLFW.GLFW_KEY_F, GLFW.GLFW_KEY_LEFT_CONTROL).categoryKey("texture_picker");
}