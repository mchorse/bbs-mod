package mchorse.bbs_mod.ui.framework.elements.overlay;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.framework.elements.input.list.UISearchList;
import mchorse.bbs_mod.ui.framework.elements.input.list.UIStringList;
import mchorse.bbs_mod.utils.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import net.fabricmc.loader.api.FabricLoader;
import mchorse.bbs_mod.ui.framework.UIContext;

public class UIVanillaSoundOverlayPanel extends UIOverlayPanel
{
    public UISearchList<String> sounds;
    
    private Consumer<String> callback;
    private boolean none;
    private UIContext context;
    private UISoundOverlayPanel soundOverlayPanel;

    public UIVanillaSoundOverlayPanel(Consumer<String> callback)
    {
        this(callback, true);
    }
    
    public UIVanillaSoundOverlayPanel(Consumer<String> callback, boolean none)
    {
        super(UIKeys.VANILLA_SOUND_SELECTION_TITLE);
        
        this.callback = callback;
        this.none = none;
        
        this.sounds = new UISearchList<>(new UIStringList((list) -> this.accept(list.get(0))));
        this.sounds.label(UIKeys.GENERAL_SEARCH).full(this.content).x(6).w(1F, -12);
        
        List<String> soundFiles = this.loadVanillaSoundFiles();
        this.sounds.list.add(soundFiles);
        this.sounds.list.sort();
        this.sounds.list.scroll.scrollSpeed *= 2;
        
        if (this.none)
        {
            this.sounds.list.getList().add(0, UIKeys.GENERAL_NONE.get());
            this.sounds.list.update();
        }
        
        this.content.add(this.sounds);
    }
    
    public UIVanillaSoundOverlayPanel(Consumer<String> callback, boolean none, UIContext context, UISoundOverlayPanel soundOverlayPanel)
    {
        this(callback, none);
        this.context = context;
        this.soundOverlayPanel = soundOverlayPanel;
    }
    
    public UIVanillaSoundOverlayPanel set(String string)
    {
        this.sounds.filter("", true);
        this.sounds.list.setCurrentScroll(string);

        if (this.none && this.sounds.list.isDeselected())
        {
            this.sounds.list.setIndex(0);
        }

        return this;
    }
    
    public UIVanillaSoundOverlayPanel callback(Consumer<String> callback)
    {
        this.callback = callback;
        
        return this;
    }
    
    // Handle sound selection
    protected void accept(String string)
    {
        if (this.callback != null)
        {
            String selectedSound = this.getValue(string);
            
            // Log selected sound
            System.out.println("Selected vanilla sound: " + selectedSound);
            
            if (!selectedSound.isEmpty())
            {
                this.checkAndCopySoundFile(selectedSound);
            }
            
            this.callback.accept(selectedSound);
            
            this.refreshSoundOverlayPanel();
        }
        
        this.close();
    }
    
    // Refresh the sound overlay panel after a delay
    private void refreshSoundOverlayPanel()
    {
        if (this.context != null)
        {
            ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
            scheduler.schedule(() -> {
                net.minecraft.client.MinecraftClient.getInstance().execute(() -> {
                    // Clear the sound cache first
                    BBSModClient.getSounds().deleteSounds();
                    
                    // Use the soundOverlayPanel reference if available
                    UISoundOverlayPanel soundOverlayPanel = this.soundOverlayPanel;
                    
                    // If not available, try to find it in the context
                    if (soundOverlayPanel == null)
                    {
                        for (UIOverlayPanel panel : this.context.menu.getRoot().getChildren(UIOverlayPanel.class))
                        {
                            if (panel instanceof UISoundOverlayPanel)
                            {
                                soundOverlayPanel = (UISoundOverlayPanel) panel;
                                break;
                            }
                        }
                    }
                    
                    if (soundOverlayPanel != null)
                    {
                        // Use the refreshSoundList method instead of manually updating
                        soundOverlayPanel.refreshSoundList();
                    }
                });
            }, 1, TimeUnit.SECONDS);
            
            scheduler.shutdown();
        }
    }
    
    protected String getValue()
    {
        return this.getValue(this.sounds.list.getCurrentFirst());
    }
    
    protected String getValue(String string)
    {
        if (!this.none)
        {
            return string;
        }
        
        return this.sounds.list.getIndex() == 0 ? "" : string;
    }
    
    @Override
    public void close()
    {
        super.close();
    }
    
