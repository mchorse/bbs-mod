package mchorse.bbs_mod.importers.types;

import com.google.common.io.Files;
import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.importers.ImporterContext;
import mchorse.bbs_mod.importers.ImporterUtils;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.UIKeys;

import java.io.File;
import java.io.IOException;

/**
 * StructureImporter
 * Permite importar archivos de estructura (.nbt) mediante arrastrar y soltar
 * hacia la carpeta assets/structures para que puedan usarse en StructureForm.
 */
public class StructureImporter implements IImporter
{
    @Override
    public IKey getName()
    {
        return UIKeys.IMPORTER_STRUCTURE_NBT;
    }

    @Override
    public File getDefaultFolder()
    {
        return new File(BBSMod.getAssetsFolder(), "structures");
    }

    @Override
    public boolean canImport(ImporterContext context)
    {
        return ImporterUtils.checkFileExtension(context.files, ".nbt");
    }

    @Override
    public void importFiles(ImporterContext context)
    {
        File destination = context.getDestination(this);

        if (!destination.exists())
        {
            destination.mkdirs();
        }

        for (File file : context.files)
        {
            try
            {
                Files.copy(file, new File(destination, file.getName()));
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
    }
}