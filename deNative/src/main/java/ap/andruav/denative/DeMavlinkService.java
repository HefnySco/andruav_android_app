package ap.andruav.denative;

import java.io.File;

import ap.andruav.denative.fclink.FcLinkBridge;

public class DeMavlinkService extends DeModuleService {

    @Override
    protected String getModuleName() {
        return "de_mavlink";
    }

    @Override
    protected String[] getModuleArgv() {
        return new String[]{
                "de_mavlink",
                "-c", "de_mavlink.config.module.json",
                "-b", "de_mavlink.local"
        };
    }

    @Override
    protected void onModuleStarting(File workDir) {
        FcLinkBridge.start(this);
    }
}
