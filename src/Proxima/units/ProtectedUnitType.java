package Proxima.units;

import Proxima.*;
import Proxima.effects.*;
import mindustry.*;
import mindustry.gen.*;
import mindustry.type.*;
import mindustry.world.meta.*;

public class ProtectedUnitType extends UnitType{

    public ProtectedUnitType(String name){
        super(name);
        flying = true;
        hitSize = 7;
        drag = 0.07f;

        health = 100f;

        outlines = false;
        drawCell = false;
        bounded = false;

        createScorch = false;
        hidden = true;

        engineSize = -1f;

        controller = u -> new NullAI();

        envEnabled = Env.any;
        envDisabled = 0;

        // 修复1: 使用正确的构造函数
        constructor = ProtectedUnit::createUnit;

        // 修复2: 设置死亡音效，避免空指针
        deathSound = ProximaSounds.expDecoy;
        deathSoundVolume = 0.5f;

        deathExplosionEffect = ProximaFX.empathyDecoyDestroy;
    }

    @Override
    public void init(){
        super.init();
        for(StatusEffect s : Vars.content.statusEffects()){
            immunities.add(s);
        }
    }

    @Override
    public void load(){
        super.load();
        ProtectedRegions.load();
    }

    @Override
    public void update(Unit unit){
        if(!(unit instanceof ProtectedUnit)){
            // 修复3: 不要直接 destroy，而是设置血量归零后自然移除
            unit.health = 0;
            unit.remove();
            return;
        }
        super.update(unit);
    }
}