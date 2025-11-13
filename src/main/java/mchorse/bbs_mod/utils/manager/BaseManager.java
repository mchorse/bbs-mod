package mchorse.bbs_mod.utils.manager;

import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.settings.values.core.ValueGroup;
import mchorse.bbs_mod.utils.StringUtils;
import mchorse.bbs_mod.utils.manager.storage.IDataStorage;
import mchorse.bbs_mod.utils.manager.storage.JSONLikeStorage;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.function.Supplier;

/**
 * Base JSON manager which loads and saves different data
 * structures based upon Data API
 */
public abstract class BaseManager <T extends ValueGroup> extends FolderManager<T>
{
    protected IDataStorage storage = new JSONLikeStorage();
    protected boolean backUps;

    public BaseManager(Supplier<File> folder)
    {
        super(folder);
    }

    @Override
    public final T create(String id, MapType data)
    {
        T object = this.createData(id, data);

        object.setId(id);

        return object;
    }

    protected abstract T createData(String id, MapType mapType);

    @Override
    public T load(String id)
    {
        try
        {
            MapType mapType = this.storage.load(this.getFile(id));
            T data = this.create(id, mapType);

            return data;
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public boolean save(String id, MapType data)
    {
        try
        {
            File file = this.getFile(id);

            if (this.backUps)
            {
                String path = file.getParentFile().getAbsolutePath();
                String backupFileName = new SimpleDateFormat("yyyy_MM_dd_HH").format(new Date());
                String filename = StringUtils.fileName(id);
                File backupFile = new File(path, "_" + StringUtils.removeExtension(filename) + "/" + filename + "." + backupFileName + ".dat");

                backupFile.getParentFile().mkdirs();

                if (file.exists())
                {
                    Files.copy(file.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }
            }

            this.storage.save(file, data);

            return true;
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        return false;
    }
}