package mchorse.bbs_mod.utils.iris;

import joptsimple.internal.Strings;
import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.graphics.texture.Texture;
import mchorse.bbs_mod.graphics.texture.TextureManager;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.utils.CollectionUtils;
import mchorse.bbs_mod.utils.DataPath;
import net.irisshaders.iris.Iris;
import net.irisshaders.iris.api.v0.IrisApi;
import net.irisshaders.iris.gl.uniform.UniformUpdateFrequency;
import net.irisshaders.iris.shaderpack.LanguageMap;
import net.irisshaders.iris.shaderpack.ShaderPack;
import net.irisshaders.iris.shaderpack.option.menu.OptionMenuContainer;
import net.irisshaders.iris.shaderpack.option.menu.OptionMenuElement;
import net.irisshaders.iris.shaderpack.option.menu.OptionMenuElementScreen;
import net.irisshaders.iris.shaderpack.option.menu.OptionMenuLinkElement;
import net.irisshaders.iris.shaderpack.option.menu.OptionMenuOptionElement;
import net.irisshaders.iris.shaderpack.properties.ShaderProperties;
import net.irisshaders.iris.texture.TextureTracker;
import net.irisshaders.iris.texture.pbr.loader.PBRTextureLoaderRegistry;
import net.irisshaders.iris.uniforms.custom.cached.CachedUniform;
import net.irisshaders.iris.uniforms.custom.cached.FloatCachedUniform;
import net.irisshaders.iris.uniforms.custom.cached.IntCachedUniform;
import net.irisshaders.iris.vertices.NormI8;
import net.irisshaders.iris.vertices.NormalHelper;
import net.irisshaders.iris.vertices.views.TriView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class IrisUtils
{
    private static Set<Texture> textureSet = new HashSet<>();
    private static ShaderProperties properties;

    public static void setShaderProperties(ShaderProperties shaderProperties)
    {
        properties = shaderProperties;
    }

    public static List<String> getSliderProperties()
    {
        return properties == null ? Collections.emptyList() : properties.getSliderOptions();
    }

    public static Map<String, String> getShadersLanguageMap(String language)
    {
        if (Iris.getCurrentPack().isPresent())
        {
            Map<String, String> map = new HashMap<>();
            ShaderPack shaderPack = Iris.getCurrentPack().get();
            LanguageMap languageMap = shaderPack.getLanguageMap();

            Map<String, String> target = languageMap.getTranslations(language);
            Map<String, String> fallback = languageMap.getTranslations("en_us");
            final String prefix = "option.";

            Map<String, DataPath> pathMap = new HashMap<>();

            collectPaths(pathMap, shaderPack.getMenuContainer(), shaderPack.getMenuContainer().mainScreen, Collections.emptyList());
            fillInPaths(map, fallback, pathMap, prefix);
            fillInPaths(map, target, pathMap, prefix);

            return map;
        }

        return Collections.emptyMap();
    }

    private static void fillInPaths(Map<String, String> map, Map<String, String> language, Map<String, DataPath> pathMap, String prefix)
    {
        if (language == null)
        {
            return;
        }

        for (Map.Entry<String, String> entry : language.entrySet())
        {
            if (entry.getKey().startsWith(prefix))
            {
                String optionId = entry.getKey().substring(prefix.length());
                DataPath path = pathMap.get(optionId);
                String value = entry.getValue();

                if (path != null)
                {
                    List<String> translations = new ArrayList<>();

                    for (int i = 0, c = path.strings.size(); i < c; i++)
                    {
                        String string = path.strings.get(i);

                        if (i == c - 1) translations.add(value);
                        else translations.add(language.getOrDefault("screen." + string, string));
                    }

                    value = Strings.join(translations, " > ");
                }

                map.put(entry.getKey(), value);
            }
        }
    }

    private static void collectPaths(Map<String, DataPath> pathMap, OptionMenuContainer container, OptionMenuElementScreen mainScreen, List<String> prefix)
    {
        for (OptionMenuElement element : mainScreen.elements)
        {
            if (element instanceof OptionMenuOptionElement option)
            {
                ArrayList<String> strings = new ArrayList<>(prefix);

                strings.add(option.optionId);
                pathMap.put(option.optionId, new DataPath(strings));
            }
            else if (element instanceof OptionMenuLinkElement link)
            {
                OptionMenuElementScreen screen = container.subScreens.get(link.targetScreenId);

                if (screen != null)
                {
                    ArrayList<String> strings = new ArrayList<>(prefix);

                    strings.add(link.targetScreenId);
                    collectPaths(pathMap, container, screen, strings);
                }
            }
        }
    }

    public static void setup()
    {
        PBRTextureLoaderRegistry.INSTANCE.register(IrisTextureWrapper.class, new IrisTextureWrapperLoader());
    }

    public static void trackTexture(Texture texture)
    {
        TextureManager textures = BBSModClient.getTextures();
        Texture error = textures.getError();

        if (texture != error && !textureSet.contains(texture))
        {
            Link key = CollectionUtils.getKey(textures.textures, texture);

            if (key == null && texture.getParent() != null)
            {
                key = CollectionUtils.getKey(textures.animatedTextures, texture.getParent());
            }

            if (key != null)
            {
                int index = -1;

                if (texture.getParent() != null)
                {
                    index = texture.getParent().textures.indexOf(texture);
                }

                TextureTracker.INSTANCE.trackTexture(texture.id, new IrisTextureWrapper(key, index));
            }

            textureSet.add(texture);
        }
    }

    public static boolean isShaderPackEnabled()
    {
        return IrisApi.getInstance().isShaderPackInUse();
    }

    public static boolean isShadowPass()
    {
        return IrisApi.getInstance().isRenderingShadowPass();
    }

    public static float[] calculateTangents(float[] v, float[] n, float[] u)
    {
        int min = Math.min(v.length / 9, Math.min(u.length / 6, n.length / 9));
        int max = Math.max(v.length / 9, Math.max(u.length / 6, n.length / 9));

        if (min != max)
        {
            return v;
        }

        return calculateTangents(new float[v.length / 3 * 4], v, n, u);
    }

    public static float[] calculateTangents(float[] t, float[] v, float[] n, float[] u)
    {
        int min = Math.min(v.length / 9, Math.min(u.length / 6, n.length / 9));
        int max = Math.max(v.length / 9, Math.max(u.length / 6, n.length / 9));

        if (min != max)
        {
            return t;
        }

        SuperTriangle triangle = new SuperTriangle();

        for (int i = 0, c = v.length / 9; i < c; i++)
        {
            int ot = i * 12;
            int oi = i * 9;
            int ou = i * 6;
            float x0 = v[oi + 0], y0 = v[oi + 1], z0 = v[oi + 2], u0 = u[ou + 0], v0 = u[ou + 1],
                  x1 = v[oi + 3], y1 = v[oi + 4], z1 = v[oi + 5], u1 = u[ou + 2], v1 = u[ou + 3],
                  x2 = v[oi + 6], y2 = v[oi + 7], z2 = v[oi + 7], u2 = u[ou + 4], v2 = u[ou + 5];

            int t1 = NormalHelper.computeTangent(n[oi + 0], n[oi + 1], n[oi + 2], triangle.set(x0, y0, z0, u0, v0, x1, y1, z1, u1, v1, x2, y2, z2, u2, v2));
            int t2 = NormalHelper.computeTangent(n[oi + 3], n[oi + 4], n[oi + 5], triangle.set(x0, y0, z0, u0, v0, x1, y1, z1, u1, v1, x2, y2, z2, u2, v2));
            int t3 = NormalHelper.computeTangent(n[oi + 6], n[oi + 7], n[oi + 8], triangle.set(x0, y0, z0, u0, v0, x1, y1, z1, u1, v1, x2, y2, z2, u2, v2));

            t[ot + 0] = NormI8.unpackX(t1);
            t[ot + 1] = NormI8.unpackY(t1);
            t[ot + 2] = NormI8.unpackZ(t1);
            t[ot + 3] = NormI8.unpackW(t1);

            t[ot + 4] = NormI8.unpackX(t2);
            t[ot + 5] = NormI8.unpackY(t2);
            t[ot + 6] = NormI8.unpackZ(t2);
            t[ot + 7] = NormI8.unpackW(t2);

            t[ot + 8] = NormI8.unpackX(t3);
            t[ot + 9] = NormI8.unpackY(t3);
            t[ot + 10] = NormI8.unpackZ(t3);
            t[ot + 11] = NormI8.unpackW(t3);
        }

        return t;
    }

    public static void addUniforms(List<CachedUniform> list, Map<String, ShaderCurves.ShaderVariable> variableMap)
    {
        for (ShaderCurves.ShaderVariable value : variableMap.values())
        {
            if (value.integer)
            {
                list.add(new IntCachedUniform(value.uniformName, UniformUpdateFrequency.PER_FRAME, () -> (int) value.getValue()));
            }
            else
            {
                list.add(new FloatCachedUniform(value.uniformName, UniformUpdateFrequency.PER_FRAME, value::getValue));
            }
        }
    }

    public static class SuperTriangle implements TriView
    {
        private float x0;
        private float y0;
        private float z0;
        private float u0;
        private float v0;

        private float x1;
        private float y1;
        private float z1;
        private float u1;
        private float v1;

        private float x2;
        private float y2;
        private float z2;
        private float u2;
        private float v2;

        public SuperTriangle set(float x0, float y0, float z0, float u0, float v0, float x1, float y1, float z1, float u1, float v1, float x2, float y2, float z2, float u2, float v2)
        {
            this.x0 = x0;
            this.y0 = y0;
            this.z0 = z0;
            this.u0 = u0;
            this.v0 = v0;

            this.x1 = x1;
            this.y1 = y1;
            this.z1 = z1;
            this.u1 = u1;
            this.v1 = v1;

            this.x2 = x2;
            this.y2 = y2;
            this.z2 = z2;
            this.u2 = u2;
            this.v2 = v2;

            return this;
        }

        @Override
        public float x(int i)
        {
            return i == 0 ? this.x0 : (i == 1 ? this.x1 : this.x2);
        }

        @Override
        public float y(int i)
        {
            return i == 0 ? this.y0 : (i == 1 ? this.y1 : this.y2);
        }

        @Override
        public float z(int i)
        {
            return i == 0 ? this.z0 : (i == 1 ? this.z1 : this.z2);
        }

        @Override
        public float u(int i)
        {
            return i == 0 ? this.u0 : (i == 1 ? this.u1 : this.u2);
        }

        @Override
        public float v(int i)
        {
            return i == 0 ? this.v0 : (i == 1 ? this.v1 : this.v2);
        }
    }
}