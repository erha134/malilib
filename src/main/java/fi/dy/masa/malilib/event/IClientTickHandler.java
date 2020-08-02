package fi.dy.masa.malilib.event;

import net.minecraft.client.Minecraft;

public interface IClientTickHandler
{
    /**
     * Called from the end of the client tick code (for world ticks, not the main game loop/rendering).
     * <br>br>
     * The classes implementing this method should be registered to {@link fi.dy.masa.malilib.event.dispatch.TickEventDispatcher}
     * @param mc
     */
    void onClientTick(Minecraft mc);
}