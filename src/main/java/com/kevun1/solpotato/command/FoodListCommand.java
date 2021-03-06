package com.kevun1.solpotato.command;

import com.kevun1.solpotato.SOLPotato;
import com.kevun1.solpotato.lib.Localization;
import com.kevun1.solpotato.tracking.*;
import com.kevun1.solpotato.tracking.benefits.BenefitsHandler;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.CommandException;
import net.minecraft.command.CommandSource;
import net.minecraft.command.arguments.EntityArgument;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Food;
import net.minecraft.util.text.*;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Objects;

import static net.minecraft.command.Commands.argument;
import static net.minecraft.command.Commands.literal;

@Mod.EventBusSubscriber(modid = SOLPotato.MOD_ID)
public final class FoodListCommand {
	private static final String name = "solpotato";
	
	@SubscribeEvent
	public static void register(RegisterCommandsEvent event) {
		event.getDispatcher().register(
			literal(name)
				.then(withPlayerArgumentOrSender(literal("sync"), FoodListCommand::syncFoodList))
				.then(withPlayerArgumentOrSender(literal("clear"), FoodListCommand::clearFoodList))
				.then(withPlayerArgumentOrSender(literal("diversity"), FoodListCommand::displayDiversity))
		);
	}
	
	@FunctionalInterface
	private interface CommandWithPlayer {
		int run(CommandContext<CommandSource> context, PlayerEntity target) throws CommandSyntaxException;
	}
	
	static ArgumentBuilder<CommandSource, ?> withPlayerArgumentOrSender(ArgumentBuilder<CommandSource, ?> base, CommandWithPlayer command) {
		String target = "target";
		return base
			.executes((context) -> command.run(context, context.getSource().asPlayer()))
			.then(argument(target, EntityArgument.player())
				.executes((context) -> command.run(context, EntityArgument.getPlayer(context, target)))
			);
	}

	static int displayDiversity(CommandContext<CommandSource> context, PlayerEntity target) {
		boolean isOp = context.getSource().hasPermissionLevel(2);
		boolean isTargetingSelf = isTargetingSelf(context, target);
		if (!isOp && !isTargetingSelf)
			throw new CommandException(localizedComponent("no_permissions"));

		double diversity = FoodList.get(target).foodDiversity();
		IFormattableTextComponent feedback = localizedComponent("diversity_feedback", diversity);
		sendFeedback(context.getSource(), feedback);
		return Command.SINGLE_SUCCESS;
	}
	
	static int syncFoodList(CommandContext<CommandSource> context, PlayerEntity target) {
		CapabilityHandler.syncFoodList(target);
		
		sendFeedback(context.getSource(), localizedComponent("sync.success"));
		System.out.println(target.getMaxHealth());
		return Command.SINGLE_SUCCESS;
	}
	
	static int clearFoodList(CommandContext<CommandSource> context, PlayerEntity target) {
		boolean isOp = context.getSource().hasPermissionLevel(2);
		boolean isTargetingSelf = isTargetingSelf(context, target);
		if (!isOp && !isTargetingSelf)
			throw new CommandException(localizedComponent("no_permissions"));
		
		FoodList.get(target).clearFood();
		FoodList.get(target).resetFoodsEaten();
		BenefitsHandler.removeAllBenefits(target);
		BenefitsHandler.updatePlayer(target);
		CapabilityHandler.syncFoodList(target);
		
		IFormattableTextComponent feedback = localizedComponent("clear.success");
		sendFeedback(context.getSource(), feedback);
		if (!isTargetingSelf) {
			target.sendStatusMessage(applyFeedbackStyle(feedback), true);
		}
		
		return Command.SINGLE_SUCCESS;
	}
	
	static void sendFeedback(CommandSource source, IFormattableTextComponent message) {
		source.sendFeedback(applyFeedbackStyle(message), true);
	}
	
	private static IFormattableTextComponent applyFeedbackStyle(IFormattableTextComponent text) {
		return text.modifyStyle(style -> style.applyFormatting(TextFormatting.DARK_AQUA));
	}
	
	static boolean isTargetingSelf(CommandContext<CommandSource> context, PlayerEntity target) {
		return target.isEntityEqual(Objects.requireNonNull(context.getSource().getEntity()));
	}
	
	static IFormattableTextComponent localizedComponent(String path, Object... args) {
		return Localization.localizedComponent("command", localizationPath(path), args);
	}
	
	static IFormattableTextComponent localizedQuantityComponent(String path, int number) {
		return Localization.localizedQuantityComponent("command", localizationPath(path), number);
	}
	
	static String localizationPath(String path) {
		return FoodListCommand.name + "." + path;
	}
}
