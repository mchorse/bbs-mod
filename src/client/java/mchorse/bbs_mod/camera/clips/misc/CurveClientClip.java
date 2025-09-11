package mchorse.bbs_mod.camera.clips.misc;

import mchorse.bbs_mod.camera.data.Position;
import mchorse.bbs_mod.utils.clips.ClipContext;
import mchorse.bbs_mod.utils.iris.ShaderCurves;
import mchorse.bbs_mod.utils.keyframes.KeyframeChannel;

public class CurveClientClip extends CurveClip
{
    public CurveClientClip()
    {}

    @Override
    protected void applyClip(ClipContext context, Position position)
    {
        super.applyClip(context, position);

        for (KeyframeChannel<Double> channel : this.channels.getChannels())
        {
            String id = channel.getId();

            if (id.startsWith(SHADER_CURVES_PREFIX))
            {
                ShaderCurves.ShaderVariable variable = ShaderCurves.variableMap.get(id.substring(SHADER_CURVES_PREFIX.length()));

                if (variable != null)
                {
                    variable.value = channel.interpolate(context.relativeTick + context.transition).floatValue();
                }
            }
        }
    }
}