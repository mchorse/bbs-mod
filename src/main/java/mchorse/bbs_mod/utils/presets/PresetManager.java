package mchorse.bbs_mod.utils.presets;

import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.data.DataToString;
import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.data.types.MapType;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class PresetManager
{
    public static final PresetManager CLIPS = new PresetManager(BBSMod.getSettingsPath("presets/clips"));
    public static final PresetManager BODY_PARTS = new PresetManager(BBSMod.getSettingsPath("presets/body_parts"));
    public static final PresetManager TEXTURES = new PresetManager(BBSMod.getSettingsPath("presets/textures"));
    public static final PresetManager KEYFRAMES = new PresetManager(BBSMod.getSettingsPath("presets/keyframes"));
    public static final PresetManager GUNS = new PresetManager(BBSMod.getSettingsPath("presets/guns"));
    public static final PresetManager ANIMATION_STATES = new PresetManager(BBSMod.getSettingsPath("presets/animation_states"));

    private File folder;

    public PresetManager(File folder)
    {
        this.folder = folder;

        this.folder.mkdirs();
    }

    public File getFolder()
    {
        return this.folder;
    }

    public MapType load(String id)
    {
        File file = new File(this.folder, id + ".json");

        if (!file.exists())
        {
            return null;
        }

        try
        {
            BaseType read = DataToString.read(file);

            if (read.isMap())
            {
                return read.asMap();
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        return null;
    }

    public void save(String id, MapType mapType)
    {
        DataToString.writeSilently(new File(this.folder, id + ".json"), mapType, true);
    }

    public List<String> getKeys()
    {
        ArrayList<String> strings = new ArrayList<>();
        File[] files = this.folder.listFiles();

        if (files == null)
        {
            return strings;
        }

        for (File file : files)
        {
            String name = file.getName();

            if (name.endsWith(".json"))
            {
                strings.add(name.substring(0, name.length() - 5));
            }
        }

        return strings;
    }
}