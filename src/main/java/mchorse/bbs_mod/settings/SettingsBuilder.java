package mchorse.bbs_mod.settings;

import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.settings.values.numeric.ValueBoolean;
import mchorse.bbs_mod.settings.values.numeric.ValueDouble;
import mchorse.bbs_mod.settings.values.numeric.ValueFloat;
import mchorse.bbs_mod.settings.values.core.ValueGroup;
import mchorse.bbs_mod.settings.values.numeric.ValueInt;
import mchorse.bbs_mod.settings.values.core.ValueLink;
import mchorse.bbs_mod.settings.values.core.ValueString;
import mchorse.bbs_mod.settings.values.base.BaseValue;
import mchorse.bbs_mod.ui.utils.icons.Icon;

import java.io.File;

public class SettingsBuilder
{
    private Settings settings;
    private ValueGroup category;

    public SettingsBuilder(Icon icon, String id, File file)
    {
        this.settings = new Settings(icon, id, file);
    }

    public Settings getConfig()
    {
        return this.settings;
    }

    public ValueGroup getCategory()
    {
        return this.category;
    }

    public SettingsBuilder category(String id)
    {
        this.settings.categories.put(id, this.category = new ValueGroup(id));
        this.category.setParent(this.settings);

        return this;
    }

    public SettingsBuilder register(BaseValue value)
    {
        if (this.category == null)
        {
            throw new IllegalStateException("A category must be created before any of the config options can created! Create a category by calling ConfigBuilder.category(String) method!");
        }

        this.category.add(value);

        return this;
    }

    public ValueInt getInt(String id, int defaultValue)
    {
        ValueInt value = new ValueInt(id, defaultValue);

        this.register(value);

        return value;
    }

    public ValueInt getInt(String id, int defaultValue, int min, int max)
    {
        ValueInt value = new ValueInt(id, defaultValue, min, max);

        this.register(value);

        return value;
    }

    public ValueFloat getFloat(String id, float defaultValue)
    {
        ValueFloat value = new ValueFloat(id, defaultValue);

        this.register(value);

        return value;
    }

    public ValueFloat getFloat(String id, float defaultValue, float min, float max)
    {
        ValueFloat value = new ValueFloat(id, defaultValue, min, max);

        this.register(value);

        return value;
    }

    public ValueDouble getDouble(String id, double defaultValue)
    {
        ValueDouble value = new ValueDouble(id, defaultValue);

        this.register(value);

        return value;
    }

    public ValueDouble getDouble(String id, double defaultValue, double min, double max)
    {
        ValueDouble value = new ValueDouble(id, defaultValue, min, max);

        this.register(value);

        return value;
    }

    public ValueBoolean getBoolean(String id, boolean defaultValue)
    {
        ValueBoolean value = new ValueBoolean(id, defaultValue);

        this.register(value);

        return value;
    }

    public ValueString getString(String id, String defaultValue)
    {
        ValueString value = new ValueString(id, defaultValue);

        this.register(value);

        return value;
    }

    public ValueLink getRL(String id, Link defaultValue)
    {
        ValueLink value = new ValueLink(id, defaultValue);

        this.register(value);

        return value;
    }
}