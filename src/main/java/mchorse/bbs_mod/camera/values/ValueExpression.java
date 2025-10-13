package mchorse.bbs_mod.camera.values;

import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.data.types.StringType;
import mchorse.bbs_mod.math.IExpression;
import mchorse.bbs_mod.math.MathBuilder;
import mchorse.bbs_mod.settings.values.base.BaseValue;

public class ValueExpression extends BaseValue
{
    public IExpression expression;
    public MathBuilder builder;
    public boolean lastError;

    public ValueExpression(String id, MathBuilder builder)
    {
        super(id);

        this.builder = builder;
    }

    public boolean isErrored()
    {
        return this.lastError;
    }

    public IExpression get()
    {
        return this.expression;
    }

    public void set(String expression) throws Exception
    {
        this.expression = this.builder.parse(expression);
    }

    public void setExpression(String string)
    {
        this.preNotify();
        this.setExpressionPrivate(string);
        this.postNotify();
    }

    private void setExpressionPrivate(String string)
    {
        try
        {
            if (string.isEmpty())
            {
                this.expression = null;
            }
            else
            {
                this.set(string);
            }

            this.lastError = false;
        }
        catch (Exception e)
        {
            this.expression = null;
            this.lastError = true;
        }
    }

    @Override
    public BaseType toData()
    {
        return new StringType(this.toString());
    }

    @Override
    public void fromData(BaseType data)
    {
        this.setExpressionPrivate(data.asString());
    }

    @Override
    public String toString()
    {
        return this.expression == null ? "" : this.expression.toString();
    }
}