package mchorse.bbs_mod.l10n;

import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.data.DataToString;
import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.events.L10nReloadEvent;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.l10n.keys.LangKey;
import mchorse.bbs_mod.resources.AssetProvider;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.ui.utility.UILanguageEditorOverlayPanel;
import mchorse.bbs_mod.ui.utils.Label;
import mchorse.bbs_mod.utils.IOUtils;
import mchorse.bbs_mod.utils.Pair;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

public class L10n
{
    public static final String DEFAULT_LANGUAGE = "en_us";

    private Map<String, LangKey> strings = new HashMap<>();
    private Set<Function<String, List<Link>>> langFiles = new LinkedHashSet<>();
    private List<Pair<String, String>> supportedLanguages;

    private static List<Pair<String, String>> read(Link link)
    {
        try
        {
            String string = IOUtils.readText(BBSMod.getProvider().getAsset(link));
            MapType mapType = DataToString.mapFromString(string);
            List<Pair<String, String>> pairs = new ArrayList<>();

            for (String key : mapType.keys())
            {
                if (mapType.has(key, BaseType.TYPE_STRING))
                {
                    pairs.add(new Pair<>(key, mapType.getString(key)));
                }
            }

            return pairs;
        }
        catch (Exception e)
        {}

        return Collections.emptyList();
    }

    public L10n()
    {
        this.reloadSupportedLanguages(read(Link.assets("extra_languages.json")));
    }

    public static IKey lang(String key)
    {
        return BBSModClient.getL10n().getKey(key);
    }

    public static IKey lang(String key, String content, IKey reference)
    {
        LangKey langKey = BBSModClient.getL10n().getKey(key, content);

        if (reference instanceof LangKey)
        {
            langKey.reference = (LangKey) reference;
        }

        return langKey;
    }

    public void reloadSupportedLanguages(List<Pair<String, String>> additionalLanguages)
    {
        this.supportedLanguages = new ArrayList<>();
        this.supportedLanguages.addAll(read(Link.assets("languages.json")));
        this.supportedLanguages.addAll(additionalLanguages);
    }

    public Map<String, LangKey> getStrings()
    {
        return this.strings;
    }

    public List<String> getSupportedLanguageCodes()
    {
        List<String> codes = new ArrayList<>();

        for (Pair<String, String> pair : this.supportedLanguages)
        {
            codes.add(pair.b);
        }

        return codes;
    }

    public List<Label<String>> getSupportedLanguageLabels()
    {
        List<Label<String>> labels = new ArrayList<>();

        for (Pair<String, String> pair : this.supportedLanguages)
        {
            labels.add(new Label<>(IKey.constant(pair.a), pair.b));
        }

        return labels;
    }

    public List<Link> getAllLinks(String lang)
    {
        List<Link> links = new ArrayList<>();

        for (Function<String, List<Link>> function : this.langFiles)
        {
            links.addAll(function.apply(lang));
        }

        return links;
    }

    public void registerOne(Function<String, Link> function)
    {
        this.langFiles.add((lang) -> Collections.singletonList(function.apply(lang)));
    }

    public void register(Function<String, List<Link>> function)
    {
        this.langFiles.add(function);
    }

    public void reload()
    {
        this.reload(BBSSettings.language.get(), BBSMod.getProvider());
    }

    public void reload(String language, AssetProvider provider)
    {
        List<Link> links = this.getAllLinks(DEFAULT_LANGUAGE);

        if (!language.equals(DEFAULT_LANGUAGE) && !language.isEmpty())
        {
            links.addAll(this.getAllLinks(language));
        }

        for (Link link : links)
        {
            try (InputStream asset = provider.getAsset(link))
            {
                System.out.println("Loading language file \"" + link + "\".");

                this.load(link, asset);
            }
            catch (Exception e)
            {
                System.err.println("Failed to load " + link + " language file!");
                e.printStackTrace();
            }
        }

        BBSMod.events.post(new L10nReloadEvent(this));

        File export = UILanguageEditorOverlayPanel.getLangEditorFolder();
        File[] files = export.listFiles();

        if (files == null)
        {
            return;
        }

        for (File file : files)
        {
            if (file.isFile() && file.getName().endsWith(".json"))
            {
                try
                {
                    this.overwrite(DataToString.mapFromString(IOUtils.readText(file)));
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
            }
        }
    }

    public void load(Link origin, InputStream stream)
    {
        MapType map = DataToString.mapFromString(IOUtils.readText(stream));

        for (Map.Entry<String, BaseType> entry : map)
        {
            if (entry.getValue().isString())
            {
                String string = entry.getValue().asString();
                LangKey langKey = this.strings.get(entry.getKey());

                if (langKey == null)
                {
                    langKey = new LangKey(origin, entry.getKey(), string);
                }
                else
                {
                    langKey.setOrigin(origin);
                    langKey.content = string;
                }

                this.strings.put(entry.getKey(), langKey);
            }
        }
    }

    public void overwrite(MapType strings)
    {
        for (Map.Entry<String, BaseType> entry : strings)
        {
            LangKey key = this.strings.get(entry.getKey());

            if (key != null && entry.getValue().isString())
            {
                key.content = entry.getValue().asString();
            }
        }
    }

    public LangKey getKey(String key)
    {
        return this.getKey(key, key);
    }

    public LangKey getKey(String key, String content)
    {
        LangKey langKey = this.strings.computeIfAbsent(key, (k) -> new LangKey(null, k, content));

        langKey.wasRequested = true;

        return langKey;
    }
}