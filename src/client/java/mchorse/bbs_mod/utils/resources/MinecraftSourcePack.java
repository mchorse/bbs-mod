package mchorse.bbs_mod.utils.resources;

import mchorse.bbs_mod.resources.ISourcePack;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.utils.DataPath;
import mchorse.bbs_mod.utils.StringUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class MinecraftSourcePack implements ISourcePack
{
    private Map<String, Object> links = new HashMap<>();
    private Map<String, Object> soundLinks = new HashMap<>();

    public MinecraftSourcePack()
    {
        this.setupPaths();
        this.setupSoundPaths();
    }

    public void setupPaths()
    {}

    public void setupSoundPaths()
    {}

    @Override
    public String getPrefix()
    {
        return "minecraft";
    }

    @Override
    public boolean hasAsset(Link link)
    {
        return false;
    }

    @Override
    public InputStream getAsset(Link link) throws IOException
    {
        return null;
    }

    @Override
    public File getFile(Link link)
    {
        return null;
    }

    @Override
    public Link getLink(File file)
    {
        return null;
    }

    @Override
    public void getLinksFromPath(Collection<Link> links, Link link, boolean recursive)
    {
        String path = link.path.endsWith("/") ? link.path.substring(0, link.path.length() - 1) : link.path;
        Map<String, Object> allLinks = this.findBasePath(path);

        if (allLinks != null)
        {
            this.traverse(links, path, allLinks, recursive);
        }
    }

    private Map<String, Object> findBasePath(String path)
    {
        if (path.isEmpty())
        {
            return this.links;
        }

        DataPath dataPath = new DataPath(path);
        Map<String, Object> map = this.links;

        for (String next : dataPath.strings)
        {
            Object o = map.get(next);

            if (o instanceof Map)
            {
                map = (Map<String, Object>) o;
            }
            else
            {
                return null;
            }
        }

        return map;
    }

    private void traverse(Collection<Link> links, String path, Map<String, Object> allLinks, boolean recursive)
    {
        for (Map.Entry<String, Object> entry : allLinks.entrySet())
        {
            if (entry.getValue() instanceof Map)
            {
                if (recursive)
                {
                    this.traverse(links, StringUtils.combinePaths(path, entry.getKey()), (Map<String, Object>) entry.getValue(), recursive);
                }

                links.add(new Link(this.getPrefix(), StringUtils.combinePaths(path, entry.getKey()) + "/"));
            }
            else
            {
                links.add(new Link(this.getPrefix(), StringUtils.combinePaths(path, entry.getKey())));
            }
        }
    }
}