package fi.dy.masa.malilib.overlay;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import fi.dy.masa.malilib.MaLiLibConfigs;
import fi.dy.masa.malilib.event.ClientTickHandler;
import fi.dy.masa.malilib.event.PostGameOverlayRenderer;
import fi.dy.masa.malilib.event.PostScreenRenderer;
import fi.dy.masa.malilib.gui.position.ScreenLocation;
import fi.dy.masa.malilib.gui.util.GuiUtils;
import fi.dy.masa.malilib.gui.widget.BaseWidget;
import fi.dy.masa.malilib.input.Context;
import fi.dy.masa.malilib.overlay.widget.InfoRendererWidget;
import fi.dy.masa.malilib.overlay.widget.StringListRendererWidget;

public class InfoOverlay implements PostGameOverlayRenderer, PostScreenRenderer, ClientTickHandler
{
    public static final InfoOverlay INSTANCE = new InfoOverlay();

    protected final HashMap<ScreenLocation, InfoArea> infoAreas = new HashMap<>();
    protected final List<InfoRendererWidget> enabledInGameWidgets = new ArrayList<>();
    protected final List<InfoRendererWidget> enabledGuiWidgets = new ArrayList<>();
    protected final List<InfoArea> activeInfoAreas = new ArrayList<>();
    protected final Minecraft mc = Minecraft.getMinecraft();
    protected boolean needsReFetch;

    public InfoArea getOrCreateInfoArea(ScreenLocation location)
    {
        return this.infoAreas.computeIfAbsent(location, (loc) -> new InfoArea(loc, this::notifyEnabledWidgetsChanged));
    }

    @Override
    public void onPostGameOverlayRender(Minecraft mc, float partialTicks)
    {
        if (mc.gameSettings.hideGUI == false)
        {
            this.renderInGame();
        }
    }

    @Override
    public void onPostScreenRender(Minecraft mc, float partialTicks)
    {
        this.renderScreen();
    }

    @Override
    public void onClientTick(Minecraft mc)
    {
        this.tick();
    }

    /**
     * Notifies the InfoOverlay of a change in the set of enabled InfoRendererWidgets,
     * causing the enabled widgets to be fetched again.
     */
    public void notifyEnabledWidgetsChanged()
    {
        this.needsReFetch = true;
        //System.out.printf("InfoOverlay#notifyWidgetChange() - size: %d\n", this.enabledInfoWidgets.size());
    }

    protected void fetchEnabledWidgets()
    {
        this.enabledInGameWidgets.clear();
        this.enabledGuiWidgets.clear();
        this.activeInfoAreas.clear();

        for (InfoArea infoArea : this.infoAreas.values())
        {
            List<InfoRendererWidget> widgets = infoArea.getEnabledWidgets();

            if (widgets.isEmpty() == false)
            {
                for (InfoRendererWidget widget : widgets)
                {
                    if (widget.shouldRenderInContext(Context.GUI))
                    {
                        this.enabledGuiWidgets.add(widget);
                    }

                    if (widget.shouldRenderInContext(Context.INGAME))
                    {
                        this.enabledInGameWidgets.add(widget);
                    }
                }

                this.activeInfoAreas.add(infoArea);
            }
        }
        //System.out.printf("InfoOverlay#fetchEnabledWidgets() - size: %d\n", this.enabledInfoWidgets.size());
    }

    /**
     * Calls the InfoRendererWidget#updateState() method on all the currently enabled widgets.
     * Don't call this unless you have your own instance of the InfoOverlay,
     * ie. don't call this on InfoOverlay.INSTANCE
     */
    public void tick()
    {
        if (this.needsReFetch)
        {
            this.fetchEnabledWidgets();
            this.needsReFetch = false;
        }

        if (GuiUtils.getCurrentScreen() != null)
        {
            for (InfoRendererWidget widget : this.enabledGuiWidgets)
            {
                // This allows the widgets to update their contents, which may also change their dimensions
                widget.updateState();
            }
        }
        else
        {
            for (InfoRendererWidget widget : this.enabledInGameWidgets)
            {
                // This allows the widgets to update their contents, which may also change their dimensions
                widget.updateState();
            }
        }

        for (InfoArea infoArea : this.activeInfoAreas)
        {
            // This allows the InfoArea to re-layout its widgets
            infoArea.updateState();
        }
    }

    /**
     * Renders all the currently enabled widgets that are set to be rendered in the in-game context.
     * Don't call this unless you have your own instance of the InfoOverlay,
     * ie. don't call this on InfoOverlay.INSTANCE
     */
    public void renderInGame()
    {
        if (this.mc.gameSettings.hideGUI == false)
        {
            boolean debug = MaLiLibConfigs.Debug.INFO_OVERLAY_DEBUG.getBooleanValue();

            if (debug)
            {
                for (InfoArea area : this.infoAreas.values())
                {
                    area.renderDebug();
                }
            }

            for (InfoRendererWidget widget : this.enabledInGameWidgets)
            {
                if (widget.shouldRenderInContext(false))
                {
                    widget.render();
                }
            }

            if (debug)
            {
                BaseWidget.renderDebugTextAndClear();
            }
        }
    }

    /**
     * Renders all the currently enabled widgets that are set to be rendered in the gui context.
     * Don't call this unless you have your own instance of the InfoOverlay,
     * ie. don't call this on InfoOverlay.INSTANCE
     */
    public void renderScreen()
    {
        boolean debug = MaLiLibConfigs.Debug.INFO_OVERLAY_DEBUG.getBooleanValue();

        if (debug)
        {
            for (InfoArea area : this.infoAreas.values())
            {
                area.renderDebug();
            }
        }

        for (InfoRendererWidget widget : this.enabledGuiWidgets)
        {
            if (widget.shouldRenderInContext(true))
            {
                widget.render();
            }
        }

        if (debug)
        {
            BaseWidget.renderDebugTextAndClear();
        }
    }

    /**
     * Convenience method to get or create a text hud at the given screen location,
     * from the default InfoOverlay instance.
     */
    public static StringListRendererWidget getTextHud(ScreenLocation location)
    {
        InfoArea area = INSTANCE.getOrCreateInfoArea(location);
        StringListRendererWidget widget = area.findWidget(StringListRendererWidget.class, (w) -> true);

        if (widget == null)
        {
            widget = new StringListRendererWidget();
            area.addWidget(widget);
        }

        return widget;
    }

    /**
     * Convenience method to find a matching widget at the given screen location
     * from the default InfoOverlay instance, or create and add a new widget if no matches are found.
     */
    public static <C extends InfoRendererWidget>
    C findOrCreateWidget(ScreenLocation location, Class<C> clazz, Predicate<C> validator, Supplier<C> factory)
    {
        InfoArea area = INSTANCE.getOrCreateInfoArea(location);
        C widget = area.findWidget(clazz, validator);

        if (widget == null)
        {
            widget = factory.get();
            area.addWidget(widget);
        }

        return widget;
    }
}