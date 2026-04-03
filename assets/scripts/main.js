Vars.maxSchematicSize = 128;
if (!Vars.headless) {
	Vars.renderer.minZoom = 0.1;
	Vars.renderer.maxZoom = 256;
}
MapResizeDialog.minSize = 1
MapResizeDialog.maxSize = 1200

require("icepeak/lib");
require("icepeak/0m-00-puller");
require("icepeak/0m-01-pusher");
require("icepeak/chordon");

require("multi-crafter");
require("hud");
require("sectorSize");