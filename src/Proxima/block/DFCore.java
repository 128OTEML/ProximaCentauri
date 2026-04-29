package Proxima.block;

import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.graphics.g2d.TextureRegion;
import arc.math.Mathf;
import arc.struct.Seq;
import arc.util.Eachable;
import arc.util.io.Reads;
import arc.util.io.Writes;
import mindustry.content.Liquids;
import mindustry.entities.TargetPriority;
import mindustry.gen.Building;
import mindustry.gen.Groups;
import mindustry.graphics.Pal;
import mindustry.logic.LAccess;
import mindustry.type.Liquid;
import mindustry.ui.Bar;
import mindustry.world.Block;
import mindustry.world.blocks.storage.CoreBlock;
import mindustry.world.consumers.ConsumeLiquidFilter;
import mindustry.world.consumers.ConsumePower;
import mindustry.world.meta.BlockGroup;
import mindustry.world.meta.BlockStatus;
import mindustry.world.meta.Env;
import arc.graphics.g2d.Lines;
import arc.graphics.g2d.Fill;
import arc.util.Time;
import mindustry.world.meta.Stat;
import mindustry.world.meta.StatUnit;
import Proxima.effects.ProximaFX;
import Proxima.liquids.ProximaLiquids;

import static mindustry.Vars.*;

/**
 * DFC核心
 * 继承自 DFCBase，实现双液体容器功能
 * 拥有两个独立的液体容器，每个容量为1280000
 * 添加了热管理系统，参考NTM实现
 */
public class DFCore extends DFCBase {

    public float liquidCapacity1 = 1280000f;
    public float liquidCapacity2 = 1280000f;
    public float maxTemperature = 10000f;
    public float heatDissipationRate = 0.1f;
    public float heatGenerationRate = 0.5f;
    public float powerCapacity1 = 100000f; // 第一个电力容量，用于接收能量发射器的能量
    public float powerCapacity2 = 100000f; // 第二个电力容量

    public DFCore(String name) {
        super(name);
        this.range = 80f;
        this.rotateSpeed = 5f;
        this.activationTime = 0f;
        
        outputsPower = false;
        consumesPower = false;
        hasItems = false;
        hasLiquids = true;
        liquidCapacity = liquidCapacity1 + liquidCapacity2;
        // 移除consPower的设置，因为我们使用自定义的电力容量系统
        
        canOverdrive = false;
        envEnabled |= Env.space | Env.underwater | Env.any;
        destructible = true;
        update = true;
        solid = true;
        outputsLiquid = true;
        sync = true;
        outlineIcon = true;
        attacks = false;
        priority = TargetPriority.core;
        
        // 直接设置drawer，避免使用包含DrawPower的默认drawer
        drawer = new mindustry.world.draw.DrawMulti(
            new mindustry.world.draw.DrawDefault(),
            new mindustry.world.draw.DrawRegion("-top")
        );
    }

    @Override
    public void setStats() {
        super.setStats();
        stats.add(Stat.liquidCapacity, liquidCapacity1, StatUnit.liquidUnits);
        stats.add(Stat.liquidCapacity, liquidCapacity2, StatUnit.liquidUnits);
        stats.add(Stat.powerCapacity, powerCapacity1, StatUnit.powerUnits);
        stats.add(Stat.powerCapacity, powerCapacity2, StatUnit.powerUnits);
        stats.add(Stat.output, maxTemperature, StatUnit.none);
        stats.add(Stat.output, heatDissipationRate, StatUnit.none);
    }

