package mchorse.bbs_mod.utils.watchdog;

import mchorse.bbs_mod.utils.Pair;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class WatchDogProxy implements IWatchDogListener
{
    private List<IWatchDogListener> listeners = new ArrayList<>();
    private List<Pair<Path, WatchDogEvent>> queue = new ArrayList<>();
    private int tick;

    public void register(IWatchDogListener listener)
    {
        this.listeners.add(listener);
    }

    public void tick()
    {
        if (this.tick == 0)
        {
            for (Pair<Path, WatchDogEvent> pair : this.queue)
            {
                for (IWatchDogListener listener : this.listeners)
                {
                    listener.accept(pair.a, pair.b);
                }
            }
        }

        this.tick -= 1;
    }

    @Override
    public void accept(Path path, WatchDogEvent event)
    {
        /* this.tick = 5;

        this.queue.add(new Pair<>(path, event)); */
        for (IWatchDogListener listener : this.listeners)
        {
            listener.accept(path, event);
        }
    }
}