package mchorse.bbs_mod.ui.utils.pose;

import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.data.DataToString;
import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.data.types.ListType;
import mchorse.bbs_mod.data.types.MapType;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Gestor simple para categorías de huesos.
 * Guarda un mapa por grupo de pose (clave String), con categorías y sus huesos.
 * Estructura:
 * {
 *   "poseGroup": {
 *     "Categoria A": ["bone1", "bone2"],
 *     "Categoria B": ["bone3"]
 *   }
 * }
 */
public class BoneCategoriesManager
{
    private static final String FILE_NAME = "bone_categories.json";
    private final Map<String, Map<String, List<String>>> cache = new HashMap<>();

    public BoneCategoriesManager()
    {
        this.load();
    }

    private File getFile()
    {
        return BBSMod.getSettingsPath(FILE_NAME);
    }

    private void load()
    {
        try
        {
            BaseType type = DataToString.read(this.getFile());

            if (type != null && type.isMap())
            {
                MapType map = (MapType) type;

                for (String group : map.keys())
                {
                    Map<String, List<String>> categories = new LinkedHashMap<>();
                    MapType cats = map.getMap(group);

                    if (cats != null)
                    {
                        for (String catName : cats.keys())
                        {
                            List<String> bones = new ArrayList<>();
                            ListType list = cats.getList(catName);

                            if (list != null)
                            {
                                for (int i = 0; i < list.size(); i++)
                                {
                                    bones.add(list.getString(i));
                                }
                            }

                            categories.put(catName, bones);
                        }
                    }

                    this.cache.put(group, categories);
                }
            }
        }
        catch (IOException e)
        {
            // Archivo no existe o no se pudo leer; dejar cache vacío
        }
    }

    private void save()
    {
        MapType root = new MapType();

        for (Map.Entry<String, Map<String, List<String>>> entry : this.cache.entrySet())
        {
            MapType cats = new MapType();
            for (Map.Entry<String, List<String>> cat : entry.getValue().entrySet())
            {
                ListType bones = new ListType();
                for (String b : cat.getValue())
                {
                    bones.addString(b);
                }
                cats.put(cat.getKey(), bones);
            }
            root.put(entry.getKey(), cats);
        }

        DataToString.writeSilently(this.getFile(), root, true);
    }

    /* API */

    public List<String> getCategories(String groupKey)
    {
        return new ArrayList<>(this.cache.computeIfAbsent(groupKey, (g) -> new LinkedHashMap<>()).keySet());
    }

    public List<String> getBones(String groupKey, String category)
    {
        Map<String, List<String>> cats = this.cache.computeIfAbsent(groupKey, (g) -> new LinkedHashMap<>());
        return new ArrayList<>(cats.getOrDefault(category, Collections.emptyList()));
    }

    public void addCategory(String groupKey, String category)
    {
        Map<String, List<String>> cats = this.cache.computeIfAbsent(groupKey, (g) -> new LinkedHashMap<>());
        cats.putIfAbsent(category, new ArrayList<>());
        this.save();
    }

    public void removeCategory(String groupKey, String category)
    {
        Map<String, List<String>> cats = this.cache.computeIfAbsent(groupKey, (g) -> new LinkedHashMap<>());
        cats.remove(category);
        this.save();
    }

    public void renameCategory(String groupKey, String oldName, String newName)
    {
        if (Objects.equals(oldName, newName)) return;

        Map<String, List<String>> cats = this.cache.computeIfAbsent(groupKey, (g) -> new LinkedHashMap<>());
        List<String> bones = cats.remove(oldName);
        if (bones == null) bones = new ArrayList<>();
        cats.put(newName, bones);
        this.save();
    }

    public void addBone(String groupKey, String category, String bone)
    {
        Map<String, List<String>> cats = this.cache.computeIfAbsent(groupKey, (g) -> new LinkedHashMap<>());
        List<String> bones = cats.computeIfAbsent(category, (c) -> new ArrayList<>());
        if (!bones.contains(bone))
        {
            bones.add(bone);
            this.save();
        }
    }

    public void removeBone(String groupKey, String category, String bone)
    {
        Map<String, List<String>> cats = this.cache.computeIfAbsent(groupKey, (g) -> new LinkedHashMap<>());
        List<String> bones = cats.get(category);
        if (bones != null && bones.remove(bone))
        {
            this.save();
        }
    }
}