package mchorse.bbs_mod.film.replays;

import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.actions.SuperFakePlayer;
import mchorse.bbs_mod.actions.types.ActionClip;
import mchorse.bbs_mod.camera.data.Point;
import mchorse.bbs_mod.camera.values.ValuePoint;
import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.film.Film;
import mchorse.bbs_mod.forms.FormUtils;
import mchorse.bbs_mod.forms.entities.IEntity;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.settings.values.base.BaseValue;
import mchorse.bbs_mod.settings.values.base.BaseValueBasic;
import mchorse.bbs_mod.settings.values.core.ValueForm;
import mchorse.bbs_mod.settings.values.core.ValueGroup;
import mchorse.bbs_mod.settings.values.core.ValueString;
import mchorse.bbs_mod.settings.values.numeric.ValueBoolean;
import mchorse.bbs_mod.settings.values.numeric.ValueFloat;
import mchorse.bbs_mod.settings.values.numeric.ValueInt;
import mchorse.bbs_mod.settings.values.base.BaseValueGroup;
import mchorse.bbs_mod.utils.clips.Clip;
import mchorse.bbs_mod.utils.clips.Clips;
import mchorse.bbs_mod.utils.keyframes.KeyframeChannel;
import mchorse.bbs_mod.utils.keyframes.KeyframeSegment;
import net.minecraft.entity.LivingEntity;

import java.util.List;
import java.util.HashMap;
import java.util.Map;

public class Replay extends ValueGroup
{
    public final ValueForm form = new ValueForm("form");
    public final ReplayKeyframes keyframes = new ReplayKeyframes("keyframes");
    public final FormProperties properties = new FormProperties("properties");
    public final Clips actions = new Clips("actions", BBSMod.getFactoryActionClips());

    public final ValueBoolean enabled = new ValueBoolean("enabled", true);
    public final ValueString label = new ValueString("label", "");
    public final ValueString nameTag = new ValueString("name_tag", "");
    public final ValueString group = new ValueString("group", "");
    public final ValueBoolean shadow = new ValueBoolean("shadow", true);
    public final ValueFloat shadowSize = new ValueFloat("shadow_size", 0.5F);
    public final ValueInt looping = new ValueInt("looping", 0);

    public final ValueBoolean actor = new ValueBoolean("actor", false);
    public final ValueBoolean fp = new ValueBoolean("fp", false);
    public final ValueBoolean relative = new ValueBoolean("relative", false);
    public final ValuePoint relativeOffset = new ValuePoint("relativeOffset", new Point(0, 0, 0));

    private final Map<String, String> customSheetTitles = new HashMap<>();

    public Replay(String id)
    {
        super(id);

        this.add(this.form);
        this.add(this.keyframes);
        this.add(this.properties);
        this.add(this.actions);

        this.add(this.enabled);
        this.add(this.label);
        this.add(this.nameTag);
        this.add(this.group);
        this.add(this.shadow);
        this.add(this.shadowSize);
        this.add(this.looping);

        this.add(this.actor);
        this.add(this.fp);
        this.add(this.relative);
        this.add(this.relativeOffset);
    }

    public String getName()
    {
        String label = this.label.get();

        if (!label.isEmpty())
        {
            return label;
        }

        Form form = this.form.get();

        if (form == null)
        {
            return "-";
        }

        return form.getDisplayName();
    }

    public void shift(float tick)
    {
        this.keyframes.shift(tick);
        this.properties.shift(tick);
        this.actions.shift(tick);
    }

    public void applyFrame(int tick, IEntity actor)
    {
        this.applyFrame(tick, actor, null);
    }

    public void applyFrame(int tick, IEntity actor, List<String> groups)
    {
        this.keyframes.apply(tick, actor, groups);
    }

    public void applyProperties(float tick, Form form)
    {
        if (form == null)
        {
            return;
        }

        for (BaseValue value : this.properties.getAll())
        {
            if (value instanceof KeyframeChannel)
            {
                this.applyProperty(tick, form, (KeyframeChannel) value);
            }
        }
    }

    private void applyProperty(float tick, Form form, KeyframeChannel value)
    {
        BaseValueBasic property = FormUtils.getProperty(form, value.getId());

        if (property == null)
        {
            return;
        }

        KeyframeSegment segment = value.find(tick);

        if (segment != null)
        {
            property.set(segment.createInterpolated());
        }
        else
        {
            Form replayForm = this.form.get();

            if (replayForm != null)
            {
                BaseValueBasic replayProperty = FormUtils.getProperty(replayForm, value.getId());

                if (replayProperty != null)
                {
                    property.set(replayProperty.get());
                }
            }
        }
    }

    public void applyActions(LivingEntity actor, SuperFakePlayer fakePlayer, Film film, int tick)
    {
        List<Clip> clips = this.actions.getClips(tick);

        for (Clip clip : clips)
        {
            ((ActionClip) clip).apply(actor, fakePlayer, film, this, tick);
        }
    }

    public void applyClientActions(int tick, IEntity entity, Film film)
    {
        tick = this.getTick(tick);

        List<Clip> clips = this.actions.getClips(tick);

        for (Clip clip : clips)
        {
            if (clip instanceof ActionClip actionClip && actionClip.isClient())
            {
                actionClip.applyClient(entity, film, this, tick);
            }
        }
    }

    public int getTick(int tick)
    {
        return this.looping.get() > 0 ? tick % this.looping.get() : tick;
    }

    public String getCustomSheetTitle(String id)
    {
        return this.customSheetTitles.get(id);
    }

    public void setCustomSheetTitle(String id, String title)
    {
        if (title == null || title.isBlank())
        {
            this.customSheetTitles.remove(id);
        }
        else
        {
            this.customSheetTitles.put(id, title);
        }
    }

    @Override
    public void copy(BaseValueGroup group)
    {
        super.copy(group);

        if (group instanceof Replay other)
        {
            this.customSheetTitles.clear();
            this.customSheetTitles.putAll(other.customSheetTitles);
        }
    }

    @Override
    public BaseType toData()
    {
        MapType map = (MapType) super.toData();

        if (!this.customSheetTitles.isEmpty())
        {
            MapType titles = new MapType();

            for (Map.Entry<String, String> entry : this.customSheetTitles.entrySet())
            {
                titles.put(entry.getKey(), new mchorse.bbs_mod.data.types.StringType(entry.getValue()));
            }

            map.put("custom_sheet_titles", titles);
        }

        return map;
    }

    @Override
    public void fromData(BaseType data)
    {
        super.fromData(data);

        if (data instanceof MapType map)
        {
            BaseType titlesType = map.get("custom_sheet_titles");

            if (titlesType instanceof MapType titles)
            {
                for (String key : titles.keys())
                {
                    BaseType value = titles.get(key);

                    if (value != null && value.isString())
                    {
                        this.customSheetTitles.put(key, value.asString());
                    }
                }
            }
        }
    }
}