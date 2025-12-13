package mchorse.bbs_mod.ui.film.utils;

import mchorse.bbs_mod.camera.utils.TimeUtils;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.IAxisConverter;

public class CameraAxisConverter implements IAxisConverter
{
    @Override
    public String format(double value)
    {
        return TimeUtils.formatTime((long) value);
    }

    @Override
    public double from(double v)
    {
        return TimeUtils.fromTime(v);
    }

    @Override
    public double to(double v)
    {
        return TimeUtils.toTime((int) v);
    }
}