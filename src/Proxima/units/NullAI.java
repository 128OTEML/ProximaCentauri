package Proxima.units;

import mindustry.entities.units.*;
import mindustry.gen.*;

public class NullAI implements UnitController{
    protected Unit unit;

    @Override
    public void unit(Unit unit){
        this.unit = unit;
    }

    @Override
    public Unit unit(){
        return unit;
    }

    // 添加必要的方法实现
    @Override
    public void updateUnit(){
        // 空实现
    }

    public void removed(){
        // 空实现
    }

    public boolean isAI(){
        return false;
    }
}