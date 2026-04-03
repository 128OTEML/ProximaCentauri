package IcePeak.unit.chordon;

import mindustry.gen.*;

public class AttackAI extends ChordonAI{
    AttackAI(){
        attack = true;
    }

    @Override
    Teamc getTarget(){
        return unit.getTarget();
    }
}
