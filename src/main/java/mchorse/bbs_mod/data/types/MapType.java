package mchorse.bbs_mod.data.types;

import mchorse.bbs_mod.data.DataStorageContext;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;

public class MapType extends BaseType implements Iterable<Map.Entry<String, BaseType>>
{
    public final Map<String, BaseType> elements;

    /* Accessors */

    public MapType()
    {
        this(true);
    }

    public MapType(boolean hash)
    {
        this.elements = hash
            ? new HashMap<>()
            : new LinkedHashMap<>();
    }

    public boolean isEmpty()
    {
        return this.elements.isEmpty();
    }

    public int size()
    {
        return this.elements.size();
    }

    public BaseType get(String key)
    {
        return this.elements.get(key);
    }

    public void put(String key, BaseType type)
    {
        if (key == null || type == null)
        {
            return;
        }

        this.elements.put(key, type);
    }

    public void remove(String key)
    {
        this.elements.remove(key);
    }

    public boolean has(String key)
    {
        return this.has(key, -1);
    }

    public boolean has(String key, int type)
    {
        BaseType value = this.elements.get(key);

        if (value == null)
        {
            return false;
        }

        return type < 0 || value.getTypeId() == type;
    }

    public void combine(MapType map)
    {
        for (Map.Entry<String, BaseType> entry : map)
        {
            this.put(entry.getKey(), entry.getValue().copy());
        }
    }

    public Set<String> keys()
    {
        return this.elements.keySet();
    }

    /* Byte array accessors */

    public void putByteArray(String key, byte[] value)
    {
        this.put(key, new ByteArrayType(value));
    }

    public byte[] getByteArray(String key)
    {
        return this.getByteArray(key, ByteArrayType.DEFAULT);
    }

    public byte[] getByteArray(String key, byte[] defaultValue)
    {
        BaseType value = this.get(key);

        return BaseType.is(value, BaseType.TYPE_BYTE_ARRAY) ? ((ByteArrayType) value).value : defaultValue;
    }

    /* Boolean accessors */

    public void putBool(String key, boolean value)
    {
        this.put(key, new ByteType(value));
    }

    public boolean getBool(String key)
    {
        return this.getBool(key, false);
    }

    public boolean getBool(String key, boolean defaultValue)
    {
        BaseType value = this.get(key);

        return BaseType.isNumeric(value) ? value.asNumeric().boolValue() : defaultValue;
    }

    /* Byte accessors */

    public void putByte(String key, byte value)
    {
        this.put(key, new ByteType(value));
    }

    public byte getByte(String key)
    {
        return this.getByte(key, (byte) 0);
    }

    public byte getByte(String key, byte defaultValue)
    {
        BaseType value = this.get(key);

        return BaseType.isNumeric(value) ? value.asNumeric().byteValue() : defaultValue;
    }

    /* Short accessors */

    public void putShort(String key, short value)
    {
        this.put(key, new ShortType(value));
    }

    public short getShort(String key)
    {
        return this.getShort(key, (short) 0);
    }

    public short getShort(String key, short defaultValue)
    {
        BaseType value = this.get(key);

        return BaseType.isNumeric(value) ? value.asNumeric().shortValue() : defaultValue;
    }

    /* Int accessors */

    public void putInt(String key, int value)
    {
        this.put(key, new IntType(value));
    }

    public int getInt(String key)
    {
        return this.getInt(key, 0);
    }

    public int getInt(String key, int defaultValue)
    {
        BaseType value = this.get(key);

        return BaseType.isNumeric(value) ? value.asNumeric().intValue() : defaultValue;
    }

    /* Float accessors */

    public void putFloat(String key, float value)
    {
        this.put(key, new FloatType(value));
    }

    public float getFloat(String key)
    {
        return this.getFloat(key, 0F);
    }

    public float getFloat(String key, float defaultValue)
    {
        BaseType value = this.get(key);

        return BaseType.isNumeric(value) ? value.asNumeric().floatValue() : defaultValue;
    }

    /* Long accessors */

    public void putLong(String key, long value)
    {
        this.put(key, new LongType(value));
    }

    public long getLong(String key)
    {
        return this.getLong(key, 0L);
    }

    public long getLong(String key, long defaultValue)
    {
        BaseType value = this.get(key);

        return BaseType.isNumeric(value) ? value.asNumeric().longValue() : defaultValue;
    }

    /* Double accessors */

    public void putDouble(String key, double value)
    {
        this.put(key, new DoubleType(value));
    }

    public double getDouble(String key)
    {
        return this.getDouble(key, 0D);
    }

    public double getDouble(String key, double defaultValue)
    {
        BaseType value = this.get(key);

        return BaseType.isNumeric(value) ? value.asNumeric().doubleValue() : defaultValue;
    }

    /* String accessors */

    public void putString(String key, String value)
    {
        this.put(key, new StringType(value));
    }

    public String getString(String key)
    {
        return this.getString(key, "");
    }

    public String getString(String key, String defaultValue)
    {
        BaseType value = this.get(key);

        return BaseType.isString(value) ? ((StringType) value).value : defaultValue;
    }

    /* Map and list accessors */

    public ListType getList(String key)
    {
        return this.getList(key, new ListType());
    }

    public ListType getList(String key, ListType defaultValue)
    {
        BaseType value = this.get(key);

        return BaseType.isList(value) ? (ListType) value : defaultValue;
    }

    public MapType getMap(String key)
    {
        return this.getMap(key, new MapType());
    }

    public MapType getMap(String key, MapType defaultValue)
    {
        BaseType value = this.get(key);

        return BaseType.isMap(value) ? (MapType) value : defaultValue;
    }

    /* Implementations */

    @Override
    public void traverseKeys(DataStorageContext context)
    {
        super.traverseKeys(context);

        for (Map.Entry<String, BaseType> entry : this.elements.entrySet())
        {
            context.put(entry.getKey());

            entry.getValue().traverseKeys(context);
        }
    }

    @Override
    public byte getTypeId()
    {
        return BaseType.TYPE_MAP;
    }

    @Override
    public BaseType copy()
    {
        MapType map = new MapType();

        for (Map.Entry<String, BaseType> entry : this)
        {
            map.put(entry.getKey(), entry.getValue().copy());
        }

        return map;
    }

    @Override
    public void read(DataStorageContext context) throws IOException
    {
        this.elements.clear();

        for (int i = 0, count = context.in.readInt(); i < count; i++)
        {
            String key = context.readKey();
            BaseType value = BaseType.fromData(context);

            this.elements.put(key, value);
        }
    }

    @Override
    public void write(DataStorageContext context) throws IOException
    {
        context.out.writeInt(this.elements.size());

        for (Map.Entry<String, BaseType> entry : this.elements.entrySet())
        {
            context.writeIndex(entry.getKey());
            BaseType.toData(context, entry.getValue());
        }
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj instanceof MapType)
        {
            return this.elements.equals(((MapType) obj).elements);
        }

        return super.equals(obj);
    }

    @Override
    public String toString()
    {
        StringJoiner joiner = new StringJoiner(",");

        for (Map.Entry<String, BaseType> entry : this)
        {
            joiner.add(entry.getKey() + ":" + entry.getValue().toString());
        }

        return "{" + joiner.toString() + "}";
    }

    @Override
    public Iterator<Map.Entry<String, BaseType>> iterator()
    {
        return this.elements.entrySet().iterator();
    }
}