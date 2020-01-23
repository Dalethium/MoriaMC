package net.fabricmc.example.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.fabricmc.example.ExampleMod;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin extends LivingEntity {

	protected PlayerEntityMixin(EntityType<? extends LivingEntity> type, World world) {
		super(type, world);
	}

	@Inject(method = "tick()V", at = @At("HEAD"))
	  private void tick(CallbackInfo info) {
	    if (!this.world.isClient) {
	      return;
	    }
	    if (ExampleMod.active && !ExampleMod.d) {
	    	new Thread() {
	    	
	    	 public void run() {
	    		if (ExampleMod.hasTorch() != -1) ExampleMod.checkAround((int) Math.round(ExampleMod.MC.player.getPos().x), (int) Math.round(ExampleMod.MC.player.getPos().y), (int) Math.round(ExampleMod.MC.player.getPos().z));
	    	}
	    		}.start();;
	    }
	  }

}
