package mchorse.bbs_mod.resources.packs;

import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.resources.ISourcePack;
import mchorse.bbs_mod.resources.Link;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;

/**
 * WorldStructuresSourcePack
 *
 * Provides access to structure files saved in the current world's folder
 * (e.g., `<world>/generated/minecraft/structures`). It hooks into the
 * existing `assets:` namespace so UI code that queries
 * `assets:structures/...` can also discover entries coming from the world.
 */
public class WorldStructuresSourcePack implements ISourcePack
{
    private static final String PREFIX = Link.ASSETS;
    private static final String STRUCTURES_PREFIX = "structures";

    @Override
    public String getPrefix()
    {
        return PREFIX;
    }

    @Override
    public boolean hasAsset(Link link)
    {
        File file = resolve(link);

        return file != null && file.exists();
    }

    @Override
    public InputStream getAsset(Link link) throws IOException
    {
        File file = resolve(link);

        if (file == null || !file.exists())
        {
            throw new IOException("World structure asset not found: " + link);
        }

        return new FileInputStream(file);
    }

    @Override
    public File getFile(Link link)
    {
        File file = resolve(link);

        return file != null && file.exists() ? file : null;
    }

    @Override
    public Link getLink(File file)
    {
        File world = BBSMod.getWorldFolder();

        if (world == null)
        {
            return null;
        }

        String base1 = new File(world, "generated/minecraft/structures").getAbsolutePath();
        String base2 = new File(world, "generated/structures").getAbsolutePath();

        String path = file.getAbsolutePath();

        if (path.startsWith(base1))
        {
            String rel = path.substring(base1.length());
            if (!rel.isEmpty() && (rel.charAt(0) == '/' || rel.charAt(0) == '\\'))
            {
                rel = rel.substring(1);
            }

            return new Link(PREFIX, STRUCTURES_PREFIX + "/" + rel.replace('\\', '/'));
        }
        else if (path.startsWith(base2))
        {
            String rel = path.substring(base2.length());
            if (!rel.isEmpty() && (rel.charAt(0) == '/' || rel.charAt(0) == '\\'))
            {
                rel = rel.substring(1);
            }

            return new Link(PREFIX, STRUCTURES_PREFIX + "/" + rel.replace('\\', '/'));
        }

        return null;
    }

    @Override
    public void getLinksFromPath(Collection<Link> links, Link link, boolean recursive)
    {
        File world = BBSMod.getWorldFolder();

        if (world == null)
        {
            return;
        }

        if (!Link.ASSETS.equals(link.source))
        {
            return;
        }

        if (!link.path.startsWith(STRUCTURES_PREFIX))
        {
            return;
        }

        String subPath = link.path.equals(STRUCTURES_PREFIX)
            ? ""
            : link.path.substring(STRUCTURES_PREFIX.length() + 1);

        File base1 = new File(world, "generated/minecraft/structures");
        File base2 = new File(world, "generated/structures");

        File dir1 = subPath.isEmpty() ? base1 : new File(base1, subPath);
        File dir2 = subPath.isEmpty() ? base2 : new File(base2, subPath);

        if (dir1.isDirectory())
        {
            ExternalAssetsSourcePack.getLinksFromPathRecursively(dir1, links, link, link.path, recursive ? 9999 : 1);
        }

        if (dir2.isDirectory())
        {
            ExternalAssetsSourcePack.getLinksFromPathRecursively(dir2, links, link, link.path, recursive ? 9999 : 1);
        }
    }

    private File resolve(Link link)
    {
        if (!Link.ASSETS.equals(link.source) || !link.path.startsWith(STRUCTURES_PREFIX))
        {
            return null;
        }

        File world = BBSMod.getWorldFolder();

        if (world == null)
        {
            return null;
        }

        String rel = link.path.substring(STRUCTURES_PREFIX.length());
        if (rel.startsWith("/"))
        {
            rel = rel.substring(1);
        }

        // Try standard locations used by vanilla and some modded setups
        File candidate1 = new File(world, "generated/minecraft/structures/" + rel);
        File candidate2 = new File(world, "generated/structures/" + rel);

        if (candidate1.exists())
        {
            return candidate1;
        }
        if (candidate2.exists())
        {
            return candidate2;
        }

        return null;
    }
}