package mchorse.bbs_mod;

import mchorse.bbs_mod.utils.watchdog.WatchDog;
import net.minecraft.client.MinecraftClient;

import java.io.File;

public class BBSResources
{
    private static WatchDog watchDog;

    public static void init()
    {
        setupWatchdog();

        BBSModClient.getFormCategories().setup();
    }

    public static void setupWatchdog()
    {
        File assetsFolder = BBSMod.getAssetsFolder();

        watchDog = new WatchDog(assetsFolder, false, (runnable) -> MinecraftClient.getInstance().execute(runnable));
        watchDog.getProxy().register(BBSModClient.getTextures());
        watchDog.getProxy().register(BBSModClient.getModels());
        watchDog.getProxy().register(BBSModClient.getSounds());
        watchDog.getProxy().register(BBSModClient.getFormCategories());

        watchDog.start();
    }

    public static void stopWatchdog()
    {
        if (watchDog != null)
        {
            watchDog.stop();
            watchDog = null;
        }
    }

    public static void tick()
    {
        if (watchDog != null)
        {
            watchDog.getProxy().tick();
        }
    }
}