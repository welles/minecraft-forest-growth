package dev.welles.forestgrowth.mixin;

import dev.welles.forestgrowth.SeedDispersal;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Natural leaves drop seeds from time to time.
 * <p>
 * Vanilla only random-ticks leaves that are about to decay, so all natural (non-persistent) leaves are made
 * random-ticking. The vanilla decay logic is unaffected: it only acts on leaves without a connected log.
 */
@Mixin(LeavesBlock.class)
public abstract class LeavesBlockMixin {
    @Inject(method = "isRandomlyTicking", at = @At("HEAD"), cancellable = true)
    private void forestgrowth$tickNaturalLeaves(BlockState state, CallbackInfoReturnable<Boolean> cir) {
        if (!state.getValue(LeavesBlock.PERSISTENT)) cir.setReturnValue(true);
    }

    @Inject(method = "randomTick", at = @At("HEAD"))
    private void forestgrowth$dropSeed(BlockState state, ServerLevel level, BlockPos pos, RandomSource random,
                                       CallbackInfo ci) {
        SeedDispersal.onRandomTick(state, level, pos, random);
    }
}
