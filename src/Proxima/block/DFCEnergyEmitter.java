package Proxima.block;

import arc.graphics.Color;
import arc.math.Mathf;
import mindustry.content.Fx;
import mindustry.entities.Effect;
import mindustry.entities.TargetPriority;
import mindustry.gen.Building;
import mindustry.graphics.Drawf;
import mindustry.graphics.Pal;
import mindustry.logic.LAccess;
import mindustry.type.Item;
import mindustry.type.Liquid;
import mindustry.ui.Bar;
import mindustry.world.Edges;
import mindustry.world.Tile;
import mindustry.world.blocks.production.GenericCrafter;
import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.struct.*;
import arc.util.*;
import mindustry.entities.units.*;
import mindustry.gen.*;
import mindustry.world.blocks.storage.CoreBlock;
import mindustry.world.consumers.ConsumeCoolant;
import mindustry.world.consumers.ConsumeLiquidBase;
import mindustry.world.draw.*;
import mindustry.world.meta.*;
import mindustry.world.blocks.*;
import mindustry.world.blocks.RotBlock;

import static mindustry.Vars.*;

/**
 * DFC能量发射器
 * 为DFC核心提供能量
 */
public class DFCEnergyEmitter extends DFCLaserEmitter {

    public float energyRange = 60f;
    public float energyTransferRate = 10f;
    public float beamWidth = 6f;
    public Color beamColor = Color.valueOf("ffcc00");
    public Color beamColorEnd = Color.valueOf("ff9900");
    public float powerConsumption = 10f;
    public float coolantConsumption = 1f;

    public DFCEnergyEmitter(String name) {
        super(name);
        size = 3;
        hasLiquids = true;
        outputsLiquid = false;
        liquidCapacity = 64000f;
        allowResupply = true;
        sync = true;
        range = energyRange;
        consumesPower = true;
        consumePower(powerConsumption);
    }

    @Override
    public void setStats() {
        super.setStats();
        stats.add(Stat.range, energyRange, StatUnit.blocks);
        stats.add(Stat.liquidCapacity, liquidCapacity, StatUnit.liquidUnits);
        stats.add(Stat.powerUse, powerConsumption, StatUnit.powerUnits);
        stats.add(Stat.output, energyTransferRate, StatUnit.powerUnits);
    }

    @Override
    public void setBars() {
        super.setBars();
        addBar("liquid", (DFCEnergyEmitterBuild entity) -> new Bar(() -> "bar.liquid", () -> entity.liquids.current() == null ? Pal.gray : entity.liquids.current().color, () -> entity.liquids.currentAmount() / liquidCapacity));
    }

    @Override
    public void drawPlace(int x, int y, int rotation, boolean valid) {
        super.drawPlace(x, y, rotation, valid);
        Drawf.dashCircle(x * tilesize + offset, y * tilesize + offset, energyRange, Pal.placing);
    }

    public class DFCEnergyEmitterBuild extends DFCLaserEmitterBuild {
        public float energy = 0f;
        public float maxEnergy = 1000f;
        public boolean energyAmplification = false;
        public float amplificationTimer = 0f;
        public float amplificationDuration = 300f; // 能量增值效果持续时间（帧）

        @Override
        public void update() {
            super.update();

            // 消耗冷却液
            if (power != null && power.status >= 1f && liquids.currentAmount() >= coolantConsumption) {
                liquids.remove(liquids.current(), coolantConsumption);
                energy = Math.min(maxEnergy, energy + energyTransferRate * Time.delta);
            }

            // 尝试找到DFC核心并传输能量
            if (energy > 0 && dfcCoreTarget != null && dfcCoreTarget instanceof DFCore.DFCoreBuild && dfcCoreTarget.isValid()) {
                transferEnergy((DFCore.DFCoreBuild) dfcCoreTarget);
            } else if (energy > 0) {
                // 如果没有选择核心，尝试自动寻找附近的DFC核心
                findDFCCore();
            }
            
            // 处理能量增值效果
            if (energyAmplification) {
                amplificationTimer += Time.delta;
                if (amplificationTimer >= amplificationDuration) {
                    energyAmplification = false;
                    amplificationTimer = 0f;
                }
            }
        }

        /**
         * 向DFC核心传输能量
         */
        private void transferEnergy(DFCore.DFCoreBuild core) {
            float transferAmount = Math.min(energy, energyTransferRate * Time.delta);
            energy -= transferAmount;
            core.receiveEnergy(transferAmount);
        }
        
        /**
         * 当能量增值时被调用
         */
        public void onEnergyAmplification() {
            energyAmplification = true;
            amplificationTimer = 0f;
        }
        
        /**
         * 自动寻找附近的DFC核心
         */
        private void findDFCCore() {
            float bestDistance = Float.MAX_VALUE;
            Building bestCore = null;
            
            // 搜索附近的DFC核心
            for (Building build : proximity) {
                if (build instanceof DFCore.DFCoreBuild && build.team == team) {
                    float distance = Mathf.dst(x, y, build.x, build.y);
                    if (distance <= energyRange && distance < bestDistance) {
                        bestDistance = distance;
                        bestCore = build;
                    }
                }
            }
            
            // 如果找到核心，设置为目标
            if (bestCore != null) {
                dfcCoreTarget = bestCore;
            }
        }

        @Override
        public void draw() {
            super.draw();

            // 绘制连接光束
            if (energy > 0 && dfcCoreTarget != null && dfcCoreTarget instanceof DFCore.DFCoreBuild && dfcCoreTarget.isValid()) {
                float alpha = 0.8f;
                float currentBeamWidth = beamWidth;
                
                // 能量增值时延长激光并增加宽度
                if (energyAmplification) {
                    currentBeamWidth *= 2f;
                    alpha = 1f;
                }
                
                // 绘制光束
                Lines.stroke(currentBeamWidth, beamColor);
                Lines.line(x, y, dfcCoreTarget.x, dfcCoreTarget.y);
                
                // 绘制光束末端效果
                Drawf.light(x, y, dfcCoreTarget.x, dfcCoreTarget.y, currentBeamWidth * 2, beamColor, alpha);
                Drawf.light(dfcCoreTarget.x, dfcCoreTarget.y, currentBeamWidth * 3, beamColorEnd, alpha);
                
                // 能量增值时绘制额外的视觉效果
                if (energyAmplification) {
                    // 绘制脉冲效果
                    float pulse = Mathf.absin(Time.time, 1f, 1f);
                    Lines.stroke(currentBeamWidth * 0.5f + pulse, beamColorEnd);
                    Lines.line(x, y, dfcCoreTarget.x, dfcCoreTarget.y);
                }
            }
        }

        @Override
        public void drawConfigure() {
            super.drawConfigure();
            
            // 绘制能量传输范围
            Drawf.dashCircle(x, y, energyRange, team.color);
        }

        @Override
        public double sense(LAccess sensor) {
            if (sensor == LAccess.totalLiquids) {
                return liquids.currentAmount();
            }
            if (sensor == LAccess.liquidCapacity) {
                return liquidCapacity;
            }
            if (sensor == LAccess.progress) {
                return energy / maxEnergy;
            }
            return super.sense(sensor);
        }
    }
}