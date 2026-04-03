package IcePeak.unit.chordon;

import IcePeak.FlameSounds;
import IcePeak.effects.FlameFX;
import IcePeak.unit.NullAI;
import mindustry.Vars;
import mindustry.gen.Unit;
import mindustry.gen.UnitEntity;
import mindustry.type.StatusEffect;
import mindustry.type.UnitType;
import mindustry.world.meta.Env;

public class ChordonUnitType extends UnitType{
    public ChordonUnitType(String name){
        super(name);
        flying = true;
        hitSize = 7;
        drag = 0.07f;

        health = 100f;

        outlines = false;
        drawCell = false;
        bounded = false;

        createScorch = false;
        hidden = false;

        engineSize = -1f;

        controller = u -> new NullAI();

        envEnabled = Env.any;
        envDisabled = 0;
        constructor = UnitEntity::create;

        deathExplosionEffect = FlameFX.empathyDecoyDestroy;
        deathSound = FlameSounds.expDecoy;

        description = """

                """;
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
        ChordonRegions.load();
    }

    @Override
    public void update(Unit unit){
        if(!(unit instanceof ChordonUnit)){
            return;
        }
        super.update(unit);
    }
}
