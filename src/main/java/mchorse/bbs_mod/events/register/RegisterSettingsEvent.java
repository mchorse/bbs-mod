package mchorse.bbs_mod.events.register;

import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.settings.SettingsBuilder;
import mchorse.bbs_mod.ui.utils.icons.Icon;

import java.io.File;
import java.util.function.Consumer;

public class RegisterSettingsEvent
{
    public void register(Icon icon, String id, Consumer<SettingsBuilder> consumer)
    {
        BBSMod.setupConfig(icon, id, new File(BBSMod.getSettingsFolder(), id + ".json"), consumer);
    }
}
