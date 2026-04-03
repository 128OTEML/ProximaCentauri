package IcePeak.block;

import IcePeak.liquids.IcePeakLiquids;
import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.struct.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.world.*;
import mindustry.world.blocks.*;
import mindustry.world.blocks.power.*;
import mindustry.world.meta.*;
import arc.struct.*;
import arc.util.*;
import mindustry.content.*;
import mindustry.gen.*;
import mindustry.logic.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.storage.CoreBlock.*;
import mindustry.world.meta.*;
import mindustry.world.blocks.liquid.*;

import static mindustry.Vars.*;
import mindustry.world.blocks.storage.CoreBlock.CoreBuild;
import mindustry.logic.LAccess;
import mindustry.world.Edges;

/**
 * RBMK反应堆基础方块
 * 参考HBM's Nuclear Tech中的RBMK结构
 */
public class RBMKBase extends PowerGenerator{
    public TextureRegion liquidRegion;
    public TextureRegion topRegion;
    public TextureRegion bottomRegion;
    
    public RBMKBase(String name){
        super(name);
        size = 2;
        update = true;
        itemCapacity = 1; // 物品容量
        hasItems = true;
        hasLiquids = true; // 启用液体存储
        liquidCapacity = 100f; // 液体容量
        solid = true;
        sync = true;
        destructible = true;
        separateItemCapacity = true;
        group = BlockGroup.transportation;
        flags = EnumSet.of(BlockFlag.storage);
        allowResupply = true;
        envEnabled = Env.any;
        outputsLiquid = true; // 允许输出液体
    }

    @Override
    public void setBars(){
        super.setBars();
        // 添加物品容量进度条
        addBar("capacity", (RBMKBaseBuild entity) -> new Bar(
            () -> "Capacity: " + (entity.items == null ? 0 : entity.items.total()) + "/" + itemCapacity,
            () -> Pal.items,
            () -> entity.items == null ? 0f : (float)entity.items.total() / itemCapacity
        ));
        // 添加热量进度条
        addBar("heat", (RBMKBaseBuild entity) -> new Bar(
            () -> "Heat: " + (int)entity.heat + "°C",
            () -> Pal.lightOrange,
            () -> entity.heat / entity.maxHeat
        ));
        // 添加中子通量进度条
        addBar("neutronFlux", (RBMKBaseBuild entity) -> new Bar(
            () -> "Neutron Flux: " + Strings.fixed(entity.neutronFlux, 2),
            () -> Pal.accent,
            () -> Mathf.clamp(entity.neutronFlux / entity.maxNeutronFlux, 0f, 1f)
        ));
        // 添加慢中子通量进度条
        addBar("neutronFluxSlow", (RBMKBaseBuild entity) -> new Bar(
            () -> "Slow Neutron Flux: " + Strings.fixed(entity.neutronFluxSlow, 2),
            () -> Pal.lightishOrange,
            () -> Mathf.clamp(entity.neutronFluxSlow / entity.maxNeutronFlux, 0f, 1f)
        ));
        // 添加快中子通量进度条
        addBar("neutronFluxFast", (RBMKBaseBuild entity) -> new Bar(
            () -> "Fast Neutron Flux: " + Strings.fixed(entity.neutronFluxFast, 2),
            () -> Pal.lightOrange,
            () -> Mathf.clamp(entity.neutronFluxFast / entity.maxNeutronFlux, 0f, 1f)
        ));
        // 添加液体进度条
        addBar("liquid", (RBMKBaseBuild entity) -> new Bar(
            () -> entity.liquids != null && entity.liquids.current() != null ? 
                entity.liquids.current().localizedName + " " + Strings.fixed(entity.liquids.currentAmount(), 1) + "/" + liquidCapacity : 
                "No Liquid",
            () -> entity.liquids != null && entity.liquids.current() != null ? entity.liquids.current().barColor() : Pal.gray,
            () -> entity.liquids != null ? entity.liquids.currentAmount() / liquidCapacity : 0f
        ));
    }

    @Override
    public void load(){
        super.load();
        liquidRegion = Core.atlas.find(name + "-liquid");
        topRegion = Core.atlas.find(name + "-top");
        bottomRegion = Core.atlas.find(name + "-bottom");
    }

    @Override
    public TextureRegion[] icons(){
        return new TextureRegion[]{bottomRegion, topRegion};
    }

    public static void drawTiledFrames(int size, float x, float y, float padding, Liquid liquid, float alpha){
        drawTiledFrames(size, x, y, padding, padding, padding, padding, liquid, alpha);
    }

