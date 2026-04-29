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
 * DFC能量接收器
 * 接收来自DFC能量发射器的能量并转换为电力
 */
public class DFCEnergyReceiver extends DFCBase {

    public float receiverRange = 60f;
    public float maxPower = 1000000f;
    public float powerGeneration = 5000f;
    public float coolantConsumption = 20f;
    public float beamWidth = 6f;
    public Color beamColor = Color.valueOf("ffcc00");
    public Color beamColorEnd = Color.valueOf("ff9900");

    public DFCEnergyReceiver(String name) {
        super(name);
        size = 3;
        hasLiquids = true;
        outputsLiquid = false;
        liquidCapacity = 64000f;
        allowResupply = true;
        sync = true;
        range = receiverRange;
        outputsPower = true;
        consumesPower = false;
    }

    @Override
    public void setStats() {
        super.setStats();
        stats.add(Stat.range, receiverRange, StatUnit.blocks);
        stats.add(Stat.liquidCapacity, liquidCapacity, StatUnit.liquidUnits);
        stats.add(Stat.powerCapacity, maxPower, StatUnit.powerUnits);
        stats.add(Stat.output, powerGeneration, StatUnit.powerUnits);
    }

    @Override
    public void setBars() {
        super.setBars();
        addBar("liquid", (DFCEnergyReceiverBuild entity) -> new Bar(() -> "bar.liquid", () -> entity.liquids.current() == null ? Pal.gray : entity.liquids.current().color, () -> entity.liquids.currentAmount() / liquidCapacity));
        addBar("power", (DFCEnergyReceiverBuild entity) -> new Bar(() -> "bar.power", () -> Pal.powerBar, () -> entity.power != null ? entity.power.status : 0f));
    }

    @Override
    public void drawPlace(int x, int y, int rotation, boolean valid) {
        super.drawPlace(x, y, rotation, valid);
        Drawf.dashCircle(x * tilesize + offset, y * tilesize + offset, receiverRange, Pal.placing);
    }

    public class DFCEnergyReceiverBuild extends DFCBlockBuild {
        public float energy = 0f;
        public float maxEnergy = 1000f;
        public int destructionLevel = 0;

        @Override
        public void update() {
            super.update();

            // 检查能量是否过高
            if (energy >= 100000000f) {
                destructionLevel = Math.min(destructionLevel + 2, 400);
                if (destructionLevel > 300 && Mathf.random() < 0.01f) {
                    explode();
                    return;
                }
            } else {
                destructionLevel = Math.max(destructionLevel - 1, 0);
            }

            // 将能量转换为电力
            if (power != null && energy > 0 && liquids.currentAmount() >= coolantConsumption) {
                liquids.remove(liquids.current(), coolantConsumption);
                float powerGenerated = energy * powerGeneration;
                power.status = Math.min(1f, power.status + powerGenerated / maxPower);
                energy = 0;
            }

            // 向电力网络输出电力
            if (power != null && power.status > 0) {
                float powerOutput = Math.min(power.status * maxPower, powerGeneration * Time.delta);
                // 这里可以根据需要实现具体的电力输出逻辑
            }
        }

        /**
         * 接收能量
         */
        public void addEnergy(float amount) {
            energy = Math.min(maxEnergy, energy + amount);
        }

        /**
         * 爆炸
         */
        private void explode() {
            // 创建爆炸效果
            Fx.reactorExplosion.at(x, y);
            // 移除建筑
            kill();
        }

        @Override
        public void draw() {
            super.draw();

            // 绘制能量指示器
            if (energy > 0) {
                float alpha = 0.8f;
                Drawf.light(x, y, beamWidth * 3, beamColor, alpha);
            }
        }

        @Override
        public void drawConfigure() {
            super.drawConfigure();
            
            // 绘制接收范围
            Drawf.dashCircle(x, y, receiverRange, team.color);
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

        @Override
        public float getPowerProduction() {
            return power != null && power.status > 0 ? powerGeneration : 0f;
        }
    }
}