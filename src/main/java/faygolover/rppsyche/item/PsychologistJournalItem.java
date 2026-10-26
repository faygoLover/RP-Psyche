package faygolover.rppsyche.item;

import faygolover.rppsyche.server.Therapy;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Журнал психолога: ПКМ по игроку — беседа (3 минуты рядом), после неё рассудок пациента +, стресс −. Не тратится;
 * откат у журнала (в NBT, тиков мира) и у пациента (12 RP-часов). Пользоваться — от «Медицины» 4 (RP Perks).
 */
public class PsychologistJournalItem extends Item {
    /** Тик мира, с которого журналом снова можно пользоваться. */
    public static final String READY = "rppsyche_ready";

    public PsychologistJournalItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player user, LivingEntity target, InteractionHand hand) {
        if (!(target instanceof Player)) return InteractionResult.PASS;
        if (user instanceof ServerPlayer doctor && target instanceof ServerPlayer patient) Therapy.start(doctor, patient, stack);
        return InteractionResult.sidedSuccess(user.level().isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.rppsyche.psychologist_journal.desc").withStyle(ChatFormatting.GRAY));
        if (level == null || !stack.hasTag()) return;
        long left = stack.getTag().getLong(READY) - level.getGameTime();
        if (left > 0) tooltip.add(Component.translatable("item.rppsyche.psychologist_journal.cooldown",
                (left + 1199) / 1200).withStyle(ChatFormatting.DARK_GRAY));
    }
}
