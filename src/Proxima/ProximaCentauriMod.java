package Proxima;

import Proxima.effects.SpecialDeathEffects;
import Proxima.liquids.ProximaLiquids;
import Proxima.special.SpecialContent;
import arc.*;
import arc.graphics.g2d.*;
import arc.input.*;
import arc.math.*;
import arc.math.geom.*;
import Proxima.block.*;
import Proxima.items.*;
import arc.util.Log;
import arc.util.Time;
import mindustry.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.mod.*;
import mindustry.ui.dialogs.BaseDialog;

public class ProximaCentauriMod extends Mod{

    public ProximaCentauriMod(){
        Log.info("Loaded ProximaCentauriMod constructor.");

        //listen for game load event
        Events.on(ClientLoadEvent.class, e -> {
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
        ProximaBlocks.load();
    }

}
