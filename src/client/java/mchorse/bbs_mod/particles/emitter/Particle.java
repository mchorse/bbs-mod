package mchorse.bbs_mod.particles.emitter;

import mchorse.bbs_mod.utils.joml.Vectors;
import org.joml.Matrix3f;
import org.joml.Vector3d;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;

public class Particle
{
    /* Randoms */
    public float random1 = (float) Math.random();
    public float random2 = (float) Math.random();
    public float random3 = (float) Math.random();
    public float random4 = (float) Math.random();

    /* States */
    public final int index;
    public final float offset;
    public int age;
    public int lifetime;
    private boolean dead;
    public boolean relativePosition;
    public boolean relativeRotation;
    public boolean relativeVelocity;
    public boolean textureScale;
    public boolean manual;

    /* Rotation */
    public float rotation;
    public float initialRotation;
    public float prevRotation;

    public float rotationVelocity;
    public float rotationAcceleration;
    public float rotationDrag;

    /* Position */
    public Vector3d position = new Vector3d();
    public Vector3d initialPosition = new Vector3d();
    public Vector3d prevPosition = new Vector3d();
    public Matrix3f matrix = new Matrix3f();
    private boolean matrixSet;

    public Vector3f speed = new Vector3f();
    public Vector3f acceleration = new Vector3f();
    public Vector3f accelerationFactor = new Vector3f(1, 1, 1);
    public float drag = 0;
    public float dragFactor = 0;

    /* Color */
    public float r = 1;
    public float g = 1;
    public float b = 1;
    public float a = 1;

    private Vector3d global = new Vector3d();

    public Map<String, Double> localValues = new HashMap<>();

    public Particle(int index, float offset)
    {
        this.index = index;
        this.offset = offset;

        this.speed.set((float) Math.random() - 0.5F, (float) Math.random() - 0.5F, (float) Math.random() - 0.5F);
        this.speed.normalize();
    }

    public void setDead()
    {
        this.dead = true;
    }

    public boolean isDead()
    {
        return this.dead;
    }

    public double getAge(float transition)
    {
        return (this.age + transition) / 20D;
    }

    public Vector3d getGlobalPosition(ParticleEmitter emitter)
    {
        return this.getGlobalPosition(emitter, this.position);
    }

    public Vector3d getGlobalPosition(ParticleEmitter emitter, Vector3d vector)
    {
        double px = vector.x;
        double py = vector.y;
        double pz = vector.z;

        if (this.relativePosition && this.relativeRotation)
        {
            Vector3f v = new Vector3f((float) px, (float) py, (float) pz);
            emitter.rotation.transform(v);

            px = v.x;
            py = v.y;
            pz = v.z;

            px += emitter.lastGlobal.x;
            py += emitter.lastGlobal.y;
            pz += emitter.lastGlobal.z;
        }

        this.global.set(px, py, pz);

        return this.global;
    }

    public void update(ParticleEmitter emitter)
    {
        this.prevRotation = this.rotation;
        this.prevPosition.set(this.position);

        this.setupMatrix(emitter);

        if (!this.manual)
        {
            float rotationAcceleration = this.rotationAcceleration / 20F -this.rotationDrag * this.rotationVelocity;

            this.rotationVelocity += rotationAcceleration / 20F;
            this.rotation = this.initialRotation + this.rotationVelocity * this.age;

            /* Position */
            Vector3f vec = new Vector3f(this.speed);
            vec.mul(-(this.drag + this.dragFactor));

            this.acceleration.add(vec);
            this.acceleration.div(20F);

            if (this.relativeVelocity)
            {
                if (this.age == 0)
                {
                    this.matrix.transform(this.speed);
                }

                this.speed.add(this.acceleration);

                vec.set(this.speed);

                vec.x *= this.accelerationFactor.x;
                vec.y *= this.accelerationFactor.y;
                vec.z *= this.accelerationFactor.z;
            }
            else
            {
                this.speed.add(this.acceleration);

                vec.set(this.speed);
                vec.x *= this.accelerationFactor.x;
                vec.y *= this.accelerationFactor.y;
                vec.z *= this.accelerationFactor.z;

                this.matrix.transform(vec);
            }

            if (this.age == 0)
            {
                vec.mul(1F + this.offset);
            }

            this.position.x += vec.x / 20F;
            this.position.y += vec.y / 20F;
            this.position.z += vec.z / 20F;
        }

        if (this.lifetime >= 0 && this.age >= this.lifetime)
        {
            this.setDead();
        }

        this.age += 1;
    }

    public void setupMatrix(ParticleEmitter emitter)
    {
        if (this.relativePosition)
        {
            if (this.relativeRotation)
            {
                this.matrix.identity();
            }
            else if (!this.matrixSet)
            {
                this.matrix.set(emitter.rotation);
                this.matrixSet = true;
            }
        }
        else if (this.relativeRotation)
        {
            this.matrix.set(emitter.rotation);
        }
        else if (this.relativeVelocity && !this.matrixSet)
        {
            this.matrix.set(emitter.rotation);
            this.matrixSet = true;
        }
        else if (this.textureScale && !this.matrixSet)
        {
            this.matrix.identity().scale(emitter.rotation.getRow(0, Vectors.TEMP_3F).length());
            this.matrixSet = true;
        }
    }
}