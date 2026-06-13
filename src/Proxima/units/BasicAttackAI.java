package Proxima.units;

import arc.math.*;
import arc.util.Time;
import mindustry.gen.*;
import mindustry.entities.bullet.*;

public class BasicAttackAI extends ProtectedAI {

    private float shootCooldown = 0f;
    private Teamc currentTarget = null;

    public BasicAttackAI() {
        attack = true;
    }

    @Override
    public void init() {
        retarget();
    }

    @Override
    public void update() {
        updateAutoParry();

        // 简化版：每60帧重新寻找目标
        if (Time.time % 60 < Time.delta) {
            retarget();
        }

        if (currentTarget != null) {
            float targetAngle = unit.angleTo(currentTarget);
            unit.rotation = Angles.moveToward(unit.rotation, targetAngle, unit.type.rotateSpeed);

            if (shootCooldown <= 0f) {
                shoot();
                shootCooldown = 30f;
            } else {
                shootCooldown -= Time.delta;
            }
        }
    }

    private void shoot() {
        if (currentTarget == null) return;

        float shootX = unit.x + Angles.trnsx(unit.rotation, unit.hitSize);
        float shootY = unit.y + Angles.trnsy(unit.rotation, unit.hitSize);

        // LaserBulletType 不需要纹理
        BulletType bullet = new LaserBulletType() {{
            damage = 15;
            lifetime = 30f;
            length = 200f;
            collidesGround = true;
            collidesAir = true;
        }};

        bullet.create(unit, unit.team, shootX, shootY, unit.rotation);
    }

    @Override
    public Teamc getTarget() {
        return currentTarget;
    }

    @Override
    public void setTarget(Teamc target) {
        this.currentTarget = target;
    }

    @Override
    public void retarget() {
        float nearestDist = Float.MAX_VALUE;
        Teamc nearest = null;

        // 寻找最近的敌方单位
        for (Unit u : Groups.unit) {
            if (u.team != unit.team && u.isAdded() && !u.dead) {
                float dist = unit.dst(u);
                if (dist < nearestDist && dist < 400f) {
                    nearestDist = dist;
                    nearest = u;
                }
            }
        }

        // 如果没有找到单位，尝试寻找敌方建筑
        if (nearest == null) {
            for (Building b : Groups.build) {
                if (b.team != unit.team && b.isAdded() && b.health > 0) {
                    float dist = unit.dst(b);
                    if (dist < nearestDist && dist < 400f) {
                        nearestDist = dist;
                        nearest = b;
                    }
                }
            }
        }

        currentTarget = nearest;
    }

    @Override
    public float weight() {
        return 10f;
    }

    @Override
    public boolean canKnockback() {
        return true;
    }

    @Override
    public boolean canTrail() {
        return true;
    }

    @Override
    public boolean updateAttackAI() {
        return true;
    }

    @Override
    public boolean updateMovementAI() {
        return true;
    }
}