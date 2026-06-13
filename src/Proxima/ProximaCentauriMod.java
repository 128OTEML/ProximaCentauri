package Proxima;

import Proxima.content.ProximaPlanets;
import Proxima.content.ProximaTechTree;
import Proxima.content.ProximaUnitTypes;
import Proxima.effects.SpecialDeathEffects;
import Proxima.liquids.ProximaLiquids;
import Proxima.special.SpecialContent;
import Proxima.input.ProximaInputHandler;
import arc.*;
import Proxima.block.*;
import Proxima.items.*;
import arc.util.Log;
import arc.util.Time;
import mindustry.*;
import mindustry.game.EventType.*;
import mindustry.input.DesktopInput;
import mindustry.mod.*;
import mindustry.ui.dialogs.BaseDialog;

public class ProximaCentauriMod extends Mod{

    public ProximaCentauriMod(){
        Log.info("Loaded ProximaCentauriMod constructor.");

        //listen for game load event
        Events.on(ClientLoadEvent.class, e -> {
            // 替换输入处理器以支持16方向箭头绘制
            if (Vars.control != null && Vars.control.input instanceof DesktopInput) {
                ProximaInputHandler proximaInput = new ProximaInputHandler();
                proximaInput.block = Vars.control.input.block;
                Vars.control.input = proximaInput;
            }

            //show dialog upon startup
            Time.runTask(10f, () -> {
                BaseDialog dialog = new BaseDialog("frog");
                dialog.cont.add("behold").row();
                //mod sprites are prefixed with the mod name (this mod is called 'example-java-mod' in its config)
                dialog.cont.image(Core.atlas.find("example-java-mod-frog")).pad(20f).row();
                dialog.cont.button("I see", dialog::hide).size(100f, 50f);
                dialog.show();
            });
        });
    }

    @Override
    public void loadContent(){
        SpecialDeathEffects.load();
        SpecialContent.load();
        ProximaItems.load();
        ProximaLiquids.load();
        ProximaUnitTypes.load();
        ProximaBlocks.load();
        ProximaPlanets.load();
        ProximaTechTree.load();
    }
}
