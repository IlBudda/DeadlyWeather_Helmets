package party.elias.deadlyweather;

import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = DeadlyWeather.MODID, bus = EventBusSubscriber.Bus.GAME)
public class CommonGameEvents {

    private static boolean absorbDamageWithHelmet(ServerPlayer player) {
        ItemStack helmet = player.getInventory().getArmor(3); // Slot 3 = helmet
        if (!helmet.isEmpty() && helmet.isDamageableItem()) {
            helmet.hurtAndBreak(1, player, EquipmentSlot.HEAD);
            return true;
        }
        return false;
    }

    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {

        Entity entity = event.getEntity();
        BlockPos blockPos = BlockPos.containing(entity.getPosition(1));
        RegistryAccess registryAccess = entity.registryAccess();

        if (entity.level() instanceof ServerLevel level && entity instanceof ServerPlayer player) {
            WeatherSettingsSD settings = WeatherSettingsSD.from(level);

            if (Config.Sunny.enable) {
                if (level.getGameTime() % Config.Sunny.damageInterval == 0
                        && !level.isRaining() && level.isDay() && level.canSeeSky(Utils.getRelevantBlockPos(player))) {
                    if (!absorbDamageWithHelmet(player)) {
                        player.hurt(new DamageSource(registryAccess.holderOrThrow(DamageTypes.IN_FIRE)), (float) Config.Sunny.damage);
                    }
                }
            }

            if (Config.Thunder.enable && Config.Thunder.PlayerSeeking.enable) {
                if (level.getGameTime() % Config.Thunder.PlayerSeeking.interval == 0
                        && level.isThundering() && level.canSeeSky(Utils.getRelevantBlockPos(player))) {
                    if (!absorbDamageWithHelmet(player)) {
                        Utils.strikeLightningAt(level, Utils.getRelevantBlockPos(player));
                    }
                }
            }

            if (Config.Rainy.enable) {
                if (level.getGameTime() % Config.Rainy.damageInterval == 0
                        && level.isRaining() && level.getBiome(blockPos).value().warmEnoughToRain(blockPos)
                        && level.canSeeSky(Utils.getRelevantBlockPos(player))) {
                    if (!absorbDamageWithHelmet(player)) {
                        player.hurt(new DamageSource(registryAccess.holderOrThrow(DeadlyWeather.ACID_DAMAGE_KEY)), (float) Config.Rainy.damage);
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public static void onRegisterCommandsEvent(RegisterCommandsEvent event) {
        DeadlyWeatherCommand.register(event.getDispatcher());
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            PacketDistributor.sendToPlayer(player, WeatherSettingsSD.from((ServerLevel) player.level()).getSettings());
        }
    }
}
