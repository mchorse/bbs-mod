package mchorse.bbs_mod.ui.film.replays;

import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.settings.values.base.BaseValue;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.film.UIFilmPanel;
import mchorse.bbs_mod.ui.forms.UINestedEdit;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIToggle;
import mchorse.bbs_mod.ui.framework.elements.input.UITrackpad;
import mchorse.bbs_mod.ui.framework.elements.input.text.UITextbox;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIOverlayPanel;
import mchorse.bbs_mod.ui.utils.UI;
import mchorse.bbs_mod.ui.utils.UIDataUtils;
import mchorse.bbs_mod.utils.colors.Colors;

import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class UIReplaysOverlayPanel extends UIOverlayPanel
{
    public UIReplayList replays;

    public UIElement properties;
    public UINestedEdit pickEdit;
    public UIToggle enabled;
    public UITextbox label;
    public UITextbox nameTag;
    public UIToggle shadow;
    public UITrackpad shadowSize;
    public UITrackpad looping;
    public UIToggle actor;
    public UIToggle fp;
    public UIToggle relative;
    public UITrackpad relativeOffsetX;
    public UITrackpad relativeOffsetY;
    public UITrackpad relativeOffsetZ;

    private Consumer<Replay> callback;
    private UIFilmPanel filmPanel;
    public UITextbox group;
    public UITextbox groupFilter;

    public UIReplaysOverlayPanel(UIFilmPanel filmPanel, Consumer<Replay> callback)
    {
        super(UIKeys.FILM_REPLAY_TITLE);

        this.callback = callback;
        this.filmPanel = filmPanel;
        this.replays = new UIReplayList((l) -> this.callback.accept(l.isEmpty() ? null : l.get(0)), this, filmPanel);

        this.pickEdit = new UINestedEdit((editing) ->
        {
            this.replays.openFormEditor(this.replays.getCurrent().get(0).form, editing, this.pickEdit::setForm);
        });
        this.pickEdit.keybinds();
        this.pickEdit.pick.tooltip(UIKeys.SCENE_REPLAYS_CONTEXT_PICK_FORM);
        this.pickEdit.edit.tooltip(UIKeys.SCENE_REPLAYS_CONTEXT_EDIT_FORM);
        this.enabled = new UIToggle(UIKeys.CAMERA_PANELS_ENABLED, (b) ->
        {
            this.edit((replay) -> replay.enabled.set(b.getValue()));
            filmPanel.getController().createEntities();
        });
        this.label = new UITextbox(1000, (s) -> this.edit((replay) -> replay.label.set(s)));
        this.label.textbox.setPlaceholder(UIKeys.FILM_REPLAY_LABEL);
        this.nameTag = new UITextbox(1000, (s) -> this.edit((replay) -> replay.nameTag.set(s)));
        this.nameTag.textbox.setPlaceholder(UIKeys.FILM_REPLAY_NAME_TAG);
        this.shadow = new UIToggle(UIKeys.FILM_REPLAY_SHADOW, (b) -> this.edit((replay) -> replay.shadow.set(b.getValue())));
        this.shadowSize = new UITrackpad((v) -> this.edit((replay) -> replay.shadowSize.set(v.floatValue())));
        this.shadowSize.tooltip(UIKeys.FILM_REPLAY_SHADOW_SIZE);
        this.looping = new UITrackpad((v) -> this.edit((replay) -> replay.looping.set(v.intValue())));
        this.looping.limit(0).integer().tooltip(UIKeys.FILM_REPLAY_LOOPING_TOOLTIP);
        this.actor = new UIToggle(UIKeys.FILM_REPLAY_ACTOR, (b) -> this.edit((replay) -> replay.actor.set(b.getValue())));
        this.actor.tooltip(UIKeys.FILM_REPLAY_ACTOR_TOOLTIP);
        this.fp = new UIToggle(UIKeys.FILM_REPLAY_FP, (b) ->
        {
            for (Replay replay : this.replays.getList())
            {
                if (replay.fp.get())
                {
                    replay.fp.set(false);
                }
            }

            this.replays.getCurrentFirst().fp.set(b.getValue());
        });
        this.relative = new UIToggle(UIKeys.CAMERA_PANELS_RELATIVE, (b) -> this.edit((replay) -> replay.relative.set(b.getValue())));
        this.relative.tooltip(UIKeys.FILM_REPLAY_RELATIVE_TOOLTIP);
        this.relativeOffsetX = new UITrackpad((v) -> this.edit((replay) -> BaseValue.edit(replay.relativeOffset, (value) -> value.get().x = v)));
        this.relativeOffsetY = new UITrackpad((v) -> this.edit((replay) -> BaseValue.edit(replay.relativeOffset, (value) -> value.get().y = v)));
        this.relativeOffsetZ = new UITrackpad((v) -> this.edit((replay) -> BaseValue.edit(replay.relativeOffset, (value) -> value.get().z = v)));
        // Add group edit and filter
        this.group = new UITextbox(1000, (s) -> this.edit((replay) -> replay.group.set(s)));
        this.groupFilter = new UITextbox(1000, (s) ->
        {
            String filter = s;
            List<Replay> all = this.filmPanel.getData().replays.getList();
            List<Replay> filtered = (filter == null || filter.isEmpty())
                ? all
                : all.stream().filter(r -> filter.equals(r.group.get())).collect(Collectors.toList());
            this.replays.setList(filtered);
        });

        this.properties = UI.scrollView(5, 6,
            UI.label(UIKeys.FILM_REPLAY_REPLAY),
            this.pickEdit, this.enabled,
            this.label, this.nameTag,
            this.shadow, this.shadowSize,
            UI.label(UIKeys.FILM_REPLAY_LOOPING),
            this.looping, this.actor, this.fp,
            this.relative, UI.row(this.relativeOffsetX, this.relativeOffsetY, this.relativeOffsetZ),
            this.group, this.groupFilter
        );
        this.properties.relative(this.replays).x(1F).wTo(this.icons.area).h(1F);
        this.replays.relative(this.content).w(0.5F).h(1F);

        this.content.add(this.replays, this.properties);
    }

    private void edit(Consumer<Replay> consumer)
    {
        if (consumer != null)
        {
            List<Replay> current = this.replays.getCurrent();

            for (Replay replay : current)
            {
                consumer.accept(replay);
            }
        }
    }

    public void setReplay(Replay replay)
    {
        this.properties.setVisible(replay != null);

        if (replay != null)
        {
            this.pickEdit.setForm(replay.form.get());
            this.enabled.setValue(replay.enabled.get());
            this.label.setText(replay.label.get());
            this.nameTag.setText(replay.nameTag.get());
            this.shadow.setValue(replay.shadow.get());
            this.shadowSize.setValue(replay.shadowSize.get());
            this.looping.setValue(replay.looping.get());
            this.actor.setValue(replay.actor.get());
            this.fp.setValue(replay.fp.get());
            this.relative.setValue(replay.relative.get());
            this.relativeOffsetX.setValue(replay.relativeOffset.get().x);
            this.relativeOffsetY.setValue(replay.relativeOffset.get().y);
            this.relativeOffsetZ.setValue(replay.relativeOffset.get().z);
            // Keep group UI in sync
            this.group.setText(replay.group.get());
        }
    }

    @Override
    protected void renderBackground(UIContext context)
    {
        super.renderBackground(context);

        this.content.area.render(context.batcher, Colors.A100);

        if (this.replays.getList().size() < 3)
        {
            UIDataUtils.renderRightClickHere(context, this.replays.area);
        }
    }
}