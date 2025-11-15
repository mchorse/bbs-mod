package mchorse.bbs_mod.ui.utils.icons;

import mchorse.bbs_mod.resources.Link;

import java.util.HashMap;
import java.util.Map;

public class Icons
{
    public static final Map<String, Icon> ICONS = new HashMap<>();
    public static final Link ATLAS = Link.assets("textures/icons.png");

    public static final Icon NONE = new Icon(null, "none", 0, 0);

    public static final Icon GEAR = register(new Icon(ATLAS, "gear", 0, 0));
    public static final Icon MORE = register(new Icon(ATLAS, "more", 16, 0));
    public static final Icon SAVED = register(new Icon(ATLAS, "saved", 32, 0));
    public static final Icon SAVE = register(new Icon(ATLAS, "save", 48, 0));
    public static final Icon ADD = register(new Icon(ATLAS, "add", 64, 0));
    public static final Icon DUPE = register(new Icon(ATLAS, "dupe", 80, 0));
    public static final Icon REMOVE = register(new Icon(ATLAS, "remove", 96, 0));
    public static final Icon POSE = register(new Icon(ATLAS, "pose", 112, 0));
    public static final Icon FILTER = register(new Icon(ATLAS, "filter", 128, 0));
    public static final Icon MOVE_UP = register(new Icon(ATLAS, "move_up", 144, 0, 16, 8));
    public static final Icon MOVE_DOWN = register(new Icon(ATLAS, "move_down", 144, 8, 16, 8));
    public static final Icon LOCKED = register(new Icon(ATLAS, "locked", 160, 0));
    public static final Icon UNLOCKED = register(new Icon(ATLAS, "unlocked", 176, 0));
    public static final Icon COPY = register(new Icon(ATLAS, "copy", 192, 0));
    public static final Icon PASTE = register(new Icon(ATLAS, "paste", 208, 0));
    public static final Icon CUT = register(new Icon(ATLAS, "cut", 224, 0));
    public static final Icon REFRESH = register(new Icon(ATLAS, "refresh", 240, 0));

    public static final Icon DOWNLOAD = register(new Icon(ATLAS, "download", 0, 16));
    public static final Icon UPLOAD = register(new Icon(ATLAS, "upload", 16, 16));
    public static final Icon SERVER = register(new Icon(ATLAS, "server", 32, 16));
    public static final Icon FOLDER = register(new Icon(ATLAS, "folder", 48, 16));
    public static final Icon IMAGE = register(new Icon(ATLAS, "image", 64, 16));
    public static final Icon EDIT = register(new Icon(ATLAS, "edit", 80, 16));
    public static final Icon MATERIAL = register(new Icon(ATLAS, "material", 96, 16));
    public static final Icon CLOSE = register(new Icon(ATLAS, "close", 112, 16));
    public static final Icon LIMB = register(new Icon(ATLAS, "limb", 128, 16));
    public static final Icon CODE = register(new Icon(ATLAS, "code", 144, 16));
    public static final Icon MOVE_LEFT = register(new Icon(ATLAS, "move_left", 146, 16, 6, 16));
    public static final Icon MOVE_RIGHT = register(new Icon(ATLAS, "move_right", 152, 16, 6, 16));
    public static final Icon HELP = register(new Icon(ATLAS, "help", 160, 16));
    public static final Icon LEFT_HANDLE = register(new Icon(ATLAS, "left_handle", 176, 16));
    public static final Icon MAIN_HANDLE = register(new Icon(ATLAS, "main_handle", 192, 16));
    public static final Icon RIGHT_HANDLE = register(new Icon(ATLAS, "right_handle", 208, 16));
    public static final Icon REVERSE = register(new Icon(ATLAS, "reverse", 224, 16));
    public static final Icon BLOCK = register(new Icon(ATLAS, "block", 240, 16));