    public static void drawTiledFrames(int size, float x, float y, float padLeft, float padRight, float padTop, float padBottom, Liquid liquid, float alpha){
        TextureRegion region = renderer.fluidFrames[liquid.gas ? 1 : 0][liquid.getAnimationFrame()];
        TextureRegion toDraw = Tmp.tr1;

        float leftBounds = size/2f * tilesize - padRight;
        float bottomBounds = size/2f * tilesize - padTop;
        Color color = Tmp.c1.set(liquid.color).a(1f);

        for(int sx = 0; sx < size; sx++){
            for(int sy = 0; sy < size; sy++){
                float relx = sx - (size-1)/2f, rely = sy - (size-1)/2f;

                toDraw.set(region);

                //truncate region if at border
                float rightBorder = relx*tilesize + padLeft, topBorder = rely*tilesize + padBottom;
                float squishX = rightBorder + tilesize/2f - leftBounds, squishY = topBorder + tilesize/2f - bottomBounds;
                float ox = 0f, oy = 0f;

                if(squishX >= 8 || squishY >= 8) continue;

                //cut out the parts that don't fit inside the padding
                if(squishX > 0){
                    toDraw.setWidth(toDraw.width - squishX * 4f);
                    ox = -squishX/2f;
                }

                if(squishY > 0){
                    toDraw.setY(toDraw.getY() + squishY * 4f);
                    oy = -squishY/2f;
                }

                Drawf.liquid(toDraw, x + rightBorder + ox, y + topBorder + oy, alpha, color);
            }
        }
    }

    public static void incinerateEffect(Building self, Building source){
        if(Mathf.chance(0.3)){
            Tile edge = Edges.getFacingEdge(source, self);
            Tile edge2 = Edges.getFacingEdge(self, source);
            if(edge != null && edge2 != null && self.wasVisible){
                Fx.coreBurn.at((edge.worldx() + edge2.worldx())/2f, (edge.worldy() + edge2.worldy())/2f);
            }
        }
    }
    /**
     * RBMK建筑基础类
     */

    public class RBMKBaseBuild extends PowerGenerator.GeneratorBuild{
        public @Nullable Building linkedCore;
        public float heat = 25f;
        public float maxHeat = 1000f;
        public boolean hasLid = false;
        public int lidType = 0; // 0: none, 1: standard, 2: glass
        public float neutronFlux = 0f; // 中子通量
        public float neutronFluxSlow = 0f; // 慢中子通量
        public float neutronFluxFast = 0f; // 快中子通量
        public float maxNeutronFlux = 10000f; // 中子通量最大值
        public float coolingEfficiency = 0f; // 冷却效率
        public float heatConductivity = 0.2f; // 热传导率

        @Override
        public boolean acceptItem(Building source, Item item){
            return linkedCore != null ? linkedCore.acceptItem(source, item) : (items != null && items.get(item) < getMaximumAccepted(item));
        }

        @Override
        public boolean canUnload(){
            return linkedCore == null ? super.canUnload() : linkedCore.canUnload();
        }

        @Override
        public void handleItem(Building source, Item item){
            if(linkedCore != null){
                if(linkedCore.items != null && linkedCore.items.get(item) >= ((CoreBuild)linkedCore).storageCapacity){
                    incinerateEffect(this, source);
                }
                ((CoreBuild)linkedCore).noEffect = true;
                linkedCore.handleItem(source, item);
            }else if(items != null){
                super.handleItem(source, item);
            }
        }

        @Override
        public void itemTaken(Item item){
            if(linkedCore != null){
                linkedCore.itemTaken(item);
            }
        }

        @Override
        public int removeStack(Item item, int amount){
            if(items == null) return 0;
            
            int result = super.removeStack(item, amount);

            if(linkedCore != null && team == state.rules.defaultTeam && state.isCampaign()){
                state.rules.sector.info.handleCoreItem(item, -result);
            }

            return result;
        }

        @Override
        public int getMaximumAccepted(Item item){
            return linkedCore != null ? linkedCore.getMaximumAccepted(item) : itemCapacity;
        }

        @Override
        public int explosionItemCap(){
            //when linked to a core, containers/vaults are made significantly less explosive.
            return linkedCore != null ? Math.min(itemCapacity/60, 6) : itemCapacity;
        }

        @Override
        public void drawSelect(){
            if(linkedCore != null){
                linkedCore.drawSelect();
            }
        }

        @Override
        public double sense(LAccess sensor){
            if(sensor == LAccess.itemCapacity && linkedCore != null) return linkedCore.sense(sensor);
            return super.sense(sensor);
        }

        @Override
        public void overwrote(Seq<Building> previous){
            //only add prev items when core is not linked
            if(linkedCore == null){
                for(Building other : previous){
                    if(other.items != null && other.items != items && !(other instanceof RBMKBaseBuild b && b.linkedCore != null)){
                        items.add(other.items);
                    }
                }

                items.each((i, a) -> items.set(i, Math.min(a, itemCapacity)));
            }
        }

