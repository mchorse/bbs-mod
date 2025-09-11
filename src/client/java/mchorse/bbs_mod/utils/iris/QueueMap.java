package mchorse.bbs_mod.utils.iris;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

public class QueueMap<K, V> implements Map<K, V>
{
    private Map<K, V> map;

    public QueueMap(Map<K, V> map)
    {
        this.map = map;
    }

    @Override
    public int size()
    {
        return this.map.size();
    }

    @Override
    public boolean isEmpty()
    {
        return this.map.isEmpty();
    }

    @Override
    public boolean containsKey(Object key)
    {
        return this.map.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value)
    {
        return this.map.containsValue(value);
    }

    @Override
    public V get(Object key)
    {
        return this.map.get(key);
    }

    @Override
    public @Nullable V put(K key, V value)
    {
        ShaderCurves.ShaderVariable variable = ShaderCurves.variableMap.get(key);

        if (variable != null && value instanceof String string)
        {
            try
            {
                variable.defaultValue = Float.parseFloat(string);
            }
            catch (Exception e)
            {}
        }

        return this.map.put(key, value);
    }

    @Override
    public V remove(Object key)
    {
        return this.map.remove(key);
    }

    @Override
    public void putAll(@NotNull Map<? extends K, ? extends V> m)
    {
        for (K k : m.keySet())
        {
            ShaderCurves.ShaderVariable variable = ShaderCurves.variableMap.get(k);

            if (variable != null && m.get(k) instanceof String string)
            {
                try
                {
                    variable.defaultValue = Float.parseFloat(string);
                }
                catch (Exception e)
                {}
            }
        }

        this.map.putAll(m);
    }

    @Override
    public void clear()
    {
        this.map.clear();
    }

    @Override
    public @NotNull Set<K> keySet()
    {
        return this.map.keySet();
    }

    @Override
    public @NotNull Collection<V> values()
    {
        return this.map.values();
    }

    @Override
    public @NotNull Set<Entry<K, V>> entrySet()
    {
        return this.map.entrySet();
    }
}