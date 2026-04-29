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
 * DFC基础类
 * 作为所有DFC反应堆组件的基础类
 * 继承自 GenericCrafter 以获得工厂功能
 */
public abstract class DFCBase extends GenericCrafter {

    public @Nullable DrawBlock drawer;

    public boolean coreMerge = true;

    public Color emptyLightColor = Color.valueOf("f8c266");
    public Color fullLightColor = Color.valueOf("fb9567");

    public float range = 80f;
    public float placeOverlapMargin = 8 * 7f;
    public float rotateSpeed = 5;
    public float fogRadiusMultiplier = 1f;
    public boolean disableOverlapCheck = false;
    /** How much time to start shooting after placement. */
    public float activationTime = 0f;

    /** Effect displayed when coolant is used. */
    public Effect coolEffect = Fx.fuelburn;
    /** How much reload is lowered by for each unit of liquid of heat capacity. */
    public float coolantMultiplier = 5f;
    /** If not null, this consumer will be used for coolant. */
    public @Nullable ConsumeLiquidBase coolant;

    public DFCBase(String name){
        super(name);
        outputsPower = true;
        consumesPower = true;
        canOverdrive = false;
        envEnabled |= Env.space | Env.underwater | Env.any;
        destructible = true;
        //batteries don't need to update
        update = true;
        solid = true;
        hasLiquids = true;
        outputsLiquid = true;
        hasItems = true;
        sync = true;
        separateItemCapacity = true;
        allowResupply = true;
        outlineIcon = true;
        attacks = true;
        priority = TargetPriority.turret;
        group = BlockGroup.turrets;
        flags = EnumSet.of(BlockFlag.turret);
    }

    @Override
    public void init(){
        super.init();

        checkDrawDefault();
        if(coolant == null){
            coolant = findConsumer(c -> c instanceof ConsumeCoolant);
        }

        checkInitCoolant();

        if(!disableOverlapCheck){
            placeOverlapRange = Math.max(placeOverlapRange, range + placeOverlapMargin);
        }
        fogRadius = Math.max(Mathf.round(range / tilesize * fogRadiusMultiplier), fogRadius);
    }

    void checkDrawDefault(){
        if(drawer == null){
            // 不使用DrawPower，避免空指针异常
            drawer = new DrawMulti(new DrawDefault(), new DrawRegion("-top"));
        }
    }

    @Override
    public void load(){
        checkDrawDefault();

        super.load();
        drawer.load(this);
        
        liquidRegion = Core.atlas.find(name + "-liquid", Core.atlas.find(name));
        topRegion = Core.atlas.find(name + "-top", Core.atlas.find(name));
        bottomRegion = Core.atlas.find(name + "-bottom", Core.atlas.find(name));
    }

    @Override
    public void drawPlanRegion(BuildPlan plan, Eachable<BuildPlan> list){
        drawer.drawPlan(this, plan, list);
    }


    @Override
    public void getRegionsToOutline(Seq<TextureRegion> out){
        drawer.getRegionsToOutline(this, out);
    }
    public TextureRegion liquidRegion;
    public TextureRegion topRegion;
    public TextureRegion bottomRegion;

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
    @Override
    public boolean outputsItems(){
        return false;
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

    @Override
    public void reinitializeConsumers(){
        checkInitCoolant();

        super.reinitializeConsumers();
    }

    void checkInitCoolant(){
        if(coolant != null){
            coolant.update = false;
            coolant.booster = true;
            coolant.optional = true;

            //json parsing does not add to consumes
            if(!hasConsumer(coolant)) consume(coolant);
        }
    }

    @Override
    public void drawPlace(int x, int y, int rotation, boolean valid){
        super.drawPlace(x, y, rotation, valid);

        Drawf.dashCircle(x * tilesize + offset, y * tilesize + offset, range, Pal.placing);

        if(fogRadiusMultiplier < 0.99f && state.rules.fog){
            Drawf.dashCircle(x * tilesize + offset, y * tilesize + offset, range * fogRadiusMultiplier, Pal.lightishGray);
        }
    }

    @Override
    public void setStats(){
        super.setStats();

        stats.add(Stat.shootRange, range / tilesize, StatUnit.blocks);
        if(activationTime > 0) stats.add(Stat.activationTime, activationTime / 60f, StatUnit.seconds);
    }

    @Override
    public void setBars(){
        super.setBars();

        if(activationTime > 0){
            addBar("activationtimer", (DFCBlockBuild entity) ->
                    new Bar(() ->
                            (entity.activationTimer > 0)? Core.bundle.format("bar.activationtimer", Mathf.ceil(entity.activationTimer / 60f)) : Core.bundle.get("bar.activated"),
                            () -> (entity.activationTimer > 0)?  Pal.lightOrange : Pal.techBlue,
                            () -> 1 - entity.activationTimer / activationTime));
        }
    }


    public class DFCBlockBuild extends Building implements RotBlock{
        @Override
        public void draw(){
            drawer.draw(this);
        }

        @Override
        public void drawLight(){
            super.drawLight();
            drawer.drawLight(this);
        }

        @Override
        public float warmup(){
            return power != null ? power.status : 0f;
        }

        @Override
        public void overwrote(Seq<Building> previous){
            for(Building other : previous){
                if(other.power != null && other.block.consPower != null && other.block.consPower.buffered){
                    float amount = other.block.consPower.capacity * other.power.status;
                    if(power != null && consPower != null){
                        power.status = Mathf.clamp(power.status + amount / consPower.capacity);
                    }
                }
            }
            //only add prev items when core is not linked
            if(linkedCore == null){
                for(Building other : previous){
                    if(other.items != null && other.items != items && !(other instanceof DFCBlockBuild b && b.linkedCore != null)){
                        items.add(other.items);
                    }
                }

                items.each((i, a) -> items.set(i, Math.min(a, itemCapacity)));
            }
        }

        @Override
        public BlockStatus status(){
            if(Mathf.equal(power.status, 0f, 0.001f)) return BlockStatus.noInput;
            if(Mathf.equal(power.status, 1f, 0.001f)) return BlockStatus.active;
            return (activationTimer <= 0)? super.status() : BlockStatus.inactive;
        }
        public @Nullable Building linkedCore;

        @Override
        public boolean acceptItem(Building source, Item item){
            return linkedCore != null ? linkedCore.acceptItem(source, item) : items.get(item) < getMaximumAccepted(item);
        }

        @Override
        public boolean canUnload(){
            return linkedCore == null ? super.canUnload() : linkedCore.canUnload();
        }

        @Override
        public void handleItem(Building source, Item item){
            if(linkedCore != null){
                if(linkedCore.items.get(item) >= ((CoreBlock.CoreBuild)linkedCore).storageCapacity){
                    incinerateEffect(this, source);
                }
                ((CoreBlock.CoreBuild)linkedCore).noEffect = true;
                linkedCore.handleItem(source, item);
            }else{
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
            Drawf.dashCircle(x, y, range(), team.color);
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
        public boolean canPickup(){
            return linkedCore == null;
        }

        @Override
        public boolean allowDeposit(){
            return linkedCore != null || super.allowDeposit();
        }
        public float rotation = 90;
        public float activationTimer = 0;

        @Override
        public void placed(){
            super.placed();
            activationTimer = activationTime;
        }

        @Override
        public void update(){
            super.update();
            if(activationTimer > 0){
                activationTimer = Math.max(0, activationTimer - Time.delta);
            }
        }

        public float range(){
            return range;
        }

        @Override
        public float buildRotation(){
            return rotation;
        }


        public float estimateDps(){
            return 0f;
        }
    }
}
