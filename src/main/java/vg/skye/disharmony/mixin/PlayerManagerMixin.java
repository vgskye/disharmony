package vg.skye.disharmony.mixin;

import net.minecraft.network.ClientConnection;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import vg.skye.disharmony.Disharmony;

import java.util.List;

@Mixin(PlayerManager.class)
public final class PlayerManagerMixin {
    @Shadow @Final private List<ServerPlayerEntity> players;

    @Inject(method = "onPlayerConnect", at = @At("TAIL"))
    private void onConnect(ClientConnection connection, ServerPlayerEntity player, CallbackInfo ci) {
        try {
            Disharmony.INSTANCE.onPlayerCountChange(this.players.size());
        } catch (Exception ignored) {}
    }

    @Inject(method = "remove", at = @At("TAIL"))
    private void onDisconnect(ServerPlayerEntity player, CallbackInfo ci) {
        try {
            Disharmony.INSTANCE.onPlayerCountChange(this.players.size());
        } catch (Exception ignored) {}
    }
}
