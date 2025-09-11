package mchorse.bbs_mod.utils.iris;

import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.utils.StringUtils;
import mchorse.bbs_mod.utils.resources.FilteredLink;
import mchorse.bbs_mod.utils.resources.MultiLink;
import net.irisshaders.iris.targets.backed.NativeImageBackedSingleColorTexture;
import net.irisshaders.iris.texture.pbr.PBRType;
import net.irisshaders.iris.texture.pbr.loader.PBRTextureLoader;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.resource.ResourceManager;

public class IrisTextureWrapperLoader implements PBRTextureLoader
{
    public NativeImageBackedSingleColorTexture defaultNormalTexture;
    public NativeImageBackedSingleColorTexture defaultSpecularTexture;

    @Override
    public void load(AbstractTexture abstractTexture, ResourceManager resourceManager, PBRTextureConsumer pbrTextureConsumer)
    {
        if (this.defaultSpecularTexture == null)
        {
            this.defaultNormalTexture = new NativeImageBackedSingleColorTexture(PBRType.NORMAL.getDefaultValue());
            this.defaultSpecularTexture = new NativeImageBackedSingleColorTexture(PBRType.SPECULAR.getDefaultValue());
        }

        if (abstractTexture instanceof IrisTextureWrapper wrapper)
        {
            Link key = wrapper.texture;
            Link normalKey = this.createPrefixedCopy(key, "_n.png");
            Link specularKey = this.createPrefixedCopy(key, "_s.png");

            pbrTextureConsumer.acceptNormalTexture(new IrisTextureWrapper(normalKey, this.defaultNormalTexture, wrapper.index));
            pbrTextureConsumer.acceptSpecularTexture(new IrisTextureWrapper(specularKey, this.defaultSpecularTexture, wrapper.index));
        }
    }

    private Link createPrefixedCopy(Link link, String suffix)
    {
        /* If given texture is a multi-link, then let's copy it and replace any of the normal
         * textures with appropriate suffixes */
        if (link instanceof MultiLink multiLink)
        {
            MultiLink newMultiLink = (MultiLink) multiLink.copy();

            for (FilteredLink child : newMultiLink.children)
            {
                if (child.path != null)
                {
                    child.path = this.createPrefixedCopy(child.path, suffix);
                }
            }

            return newMultiLink;
        }

        return new Link(link.source, StringUtils.removeExtension(link.path) + suffix);
    }
}