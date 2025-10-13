package mchorse.bbs_mod.settings.values.ui;

import mchorse.bbs_mod.settings.values.core.ValueGroup;
import mchorse.bbs_mod.settings.values.core.ValueString;
import mchorse.bbs_mod.settings.values.numeric.ValueInt;

public class ValueVideoSettings extends ValueGroup
{
    public static final String DEFAULT_FFMPEG_ARGUMENTS = "-f rawvideo -pix_fmt bgr24 -s %WIDTH%x%HEIGHT% -r %FPS% -i - -vf %FILTERS% -c:v libx264 -preset ultrafast -tune zerolatency -qp 18 -pix_fmt yuv420p %NAME%.mp4";

    public final ValueString arguments = new ValueString("arguments", DEFAULT_FFMPEG_ARGUMENTS);
    public final ValueInt width = new ValueInt("width", 1280, 2, 8096);
    public final ValueInt height = new ValueInt("height", 720, 2, 8096);
    public final ValueInt frameRate = new ValueInt("frameRate", 60, 10, 1000);
    public final ValueInt motionBlur = new ValueInt("motionBlur", 0, 0, 6);
    public final ValueInt heldFrames = new ValueInt("heldFrames", 1, 1, 1000);
    public final ValueString path = new ValueString("exportPath", "");

    public ValueVideoSettings(String id)
    {
        super(id);

        this.add(this.arguments);
        this.add(this.width);
        this.add(this.height);
        this.add(this.frameRate);
        this.add(this.motionBlur);
        this.add(this.heldFrames);
        this.add(this.path);
    }
}