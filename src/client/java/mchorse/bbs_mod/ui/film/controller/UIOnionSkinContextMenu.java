package mchorse.bbs_mod.ui.film.controller;

import mchorse.bbs_mod.film.replays.Replay;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.settings.values.ui.ValueOnionSkin;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.dashboard.panels.UIDashboardPanels;
import mchorse.bbs_mod.ui.film.UIFilmPanel;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIIcon;
import mchorse.bbs_mod.ui.framework.elements.context.UIContextMenu;
import mchorse.bbs_mod.ui.framework.elements.input.UIColor;
import mchorse.bbs_mod.ui.framework.elements.input.UITrackpad;
import mchorse.bbs_mod.ui.utils.UI;
import mchorse.bbs_mod.ui.utils.icons.Icons;

public class UIOnionSkinContextMenu extends UIContextMenu
{
    public UIIcon enable;
    public UIIcon all;
    public UIIcon group;
    public UITrackpad preFrames;
    public UIColor preColor;
    public UITrackpad postFrames;
    public UIColor postColor;

    private UIElement column;

    private UIFilmPanel panel;
    private ValueOnionSkin onionSkin;

    public UIOnionSkinContextMenu(UIFilmPanel panel, ValueOnionSkin onionSkin)
    {
        this.panel = panel;
        this.onionSkin = onionSkin;

        this.enable = new UIIcon(Icons.VISIBLE, (b) -> this.onionSkin.enabled.set(!this.onionSkin.enabled.get()));
        this.enable.tooltip(UIKeys.FILM_CONTROLLER_ONION_SKIN_TITLE);
        this.preFrames = new UITrackpad((v) -> this.onionSkin.preFrames.set(v.intValue()));
        this.preFrames.limit(0, 10, true).setValue(this.onionSkin.preFrames.get());
        this.preColor = new UIColor((c) -> this.onionSkin.preColor.set(c));
        this.preColor.withAlpha().setColor(this.onionSkin.preColor.get());
        this.postFrames = new UITrackpad((v) -> this.onionSkin.postFrames.set(v.intValue()));
        this.postFrames.limit(0, 10, true).setValue(this.onionSkin.postFrames.get());
        this.postColor = new UIColor((c) -> this.onionSkin.postColor.set(c));
        this.postColor.withAlpha().setColor(this.onionSkin.postColor.get());
        this.all = new UIIcon(Icons.POSE, (b) -> this.onionSkin.all.set(!this.onionSkin.all.get()));
        this.all.tooltip(UIKeys.FILM_CONTROLLER_ONION_SKIN_ALL_DESCRIPTION);
        this.group = new UIIcon(Icons.MORE, (b) ->
        {
            this.getContext().replaceContextMenu((menu) ->
            {
                Replay replay = this.panel.replayEditor.getReplay();

                if (replay == null)
                {
                    return;
                }

                for (String property : replay.properties.properties.keySet())
                {
                    menu.action(Icons.FOLDER, IKey.constant(property), this.onionSkin.group.get().equals(property), () ->
                    {
                        this.onionSkin.group.set(property);
                        this.group.tooltip(UIKeys.FILM_CONTROLLER_ONION_SKIN_GROUP.format(property));
                    });
                }
            });
        });
        this.group.tooltip(UIKeys.FILM_CONTROLLER_ONION_SKIN_GROUP.format(this.onionSkin.group.get()));

        UIElement row = UI.row(this.enable, this.all, this.group);

        this.column = UI.column(5, 10,
            row,
            UI.row(this.preFrames, this.preColor).tooltip(UIKeys.FILM_CONTROLLER_ONION_SKIN_PREV),
            UI.row(this.postFrames, this.postColor).tooltip(UIKeys.FILM_CONTROLLER_ONION_SKIN_NEXT)
        );
        this.column.relative(this).w(140);

        this.add(this.column);
        this.column.resize();
    }

    @Override
    public boolean isEmpty()
    {
        return false;
    }

    @Override
    public void setMouse(UIContext context)
    {
        this.xy(context.mouseX(), context.mouseY())
            .wh(this.column.area.w, this.column.area.h)
            .bounds(context.menu.overlay, 5);
    }

    @Override
    protected void renderBackground(UIContext context)
    {
        super.renderBackground(context);

        if (this.onionSkin.enabled.get())
        {
            UIDashboardPanels.renderHighlight(context.batcher, this.enable.area);
        }

        if (this.onionSkin.all.get())
        {
            UIDashboardPanels.renderHighlight(context.batcher, this.all.area);
        }
    }
}