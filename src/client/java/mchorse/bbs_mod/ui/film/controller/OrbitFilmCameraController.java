package mchorse.bbs_mod.ui.film.controller;

import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.camera.Camera;
import mchorse.bbs_mod.camera.controller.ICameraController;
import mchorse.bbs_mod.cubic.ModelInstance;
import mchorse.bbs_mod.film.BaseFilmController;
import mchorse.bbs_mod.forms.FormUtilsClient;
import mchorse.bbs_mod.forms.entities.IEntity;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.forms.forms.ModelForm;
import mchorse.bbs_mod.forms.forms.utils.Anchor;
import mchorse.bbs_mod.forms.renderers.ModelFormRenderer;
import mchorse.bbs_mod.forms.renderers.utils.MatrixCache;
import mchorse.bbs_mod.graphics.window.Window;
import mchorse.bbs_mod.ui.Keys;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.utils.Area;
import mchorse.bbs_mod.ui.utils.keys.KeyAction;
import mchorse.bbs_mod.ui.utils.keys.KeyCombo;
import mchorse.bbs_mod.utils.MathUtils;
import mchorse.bbs_mod.utils.Pair;
import mchorse.bbs_mod.utils.interps.Lerps;
import mchorse.bbs_mod.utils.joml.Vectors;
import org.joml.Intersectionf;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector2i;
import org.joml.Vector3f;
import org.joml.Vector3i;

public class OrbitFilmCameraController implements ICameraController
{
    private UIFilmController controller;

    public boolean enabled;

    private boolean orbiting;
    private Vector2f rotation = new Vector2f();
    private Vector2i last = new Vector2i();
    private Vector3f position = new Vector3f();

    private float distance;
    private float offsetY;
    private boolean center;

    protected Vector3i velocityPosition = new Vector3i();

    public OrbitFilmCameraController(UIFilmController controller)
    {
        this.controller = controller;
    }

    public void start(UIContext context)
    {
        if (context.mouseButton != 2)
        {
            return;
        }

        this.center = Window.isKeyPressed(Keys.FLIGHT_ORBIT.getMainKey());
        this.orbiting = true;
        this.last.set(context.mouseX, context.mouseY);

        if (this.center)
        {
            Vector3f rayDirection = this.rotateVector(0F, 0F, -1F, this.rotation.y, this.rotation.x, false);
            Vector3f normal = Vectors.TEMP_3F.set(rayDirection).mul(-1F, 0F, -1F).normalize();

            float t = Intersectionf.intersectRayPlane(this.position, rayDirection, new Vector3f(0, this.offsetY, 0), normal, 0.0001F);
            Vector3f p = new Vector3f(rayDirection).mul(t).add(this.position);

            p.x = 0;
            p.z = 0;

            this.distance = this.position.distance(p);
            this.offsetY = p.y;
        }
    }

    public void stop()
    {
        if (this.center)
        {
            this.position.set(this.rotateVector(0F, 0F, 1F, this.rotation.y, this.rotation.x, false).mul(this.distance));
            this.position.add(0, this.offsetY, 0);
        }

        this.orbiting = false;
        this.center = false;
    }

    public boolean keyPressed(UIContext context, Area area)
    {
        if (!this.enabled || context.isFocused())
        {
            return false;
        }

        if (area.isInside(context) || (!this.velocityPosition.equals(0, 0, 0) && context.getKeyAction() == KeyAction.RELEASED))
        {
            int x = this.getFactor(context, Keys.FLIGHT_LEFT, Keys.FLIGHT_RIGHT, this.velocityPosition.x);
            int y = this.getFactor(context, Keys.FLIGHT_UP, Keys.FLIGHT_DOWN, this.velocityPosition.y);
            int z = this.getFactor(context, Keys.FLIGHT_FORWARD, Keys.FLIGHT_BACKWARD, this.velocityPosition.z);
            boolean changed = x != this.velocityPosition.x || y != this.velocityPosition.y || z != this.velocityPosition.z;

            this.velocityPosition.set(x, y, z);

            return changed;
        }

        return false;
    }

    protected int getFactor(UIContext context, KeyCombo positive, KeyCombo negative, int x)
    {
        if (context.isPressed(positive.getMainKey()))
        {
            return 1;
        }
        else if (context.isPressed(negative.getMainKey()))
        {
            return -1;
        }
        else if (
            (context.isReleased(positive.getMainKey()) && x > 0) ||
            (context.isReleased(negative.getMainKey()) && x < 0)
        ) {
            return 0;
        }

        return x;
    }

