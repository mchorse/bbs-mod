package mchorse.bbs_mod.utils.iris;

import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.graphics.texture.Texture;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.utils.CollectionUtils;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.resource.ResourceManager;
import org.lwjgl.opengl.GL11;

import java.io.IOException;

public class IrisTextureWrapper extends AbstractTexture
{
    public final Link texture;
    public final AbstractTexture fallback;
    public final int index;

    public IrisTextureWrapper(Link texture, int index)
    {
        this(texture, null, index);
    }

    public IrisTextureWrapper(Link texture, AbstractTexture fallback, int index)
    {
        this.texture = texture;
        this.fallback = fallback;
        this.index = index;
    }

    @Override
    public void load(ResourceManager manager) throws IOException
    {}

    @Override
    public int getGlId()
    {
        Texture texture = BBSModClient.getTextures().getTexture(this.texture, GL11.GL_NEAREST, true);

        if (texture == null || texture == BBSModClient.getTextures().getError())
        {
            return this.fallback == null ? -1 : this.fallback.getGlId();
        }

        if (this.index >= 0 && texture.getParent() != null)
        {
            Texture frame = CollectionUtils.getSafe(texture.getParent().textures, this.index);

            if (frame != null)
            {
                return frame.id;
            }
        }

        return texture.id;
    }

    @Override
    public void close()
    {
        BBSModClient.getTextures().delete(this.texture);
    }
}