    public static final Icon FAVORITE = register(new Icon(ATLAS, "favorite", 0, 32));
    public static final Icon VISIBLE = register(new Icon(ATLAS, "visible", 16, 32));
    public static final Icon INVISIBLE = register(new Icon(ATLAS, "invisible", 32, 32));
    public static final Icon PLAY = register(new Icon(ATLAS, "play", 48, 32));
    public static final Icon PAUSE = register(new Icon(ATLAS, "pause", 64, 32));
    public static final Icon MAXIMIZE = register(new Icon(ATLAS, "maximize", 80, 32));
    public static final Icon MINIMIZE = register(new Icon(ATLAS, "minimize", 96, 32));
    public static final Icon STOP = register(new Icon(ATLAS, "stop", 112, 32));
    public static final Icon FULLSCREEN = register(new Icon(ATLAS, "fullscreen", 128, 32));
    public static final Icon ALL_DIRECTIONS = register(new Icon(ATLAS, "all_directions", 144, 32));
    public static final Icon SPHERE = register(new Icon(ATLAS, "sphere", 160, 32));
    public static final Icon SHIFT_TO = register(new Icon(ATLAS, "shift_to", 176, 32));
    public static final Icon SHIFT_FORWARD = register(new Icon(ATLAS, "shift_forward", 192, 32));
    public static final Icon SHIFT_BACKWARD = register(new Icon(ATLAS, "shift_backward", 208, 32));
    public static final Icon MOVE_TO = register(new Icon(ATLAS, "move_to", 224, 32));
    public static final Icon GRAPH = register(new Icon(ATLAS, "graph", 240, 32));

    public static final Icon WRENCH = register(new Icon(ATLAS, "wrench", 0, 48));
    public static final Icon EXCLAMATION = register(new Icon(ATLAS, "exclamation", 16, 48));
    public static final Icon LEFTLOAD = register(new Icon(ATLAS, "leftload", 32, 48));
    public static final Icon RIGHTLOAD = register(new Icon(ATLAS, "rightload", 48, 48));
    public static final Icon BUBBLE = register(new Icon(ATLAS, "bubble", 64, 48));
    public static final Icon FILE = register(new Icon(ATLAS, "file", 80, 48));
    public static final Icon PROCESSOR = register(new Icon(ATLAS, "processor", 96, 48));
    public static final Icon MAZE = register(new Icon(ATLAS, "maze", 112, 48));
    public static final Icon BOOKMARK = register(new Icon(ATLAS, "bookmark", 128, 48));
    public static final Icon SOUND = register(new Icon(ATLAS, "sound", 144, 48));
    public static final Icon SEARCH = register(new Icon(ATLAS, "search", 160, 48));
    public static final Icon CYLINDER = register(new Icon(ATLAS, "cylinder", 176, 48));
    public static final Icon LINE = register(new Icon(ATLAS, "line", 192, 48));
    public static final Icon REDO = register(new Icon(ATLAS, "redo", 208, 48));
    public static final Icon UNDO = register(new Icon(ATLAS, "undo", 224, 48));
    public static final Icon CONSOLE = register(new Icon(ATLAS, "console", 240, 48));

    public static final Icon IN = register(new Icon(ATLAS, "in", 0, 64));
    public static final Icon OUT = register(new Icon(ATLAS, "out", 16, 64));
    public static final Icon PROPERTIES = register(new Icon(ATLAS, "properties", 32, 64));
    public static final Icon FONT = register(new Icon(ATLAS, "font", 48, 64));
    public static final Icon FRUSTUM = register(new Icon(ATLAS, "frustum", 64, 64));
    public static final Icon FRAME_NEXT = register(new Icon(ATLAS, "frame_next", 80, 64));
    public static final Icon FRAME_PREV = register(new Icon(ATLAS, "frame_prev", 96, 64));
    public static final Icon FORWARD = register(new Icon(ATLAS, "forward", 112, 64));
    public static final Icon BACKWARD = register(new Icon(ATLAS, "backward", 128, 64));
    public static final Icon PLANE = register(new Icon(ATLAS, "plane", 144, 64));
    public static final Icon HELICOPTER = register(new Icon(ATLAS, "helicopter", 160, 64));
    public static final Icon ORBIT = register(new Icon(ATLAS, "orbit", 176, 64));
    public static final Icon CURVES = register(new Icon(ATLAS, "curves", 192, 64));
    public static final Icon ENVELOPE = register(new Icon(ATLAS, "envelope", 208, 64));
    public static final Icon PLAYER = register(new Icon(ATLAS, "player", 224, 64));
    public static final Icon TRASH = register(new Icon(ATLAS, "trash", 240, 64));

