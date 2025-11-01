package mchorse.bbs_mod.ui.utils.pose;

import mchorse.bbs_mod.cubic.ModelInstance;
import mchorse.bbs_mod.cubic.animation.ActionConfig;
import mchorse.bbs_mod.cubic.animation.ActionsConfig;
import mchorse.bbs_mod.cubic.animation.IAnimator;
import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.forms.FormUtilsClient;
import mchorse.bbs_mod.forms.forms.ModelForm;
import mchorse.bbs_mod.forms.renderers.ModelFormRenderer;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIToggle;
import mchorse.bbs_mod.ui.framework.elements.input.UITrackpad;
import mchorse.bbs_mod.ui.framework.elements.input.list.UISearchList;
import mchorse.bbs_mod.ui.framework.elements.input.list.UIStringList;
import mchorse.bbs_mod.ui.utils.UI;

import java.util.Collection;

public class UIActionsConfigEditor extends UIElement
{
    public UIStringList actions;
    public UISearchList<String> animations;
    public UIToggle loop;
    public UITrackpad speed;
    public UITrackpad fade;
    public UITrackpad tick;

    private ActionsConfig configs;
    private ActionConfig config;
    private Runnable preCallback;
    private Runnable postCallback;

    public UIActionsConfigEditor(Runnable preCallback, Runnable postCallback)
    {
        this.preCallback = preCallback;
        this.postCallback = postCallback;

        this.actions = new UIStringList((l) -> this.pickAction(l.get(0), false));
        this.actions.scroll.cancelScrolling();
        this.actions.background().h(112);

        this.animations = new UISearchList<>(new UIStringList((l) ->
        {
            this.callback(this.preCallback);
            this.config.name = this.animations.list.getIndex() == 0 ? "" : l.get(0);
            this.callback(this.postCallback);
        }));
        this.animations.list.cancelScrollEdge();
        this.animations.label(UIKeys.GENERAL_SEARCH).list.background();
        this.animations.h(112);
        this.loop = new UIToggle(UIKeys.FORMS_EDITORS_ACTIONS_LOOPS, (b) ->
        {
            this.callback(this.preCallback);
            this.config.loop = b.getValue();
            this.callback(this.postCallback);
        });
        this.speed = new UITrackpad((v) ->
        {
            this.callback(this.preCallback);
            this.config.speed = v.floatValue();
            this.callback(this.postCallback);
        });
        this.fade = new UITrackpad((v) ->
        {
            this.callback(this.preCallback);
            this.config.fade = v.floatValue();
            this.callback(this.postCallback);
        });
        this.fade.limit(0);
        this.tick = new UITrackpad((v) ->
        {
            this.callback(this.preCallback);
            this.config.tick = v.intValue();
            this.callback(this.postCallback);
        });
        this.tick.limit(0).integer();

        this.column().vertical().stretch();
        this.add(UI.label(UIKeys.FORMS_EDITORS_MODEL_ACTIONS), this.actions);
        this.add(UI.label(UIKeys.FORMS_EDITORS_ACTIONS_ANIMATIONS).marginTop(6), this.animations, this.loop);
        this.add(UI.label(UIKeys.FORMS_EDITORS_ACTIONS_SPEED).marginTop(6), this.speed);
        this.add(UI.label(UIKeys.FORMS_EDITORS_ACTIONS_FADE).marginTop(6), this.fade);
        this.add(UI.label(UIKeys.FORMS_EDITORS_ACTIONS_TICK).marginTop(6), this.tick);
    }

    private void callback(Runnable runnable)
    {
        if (runnable != null)
        {
            runnable.run();
        }
    }

    public void setConfigs(ActionsConfig configs, ModelForm form)
    {
        ModelFormRenderer renderer = (ModelFormRenderer) FormUtilsClient.getRenderer(form);
        ModelInstance model = renderer.getModel();

        renderer.ensureAnimator(0F);

        IAnimator animator = renderer.getAnimator();
        Collection<String> animations = model != null ? model.animations.animations.keySet() : null;
        Collection<String> actions = animator != null ? animator.getActions() : null;

        this.setConfigs(configs, animations, actions);
    }

    public void setConfigs(ActionsConfig configs, Collection<String> animations, Collection<String> actions)
    {
        this.configs = configs;

        this.animations.list.clear();
        this.actions.clear();

        if (animations != null)
        {
            this.animations.list.add(animations);
            this.animations.list.sort();
            this.animations.list.getList().add(0, UIKeys.GENERAL_NONE.get());
            this.animations.list.update();
        }

        if (actions != null)
        {
            this.actions.add(actions);
            this.actions.sort();

            this.pickAction("idle", true);
        }
    }

    private void pickAction(String key, boolean select)
    {
        ActionsConfig config = this.configs;

        this.config = config.actions.get(key);

        if (this.config == null)
        {
            this.config = new ActionConfig(key);

            config.actions.put(key, this.config);
        }

        this.animations.list.setCurrentScroll(this.config.name);

        if (this.animations.list.getIndex() == -1)
        {
            this.animations.list.setIndex(0);
        }

        this.loop.setValue(this.config.loop);
        this.speed.setValue(this.config.speed);
        this.fade.setValue(this.config.fade);
        this.tick.setValue(this.config.tick);

        if (select)
        {
            this.actions.setCurrentScroll(key);
        }
    }

    @Override
    public void collectUndoData(MapType data)
    {
        super.collectUndoData(data);

        data.putString("action", this.actions.getCurrentFirst());
    }

    @Override
    public void applyUndoData(MapType data)
    {
        super.applyUndoData(data);

        this.pickAction(data.getString("action"), true);
    }
}