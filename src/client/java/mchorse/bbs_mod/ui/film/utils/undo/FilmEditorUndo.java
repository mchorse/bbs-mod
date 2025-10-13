package mchorse.bbs_mod.ui.film.utils.undo;

import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.settings.values.core.ValueGroup;
import mchorse.bbs_mod.ui.film.UIFilmPanel;
import mchorse.bbs_mod.utils.undo.IUndo;

public abstract class FilmEditorUndo implements IUndo<ValueGroup>
{
    public MapType uiBefore;
    public MapType uiAfter;

    public MapType getUIData(boolean redo)
    {
        return redo ? this.uiAfter : this.uiBefore;
    }

    public void cacheBefore(MapType uiData)
    {
        this.uiBefore = uiData;
    }

    public void cacheAfter(UIFilmPanel editor)
    {
        this.uiAfter = editor.collectAllUndoData();
    }
}