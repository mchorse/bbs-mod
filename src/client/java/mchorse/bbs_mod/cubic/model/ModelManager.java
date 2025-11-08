package mchorse.bbs_mod.cubic.model;

import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.cubic.ModelInstance;
import mchorse.bbs_mod.cubic.MolangHelper;
import mchorse.bbs_mod.cubic.model.loaders.BOBJModelLoader;
import mchorse.bbs_mod.cubic.model.loaders.CubicModelLoader;
import mchorse.bbs_mod.cubic.model.loaders.GeoCubicModelLoader;
import mchorse.bbs_mod.cubic.model.loaders.IModelLoader;
import mchorse.bbs_mod.cubic.model.loaders.VoxModelLoader;
import mchorse.bbs_mod.data.DataToString;
import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.math.molang.MolangParser;
import mchorse.bbs_mod.resources.AssetProvider;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.utils.IOUtils;
import mchorse.bbs_mod.utils.StringUtils;
import mchorse.bbs_mod.utils.pose.PoseManager;
import mchorse.bbs_mod.utils.pose.ShapeKeysManager;
import mchorse.bbs_mod.utils.watchdog.IWatchDogListener;
import mchorse.bbs_mod.utils.watchdog.WatchDogEvent;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ModelManager implements IWatchDogListener
{
    public static final String MODELS_PREFIX = "models/";

    public final Map<String, ModelInstance> models = new HashMap<>();
    public final List<IModelLoader> loaders = new ArrayList<>();
    public final AssetProvider provider;
    public final MolangParser parser;

    private ModelLoader loader = new ModelLoader(this);

    public ModelManager(AssetProvider provider)
    {
        this.provider = provider;
        this.parser = new MolangParser();

        MolangHelper.registerVars(this.parser);

        this.setupLoaders();
    }

    private void setupLoaders()
    {
        this.loaders.clear();
        this.loaders.add(new BOBJModelLoader());
        this.loaders.add(new CubicModelLoader());
        this.loaders.add(new GeoCubicModelLoader());
        this.loaders.add(new VoxModelLoader());
    }

    /**
     * Get all models that can be loaded by
     */
    public List<String> getAvailableKeys()
    {
        List<Link> models = new ArrayList<>(BBSMod.getProvider().getLinksFromPath(Link.assets("models"), true));
        Set<String> keys = new HashSet<>();

        models.sort((a, b) -> a.toString().compareToIgnoreCase(b.toString()));

        for (Link link : models)
        {
            if (this.isRelodable(link))
            {
                String path = link.path;

                int slash = path.indexOf('/');
                int lastSlash = path.lastIndexOf('/');

                if (slash != lastSlash)
                {
                    path = path.substring(slash + 1, lastSlash);

                    keys.add(path);
                }
            }
        }

        return new ArrayList<>(keys);
    }

    public ModelInstance getModel(String id)
    {
        if (this.models.containsKey(id))
        {
            return this.models.get(id);
        }

        this.models.put(id, null);
        this.loader.add(id);

        return null;
    }

    public ModelInstance loadModel(String id)
    {
        ModelInstance model = null;
        Link modelLink = Link.assets(MODELS_PREFIX + id);
        Collection<Link> links = this.provider.getLinksFromPath(modelLink, true);
        MapType config = this.loadConfig(modelLink);

        for (IModelLoader loader : this.loaders)
        {
            model = loader.load(id, this, modelLink, links, config);

            if (model != null)
            {
                break;
            }
        }

        if (model == null)
        {
            System.err.println("Model \"" + id + "\" wasn't loaded properly, or was loaded with no top level groups!");
        }
        else
        {
            System.out.println("Model \"" + id + "\" was loaded!");

            model.setup();
        }

        this.models.put(id, model);

        return model;
    }

    private MapType loadConfig(Link modelLink)
    {
        try (InputStream asset = this.provider.getAsset(modelLink.combine("config.json")))
        {
            String string = IOUtils.readText(asset);

            return (MapType) DataToString.fromString(string);
        }
        catch (Exception e)
        {}

        return null;
    }

    public void reload()
    {
        for (ModelInstance model : this.models.values())
        {
            if (model != null)
            {
                model.delete();
            }
        }

        this.models.clear();
        PoseManager.INSTANCE.clear();
        ShapeKeysManager.INSTANCE.clear();
        this.setupLoaders();
    }

    public boolean isRelodable(Link link)
    {
        if (!link.path.startsWith(MODELS_PREFIX))
        {
            return false;
        }

        if (link.path.contains("/animations/") || link.path.contains("/shapes/"))
        {
            return false;
        }

        return link.path.endsWith(".bbs.json")
            || link.path.endsWith(".geo.json")
            || link.path.endsWith(".bobj")
            || link.path.endsWith(".obj")
            || link.path.endsWith(".animation.json")
            || link.path.endsWith(".vox")
            || link.path.endsWith("/config.json");
    }

    /**
     * Watch dog listener implementation. This is a pretty bad hardcoded
     * solution that would only work for the cubic model loader.
     */
    @Override
    public void accept(Path path, WatchDogEvent event)
    {
        Link link = BBSMod.getProvider().getLink(path.toFile());

        if (link == null)
        {
            return;
        }

        if (this.isRelodable(link))
        {
            String key = StringUtils.parentPath(link.path.substring(MODELS_PREFIX.length()));
            ModelInstance model = this.models.remove(key);

            if (model != null)
            {
                model.delete();
            }
        }
    }
}