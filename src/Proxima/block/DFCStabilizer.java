package Proxima.block;

import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.graphics.g2d.Lines;
import arc.graphics.g2d.TextureRegion;
import arc.math.Mathf;
import arc.struct.Seq;
import arc.util.Eachable;
import arc.util.Time;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.content.Liquids;
import mindustry.content.Items;
import mindustry.entities.TargetPriority;
import mindustry.gen.Building;
import mindustry.graphics.Drawf;
import mindustry.graphics.Pal;
import mindustry.logic.LAccess;
import mindustry.type.Liquid;
import mindustry.ui.Bar;
import mindustry.world.Block;
import mindustry.world.blocks.storage.CoreBlock;
import mindustry.world.consumers.ConsumeLiquidFilter;
import mindustry.world.meta.BlockGroup;
import mindustry.world.meta.BlockStatus;
import mindustry.world.meta.Env;
import mindustry.world.meta.Stat;
import mindustry.world.meta.StatUnit;

import static mindustry.Vars.*;

/**
 * DFC稳定器
 * 继承自 DFCLaserEmitter，用于稳定DFC核心
 * 提供稳定化效果，消耗电力，控制核心温度
 */
public class DFCStabilizer extends DFCLaserEmitter {

    // 稳定化效果范围
    public float stabilizerRange = 60f;
    
    // 电力消耗
    public float powerConsumption = 10f;
    
    // 稳定化强度
    public float stabilizationStrength = 0.1f;
    
    // 目标温度范围
    public float targetTemperature = 5000f;
    public float temperatureRange = 500f;

    public DFCStabilizer(String name) {
        super(name);
        this.range = stabilizerRange;
        this.rotateSpeed = 2f;
        this.activationTime = 60f;
        
        // 设置建筑大小为3
        size = 3;
        
        // 消耗电力
        outputsPower = false;
        consumesPower = true;
        hasItems = false;
        hasLiquids = false;
        
        // 启用配置功能
        configurable = true;
        saveConfig = false;
        
        // 不攻击，只稳定
        attacks = false;
        
        // 添加电力消耗
        consumePower(powerConsumption);
        
        coolantMultiplier = 1f;
    }
    
    @Override
    public void setStats() {
        super.setStats();
        stats.add(Stat.powerUse, powerConsumption, StatUnit.powerUnits);
        stats.add(Stat.range, stabilizerRange, StatUnit.blocks);
        stats.add(Stat.output, stabilizationStrength, StatUnit.none);
        stats.add(Stat.output, targetTemperature, StatUnit.none);
    }

    @Override
    public void setBars() {
        super.setBars();
        addBar("stabilization", (DFCStabilizerBuild entity) -> new Bar(() -> "Stabilization", () -> entity.isStabilizing ? Color.valueOf("00ff88") : Pal.gray, () -> entity.isStabilizing ? 1f : 0f));
    }

    public class DFCStabilizerBuild extends DFCLaserEmitterBuild {
        // 稳定化状态
        public boolean isStabilizing;
        public float stabilizationProgress = 0f;

        @Override
        public void update() {
            super.update();
            
            // 稳定化逻辑 - 使用继承自DFCLaserEmitter的dfcCoreTarget
            if (dfcCoreTarget != null && !state.isPaused()) {
                if (power != null && power.status >= 1f && dfcCoreTarget instanceof DFCore.DFCoreBuild) {
                    isStabilizing = true;
                    
                    // 提供稳定化效果
                    stabilizeCore((DFCore.DFCoreBuild) dfcCoreTarget);
                } else {
                    isStabilizing = false;
                    stabilizationProgress = 0f;
                }
            } else {
                isStabilizing = false;
                stabilizationProgress = 0f;
            }
        }
        
        /**
         * 稳定DFC核心
         */
        private void stabilizeCore(DFCore.DFCoreBuild core) {
            // 计算温度差
            float tempDiff = core.temperature - targetTemperature;
            
            // 计算稳定化效果
            float stabilizationEffect = 0f;
            if (Math.abs(tempDiff) > temperatureRange) {
                // 温度超出范围，进行稳定化
                stabilizationEffect = -tempDiff * stabilizationStrength * Time.delta;
                
                // 应用稳定化效果
                if (tempDiff > 0) {
                    core.releaseHeat(Math.abs(stabilizationEffect));
                } else {
                    core.absorbHeat(Math.abs(stabilizationEffect));
                }
                
                // 更新稳定化进度
                stabilizationProgress = Mathf.clamp(stabilizationProgress + 0.01f * Time.delta, 0f, 1f);
            } else {
                // 温度在范围内，维持稳定
                stabilizationProgress = Mathf.clamp(stabilizationProgress - 0.005f * Time.delta, 0f, 1f);
            }
        }
        
