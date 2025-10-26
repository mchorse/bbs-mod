package mchorse.bbs_mod.camera.clips.misc;

import mchorse.bbs_mod.camera.clips.CameraClip;
import mchorse.bbs_mod.camera.data.Position;
import mchorse.bbs_mod.camera.values.ValueChannels;
import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.settings.values.core.ValueString;
import mchorse.bbs_mod.utils.clips.Clip;
import mchorse.bbs_mod.utils.clips.ClipContext;
import mchorse.bbs_mod.utils.keyframes.KeyframeChannel;

import java.util.HashMap;
import java.util.Map;

public class CurveClip extends CameraClip
{
    public static final String SHADER_CURVES_PREFIX = "curve.";

    public final ValueChannels channels = new ValueChannels("channels");

    public static Map<String, Double> getValues(ClipContext context)
    {
        if (context.clipData.containsKey("curve_data"))
        {
            return (Map<String, Double>) context.clipData.get("curve_data");
        }

        Map<String, Double> data = new HashMap<>();

        context.clipData.put("curve_data", data);

        return data;
    }

    public CurveClip()
    {
        this.add(this.channels);
        this.channels.addChannel("sun_rotation");
    }

    @Override
    protected void applyClip(ClipContext context, Position position)
    {
        Map<String, Double> values = getValues(context);

        for (KeyframeChannel<Double> channel : this.channels.getChannels())
        {
            if (!channel.isEmpty())
            {
                values.put(channel.getId(), channel.interpolate(context.relativeTick + context.transition));
            }
        }
    }

    @Override
    protected Clip create()
    {
        return new CurveClip();
    }

    @Override
    public void fromData(BaseType data)
    {
        if (data.isMap())
        {
            MapType map = data.asMap();

            if (map.has("key") && map.has("channel"))
            {
                ValueString key = new ValueString("key", "sun_rotation");

                key.fromData(map.get("key"));

                KeyframeChannel<Double> channel = this.channels.addChannel(key.get());

                channel.fromData(map.get("channel"));
            }
        }

        super.fromData(data);
    }
}