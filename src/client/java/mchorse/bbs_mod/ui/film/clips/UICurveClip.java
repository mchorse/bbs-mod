package mchorse.bbs_mod.ui.film.clips;

import mchorse.bbs_mod.camera.clips.misc.CurveClip;
import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.Keys;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.film.IUIClipsDelegate;
import mchorse.bbs_mod.ui.film.replays.UIReplaysEditor;
import mchorse.bbs_mod.ui.film.utils.keyframes.UIFilmKeyframes;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframeEditor;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframeSheet;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIOverlay;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIStringOverlayPanel;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.utils.clips.Clips;
import mchorse.bbs_mod.utils.colors.Colors;
import mchorse.bbs_mod.utils.iris.ShaderCurves;
import mchorse.bbs_mod.utils.keyframes.KeyframeChannel;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class UICurveClip extends UIClip<CurveClip>
{
    public UIKeyframeEditor keyframes;
    public UIButton edit;

    public UICurveClip(CurveClip clip, IUIClipsDelegate editor)
    {
        super(clip, editor);
    }

    public static void offerCurveKeys(UIContext context, List<String> existing, Consumer<String> callback)
    {
        List<String> list = new ArrayList<>();

        for (ShaderCurves.ShaderVariable value : ShaderCurves.variableMap.values())
        {
            list.add(CurveClip.SHADER_CURVES_PREFIX + value.name);
        }

        list.add(ShaderCurves.BRIGHTNESS);
        list.add(ShaderCurves.SUN_ROTATION);
        list.removeAll(existing);

        UIStringOverlayPanel panel = new UIStringOverlayPanel(UIKeys.CAMERA_PANELS_PICK_KEY, list, callback);

        panel.strings.list.sort();

        UIOverlay.addOverlay(context, panel);
    }

    @Override
    protected void registerUI()
    {
        super.registerUI();

        this.keyframes = new UIKeyframeEditor((consumer) -> new UIFilmKeyframes(this.editor, consumer));
        this.keyframes.view.backgroundRenderer((context) ->
        {
            UIReplaysEditor.renderBackground(context, this.keyframes.view, (Clips) this.clip.getParent(), this.clip.tick.get());
        });
        this.keyframes.view.duration(() -> this.clip.duration.get());
        this.keyframes.setUndoId("curve_keyframes");

        this.keyframes.view.context((menu) ->
        {
            menu.action(Icons.ADD, UIKeys.CAMERA_PANELS_CURVE_ADD, () ->
            {
                List<String> existing = new ArrayList<>();

                for (KeyframeChannel<Double> channel : this.clip.channels.getChannels())
                {
                    existing.add(channel.getId());
                }

                offerCurveKeys(this.getContext(), existing, (s) ->
                {
                    this.clip.channels.addChannel(s);
                    this.fillData();
                });
            }).order(-3);

            UIKeyframeSheet sheet = this.keyframes.view.getDopeSheet().getSheet(this.getContext().mouseY);

            if (sheet != null)
            {
                menu.action(Icons.REMOVE, UIKeys.CAMERA_PANELS_CURVE_REMOVE, Colors.RED, () ->
                {
                    this.clip.channels.removeChannel(sheet.channel);
                    this.fillData();
                });
            }
        });

        this.edit = new UIButton(UIKeys.CAMERA_PANELS_EDIT_KEYFRAMES, (b) ->
        {
            this.editor.embedView(this.keyframes);
            this.keyframes.view.resetView();
        });
        this.edit.keys().register(Keys.FORMS_EDIT, () -> this.edit.clickItself());
    }

    private void addChannel(KeyframeChannel<Double> channel)
    {
        this.keyframes.view.addSheet(new UIKeyframeSheet(channel.getId(), IKey.constant(channel.getId()), channel.getId().hashCode() & Colors.RGB, false, channel, null));
    }

    @Override
    protected void registerPanels()
    {
        super.registerPanels();

        this.panels.add(UIClip.label(UIKeys.C_CLIP.get("bbs:curve")).marginTop(12), this.edit);
    }

    @Override
    public void fillData()
    {
        super.fillData();

        this.keyframes.view.removeAllSheets();

        for (KeyframeChannel<Double> channel : this.clip.channels.getChannels())
        {
            this.addChannel(channel);
        }
    }

    @Override
    public void updateDuration(int duration)
    {
        super.updateDuration(duration);

        this.keyframes.updateConverter();
    }

    @Override
    public void applyUndoData(MapType data)
    {
        if (data.getString("embed").equals("curve"))
        {
            this.editor.embedView(this.keyframes);
            this.keyframes.view.resetView();
        }

        super.applyUndoData(data);
    }

    @Override
    public void collectUndoData(MapType data)
    {
        if (this.keyframes.hasParent())
        {
            data.putString("embed", "curve");
        }

        super.collectUndoData(data);
    }
}