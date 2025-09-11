package mchorse.bbs_mod.forms.properties;

import mchorse.bbs_mod.data.IMapSerializable;
import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.utils.keyframes.factories.KeyframeFactories;

public class AnchorProperty extends BaseTweenProperty<AnchorProperty.Anchor>
{
    public AnchorProperty(Form form, String key)
    {
        super(form, key, new Anchor(), KeyframeFactories.ANCHOR);
    }

    public static class Anchor implements IMapSerializable
    {
        public int actor = -1;
        public String attachment = "";
        public boolean translate = false;
        public boolean scale = false;

        public int previousActor = -2;
        public String previousAttachment = "";
        public boolean previousTranslate = false;
        public boolean previousScale = false;
        public float x;

        @Override
        public boolean equals(Object obj)
        {
            if (super.equals(obj))
            {
                return true;
            }

            if (obj instanceof Anchor anchor)
            {
                return this.actor == anchor.actor
                    && this.attachment.equals(anchor.attachment)
                    && this.translate == anchor.translate
                    && this.scale == anchor.scale;
            }

            return false;
        }

        @Override
        public void fromData(MapType data)
        {
            this.actor = data.getInt("actor");
            this.attachment = data.getString("attachment");
            this.translate = data.getBool("translate", false);
            this.scale = data.getBool("scale", false);
        }

        @Override
        public void toData(MapType data)
        {
            data.putInt("actor", this.actor);
            data.putString("attachment", this.attachment);
            data.putBool("translate", this.translate);
            data.putBool("scale", this.scale);
        }
    }
}