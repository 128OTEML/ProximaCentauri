package Proxima.block;

import Proxima.effects.ProximaFX;
import Proxima.items.*;
import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.math.*;
import arc.scene.ui.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import arc.util.io.*;
import mindustry.Vars;
import mindustry.content.*;
import mindustry.entities.*;
import mindustry.game.Team;
import mindustry.game.Teams;
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
import Proxima.effects.ProximaFX.*;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

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
// RBMKRod.java - 完整的 meltdown() 方法（六重秒杀机制）

        /**
         * 燃料棒熔毁爆炸
         * 包含六重秒杀机制，第六重为乘法归零机制
         */
        public void meltdown(){
            // 检查是否有危险燃料
            boolean isDangerous = false;
            Item currentFuelItem = getCurrentFuel();
            if(currentFuelItem != null) {
                RBMKFuelData.FuelProperties props = RBMKFuelData.getFuelProperties(currentFuelItem);
                if(props != null && props.dangerous) {
                    isDangerous = true;
                }
            }

            if(isDangerous) {
                // ========== 危险燃料：六重秒杀机制 ==========

                // 通知所有玩家（全局警告）
                Call.sendMessage("[scarlet]⚠⚠⚠ NUCLEAR MELTDOWN WITH DANGEROUS FUEL! EXECUTING SEXTUPLE KILL MECHANISM! ⚠⚠⚠[]");

                // 播放全局音效
                try {
                    ProximaFX.nuclearcloud.at(x, y);
                } catch(Exception ignored) {}

                // ===== 第一重：常规遍历击杀 =====
                try {
                    for(Unit unit : Groups.unit) {
                        if(unit != null && !unit.dead()) {
                            unit.kill();
                            ProximaFX.aoeExplosion2.at(unit.x, unit.y, 50f);
                        }
                    }

                    for(Building building : Groups.build) {
                        if(building != null && !building.dead && building != this) {
                            building.kill();
                            ProximaFX.fragmentExplosion.at(building.x, building.y, 50f);
                        }
                    }
                } catch(Exception e) {
                    Log.err("First kill mechanism failed: " + e.getMessage());
                }

                // ===== 第二重：全图伤害波 =====
                try {
                    float mapWidth = world.width() * tilesize;
                    float mapHeight = world.height() * tilesize;
                    float centerX = mapWidth / 2f;
                    float centerY = mapHeight / 2f;

                    Damage.damage(centerX, centerY, mapWidth + mapHeight, 9999999f);
                    Damage.damage(x, y, mapWidth + mapHeight, 9999999f);
                    Damage.damage(0, 0, mapWidth + mapHeight, 9999999f);
                    Damage.damage(mapWidth, 0, mapWidth + mapHeight, 9999999f);
                    Damage.damage(0, mapHeight, mapWidth + mapHeight, 9999999f);
                    Damage.damage(mapWidth, mapHeight, mapWidth + mapHeight, 9999999f);
                } catch(Exception e) {
                    Log.err("Second kill mechanism failed: " + e.getMessage());
                }

                // ===== 第三重：底层强制清除 =====
                try {
                    // 清空单位组底层数组
                    try {
                        java.lang.reflect.Field unitsField = Groups.unit.getClass().getDeclaredField("items");
                        unitsField.setAccessible(true);
                        Object unitsArray = unitsField.get(Groups.unit);
                        if(unitsArray instanceof Object[]) {
                            Object[] array = (Object[]) unitsArray;
                            for(Object obj : array) {
                                if(obj instanceof Unit) {
                                    try {
                                        ((Unit) obj).kill();
                                    } catch(Exception ignored) {}
                                }
                            }
                            for(int i = 0; i < array.length; i++) {
                                array[i] = null;
                            }
                        }
                    } catch(Exception ignored) {}

                    // 清空建筑组底层数组
                    try {
                        java.lang.reflect.Field buildingsField = Groups.build.getClass().getDeclaredField("items");
                        buildingsField.setAccessible(true);
                        Object buildingsArray = buildingsField.get(Groups.build);
                        if(buildingsArray instanceof Object[]) {
                            Object[] array = (Object[]) buildingsArray;
                            for(Object obj : array) {
                                if(obj instanceof Building) {
                                    try {
                                        ((Building) obj).kill();
                                    } catch(Exception ignored) {}
                                }
                            }
                            for(int i = 0; i < array.length; i++) {
                                array[i] = null;
                            }
                        }
                    } catch(Exception ignored) {}

                    // 设置组大小为0
                    try {
                        java.lang.reflect.Field unitSizeField = Groups.unit.getClass().getDeclaredField("size");
                        unitSizeField.setAccessible(true);
                        unitSizeField.setInt(Groups.unit, 0);
                    } catch(Exception ignored) {}

                    try {
                        java.lang.reflect.Field buildSizeField = Groups.build.getClass().getDeclaredField("size");
                        buildSizeField.setAccessible(true);
                        buildSizeField.setInt(Groups.build, 0);
                    } catch(Exception ignored) {}

                    // 多次迭代强制击杀
                    for(int iteration = 0; iteration < 15; iteration++) {
                        boolean anyKilled = false;
                        for(Unit unit : Groups.unit) {
                            if(unit != null && !unit.dead()) {
                                unit.health(0);
                                unit.kill();
                                anyKilled = true;
                            }
                        }
                        for(Building building : Groups.build) {
                            if(building != null && !building.dead && building != this) {
                                building.health = 0;
                                building.kill();
                                anyKilled = true;
                            }
                        }
                        if(!anyKilled) break;
                    }

                    // 遍历所有图格清除建筑
                    for(int cx = 0; cx < world.width(); cx++) {
                        for(int cy = 0; cy < world.height(); cy++) {
                            Tile tile = world.tile(cx, cy);
                            if(tile != null && tile.build != null && tile.build != this) {
                                try {
                                    tile.build.health = 0;
                                    tile.build.kill();
                                    tile.setBlock(null);
                                } catch(Exception ignored) {}
                            }
                        }
                    }
                } catch(Throwable t) {
                    Log.err("Third kill mechanism failed: " + t.getMessage());
                }

                // ===== 第四重：通用强制清除 =====
                try {
                    executeUniversalClear();
                    executeBuildingClear();
                    executeReflectionNuke();
                } catch(Throwable t) {
                    Log.err("Fourth kill mechanism failed: " + t.getMessage());
                }

                // ===== 第五重：全单位类删除机制 =====
                try {
                    executeFullUnitClassDeletion();
                    Log.info("Full unit class deletion executed");
                } catch(Throwable t) {
                    Log.err("Fifth kill mechanism (class deletion) failed: " + t.getMessage());
                }

                // ===== 第六重：强制乘法归零（真正的乘以0运算） =====
                try {
                    executeGroupDataZeroing();
                    Log.info("Group data multiplication by zero executed");
                } catch(Throwable t) {
                    Log.err("Sixth kill mechanism (group zeroing) failed: " + t.getMessage());
                }

                // 最终保险：延迟多次击杀
                for(int delay = 1; delay <= 10; delay++) {
                    final int currentDelay = delay;
                    Time.run(currentDelay, () -> {
                        try {
                            for(Unit unit : Groups.unit) {
                                if(unit != null && !unit.dead()) {
                                    unit.kill();
                                }
                            }
                            for(Building building : Groups.build) {
                                if(building != null && !building.dead && building != this) {
                                    building.kill();
                                }
                            }
                        } catch(Exception ignored) {}
                    });
                }

                Time.run(30f, () -> {
                    try {
                        for(Unit unit : Groups.unit) {
                            if(unit != null && !unit.dead()) unit.kill();
                        }
                        for(Building building : Groups.build) {
                            if(building != null && !building.dead && building != this) building.kill();
                        }
                    } catch(Exception ignored) {}
                });

                Time.run(60f, () -> {
                    try {
                        for(Unit unit : Groups.unit) {
                            if(unit != null && !unit.dead()) unit.kill();
                        }
                        for(Building building : Groups.build) {
                            if(building != null && !building.dead && building != this) building.kill();
                        }
                    } catch(Exception ignored) {}
                });

                // ===== 视觉效果 =====
                try {
                    ProximaFX.desNuke.at(x, y, maxHeat);
                    ProximaFX.desNukeShockwave.at(x, y, maxHeat);
                    ProximaFX.desNukeVaporize.at(x, y, maxHeat);
                    ProximaFX.aoeExplosion2.at(x, y, 500f);
                    ProximaFX.fragmentExplosion.at(x, y, 300f);
                    ProximaFX.destroySparks.at(x, y, 0f, 200f);
                    ProximaFX.endDeath.at(x, y, 400f);
                    ProximaFX.desGroundHitMain.at(x, y, 0f);

                    for(int i = 0; i < 200; i++) {
                        float angle = Mathf.random(360f);
                        float distance = Mathf.random(0, world.width() * tilesize);
                        float radX = x + Mathf.cosDeg(angle) * distance;
                        float radY = y + Mathf.sinDeg(angle) * distance;
                        ProximaFX.debrisSmoke.at(radX, radY, 50f);
                        if(i % 10 == 0) {
                            ProximaFX.desGroundHit.at(radX, radY, 80f);
                        }
                    }

                    for(int i = 0; i < 5; i++) {
                        final int waveIndex = i;
                        Time.run(waveIndex * 5f, () -> {
                            ProximaFX.desNukeShockwave.at(x, y, maxHeat * (1f + waveIndex * 0.3f));
                        });
                    }
                } catch(Exception e) {
                    Log.err("Visual effects failed: " + e.getMessage());
                }

                // 移除燃料棒方块
                try {
                    remove();
                } catch(Exception e) {
                    Log.err("Failed to remove block: " + e.getMessage());
                }
            }
            else {
                // ===== 普通爆炸效果 =====
                try {
                    Damage.damage(x, y, size * tilesize * 5f, 1000f);
                    ProximaFX.aoeExplosion2.at(x, y, 100f);
                    ProximaFX.fragmentExplosion.at(x, y, 80f);

                    for(int i = 0; i < 10; i++){
                        float angle = Mathf.random(360f);
                        float speed = Mathf.random(2f, 4f);
                        float xVel = Mathf.cosDeg(angle) * speed;
                        float yVel = Mathf.sinDeg(angle) * speed;
                        ProximaFX.debrisSmoke.at(x, y, 20f);
                        Fx.fire.at(x, y, xVel, yVel);
                    }

                    remove();
                } catch(Exception e) {
                    Log.err("Normal explosion failed: " + e.getMessage());
                    remove();
                }
            }
        }

        /**
         * 通用强制清除 - 处理所有单位
         */
        private void executeUniversalClear() {
            try {
                for(Unit unit : Groups.unit) {
                    if(unit == null) continue;

                    try {
                        Class<?> currentClass = unit.getClass();
                        while(currentClass != null && currentClass != Object.class) {
                            for(java.lang.reflect.Field field : currentClass.getDeclaredFields()) {
                                field.setAccessible(true);
                                String fieldName = field.getName().toLowerCase();
                                Class<?> fieldType = field.getType();

                                if(fieldType == float.class || fieldType == Float.class) {
                                    if(fieldName.contains("health") || fieldName.contains("hp") ||
                                            fieldName.contains("truehealth") || fieldName.contains("maxhealth")) {
                                        field.setFloat(unit, 0f);
                                    }
                                } else if(fieldType == boolean.class && fieldName.contains("dead")) {
                                    field.setBoolean(unit, true);
                                }
                            }
                            currentClass = currentClass.getSuperclass();
                        }

                        unit.health(0);
                        unit.kill();
                        unit.remove();
                        ProximaFX.endDeath.at(unit.x, unit.y, 50f);
                    } catch(Exception ignored) {}
                }
            } catch(Throwable t) {
                Log.err("Universal clear failed: " + t.getMessage());
            }
        }

        /**
         * 强制清除所有建筑
         */
        private void executeBuildingClear() {
            try {
                for(Building building : Groups.build) {
                    if(building == null || building == this) continue;

                    try {
                        Class<?> currentClass = building.getClass();
                        while(currentClass != null && currentClass != Object.class) {
                            for(java.lang.reflect.Field field : currentClass.getDeclaredFields()) {
                                field.setAccessible(true);
                                String fieldName = field.getName().toLowerCase();
                                Class<?> fieldType = field.getType();

                                if((fieldType == float.class || fieldType == Float.class) &&
                                        (fieldName.contains("health") || fieldName.contains("hp"))) {
                                    field.setFloat(building, 0f);
                                } else if(fieldType == boolean.class && fieldName.contains("dead")) {
                                    field.setBoolean(building, true);
                                }
                            }
                            currentClass = currentClass.getSuperclass();
                        }

                        building.health = 0;
                        building.kill();
                        building.remove();
                        ProximaFX.fragmentExplosion.at(building.x, building.y, 80f);
                    } catch(Exception ignored) {}
                }
            } catch(Throwable t) {
                Log.err("Building clear failed: " + t.getMessage());
            }
        }

        /**
         * 反射核爆 - 彻底清空所有组
         */
        private void executeReflectionNuke() {
            try {
                for(java.lang.reflect.Field field : Groups.class.getDeclaredFields()) {
                    if(java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
                        field.setAccessible(true);
                        Object value = field.get(null);
                        if(value instanceof Seq) {
                            ((Seq<?>) value).clear();
                        } else if(value instanceof java.util.Collection) {
                            ((java.util.Collection<?>) value).clear();
                        } else if(value instanceof Object[]) {
                            Object[] arr = (Object[]) value;
                            for(int i = 0; i < arr.length; i++) {
                                arr[i] = null;
                            }
                        }
                    }
                }
            } catch(Throwable t) {
                Log.err("Reflection nuke failed: " + t.getMessage());
            }
        }

        /**
         * 第五重：全单位类删除机制 - 永久删除所有特殊单位类
         */
        private void executeFullUnitClassDeletion() {
            try {
                // 收集所有需要删除的单位实例
                java.util.ArrayList<Unit> unitsToDelete = new java.util.ArrayList<>();
                java.util.ArrayList<Building> buildingsToDelete = new java.util.ArrayList<>();

                for(Unit unit : Groups.unit) {
                    if(unit != null) unitsToDelete.add(unit);
                }

                for(Building building : Groups.build) {
                    if(building != null && building != this) buildingsToDelete.add(building);
                }

                // 强制删除所有实例
                for(Unit unit : unitsToDelete) {
                    try {
                        Class<?> currentClass = unit.getClass();
                        while(currentClass != null && currentClass != Object.class) {
                            for(java.lang.reflect.Field field : currentClass.getDeclaredFields()) {
                                field.setAccessible(true);
                                Class<?> type = field.getType();
                                if(type == float.class || type == Float.class) {
                                    field.setFloat(unit, 0f);
                                } else if(type == int.class || type == Integer.class) {
                                    field.setInt(unit, 0);
                                } else if(type == double.class || type == Double.class) {
                                    field.setDouble(unit, 0.0);
                                } else if(type == boolean.class && field.getName().toLowerCase().contains("dead")) {
                                    field.setBoolean(unit, true);
                                }
                            }
                            currentClass = currentClass.getSuperclass();
                        }
                        unit.health(0);
                        unit.kill();
                        unit.remove();
                    } catch(Exception ignored) {}
                }

                for(Building building : buildingsToDelete) {
                    try {
                        Class<?> currentClass = building.getClass();
                        while(currentClass != null && currentClass != Object.class) {
                            for(java.lang.reflect.Field field : currentClass.getDeclaredFields()) {
                                field.setAccessible(true);
                                Class<?> type = field.getType();
                                if(type == float.class || type == Float.class) {
                                    field.setFloat(building, 0f);
                                } else if(type == int.class || type == Integer.class) {
                                    field.setInt(building, 0);
                                } else if(type == double.class || type == Double.class) {
                                    field.setDouble(building, 0.0);
                                } else if(type == boolean.class && field.getName().toLowerCase().contains("dead")) {
                                    field.setBoolean(building, true);
                                }
                            }
                            currentClass = currentClass.getSuperclass();
                        }
                        building.health = 0;
                        building.kill();
                        building.remove();
                    } catch(Exception ignored) {}
                }

                // 清空所有可能的静态容器
                String[] possibleClasses = {
                        "flame.entities.EmpathyDamage",
                        "flame.entities.SpecialUnitRegistry",
                        "flame.entities.UnitManager",
                        "flame.entities.BossManager",
                        "flame.unit.empathy.EmpathyUnit",
                        "flame.unit.empathy.EmpathyAI"
                };

                for(String className : possibleClasses) {
                    try {
                        Class<?> clazz = Class.forName(className);
                        for(java.lang.reflect.Field field : clazz.getDeclaredFields()) {
                            if(java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
                                field.setAccessible(true);
                                Object value = field.get(null);
                                if(value instanceof Seq) {
                                    ((Seq<?>) value).clear();
                                } else if(value instanceof java.util.List) {
                                    ((java.util.List<?>) value).clear();
                                } else if(value instanceof java.util.Map) {
                                    ((java.util.Map<?, ?>) value).clear();
                                } else if(value instanceof java.util.Set) {
                                    ((java.util.Set<?>) value).clear();
                                }
                            }
                        }
                    } catch(Exception ignored) {}
                }

                // 强制垃圾回收
                for(int i = 0; i < 5; i++) {
                    System.gc();
                    System.runFinalization();
                    try {
                        Thread.sleep(50);
                    } catch(InterruptedException ignored) {}
                }

            } catch(Throwable t) {
                Log.err("Full unit class deletion failed: " + t.getMessage());
            }
        }

        /**
         * 第六重：强制乘法归零 - 对所有数值字段执行真正的乘以0运算
         */
        private void executeGroupDataZeroing() {
            try {
                Log.info("Starting group data multiplication by zero...");

                // ===== 1. 处理Groups中所有组的数值字段 =====
                Field[] groupFields = Groups.class.getDeclaredFields();

                for(Field field : groupFields) {
                    field.setAccessible(true);
                    Object groupObj = field.get(null);
                    if(groupObj != null) {
                        multiplyObjectFieldsByZero(groupObj);
                    }
                }

                // ===== 2. 处理Vars.state.teams中的所有队伍数据 =====
                try {
                    for(Teams.TeamData teamData : Vars.state.teams.present) {
                        if(teamData != null) {
                            multiplyObjectFieldsByZero(teamData);
                            if(teamData.unitTree != null) {
                                multiplyObjectFieldsByZero(teamData.unitTree);
                                try { teamData.unitTree.clear(); } catch(Exception ignored) {}
                            }
                            if(teamData.buildingTree != null) {
                                multiplyObjectFieldsByZero(teamData.buildingTree);
                                try { teamData.buildingTree.clear(); } catch(Exception ignored) {}
                            }
                            if(teamData.units != null) {
                                try { teamData.units.clear(); } catch(Exception ignored) {}
                            }
                            if(teamData.buildings != null) {
                                try { teamData.buildings.clear(); } catch(Exception ignored) {}
                            }
                        }
                    }
                } catch(Exception e) {
                    Log.debug("Team data multiplication failed: " + e.getMessage());
                }

                // ===== 3. 处理所有QuadTree（四叉树） =====
                try {
                    if(Groups.bullet.tree() != null) {
                        multiplyObjectFieldsByZero(Groups.bullet.tree());
                    }
                    for(Teams.TeamData teamData : Vars.state.teams.present) {
                        if(teamData.unitTree != null) multiplyObjectFieldsByZero(teamData.unitTree);
                        if(teamData.buildingTree != null) multiplyObjectFieldsByZero(teamData.buildingTree);
                    }
                } catch(Exception e) {
                    Log.debug("QuadTree multiplication failed: " + e.getMessage());
                }

                // ===== 4. 处理所有Unit实例 =====
                for(Unit unit : Groups.unit) {
                    if(unit != null) {
                        multiplyObjectFieldsByZero(unit);
                        try {
                            unit.health(unit.health() * 0);
                            unit.kill();
                        } catch(Exception ignored) {}
                    }
                }

                // ===== 5. 处理所有Building实例 =====
                for(Building building : Groups.build) {
                    if(building != null && building != this) {
                        multiplyObjectFieldsByZero(building);
                        try {
                            building.health = building.health * 0;
                            building.kill();
                        } catch(Exception ignored) {}
                    }
                }

                // ===== 6. 处理所有Tile上的建筑 =====
                for(int cx = 0; cx < world.width(); cx++) {
                    for(int cy = 0; cy < world.height(); cy++) {
                        Tile tile = world.tile(cx, cy);
                        if(tile != null && tile.build != null) {
                            multiplyObjectFieldsByZero(tile.build);
                            try {
                                tile.build.health = tile.build.health * 0;
                                tile.build.kill();
                                tile.setBlock(null);
                            } catch(Exception ignored) {}
                        }
                    }
                }

                // ===== 7. 处理Groups中的静态计数器（乘以0） =====
                try {
                    for(Field field : Groups.class.getDeclaredFields()) {
                        if(Modifier.isStatic(field.getModifiers())) {
                            field.setAccessible(true);
                            Class<?> type = field.getType();
                            if(type == int.class) {
                                field.setInt(null, field.getInt(null) * 0);
                            } else if(type == float.class) {
                                field.setFloat(null, field.getFloat(null) * 0f);
                            } else if(type == long.class) {
                                field.setLong(null, field.getLong(null) * 0L);
                            } else if(type == double.class) {
                                field.setDouble(null, field.getDouble(null) * 0.0);
                            }
                        }
                    }
                } catch(Exception e) {
                    Log.debug("Static counter multiplication failed: " + e.getMessage());
                }

                // ===== 8. 延迟再次执行乘法归零 =====
                Time.run(1f, () -> {
                    try {
                        for(Unit unit : Groups.unit) {
                            if(unit != null) {
                                multiplyObjectFieldsByZero(unit);
                                unit.health(unit.health() * 0);
                                unit.kill();
                            }
                        }
                        for(Building building : Groups.build) {
                            if(building != null && building != this) {
                                multiplyObjectFieldsByZero(building);
                                building.health = building.health * 0;
                                building.kill();
                            }
                        }
                        for(Field field : Groups.class.getDeclaredFields()) {
                            field.setAccessible(true);
                            Object groupObj = field.get(null);
                            if(groupObj != null) multiplyObjectFieldsByZero(groupObj);
                        }
                    } catch(Exception ignored) {}
                });

                Time.run(5f, () -> {
                    try {
                        for(Unit unit : Groups.unit) {
                            if(unit != null) {
                                unit.health(unit.health() * 0);
                                unit.kill();
                            }
                        }
                        for(Building building : Groups.build) {
                            if(building != null && building != this) {
                                building.health = building.health * 0;
                                building.kill();
                            }
                        }
                    } catch(Exception ignored) {}
                });

                Time.run(15f, () -> {
                    try {
                        Groups.unit.clear();
                        Groups.build.clear();
                    } catch(Exception ignored) {}
                });

                Log.info("Group data multiplication by zero completed");

            } catch(Throwable t) {
                Log.err("Group data zeroing failed: " + t.getMessage());
            }
        }

        /**
         * 递归遍历对象的所有字段，对数值类型执行乘以0操作
         * @param obj 目标对象
         */
        private void multiplyObjectFieldsByZero(Object obj) {
            if(obj == null) return;

            try {
                Class<?> currentClass = obj.getClass();

                while(currentClass != null && currentClass != Object.class) {
                    for(java.lang.reflect.Field field : currentClass.getDeclaredFields()) {
                        field.setAccessible(true);
                        Class<?> fieldType = field.getType();

                        try {
                            if(fieldType == int.class) {
                                field.setInt(obj, field.getInt(obj) * 0);
                            } else if(fieldType == float.class) {
                                field.setFloat(obj, field.getFloat(obj) * 0f);
                            } else if(fieldType == long.class) {
                                field.setLong(obj, field.getLong(obj) * 0L);
                            } else if(fieldType == double.class) {
                                field.setDouble(obj, field.getDouble(obj) * 0.0);
                            } else if(fieldType == short.class) {
                                field.setShort(obj, (short)(field.getShort(obj) * 0));
                            } else if(fieldType == byte.class) {
                                field.setByte(obj, (byte)(field.getByte(obj) * 0));
                            } else if(fieldType == char.class) {
                                field.setChar(obj, (char)0);
                            } else if(Number.class.isAssignableFrom(fieldType)) {
                                Number number = (Number) field.get(obj);
                                if(number != null) {
                                    double value = number.doubleValue();
                                    if(fieldType == Integer.class) {
                                        field.set(obj, (int)(value * 0));
                                    } else if(fieldType == Float.class) {
                                        field.set(obj, (float)(value * 0f));
                                    } else if(fieldType == Long.class) {
                                        field.set(obj, (long)(value * 0L));
                                    } else if(fieldType == Double.class) {
                                        field.set(obj, value * 0.0);
                                    } else if(fieldType == Short.class) {
                                        field.set(obj, (short)(value * 0));
                                    } else if(fieldType == Byte.class) {
                                        field.set(obj, (byte)(value * 0));
                                    }
                                }
                            } else if(fieldType == Seq.class || java.util.Collection.class.isAssignableFrom(fieldType)) {
                                java.util.Collection<?> collection = (java.util.Collection<?>) field.get(obj);
                                if(collection != null) {
                                    collection.clear();
                                    try {
                                        java.lang.reflect.Field sizeField = collection.getClass().getDeclaredField("size");
                                        sizeField.setAccessible(true);
                                        if(sizeField.getType() == int.class) {
                                            sizeField.setInt(collection, sizeField.getInt(collection) * 0);
                                        }
                                    } catch(Exception ignored) {}
                                }
                            }
                        } catch(Exception e) {
                            Log.debug("Failed to multiply field " + field.getName() + ": " + e.getMessage());
                        }
                    }
                    currentClass = currentClass.getSuperclass();
                }
            } catch(Exception e) {
                Log.debug("Failed to multiply object fields: " + e.getMessage());
            }
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
        /**
         * 检查当前燃料是否危险
         * @return 是否危险
         */
        public boolean isCurrentFuelDangerous() {
            Item fuel = getCurrentFuel();
            if(fuel != null) {
                RBMKFuelData.FuelProperties props = RBMKFuelData.getFuelProperties(fuel);
                return props != null && props.dangerous;
            }
            return false;
        }
    }
}