    @Override
    public void setBars() {
        super.setBars();
        
        addBar("liquid1", (DFCoreBuild entity) ->
            new Bar(() ->
                entity.liquid1 == ProximaLiquids.dfcAntimatter ? "Antimatter" : entity.liquid1.localizedName,
                () -> entity.liquid1.color,
                () -> entity.liquidAmount1 / liquidCapacity1
            )
        );
        
        addBar("liquid2", (DFCoreBuild entity) ->
            new Bar(() ->
                entity.liquid2 == ProximaLiquids.dfcPositiveMatter ? "Positive matter" : entity.liquid2.localizedName,
                () -> entity.liquid2.color,
                () -> entity.liquidAmount2 / liquidCapacity2
            )
        );
        
        addBar("power1", (DFCoreBuild entity) ->
            new Bar(() -> "Power Capacity 1",
                () -> Color.yellow,
                () -> entity.powerAmount1 / powerCapacity1
            )
        );
        
        addBar("power2", (DFCoreBuild entity) ->
            new Bar(() -> "Power Capacity 2",
                () -> Color.orange,
                () -> entity.powerAmount2 / powerCapacity2
            )
        );
        
        addBar("temperature", (DFCoreBuild entity) ->
            new Bar(() -> "Temperature",
                () -> entity.temperature > maxTemperature * 0.8 ? Color.red : entity.temperature > maxTemperature * 0.5 ? Color.orange : Color.yellow,
                () -> entity.temperature / maxTemperature
            )
        );
    }

    public class DFCoreBuild extends DFCBlockBuild {
        public Liquid liquid1 = ProximaLiquids.dfcAntimatter;
        public float liquidAmount1 = 0f;
        
        public Liquid liquid2 = ProximaLiquids.dfcPositiveMatter;
        public float liquidAmount2 = 0f;
        
        public float powerAmount1 = 0f; // 第一个电力容量，用于接收能量发射器的能量
        public float powerAmount2 = 0f; // 第二个电力容量
        
        public float temperature = 25f; // 初始温度25°C

        @Override
        public void update() {
            super.update();
            
            if (liquidAmount1 > liquidCapacity1) {
                liquidAmount1 = liquidCapacity1;
            }
            if (liquidAmount2 > liquidCapacity2) {
                liquidAmount2 = liquidCapacity2;
            }
            if (powerAmount1 > powerCapacity1) {
                powerAmount1 = powerCapacity1;
            }
            if (powerAmount2 > powerCapacity2) {
                powerAmount2 = powerCapacity2;
            }
            
            // 热管理逻辑
            manageHeat();
            
            // 检查温度是否到达最大值，如果是则产生范围伤害
            checkTemperatureAndKill();
            
            // 检查资源并执行电力增值
            checkResourceAndGeneratePower();
        }
        
        /**
         * 检查温度是否到达最大值，如果是则产生范围伤害
         */
        private void checkTemperatureAndKill() {
            if (temperature >= maxTemperature) {
                // 执行范围无限大的kill命令
                executeKillCommand();
            }
        }
        
        /**
         * 执行范围无限大的kill命令
         */
        private void executeKillCommand() {
            // 播放爆炸效果
            ProximaFX.aoeExplosion2.at(x, y, 500f);
            ProximaFX.fragmentExplosion.at(x, y, 300f);
            ProximaFX.destroySparks.at(x, y, 0f, 200f); // 传递200f作为data参数
            ProximaFX.endDeath.at(x, y, 400f);
            ProximaFX.desGroundHitMain.at(x, y, 0f);
            
            // 杀死所有单位
            Groups.unit.each(unit -> {
                unit.kill();
            });
            
            // 摧毁所有建筑
            Groups.build.each(build -> {
                build.kill();
            });
        }
        
        /**
         * 热管理逻辑
         */
        private void manageHeat() {
            // 基于液体量和类型生成热量
            float heatGeneration = (liquidAmount1 + liquidAmount2) / (liquidCapacity1 + liquidCapacity2) * heatGenerationRate * Time.delta;
            
            // 热量传递和耗散
            temperature += heatGeneration;
            temperature -= heatDissipationRate * Time.delta;
            
            // 温度限制
            temperature = Math.max(25f, Math.min(maxTemperature, temperature));
        }
        
        /**
         * 检查结构内容量并执行电力增值
         */
        private void checkResourceAndGeneratePower() {
            // 检查是否同时存在20单位的正反物质
            if (liquidAmount1 >= 20 && liquidAmount2 >= 20) {
                // 检查第一电力容量是否存在10000单位电力
                if (powerAmount1 >= 10000) {
                    // 检查是否有正在工作的稳定器连接到核心
                    if (!hasWorkingStabilizer()) {
                        // 如果没有稳定器，直接爆炸
                        executeKillCommand();
                        return;
                    }
                    
                    // 执行NTM中的DFC电力增值逻辑
                    generatePower();
                }
            }
        }
        
