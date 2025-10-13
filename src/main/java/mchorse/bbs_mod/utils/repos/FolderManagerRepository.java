package mchorse.bbs_mod.utils.repos;

import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.settings.values.core.ValueGroup;
import mchorse.bbs_mod.utils.manager.FolderManager;

import java.io.File;
import java.util.Collection;
import java.util.function.Consumer;

public class FolderManagerRepository <T extends ValueGroup> implements IRepository<T>
{
    private FolderManager<T> manager;

    public FolderManagerRepository(FolderManager<T> manager)
    {
        this.manager = manager;
    }

    @Override
    public T create(String id, MapType data)
    {
        return this.manager.create(id, data);
    }

    @Override
    public void load(String id, Consumer<T> callback)
    {
        T load = this.manager.load(id);

        if (callback != null)
        {
            callback.accept(load);
        }
    }

    @Override
    public void save(String id, MapType data)
    {
        this.manager.save(id, data);
    }

    @Override
    public void rename(String id, String name)
    {
        this.manager.rename(id, name);
    }

    @Override
    public void delete(String id)
    {
        this.manager.delete(id);
    }

    @Override
    public void requestKeys(Consumer<Collection<String>> callback)
    {
        if (callback != null)
        {
            callback.accept(this.manager.getKeys());
        }
    }

    @Override
    public File getFolder()
    {
        return this.manager.getFolder();
    }

    @Override
    public void addFolder(String path, Consumer<Boolean> callback)
    {
        if (callback != null)
        {
            callback.accept(this.manager.addFolder(path));
        }
    }

    @Override
    public void renameFolder(String path, String name, Consumer<Boolean> callback)
    {
        if (callback != null)
        {
            callback.accept(this.manager.renameFolder(path, name));
        }
    }

    @Override
    public void deleteFolder(String path, Consumer<Boolean> callback)
    {
        if (callback != null)
        {
            callback.accept(this.manager.deleteFolder(path));
        }
    }
}