        @Override
        public boolean canPickup(){
            return linkedCore == null;
        }

        @Override
        public boolean allowDeposit(){
            return linkedCore != null || super.allowDeposit();
        }

        @Override
        public void updateTile(){
            // 储存流体
            dumpLiquid(liquids.current());
            // 热量传导
            conductHeat();
            // 中子通量传导
            conductNeutronFlux();
            // 基础热量散失
            heat = Mathf.clamp(heat - 0.64f * delta(), 25f, maxHeat);
            // 限制中子通量最大值
            neutronFlux = Mathf.clamp(neutronFlux, 0f, maxNeutronFlux);
            neutronFluxSlow = Mathf.clamp(neutronFluxSlow, 0f, maxNeutronFlux);
            neutronFluxFast = Mathf.clamp(neutronFluxFast, 0f, maxNeutronFlux);
        }
        
        /**
         * 热量传导
         * 向相邻的RBMK方块传导热量
         */
        public void conductHeat(){
            for(Building other : proximity){
                if(other instanceof RBMKBaseBuild){
                    RBMKBaseBuild rbmk = (RBMKBaseBuild) other;
                    // 计算热量差
                    float heatDiff = heat - rbmk.heat;
                    // 传导热量，考虑热传导率
                    float heatTransfer = heatDiff * heatConductivity * delta();
                    // 限制传导量，避免震荡
                    heatTransfer = Mathf.clamp(heatTransfer, -Math.abs(heatDiff) * 0.5f, Math.abs(heatDiff) * 0.5f);
                    // 传导热量
                    heat -= heatTransfer;
                    rbmk.heat += heatTransfer;
                }
            }
        }
        
        /**
         * 中子通量传导
         * 向相邻的RBMK方块传导中子通量
         */
        public void conductNeutronFlux(){
            for(Building other : proximity){
                if(other instanceof RBMKBaseBuild){
                    RBMKBaseBuild rbmk = (RBMKBaseBuild) other;
                    // 传导中子通量
                    float fluxTransfer = neutronFlux * 0.1f * delta();
                    neutronFlux -= fluxTransfer;
                    rbmk.neutronFlux += fluxTransfer;
                    
                    // 传导慢中子通量
                    float slowFluxTransfer = neutronFluxSlow * 0.1f * delta();
                    neutronFluxSlow -= slowFluxTransfer;
                    rbmk.neutronFluxSlow += slowFluxTransfer;
                    
                    // 传导快中子通量
                    float fastFluxTransfer = neutronFluxFast * 0.1f * delta();
                    neutronFluxFast -= fastFluxTransfer;
                    rbmk.neutronFluxFast += fastFluxTransfer;
                }
            }
        }

        @Override
        public void draw(){
            float rotation = rotate ? rotdeg() : 0;
            Draw.rect(bottomRegion, x, y, rotation);

            if(liquids != null && liquids.currentAmount() > 0.001f){
                drawTiledFrames(size, x, y, 2f, liquids.current(), liquids.currentAmount() / liquidCapacity);
            }

            Draw.rect(topRegion, x, y, rotation);
            
            // 根据热量显示颜色
            if(heat > 100f){
                Draw.z(Layer.blockAdditive);
                Draw.blend(Blending.additive);
                float intensity = Mathf.clamp((heat - 100f) / (maxHeat - 100f), 0f, 1f);
                Draw.color(Pal.lightOrange, intensity * 0.5f);
                Fill.square(x, y, size * tilesize / 2f - 2f);
                Draw.blend();
                Draw.color();
            }
        }

        @Override
        public void write(Writes write){
            super.write(write);
            write.f(heat);
            write.f(maxHeat);
            write.bool(hasLid);
            write.i(lidType);
            write.f(neutronFlux);
            write.f(neutronFluxSlow);
            write.f(neutronFluxFast);
            write.f(maxNeutronFlux);
            write.f(coolingEfficiency);
            write.f(heatConductivity);
        }
        
        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);
            heat = read.f();
            maxHeat = read.f();
            hasLid = read.bool();
            lidType = read.i();
            neutronFlux = read.f();
            neutronFluxSlow = read.f();
            neutronFluxFast = read.f();
            maxNeutronFlux = read.f();
            coolingEfficiency = read.f();
            heatConductivity = read.f();
        }

        @Override
        public boolean acceptLiquid(Building source, Liquid liquid){
            return (liquids.current() == liquid || liquids.currentAmount() < 0.2f);
        }
    }

    @Override
    public void setStats() {
        super.setStats();
        stats.add(Stat.itemCapacity, itemCapacity);
        stats.add(Stat.liquidCapacity, liquidCapacity);
    }
}