        /**
         * 检查是否有正在工作的稳定器连接到核心
         */
        private boolean hasWorkingStabilizer() {
            // 检查所有建筑，寻找正在工作的DFC稳定器
            for (Building build : proximity) {
                if (build instanceof DFCStabilizer.DFCStabilizerBuild) {
                    DFCStabilizer.DFCStabilizerBuild stabilizer = (DFCStabilizer.DFCStabilizerBuild) build;
                    // 检查稳定器是否正在工作且目标是当前核心
                    if (stabilizer.isStabilizing && stabilizer.dfcCoreTarget == this) {
                        return true;
                    }
                }
            }
            return false;
        }
        
        /**
         * 按照NTM中的DFC电力增值逻辑生成电力
         */
        private void generatePower() {
            // 消耗200单位的反物质和正物质
            liquidAmount1 -= 200;
            liquidAmount2 -= 200;
            
            // 消耗5000单位的电力
            powerAmount1 -= 5000;
            
            // 生成电力增值（这里按照NTM的逻辑，生成50000单位电力到第二个电力容量）
            powerAmount2 += 50000;
            if (powerAmount2 > powerCapacity2) {
                powerAmount2 = powerCapacity2;
            }
            
            // 增加核心热量
            absorbHeat(1000f); // 增加1000单位的热量
            
            // 通知能量发射器发生了能量增值
            notifyEnergyEmitters();
        }
        
        /**
         * 通知能量发射器发生了能量增值
         */
        private void notifyEnergyEmitters() {
            // 查找附近的能量发射器并通知它们
            for (Building build : proximity) {
                if (build instanceof DFCEnergyEmitter.DFCEnergyEmitterBuild) {
                    DFCEnergyEmitter.DFCEnergyEmitterBuild emitter = (DFCEnergyEmitter.DFCEnergyEmitterBuild) build;
                    if (emitter.dfcCoreTarget == this) {
                        emitter.onEnergyAmplification();
                    }
                }
            }
        }
        
        /**
         * 吸收热量
         */
        public void absorbHeat(float amount) {
            temperature = Math.min(maxTemperature, temperature + amount);
        }
        
        /**
         * 释放热量
         */
        public void releaseHeat(float amount) {
            temperature = Math.max(25f, temperature - amount);
        }
        
        /**
         * 接收能量到第一个电力容量
         */
        public void receiveEnergy(float amount) {
            powerAmount1 += amount;
            if (powerAmount1 > powerCapacity1) {
                powerAmount1 = powerCapacity1;
            }
        }

        @Override
        public boolean acceptLiquid(Building source, Liquid liquid) {
            // 第一个容量只接受反物质
            if (liquidAmount1 < liquidCapacity1 && (liquid1 == liquid || liquidAmount1 == 0) && liquid == ProximaLiquids.dfcAntimatter) {
                return true;
            }
            // 第二个容量只接受正物质
            if (liquidAmount2 < liquidCapacity2 && (liquid2 == liquid || liquidAmount2 == 0) && liquid == ProximaLiquids.dfcPositiveMatter) {
                return true;
            }
            return false;
        }

        @Override
        public void handleLiquid(Building source, Liquid liquid, float amount) {
            // 第一个容量只接受反物质
            if (liquidAmount1 < liquidCapacity1 && (liquid1 == liquid || liquidAmount1 == 0) && liquid == ProximaLiquids.dfcAntimatter) {
                if (liquidAmount1 == 0) {
                    liquid1 = liquid;
                }
                liquidAmount1 += amount;
                if (liquidAmount1 > liquidCapacity1) {
                    liquidAmount1 = liquidCapacity1;
                }
            } else if (liquidAmount2 < liquidCapacity2 && (liquid2 == liquid || liquidAmount2 == 0) && liquid == ProximaLiquids.dfcPositiveMatter) {
                // 第二个容量只接受正物质
                if (liquidAmount2 == 0) {
                    liquid2 = liquid;
                }
                liquidAmount2 += amount;
                if (liquidAmount2 > liquidCapacity2) {
                    liquidAmount2 = liquidCapacity2;
                }
            }
        }