    // Load vanilla sound files from game assets
    private List<String> loadVanillaSoundFiles()
    {
        List<String> soundFiles = new ArrayList<>();
        
        try
        {
            File gameDir = FabricLoader.getInstance().getGameDir().toFile();
            File indexesDir = new File(gameDir, "assets/indexes");
            
            if (indexesDir.exists() && indexesDir.isDirectory())
            {
                File[] files = indexesDir.listFiles();
                
                if (files != null && files.length > 0)
                {
                    for (File file : files)
                    {
                        if (file.getName().endsWith(".json"))
                        {
                            // Log found JSON file
                            System.out.println("Found JSON file: " + file.getAbsolutePath());
                            String jsonText = IOUtils.readText(file);
                            JsonObject jsonObject = JsonParser.parseString(jsonText).getAsJsonObject();
                            JsonObject objects = jsonObject.getAsJsonObject("objects");
                            
                            int soundCount = 0;
                            for (String key : objects.keySet())
                            {
                                if (key.contains("sounds"))
                                {
                                    String fileName = extractFileName(key);
                                    if (fileName != null && !fileName.isEmpty())
                                    {
                                        soundFiles.add(fileName);
                                        soundCount++;
                                    }
                                }
                            }
                            
                            break;
                        }
                    }
                }
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        
        return soundFiles;
    }
    
    private String extractFileName(String path)
    {
        if (!path.contains("sounds"))
        {
            return null;
        }
        
        String[] parts = path.split("/");
        
        if (parts.length < 2)
        {
            return null;
        }
        
        String fileName = parts[parts.length - 1];
        
        if (fileName.endsWith(".ogg"))
        {
            return fileName.substring(0, fileName.length() - 4);
        }
        
        return null;
    }
    
    // Check if sound exists and copy if not
    private void checkAndCopySoundFile(String soundName)
    {
        try
        {
            if (isSoundExistsInAudioEvents(soundName))
            {
                return;
            }
            
            String hash = getSoundFileHash(soundName);
            if (hash == null || hash.isEmpty())
            {
                return;
            }
            
            File gameDir = FabricLoader.getInstance().getGameDir().toFile();
            String hashPrefix = hash.substring(0, 2);
            File objectsDir = new File(gameDir, "assets/objects/" + hashPrefix);
            File sourceFile = new File(objectsDir, hash);
            
            if (!sourceFile.exists())
            {
                return;
            }
            
            File audioDir = new File(gameDir, "config/bbs/assets/audio");
            if (!audioDir.exists())
            {
                audioDir.mkdirs();
            }
            
            File targetFile = new File(audioDir, soundName + ".ogg");
            
            Files.copy(sourceFile.toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
    
    private boolean isSoundExistsInAudioEvents(String soundName)
    {
        try
        {
            Set<String> soundEvents = getSoundEvents();
            
            for (String event : soundEvents)
            {
                String eventName = extractEventName(event);
                if (soundName.equals(eventName))
                {
                    return true;
                }
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        
        return false;
    }
    
    private Set<String> getSoundEvents()
    {
        Set<String> locations = new java.util.HashSet<>();

        for (Link link : BBSMod.getProvider().getLinksFromPath(Link.assets("audio")))
        {
            String pathLower = link.path.toLowerCase();

            boolean supportedExtension = pathLower.endsWith(".wav") || pathLower.endsWith(".ogg");

            if (supportedExtension)
            {
                locations.add(link.toString());
            }
        }

        return locations;
    }
    
    private String extractEventName(String event)
    {
        String[] parts = event.split("/");
        
        if (parts.length > 0)
        {
            String fileName = parts[parts.length - 1];
            
            if (fileName.endsWith(".ogg"))
            {
                return fileName.substring(0, fileName.length() - 4);
            }
            else if (fileName.endsWith(".wav"))
            {
                return fileName.substring(0, fileName.length() - 4);
            }
        }
        
        return event;
    }
    
    // Get the hash of a sound file from the assets index
    private String getSoundFileHash(String soundName)
    {
        try
        {
            File gameDir = FabricLoader.getInstance().getGameDir().toFile();
            File indexesDir = new File(gameDir, "assets/indexes");
            
            if (indexesDir.exists() && indexesDir.isDirectory())
            {
                File[] files = indexesDir.listFiles();
                
                if (files != null && files.length > 0)
                {
                    for (File file : files)
                    {
                        if (file.getName().endsWith(".json"))
                        {
                            String jsonText = IOUtils.readText(file);
                            JsonObject jsonObject = JsonParser.parseString(jsonText).getAsJsonObject();
                            JsonObject objects = jsonObject.getAsJsonObject("objects");
                            
                            for (String key : objects.keySet())
                            {
                                if (key.contains("sounds"))
                                {
                                    String fileName = extractFileName(key);
                                    if (fileName != null && fileName.equals(soundName))
                                    {
                                        JsonObject soundObject = objects.getAsJsonObject(key);
                                        return soundObject.get("hash").getAsString();
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        
        return null;
    }
}