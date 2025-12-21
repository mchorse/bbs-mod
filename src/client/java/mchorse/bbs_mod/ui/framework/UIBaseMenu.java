package mchorse.bbs_mod.ui.framework;

import com.mojang.blaze3d.systems.RenderSystem;
import mchorse.bbs_mod.ui.Keys;
import mchorse.bbs_mod.ui.framework.elements.IUIElement;
import mchorse.bbs_mod.ui.framework.elements.IViewport;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.utils.IViewportStack;
import mchorse.bbs_mod.ui.utils.Area;
import mchorse.bbs_mod.ui.utils.Gizmo;
import mchorse.bbs_mod.ui.utils.renderers.InputRenderer;
import mchorse.bbs_mod.utils.colors.Colors;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;

/**
 * Base class for GUI screens using this framework
 */
public abstract class UIBaseMenu
{
    public static boolean renderAxes = true;

    private static InputRenderer inputRenderer = new InputRenderer();

    private UIRootElement root;
    public UIElement main;
    public UIElement overlay;
    public UIContext context;
    public Area viewport = new Area();

    public int width;
    public int height;

    public UIBaseMenu()
    {
        this.context = new UIContext(this);

        this.root = new UIRootElement(this.context);
        this.root.markContainer().full(this.viewport);

        this.main = new UIElement();
        this.main.full(this.viewport);
        this.overlay = new UIElement();
        this.overlay.full(this.viewport);
        this.root.add(this.main, this.overlay);

        UIElement popka = new UIElement();

        popka.keys().register(Keys.KEYBINDS, () -> this.context.toggleKeybinds());
        popka.keys().register(Keys.TRANSFORMATIONS_TOGGLE_AXES, () -> renderAxes = !renderAxes);
        this.root.add(popka);

        this.context.keybinds.relative(this.viewport).wh(0.5F, 1F);
    }

    public UIRootElement getRoot()
    {
        return this.root;
    }

    public boolean canHideHUD()
    {
        return true;
    }

    public boolean canPause()
    {
        return true;
    }

    public boolean canRefresh()
    {
        return true;
    }

    public void onOpen(UIBaseMenu oldMenu)
    {}

    public void onClose(UIBaseMenu nextMenu)
    {}

    public void update()
    {
        this.context.update();
    }

    public void resize(int width, int height)
    {
        this.width = width;
        this.height = height;

        this.viewport.set(0, 0, this.width, this.height);
        this.viewportSet();

        this.context.pushViewport(this.viewport);
        this.root.resize();
        this.context.popViewport();
    }

    protected void viewportSet()
    {}

    public boolean mouseClicked(int mouseX, int mouseY, int mouseButton)
    {
        boolean result = false;

        this.context.setMouse(mouseX, mouseY, mouseButton);

        if (this.root.isEnabled())
        {
            this.context.pushViewport(this.viewport);

            IUIElement element = this.root.mouseClicked(this.context);

            this.context.popViewport();

            result = element != null;
        }

        return result;
    }

    public boolean mouseScrolled(int x, int y, double h, double v)
    {
        boolean result = false;

        this.context.setMouseWheel(x, y, v, h);

        if (this.root.isEnabled())
        {
            this.context.pushViewport(this.viewport);

            IUIElement element = this.root.mouseScrolled(this.context);

            this.context.popViewport();

            result = element != null;
        }

        return result;
    }

    public boolean mouseReleased(int mouseX, int mouseY, int mouseButton)
    {
        boolean result = false;

        this.context.setMouse(mouseX, mouseY, mouseButton);

        if (this.root.isEnabled())
        {
            this.context.pushViewport(this.viewport);

            IUIElement element = this.root.mouseReleased(this.context);

            this.context.popViewport();

            result = element != null;
        }

        Gizmo.INSTANCE.stop();

        return result;
    }

    public boolean handleKey(int key, int scanCode, int action, int mods)
    {
        if (action == GLFW.GLFW_PRESS)
        {
            inputRenderer.keyPressed(this.context, key);
        }

        this.context.setKeyEvent(key, scanCode, action);

        IUIElement element = this.root.keyPressed(this.context);

        if (this.root.isEnabled() && element != null)
        {
            return true;
        }

        if (this.context.isPressed(GLFW.GLFW_KEY_ESCAPE))
        {
            this.closeMenu();

            return true;
        }

        return false;
    }

    public void handleTextInput(int key)
    {
        this.context.setKeyTyped((char) key);

        if (this.root.isEnabled())
        {
            this.root.textInput(this.context);
        }
    }

    /**
     * This method is called when this screen is about to get closed
     */
    protected void closeMenu()
    {
        MinecraftClient.getInstance().setScreen(null);
    }

    public void closeThisMenu()
    {
        this.closeMenu();
    }

    public void renderDefaultBackground()
    {
        this.context.batcher.box(0, 0, this.width, this.height, Colors.A50);
    }

    public void renderMenu(UIRenderingContext context, int mouseX, int mouseY)
    {
        RenderSystem.depthFunc(GL11.GL_ALWAYS);

        this.context.resetMatrix();
        this.context.setMouse(mouseX, mouseY);

        this.preRenderMenu(context);

        if (this.root.isVisible())
        {
            this.context.reset();
            this.context.pushViewport(this.viewport);

            this.root.render(this.context);

            this.context.popViewport();
            this.context.postRender();
        }

        if (this.main.isVisible())
        {
            inputRenderer.render(this, mouseX, mouseY);
        }

        RenderSystem.depthFunc(GL11.GL_LEQUAL);
    }

    protected void preRenderMenu(UIRenderingContext context)
    {}

    public void startRenderFrame(float tickDelta)
    {}

    public void renderInWorld(WorldRenderContext context)
    {}

    public static class UIRootElement extends UIElement implements IViewport
    {
        private UIContext context;

        public UIRootElement(UIContext context)
        {
            super();

            this.context = context;

            this.markContainer();
        }

        public UIContext getContext()
        {
            return this.context;
        }

        @Override
        public void apply(IViewportStack stack)
        {
            stack.pushViewport(this.area);
        }

        @Override
        public void unapply(IViewportStack stack)
        {
            stack.popViewport();
        }
    }
}