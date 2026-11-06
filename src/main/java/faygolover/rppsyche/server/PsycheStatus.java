package faygolover.rppsyche.server;

import faygolover.rpcore.api.StatusLines;
import faygolover.rppsyche.capability.PsycheData;
import faygolover.rppsyche.core.Psyche;
import faygolover.rppsyche.core.PsycheState;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.List;

/**
 * Психика в осмотре RP Medicine (строки RP Core). Сам — ощущениями, без названия стадии; другой при осмотре — только
 * то, что видно со стороны (дыхание, руки, взгляд, речь); ГМ сканером и в панели — цифры и стадия. Шкал нет нигде.
 */
public final class PsycheStatus implements StatusLines.Source {
    public static final PsycheStatus INSTANCE = new PsycheStatus();

    private PsycheStatus() {}

    @Override
    public void lines(Player target, StatusLines.Viewer viewer, List<StatusLines.Line> out) {
        if (!(target instanceof ServerPlayer sp)) return;
        PsycheData d = PsycheData.get(sp);
        if (d == null) return;
        PsycheState s = d.state;
        int stage = Psyche.sanityStage(s.sanity);
        switch (viewer) {
            case SELF -> {
                if (!PsycheService.affected(sp) || PsycheService.paused(d)) return;
                if (s.stress >= 55) out.add(new StatusLines.Line("rppsyche.status.self_anxious"));
                if (stage > 0) out.add(new StatusLines.Line("rppsyche.status.self_stage" + stage));
            }
            case OTHER -> {
                if (!PsycheService.affected(sp) || PsycheService.paused(d)) return;
                if (s.stress >= 80) out.add(new StatusLines.Line("rppsyche.status.other_panic"));
                else if (s.stress >= 65) out.add(new StatusLines.Line("rppsyche.status.other_tremor"));
                else if (s.stress >= 55) out.add(new StatusLines.Line("rppsyche.status.other_anxious"));
                // Надломленного со стороны не видно.
                if (stage >= 2) out.add(new StatusLines.Line("rppsyche.status.other_stage" + stage));
            }
            case GM -> {
                out.add(new StatusLines.Line("rppsyche.status.gm", (int) Math.round(s.stress), (int) Math.round(s.sanity)));
                out.add(new StatusLines.Line("rppsyche.status.gm_stage" + stage));
                if (PsycheService.paused(d)) out.add(new StatusLines.Line("rppsyche.status.gm_paused"));
            }
        }
    }

    /** Заключение психолога после беседы — по тому, с чем пациент пришёл. */
    public static String conclusion(PsycheState s) {
        int stage = Psyche.sanityStage(s.sanity);
        if (stage == 0) return s.stress >= 55 ? "rppsyche.therapy.conclusion_anxious" : "rppsyche.therapy.conclusion_0";
        return "rppsyche.therapy.conclusion_" + stage;
    }
}
