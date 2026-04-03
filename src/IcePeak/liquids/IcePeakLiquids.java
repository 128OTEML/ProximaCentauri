package IcePeak.liquids;

import arc.graphics.Color;
import mindustry.content.StatusEffects;
import mindustry.type.Liquid;

/**
 * IcePeak模组液体定义
 */
public class IcePeakLiquids{
    public static Liquid steam;

    public static void load(){
        // 蒸汽 - 用于RBMK反应堆换热
        steam = new Liquid("steam", Color.valueOf("dddddd")){{
            gas = true; // 是气体
            temperature = 1.5f; // 高温
            heatCapacity = 0.8f; // 热容量
            viscosity = 0.1f; // 低粘度
            explosiveness = 0.1f; // 轻微爆炸性
            effect = StatusEffects.wet; // 效果：湿润
        }};
    }
}
