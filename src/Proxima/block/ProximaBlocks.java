package Proxima.block;

import arc.struct.*;
import mindustry.content.*;
import mindustry.type.*;
import mindustry.world.*;
import mindustry.world.blocks.environment.*;
import mindustry.world.meta.BuildVisibility;
import Proxima.items.*;

public class ProximaBlocks{
    public static Block oreIron, oreUranium, oreManganese, oreQuartz;

    public static final Seq<Block> proximaOres = new Seq<>();

    // 比邻星核心方块
    public static Block proximaCore;

    public static Block largeHeatPipe;

    // RBMK反应堆方块
    public static Block rbmkBase;
    public static Block rbmkRod;
    public static Block rbmkRodModerated;
    public static Block rbmkControl;
    public static Block rbmkControlModerated;
    public static Block rbmkCooler;
    public static Block rbmkReflector;
    public static Block rbmkModerator;
    public static Block rbmkAbsorber;
    public static Block rbmkBlank;
    public static Block rbmkBoiler;
    public static Block rbmkHeater;
    public static Block rbmkConsole;

    // 末日防空炮台
    public static Block endAntiAirTurret;

    // RBMK推送器
    public static Block rbmkPusher;

    // 大型蒸汽轮机
    public static Block largeSteamTurbine;

    // DFC 系统
    // DFCBase 保留作为基础类
    public static Block dfcLaserEmitter;
    public static Block dfcCore;
    public static Block dfcStabilizer;
    public static Block dfcInjector;
    public static Block dfcEnergyEmitter;
    public static Block dfcEnergyReceiver;
    public static Block dfcExchanger;
    public static Block dfcCreativeEmitter;

    // 机械臂
    public static Block mechanicalArm;

    // 传送带
    public static Block proximaConveyor;
    
    // 侧输出传送带
    public static Block sideOutputConveyor;
    
    // 高速侧输出传送带
    public static Block fastSideOutputConveyor;

    // Junction
    public static Block proximaJunction;

    // 测试容器
    public static Block proximaTestContainer;

    // 分类物品桥
    public static Block adaptItemBridge;

    // 分类流体桥
    public static Block adaptLiquidBridge;

    // 管道
    public static Block pipe;

