package net.fabricmc.example;

import org.lwjgl.glfw.GLFW;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.keybinding.FabricKeyBinding;
import net.fabricmc.fabric.api.client.keybinding.KeyBindingRegistry;
import net.fabricmc.fabric.api.event.client.ClientTickCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.packet.PlayerMoveC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RayTraceContext;

public class ExampleMod implements ModInitializer {
	private static FabricKeyBinding keyBinding;
	public  static boolean active = false;

	private static int radius = 5;
	public static final MinecraftClient MC = MinecraftClient.getInstance();
	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.
		keyBinding = FabricKeyBinding.Builder.create(
			    new Identifier("key", "autoplacetorch"),
			    net.minecraft.client.util.InputUtil.Type.KEYSYM,
			    GLFW.GLFW_KEY_R,
			    "Torchifier"
			).build();
		KeyBindingRegistry.INSTANCE.register(keyBinding);
		ClientTickCallback.EVENT.register(e ->
		{
			if(keyBinding.isPressed()) active = !active;
		});
		System.out.println("Hello Fabric world!");
	}
	  public static int hasTorch() {
		    for (int slot = 0; slot <= 8; slot++) {
		      if (isTorchItem((ItemStack)MC.player.inventory.getInvStack(slot)))
		        return slot; 
		    } 
		    return -1;
		  }
		  
		  private static boolean isTorchItem(ItemStack candidate) {
		    if (candidate.getItem().equals(Items.TORCH))
		      return true; 
		    return false;
		  }
	public static void checkAround(int x, int y, int z) {
		for (int xx = -radius; xx <= radius; xx++) {
			for (int yy = -radius; yy <= radius; yy++) {
				for (int zz = -radius; zz <= radius; zz++) {
					BlockPos pos = new BlockPos(xx + x, yy + y, zz + z);
					int light = MC.world.getLightLevel(net.minecraft.world.LightType.BLOCK, pos);
					if (light <= 7) {
						if (placeTorch(new BlockPos(xx + x, yy + y, zz + z))) return;
					}
				}
			}
		}
	}

	public static Vec3d getEyesPos() {
		ClientPlayerEntity player = MC.player;

		return new Vec3d(player.getX(), player.getY() + player.getEyeHeight(player.getPose()), player.getZ());
	}

	public static Rotation getNeededRotations(Vec3d vec) {
		Vec3d eyesPos = getEyesPos();

		double diffX = vec.x - eyesPos.x;
		double diffY = vec.y - eyesPos.y;
		double diffZ = vec.z - eyesPos.z;

		double diffXZ = Math.sqrt(diffX * diffX + diffZ * diffZ);

		float yaw = (float) Math.toDegrees(Math.atan2(diffZ, diffX)) - 90F;
		float pitch = (float) -Math.toDegrees(Math.atan2(diffY, diffXZ));

		return new Rotation(yaw, pitch);
	}

	public static final class Rotation {
		private final float yaw;
		private final float pitch;

		public Rotation(float yaw, float pitch) {
			this.yaw = MathHelper.wrapDegrees(yaw);
			this.pitch = MathHelper.wrapDegrees(pitch);
		}

		public float getYaw() {
			return yaw;
		}

		public float getPitch() {
			return pitch;
		}
	}

	public static boolean placeTorch(BlockPos pos) {
		Vec3d posVec = new Vec3d(pos).add(0.5, 0.5, 0.5);
		Vec3d eyesPos = getEyesPos();
		double distanceSqPosVec = eyesPos.squaredDistanceTo(posVec);
		double rangeSq = Math.pow(5, 2);
		for (Direction side : Direction.values()) {
			BlockPos neighbor = pos.offset(side);

			// check if neighbor can be right clicked
			if (!BlockUtils.canBeClicked(neighbor) || BlockUtils.getState(neighbor).getMaterial().isReplaceable())
				continue;

			Vec3d dirVec = new Vec3d(side.getVector());
			Vec3d hitVec = posVec.add(dirVec.multiply(0.5));

			// check if hitVec is within range
			if (eyesPos.squaredDistanceTo(hitVec) > rangeSq)
				continue;

			// check if side is visible (facing away from player)
			if (distanceSqPosVec > eyesPos.squaredDistanceTo(posVec.add(dirVec)))
				continue;

			// check line of sight
			if (MC.world.rayTrace(new RayTraceContext(getEyesPos(), hitVec, RayTraceContext.ShapeType.COLLIDER, RayTraceContext.FluidHandling.NONE, MC.player)).getType() != HitResult.Type.MISS)
				continue;

			// face block
			Rotation rotation = getNeededRotations(hitVec);
			PlayerMoveC2SPacket.LookOnly packet = new PlayerMoveC2SPacket.LookOnly(rotation.getYaw(), rotation.getPitch(), MC.player.onGround);
			MC.player.networkHandler.sendPacket(packet);

			// place block
			int s = MC.player.inventory.selectedSlot;
			MC.player.inventory.selectedSlot = hasTorch();
			MC.interactionManager.interactBlock(MC.player, MC.world, Hand.MAIN_HAND, new BlockHitResult(hitVec, side, pos, false));
			MC.interactionManager.interactItem(MC.player, MC.world, Hand.MAIN_HAND);
			//MC.player.swingHand(Hand.MAIN_HAND);
			MC.player.inventory.selectedSlot = s;
			return true;
		}
		return false;
	}
}
