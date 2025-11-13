package mchorse.bbs_mod.utils.clips;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public class ClipData
{
    private Map<String, Object> map = new ConcurrentHashMap<>();

    public void clear()
    {
        this.map.clear();
    }

    public <T> T get(String key, Supplier<T> supplier)
    {
        Object o = this.map.get(key);

        if (o == null)
        {
            o = supplier.get();

            this.map.put(key, o);
        }

        return (T) o;
    }
}