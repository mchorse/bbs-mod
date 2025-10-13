package mchorse.bbs_mod.ui.film.utils.undo;

import mchorse.bbs_mod.settings.values.core.ValueGroup;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.film.UIFilmPanel;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIOverlayPanel;
import mchorse.bbs_mod.ui.utils.UIUtils;
import mchorse.bbs_mod.utils.undo.UndoManager;

public class UIUndoHistoryOverlay extends UIOverlayPanel
{
    private UIUndoList<ValueGroup> list;

    private UIFilmPanel panel;

    public UIUndoHistoryOverlay(UIFilmPanel panel)
    {
        super(UIKeys.FILM_HISTORY_TITLE);

        this.panel = panel;

        this.list = new UIUndoList((l) ->
        {
            int index = this.list.getIndex();
            UndoManager<ValueGroup> undoManager = this.panel.getUndoHandler().getUndoManager();

            while (undoManager.getCurrentUndoIndex() != index)
            {
                if (undoManager.getCurrentUndoIndex() > index)
                {
                    undoManager.undo(this.panel.getData());
                }
                else
                {
                    undoManager.redo(this.panel.getData());
                }
            }

            UIUtils.playClick();
        });
        this.list.setList(this.panel.getUndoHandler().getUndoManager().getUndos());
        this.list.full(this.content);
        this.list.setIndex(this.panel.getUndoHandler().getUndoManager().getCurrentUndoIndex());

        this.content.add(this.list);
    }
}