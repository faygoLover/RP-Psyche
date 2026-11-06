package faygolover.rppsyche.server;

import faygolover.rpcore.api.RpCoreAPI;
import faygolover.rpcore.api.RpIds;
import faygolover.rppsyche.capability.PsycheData;
import faygolover.rppsyche.core.Settings;
import faygolover.rppsyche.item.PsychologistJournalItem;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.OptionalInt;
import java.util.UUID;

/**
 * Беседа с психологом (журнал психолога). Держать ничего не нужно: идёт {@link Settings#therapySeconds} секунд, раз в
 * {@link Settings#therapyCheckSeconds} проверяется, что оба рядом; разошлись — беседа сорвалась. Итог — рассудок и
 * стресс пациента. Ход виден обоим в строке над хотбаром.
 */
public final class Therapy {
    private Therapy() {}

    private static final class Session {
        final UUID doctor, patient;
        final long start;
        long nextCheck;

        Session(UUID doctor, UUID patient, long start) {
            this.doctor = doctor;
            this.patient = patient;
            this.start = start;
        }
    }

    /** Беседы по врачу. */
    private static final Map<UUID, Session> SESSIONS = new HashMap<>();

    public static void start(ServerPlayer doctor, ServerPlayer patient, ItemStack journal) {
        Settings c = Settings.get();
        if (doctor == patient) return;
        if (busy(doctor.getUUID()) || busy(patient.getUUID())) {
            say(doctor, "rppsyche.therapy.busy");
            return;
        }
        if (RpCoreAPI.isSkillManaged(RpIds.MEDICINE)) {
            OptionalInt lv = RpCoreAPI.skill(doctor, RpIds.MEDICINE);
            if (lv.isPresent() && lv.getAsInt() < c.therapySkill) {
                say(doctor, "rppsyche.therapy.skill", c.therapySkill);
                return;
            }
        }
        long now = doctor.level().getGameTime();
        long ready = journal.hasTag() ? journal.getTag().getLong(PsychologistJournalItem.READY) : 0;
        if (now < ready) {
            say(doctor, "rppsyche.therapy.journal_cooldown", (ready - now + 1199) / 1200);
            return;
        }
        PsycheData pd = PsycheData.get(patient);
        if (pd == null) return;
        long day = patient.level().getDayTime();
        long last = pd.state.therapyAt;
        if (last != Long.MIN_VALUE && day >= last && day - last < c.therapyPatientTicks) {
            say(doctor, "rppsyche.therapy.patient_cooldown", patient.getDisplayName());
            return;
        }
        journal.getOrCreateTag().putLong(PsychologistJournalItem.READY, now + (long) (c.therapyJournalCooldown * 20));
        Session s = new Session(doctor.getUUID(), patient.getUUID(), now);
        s.nextCheck = now + (long) (c.therapyCheckSeconds * 20);
        SESSIONS.put(doctor.getUUID(), s);
        say(doctor, "rppsyche.therapy.started", patient.getDisplayName());
        say(patient, "rppsyche.therapy.started_patient", doctor.getDisplayName());
    }

    private static boolean busy(UUID id) {
        for (Session s : SESSIONS.values()) if (s.doctor.equals(id) || s.patient.equals(id)) return true;
        return false;
    }

    public static void onServerTick(TickEvent.ServerTickEvent e) {
        if (e.phase != TickEvent.Phase.END || SESSIONS.isEmpty()) return;
        MinecraftServer server = e.getServer();
        if (server.getTickCount() % 20 != 0) return;
        Settings c = Settings.get();
        for (Iterator<Session> it = SESSIONS.values().iterator(); it.hasNext(); ) {
            Session s = it.next();
            ServerPlayer doc = server.getPlayerList().getPlayer(s.doctor);
            ServerPlayer pat = server.getPlayerList().getPlayer(s.patient);
            if (doc == null || pat == null || !doc.isAlive() || !pat.isAlive() || doc.level() != pat.level()) {
                if (doc != null) say(doc, "rppsyche.therapy.broken");
                if (pat != null) say(pat, "rppsyche.therapy.broken");
                it.remove();
                continue;
            }
            long now = doc.level().getGameTime();
            if (now >= s.nextCheck) {
                s.nextCheck = now + (long) (c.therapyCheckSeconds * 20);
                if (doc.distanceToSqr(pat) > c.therapyRadius * c.therapyRadius) {
                    say(doc, "rppsyche.therapy.broken");
                    say(pat, "rppsyche.therapy.broken");
                    it.remove();
                    continue;
                }
            }
            double done = (now - s.start) / 20.0;
            if (done >= c.therapySeconds) {
                PsycheData pd = PsycheData.get(pat);
                if (pd != null) {
                    // Заключение — только врачу, в чат: записать в медкарту.
                    doc.sendSystemMessage(Component.translatable(PsycheStatus.conclusion(pd.state), pat.getDisplayName())
                            .withStyle(ChatFormatting.AQUA));
                    pd.sanityBoost += c.therapySanity;
                    pd.relief += c.therapyRelief;
                    pd.state.therapyAt = pat.level().getDayTime();
                }
                say(doc, "rppsyche.therapy.done");
                say(pat, "rppsyche.therapy.done_patient");
                it.remove();
                continue;
            }
            Component bar = progress(done / c.therapySeconds, c.therapySeconds - done);
            doc.displayClientMessage(bar, true);
            pat.displayClientMessage(bar, true);
        }
    }

    /** «Беседа ▮▮▮▮▯▯▯▯▯▯ 1:40». */
    private static Component progress(double share, double left) {
        int n = (int) Math.round(Math.max(0, Math.min(1, share)) * 10);
        int sec = (int) Math.ceil(Math.max(0, left));
        String bar = "▮".repeat(n) + "▯".repeat(10 - n);
        return Component.translatable("rppsyche.therapy.progress", bar, String.format("%d:%02d", sec / 60, sec % 60))
                .withStyle(ChatFormatting.AQUA);
    }

    private static void say(ServerPlayer p, String key, Object... args) {
        p.displayClientMessage(Component.translatable(key, args).withStyle(ChatFormatting.AQUA), true);
    }

    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        UUID id = e.getEntity().getUUID();
        SESSIONS.values().removeIf(s -> s.doctor.equals(id) || s.patient.equals(id));
    }

    public static void clear() {
        SESSIONS.clear();
    }
}