    public void handleOrbiting(UIContext context)
    {
        if (this.orbiting)
        {
            int x = context.mouseX;
            int y = context.mouseY;

            this.rotation.add(
                -(y - this.last.y) * this.controller.panel.dashboard.orbit.getAngleSpeed(),
                -(x - this.last.x) * this.controller.panel.dashboard.orbit.getAngleSpeed()
            );

            this.last.set(x, y);
        }
    }

    public boolean update(UIContext context)
    {
        if (!this.enabled || context.isFocused())
        {
            return false;
        }

        boolean changed = false;

        if (this.velocityPosition.lengthSquared() > 0 && !this.center)
        {
            this.position.add(this.rotateVector(-this.velocityPosition.x, this.velocityPosition.y, -this.velocityPosition.z, this.rotation.y, this.rotation.x).mul(this.getSpeed()));

            changed = true;
        }
        else if (this.center)
        {
            this.position.set(this.rotateVector(0F, 0F, 1F, this.rotation.y, this.rotation.x).mul(this.distance));
            this.position.add(0, this.offsetY, 0);
        }

        return changed;
    }

    protected float getSpeed()
    {
        return this.controller.panel.dashboard.orbit.getSpeed();
    }

    protected Vector3f rotateVector(float x, float y, float z, float yaw, float pitch)
    {
        return this.rotateVector(x, y, z, yaw, pitch, BBSSettings.editorHorizontalFlight.get());
    }

    protected Vector3f rotateVector(float x, float y, float z, float yaw, float pitch, boolean horizontal)
    {
        Matrix3f rotation = new Matrix3f();
        Vector3f rotate = new Vector3f(x, y, z);

        rotation.rotateY(yaw);

        if (!horizontal)
        {
            rotation.rotateX(pitch);
        }

        rotation.transform(rotate);

        return rotate;
    }

    @Override
    public void setup(Camera camera, float transition)
    {
        IEntity entity = this.controller.getCurrentEntity();

        if (entity != null)
        {
            float renderYaw = MathUtils.toRad(-Lerps.lerp(entity.getPrevBodyYaw(), entity.getBodyYaw(), transition) + 180F);
            Vector3f offset = this.rotateVector(this.position.x, this.position.y, this.position.z, renderYaw, 0F);

            if (this.center)
            {
                offset = this.rotateVector(0F, 0F, 1F, this.rotation.y + renderYaw, this.rotation.x, false).mul(this.distance);
                offset.add(0, this.offsetY, 0);
            }

            Form form = entity.getForm();
            double h = entity.getPickingHitbox().h / 2;
            double x = Lerps.lerp(entity.getPrevX(), entity.getX(), transition);
            double y = Lerps.lerp(entity.getPrevY(), entity.getY(), transition) + h;
            double z = Lerps.lerp(entity.getPrevZ(), entity.getZ(), transition);

            if (form != null)
            {
                MatrixCache map = FormUtilsClient.getRenderer(form).collectMatrices(entity, transition);
                String group = "anchor";

                if (form instanceof ModelForm modelForm)
                {
                    ModelInstance model = ModelFormRenderer.getModel(modelForm);

                    if (model != null)
                    {
                        String anchor = model.getAnchor();

                        group = anchor.isEmpty() ? group : anchor;
                    }
                }

                Matrix4f anchor = map.get(group).matrix();

                if (anchor != null)
                {
                    Anchor v = form.anchor.get();
                    Matrix4f defaultMatrix = BaseFilmController.getMatrixForRenderWithRotation(entity, x, y, z, transition);
                    Pair<Matrix4f, Float> totalMatrix = BaseFilmController.getTotalMatrix(this.controller.getEntities(), v, defaultMatrix, x, y, z, transition, 0);

                    if (totalMatrix.a != null)
                    {
                        defaultMatrix = totalMatrix.a;
                    }

                    defaultMatrix.mul(anchor);

                    Vector3f translate = defaultMatrix.getTranslation(Vectors.TEMP_3F);

                    x += translate.x;
                    y += translate.y;
                    z += translate.z;
                }
            }

            camera.position.set(x, y, z);
            camera.position.add(offset);
            camera.rotation.set(-this.rotation.x, -(this.rotation.y + renderYaw), 0);
        }
    }

    @Override
    public int getPriority()
    {
        return 20;
    }

    public void reset()
    {
        this.position.set(0F, 0F, -4F);
        this.rotation.set(0F, Math.PI);
    }
}