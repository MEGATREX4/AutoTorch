package com.megatrex4;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.block.Block;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Direction;
import net.minecraft.util.hit.BlockHitResult;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigHolder;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.minecraft.world.LightType;
import org.lwjgl.glfw.GLFW;

import static org.apache.logging.log4j.LogManager.getLogger;

@Environment(EnvType.CLIENT)
public class AutoTorch implements ModInitializer {

	public static final String MOD_ID = "autotorch";

	private static final org.apache.logging.log4j.Logger LOGGER = getLogger(MOD_ID);

	private MinecraftClient client;
	public ConfigHolder<AutoTorchConfig> CONFIG;
	private AutoTorchConfig AUTOTORCH;
	boolean enabled = false;

	private static final KeyBinding AutoPlaceBinding = KeyBindingHelper.registerKeyBinding(
			new KeyBinding(
					"autotorch.keybinding",
					InputUtil.Type.KEYSYM,
					GLFW.GLFW_KEY_Y,
					"autotorch.category"
			)
	);

	@Override
	public void onInitialize() {
		LOGGER.info("AutoTorch initialized.");

		this.client = MinecraftClient.getInstance();
		CONFIG = AutoConfig.register(AutoTorchConfig.class, GsonConfigSerializer::new);
		ClientTickEvents.END_CLIENT_TICK.register(this::tick);
		AUTOTORCH = CONFIG.getConfig();
		CONFIG.registerLoadListener((manager, data) -> {
			AUTOTORCH = data;
			return ActionResult.SUCCESS;
		});
	}

	public void tick(MinecraftClient client) {
		if (client.player != null && client.world != null) {
			if (AutoPlaceBinding.wasPressed()) {
				enabled = !enabled;
				var msg = enabled
						? Text.translatable("autotorch.message.enabled")
						: Text.translatable("autotorch.message.disabled");
				client.player.sendMessage(msg, true);
			}

			if (!enabled) return;

			BlockPos playerBlock = client.player.getBlockPos();
			if (client.world.getLightLevel(LightType.BLOCK, playerBlock) < AUTOTORCH.getLightLevel()
					&& canPlaceTorch(playerBlock)) {
				Item torchItem = findTorchInInventory();
				if (torchItem != null) {
					placeTorch(playerBlock);
				}
			}
		}
	}

	private Item findTorchInInventory() {
		for (int i = 0; i < client.player.getInventory().size(); i++) {
			ItemStack stack = client.player.getInventory().getStack(i);
			if (isValidTorch(stack.getItem())) {
				return stack.getItem();
			}
		}
		return null;
	}

	private boolean isValidTorch(Item item) {
		return AUTOTORCH.getTorches().contains(Registries.ITEM.getId(item).toString());
	}

	private void placeTorch(BlockPos pos) {
		if (!client.player.isSpectator()) {
			ItemStack torchStack = getTorchStackFromInventory();
			if (torchStack != null && torchStack.getCount() > 0) {
				ItemStack mainHandItem = client.player.getStackInHand(Hand.MAIN_HAND);
				ItemStack offHandItem = client.player.getStackInHand(Hand.OFF_HAND);

				if (mainHandItem.getItem() == torchStack.getItem()) {
					interactWithBlock(pos, Hand.MAIN_HAND);
				} else if (offHandItem.getItem() == torchStack.getItem()) {
					interactWithBlock(pos, Hand.OFF_HAND);
				}

				if (!client.player.isCreative()) {
					torchStack.decrement(1);
				}
			}
		}
	}

	private ItemStack getTorchStackFromInventory() {
		for (int i = 0; i < client.player.getInventory().size(); i++) {
			ItemStack stack = client.player.getInventory().getStack(i);
			if (isValidTorch(stack.getItem())) {
				return stack;
			}
		}
		return null;
	}

	private void interactWithBlock(BlockPos pos, Hand hand) {
		Vec3d hitVec = Vec3d.ofBottomCenter(pos);
		BlockHitResult blockHitResult = new BlockHitResult(hitVec, Direction.DOWN, pos, false);

		client.interactionManager.interactBlock(client.player, hand, blockHitResult);
	}

	public boolean canPlaceTorch(BlockPos pos) {
		return (client.world.getBlockState(pos).getFluidState().isEmpty() &&
				Block.sideCoversSmallSquare(client.world, pos.down(), Direction.UP));
	}
}
