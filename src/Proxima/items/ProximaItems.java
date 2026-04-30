package Proxima.items;

import arc.graphics.*;
import arc.struct.*;
import mindustry.content.*;
import mindustry.type.*;

/**
 * Proxima物品注册
 */
public class ProximaItems{
    public static Item iron, uranium, manganese, quartz;
    
    public static final Seq<Item> proximaOreItems = new Seq<>();
    
    public static Item plutonium238BerylliumSource; // 钚238-铍中子源
    public static Item heu235UraniumFuel; // HEU-235铀燃料棒
    
    // RBMK燃料棒
    public static RBMKRodItem thoriumFuelRod;
    public static RBMKRodItem uraniumFuelRod;
    public static RBMKRodItem plutoniumFuelRod;
    public static RBMKRodItem enrichedThoriumFuelRod;
    public static RBMKRodItem enrichedUraniumFuelRod;
    public static RBMKRodItem enrichedPlutoniumFuelRod;
    

    
    public static void load(){
        iron = new Item("iron", Color.valueOf("a8a8a8")){{
            hardness = 2;
            cost = 0.8f;
        }};
        
        uranium = new Item("uranium", Color.valueOf("7fff00")){{
            hardness = 5;
            cost = 1.5f;
            radioactivity = 1.2f;
            explosiveness = 0.3f;
            healthScaling = 0.15f;
        }};
        
        manganese = new Item("manganese", Color.valueOf("555555")){{
            hardness = 4;
            cost = 1.3f;
            healthScaling = 0.7f;
        }};
        
        quartz = new Item("quartz", Color.valueOf("f0f0f0")){{
            cost = 0.9f;
        }};
        
        proximaOreItems.addAll(iron, uranium, manganese, quartz);
        
        // 钚238-铍中子源 - 深蓝色带放射性
        plutonium238BerylliumSource = new RBMKRodItem("plutonium238-beryllium-source", new Color(0.2f, 0.3f, 0.8f)){{
            yield = 0.8f;
            heat = 15f;
            selfRate = 0.3f;
            diffusion = 0.8f;
            meltingPoint = 2500f;
            isNeutronSource = true;
            enrichment = 1f;
            cost = 5000;
            radioactivity = 5f;
        }};
        
        // HEU-235铀燃料棒 - 亮绿色
        heu235UraniumFuel = new RBMKRodItem("heu235-uranium-fuel", new Color(0.3f, 0.8f, 0.2f)){{
            yield = 1f;
            heat = 20f;
            selfRate = 0.05f;
            diffusion = 1f;
            meltingPoint = 2000f;
            isNeutronSource = false;
            enrichment = 0.95f;
            cost = 3000;
            radioactivity = 3f;
        }};
        
        // RBMK燃料棒
        thoriumFuelRod = new RBMKRodItem("thorium-fuel-rod", new Color(0.8f, 0.6f, 0.2f)){{
            yield = 0.8f;
            heat = 10f;
            selfRate = 0.01f;
            diffusion = 1f;
            meltingPoint = 2000f;
            isNeutronSource = false;
            enrichment = 0.8f;
            cost = 1000;
            radioactivity = 1f;
        }};
        
        uraniumFuelRod = new RBMKRodItem("uranium-fuel-rod", new Color(0.3f, 0.8f, 0.2f)){{
            yield = 1f;
            heat = 15f;
            selfRate = 0.03f;
            diffusion = 1f;
            meltingPoint = 2000f;
            isNeutronSource = false;
            enrichment = 0.9f;
            cost = 1500;
            radioactivity = 2f;
        }};
        
        plutoniumFuelRod = new RBMKRodItem("plutonium-fuel-rod", new Color(0.8f, 0.2f, 0.2f)){{
            yield = 1.2f;
            heat = 20f;
            selfRate = 0.05f;
            diffusion = 1f;
            meltingPoint = 2000f;
            isNeutronSource = false;
            enrichment = 0.95f;
            cost = 2000;
            radioactivity = 3f;
        }};
        
        enrichedThoriumFuelRod = new RBMKRodItem("enriched-thorium-fuel-rod", new Color(0.9f, 0.7f, 0.3f)){{
            yield = 1f;
            heat = 15f;
            selfRate = 0.02f;
            diffusion = 1f;
            meltingPoint = 2000f;
            isNeutronSource = false;
            enrichment = 0.9f;
            cost = 1500;
            radioactivity = 1.5f;
        }};
        
        enrichedUraniumFuelRod = new RBMKRodItem("enriched-uranium-fuel-rod", new Color(0.4f, 0.9f, 0.3f)){{
            yield = 1.2f;
            heat = 20f;
            selfRate = 0.04f;
            diffusion = 1f;
            meltingPoint = 2000f;
            isNeutronSource = false;
            enrichment = 0.95f;
            cost = 2000;
            radioactivity = 2.5f;
        }};
        
        enrichedPlutoniumFuelRod = new RBMKRodItem("enriched-plutonium-fuel-rod", new Color(0.9f, 0.3f, 0.3f)){{
            yield = 1.5f;
            heat = 25f;
            selfRate = 0.06f;
            diffusion = 1f;
            meltingPoint = 2000f;
            isNeutronSource = true;
            enrichment = 1f;
            cost = 2500;
            radioactivity = 3.5f;
        }};
        

    }
}
