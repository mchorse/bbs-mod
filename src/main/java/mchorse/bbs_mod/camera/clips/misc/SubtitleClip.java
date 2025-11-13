package mchorse.bbs_mod.camera.clips.misc;

import mchorse.bbs_mod.camera.clips.CameraClip;
import mchorse.bbs_mod.camera.data.Position;
import mchorse.bbs_mod.settings.values.core.ValueTransform;
import mchorse.bbs_mod.settings.values.numeric.ValueBoolean;
import mchorse.bbs_mod.settings.values.numeric.ValueFloat;
import mchorse.bbs_mod.settings.values.numeric.ValueInt;
import mchorse.bbs_mod.utils.clips.Clip;
import mchorse.bbs_mod.utils.clips.ClipContext;
import mchorse.bbs_mod.utils.colors.Colors;
import mchorse.bbs_mod.utils.pose.Transform;

import java.util.ArrayList;
import java.util.List;

public class SubtitleClip extends CameraClip
{
    public ValueInt x = new ValueInt("x", 0);
    public ValueInt y = new ValueInt("y", 0);
    public ValueFloat size = new ValueFloat("size", 10F);
    public ValueFloat anchorX = new ValueFloat("anchorX", 0.5F);
    public ValueFloat anchorY = new ValueFloat("anchorY", 0.5F);
    public ValueInt color = new ValueInt("color", Colors.WHITE);
    public ValueBoolean textShadow = new ValueBoolean("textShadow", true);
    public ValueFloat windowX = new ValueFloat("windowX", 0.5F);
    public ValueFloat windowY = new ValueFloat("windowY", 0.5F);
    public ValueInt background = new ValueInt("background", 0);
    public ValueFloat backgroundOffset = new ValueFloat("backgroundOffset", 2F);
    public ValueFloat shadow = new ValueFloat("shadow", 0F);
    public ValueBoolean shadowOpaque = new ValueBoolean("shadowOpaque", false);
    public ValueTransform transform = new ValueTransform("transform", new Transform());
    public ValueInt lineHeight = new ValueInt("lineHeight", 12);
    public ValueInt maxWidth = new ValueInt("maxWidth", 0);

    private Subtitle subtitle = new Subtitle();

    public static List<Subtitle> getSubtitles(ClipContext context)
    {
        return context.clipData.get("subtitles", ArrayList::new);
    }

    public SubtitleClip()
    {
        this.add(this.x);
        this.add(this.y);
        this.add(this.size);
        this.add(this.anchorX);
        this.add(this.anchorY);
        this.add(this.color);
        this.add(this.textShadow);
        this.add(this.windowX);
        this.add(this.windowY);
        this.add(this.background);
        this.add(this.backgroundOffset);
        this.add(this.shadow);
        this.add(this.shadowOpaque);
        this.add(this.transform);
        this.add(this.lineHeight);
        this.add(this.maxWidth);
    }

    @Override
    protected void applyClip(ClipContext context, Position position)
    {
        List<Subtitle> subtitles = getSubtitles(context);
        float factor = this.envelope.factorEnabled(this.duration.get(), context.relativeTick + context.transition);
        int color = Colors.setA(this.color.get(), factor * Colors.getA(this.color.get()));

        this.subtitle.update(this.title.get(), this.x.get(), this.y.get(), this.size.get(), this.anchorX.get(), this.anchorY.get(), color, this.textShadow.get());
        this.subtitle.updateWindow(this.windowX.get(), this.windowY.get());
        this.subtitle.updateBackground(this.background.get(), this.backgroundOffset.get(), this.shadow.get(), this.shadowOpaque.get());
        this.subtitle.updateTransform(this.transform.get(), factor);
        this.subtitle.updateConstraints(this.lineHeight.get(), this.maxWidth.get());
        subtitles.add(this.subtitle);
    }

    @Override
    protected Clip create()
    {
        return new SubtitleClip();
    }
}