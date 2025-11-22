package mchorse.bbs_mod.forms.sections;

import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.forms.FormCategories;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.forms.forms.StructureForm;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.utils.watchdog.WatchDogEvent;

import java.nio.file.Path;
import java.util.Objects;

public class StructureFormSection extends SubFormSection
{
    public StructureFormSection(FormCategories parent)
    {
        super(parent);
    }

    @Override
    public void initiate()
    {
        try
        {
            for (Link link : BBSMod.getProvider().getLinksFromPath(Link.assets("structures")))
            {
                if (link.path.toLowerCase().endsWith(".nbt"))
                {
                    this.add(link.path);
                }
            }
        }
        catch (Exception e)
        {
            /* Fails silently if the folder doesn't exist */
        }
    }

    @Override
    protected IKey getTitle()
    {
        /* No hay clave dedicada; usar título literal */
        return UIKeys.FORMS_CATEGORIES_STRUCTURES;
    }

    @Override
    protected Form create(String key)
    {
        StructureForm form = new StructureForm();

        form.structureFile.set(key);

        return form;
    }

    @Override
    protected boolean isEqual(Form form, String key)
    {
        StructureForm structureForm = (StructureForm) form;

        return Objects.equals(structureForm.structureFile.get(), key);
    }

    @Override
    public void accept(Path path, WatchDogEvent event)
    {
        Link link = BBSMod.getProvider().getLink(path.toFile());

        if (link.path.startsWith("structures/") && link.path.endsWith(".nbt"))
        {
            String key = link.path;

            if (event == WatchDogEvent.DELETED)
            {
                this.remove(key);
            }
            else
            {
                this.add(key);
            }

            this.parent.markDirty();
        }
    }
}