    public static void load(){
        oreIron = new OreBlock(ProximaItems.iron){{
            variants = 3;
        }};

        oreUranium = new OreBlock(ProximaItems.uranium){{
            variants = 3;
        }};

        oreManganese = new OreBlock(ProximaItems.manganese){{
            variants = 3;
        }};

        oreQuartz = new OreBlock(ProximaItems.quartz){{
            variants = 3;
        }};

        proximaOres.addAll(oreIron, oreUranium, oreManganese, oreQuartz);

        // 比邻星核心方块
        proximaCore = new ProximaCoreBlock("proxima-core");

        largeHeatPipe = new LargeHeatPipe("large-heat-pipe"){{
            requirements(Category.crafting, ItemStack.with(
                Items.copper, 50,
                Items.lead, 100,
                Items.graphite, 50
            ));
        }};

        // RBMK基础方块
        rbmkBase = new RBMKBase("rbmk-base"){{
            requirements(Category.power, ItemStack.with(
                Items.copper, 100,
                Items.lead, 100,
                Items.graphite, 50
            ));
        }};

        // RBMK燃料棒
        rbmkRod = new RBMKRod("rbmk-rod"){{
            requirements(Category.power, ItemStack.with(
                Items.copper, 200,
                Items.lead, 150,
                Items.titanium, 100,
                Items.thorium, 25
            ));
        }};

        // RBMK慢化燃料棒
        rbmkRodModerated = new RBMKRod("rbmk-rod-moderated"){{
            moderated = true;
            powerProduction = 75f;
            fuelConsumption = 0.0015f;
            requirements(Category.power, ItemStack.with(
                Items.copper, 250,
                Items.lead, 200,
                Items.titanium, 150,
                Items.thorium, 50,
                Items.graphite, 100
            ));
        }};

        // RBMK控制棒
        rbmkControl = new RBMKControl("rbmk-control"){{
            requirements(Category.power, ItemStack.with(
                Items.copper, 150,
                Items.lead, 100,
                Items.titanium, 50,
                Items.graphite, 50
            ));
        }};

        // RBMK慢化控制棒
        rbmkControlModerated = new RBMKControl("rbmk-control-moderated"){{
            moderated = true;
            maxControlValue = 1.5f;
            requirements(Category.power, ItemStack.with(
                Items.copper, 200,
                Items.lead, 150,
                Items.titanium, 100,
                Items.graphite, 100
            ));
        }};

        // RBMK冷却器
        rbmkCooler = new RBMKCooler("rbmk-cooler"){{
            requirements(Category.power, ItemStack.with(
                Items.copper, 100,
                Items.lead, 80,
                Items.metaglass, 50
            ));
        }};

        // RBMK反射器
        rbmkReflector = new RBMKReflector("rbmk-reflector"){{
            requirements(Category.power, ItemStack.with(
                Items.copper, 100,
                Items.lead, 150,
                Items.graphite, 100
            ));
        }};

        // RBMK慢化剂
        rbmkModerator = new RBMKModerator("rbmk-moderator"){{
            requirements(Category.power, ItemStack.with(
                Items.copper, 100,
                Items.lead, 100,
                Items.graphite, 150
            ));
        }};

        // RBMK吸收器
        rbmkAbsorber = new RBMKAbsorber("rbmk-absorber"){{
            requirements(Category.power, ItemStack.with(
                Items.copper, 100,
                Items.lead, 200,
                Items.graphite, 50
            ));
        }};

        // RBMK空白结构
        rbmkBlank = new RBMKBlank("rbmk-blank"){{
            requirements(Category.power, ItemStack.with(
                Items.copper, 50,
                Items.lead, 50
            ));
        }};

        // RBMK锅炉
        rbmkBoiler = new RBMKBoiler("rbmk-boiler"){{
            requirements(Category.power, ItemStack.with(
                Items.copper, 150,
                Items.lead, 100,
                Items.metaglass, 80,
                Items.graphite, 50
            ));
        }};

        // RBMK加热器
        rbmkHeater = new RBMKHeater("rbmk-heater"){{
            requirements(Category.power, ItemStack.with(
                Items.copper, 120,
                Items.lead, 100,
                Items.titanium, 80,
                Items.graphite, 50
            ));
        }};

        // RBMK控制台
        rbmkConsole = new RBMKConsole("rbmk-console"){{
            requirements(Category.power, ItemStack.with(
                Items.copper, 1000,
                Items.lead, 800,
                Items.silicon, 300,
                Items.titanium, 500,
                Items.thorium, 100
            ));
        }};

        // RBMK推送器
        rbmkPusher = new RBMKPusher("rbmk-pusher"){{
            requirements(Category.power, ItemStack.with(
                Items.copper, 200,
                Items.lead, 150,
                Items.titanium, 100,
                Items.silicon, 50
            ));
        }};

        // 末日防空炮台
        endAntiAirTurret = new EndAntiAirTurret("end-anti-air-turret"){{
            requirements(Category.turret, ItemStack.with(
                Items.copper, 5000,
                Items.lead, 4000,
                Items.titanium, 3000,
                Items.thorium, 2000,
                Items.silicon, 1500
            ));
        }};

        // 大型蒸汽轮机
        largeSteamTurbine = new LargeSteamTurbine("large-steam-turbine"){{
            requirements(Category.power, ItemStack.with(
                Items.titanium, 300,
                Items.silicon, 200,
                Items.graphite, 150
            ));
        }};

        // 分类物品桥
        adaptItemBridge = new AdaptItemBridge("adapt-item-bridge"){{
            requirements(Category.distribution, ItemStack.with(
                Items.copper, 50,
                Items.lead, 30,
                Items.titanium, 20,
                Items.silicon, 15
            ));
            buildVisibility = BuildVisibility.shown;
            alwaysUnlocked = true;

            hasPower = false;
            range = 6;
            health = 300;

            placeableLiquid = true;
        }};

        // 分类流体桥
        adaptLiquidBridge = new AdaptLiquidBridge("adapt-liquid-bridge"){{
            requirements(Category.distribution, ItemStack.with(
                Items.copper, 60,
                Items.lead, 40,
                Items.titanium, 25,
                Items.silicon, 20
            ));
            buildVisibility = BuildVisibility.shown;
            alwaysUnlocked = true;

            hasPower = false;
            range = 6;
            health = 350;
        }};

        // DFC 激光发射器
        dfcLaserEmitter = new DFCLaserEmitter("dfc-laser-emitter"){{
            requirements(Category.turret, ItemStack.with(
                Items.copper, 2000,
                Items.lead, 1500,
                Items.titanium, 1000,
                Items.thorium, 500,
                Items.silicon, 300
            ));
        }};

        // DFC 核心
        dfcCore = new DFCore("dfc-core"){{
            requirements(Category.power, ItemStack.with(
                Items.copper, 3000,
                Items.lead, 2500,
                Items.titanium, 2000,
                Items.thorium, 1000,
                Items.silicon, 500
            ));
        }};

        // DFC 稳定器
        dfcStabilizer = new DFCStabilizer("dfc-stabilizer"){{
            requirements(Category.power, ItemStack.with(
                Items.copper, 2500,
                Items.lead, 2000,
                Items.titanium, 1500,
                Items.thorium, 800,
                Items.silicon, 400
            ));
        }};

        // DFC 燃料注入器
        dfcInjector = new DFCInjector("dfc-injector"){{
            requirements(Category.power, ItemStack.with(
                Items.copper, 1500,
                Items.lead, 1200,
                Items.titanium, 800,
                Items.silicon, 300
            ));
        }};

        // DFC 能量发射器
        dfcEnergyEmitter = new DFCEnergyEmitter("dfc-energy-emitter"){{
            requirements(Category.power, ItemStack.with(
                Items.copper, 2000,
                Items.lead, 1800,
                Items.titanium, 1200,
                Items.silicon, 500
            ));
        }};

        // DFC 能量接收器
        dfcEnergyReceiver = new DFCEnergyReceiver("dfc-energy-receiver"){{
            requirements(Category.power, ItemStack.with(
                Items.copper, 1800,
                Items.lead, 1600,
                Items.titanium, 1000,
                Items.silicon, 400
            ));
        }};

        // DFC 交换器
        dfcExchanger = new DFCExchanger("dfc-exchanger"){{
            requirements(Category.power, ItemStack.with(
                Items.copper, 1600,
                Items.lead, 1400,
                Items.titanium, 900,
                Items.silicon, 350
            ));
        }};

        // DFC 创意发射器
        dfcCreativeEmitter = new DFCCreativeEmitter("dfc-creative-emitter"){{
            requirements(Category.power, ItemStack.with(
                Items.copper, 5000,
                Items.lead, 4000,
                Items.titanium, 3000,
                Items.thorium, 2000,
                Items.silicon, 1500
            ));
        }};

        // 机械臂
        mechanicalArm = new MechanicalArm("mechanical-arm"){{
            requirements(Category.logic, ItemStack.with(
                Items.copper, 100,
                Items.lead, 75,
                Items.titanium, 50,
                Items.silicon, 25
            ));
        }};
        
        // 高速侧输出传送带
        fastSideOutputConveyor = new SideOutputConveyor("fast-side-output-conveyor"){{
            speed = 0.15f;
            displayedSpeed = 20f;
            requirements(Category.distribution, ItemStack.with(
                Items.copper, 20,
                Items.lead, 10,
                Items.titanium, 5
            ));
        }};

        // 万用交叉器
        proximaJunction = new Junction("proxima-junction"){{
            requirements(Category.distribution, ItemStack.with(
                Items.copper, 15,
                Items.lead, 10
            ));
        }};

        // 管道
        pipe = new Pipe("pipe"){{
            requirements(Category.liquid, ItemStack.with(
                Items.copper, 10,
                Items.lead, 5
            ));
            bridgeReplacement = adaptLiquidBridge;
        }};
    }
}
