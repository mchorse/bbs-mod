package mchorse.bbs_mod.forms;

import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.data.types.ListType;
import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.forms.forms.BodyPart;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.forms.states.AnimationState;
import mchorse.bbs_mod.settings.values.base.BaseValue;
import mchorse.bbs_mod.settings.values.base.BaseValueBasic;
import mchorse.bbs_mod.utils.CollectionUtils;
import mchorse.bbs_mod.utils.StringUtils;
import mchorse.bbs_mod.utils.keyframes.KeyframeChannel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class FormUtils
{
    public static final String PATH_SEPARATOR = "/";

    private static final List<String> path = new ArrayList<>();

    public static Form fromData(BaseType data)
    {
        if (data instanceof MapType map)
        {
            return fromData(map);
        }

        return null;
    }

    public static Form fromData(MapType data)
    {
        try
        {
            return data == null ? null : BBSMod.getForms().fromData(data);
        }
        catch (Exception e)
        {}

        return null;
    }

    public static MapType toData(Form form)
    {
        return form == null ? null : BBSMod.getForms().toData(form);
    }

    public static Form copy(Form form)
    {
        if (form != null)
        {
            FormArchitect forms = BBSMod.getForms();

            return forms.fromData(forms.toData(form));
        }

        return null;
    }

    public static Form getRoot(Form form)
    {
        while (form.getParent() != null)
        {
            form = form.getParentForm();
        }

        return form;
    }

    public static Form getForm(BaseValue property)
    {
        if (property.getParent() instanceof Form form)
        {
            return form;
        }

        return null;
    }

    public static Form getForm(Form form, String path)
    {
        String[] split = path.split(PATH_SEPARATOR);

        for (String s : split)
        {
            try
            {
                int index = Integer.parseInt(s);
                BodyPart safe = CollectionUtils.getSafe(form.parts.getAllTyped(), index);

                if (safe != null)
                {
                    form = safe.getForm();
                }
                else
                {
                    break;
                }
            }
            catch (Exception e)
            {
                break;
            }
        }

        return form;
    }

    public static String getPath(Form form)
    {
        if (form.getParent() == null)
        {
            return "";
        }

        path.clear();

        while (form != null)
        {
            Form parent = form.getParentForm();

            if (parent != null)
            {
                int i = 0;

                for (BodyPart part : parent.parts.getAllTyped())
                {
                    if (part.getForm() == form)
                    {
                        path.add(String.valueOf(i));
                    }

                    i += 1;
                }
            }

            form = parent;
        }

        Collections.reverse(path);

        return String.join(PATH_SEPARATOR, path);
    }

    /* Form properties utils */

    public static String getPropertyPath(BaseValue property)
    {
        path.clear();
        path.add(property.getId());

        Form form = getForm(property);

        while (form != null)
        {
            Form parent = form.getParentForm();

            if (parent != null)
            {
                int i = 0;

                for (BodyPart part : parent.parts.getAllTyped())
                {
                    if (part.getForm() == form)
                    {
                        path.add(String.valueOf(i));
                    }

                    i += 1;
                }
            }

            form = parent;
        }

        Collections.reverse(path);

        return String.join(PATH_SEPARATOR, path);
    }

    public static List<String> collectPropertyPaths(Form form)
    {
        List<String> properties = new ArrayList<>();

        collectPropertyPaths(form, properties, "");

        /* There is no need to animate body part anchor properties */
        Iterator<String> it = properties.iterator();

        while (it.hasNext())
        {
            if (it.next().endsWith("/anchor"))
            {
                it.remove();
            }
        }

        return properties;
    }

    public static void collectPropertyPaths(Form form, List<String> properties, String prefix)
    {
        if (form == null || !form.animatable.get())
        {
            return;
        }

        for (BaseValue property : form.getAll())
        {
            if (property.isVisible())
            {
                properties.add(StringUtils.combinePaths(prefix, property.getId()));
            }
        }

        List<BodyPart> all = form.parts.getAllTyped();

        for (int i = 0; i < all.size(); i++)
        {
            String newPrefix = StringUtils.combinePaths(prefix, String.valueOf(i));

            collectPropertyPaths(all.get(i).getForm(), properties, newPrefix);
        }
    }

    public static BaseValueBasic getProperty(Form form, String path)
    {
        if (form == null)
        {
            return null;
        }

        if (!path.contains(PATH_SEPARATOR))
        {
            return form.getAllMap().get(path);
        }

        String[] segments = path.split(PATH_SEPARATOR);

        for (int i = 0; i < segments.length; i++)
        {
            String segment = segments[i];
            BaseValueBasic property = form.getAllMap().get(segment);

            if (property == null)
            {
                try
                {
                    int index = Integer.parseInt(segment);

                    if (CollectionUtils.inRange(form.parts.getAll(), index))
                    {
                        form = form.parts.getAllTyped().get(index).getForm();

                        if (form == null)
                        {
                            return null;
                        }
                    }
                    else
                    {
                        return null;
                    }
                }
                catch (Exception e)
                {}
            }
            else
            {
                return property;
            }
        }

        return null;
    }

    /**
     * Prior to 1.6, there was a mechanism called state triggers (commissioned by Checkpoint).
     *
     * It was a way to override form properties by pressing a key. In 1.6, they were superseded
     * by animation states mechanism. This code converts the data from state trigger format into
     * animation states. It's not 1-to-1, but better than nothing.
     */
    public static void readOldStateTriggers(Form form, MapType map)
    {
        if (map.has("stateTriggers") && map.getMap("stateTriggers").has("list"))
        {
            ListType list = map.getMap("stateTriggers").getList("list");

            for (BaseType type : list)
            {
                if (!type.isMap())
                {
                    continue;
                }

                MapType stateTrigger = type.asMap();
                AnimationState state = new AnimationState("");
                MapType states = stateTrigger.getMap("states");

                state.id.set(stateTrigger.getString("id"));
                state.keybind.set(stateTrigger.getInt("hotkey"));

                for (String key : states.keys())
                {
                    BaseType stateData = states.get(key);
                    KeyframeChannel channel = state.properties.getOrCreate(form, key);

                    if (channel != null)
                    {
                        Object o = channel.getFactory().fromData(stateData);

                        channel.insert(0F, o);
                    }
                }

                form.states.add(state);
            }
        }

        form.states.sync();
    }
}