package mchorse.bbs_mod.forms;

import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.data.DataToString;
import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.settings.values.numeric.ValueBoolean;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class VisibilityManager
{
    private final List<ValueBoolean> visibility = new ArrayList<>();

    public ValueBoolean get(String id)
    {
        return this.get(id, true);
    }

    public ValueBoolean get(String id, boolean defaultValue)
    {
        for (ValueBoolean visibility : this.visibility)
        {
            if (visibility.getId().equals(id))
            {
                return visibility;
            }
        }

        ValueBoolean value = new ValueBoolean(id, defaultValue);

        value.postCallback((v, f) ->
        {
            if (f != 1) this.write();
        });
        this.visibility.add(value);

        return value;
    }

    public void remove(String id)
    {
        this.visibility.removeIf(visibility -> visibility.getId().equals(id));

        this.write();
    }

    public void read()
    {
        try
        {
            BaseType data = DataToString.read(BBSMod.getSettingsPath("categories.json"));

            if (data instanceof MapType map)
            {
                for (String key : map.keys())
                {
                    this.get(key, map.getBool(key)).set(map.getBool(key), 1);
                }
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    public void write()
    {
        MapType type = new MapType();

        for (ValueBoolean value : this.visibility)
        {
            type.putBool(value.getId(), value.get());
        }

        DataToString.writeSilently(BBSMod.getSettingsPath("categories.json"), type, true);
    }
}