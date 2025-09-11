package mchorse.bbs_mod.ui.framework.elements.input.keyframes.factories;

import io.netty.util.collection.IntObjectMap;
import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.forms.FormUtilsClient;
import mchorse.bbs_mod.forms.entities.IEntity;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.forms.properties.AnchorProperty;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.settings.values.base.BaseValue;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.film.UIFilmPanel;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIToggle;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframes;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.utils.colors.Colors;
import mchorse.bbs_mod.utils.keyframes.Keyframe;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class UIAnchorKeyframeFactory extends UIKeyframeFactory<AnchorProperty.Anchor>
{
    private UIButton actor;
    private UIButton attachment;
    private UIToggle translate;
    private UIToggle scale;

    public static void displayActors(UIContext context, IntObjectMap<IEntity> entities, int value, Consumer<Integer> callback)
    {
        List<UIFilmPanel> children = context.menu.main.getChildren(UIFilmPanel.class);
        UIFilmPanel panel = children.isEmpty() ? null : children.get(0);
        List<Replay> replays = panel != null ? panel.getData().replays.getList() : null;

        context.replaceContextMenu((menu) ->
        {
            menu.action(Icons.CLOSE, UIKeys.GENERAL_NONE, Colors.NEGATIVE, () -> callback.accept(-1));

            for (int i = 0; i < entities.size(); i++)
            {
                final int actor = i;
                IEntity entity = entities.get(i);
                Replay replay = replays == null ? null : replays.get(i);
                Form form = entity.getForm();
                String stringLabel = i + (replay != null ? " - " + replay.getName() : (form == null ? "" : " - " + form.getIdOrName()));
                IKey label = IKey.constant(stringLabel);

                menu.action(Icons.CLOSE, label, actor == value, () -> callback.accept(actor));
            }
        });
    }

    public UIAnchorKeyframeFactory(Keyframe<AnchorProperty.Anchor> keyframe, UIKeyframes editor)
    {
        super(keyframe, editor);

        this.actor = new UIButton(UIKeys.GENERIC_KEYFRAMES_ANCHOR_PICK_ACTOR, (b) -> this.displayActors());
        this.attachment = new UIButton(UIKeys.GENERIC_KEYFRAMES_ANCHOR_PICK_ATTACHMENT, (b) -> this.displayAttachments());
        this.translate = new UIToggle(UIKeys.TRANSFORMS_TRANSLATE, (b) -> this.setTranslate(b.getValue()));
        this.translate.setValue(keyframe.getValue().translate);
        this.scale = new UIToggle(UIKeys.TRANSFORMS_SCALE, (b) -> this.setScale(b.getValue()));
        this.scale.setValue(keyframe.getValue().scale);

        this.scroll.add(this.actor, this.attachment, this.translate, this.scale);
    }

    private void displayActors()
    {
        UIFilmPanel panel = this.getPanel();

        displayActors(this.getContext(), panel.getController().getEntities(), this.keyframe.getValue().actor, this::setActor);
    }

    private void displayAttachments()
    {
        UIFilmPanel panel = this.getPanel();
        int index = this.keyframe.getValue().actor;
        IEntity entity = panel.getController().getEntities().get(index);

        if (entity == null || entity.getForm() == null)
        {
            return;
        }

        Form form = entity.getForm();
        Map<String, Matrix4f> map = new HashMap<>();
        MatrixStack stack = new MatrixStack();

        FormUtilsClient.getRenderer(form).collectMatrices(entity, null, stack, map, "", 0);

        List<String> attachments = new ArrayList<>(map.keySet());

        attachments.sort(String::compareToIgnoreCase);

        if (attachments.isEmpty())
        {
            return;
        }

        String value = this.keyframe.getValue().attachment;

        this.getContext().replaceContextMenu((menu) ->
        {
            for (String attachment : attachments)
            {
                menu.action(Icons.LIMB, IKey.constant(attachment), attachment.equals(value), () -> this.setAttachment(attachment));
            }
        });
    }

    private void setActor(int actor)
    {
        BaseValue.edit(this.keyframe, (value) -> value.getValue().actor = actor);
    }

    private void setAttachment(String attachment)
    {
        BaseValue.edit(this.keyframe, (value) -> value.getValue().attachment = attachment);
    }

    private void setTranslate(boolean translate)
    {
        BaseValue.edit(this.keyframe, (value) -> value.getValue().translate = translate);
    }

    private void setScale(boolean scale)
    {
        BaseValue.edit(this.keyframe, (value) -> value.getValue().scale = scale);
    }

    private UIFilmPanel getPanel()
    {
        return this.getContext().menu.getRoot().getChildren(UIFilmPanel.class).get(0);
    }
}