    public static final Icon YOUTUBE = register(new Icon(ATLAS, "youtube", 0, 80));
    public static final Icon TWITTER = register(new Icon(ATLAS, "twitter", 16, 80));
    public static final Icon CHICKEN = register(new Icon(ATLAS, "chicken", 32, 80));
    public static final Icon SPRAY = register(new Icon(ATLAS, "spray", 48, 80));
    public static final Icon BUCKET = register(new Icon(ATLAS, "bucket", 64, 80));
    public static final Icon TREE = register(new Icon(ATLAS, "tree", 80, 80));
    public static final Icon CROPS = register(new Icon(ATLAS, "crops", 96, 80));
    public static final Icon SLAB = register(new Icon(ATLAS, "slab", 112, 80));
    public static final Icon STAIR = register(new Icon(ATLAS, "stair", 128, 80));
    public static final Icon GLOBE = register(new Icon(ATLAS, "globe", 144, 80));
    public static final Icon BULLET = register(new Icon(ATLAS, "bullet", 160, 80));
    public static final Icon PARTICLE = register(new Icon(ATLAS, "particle", 176, 80));
    public static final Icon SCENE = register(new Icon(ATLAS, "scene", 192, 80));
    public static final Icon EDITOR = register(new Icon(ATLAS, "editor", 208, 80));
    public static final Icon LOOKING = register(new Icon(ATLAS, "looking", 224, 80));
    public static final Icon EXTERNAL = register(new Icon(ATLAS, "external", 240, 80));

    public static final Icon FILM = register(new Icon(ATLAS, "film", 0, 96));
    public static final Icon OUTLINE = register(new Icon(ATLAS, "outline", 16, 96));
    public static final Icon BRICKS = register(new Icon(ATLAS, "bricks", 32, 96));
    public static final Icon CONVERT = register(new Icon(ATLAS, "convert", 48, 96));
    public static final Icon JOYSTICK = register(new Icon(ATLAS, "joystick", 64, 96));
    public static final Icon CUP = register(new Icon(ATLAS, "cup", 80, 96));
    public static final Icon CHECKMARK = register(new Icon(ATLAS, "checkmark", 96, 96));
    public static final Icon STRUCTURE = register(new Icon(ATLAS, "structure", 112, 96));
    public static final Icon ARC = register(new Icon(ATLAS, "arc", 128, 96));
    public static final Icon LIST = register(new Icon(ATLAS, "list", 144, 96));
    public static final Icon SETTINGS = register(new Icon(ATLAS, "settings", 160, 96));
    public static final Icon GALLERY = register(new Icon(ATLAS, "gallery", 176, 96));
    public static final Icon EXCHANGE = register(new Icon(ATLAS, "exchange", 192, 96));
    public static final Icon ARROW_UP = register(new Icon(ATLAS, "arrow_up", 208, 96));
    public static final Icon ARROW_DOWN = register(new Icon(ATLAS, "arrow_down", 224, 96));
    public static final Icon ARROW_RIGHT = register(new Icon(ATLAS, "arrow_right", 240, 96));

    public static final Icon ARROW_LEFT = register(new Icon(ATLAS, "arrow_left", 0, 112));
    public static final Icon HEART = register(new Icon(ATLAS, "heart", 16, 112));
    public static final Icon SHARD = register(new Icon(ATLAS, "shard", 32, 112));
    public static final Icon POINTER = register(new Icon(ATLAS, "pointer", 48, 112));
    public static final Icon SNOWFLAKE = register(new Icon(ATLAS, "snowflake", 64, 112));
    public static final Icon OUTLINE_SPHERE = register(new Icon(ATLAS, "outline_sphere", 80, 112));
    public static final Icon CAMERA = register(new Icon(ATLAS, "camera", 96, 112));
    public static final Icon FADING = register(new Icon(ATLAS, "fading", 112, 112));
    public static final Icon TIME = register(new Icon(ATLAS, "time", 128, 112));
    public static final Icon LIGHT = register(new Icon(ATLAS, "light", 144, 112));
    public static final Icon KEY_CAP = register(new Icon(ATLAS, "key_cap", 160, 112));
    public static final Icon LEFT_STICK = register(new Icon(ATLAS, "left_stick", 176, 112));
    public static final Icon RIGHT_STICK = register(new Icon(ATLAS, "right_stick", 192, 112));
    public static final Icon TRIGGER = register(new Icon(ATLAS, "trigger", 208, 112));
    public static final Icon KEY = register(new Icon(ATLAS, "key", 224, 112));
    public static final Icon VOICE = register(new Icon(ATLAS, "voice", 240, 112));

