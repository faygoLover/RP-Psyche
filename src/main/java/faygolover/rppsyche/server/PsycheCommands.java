package faygolover.rppsyche.server;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import faygolover.rpcore.api.RpCoreAPI;
import faygolover.rppsyche.RpPsyche;
import faygolover.rppsyche.capability.PsycheData;
import faygolover.rppsyche.core.Psyche;
import faygolover.rppsyche.core.PsycheState;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;

import java.util.Collection;
import java.util.Locale;

/** {@code /rppsyche}: цифры психики — только ГМу (уровень прав 2). */
public final class PsycheCommands {
    private PsycheCommands() {}

    private enum Scale { STRESS, SANITY }

    public static void describe() {
        RpCoreAPI.registerCommand(RpPsyche.MODID, "/rppsyche get <игрок>", "rppsyche.cmd.get", 2, false);
        RpCoreAPI.registerCommand(RpPsyche.MODID, "/rppsyche set <игроки> stress|sanity <0–100>", "rppsyche.cmd.set", 2, false);
        RpCoreAPI.registerCommand(RpPsyche.MODID, "/rppsyche add <игроки> stress|sanity <число>", "rppsyche.cmd.add", 2, false);
    }

    public static void register(RegisterCommandsEvent e) {
        CommandDispatcher<CommandSourceStack> d = e.getDispatcher();
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("rppsyche").requires(s -> s.hasPermission(2));
        root.then(Commands.literal("get").then(Commands.argument("player", EntityArgument.player())
                .executes(PsycheCommands::get)));
        for (boolean add : new boolean[]{false, true}) {
            var branch = Commands.literal(add ? "add" : "set");
            var targets = Commands.argument("targets", EntityArgument.players());
            for (Scale scale : Scale.values()) {
                targets.then(Commands.literal(scale.name().toLowerCase(Locale.ROOT))
                        .then(Commands.argument("value", DoubleArgumentType.doubleArg(-100, 100))
                                .executes(ctx -> change(ctx, scale, add))));
            }
            root.then(branch.then(targets));
        }
        d.register(root);
    }

    private static int get(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer p = EntityArgument.getPlayer(ctx, "player");
        PsycheData data = PsycheData.get(p);
        if (data == null) return 0;
        PsycheState s = data.state;
        ctx.getSource().sendSuccess(() -> Component.translatable("rppsyche.cmd.get.result", p.getDisplayName(),
                String.format(Locale.ROOT, "%.1f", s.stress), String.format(Locale.ROOT, "%.1f", s.sanity),
                Component.translatable("rppsyche.stage." + Psyche.sanityStage(s.sanity)),
                (int) s.darkSeconds, (int) (s.aloneSeconds / 60)), false);
        return 1;
    }

    private static int change(CommandContext<CommandSourceStack> ctx, Scale scale, boolean add) throws CommandSyntaxException {
        Collection<ServerPlayer> players = EntityArgument.getPlayers(ctx, "targets");
        double value = DoubleArgumentType.getDouble(ctx, "value");
        int n = 0;
        for (ServerPlayer p : players) {
            PsycheData data = PsycheData.get(p);
            if (data == null) continue;
            PsycheState s = data.state;
            if (scale == Scale.STRESS) {
                s.stress = Psyche.clamp(add ? s.stress + value : value);
                s.pending = 0;
            } else {
                s.sanity = Psyche.clamp(add ? s.sanity + value : value);
            }
            PsycheService.resync(p);
            n++;
        }
        int count = n;
        ctx.getSource().sendSuccess(() -> Component.translatable("rppsyche.cmd.changed", count), true);
        return n;
    }
}
