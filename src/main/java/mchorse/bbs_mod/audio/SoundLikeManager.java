package mchorse.bbs_mod.audio;

import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.data.storage.DataFileStorage;
import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.data.types.ListType;
import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.data.types.StringType;
import mchorse.bbs_mod.resources.Link;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * Manages audio files' like/unlike status
 */
public class SoundLikeManager
{
    private static final String LIKED_SOUNDS_FILE = "liked_sounds.dat";
    private static final Logger LOGGER = LogUtils.getLogger();

    private final LinkedHashMap<String, LikedSound> likedSounds = new LinkedHashMap<>();
    private DataFileStorage storage;
    private boolean loadErrorLogged = false;
    private boolean saveErrorLogged = false;
    
    public SoundLikeManager()
    {
        File dataDir = new File(BBSMod.getSettingsFolder().getParentFile(), "data");
        File audioDir = new File(dataDir, "audio");
        if (!audioDir.exists())
        {
            audioDir.mkdirs();
        }

        File configFile = new File(audioDir, LIKED_SOUNDS_FILE);
        this.storage = new DataFileStorage(configFile);

        loadLikedSounds();
    }
    
    private void loadLikedSounds()
    {
        if (!this.storage.getFile().exists())
        {
            return;
        }

        try
        {
            BaseType data = this.storage.read();
            if (data != null && data.isList())
            {
                this.likedSounds.clear();
                ListType listType = data.asList();

                for (BaseType item : listType)
                {
                    if (item.isString())
                    {
                        this.addLoadedLikedSound(item.asString(), null);
                    }
                    else if (item.isMap())
                    {
                        MapType map = item.asMap();
                        BaseType pathEntry = map.get("path");

                        if (pathEntry != null && pathEntry.isString())
                        {
                            String path = pathEntry.asString();
                            BaseType nameEntry = map.get("name");
                            String display = nameEntry != null && nameEntry.isString() ? nameEntry.asString() : null;
                            this.addLoadedLikedSound(path, display);
                        }
                    }
                }
            }
        }
        catch (Exception e)
        {
            if (!this.loadErrorLogged)
            {
                LOGGER.error("Failed to load liked sounds from " + this.storage.getFile().getAbsolutePath(), e);
                this.loadErrorLogged = true;
            }
        }
    }
    
    private void saveLikedSounds()
    {
        try
        {
            ListType listData = new ListType();

            for (LikedSound sound : this.likedSounds.values())
            {
                MapType map = new MapType();

                map.put("path", new StringType(sound.getPath()));
                map.put("name", new StringType(sound.getDisplayName()));
                listData.add(map);
            }

            this.storage.write(listData);
        }
        catch (Exception e)
        {
            if (!this.saveErrorLogged)
            {
                LOGGER.error("Failed to save liked sounds to " + this.storage.getFile().getAbsolutePath(), e);

                this.saveErrorLogged = true;
            }
        }
    }

    public void setSoundLiked(String soundName, String displayName, boolean liked)
    {
        String normalized = this.normalizePath(soundName);

        if (normalized == null)
        {
            return;
        }

        this.setSoundLikedNormalized(normalized, displayName, liked);
    }

    private void setSoundLikedNormalized(String normalized, String displayName, boolean liked)
    {
        if (displayName == null)
        {
            displayName = this.getDefaultDisplayName(normalized);
        }

        boolean changed;

        if (liked)
        {
            LikedSound previous = this.likedSounds.put(normalized, new LikedSound(normalized, displayName));
            changed = previous == null || !previous.getDisplayName().equals(displayName);
        }
        else
        {
            changed = this.likedSounds.remove(normalized) != null;
        }

        if (changed)
        {
            this.saveLikedSounds();
        }
    }
    
    public boolean isSoundLiked(String soundName)
    {
        String normalized = this.normalizePath(soundName);

        return normalized != null && this.likedSounds.containsKey(normalized);
    }
    
    public boolean toggleSoundLiked(String soundName)
    {
        return this.toggleSoundLiked(soundName, null);
    }

    public boolean toggleSoundLiked(String soundName, String displayName)
    {
        String normalized = this.normalizePath(soundName);

        if (normalized == null)
        {
            return false;
        }

        boolean liked = !this.likedSounds.containsKey(normalized);
        this.setSoundLikedNormalized(normalized, displayName, liked);
        LOGGER.info("Toggled liked status for sound: " + normalized + " to " + (liked ? "liked" : "unliked"));

        return liked;
    }
    
    public String getDisplayName(String soundName)
    {
        String normalized = this.normalizePath(soundName);

        if (normalized == null)
        {
            return null;
        }

        LikedSound sound = this.likedSounds.get(normalized);

        return sound != null ? sound.getDisplayName() : null;
    }

    public java.util.List<LikedSound> getLikedSounds()
    {
        return new ArrayList<>(this.likedSounds.values());
    }
    
    public void removeSound(String soundName)
    {
        String normalized = this.normalizePath(soundName);

        if (normalized == null)
        {
            return;
        }

        if (this.likedSounds.remove(normalized) != null)
        {
            this.saveLikedSounds();
            LOGGER.info("Removed sound: " + normalized);
        }
    }

    private String normalizePath(String soundName)
    {
        if (soundName == null || soundName.isEmpty())
        {
            return null;
        }

        return Link.create(soundName).toString();
    }

    private String getDefaultDisplayName(String soundName)
    {
        Link link = Link.create(soundName);

        if (link.path == null || link.path.isEmpty())
        {
            return link.toString();
        }

        return link.path;
    }

    private void addLoadedLikedSound(String soundPath, String displayName)
    {
        String normalized = this.normalizePath(soundPath);

        if (normalized == null)
        {
            return;
        }

        if (displayName == null)
        {
            displayName = this.getDefaultDisplayName(normalized);
        }

        this.likedSounds.put(normalized, new LikedSound(normalized, displayName));
    }

    public static class LikedSound
    {
        private final String path;
        private final String displayName;

        public LikedSound(String path, String displayName)
        {
            this.path = path;
            this.displayName = displayName;
        }

        public String getPath()
        {
            return this.path;
        }

        public String getDisplayName()
        {
            return this.displayName;
        }
    }
}