    public static final Icon TWITCH = register(new Icon(ATLAS, "twitch", 0, 128));
    public static final Icon BILIBILI = register(new Icon(ATLAS, "bilibili", 16, 128));
    public static final Icon TIKTOK = register(new Icon(ATLAS, "tiktok", 32, 128));
    public static final Icon LINK = register(new Icon(ATLAS, "link", 48, 128));
    public static final Icon USER = register(new Icon(ATLAS, "user", 64, 128));
    public static final Icon MORPH = register(new Icon(ATLAS, "morph", 80, 128));
    public static final Icon SCALE = register(new Icon(ATLAS, "scale", 96, 128));
    public static final Icon X = register(new Icon(ATLAS, "x", 112, 128));
    public static final Icon Y = register(new Icon(ATLAS, "y", 128, 128));
    public static final Icon Z = register(new Icon(ATLAS, "z", 144, 128));
    public static final Icon VIDEO_CAMERA = register(new Icon(ATLAS, "video_camera", 160, 128));
    public static final Icon COLOR = register(new Icon(ATLAS, "color", 176, 128));
    public static final Icon CLIP_HANLDE_LEFT = register(new Icon(ATLAS, "clip_hanlde_left", 192, 128, 8, 16));
    public static final Icon CLIP_HANLDE_RIGHT = register(new Icon(ATLAS, "clip_hanlde_right", 200, 128, 8, 16));
    public static final Icon ACTION = register(new Icon(ATLAS, "action", 208, 128));
    public static final Icon DROP = register(new Icon(ATLAS, "drop", 224, 128));
    public static final Icon ONION_SKIN = register(new Icon(ATLAS, "onion_skin", 240, 128));

    public static final Icon SUN = register(new Icon(ATLAS, "sun", 0, 144));
    public static final Icon STOPWATCH = register(new Icon(ATLAS, "stopwatch", 16, 144));
    public static final Icon HEART_ALT = register(new Icon(ATLAS, "heart_alt", 32, 144));
    public static final Icon SKULL = register(new Icon(ATLAS, "skull", 48, 144));

    public static final Icon INTERP_LINEAR = register(new Icon(ATLAS, "interp_linear", 0, 192));
    public static final Icon INTERP_CONST = register(new Icon(ATLAS, "interp_const", 16, 192));
    public static final Icon INTERP_STEP = register(new Icon(ATLAS, "interp_step", 32, 192));
    public static final Icon INTERP_QUAD_INOUT = register(new Icon(ATLAS, "interp_quad_inout", 48, 192));
    public static final Icon INTERP_CUBIC_INOUT = register(new Icon(ATLAS, "interp_cubic_inout", 64, 192));
    public static final Icon INTERP_QUART_INOUT = register(new Icon(ATLAS, "interp_quart_inout", 80, 192));
    public static final Icon INTERP_QUINT_INOUT = register(new Icon(ATLAS, "interp_quint_inout", 96, 192));
    public static final Icon INTERP_EXP_INOUT = register(new Icon(ATLAS, "interp_exp_inout", 112, 192));
    public static final Icon INTERP_BACK_INOUT = register(new Icon(ATLAS, "interp_back_inout", 128, 192));
    public static final Icon INTERP_ELASTIC_INOUT = register(new Icon(ATLAS, "interp_elastic_inout", 144, 192));
    public static final Icon INTERP_BOUNCE_INOUT = register(new Icon(ATLAS, "interp_bounce_inout", 160, 192));
    public static final Icon INTERP_SINE_INOUT = register(new Icon(ATLAS, "interp_sine_inout", 176, 192));
    public static final Icon INTERP_CIRCLE_INOUT = register(new Icon(ATLAS, "interp_circle_inout", 192, 192));
    public static final Icon INTERP_BEZIER = register(new Icon(ATLAS, "interp_bezier", 208, 192));

