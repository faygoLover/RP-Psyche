package faygolover.rppsyche.capability;

import faygolover.rppsyche.RpPsyche;
import faygolover.rppsyche.core.PsycheState;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;

/** Психика игрока (сохраняется) и служебное состояние сервера (не сохраняется). */
public final class PsycheData {
    public static final Capability<PsycheData> CAP = CapabilityManager.get(new CapabilityToken<>() {});
    public static final ResourceLocation ID = new ResourceLocation(RpPsyche.MODID, "psyche");

    public final PsycheState state = new PsycheState();

    // ---- служебное (сервер)
    /** Толчки стресса, пришедшие событиями с прошлой секунды; облегчение; разовые удары по рассудку. */
    public double impulse;
    public double relief;
    public double sanityHit;
    public double sanityBoost;
    /** Был в клинической смерти (чтобы заметить возвращение). */
    public boolean wasClinical;
    /** Последнее отправленное клиенту. */
    public float sentStress = -1, sentSanity = -1;
    public int sentFlags = -1;
    public int syncAge;
    /** Секунды до следующей отправки ощущений мода тела. */
    public int sensationTimer;
    /** Голос: идёт фраза; её кто-то слышал; когда (по часам психики) кончились неуслышанные фразы. */
    public boolean inUtterance;
    public boolean utteranceHeard;
    public final ArrayDeque<Double> unheard = new ArrayDeque<>();

    @Nullable
    public static PsycheData get(Player p) {
        return p.getCapability(CAP).resolve().orElse(null);
    }

    public CompoundTag save() {
        PsycheState s = state;
        CompoundTag t = new CompoundTag();
        t.putDouble("stress", s.stress);
        t.putDouble("sanity", s.sanity);
        t.putDouble("pending", s.pending);
        t.putDouble("dark", s.darkSeconds);
        t.putDouble("alone", s.aloneSeconds);
        t.putDouble("clock", s.clock);
        t.putDouble("slept_at", s.sleptAt);
        t.putDouble("ate_at", s.ateAt);
        t.putDouble("panic_at", s.panicAt);
        t.putDouble("self_talk_at", s.selfTalkAt);
        t.putBoolean("was_clinical", wasClinical);
        return t;
    }

    public void load(CompoundTag t) {
        PsycheState s = state;
        if (t.contains("stress")) s.stress = t.getDouble("stress");
        if (t.contains("sanity")) s.sanity = t.getDouble("sanity");
        s.pending = t.getDouble("pending");
        s.darkSeconds = t.getDouble("dark");
        s.aloneSeconds = t.getDouble("alone");
        s.clock = t.getDouble("clock");
        if (t.contains("slept_at")) s.sleptAt = t.getDouble("slept_at");
        if (t.contains("ate_at")) s.ateAt = t.getDouble("ate_at");
        if (t.contains("panic_at")) s.panicAt = t.getDouble("panic_at");
        if (t.contains("self_talk_at")) s.selfTalkAt = t.getDouble("self_talk_at");
        wasClinical = t.getBoolean("was_clinical");
    }

    public static final class Provider implements ICapabilitySerializable<CompoundTag> {
        private final PsycheData data = new PsycheData();
        private final LazyOptional<PsycheData> opt = LazyOptional.of(() -> data);

        @Override
        public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
            return CAP.orEmpty(cap, opt);
        }

        @Override
        public CompoundTag serializeNBT() {
            return data.save();
        }

        @Override
        public void deserializeNBT(CompoundTag nbt) {
            data.load(nbt);
        }
    }
}
