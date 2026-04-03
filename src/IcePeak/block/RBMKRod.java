package IcePeak.block;

import IcePeak.items.*;
import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
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
import mindustry.world.draw.*;
import mindustry.world.meta.*;
import mindustry.entities.units.BuildPlan;

import static mindustry.Vars.*;

/**
 * RBMK燃料棒
 * 产生热量和电力，需要冷却
 * 燃料数据直接存储在燃料棒物品中
 * 参考HBM's Nuclear Tech mod的实现
 */
public class RBMKRod extends RBMKBase {
    public boolean moderated = false;
    public float powerProduction = 50f;
    public float fuelConsumption = 0.001f;
    public DrawBlock drawer = new DrawDefault();
    public boolean useBlockDrawer = true;
    
    public RBMKRod(String name){
        super(name);
        hasPower = true;
        itemCapacity = 1; // 物品容量
        powerProduction = 50f;
        
        requirements(Category.power, ItemStack.with(
            Items.copper, 200,
            Items.lead, 150,
            Items.titanium, 100,
            Items.thorium, 25
        ));
    }
    
    @Override
    public void init() {
        super.init();
        // 初始化燃料数据
        RBMKFuelData.initDefaultFuels();
    }

    // 设置属性面板
    @Override
    public void setStats(){
        super.setStats();
        stats.add(Stat.input, table -> {
            table.row();
            table.add("[accent]Accepted Fuels:[]").row();
            table.add("Any item with fuel properties").row();
        });
    }
    
    @Override
    public void setBars(){
        super.setBars();
        addBar("heat", (RBMKRodBuild entity) -> new Bar(
            () -> "Heat: " + (int)entity.heat + "°C",
            () -> Pal.lightOrange,
            () -> entity.heat / entity.maxHeat
        ));
        addBar("fuel", (RBMKRodBuild entity) -> {
            Item currentFuel = entity.getCurrentFuel();
            if(currentFuel == null) {
                return new Bar(
                    () -> "Fuel: None",
                    () -> Pal.gray,
                    () -> 0f
                );
            }
            return new Bar(
                () -> "Fuel: " + currentFuel.localizedName + " (" + entity.items.get(currentFuel) + ")",
                () -> Pal.ammo,
                () -> 1f
            );
        });
        addBar("neutronFlux", (RBMKRodBuild entity) -> new Bar(
            () -> "Neutron Flux: " + Strings.fixed(entity.neutronFlux, 2),
            () -> Pal.accent,
            () -> Mathf.clamp(entity.neutronFlux / 100f, 0f, 1f)
        ));
        
        // 添加物品容量进度条
        addBar("capacity", (RBMKRodBuild entity) -> new Bar(
            () -> "Capacity: " + entity.items.total() + "/" + itemCapacity,
            () -> Pal.items,
            () -> (float)entity.items.total() / itemCapacity
        ));
        
    }
    
    @Override
    public void load() {
        super.load();
        if(useBlockDrawer) drawer.load(this);
    }
    

    
    @Override
    public TextureRegion[] icons() {
        return useBlockDrawer ? drawer.icons(this) : super.icons();
    }
    
    public class RBMKRodBuild extends RBMKBaseBuild {
        public float rodLevel = 0.5f;
        public boolean explodeOnBroken = true;
        public boolean hasRod = false;
        public int rodColor = 0;
        public float enrichment = 1f;
        public float xenonPoison = 0f;
        public float coreHeat = 25f;
        public float productionEfficiency = 0f; // 生产效率
        public float previousNeutronFlux = 0f; // 前一帧的中子通量
        public float controlRodLimit = 1f; // 控制棒限制因子，1=无限制，0=完全限制
        
        // 当前使用的燃料
        public Item currentFuel = null;
        
        /**
         * 获取当前燃料
         * @return 当前燃料物品，如果没有则返回null
         */
        public Item getCurrentFuel(){
            if(items == null) return null;
            // 遍历所有物品，找到第一个燃料
            for(Item item : content.items()){
                if(items.get(item) > 0 && RBMKFuelData.isFuel(item)){
                    return item;
                }
            }
            return null;
        }
        
