package mchorse.bbs_mod.events.register;

import mchorse.bbs_mod.resources.AssetProvider;

public class RegisterSourcePacksEvent
{
    public final AssetProvider provider;

    public RegisterSourcePacksEvent(AssetProvider provider)
    {
        this.provider = provider;
    }
}
