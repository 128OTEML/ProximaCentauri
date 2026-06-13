package Proxima.units;

import arc.math.*;
import arc.math.geom.*;
import arc.util.Time;
import mindustry.gen.*;

public class BasicMovementAI extends ProtectedAI {

    private final Vec2 wanderTarget = new Vec2();
    private float wanderCooldown = 0f;

    public BasicMovementAI() {
        attack = false;
    }

    @Override
    public void init() {
        wanderTarget.set(unit.x, unit.y);
    }

    @Override
    public void update() {
        if (wanderCooldown <= 0f) {
            float angle = Mathf.random(360f);
            float distance = Mathf.random(100f, 200f);
            wanderTarget.set(unit.x + Angles.trnsx(angle, distance),
                    unit.y + Angles.trnsy(angle, distance));
            wanderCooldown = Mathf.random(60f, 180f);
        } else {
            wanderCooldown -= Time.delta;
        }

        float angle = unit.angleTo(wanderTarget);
        // 使用速度移动而不是直接设置位置
        float moveSpeed = unit.type.speed * Time.delta;
        unit.vel.add(Angles.trnsx(angle, moveSpeed), Angles.trnsy(angle, moveSpeed));

        // 限制最大速度
        if (unit.vel.len() > unit.type.speed) {
            unit.vel.setLength(unit.type.speed);
        }

        unit.rotation = angle;
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