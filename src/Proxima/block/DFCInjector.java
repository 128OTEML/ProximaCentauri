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
import arc.util.io.*;
import mindustry.entities.units.*;
import mindustry.gen.*;
import mindustry.world.blocks.storage.CoreBlock;
import mindustry.world.consumers.ConsumeCoolant;
import mindustry.world.consumers.ConsumeLiquidBase;
import mindustry.world.draw.*;
import mindustry.world.meta.*;
import mindustry.world.blocks.*;
import mindustry.world.blocks.RotBlock;
import Proxima.liquids.ProximaLiquids;

import static mindustry.Vars.*;

/**
 * DFC燃料注入器
 * 为DFC核心提供液体燃料
 * 拥有两个独立的液体容器，分别用于储存反物质和正物质
 */
public class DFCInjector extends DFCLaserEmitter {

    public float injectorRange = 60f;
    public float liquidTransferRate = 10f;
    public float beamWidth = 4f;
    public Color beamColor = Color.valueOf("47c1ff");
    public Color beamColorEnd = Color.valueOf("0080ff");
    
    // 两个独立的液体容量
    public float antimatterCapacity = 1280000f;
    public float positiveMatterCapacity = 1280000f;

    public DFCInjector(String name) {
        super(name);
        size = 3;
        hasLiquids = true;
        outputsLiquid = false;
        liquidCapacity = antimatterCapacity + positiveMatterCapacity;
        allowResupply = true;
        sync = true;
        range = injectorRange;
    }

    @Override
    public void setStats() {
        super.setStats();
        stats.add(Stat.range, injectorRange, StatUnit.blocks);
        stats.add(Stat.liquidCapacity, antimatterCapacity, StatUnit.liquidUnits);
        stats.add(Stat.liquidCapacity, positiveMatterCapacity, StatUnit.liquidUnits);
        stats.add(Stat.output, liquidTransferRate, StatUnit.liquidUnits);
    }

    @Override
    public void setBars() {
        super.setBars();
        addBar("antimatter", (DFCInjectorBuild entity) -> new Bar(() -> "Antimatter", () -> ProximaLiquids.dfcAntimatter.color, () -> entity.antimatterAmount / antimatterCapacity));
        addBar("positiveMatter", (DFCInjectorBuild entity) -> new Bar(() -> "Positive Matter", () -> ProximaLiquids.dfcPositiveMatter.color, () -> entity.positiveMatterAmount / positiveMatterCapacity));
    }

    @Override
    public void drawPlace(int x, int y, int rotation, boolean valid) {
        super.drawPlace(x, y, rotation, valid);
        Drawf.dashCircle(x * tilesize + offset, y * tilesize + offset, injectorRange, Pal.placing);
    }

    public class DFCInjectorBuild extends DFCLaserEmitterBuild {
        // 两个独立的液体储存
        public float antimatterAmount = 0f;
        public float positiveMatterAmount = 0f;

        @Override
        public void update() {
            super.update();

            // 尝试找到DFC核心并传输液体
            if (dfcCoreTarget != null && dfcCoreTarget instanceof DFCore.DFCoreBuild && dfcCoreTarget.isValid()) {
                transferLiquids((DFCore.DFCoreBuild) dfcCoreTarget);
            }
        }

        /**
         * 向DFC核心传输液体
         */
        private void transferLiquids(DFCore.DFCoreBuild core) {
            // 传输反物质
            if (antimatterAmount > 0) {
                float amount = Math.min(liquidTransferRate * Time.delta, antimatterAmount);
                if (core.liquidAmount1 < ((DFCore)core.block).liquidCapacity1) {
                    core.liquid1 = ProximaLiquids.dfcAntimatter;
                    core.liquidAmount1 += amount;
                    if (core.liquidAmount1 > ((DFCore)core.block).liquidCapacity1) {
                        core.liquidAmount1 = ((DFCore)core.block).liquidCapacity1;
                    }
                    antimatterAmount -= amount;
                }
            }
            
            // 传输正物质
            if (positiveMatterAmount > 0) {
                float amount = Math.min(liquidTransferRate * Time.delta, positiveMatterAmount);
                if (core.liquidAmount2 < ((DFCore)core.block).liquidCapacity2) {
                    core.liquid2 = ProximaLiquids.dfcPositiveMatter;
                    core.liquidAmount2 += amount;
                    if (core.liquidAmount2 > ((DFCore)core.block).liquidCapacity2) {
                        core.liquidAmount2 = ((DFCore)core.block).liquidCapacity2;
                    }
                    positiveMatterAmount -= amount;
                }
            }
        }

        @Override
        public boolean acceptLiquid(Building source, Liquid liquid) {
            // 白名单：只接受反物质和正物质
            if (liquid == ProximaLiquids.dfcAntimatter) {
                return antimatterAmount < antimatterCapacity;
            } else if (liquid == ProximaLiquids.dfcPositiveMatter) {
                return positiveMatterAmount < positiveMatterCapacity;
            }
            return false;
        }

        @Override
        public void handleLiquid(Building source, Liquid liquid, float amount) {
            // 根据液体类型添加到对应的容器
            if (liquid == ProximaLiquids.dfcAntimatter) {
                antimatterAmount += amount;
                if (antimatterAmount > antimatterCapacity) {
                    antimatterAmount = antimatterCapacity;
                }
            } else if (liquid == ProximaLiquids.dfcPositiveMatter) {
                positiveMatterAmount += amount;
                if (positiveMatterAmount > positiveMatterCapacity) {
                    positiveMatterAmount = positiveMatterCapacity;
                }
            }
        }

        @Override
        public void draw() {
            super.draw();

            // 绘制连接光束
            if (dfcCoreTarget != null && dfcCoreTarget instanceof DFCore.DFCoreBuild && dfcCoreTarget.isValid()) {
                float alpha = 0.6f;
                
                // 绘制反物质光束
                if (antimatterAmount > 0) {
                    Lines.stroke(beamWidth, ProximaLiquids.dfcAntimatter.color);
                    Lines.line(x, y, dfcCoreTarget.x, dfcCoreTarget.y);
                    Drawf.light(x, y, dfcCoreTarget.x, dfcCoreTarget.y, beamWidth * 2, ProximaLiquids.dfcAntimatter.color, alpha);
                }
                
                // 绘制正物质光束
                if (positiveMatterAmount > 0) {
                    Lines.stroke(beamWidth, ProximaLiquids.dfcPositiveMatter.color);
                    Lines.line(x, y, dfcCoreTarget.x, dfcCoreTarget.y);
                    Drawf.light(x, y, dfcCoreTarget.x, dfcCoreTarget.y, beamWidth * 2, ProximaLiquids.dfcPositiveMatter.color, alpha);
                }
            }
        }

        @Override
        public void drawConfigure() {
            super.drawConfigure();
            
            // 绘制注入范围
            Drawf.dashCircle(x, y, injectorRange, team.color);
        }

        @Override
        public double sense(LAccess sensor) {
            if (sensor == LAccess.totalLiquids) {
                return antimatterAmount + positiveMatterAmount;
            }
            if (sensor == LAccess.liquidCapacity) {
                return antimatterCapacity + positiveMatterCapacity;
            }
            return super.sense(sensor);
        }
        
        @Override
        public void write(Writes write) {
            super.write(write);
            write.f(antimatterAmount);
            write.f(positiveMatterAmount);
        }
        
        @Override
        public void read(Reads read, byte revision) {
            super.read(read, revision);
            antimatterAmount = read.f();
            positiveMatterAmount = read.f();
        }
    }
}