    public static final Icon INTERP_QUAD_OUT = register(new Icon(ATLAS, "interp_quad_out", 48, 208));
    public static final Icon INTERP_CUBIC_OUT = register(new Icon(ATLAS, "interp_cubic_out", 64, 208));
    public static final Icon INTERP_QUART_OUT = register(new Icon(ATLAS, "interp_quart_out", 80, 208));
    public static final Icon INTERP_QUINT_OUT = register(new Icon(ATLAS, "interp_quint_out", 96, 208));
    public static final Icon INTERP_EXP_OUT = register(new Icon(ATLAS, "interp_exp_out", 112, 208));
    public static final Icon INTERP_BACK_OUT = register(new Icon(ATLAS, "interp_back_out", 128, 208));
    public static final Icon INTERP_ELASTIC_OUT = register(new Icon(ATLAS, "interp_elastic_out", 144, 208));
    public static final Icon INTERP_BOUNCE_OUT = register(new Icon(ATLAS, "interp_bounce_out", 160, 208));
    public static final Icon INTERP_SINE_OUT = register(new Icon(ATLAS, "interp_sine_out", 176, 208));
    public static final Icon INTERP_CIRCLE_OUT = register(new Icon(ATLAS, "interp_circle_out", 192, 208));

    public static final Icon INTERP_QUAD_IN = register(new Icon(ATLAS, "interp_quad_in", 48, 224));
    public static final Icon INTERP_CUBIC_IN = register(new Icon(ATLAS, "interp_cubic_in", 64, 224));
    public static final Icon INTERP_QUART_IN = register(new Icon(ATLAS, "interp_quart_in", 80, 224));
    public static final Icon INTERP_QUINT_IN = register(new Icon(ATLAS, "interp_quint_in", 96, 224));
    public static final Icon INTERP_EXP_IN = register(new Icon(ATLAS, "interp_exp_in", 112, 224));
    public static final Icon INTERP_BACK_IN = register(new Icon(ATLAS, "interp_back_in", 128, 224));
    public static final Icon INTERP_ELASTIC_IN = register(new Icon(ATLAS, "interp_elastic_in", 144, 224));
    public static final Icon INTERP_BOUNCE_IN = register(new Icon(ATLAS, "interp_bounce_in", 160, 224));
    public static final Icon INTERP_SINE_IN = register(new Icon(ATLAS, "interp_sine_in", 176, 224));
    public static final Icon INTERP_CIRCLE_IN = register(new Icon(ATLAS, "interp_circle_in", 192, 224));

    public static final Icon CHECKBOARD = register(new Icon(ATLAS, "checkboard", 0, 240));
    public static final Icon DISABLED = register(new Icon(ATLAS, "disabled", 16, 240));
    public static final Icon CURSOR = register(new Icon(ATLAS, "cursor", 32, 240));

    public static final Icon MOUSE_BODY = new Icon(ATLAS, "mouse_body", 241, 237, 14, 18);
    public static final Icon MOUSE_LMB = new Icon(ATLAS, "mouse_lmb", 242, 229, 6, 7);
    public static final Icon MOUSE_RMB = new Icon(ATLAS, "mouse_rmb", 248, 229, 6, 7);
    public static final Icon KEY_CAP_LEFT = new Icon(ATLAS, "key_cap", 220, 236, 4, 20);
    public static final Icon KEY_CAP_RIGHT = new Icon(ATLAS, "key_cap", 236, 236, 4, 20);
    public static final Icon KEY_CAP_REPEATABLE = new Icon(ATLAS, "key_cap_repeatable", 224, 236, 12, 20);

    public static final Icon SHAPES = register(new Icon(ATLAS, "shapes", 66, 144, 12, 14));
    public static final Icon SQUARE = register(new Icon(ATLAS, "square", 83, 145, 12, 13));
    public static final Icon TRIANGLE = register(new Icon(ATLAS, "triangle", 98, 145, 13, 13));
    public static final Icon DIAMOND = register(new Icon(ATLAS, "diamond", 115, 145, 12, 13));
    public static final Icon CIRCLE = register(new Icon(ATLAS, "circle", 131, 147, 10, 10));
    public static final Icon TRI_STAR = register(new Icon(ATLAS, "tri_star", 161, 144, 13, 13));
    public static final Icon FOUR_STAR = register(new Icon(ATLAS, "four_star", 145, 144, 13, 13));
    public static final Icon FIVE_STAR = register(new Icon(ATLAS, "five_star", 177, 144, 14, 14));
    public static final Icon SIX_STAR = register(new Icon(ATLAS, "six_star", 194, 144, 12, 15));

    public static Icon register(Icon icon)
    {
        if (ICONS.containsKey(icon.id))
        {
            try
            {
                throw new IllegalStateException("[Icons] Icon " + icon.id + " was already registered prior...");
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
        else
        {
            ICONS.put(icon.id, icon);
        }

        return icon;
    }
}