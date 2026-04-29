/**
 * DFC创意发射器
 * 用于无限提供液体和能量到DFC核心
 * 继承自DFCLaserEmitter以获得激光发射功能
 */
package Proxima.block;

import arc.graphics.Color;
import arc.math.Mathf;
import mindustry.content.Fx;
import mindustry.content.Liquids;
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

public class DFCCreativeEmitter extends DFCLaserEmitter {

    public float emitterRange = 80f;
    public float liquidProduction = 1000f;
    public float energyProduction = 10000f;
    public float beamWidth = 6f;
    public Color beamColor = Color.valueOf("ffffff");
    public Color beamColorEnd = Color.valueOf("ffff00");

    public DFCCreativeEmitter(String name) {
        super(name);
        size = 3;
        hasLiquids = true;
        outputsLiquid = true;
        liquidCapacity = 10000f;
        allowResupply = true;
        sync = true;
        range = emitterRange;
        consumesPower = false; // 创意模式不需要电力
    }

    @Override
    public void setStats() {
        super.setStats();
        stats.add(Stat.range, emitterRange, StatUnit.blocks);
        stats.add(Stat.liquidCapacity, liquidCapacity, StatUnit.liquidUnits);
        stats.add(Stat.output, liquidProduction, StatUnit.liquidUnits);
        stats.add(Stat.output, energyProduction, StatUnit.powerUnits);
    }

    @Override
    public void setBars() {
        super.setBars();
        addBar("liquid", (DFCCreativeEmitterBuild entity) -> new Bar(() -> "bar.liquid", () -> entity.liquids.current() == null ? Pal.gray : entity.liquids.current().color, () -> entity.liquids.currentAmount() / liquidCapacity));
    }

    @Override
    public void drawPlace(int x, int y, int rotation, boolean valid) {
        super.drawPlace(x, y, rotation, valid);
        Drawf.dashCircle(x * tilesize + offset, y * tilesize + offset, emitterRange, Pal.placing);
    }

    public class DFCCreativeEmitterBuild extends DFCLaserEmitterBuild {
        public float productionTimer = 0f;
        public float productionInterval = 1f;

        @Override
        public void update() {
            super.update();

            // 创意模式：无限生成液体
            if (liquids.currentAmount() < liquidCapacity) {
                if (liquids.current() == null) {
                    // 默认使用水
                    liquids.add(Liquids.water, liquidProduction * Time.delta);
                } else {
                    liquids.add(liquids.current(), liquidProduction * Time.delta);
                }
            }

            // 使用从DFCLaserEmitter继承来的dfcCoreTarget进行能量和液体传输
            if (dfcCoreTarget != null && dfcCoreTarget instanceof DFCore.DFCoreBuild) {
                DFCore.DFCoreBuild core = (DFCore.DFCoreBuild) dfcCoreTarget;
                
                productionTimer += Time.delta;
                if (productionTimer >= productionInterval) {
                    productionTimer = 0f;
                    transferResources(core);
                }
            }
        }

        /**
         * 向DFC核心传输资源
         */
        private void transferResources(DFCore.DFCoreBuild core) {
            // 传输液体
            if (liquids.current() != null && liquids.currentAmount() > 0) {
                float transferAmount = Math.min(liquids.currentAmount(), liquidProduction * Time.delta);
                if (core.liquidAmount1 < ((DFCore)core.block).liquidCapacity1) {
                    core.liquid1 = liquids.current();
                    core.liquidAmount1 += transferAmount;
                    if (core.liquidAmount1 > ((DFCore)core.block).liquidCapacity1) {
                        core.liquidAmount1 = ((DFCore)core.block).liquidCapacity1;
                    }
                } else if (core.liquidAmount2 < ((DFCore)core.block).liquidCapacity2) {
                    core.liquid2 = liquids.current();
                    core.liquidAmount2 += transferAmount;
                    if (core.liquidAmount2 > ((DFCore)core.block).liquidCapacity2) {
                        core.liquidAmount2 = ((DFCore)core.block).liquidCapacity2;
                    }
                }
            }

            // 绘制资源传输光束
            drawResourceBeam(core);
        }

        /**
         * 绘制资源传输光束
         */
        private void drawResourceBeam(DFCore.DFCoreBuild core) {
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

            // 绘制创意发射器的特效
            if (dfcCoreTarget != null && dfcCoreTarget.isValid()) {
                float alpha = Mathf.absin(Time.time, 2f, 0.5f) + 0.5f;
                Drawf.light(x, y, beamWidth * 4, beamColor, alpha);
            }
        }

        @Override
        public void drawConfigure() {
            super.drawConfigure();
            
            // 绘制发射范围
            Drawf.dashCircle(x, y, emitterRange, team.color);
        }

        @Override
        public boolean acceptLiquid(Building source, Liquid liquid) {
            // 创意发射器可以接受任何液体
            return true;
        }

        @Override
        public void handleLiquid(Building source, Liquid liquid, float amount) {
            // 处理输入液体
            liquids.add(liquid, amount);
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
                return 1f; // 创意模式始终满进度
            }
            return super.sense(sensor);
        }
    }
}