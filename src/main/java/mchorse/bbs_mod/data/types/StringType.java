package mchorse.bbs_mod.data.types;

import mchorse.bbs_mod.data.DataStorageContext;
import mchorse.bbs_mod.data.DataToString;

import java.io.IOException;

public class StringType extends BaseType
{
    public String value = "";

    public StringType()
    {}

    public StringType(String value)
    {
        this.value = value == null ? "" : value;
    }

    @Override
    public byte getTypeId()
    {
        return BaseType.TYPE_STRING;
    }

    @Override
    public BaseType copy()
    {
        return new StringType(this.value);
    }

    @Override
    public void read(DataStorageContext context) throws IOException
    {
        this.value = context.in.readUTF();
    }

    @Override
    public void write(DataStorageContext context) throws IOException
    {
        context.out.writeUTF(this.value);
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj instanceof StringType)
        {
            return this.value.equals(((StringType) obj).value);
        }

        return super.equals(obj);
    }

    @Override
    public String toString()
    {
        return DataToString.escapeQuoted(this.value);
    }
}