        public float getLiquid(Liquid liquid) {
            float total = 0f;
            if (liquid1 == liquid) {
                total += liquidAmount1;
            }
            if (liquid2 == liquid) {
                total += liquidAmount2;
            }
            return total;
        }

        public float liquidCapacity() {
            return liquidCapacity1 + liquidCapacity2;
        }

        public float liquidsTotal() {
            return liquidAmount1 + liquidAmount2;
        }
        
        public float powerCapacity() {
            return powerCapacity1 + powerCapacity2;
        }
        
        public float powerTotal() {
            return powerAmount1 + powerAmount2;
        }

        @Override
        public void write(Writes write) {
            super.write(write);
            write.s(liquid1.id);
            write.f(liquidAmount1);
            write.s(liquid2.id);
            write.f(liquidAmount2);
            write.f(powerAmount1);
            write.f(powerAmount2);
            write.f(temperature);
        }

        @Override
        public void read(Reads read, byte revision) {
            super.read(read, revision);
            liquid1 = content.liquid(read.s());
            liquidAmount1 = read.f();
            liquid2 = content.liquid(read.s());
            liquidAmount2 = read.f();
            powerAmount1 = read.f();
            powerAmount2 = read.f();
            temperature = read.f();
        }

        @Override
        public double sense(LAccess sensor) {
            if (sensor == LAccess.totalLiquids) {
                return liquidsTotal();
            }
            if (sensor == LAccess.liquidCapacity) {
                return liquidCapacity();
            }
            if (sensor == LAccess.progress) {
                return powerAmount1 / powerCapacity1;
            }
            return super.sense(sensor);
        }

        @Override
        public Object senseObject(LAccess sensor) {
            return super.senseObject(sensor);
        }

        @Override
        public void draw() {
            super.draw();
            
            // 绘制温度相关的视觉效果
            drawTemperatureEffects();
        }
        
        /**
         * 绘制温度相关的视觉效果
         */
        private void drawTemperatureEffects() {
            float tempRatio = temperature / maxTemperature;
            float alpha = Mathf.clamp(tempRatio - 0.5f, 0f, 1f) * 2f;
            
            // 根据温度显示不同的颜色
            Color glowColor;
            if (tempRatio > 0.8) {
                glowColor = Color.red;
            } else if (tempRatio > 0.6) {
                glowColor = Color.orange;
            } else if (tempRatio > 0.4) {
                glowColor = Color.yellow;
            } else {
                glowColor = Color.green;
            }
            
            // 绘制核心发光效果
            if (alpha > 0) {
                float size = tilesize * 3 * (1f + tempRatio * 0.5f);
                Draw.color(glowColor, alpha * 0.3f);
                Fill.circle(x, y, size);
                
                // 绘制脉动效果
                float pulse = Mathf.absin(Time.time, 2f, 0.2f) + 0.8f;
                Draw.color(glowColor, alpha * 0.6f * pulse);
                Lines.stroke(2f * alpha * pulse);
                Lines.circle(x, y, size * 0.8f);
                
                Draw.reset();
            }
            
            // 绘制液体状态指示器
            drawLiquidIndicators();
        }
        
        /**
         * 绘制液体状态指示器
         */
        private void drawLiquidIndicators() {
            // 绘制液体1指示器
            if (liquidAmount1 > 0) {
                float liquidRatio1 = liquidAmount1 / liquidCapacity1;
                float height1 = tilesize * 3 * liquidRatio1;
                
                Draw.color(liquid1.color, 0.6f);
                Fill.rect(x - tilesize * 0.8f, y - tilesize * 1.5f + height1 / 2, tilesize * 0.3f, height1);
            }
            
            // 绘制液体2指示器
            if (liquidAmount2 > 0) {
                float liquidRatio2 = liquidAmount2 / liquidCapacity2;
                float height2 = tilesize * 3 * liquidRatio2;
                
                Draw.color(liquid2.color, 0.6f);
                Fill.rect(x + tilesize * 0.8f, y - tilesize * 1.5f + height2 / 2, tilesize * 0.3f, height2);
            }
            
            Draw.reset();
        }
    }
}
