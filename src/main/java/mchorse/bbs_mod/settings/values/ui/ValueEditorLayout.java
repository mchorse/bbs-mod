package mchorse.bbs_mod.settings.values.ui;

import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.settings.values.base.BaseValue;

public class ValueEditorLayout extends BaseValue
{
    private boolean horizontal;
    private float mainSizeH = 0.66F;
    private float mainSizeV = 0.66F;
    private float editorSizeH = 0.5F;
    private float editorSizeV = 0.5F;

    public ValueEditorLayout(String id)
    {
        super(id);
    }

    public void setHorizontal(boolean horizontal)
    {
        BaseValue.edit(this, (v) -> this.horizontal = horizontal);
    }

    public void setMainSizeH(float mainSizeH)
    {
        BaseValue.edit(this, (v) -> this.mainSizeH = mainSizeH);
    }

    public void setMainSizeV(float mainSizeV)
    {
        BaseValue.edit(this, (v) -> this.mainSizeV = mainSizeV);
    }

    public void setEditorSizeH(float editorSizeH)
    {
        BaseValue.edit(this, (v) -> this.editorSizeH = editorSizeH);
    }

    public void setEditorSizeV(float editorSizeV)
    {
        BaseValue.edit(this, (v) -> this.editorSizeV = editorSizeV);
    }

    public boolean isHorizontal()
    {
        return this.horizontal;
    }

    public float getMainSizeH()
    {
        return this.mainSizeH;
    }

    public float getMainSizeV()
    {
        return this.mainSizeV;
    }

    public float getEditorSizeH()
    {
        return this.editorSizeH;
    }

    public float getEditorSizeV()
    {
        return this.editorSizeV;
    }

    @Override
    public BaseType toData()
    {
        MapType data = new MapType();

        data.putBool("horizontal", this.horizontal);
        data.putFloat("main_size_h", this.mainSizeH);
        data.putFloat("main_size_v", this.mainSizeV);
        data.putFloat("editor_size_h", this.editorSizeH);
        data.putFloat("editor_size_v", this.editorSizeV);

        return data;
    }

    @Override
    public void fromData(BaseType data)
    {
        if (data.isMap())
        {
            MapType map = data.asMap();

            this.horizontal = map.getBool("horizontal");
            this.mainSizeH = map.getFloat("main_size_h");
            this.mainSizeV = map.getFloat("main_size_v");
            this.editorSizeH = map.getFloat("editor_size_h");
            this.editorSizeV = map.getFloat("editor_size_v");
        }
    }
}