        @Override
        public void draw() {
            super.draw();
            
            // 绘制稳定化光束
            if (isStabilizing && dfcCoreTarget != null && dfcCoreTarget instanceof DFCore.DFCoreBuild) {
                DFCore.DFCoreBuild core = (DFCore.DFCoreBuild) dfcCoreTarget;
                float tempDiff = Math.abs(core.temperature - targetTemperature);
                float intensity = Mathf.clamp(tempDiff / 1000f, 0.1f, 1f);
                float alpha = 0.6f * intensity;
                
                // 计算1.5个游戏格前的发射点
                float rad = rotation * Mathf.degRad;
                float startX = x + Mathf.cos(rad) * 1.5f * tilesize;
                float startY = y + Mathf.sin(rad) * 1.5f * tilesize;
                
                // 绘制稳定化光束
                drawStabilizationBeam(startX, startY, dfcCoreTarget.x, dfcCoreTarget.y, alpha, intensity);
            }
        }
        
        /**
         * 绘制稳定化光束
         */
        private void drawStabilizationBeam(float startX, float startY, float endX, float endY, float alpha, float intensity) {
            float length = Mathf.dst(startX, startY, endX, endY);
            int count = (int)(length / 12f) + 2;
            
            // 稳定化光束宽度
            float beamWidth = 2f * alpha * intensity;
            
            // 绘制稳定化光束
            Draw.color(Color.valueOf("00ff88").cpy().a(0.8f * alpha));
            Lines.stroke(beamWidth * 2f);
            Lines.beginLine();
            
            for(int i = 0; i < count; i++){
                float fin = i / (count - 1f);
                float x = Mathf.lerp(startX, endX, fin);
                float y = Mathf.lerp(startY, endY, fin);
                
                // 添加波动效果，根据稳定化进度变化
                if (fin > 0 && fin < 1) {
                    float waveValue = Mathf.sin(fin * 10f + Time.time / 15f) * 2f * (1f - Math.abs(fin - 0.5f) * 2f) * intensity;
                    float dx = endX - startX;
                    float dy = endY - startY;
                    float perpX = -dy / length;
                    float perpY = dx / length;
                    x += perpX * waveValue;
                    y += perpY * waveValue;
                }
                
                Lines.linePoint(x, y);
            }
            
            Lines.endLine(false);
            
            // 绘制内层光束
            Draw.color(Color.valueOf("00ffcc").cpy().a(alpha));
            Lines.stroke(beamWidth);
            Lines.beginLine();
            
            for(int i = 0; i < count; i++){
                float fin = i / (count - 1f);
                float x = Mathf.lerp(startX, endX, fin);
                float y = Mathf.lerp(startY, endY, fin);
                Lines.linePoint(x, y);
            }
            
            Lines.endLine(false);
            
            // 发光效果
            Drawf.light(startX, startY, endX, endY, 30f * alpha * intensity, Color.valueOf("00ff88"), 0.5f * alpha);
            
            // 稳定化粒子效果
            if (Mathf.chance(0.1f * intensity)) {
                float randomX = Mathf.random(startX, endX);
                float randomY = Mathf.random(startY, endY);
                Drawf.tri(randomX, randomY, 3f * intensity, 6f * intensity, Mathf.random(360f));
            }
            
            Draw.reset();
        }
        
        @Override
        public void drawConfigure() {
            super.drawConfigure();
            
            // 绘制稳定化范围
            Drawf.dashCircle(x, y, stabilizerRange, team.color);
        }
        

        
        @Override
        public Object senseObject(LAccess sensor) {
            return super.senseObject(sensor);
        }
        
        @Override
        public double sense(LAccess sensor) {
            if (sensor == LAccess.totalLiquids) {
                return dfcCoreTarget != null && dfcCoreTarget instanceof DFCore.DFCoreBuild ? ((DFCore.DFCoreBuild)dfcCoreTarget).liquidsTotal() : 0;
            }
            if (sensor == LAccess.liquidCapacity) {
                return dfcCoreTarget != null && dfcCoreTarget instanceof DFCore.DFCoreBuild ? ((DFCore.DFCoreBuild)dfcCoreTarget).liquidCapacity() : 0;
            }
            return super.sense(sensor);
        }
    }
}