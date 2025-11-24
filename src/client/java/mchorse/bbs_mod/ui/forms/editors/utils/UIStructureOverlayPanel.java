package mchorse.bbs_mod.ui.forms.editors.utils;

import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIIcon;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIStringOverlayPanel;
import mchorse.bbs_mod.ui.utils.icons.Icons;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

public class UIStructureOverlayPanel extends UIStringOverlayPanel
{
    private UIIcon saveButton;

    private static Set<String> getStructureFiles()
    {
        Set<String> locations = new HashSet<>();

        try
        {
            for (Link link : BBSMod.getProvider().getLinksFromPath(Link.assets("structures")))
            {
                if (link.path.toLowerCase().endsWith(".nbt"))
                {
                    locations.add(link.toString());
                }
            }
        }
        catch (Exception e)
        {}

        return locations;
    }

    private static boolean isFromWorld(String world)
    {
        if (world == null || world.isEmpty())
        {
            return false;
        }

        Link link = Link.create(world);
        File worldFolder = BBSMod.getWorldFolder();

        if (worldFolder == null)
        {
            return false;
        }

        String relative = link.path.substring("structures/".length());
        File a = new File(worldFolder, "generated/minecraft/structures/" + relative);
        File b = new File(worldFolder, "generated/structures/" + relative);

        return a.exists() || b.exists();
    }

    public UIStructureOverlayPanel(IKey title, Consumer<Link> callback)
    {
        super(title, false, getStructureFiles(), (str) ->
        {
            if (callback != null)
            {
                callback.accept(str.isEmpty() ? null : Link.create(str));
            }
        });

        this.saveButton = new UIIcon(Icons.DOWNLOAD, (b) -> this.saveStructure());
        this.saveButton.tooltip(UIKeys.FORMS_EDITORS_STRUCTURE_SAVE_STRUCTURE);
        this.icons.add(this.saveButton);

        this.updateSaveButtonVisibility();
    }

    @Override
    public UIStringOverlayPanel set(Link link)
    {
        UIStringOverlayPanel set = super.set(link);

        this.updateSaveButtonVisibility();

        return set;
    }

    @Override
    public UIStringOverlayPanel set(String string)
    {
        UIStringOverlayPanel set = super.set(string);

        this.updateSaveButtonVisibility();

        return set;
    }

    @Override
    protected void accept(String string)
    {
        this.updateSaveButtonVisibility();

        super.accept(string);
    }

    private void updateSaveButtonVisibility()
    {
        String current = this.getCurrent();

        this.saveButton.setEnabled(!current.isEmpty() && isFromWorld(current));
    }

    private String getCurrent()
    {
        String first = this.strings.list.getCurrentFirst();

        return first == null ? "" : first;
    }

    private void saveStructure()
    {
        String current = this.getCurrent();

        if (current.isEmpty())
        {
            return;
        }

        try
        {
            Link sourceLink = Link.create(current);
            File sourceFile = BBSMod.getProvider().getFile(sourceLink);
            
            if (sourceFile == null || !sourceFile.exists())
            {
                System.err.println("[BBS] Can't find the source file: " + current);

                return;
            }

            String fileName = sourceLink.path;

            if (fileName.startsWith("structures/"))
            {
                fileName = fileName.substring("structures/".length());
            }

            File assetsStructuresDir = new File(BBSMod.getAssetsFolder(), "structures");
            File destFile = new File(assetsStructuresDir, fileName);

            assetsStructuresDir.mkdirs();
            Files.copy(sourceFile.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

            System.out.println("[BBS] Structure was saved: " + sourceFile.getAbsolutePath() + " -> " + destFile.getAbsolutePath());

            this.getContext().notifySuccess(UIKeys.FORMS_EDITORS_STRUCTURE_SAVE_STRUCTURE_NOTIFICATION.format(current));
        }
        catch (IOException e)
        {
            System.err.println("[BBS] Error saving a structure: " + e.getMessage());

            e.printStackTrace();
        }
    }
}