package Proxima.block;

import Proxima.liquids.ProximaLiquids;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.graphics.g2d.Lines.*;
import arc.math.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.world.*;
import mindustry.world.blocks.power.*;
import mindustry.world.draw.*;
import mindustry.world.meta.*;

import static mindustry.Vars.*;

/**
 * 大型蒸汽轮机
 * 基于普通蒸汽轮机，但效率更高，发电更多
 */
public class LargeSteamTurbine extends ConsumeGenerator {
    public DrawBlock drawer = new DrawMulti(
        new DrawRegion("-bottom"),
        new DrawRegion(),
        new DrawRegion("-blades") {{ spinSprite = true; rotateSpeed = 8f; }},
        new DrawRegion("-blades") {{ spinSprite = true; rotateSpeed = 8f; rotation = 30f; }},
        new DrawRegion("-blades") {{ spinSprite = true; rotateSpeed = 8f; rotation = 60f; }},
        new DrawRegion("-top")
    );
    
    public LargeSteamTurbine(String name) {
        super(name);
        size = 6; // 增大体积到6x6
        liquidCapacity = 1000; // 大幅增加液体容量
        powerProduction = 60f; // 8倍效率，7.5f * 8
        warmupSpeed = 0.015f; // 加快预热速度
        effectChance = 0.5f; // 效果触发几率
        generateEffectRange = 8f; // 效果范围
        squareSprite = false;
        
        // 使用现有的烟雾效果作为蒸汽效果
        generateEffect = Fx.smoke;
        
        // 8倍效率，消耗也相应增加
        consumeLiquid(ProximaLiquids.steam, 5f); // 0.625f * 8
        
        requirements(Category.power, ItemStack.with(
            Items.copper, 1200,
            Items.titanium, 800,
            Items.silicon, 500,
            Items.graphite, 400
        ));
    }
    
    @Override
    public void setStats() {
        super.setStats();
        stats.add(Stat.abilities, "Higher efficiency and power output");
        stats.add(Stat.abilities, "Larger size for better performance");
    }
    
    @Override
    public void setBars() {
        super.setBars();
        addBar("steam", (LargeSteamTurbineBuild entity) -> new Bar(
            () -> "Steam: " + (int)entity.liquids.get(ProximaLiquids.steam) + "/" + liquidCapacity,
            () -> Color.gray, // 使用灰色作为替代
            () -> entity.liquids.get(ProximaLiquids.steam) / liquidCapacity
        ));
    }
    
    @Override
    public void load() {
        super.load();
        drawer.load(this);
    }
    
    @Override
    public TextureRegion[] icons() {
        return drawer.icons(this);
    }
    
    public class LargeSteamTurbineBuild extends ConsumeGeneratorBuild {
        @Override
        public void draw() {
            drawer.draw(this);
        }
        
        @Override
        public void updateTile() {
            super.updateTile();
            
            // 增加额外的视觉效果，只在结构内有蒸汽时出现
            if(liquids.get(ProximaLiquids.steam) > 0 && Mathf.chance(effectChance * delta())) {
                // 绘制蒸汽效果
                Fx.smoke.at(x + Mathf.range(size * tilesize / 2f), y + Mathf.range(size * tilesize / 2f));
            }
        }
        
        @Override
        public void write(Writes write) {
            super.write(write);
        }
        
        @Override
        public void read(Reads read, byte revision) {
            super.read(read, revision);
        }
    }
}