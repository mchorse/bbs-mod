package mchorse.bbs_mod.ui.framework.elements.input.keyframes;

public interface IAxisConverter
{
    public String format(double value);

    public double from(double x);

    public double to(double x);
}