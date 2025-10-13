package mchorse.bbs_mod.ui.framework.elements.input.keyframes.factories;

import mchorse.bbs_mod.forms.forms.utils.ParticleSettings;
import mchorse.bbs_mod.ui.forms.editors.utils.UIParticleSettings;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframes;
import mchorse.bbs_mod.utils.keyframes.Keyframe;
import net.minecraft.util.Identifier;

public class UIParticleSettingsKeyframeFactory extends UIKeyframeFactory<ParticleSettings>
{
    private UIParticleSettings settings;

    public UIParticleSettingsKeyframeFactory(Keyframe<ParticleSettings> keyframe, UIKeyframes editor)
    {
        super(keyframe, editor);

        this.settings = new UIParticleSettings();
        this.settings.setSettings(keyframe.getValue());

        this.scroll.add(this.settings);
    }

    public static class UIParticleSettingsEditor extends UIParticleSettings
    {
        private final UIParticleSettingsKeyframeFactory editor;

        public UIParticleSettingsEditor(UIParticleSettingsKeyframeFactory editor)
        {
            super();

            this.editor = editor;
        }

        @Override
        protected void setParticle(Identifier id)
        {
            this.editor.keyframe.preNotify();
            super.setParticle(id);
            this.editor.keyframe.postNotify();
        }

        @Override
        protected void setArguments(String args)
        {
            this.editor.keyframe.preNotify();
            super.setArguments(args);
            this.editor.keyframe.postNotify();
        }
    }
}