package Proxima.block;

import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.struct.*;
import arc.util.*;
import Proxima.*;
import Proxima.graphics.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.entities.bullet.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.world.blocks.*;
import mindustry.world.blocks.defense.*;
import mindustry.world.blocks.defense.turrets.*;
import mindustry.world.meta.*;

import static mindustry.Vars.*;

public class EndAntiAirTurret extends Turret{
    protected float targetingRange = 700f;

    static Effect hitAirEffect = new Effect(10f, 800f * 3f, e -> {
        Draw.color(ProximaPal.red);
        GraphicUtils.diamond(e.x, e.y, 5f * e.fout(), e.color.r * 2f, e.rotation);
    });

    public EndAntiAirTurret(String name){
        super(name);
        size = 5;
        range = 700f;
        reload = 60f;
        targetAir = true;
        targetGround = false;
        rotateSpeed = 3f;
        health = 10000;
        requirements(Category.turret, ItemStack.with(
            Items.copper, 5000,
            Items.lead, 4000,
            Items.titanium, 3000,
            Items.thorium, 2000,
            Items.silicon, 1500
        ));
    }

    @Override
    public void setStats(){
        super.setStats();
        stats.add(Stat.range, range / tilesize, StatUnit.blocks);
        stats.add(Stat.damage, 9000f + " + maxHealth/15");
    }

    public class EndAntiAirTurretBuild extends TurretBuild{
        Seq<Teamc> targets = new Seq<>();
        Seq<Float> targetWarmups = new Seq<>();

        @Override
        public void updateTile(){
            super.updateTile();
            
            // 清理无效目标
            targets.removeAll(t -> t == null || !t.isAdded() || (t instanceof Healthc h && !h.isValid()) || !Mathf.within(x, y, t.x(), t.y(), range + (t instanceof Sized s ? s.hitSize() / 2f : 0f)));
            targetWarmups.truncate(targets.size);
            
            // 寻找新目标
            if(targets.size < 1600){
                Utils.scanEnemies(team, x, y, range, true, false, u -> {
                    if(u instanceof Unit && !targets.contains(u)){
                        targets.add(u);
                        targetWarmups.add(0f);
                    }
                });
            }
            
            // 更新目标预热
            for(int i = 0; i < targets.size; i++){
                targetWarmups.set(i, Mathf.approachDelta(targetWarmups.get(i), 1f, 1f / 30f));
            }
            
            // 开火逻辑
            if(reload >= EndAntiAirTurret.this.reload && targets.size > 0){
                for(int i = 0; i < targets.size; i++){
                    Teamc t = targets.get(i);
                    
                    float mx = (x + t.x()) / 2f;
                    float my = (y + t.y()) / 2f;
                    float dst = t.dst(x, y);
                    hitAirEffect.at(mx, my, t.angleTo(x, y));
                    
                    if(t instanceof Healthc h){
                        h.damage(h.maxHealth() / 15f + 9000f);
                    }
                }
                reload = 0f;
            }
        }

        @Override
        public void draw(){
            super.draw();
            
            // 绘制激光效果
            float z = Draw.z();
            
            // 主激光
            Draw.z(Layer.bullet);
            Draw.color(ProximaPal.red);
            for(int i = 0; i < targets.size; i++){
                Teamc t = targets.get(i);
                float warm = targetWarmups.get(i);
                
                float progress = reload / EndAntiAirTurret.this.reload;
                float width = warm * Interp.pow2Out.apply(progress) * (3f + Mathf.absin(Mathf.pow(progress, 4f) * EndAntiAirTurret.this.reload, 1.5f, 2.5f) + Mathf.absin(3f, 0.2f));
                
                // 绘制主激光线
                Lines.stroke(width * 1.25f);
                Lines.line(x, y, t.x(), t.y(), false);
                
                // 绘制能量球
                float ballSize = width * 2f;
                Fill.circle(x, y, ballSize);
                Fill.circle(t.x(), t.y(), ballSize);
                
                // 绘制脉冲效果
                float pulse = 1f + Mathf.absin(Time.time, 2f, 0.3f);
                float pulseSize = ballSize * pulse;
                Draw.color(ProximaPal.red, 0.5f);
                Fill.circle(x, y, pulseSize);
                Fill.circle(t.x(), t.y(), pulseSize);
            }
            
            // 辅助激光
            Draw.z(Layer.bullet + 0.01f);
            Draw.color(Color.white, 0.8f);
            for(int i = 0; i < targets.size; i++){
                Teamc t = targets.get(i);
                float warm = targetWarmups.get(i);
                
                float progress = reload / EndAntiAirTurret.this.reload;
                float width = warm * Interp.pow2Out.apply(progress) * (2f + Mathf.absin(Mathf.pow(progress, 4f) * EndAntiAirTurret.this.reload, 1f, 1.5f) + Mathf.absin(2f, 0.1f));
                
                // 绘制辅助激光线
                Lines.stroke(width * 0.5f);
                Lines.line(x, y, t.x(), t.y(), false);
                
                // 绘制细小的能量点
                float segments = Math.max(2, (int)(t.dst(x, y) / 30f));
                for(int j = 0; j < segments; j++){
                    float f = j / (float)(segments - 1);
                    float px = Mathf.lerp(x, t.x(), f);
                    float py = Mathf.lerp(y, t.y(), f);
                    float dotSize = width * 0.8f * (1f + Mathf.sin(Time.time + j * 10f) * 0.3f);
                    Fill.circle(px, py, dotSize);
                }
            }
            
            // 重置颜色和层级
            Draw.color();
            Draw.z(z);
        }
    }
}