        /**
         * 检查是否有燃料
         * @return 是否有燃料
         */
        public boolean hasFuel(){
            return getCurrentFuel() != null;
        }
        
        /**
         * 获取当前燃料的属性
         * @return 燃料属性，如果没有燃料则返回默认属性
         */
        public RBMKFuelData.FuelProperties getFuelProperties(){
            Item fuel = getCurrentFuel();
            if(fuel != null){
                RBMKFuelData.FuelProperties props = RBMKFuelData.getFuelProperties(fuel);
                if(props != null) return props;
            }
            // 返回默认属性
            return new RBMKFuelData.FuelProperties();
        }
        
        @Override
        public void updateTile(){
            super.updateTile();
            
            if(items == null) return;
            
            // 扫描周围3X3范围内的控制棒，获取控制棒的滑轨数值
            float controlRodValue = 1f; // 默认值为1（无限制）
            int controlRodCount = 0;
            
            for(int dx = -1; dx <= 1; dx++){
                for(int dy = -1; dy <= 1; dy++){
                    if(dx == 0 && dy == 0) continue; // 跳过自身
                    
                    Building build = world.build((int)(x + dx), (int)(y + dy));
                    if(build != null && build.block instanceof RBMKControl){
                        RBMKControl.RBMKControlBuild control = (RBMKControl.RBMKControlBuild)build;
                        controlRodValue += control.controlValue;
                        controlRodCount++;
                    }
                }
            }
            
            // 计算平均控制棒值，如果有多个控制棒
            if(controlRodCount > 0){
                controlRodValue = controlRodValue / (controlRodCount + 1); // +1是为了包含默认值
            }
            controlRodLimit = Mathf.clamp(controlRodValue, 0f, 1f);
            
            // 保存当前中子通量作为前一帧的值
            previousNeutronFlux = neutronFlux;
            
            // 更新当前燃料
            currentFuel = getCurrentFuel();
            
            // 检查是否有燃料
            if(currentFuel != null){
                RBMKFuelData.FuelProperties props = getFuelProperties();
                
                // 计算中子通量（使用RBMKBase的变量）
                float fastFlux = neutronFluxFast;
                float slowFlux = neutronFluxSlow;
                
                // 产生热量 - 使用燃料数据
            float heatIncrement = 0f;
            if(props.isNeutronSource){
                // 中子源燃料自身发热
                heatIncrement = props.heat * rodLevel * delta() * 1.5f;
            } else if(neutronFlux > 0.1f){
                // 非中子源燃料消耗中子来发热，提高2倍换热速度
                float neutronConsumption = Math.min(neutronFlux, 0.5f * delta());
                heatIncrement = props.heat * rodLevel * delta() * (neutronConsumption / (0.5f * delta())) * 2f;
                
                // 消耗中子
                neutronFlux -= neutronConsumption;
                neutronFluxFast -= neutronConsumption * 0.6f;
                neutronFluxSlow -= neutronConsumption * 0.4f;
            }
                
                // 乘以控制棒的滑轨数值
                heatIncrement *= controlRodLimit;
                
                heat = Mathf.clamp(heat + heatIncrement, 25f, maxHeat);
                coreHeat = Mathf.clamp(coreHeat + heatIncrement * 1.2f, 25f, props.meltingPoint);
                
                // 产生电力 - 使用燃料数据
                float efficiency = 0f;
                if(heatIncrement > 0f){
                    efficiency = Mathf.clamp(0.5f + (heat - 25f) * 0.0005f, 0f, 1f);
                    // 考虑氙毒的影响
                    efficiency *= (1 - xenonPoison * 0.01f);
                    
                    // 中子通量线性增益：中子越多效率越高，8000时达到3.5倍
                    float neutronBonus = 1f + Mathf.clamp(neutronFlux / 8000f, 0f, 1f) * 2.5f;
                    efficiency *= neutronBonus;
                }
                productionEfficiency = efficiency * props.enrichment;
                
                // 产生中子通量 - 只有中子源燃料才产生中子
                if(props.isNeutronSource){
                    // 将中子产生速度提高8倍，再提高2.5倍
                    float neutronProduction = props.enrichment * 80f * 2.5f * delta();
                    float fastProduction = props.enrichment * 64f * 2.5f * delta();
                    float slowProduction = props.enrichment * 16f * 2.5f * delta();
                    
                    // 乘以控制棒的滑轨数值
                    neutronProduction *= controlRodLimit;
                    fastProduction *= controlRodLimit;
                    slowProduction *= controlRodLimit;
                    
                    neutronFlux += neutronProduction;
                    neutronFluxFast += fastProduction;
                    neutronFluxSlow += slowProduction;
                }
                else {
                    // 将非中子源的产中子速度设为0
                    float neutronProduction = 0f * delta();
                    neutronFlux += neutronProduction;
                    neutronFluxFast += neutronProduction * 0.6f;
                    neutronFluxSlow += neutronProduction * 0.4f;
                }
                
                // 检测非中子源燃料的中子下降情况
                if(!props.isNeutronSource && neutronFlux < previousNeutronFlux - 0.01f){
                    // 中子开始下降，直接清零结构内的中子
                    neutronFlux = 0f;
                    neutronFluxFast = 0f;
                    neutronFluxSlow = 0f;
                }
                
                // 消耗燃料
                if(Mathf.chance(props.fuelConsumptionRate * delta())){
                    items.remove(currentFuel, 1);
                    // 如果燃料耗尽，重置状态
                    if(items.get(currentFuel) <= 0){
                        enrichment = 1f;
                        xenonPoison = 0f;
                        coreHeat = 25f;
                    }
                }
                
                // 模拟燃料消耗和氙毒产生
                if(Mathf.chance(props.xenonGenerationRate * delta())){
                    enrichment = Mathf.clamp(enrichment - 0.001f, 0f, 1f);
                    // 氙毒产生
                    if(enrichment > 0.5f){
                        xenonPoison = Mathf.clamp(xenonPoison + 0.01f, 0f, 100f);
                    } else {
                        // 氙毒衰变
                        xenonPoison = Mathf.clamp(xenonPoison - 0.005f, 0f, 100f);
                    }
                }
                
                hasRod = true;
            } else {
                // 没有燃料时热量散失
                heat = Mathf.clamp(heat - 0.05f * delta(), 25f, maxHeat);
                coreHeat = Mathf.clamp(coreHeat - 0.04f * delta(), 25f, 2000f);
                // 氙毒衰变
                xenonPoison = Mathf.clamp(xenonPoison - 0.01f * delta(), 0f, 100f);
                hasRod = false;
                productionEfficiency = 0f;
            }
            
            // 过热检查 - 只在热量过高时爆炸
            if(heat >= maxHeat){
                meltdown();
            }
        }
        
