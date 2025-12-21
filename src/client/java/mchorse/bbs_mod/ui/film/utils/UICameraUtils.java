package mchorse.bbs_mod.ui.film.utils;

import mchorse.bbs_mod.camera.data.Angle;
import mchorse.bbs_mod.camera.data.Point;
import mchorse.bbs_mod.camera.data.Position;
import mchorse.bbs_mod.camera.values.ValueAngle;
import mchorse.bbs_mod.camera.values.ValuePoint;
import mchorse.bbs_mod.camera.values.ValuePosition;
import mchorse.bbs_mod.graphics.window.Window;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.film.IUIClipsDelegate;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.utils.InterpolationUtils;
import mchorse.bbs_mod.ui.utils.context.ContextAction;
import mchorse.bbs_mod.ui.utils.context.ContextMenuManager;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.utils.colors.Colors;
import mchorse.bbs_mod.utils.interps.IInterp;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.StringJoiner;
import java.util.function.Consumer;

public class UICameraUtils
{
    public static final IKey KEYS_CATEGORY = UIKeys.INTERPOLATIONS_KEY_CATEGORY;

    /* Interpolations context menu */

    public static void interps(UIContext context, Collection<IInterp> values, IInterp current, Consumer<IInterp> consumer)
    {
        context.replaceContextMenu((menu) ->
        {
            for (IInterp interpolation : values)
            {
                ContextAction action = menu.action(Icons.ADD, InterpolationUtils.getName(interpolation), interpolation == current, () -> consumer.accept(interpolation));

                InterpolationUtils.setupKeybind(interpolation, action, KEYS_CATEGORY);
            }
        });
    }

    /* Position UX context menu */

    public static void positionContextMenu(ContextMenuManager menu, IUIClipsDelegate editor, ValuePosition value)
    {
        menu.action(Icons.COPY, UIKeys.CAMERA_PANELS_CONTEXT_COPY_POSITION, Colors.NEGATIVE, () ->
        {
            Map<String, Double> map = new LinkedHashMap<>();

            copyPoint(map, value.getPoint().get());
            copyAngle(map, value.getAngle().get());

            Window.setClipboard(mapToString(map));
        });

        menu.action(Icons.PASTE, UIKeys.CAMERA_PANELS_CONTEXT_PASTE_POSITION, () ->
        {
            Map<String, Double> map = stringToMap(Window.getClipboard());
            Position position = new Position();
            Point point = createPoint(map);
            Angle angle = createAngle(map);

            if (point != null && angle != null)
            {
                position.point.set(point);
                position.angle.set(angle);

                value.set(position);
                editor.fillData();
            }
        });

        pointContextMenu(menu, editor, value.getPoint());
        angleContextMenu(menu, editor, value.getAngle());
    }

    public static void pointContextMenu(ContextMenuManager menu, IUIClipsDelegate editor, ValuePoint value)
    {
        menu.action(Icons.COPY, UIKeys.CAMERA_PANELS_CONTEXT_COPY_POINT, Colors.POSITIVE, () ->
        {
            Map<String, Double> map = new LinkedHashMap<>();

            copyPoint(map, value.get());
            Window.setClipboard(mapToString(map));
        });

        menu.action(Icons.PASTE, UIKeys.CAMERA_PANELS_CONTEXT_PASTE_POINT, () ->
        {
            Point point = createPoint(stringToMap(Window.getClipboard()));

            if (point != null)
            {
                value.set(point);
                editor.fillData();
            }
        });
    }

    public static void copyPoint(Map<String, Double> map, Point point)
    {
        map.put("X", point.x);
        map.put("Y", point.y);
        map.put("Z", point.z);
    }

    public static Point createPoint(Map<String, Double> map)
    {
        if (map.containsKey("x") && map.containsKey("y") && map.containsKey("z"))
        {
            Point newPoint = new Point(0, 0, 0);

            if (map.containsKey("x")) newPoint.x = map.get("x");
            if (map.containsKey("y")) newPoint.y = map.get("y");
            if (map.containsKey("z")) newPoint.z = map.get("z");

            return newPoint;
        }

        return null;
    }

    public static void angleContextMenu(ContextMenuManager menu, IUIClipsDelegate editor, ValueAngle value)
    {
        menu.action(Icons.COPY, UIKeys.CAMERA_PANELS_CONTEXT_COPY_ANGLE, Colors.INACTIVE, () ->
        {
            Map<String, Double> map = new LinkedHashMap<>();

            copyAngle(map, value.get());
            Window.setClipboard(mapToString(map));
        });

        menu.action(Icons.PASTE, UIKeys.CAMERA_PANELS_CONTEXT_PASTE_ANGLE, () ->
        {
            Angle angle = createAngle(stringToMap(Window.getClipboard()));

            if (angle != null)
            {
                value.set(angle);
                editor.fillData();
            }
        });
    }

    public static void copyAngle(Map<String, Double> map, Angle angle)
    {
        map.put("Yaw", (double) angle.yaw);
        map.put("Pitch", (double) angle.pitch);
        map.put("Roll", (double) angle.roll);
        map.put("FOV", (double) angle.fov);
    }

    public static Angle createAngle(Map<String, Double> map)
    {
        if (map.containsKey("yaw") && map.containsKey("pitch"))
        {
            Angle newAngle = new Angle(0, 0);

            if (map.containsKey("yaw")) newAngle.yaw = map.get("yaw").floatValue();
            if (map.containsKey("pitch")) newAngle.pitch = map.get("pitch").floatValue();
            if (map.containsKey("roll")) newAngle.roll = map.get("roll").floatValue();
            if (map.containsKey("fov")) newAngle.fov = map.get("fov").floatValue();

            return newAngle;
        }

        return null;
    }

    public static String mapToString(Map<String, Double> data)
    {
        StringJoiner joiner = new StringJoiner("\n");

        for (String key : data.keySet())
        {
            joiner.add(key + ": " + data.get(key));
        }

        return joiner.toString();
    }

    public static Map<String, Double> stringToMap(String string)
    {
        Map<String, Double> map = new LinkedHashMap<>();

        for (String line : string.split("\n"))
        {
            String[] splits = line.split(":");

            if (splits.length == 2)
            {
                try
                {
                    map.put(splits[0].trim().toLowerCase(), Double.parseDouble(splits[1].trim()));
                }
                catch (Exception e)
                {}
            }
        }

        return map;
    }
}