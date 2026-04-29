package Proxima.block;

import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.entities.units.*;
import mindustry.game.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.logic.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.world.*;
import mindustry.world.blocks.heat.*;
import mindustry.world.consumers.*;
import mindustry.world.meta.*;

import java.util.*;

import static mindustry.Vars.*;

public class LargeHeatPipe extends Block{
    public float visualMaxHeat = 450f;
    public Color heatColor = Color.valueOf("90a2fc");

    public LargeHeatPipe(String name){
        super(name);
        size = 2;
        update = solid = true;
        rotate = false;
        underBullets = true;
        health = 200;
        armor = 4;
        requirements(Category.crafting, ItemStack.with(
            Items.copper, 50,
            Items.lead, 100,
            Items.graphite, 50
        ));
        researchCostMultiplier = 2f;
    }

    @Override
    public void setStats(){
        super.setStats();
        stats.add(Stat.heatCapacity, visualMaxHeat, StatUnit.heatUnits);
    }

    @Override
    public void setBars(){
        super.setBars();
        addBar("heat", (LargeHeatPipeBuild entity) -> new Bar(
            () -> Core.bundle.format("bar.heatamount", (int)entity.heat),
            () -> heatColor,
            () -> entity.heat / visualMaxHeat
        ));
    }

    @Override
    public void drawPlace(int x, int y, int rotation, boolean valid){
        super.drawPlace(x, y, rotation, valid);
        
        if(!valid) return;
        
        for(int i = 0; i < 4; i++){
            Building near = world.build(x * tilesize + Geometry.d4[i].x * size * tilesize / 2, y * tilesize + Geometry.d4[i].y * size * tilesize / 2);
            if(near != null && near.block instanceof HeatBlock){
                Drawf.square(near.x, near.y, near.block.size * tilesize / 2f, Pal.accent);
            }
        }
    }

    public class LargeHeatPipeBuild extends Building implements HeatBlock, HeatConsumer{
        public float heat = 0f;
        public float[] sideHeat = new float[4];
        public IntSet cameFrom = new IntSet();
        public long lastHeatUpdate = -1;

        @Override
        public void draw(){
            Draw.rect(region, x, y);
            
            if(heat > 0){
                Draw.z(Layer.blockAdditive);
                Draw.blend(Blending.additive);
                Draw.color(heatColor, heat / visualMaxHeat * 0.8f);
                Fill.square(x, y, size * tilesize / 2f);
                Draw.blend();
                Draw.color();
            }
        }

        @Override
        public void drawSelect(){
            super.drawSelect();
            
            for(int i = 0; i < 4; i++){
                Building near = nearby(i);
                if(near != null && near.block instanceof HeatBlock){
                    Drawf.square(near.x, near.y, near.block.size * tilesize / 2f, Pal.accent);
                }
            }
        }

        @Override
        public void updateTile(){
            updateHeat();
        }

        public void updateHeat(){
            if(lastHeatUpdate == state.updateId) return;

            lastHeatUpdate = state.updateId;
            heat = calculateHeatFourWay(sideHeat, cameFrom);
        }

        protected float calculateHeatFourWay(float[] sideHeat, @Nullable IntSet cameFrom){
            Arrays.fill(sideHeat, 0f);
            if(cameFrom != null) cameFrom.clear();

            float totalHeat = 0f;

            for(var build : proximity){
                if(build != null && build.team == team && build instanceof HeatBlock heater){
                    if(cameFrom != null && cameFrom.contains(build.id)) continue;

                    if(heater instanceof LargeHeatPipeBuild pipe && pipe.cameFrom.contains(id)){
                        continue;
                    }

                    float diff = (Math.min(Math.abs(build.x - x), Math.abs(build.y - y)) / tilesize);
                    int contactPoints = Math.min((int)(block.size/2f + build.block.size/2f - diff), Math.min(build.block.size, block.size));

                    float add = heater.heat() / build.block.size * contactPoints;

                    int dir = Mathf.mod(relativeTo(build), 4);
                    sideHeat[dir] += add;
                    totalHeat += add;

                    if(cameFrom != null){
                        cameFrom.add(build.id);
                        if(heater instanceof LargeHeatPipeBuild pipe){
                            cameFrom.addAll(pipe.cameFrom);
                        }
                    }

                    if(heater instanceof LargeHeatPipeBuild pipe){
                        pipe.updateHeat();
                    }
                }
            }
            return totalHeat;
        }

        @Override
        public float[] sideHeat(){
            return sideHeat;
        }

        @Override
        public float heatRequirement(){
            return visualMaxHeat;
        }

        @Override
        public float heat(){
            updateHeat();
            return heat;
        }

        @Override
        public float heatFrac(){
            return heat / visualMaxHeat;
        }

        @Override
        public void write(Writes write){
            super.write(write);
            write.f(heat);
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);
            heat = read.f();
        }
    }
}