        public void meltdown(){
            // 熔毁效果
            Damage.damage(x, y, size * tilesize * 5f, 1000f);
            Fx.explosion.at(x, y);
            Fx.massiveExplosion.at(x, y);
            
            // 产生放射性碎片
            for(int i = 0; i < 5; i++){
                float angle = Mathf.random(360f);
                float speed = Mathf.random(2f, 4f);
                float xVel = Mathf.cosDeg(angle) * speed;
                float yVel = Mathf.sinDeg(angle) * speed;
                Fx.fire.at(x, y, xVel, yVel);
            }
            
            remove();
        }
        
        @Override
        public void draw(){
            if(useBlockDrawer) drawer.draw(this);
            else super.draw();
            // 绘制热量指示
            if(heat > maxHeat * 0.7f){
                Draw.color(Color.red);
                Draw.alpha(0.5f + Mathf.sin(Time.time * 10f) * 0.2f);
                Fill.circle(x, y, size * tilesize / 2f);
            } else if(heat > maxHeat * 0.5f){
                Draw.color(Color.orange);
                Draw.alpha(0.3f + Mathf.sin(Time.time * 5f) * 0.1f);
                Fill.circle(x, y, size * tilesize / 2f);
            }
            
            Draw.color();
        }
        
        @Override
        public void write(Writes write){
            super.write(write);
            write.f(rodLevel);
            write.bool(explodeOnBroken);
            write.bool(hasRod);
            write.i(rodColor);
            write.f(enrichment);
            write.f(xenonPoison);
            write.f(coreHeat);
            write.f(productionEfficiency); // 写入生产效率
            write.f(previousNeutronFlux); // 写入前一帧的中子通量
            write.f(controlRodLimit); // 写入控制棒限制因子
            // 写入当前燃料的ID
            write.i(currentFuel == null ? -1 : currentFuel.id);
        }
        
        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);
            rodLevel = read.f();
            explodeOnBroken = read.bool();
            hasRod = read.bool();
            rodColor = read.i();
            enrichment = read.f();
            xenonPoison = read.f();
            coreHeat = read.f();
            productionEfficiency = read.f(); // 读取生产效率
            if(revision >= 9){
                previousNeutronFlux = read.f(); // 读取前一帧的中子通量
            }
            if(revision >= 10){
                controlRodLimit = read.f(); // 读取控制棒限制因子
            } else {
                controlRodLimit = 1f; // 默认值
            }
            // 读取当前燃料的ID
            int fuelId = read.i();
            currentFuel = fuelId == -1 ? null : content.item(fuelId);
        }
        
        @Override
        public byte version(){
            return 10; // 更新版本号
        }
        
        // 接受任何物品
        public boolean acceptItem(Teamc source, Item item) {
            return true;
        }
        
        public int acceptStack(Item item, int amount, Teamc source) {
            // 返回实际可以接受的数量，考虑物品容量
            return Math.min(amount, itemCapacity - items.get(item));
        }
        
        public void handleStack(Item item, int amount, Teamc source) {
            // 处理物品，添加到物品存储中
            items.add(item, amount);
        }
        
        @Override
        public int getMaximumAccepted(Item item) {
            // 返回实际可以接受的数量，考虑物品容量
            return itemCapacity - items.get(item);
        }
        
        @Override
        public void buildConfiguration(Table table) {
            Table cont = new Table().top();
            cont.left().defaults().left().growX();
            
            // 显示当前燃料信息
            Runnable rebuild = () -> {
                cont.clearChildren();
                
                Item fuel = getCurrentFuel();
                if(fuel != null){
                    RBMKFuelData.FuelProperties props = RBMKFuelData.getFuelProperties(fuel);
                    
                    cont.table(Styles.grayPanel, info -> {
                        info.left().defaults().left();
                        info.add("[accent]Current Fuel:[] " + fuel.localizedName).row();
                        info.add("Amount: " + items.get(fuel)).row();
                        if(props != null){
                            info.add("Heat Output: " + props.heat + "°C/s").row();
                            info.add("Enrichment: " + (int)(props.enrichment * 100) + "%").row();
                            info.add("Neutron Source: " + (props.isNeutronSource ? "Yes" : "No")).row();
                            info.add("Melting Point: " + props.meltingPoint + "°C").row();
                        }
                    }).growX().left().pad(10);
                    cont.row();
                    
                    // 显示燃料棒状态
                    cont.table(Styles.grayPanel, info -> {
                        info.left().defaults().left();
                        info.add("[accent]Fuel Rod Status:").row();
                        info.add("Enrichment: " + (int)(enrichment * 100) + "%").row();
                        info.add("Xenon Poison: " + (int)xenonPoison + "%").row();
                        info.add("Core Heat: " + (int)coreHeat + "°C").row();
                        info.add("Hull Heat: " + (int)heat + "°C").row();
                    }).growX().left().pad(10);
                } else {
                    cont.add("[gray]No fuel present[]").pad(10);
                }
            };
            
            rebuild.run();
            
            Table main = new Table().background(Styles.black6);
            ScrollPane pane = new ScrollPane(cont, Styles.smallPane);
            pane.setScrollingDisabled(true, false);
            pane.setOverscroll(false, false);
            main.add(pane).maxHeight(300);
            table.top().add(main);
        }
    }
}
