package mchorse.bbs_mod.ui.model_blocks;

import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.blocks.entities.ModelProperties;
import mchorse.bbs_mod.camera.OrbitDistanceCamera;
import mchorse.bbs_mod.camera.controller.OrbitCameraController;
import mchorse.bbs_mod.forms.FormUtils;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.graphics.window.Window;
import mchorse.bbs_mod.items.GunProperties;
import mchorse.bbs_mod.network.ClientNetwork;
import mchorse.bbs_mod.ui.Keys;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.dashboard.utils.UIOrbitCamera;
import mchorse.bbs_mod.ui.forms.UIFormPalette;
import mchorse.bbs_mod.ui.forms.UINestedEdit;
import mchorse.bbs_mod.ui.framework.UIBaseMenu;
import mchorse.bbs_mod.ui.framework.UIRenderingContext;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIIcon;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIToggle;
import mchorse.bbs_mod.ui.framework.elements.context.UIInterpolationContextMenu;
import mchorse.bbs_mod.ui.framework.elements.input.UIPropTransform;
import mchorse.bbs_mod.ui.framework.elements.input.UITrackpad;
import mchorse.bbs_mod.ui.framework.elements.input.text.UITextbox;
import mchorse.bbs_mod.ui.framework.elements.utils.Batcher2D;
import mchorse.bbs_mod.gizmos.BoneGizmoSystem;
import mchorse.bbs_mod.ui.utils.Area;
import mchorse.bbs_mod.ui.utils.UI;
import mchorse.bbs_mod.ui.utils.UIUtils;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.ui.utils.presets.UICopyPasteController;
import mchorse.bbs_mod.ui.utils.presets.UIPresetContextMenu;
import mchorse.bbs_mod.utils.CollectionUtils;
import mchorse.bbs_mod.utils.MathUtils;
import mchorse.bbs_mod.utils.colors.Colors;
import mchorse.bbs_mod.utils.pose.Transform;
import mchorse.bbs_mod.utils.presets.PresetManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.Perspective;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class UIModelBlockEditorMenu extends UIBaseMenu
{
    private static int lastSection;

    public UIElement iconBar;
    public UIIcon def;
    public UIIcon thirdPerson;
    public UIIcon firstPerson;
    public UIIcon inventory;
    public UIIcon gun;
    public UIIcon projectile;
    public UIIcon impact;
    public UIIcon zoom;
    public UIIcon commands;

    public Map<UIElement, UIIcon> sections = new HashMap<>();
    public UIElement currentSection;
    public UIElement sectionDefault;
    public UIElement sectionTp;
    public UIElement sectionFp;
    public UIElement sectionInventory;
    public UIElement sectionGun;
    public UIElement sectionProjectile;
    public UIElement sectionImpact;
    public UIElement sectionZoom;
    public UIElement sectionCommands;

    /* Data */
    private ModelProperties properties;
    private GunProperties gunProperties;

    /* Camera */
    private UIOrbitCamera uiOrbitCamera;
    private OrbitCameraController orbitCameraController;

    private UICopyPasteController copyPasteController;

    public UIModelBlockEditorMenu(ModelProperties properties)
    {
        this.properties = properties;

        if (properties instanceof GunProperties gunProperties)
        {
            this.gunProperties = gunProperties;
        }

        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        OrbitDistanceCamera orbit = new OrbitDistanceCamera();

        orbit.distance.setX(14);
        orbit.setFovRoll(false);
        this.uiOrbitCamera = new UIOrbitCamera();
        this.uiOrbitCamera.setControl(true);
        this.uiOrbitCamera.orbit = orbit;
        this.orbitCameraController = new OrbitCameraController(this.uiOrbitCamera.orbit);
        this.orbitCameraController.camera.position.set(player.getPos().x, player.getPos().y + 1D, player.getPos().z);
        this.orbitCameraController.camera.rotation.set(0, MathUtils.toRad(player.bodyYaw), 0);

        if (this.gunProperties != null)
        {
            this.copyPasteController = new UICopyPasteController(PresetManager.GUNS, "_CopyGun")
                .supplier(() -> this.gunProperties.toData())
                .consumer((data, x, y) ->
                {
                    this.gunProperties.fromData(data);

                    this.saveSection();
                    this.createUI();
                    this.main.resize();
                });

            this.main.context((menu) ->
            {
                menu.custom(new UIPresetContextMenu(this.copyPasteController)
                    .labels(UIKeys.GUN_CONTEXT_COPY, UIKeys.GUN_CONTEXT_PASTE));
            });
        }

        this.createUI();
    }

    public GunProperties getGunProperties()
    {
        return this.gunProperties;
    }

    private void createUI()
    {
        this.sections.clear();
        this.main.removeAll();

        /* Initiate sections */
        this.sectionDefault = this.createTransform(
            this.properties.getTransform(),
            () -> this.properties.getForm(),
            (f) -> this.properties.setForm(f)
        );

        this.sectionTp = this.createTransform(
            this.properties.getTransformThirdPerson(),
            () -> this.properties.getFormThirdPerson(),
            (f) -> this.properties.setFormThirdPerson(f)
        );

        this.sectionFp = this.createTransform(
            this.properties.getTransformFirstPerson(),
            () -> this.properties.getFormFirstPerson(),
            (f) -> this.properties.setFormFirstPerson(f)
        );

        this.sectionInventory = this.createTransform(
            this.properties.getTransformInventory(),
            () -> this.properties.getFormInventory(),
            (f) -> this.properties.setFormInventory(f)
        );

        GunProperties gun = this.gunProperties;

        if (gun != null)
        {
            /* Gun properties */
            UIToggle launch = new UIToggle(UIKeys.GUN_ITEM_LAUNCH, (b) -> gun.launch = b.getValue());
            UITrackpad launchPower = new UITrackpad((v) -> gun.launchPower = v.floatValue());
            UIToggle launchAdditive = new UIToggle(UIKeys.GUN_ITEM_ADDITIVE, (b) -> gun.launchAdditive = b.getValue());
            UITrackpad scatterX = new UITrackpad((v) -> gun.scatterX = v.floatValue());
            UITrackpad scatterY = new UITrackpad((v) -> gun.scatterY = v.floatValue());
            UITrackpad projectiles = new UITrackpad((v) -> gun.projectiles = v.intValue()).limit(1).integer();

            launch.setValue(gun.launch);
            launchPower.setValue(gun.launchPower);
            launchAdditive.setValue(gun.launchAdditive);
            scatterX.setValue(gun.scatterX);
            scatterX.tooltip(UIKeys.GUN_ITEM_SCATTER_H);
            scatterY.setValue(gun.scatterY);
            scatterY.tooltip(UIKeys.GUN_ITEM_SCATTER_V);
            projectiles.setValue(gun.projectiles);
            projectiles.limit(1).integer();

            this.sectionGun = UI.scrollView(5, 10,
                launch, launchPower, launchAdditive,
                UI.label(UIKeys.GUN_ITEM_SCATTER).background().marginTop(6), UI.row(scatterX, scatterY),
                UI.label(UIKeys.GUN_ITEM_PROJECTILES).background().marginTop(6), projectiles
            );
            this.sectionGun.relative(this.viewport).x(1F).w(200).h(1F).anchorX(1F);
            this.gun = new UIIcon(Icons.GEAR, (b) -> this.setSection(this.sectionGun));
            this.gun.tooltip(UIKeys.GUN_ITEM_TITLE);

            /* Projectile */
            UINestedEdit projectileForm = new UINestedEdit((edit) -> UIFormPalette.open(this.main, edit, gun.projectileForm, (f) ->
            {
                gun.projectileForm = FormUtils.copy(f);
                this.sectionProjectile.getChildren(UINestedEdit.class).get(0).setForm(f);
            })).keybinds();
            UIPropTransform projectileTransform = new UIPropTransform();
            UIToggle useTarget = new UIToggle(UIKeys.GUN_PROJECTILE_USE_TARGET, (b) -> gun.useTarget = b.getValue());
            UITrackpad lifeSpan = new UITrackpad((v) -> gun.lifeSpan = v.intValue());
            UITrackpad speed = new UITrackpad((v) -> gun.speed = v.floatValue());
            UITrackpad friction = new UITrackpad((v) -> gun.friction = v.floatValue());
            UITrackpad gravity = new UITrackpad((v) -> gun.gravity = v.floatValue());
            UIToggle yaw = new UIToggle(UIKeys.GUN_PROJECTILE_YAW, (b) -> gun.yaw = b.getValue());
            UIToggle pitch = new UIToggle(UIKeys.GUN_PROJECTILE_PITCH, (b) -> gun.pitch = b.getValue());
            UITrackpad fadeIn = new UITrackpad((v) -> gun.fadeIn = v.intValue());
            UITrackpad fadeOut = new UITrackpad((v) -> gun.fadeOut = v.intValue());

            projectileForm.setForm(gun.projectileForm);
            projectileTransform.setTransform(gun.projectileTransform);
            useTarget.setValue(gun.useTarget);
            lifeSpan.setValue(gun.lifeSpan);
            speed.setValue(gun.speed);
            friction.setValue(gun.friction);
            gravity.setValue(gun.gravity);
            yaw.setValue(gun.yaw);
            yaw.tooltip(UIKeys.GUN_PROJECTILE_YAW_TOOLTIP);
            pitch.setValue(gun.pitch);
            pitch.tooltip(UIKeys.GUN_PROJECTILE_PITCH_TOOLTIP);
            fadeIn.setValue(gun.fadeIn);
            fadeIn.limit(0).integer().tooltip(UIKeys.GUN_PROJECTILE_FADE_IN);
            fadeOut.setValue(gun.fadeOut);
            fadeOut.limit(0).integer().tooltip(UIKeys.GUN_PROJECTILE_FADE_OUT);

            this.sectionProjectile = UI.scrollView(5, 10,
                UI.label(UIKeys.GUN_PROJECTILE_FORM).background(), projectileForm,
                UI.label(UIKeys.GUN_PROJECTILE_TRANSFORM).background().marginTop(6), projectileTransform,
                useTarget.marginTop(6),
                UI.label(UIKeys.GUN_PROJECTILE_LIFE_SPAN).background().marginTop(6), lifeSpan,
                UI.label(UIKeys.GUN_PROJECTILE_SPEED).background().marginTop(6), speed,
                UI.label(UIKeys.GUN_PROJECTILE_FRICTION).background(), friction,
                UI.label(UIKeys.GUN_PROJECTILE_GRAVITY).background(), gravity,
                UI.label(UIKeys.GUN_PROJECTILE_ROTATIONS).background().marginTop(6), UI.row(yaw, pitch),
                UI.label(UIKeys.GUN_PROJECTILE_FADING).background().marginTop(6), UI.row(fadeIn, fadeOut)
            );
            this.sectionProjectile.relative(this.viewport).x(1F).w(200).h(1F).anchorX(1F);
            this.projectile = new UIIcon(Icons.BULLET, (b) -> this.setSection(this.sectionProjectile));
            this.projectile.tooltip(UIKeys.GUN_PROJECTILE_TITLE);

            /* Impact */
            UINestedEdit impactForm = new UINestedEdit((edit) -> UIFormPalette.open(this.main, edit, gun.impactForm, (f) ->
            {
                gun.impactForm = FormUtils.copy(f);
                this.sectionImpact.getChildren(UINestedEdit.class).get(0).setForm(f);
            })).keybinds();
            UITrackpad bounceHits = new UITrackpad((v) -> gun.bounces = v.intValue());
            UITrackpad bounceDamping = new UITrackpad((v) -> gun.bounceDamping = v.floatValue());
            UIToggle vanish = new UIToggle(UIKeys.GUN_IMPACT_VANISH, (b) -> gun.vanish = b.getValue());
            UITrackpad damage = new UITrackpad((v) -> gun.damage = v.floatValue());
            UITrackpad knockback = new UITrackpad((v) -> gun.knockback = v.floatValue());
            UIToggle collideBlocks = new UIToggle(UIKeys.GUN_IMPACT_BLOCKS, (b) -> gun.collideBlocks = b.getValue());
            UIToggle collideEntities = new UIToggle(UIKeys.GUN_IMPACT_ENTITIES, (b) -> gun.collideEntities = b.getValue());

            impactForm.setForm(gun.impactForm);
            bounceHits.setValue(gun.bounces);
            bounceDamping.setValue(gun.bounceDamping);
            vanish.setValue(gun.vanish);
            damage.setValue(gun.damage);
            knockback.setValue(gun.knockback);
            collideBlocks.setValue(gun.collideBlocks);
            collideEntities.setValue(gun.collideEntities);

            this.sectionImpact = UI.scrollView(5, 10,
                UI.label(UIKeys.GUN_IMPACT_FORM).background(), impactForm,
                UI.label(UIKeys.GUN_IMPACT_BOUNCES).background().marginTop(6), bounceHits,
                UI.label(UIKeys.GUN_IMPACT_BOUNCE_DAMPING).background(), bounceDamping,
                vanish.marginTop(6),
                UI.label(UIKeys.GUN_IMPACT_DAMAGE).background().marginTop(6), damage,
                UI.label(UIKeys.GUN_IMPACT_KNOCKBACK).background().marginTop(6), knockback,
                UI.label(UIKeys.GUN_IMPACT_COLLISION).background().marginTop(6), UI.row(collideBlocks, collideEntities)
            );
            this.sectionImpact.relative(this.viewport).x(1F).w(200).h(1F).anchorX(1F);
            this.impact = new UIIcon(Icons.DOWNLOAD, (b) -> this.setSection(this.sectionImpact));
            this.impact.tooltip(UIKeys.GUN_IMPACT_TITLE);

            /* Zoom */
            UINestedEdit zoomForm = new UINestedEdit((edit) -> UIFormPalette.open(this.main, edit, gun.zoomForm, (f) ->
            {
                gun.zoomForm = FormUtils.copy(f);
                this.sectionZoom.getChildren(UINestedEdit.class).get(0).setForm(f);
            })).keybinds();
            UIPropTransform zoomTransform = new UIPropTransform();
            UITextbox cmdZoomOn = new UITextbox(10000, (t) -> gun.cmdZoomOn = t);
            UITextbox cmdZoomOff = new UITextbox(10000, (t) -> gun.cmdZoomOff = t);
            UIIcon fovInterp = new UIIcon(Icons.GRAPH, (b) ->
            {
                this.context.replaceContextMenu(new UIInterpolationContextMenu(gun.fovInterp));
            });
            UITrackpad fovDuration = new UITrackpad((v) -> gun.fovDuration = v.intValue());
            UITrackpad fovTarget = new UITrackpad((v) -> gun.fovTarget = v.floatValue());

            zoomForm.setForm(gun.zoomForm);
            zoomTransform.setTransform(gun.zoomTransform);
            cmdZoomOn.setText(gun.cmdZoomOn);
            cmdZoomOff.setText(gun.cmdZoomOff);
            fovDuration.limit(1, 1000, true);
            fovDuration.setValue(gun.fovDuration);
            fovTarget.limit(0.001D, 179.999F);
            fovTarget.setValue(gun.fovTarget);

            this.sectionZoom = UI.scrollView(5, 10,
                UI.label(UIKeys.GUN_ZOOM_FORM).background(), zoomForm, zoomTransform,
                UI.label(UIKeys.GUN_ZOOM_ON).background().marginTop(6), cmdZoomOn,
                UI.label(UIKeys.GUN_ZOOM_OFF).background(), cmdZoomOff,
                UI.label(UIKeys.GUN_ZOOM_FOV_DURATION).background().marginTop(6), UI.row(fovDuration, fovInterp),
                UI.label(UIKeys.GUN_ZOOM_FOV_TARGET).background().marginTop(6), fovTarget
            );
            this.sectionZoom.relative(this.viewport).x(1F).w(200).h(1F).anchorX(1F);
            this.zoom = new UIIcon(Icons.SEARCH, (b) -> this.setSection(this.sectionZoom));
            this.zoom.tooltip(UIKeys.GUN_ZOOM_TITLE);

            /* Commands */
            UITextbox cmdFiring = new UITextbox(10000, (t) -> gun.cmdFiring = t);
            UITextbox cmdImpact = new UITextbox(10000, (t) -> gun.cmdImpact = t);
            UITextbox cmdVanish = new UITextbox(10000, (t) -> gun.cmdVanish = t);
            UITextbox cmdTicking = new UITextbox(10000, (t) -> gun.cmdTicking = t);
            UITrackpad ticking = new UITrackpad((v) -> gun.ticking = v.intValue());

            cmdFiring.setText(gun.cmdFiring);
            cmdImpact.setText(gun.cmdImpact);
            cmdVanish.setText(gun.cmdVanish);
            cmdTicking.setText(gun.cmdTicking);
            ticking.limit(0).integer().setValue(gun.ticking);
            ticking.tooltip(UIKeys.GUN_COMMANDS_TICKING_TOOLTIP);

            this.sectionCommands = UI.scrollView(5, 10,
                UI.label(UIKeys.GUN_COMMANDS_FIRING).background(), cmdFiring,
                UI.label(UIKeys.GUN_COMMANDS_IMPACT).background(), cmdImpact,
                UI.label(UIKeys.GUN_COMMANDS_VANISH).background(), cmdVanish,
                UI.label(UIKeys.GUN_COMMANDS_TICKING).background(), cmdTicking,
                ticking
            );
            this.sectionCommands.relative(this.viewport).x(1F).w(200).h(1F).anchorX(1F);
            this.commands = new UIIcon(Icons.CONSOLE, (b) -> this.setSection(this.sectionCommands));
            this.commands.tooltip(UIKeys.GUN_COMMANDS_TITLE);
        }

        this.def = new UIIcon(Icons.OUTLINE_SPHERE, (b) -> this.setSection(this.sectionDefault));
        this.def.tooltip(UIKeys.MODEL_BLOCKS_TRANSFORM_DEFAULT);
        this.thirdPerson = new UIIcon(Icons.POSE, (b) -> this.setSection(this.sectionTp));
        this.thirdPerson.tooltip(UIKeys.MODEL_BLOCKS_TRANSFORM_THIRD_PERSON);
        this.firstPerson = new UIIcon(Icons.LIMB, (b) -> this.setSection(this.sectionFp));
        this.firstPerson.tooltip(UIKeys.MODEL_BLOCKS_TRANSFORM_FIRST_PERSON);
        this.inventory = new UIIcon(Icons.SPHERE, (b) -> this.setSection(this.sectionInventory));
        this.inventory.tooltip(UIKeys.MODEL_BLOCKS_TRANSFORM_INVENTORY);

        this.sections.put(this.sectionDefault, this.def);
        this.sections.put(this.sectionTp, this.thirdPerson);
        this.sections.put(this.sectionFp, this.firstPerson);
        this.sections.put(this.sectionInventory, this.inventory);

        this.iconBar = UI.row(0, this.def, this.thirdPerson, this.firstPerson, this.inventory, this.gun, this.projectile, this.impact, this.zoom, this.commands);
        this.iconBar.row().resize();
        this.iconBar.relative(this.viewport).x(0.5F).h(20).anchor(0.5F, 0F);

        this.main.add(this.uiOrbitCamera, this.iconBar);
        this.main.add(this.sectionDefault, this.sectionTp, this.sectionFp, this.sectionInventory);

        if (gun != null)
        {
            this.sections.put(this.sectionGun, this.gun);
            this.sections.put(this.sectionProjectile, this.projectile);
            this.sections.put(this.sectionImpact, this.impact);
            this.sections.put(this.sectionZoom, this.zoom);
            this.sections.put(this.sectionCommands, this.commands);

            this.main.add(this.sectionGun, this.sectionProjectile, this.sectionImpact, this.sectionZoom, this.sectionCommands);
        }

        int index = Math.min(lastSection, this.sections.size() - 1);

        this.setSection(CollectionUtils.getKey(this.sections, (UIIcon) this.iconBar.getChildren().get(index)));
        this.main.keys().register(Keys.FILM_CONTROLLER_CYCLE_EDITORS, () ->
        {
            List<UIIcon> children = this.iconBar.getChildren(UIIcon.class);
            int i = children.indexOf(this.sections.get(this.currentSection));
            int newIndex = MathUtils.cycler(i + (Window.isShiftPressed() ? -1 : 1), children);

            this.setSection(CollectionUtils.getKey(this.sections, children.get(newIndex)));
            UIUtils.playClick();
        });
    }

    private UIElement createTransform(Transform transform, Supplier<Form> formSupplier, Consumer<Form> formConsumer)
    {
        UIElement section = UI.column();
        UINestedEdit uiPickEdit = new UINestedEdit((edit) -> UIFormPalette.open(this.main, edit, formSupplier.get(), (f) ->
        {
            formConsumer.accept(FormUtils.copy(f));
            section.getChildren(UINestedEdit.class).get(0).setForm(f);
        })).keybinds();
        UIPropTransform uiTransform = new UIPropTransform();

        uiPickEdit.setForm(formSupplier.get());
        uiTransform.enableHotkeys().h(95);
        uiTransform.setTransform(transform);

        section.add(uiPickEdit, uiTransform);
        section.relative(this.viewport).x(1F, -10).y(0.5F).w(200).anchor(1F, 0.5F);

        return section;
    }

    @Override
    public boolean canHideHUD()
    {
        return false;
    }

    @Override
    public void onClose(UIBaseMenu nextMenu)
    {
        super.onClose(nextMenu);

        this.saveSection();
        BBSModClient.getCameraController().remove(this.orbitCameraController);
        ClientNetwork.sendModelBlockTransforms(this.properties.toData());
    }

    private void saveSection()
    {
        lastSection = this.iconBar.getChildren().indexOf(this.sections.get(this.currentSection));
    }

    private void setSection(UIElement element)
    {
        if (element != this.currentSection)
        {
            this.uiOrbitCamera.setEnabled(element == this.sectionTp);

            if (element == this.sectionTp)
            {
                MinecraftClient.getInstance().options.setPerspective(Perspective.THIRD_PERSON_FRONT);
                BBSModClient.getCameraController().add(this.orbitCameraController);
            }
            else
            {
                MinecraftClient.getInstance().options.setPerspective(Perspective.FIRST_PERSON);
                BBSModClient.getCameraController().remove(this.orbitCameraController);
            }
        }

        for (UIElement uiElement : this.sections.keySet())
        {
            uiElement.setVisible(false);
        }

        element.setVisible(true);

        this.currentSection = element;
    }

    @Override
    protected void preRenderMenu(UIRenderingContext context)
    {
        super.preRenderMenu(context);
        context.batcher.gradientVBox(0, 0, this.width, 20, Colors.A75, 0);

        UIIcon icon = this.sections.get(this.currentSection);

        if (icon != null)
        {
            this.renderHighlight(context.batcher, icon.area);
        }

        /* Gizmo removido del menú del editor de bloque de modelo */
    }

    private void renderHighlight(Batcher2D batcher, Area area)
    {
        int color = BBSSettings.primaryColor.get();

        batcher.box(area.x, area.y, area.ex(), area.y + 2, Colors.A100 | color);
        batcher.gradientVBox(area.x, area.y + 2, area.ex(), area.ey(), Colors.A50 | color, color);
    }
}