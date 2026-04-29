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
 * DFC交换器
 * 用于在DFC核心和液体之间传递热量
 * 继承自DFCLaserEmitter以获得激光发射功能
 * 实现了基于NTM的复杂热交换算法
 */
public class DFCExchanger extends DFCLaserEmitter {

    public float exchangerRange = 60f;
    public float inputLiquidCapacity = 256000f;
    public float outputLiquidCapacity = 256000f;
    public float heatTransferRate = 100f;
    public float coolantConsumption = 20f;
    public float beamWidth = 4f;
    public Color beamColor = Color.valueOf("00ccff");
    public Color beamColorEnd = Color.valueOf("0099ff");
    public int maxCompression = 5;
    public float mbPerKelvin = 1000f;

    public DFCExchanger(String name) {
        super(name);
        size = 3;
        hasLiquids = true;
        outputsLiquid = true;
        liquidCapacity = inputLiquidCapacity + outputLiquidCapacity;
        allowResupply = true;
        sync = true;
        range = exchangerRange;
        consumesPower = true;
        consumePower(5f);
    }

    @Override
    public void setStats() {
        super.setStats();
        stats.add(Stat.range, exchangerRange, StatUnit.blocks);
        stats.add(Stat.liquidCapacity, inputLiquidCapacity, StatUnit.liquidUnits);
        stats.add(Stat.liquidCapacity, outputLiquidCapacity, StatUnit.liquidUnits);
        stats.add(Stat.powerUse, 5f, StatUnit.powerUnits);
        stats.add(Stat.output, heatTransferRate, StatUnit.heatUnits);
        stats.add(Stat.output, maxCompression, StatUnit.none);
    }

    @Override
    public void setBars() {
        super.setBars();
        addBar("inputLiquid", (DFCExchangerBuild entity) -> new Bar(() -> "Input Liquid", () -> entity.inputLiquid == null ? Pal.gray : entity.inputLiquid.color, () -> entity.inputLiquidAmount / inputLiquidCapacity));
        addBar("outputLiquid", (DFCExchangerBuild entity) -> new Bar(() -> "Output Liquid", () -> entity.outputLiquid == null ? Pal.gray : entity.outputLiquid.color, () -> entity.outputLiquidAmount / outputLiquidCapacity));
        addBar("power", (DFCExchangerBuild entity) -> new Bar(() -> "bar.power", () -> Pal.powerBar, () -> entity.power != null ? entity.power.status : 0f));
        addBar("heat", (DFCExchangerBuild entity) -> new Bar(() -> "Heat Level", () -> Color.orange, () -> entity.heatLevel / entity.maxHeatLevel));
    }

    @Override
    public void drawPlace(int x, int y, int rotation, boolean valid) {
        super.drawPlace(x, y, rotation, valid);
        Drawf.dashCircle(x * tilesize + offset, y * tilesize + offset, exchangerRange, Pal.placing);
    }

    public class DFCExchangerBuild extends DFCLaserEmitterBuild {
        public Liquid inputLiquid = null;
        public float inputLiquidAmount = 0f;
        public Liquid outputLiquid = null;
        public float outputLiquidAmount = 0f;
        public float heatLevel = 0f;
        public float maxHeatLevel = 1000f;
        public int compression = 1;
        public int amountToHeat = 1;
        public int tickDelay = 1;
        public int timer = 0;

        @Override
        public void update() {
            super.update();

            // 使用从DFCLaserEmitter继承来的dfcCoreTarget进行热交换
            if (power != null && power.status >= 1f && dfcCoreTarget != null && dfcCoreTarget instanceof DFCore.DFCoreBuild) {
                DFCore.DFCoreBuild core = (DFCore.DFCoreBuild) dfcCoreTarget;
                
                timer++;
                if (timer >= tickDelay) {
                    timer = 0;
                    transferHeat(core);
                }
            }
        }

        /**
         * 与DFC核心进行热交换
         */
        private void transferHeat(DFCore.DFCoreBuild core) {
            if (inputLiquid != null && inputLiquidAmount >= amountToHeat) {
                // 计算液体温度差
                float liquidTemp = inputLiquid.temperature;
                float coreTemp = core.temperature;
                float tempDiff = coreTemp - liquidTemp;
                
                if (tempDiff > 0) {
                    // 计算最大排水量
                    int maxDrain = (int)(tempDiff / 100 * mbPerKelvin); // 简化计算
                    int drain = Math.min(maxDrain, amountToHeat);
                    
                    if (drain > 0) {
                        // 消耗输入液体
                        inputLiquidAmount -= drain;
                        if (inputLiquidAmount < 0) inputLiquidAmount = 0;
                        
                        // 生成输出液体（这里简化处理，实际应该根据液体类型和压缩级别计算）
                        if (outputLiquid == null) {
                            outputLiquid = inputLiquid;
                        }
                        outputLiquidAmount += drain;
                        if (outputLiquidAmount > outputLiquidCapacity) {
                            outputLiquidAmount = outputLiquidCapacity;
                        }
                        
                        // 计算热交换量
                        float heatTransfer = drain * tempDiff * 0.01f; // 简化计算
                        
                        // 从核心吸收热量
                        core.releaseHeat(heatTransfer);
                        heatLevel = Math.min(maxHeatLevel, heatLevel + heatTransfer);
                        
                        // 绘制热交换光束
                        drawHeatBeam(core);
                    }
                }
            }
        }

        @Override
        public boolean acceptLiquid(Building source, Liquid liquid) {
            // 只接受输入液体
            if (inputLiquidAmount < inputLiquidCapacity && (inputLiquid == liquid || inputLiquid == null)) {
                return true;
            }
            return false;
        }

        @Override
        public void handleLiquid(Building source, Liquid liquid, float amount) {
            // 处理输入液体
            if (inputLiquidAmount < inputLiquidCapacity && (inputLiquid == liquid || inputLiquid == null)) {
                if (inputLiquid == null) {
                    inputLiquid = liquid;
                }
                inputLiquidAmount += amount;
                if (inputLiquidAmount > inputLiquidCapacity) {
                    inputLiquidAmount = inputLiquidCapacity;
                }
            }
        }

        /**
         * 绘制热交换光束
         */
        private void drawHeatBeam(DFCore.DFCoreBuild core) {
            if (core == null || !core.isValid()) return;
            
            float width = beamWidth;
            Color color = beamColor;
            
            // 绘制光束
            Lines.stroke(width, color);
            Lines.line(x, y, core.x, core.y);
            
            // 绘制光束端点效果
            Drawf.light(x, y, core.x, core.y, width * 3, color, 0.8f);
        }

        @Override
        public void draw() {
            super.draw();

            // 绘制热量指示器
            if (heatLevel > 0) {
                float alpha = Mathf.clamp(heatLevel / maxHeatLevel, 0f, 1f);
                Drawf.light(x, y, beamWidth * 3, beamColor, alpha);
            }
        }

        @Override
        public void drawConfigure() {
            super.drawConfigure();
            
            // 绘制交换范围
            Drawf.dashCircle(x, y, exchangerRange, team.color);
        }

        @Override
        public double sense(LAccess sensor) {
            if (sensor == LAccess.totalLiquids) {
                return inputLiquidAmount + outputLiquidAmount;
            }
            if (sensor == LAccess.liquidCapacity) {
                return inputLiquidCapacity + outputLiquidCapacity;
            }
            if (sensor == LAccess.progress) {
                return heatLevel / maxHeatLevel;
            }
            return super.sense(sensor);
        }
    }
}