package mchorse.bbs_mod.cubic.model.loaders;

import mchorse.bbs_mod.bobj.BOBJAction;
import mchorse.bbs_mod.bobj.BOBJArmature;
import mchorse.bbs_mod.bobj.BOBJChannel;
import mchorse.bbs_mod.bobj.BOBJGroup;
import mchorse.bbs_mod.bobj.BOBJKeyframe;
import mchorse.bbs_mod.bobj.BOBJLoader;
import mchorse.bbs_mod.cubic.ModelInstance;
import mchorse.bbs_mod.cubic.data.animation.Animation;
import mchorse.bbs_mod.cubic.data.animation.AnimationPart;
import mchorse.bbs_mod.cubic.data.animation.Animations;
import mchorse.bbs_mod.cubic.model.ModelManager;
import mchorse.bbs_mod.cubic.model.bobj.BOBJModel;
import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.math.Constant;
import mchorse.bbs_mod.math.molang.MolangParser;
import mchorse.bbs_mod.math.molang.expressions.MolangExpression;
import mchorse.bbs_mod.math.molang.expressions.MolangValue;
import mchorse.bbs_mod.resources.AssetProvider;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.utils.keyframes.Keyframe;
import mchorse.bbs_mod.utils.keyframes.KeyframeChannel;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class BOBJModelLoader implements IModelLoader
{
    private Animations defaultAnimations;

    @Override
    public ModelInstance load(String id, ModelManager models, Link model, Collection<Link> links, MapType config)
    {
        Link modelBOBJ = IModelLoader.getLink(model.combine("model.bobj"), links, ".bobj");
        Link modelTexture = IModelLoader.getLink(model.combine("model.png"), links, ".png");

        try (InputStream stream = models.provider.getAsset(modelBOBJ))
        {
            BOBJLoader.BOBJData bobjData = BOBJLoader.readData(stream);

            if (bobjData.armatures.isEmpty())
            {
                System.err.println("Model \"" + model + "\" doesn't have an armature!");

                return null;
            }

            BOBJArmature armature = bobjData.armatures.values().iterator().next();
            BOBJLoader.BOBJMesh finalMesh = null;

            for (BOBJLoader.BOBJMesh mesh : bobjData.meshes)
            {
                if (mesh.armature == armature)
                {
                    finalMesh = mesh;

                    break;
                }
            }

            if (finalMesh != null)
            {
                BOBJLoader.CompiledData compiledData = BOBJLoader.compileMesh(bobjData, finalMesh);
                BOBJModel bobjModel = new BOBJModel(armature, compiledData, id.startsWith("emoticons") && id.endsWith("_simple"));

                bobjData.initiateArmatures();

                ModelInstance instance = new ModelInstance(id, bobjModel, this.convertAnimations(bobjData, new Animations(models.parser)), modelTexture);

                if (id.startsWith("emoticons/"))
                {
                    if (this.defaultAnimations == null)
                    {
                        this.loadDefaultAnimations(models.provider, models.parser);
                    }

                    if (this.defaultAnimations != null)
                    {
                        for (Animation value : this.defaultAnimations.animations.values())
                        {
                            instance.animations.add(value);
                        }
                    }
                }

                instance.applyConfig(config);

                return instance;
            }

            System.err.println("Model \"" + model + "\" doesn't have a mesh connected to one of the armatures!");
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        return null;
    }

    public void loadDefaultAnimations(AssetProvider provider, MolangParser parser)
    {
        this.defaultAnimations = new Animations(parser);

        List<Link> actionsList = new ArrayList<>();

        actionsList.add(Link.assets("actions.bobj"));

        for (Link link : provider.getLinksFromPath(Link.assets("emotes")))
        {
            if (link.path.endsWith(".bobj"))
            {
                actionsList.add(link);
            }
        }

        for (Link link : actionsList)
        {
            try (InputStream stream = provider.getAsset(link))
            {
                BOBJLoader.BOBJData bobjData = BOBJLoader.readData(stream);

                this.convertAnimations(bobjData, this.defaultAnimations);
            }
            catch (Exception e)
            {
                System.err.println("Failed to load Emoticons " + link + "!");
                e.printStackTrace();
            }
        }
    }

    private Animations convertAnimations(BOBJLoader.BOBJData bobjData, Animations animations)
    {
        for (Map.Entry<String, BOBJAction> entry : bobjData.actions.entrySet())
        {
            Animation animation = new Animation(entry.getKey(), animations.parser);

            this.fillAnimation(animation, entry.getValue());
            animations.add(animation);
        }

        return animations;
    }

    private void fillAnimation(Animation animation, BOBJAction value)
    {
        MolangParser parser = animation.parser;

        for (Map.Entry<String, BOBJGroup> entry : value.groups.entrySet())
        {
            AnimationPart part = new AnimationPart(parser);

            for (BOBJChannel channel : entry.getValue().channels)
            {
                if (channel.path.equals("location"))
                {
                    if (channel.index == 0) this.copyKeyframes(parser, part.x, channel);
                    else if (channel.index == 1) this.copyKeyframes(parser, part.y, channel);
                    else if (channel.index == 2) this.copyKeyframes(parser, part.z, channel);
                }
                else if (channel.path.equals("scale"))
                {
                    if (channel.index == 0) this.copyKeyframes(parser, part.sx, channel);
                    else if (channel.index == 1) this.copyKeyframes(parser, part.sy, channel);
                    else if (channel.index == 2) this.copyKeyframes(parser, part.sz, channel);
                }
                else
                {
                    if (channel.index == 0) this.copyKeyframes(parser, part.rx, channel);
                    else if (channel.index == 1) this.copyKeyframes(parser, part.ry, channel);
                    else if (channel.index == 2) this.copyKeyframes(parser, part.rz, channel);
                }
            }

            animation.parts.put(entry.getKey(), part);
        }

        /* Insert head keyframes */
        AnimationPart head = animation.parts.get("head");

        if (head == null)
        {
            head = new AnimationPart(parser);

            animation.parts.put("head", head);

            this.fillHeadVariables(parser, head);
        }
        else if (head.rx.isEmpty())
        {
            this.fillHeadVariables(parser, head);
        }

        animation.setLength(value.getDuration() / 20F);
    }

    private void copyKeyframes(MolangParser parser, KeyframeChannel<MolangExpression> keyframeChannel, BOBJChannel channel)
    {
        for (int i = 0, c = channel.keyframes.size(); i < c; i++)
        {
            BOBJKeyframe a = channel.keyframes.get(i);
            BOBJKeyframe b = a;

            if (i - 1 >= 0)
            {
                b = channel.keyframes.get(i - 1);
            }

            MolangValue value = new MolangValue(parser, new Constant(a.value));
            int index = keyframeChannel.insert(a.frame, value);

            Keyframe<MolangExpression> keyframe = keyframeChannel.get(index);

            /* Fill in interpolation and bezier handles */
            keyframe.getInterpolation().setInterp(b.interpolation.interp);
            keyframe.lx = a.frame - a.leftX;
            keyframe.ly = a.leftY - a.value;
            keyframe.rx = a.rightX - a.frame;
            keyframe.ry = a.rightY - a.value;
        }

        keyframeChannel.sort();
    }

    private void fillHeadVariables(MolangParser parser, AnimationPart head)
    {
        head.rx.insert(0F, parseExpression(parser, "query.head_pitch / 180 * " + Math.PI));
        head.ry.insert(0F, parseExpression(parser, "-query.head_yaw / 180 * " + Math.PI));
    }

    private static MolangExpression parseExpression(MolangParser parser, String expression)
    {
        try
        {
            return new MolangValue(parser, parser.parse(expression));
        }
        catch (Exception e)
        {}

        return MolangParser.ZERO;
    }
}