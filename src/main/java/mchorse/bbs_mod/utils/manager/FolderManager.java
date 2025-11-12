package mchorse.bbs_mod.utils.manager;

import mchorse.bbs_mod.settings.values.core.ValueGroup;

import java.io.File;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Folder based manager
 */
public abstract class FolderManager <T extends ValueGroup> implements IManager<T>
{
    protected Supplier<File> folder;

    public FolderManager(Supplier<File> folder)
    {
        this.folder = folder;
    }

    public File getFolder()
    {
        File file = this.folder.get();

        if (!file.exists())
        {
            file.mkdirs();
        }

        return file;
    }

    @Override
    public boolean exists(String name)
    {
        return this.getFile(name).exists();
    }

    @Override
    public boolean rename(String from, String to)
    {
        File file = this.getFile(from);

        if (file != null && file.exists())
        {
            if (file.renameTo(this.getFile(to)))
            {
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean delete(String name)
    {
        File file = this.getFile(name);

        return file != null && file.delete();
    }

    /**
     * Add a folder.
     */
    public boolean addFolder(String path)
    {
        File folder = this.getFolder(this.normalizePath(path));

        if (folder.exists())
        {
            return false;
        }

        return folder.mkdirs();
    }

    /**
     * Rename given folder to another name. From and to arguments expect trailing slashes!
     */
    public boolean renameFolder(String from, String to)
    {
        from = this.normalizePath(from);
        to = this.normalizePath(to);

        File folder = this.getFolder(from);

        if (folder.isDirectory())
        {
            if (folder.renameTo(this.getFolder(to)))
            {
                return true;
            }
        }

        return false;
    }

    /**
     * Delete given folder. It only works if the folder is empty.
     */
    public boolean deleteFolder(String path)
    {
        File folder = this.getFolder(this.normalizePath(path));

        if (folder.isDirectory())
        {
            if (folder.delete())
            {
                return true;
            }
        }

        return false;
    }

    private String normalizePath(String path)
    {
        return path.endsWith("/") ? path : path + "/";
    }

    @Override
    public Collection<String> getKeys()
    {
        Set<String> set = new HashSet<>();

        if (this.folder == null)
        {
            return set;
        }

        this.recursiveFind(set, this.getFolder(), "");

        return set;
    }

    private void recursiveFind(Set<String> set, File folder, String prefix)
    {
        for (File file : folder.listFiles())
        {
            String name = file.getName();

            if (file.isFile() && this.isData(file))
            {
                set.add(prefix + name.substring(0, name.lastIndexOf(".")));
            }
            else if (file.isDirectory() && !file.getName().startsWith("_"))
            {
                File[] files = file.listFiles();

                if (files == null || files.length == 0)
                {
                    set.add(prefix + name + "/");
                }
                else
                {
                    this.recursiveFind(set, file, prefix + name + "/");
                }
            }
        }
    }

    protected boolean isData(File file)
    {
        return file.getName().endsWith(this.getExtension());
    }

    public File getFile(String name)
    {
        if (this.folder == null)
        {
            return null;
        }

        return new File(this.getFolder(), name + this.getExtension());
    }

    public File getFolder(String path)
    {
        if (this.folder == null)
        {
            return null;
        }

        return new File(this.getFolder(), path);
    }

    protected String getExtension()
    {
        return ".json";
    }
}