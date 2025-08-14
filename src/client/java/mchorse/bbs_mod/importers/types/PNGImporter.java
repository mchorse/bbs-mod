package mchorse.bbs_mod.importers.types;

import com.google.common.io.Files;
import mchorse.bbs_mod.importers.ImporterContext;
import mchorse.bbs_mod.importers.ImporterUtils;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.UIKeys;

import java.io.File;
import java.io.IOException;

public class PNGImporter implements IImporter
{
    @Override
    public IKey getName()
    {
        return UIKeys.IMPORTER_PNG;
    }

    @Override
    public boolean canImport(ImporterContext context)
    {
        return ImporterUtils.checkFileExtension(context.files, ".png");
    }

    @Override
    public void importFiles(ImporterContext context)
    {
        for (File file : context.files)
        {
            try
            {
                File destination = context.getDestination(this);

                Files.copy(file, new File(destination, file.getName()));
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
    }
}