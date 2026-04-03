package IcePeak.unit.chordon;

import arc.math.*;
import arc.math.geom.*;
import arc.util.*;
import IcePeak.bullets.*;
import IcePeak.entities.*;
import mindustry.entities.*;
import mindustry.gen.*;

public class PinAttack extends AttackAI{
    float reloadTime = 0f;
    float barrageTime = 0f, spawnTimer = 0f;
    float bulletHellTime = 120f;
    int barrages = 0;

    float targetX = 0f;
    float targetY = 0f;

    @Override
    void reset(){
        reloadTime = 0f;
        barrageTime = 0f;
        spawnTimer = 0f;
        bulletHellTime = 0f;
        barrages = 0;
    }

    @Override
    boolean teleSwapCompatible(){
        return true;
    }

    @Override
    void update(){
        Teamc target = unit.getTarget();
        if(target != null){
            targetX = target.getX();
            targetY = target.getY();

            if(unit.within(target, 620f)){
                if(reloadTime <= 0f){
                    reloadTime = Mathf.random(60f, 120f);
                    barrageTime = Mathf.random(15f, 60f);
                }
                if(bulletHellTime <= 0f){
                    bulletHellTime = 5f * 60f;
                }
                unit.rotate(2f, unit.angleTo(target), 15f);
            }
        }

        spawnTimer = Math.max(0f, spawnTimer - Time.delta);
        if(barrageTime > 0){
            if(spawnTimer <= 0f){
                Vec2 v = Tmp.v1.trns(unit.rotation, -40f, Mathf.range(40f)).add(unit);

                float vx = 0f;
                float vy = 0f;
                if(target instanceof Hitboxc h){
                    vx = h.deltaX();
                    vy = h.deltaY();
                }

                Vec2 in = Tmp.v2.set(targetX, targetY);
                if(Mathf.chance(0.5f)) in = Predict.intercept(v.x, v.y, targetX, targetY, vx, vy, 10f);

                FlameBullets.pin.create(unit, unit.team, v.x, v.y, v.angleTo(in));
                Sounds.massdriver.at(v.x, v.y, 2f);

                spawnTimer = unit.isDecoy() ? 6f : 3f;
            }
            barrageTime -= Time.delta;
            if(barrageTime <= 0f){
                barrages++;
                if(barrages >= 4 || quickSwap || unit.useLethal()){
                    barrages = 0;
                    //reloadTime = 0f;
                    unit.randAI(true, unit.health < 50f);
                }
            }
        }else{
            reloadTime = Math.max(0f, reloadTime - Time.delta);
        }
    }

    @Override
    void updatePassive(){
        bulletHellTime -= Time.delta;
    }

    @Override
    void endOnce(){
        reset();
    }
}
