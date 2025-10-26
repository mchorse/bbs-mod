package mchorse.bbs_mod.camera.values;

import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.settings.values.base.BaseValue;
import mchorse.bbs_mod.settings.values.core.ValueGroup;
import mchorse.bbs_mod.utils.keyframes.KeyframeChannel;
import mchorse.bbs_mod.utils.keyframes.factories.KeyframeFactories;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ValueChannels extends ValueGroup
{
    private List<KeyframeChannel<Double>> list = new ArrayList<>();

    public ValueChannels(String id)
    {
        super(id);
    }

    public KeyframeChannel<Double> addChannel(String s)
    {
        KeyframeChannel<Double> channel = new KeyframeChannel<>(s, KeyframeFactories.DOUBLE);

        this.preNotify();
        this.add(channel);
        this.postNotify();

        return channel;
    }

    public void removeChannel(KeyframeChannel channel)
    {
        BaseValue baseValue = this.get(channel.getId());

        if (baseValue == channel)
        {
            this.preNotify();
            this.remove(baseValue);
            this.postNotify();
        }
    }

    public List<KeyframeChannel<Double>> getChannels()
    {
        this.list.clear();

        for (BaseValue baseValue : this.getAll())
        {
            if (baseValue instanceof KeyframeChannel<?> channel && channel.getFactory() == KeyframeFactories.DOUBLE)
            {
                this.list.add((KeyframeChannel<Double>) channel);
            }
        }

        this.list.sort((a, b) -> a.getId().compareToIgnoreCase(b.getId()));

        return this.list;
    }

    @Override
    public void fromData(BaseType data)
    {
        this.removeAll();

        if (data.isMap())
        {
            MapType map = data.asMap();
            Set<String> keys = new HashSet<>(map.keys());

            for (String key : keys)
            {
                String newKey = key.replaceAll("/", ".");

                if (!newKey.equals(key))
                {
                    map.put(newKey, map.get(key));
                    map.remove(key);
                }

                this.add(new KeyframeChannel<>(newKey, KeyframeFactories.DOUBLE));
            }
        }

        super.fromData(data);
    }
}