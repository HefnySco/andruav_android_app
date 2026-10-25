package ap.andruav.denative;

public class DeCommService extends DeModuleService {

    @Override
    protected String getModuleName() {
        return "de_comm";
    }

    @Override
    protected String[] getModuleArgv() {
        return new String[]{
                "de_comm",
                "-c", "de_comm.config.module.json",
                "-b", "de_comm.local"
        };
    }
}
