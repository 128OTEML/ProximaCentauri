package Proxima.units;

import mindustry.gen.*;

public class AttackAI extends ProtectedAI{
    AttackAI(){
        attack = true;
    }

    @Override
    Teamc getTarget(){
        return unit.getTarget();
    }
}
