package mchorse.bbs_mod.ui.film.utils;

import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.network.ClientNetwork;
import mchorse.bbs_mod.settings.values.IValueListener;
import mchorse.bbs_mod.settings.values.core.ValueGroup;
import mchorse.bbs_mod.settings.values.base.BaseValue;
import mchorse.bbs_mod.ui.film.UIFilmPanel;
import mchorse.bbs_mod.ui.film.utils.undo.ValueChangeUndo;
import mchorse.bbs_mod.utils.Timer;
import mchorse.bbs_mod.utils.clips.Clips;
import mchorse.bbs_mod.utils.undo.CompoundUndo;
import mchorse.bbs_mod.utils.undo.IUndo;
import mchorse.bbs_mod.utils.undo.UndoManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class UIFilmUndoHandler
{
    private UndoManager<ValueGroup> undoManager;

    private Map<BaseValue, BaseType> cachedValues = new HashMap<>();
    private boolean cacheMarkLastUndoNoMerging;
    private MapType uiData;

    private Timer undoTimer = new Timer(1000);
    private Timer actionsTimer = new Timer(100);
    private Set<BaseValue> syncData = new HashSet<>();

    private UIFilmPanel panel;

    public UIFilmUndoHandler(UIFilmPanel panel)
    {
        this.panel = panel;

        this.undoManager = new UndoManager<>(100);
        this.undoManager.setCallback(this::handleUndos);
    }

    public UndoManager<ValueGroup> getUndoManager()
    {
        return this.undoManager;
    }

    /**
     * Handle undo/redo. This method primarily updates the UI state, according to
     * the undo/redo changes were done.
     */
    private void handleUndos(IUndo<ValueGroup> undo, boolean redo)
    {
        IUndo<ValueGroup> anotherUndo = undo;

        if (anotherUndo instanceof CompoundUndo)
        {
            anotherUndo = ((CompoundUndo<ValueGroup>) anotherUndo).getFirst(ValueChangeUndo.class);
        }

        if (anotherUndo instanceof ValueChangeUndo)
        {
            ValueChangeUndo change = (ValueChangeUndo) anotherUndo;

            this.panel.applyAllUndoData(change.getUIData(redo));
        }
    }

    public void handlePreValues(BaseValue baseValue, int flag)
    {
        if (this.uiData == null)
        {
            this.uiData = this.panel.collectAllUndoData();
        }

        if (!this.cachedValues.containsKey(baseValue))
        {
            this.cachedValues.put(baseValue, baseValue.toData());
        }

        if ((flag & IValueListener.FLAG_UNMERGEABLE) != 0)
        {
            this.cacheMarkLastUndoNoMerging = true;
        }
    }

    public void submitUndo()
    {
        this.handleTimers();

        if (this.cachedValues.isEmpty())
        {
            return;
        }

        this.reduceUndoRedundancy();

        List<ValueChangeUndo> changeUndos = new ArrayList<>();

        for (Map.Entry<BaseValue, BaseType> entry : this.cachedValues.entrySet())
        {
            BaseValue value = entry.getKey();
            ValueChangeUndo undo = new ValueChangeUndo(value.getPath(), entry.getValue(), value.toData());

            undo.cacheAfter(this.panel);
            undo.cacheBefore(this.uiData);
            changeUndos.add(undo);

            if (this.isReplayActions(value))
            {
                this.syncData.add(value);
                this.actionsTimer.mark();
            }
        }

        if (changeUndos.size() == 1)
        {
            this.undoManager.pushUndo(changeUndos.get(0));
        }
        else if (!changeUndos.isEmpty())
        {
            this.undoManager.pushUndo(new CompoundUndo<>(changeUndos.toArray(new IUndo[0])));
        }

        this.cachedValues.clear();
        this.uiData = null;

        this.undoTimer.mark();

        if (this.cacheMarkLastUndoNoMerging)
        {
            this.cacheMarkLastUndoNoMerging = false;

            this.markLastUndoNoMerging();
        }
    }

    private void handleTimers()
    {
        if (this.undoTimer.checkReset())
        {
            this.markLastUndoNoMerging();
        }

        if (this.actionsTimer.checkReset())
        {
            for (BaseValue syncData : this.syncData)
            {
                ClientNetwork.sendSyncData(this.panel.getData().getId(), syncData);
            }

            this.syncData.clear();
        }
    }

    private boolean isReplayActions(BaseValue value)
    {
        String path = value.getPath().toString();

        if (
            path.endsWith("/replays") ||
            path.endsWith("/keyframes") ||
            path.contains("/keyframes/x") ||
            path.contains("/keyframes/y") ||
            path.contains("/keyframes/z") ||
            path.contains("/keyframes/item_main_hand") ||
            path.contains("/keyframes/item_off_hand") ||
            path.contains("/keyframes/item_head") ||
            path.contains("/keyframes/item_chest") ||
            path.contains("/keyframes/item_legs") ||
            path.contains("/keyframes/item_feet") ||
            path.endsWith("/actor") ||
            path.endsWith("/enabled") ||
            path.endsWith("/form")
        ) {
            return true;
        }

        /* Specifically for overwriting full replay like what's done when recording
         * data in the world! */
        if (value.getParentValue() != null && value.getParentValue().getId().equals("replays"))
        {
            return true;
        }

        while (value != null)
        {
            if (value instanceof Clips clips && clips.getFactory() == BBSMod.getFactoryActionClips())
            {
                return true;
            }

            value = value.getParentValue();
        }

        return false;
    }

    /**
     * Remove any child tree entries if one of the parents is present already.
     * For example, let's say the undo submitted at the same time:
     *
     * - film.clips
     * - film.clips.0
     * - film.clips.0.duration
     *
     * There is no point in caching .0 and .0.duration since films .clips will get
     * cached anyway. Therefore, it's smart to eliminate those from the cache, and
     * submit only films.clips.
     */
    private void reduceUndoRedundancy()
    {
        Iterator<BaseValue> it = this.cachedValues.keySet().iterator();

        while (it.hasNext())
        {
            BaseValue value = it.next().getParentValue();
            boolean remove = false;

            while (value != null)
            {
                if (this.cachedValues.containsKey(value))
                {
                    remove = true;

                    break;
                }

                value = value.getParentValue();
            }

            if (remove)
            {
                it.remove();
            }
        }
    }

    public void markLastUndoNoMerging()
    {
        IUndo<ValueGroup> undo = this.undoManager.getCurrentUndo();

        if (undo != null)
        {
            undo.noMerging();
        }
    }
}
