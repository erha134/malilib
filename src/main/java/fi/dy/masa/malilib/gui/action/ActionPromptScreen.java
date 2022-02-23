package fi.dy.masa.malilib.gui.action;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import org.lwjgl.input.Keyboard;
import fi.dy.masa.malilib.MaLiLibConfigs;
import fi.dy.masa.malilib.action.ActionContext;
import fi.dy.masa.malilib.action.ActionList;
import fi.dy.masa.malilib.action.NamedAction;
import fi.dy.masa.malilib.gui.BaseListScreen;
import fi.dy.masa.malilib.gui.BaseScreen;
import fi.dy.masa.malilib.gui.icon.DefaultIcons;
import fi.dy.masa.malilib.gui.widget.BaseTextFieldWidget;
import fi.dy.masa.malilib.gui.widget.CheckBoxWidget;
import fi.dy.masa.malilib.gui.widget.DropDownListWidget;
import fi.dy.masa.malilib.gui.widget.list.DataListWidget;
import fi.dy.masa.malilib.gui.widget.list.entry.action.ActionPromptEntryWidget;
import fi.dy.masa.malilib.input.ActionResult;
import fi.dy.masa.malilib.util.StringUtils;

public class ActionPromptScreen extends BaseListScreen<DataListWidget<NamedAction>>
{
    protected final List<NamedAction> filteredActions = new ArrayList<>();
    protected final DropDownListWidget<ActionList> dropDownWidget;
    protected final BaseTextFieldWidget searchTextField;
    protected final CheckBoxWidget fuzzySearchCheckBoxWidget;
    protected final CheckBoxWidget rememberSearchCheckBoxWidget;
    protected final CheckBoxWidget searchDisplayNameCheckBoxWidget;

    public ActionPromptScreen()
    {
        super(0, 32, 0, 32);

        List<ActionList> lists = ActionList.getActionLists();
        this.dropDownWidget = new DropDownListWidget<>(-1, 16, 80, 4, lists, ActionList::getDisplayName);
        this.dropDownWidget.setSelectedEntry(ActionList.getSelectedList(lists));
        this.dropDownWidget.setSelectionListener(this::onListSelectionChanged);

        String label = "malilib.checkbox.action_prompt_screen.remember_search";
        String hoverKey = "malilib.hover.action.prompt_screen.remember_search_text";
        this.rememberSearchCheckBoxWidget = new CheckBoxWidget(label, hoverKey);
        this.rememberSearchCheckBoxWidget.setBooleanStorage(MaLiLibConfigs.Generic.ACTION_PROMPT_REMEMBER_SEARCH);

        label = "malilib.checkbox.action_prompt_screen.fuzzy_search";
        hoverKey = "malilib.hover.action.prompt_screen.use_fuzzy_search";
        this.fuzzySearchCheckBoxWidget = new CheckBoxWidget(label, hoverKey);
        this.fuzzySearchCheckBoxWidget.setBooleanStorage(MaLiLibConfigs.Generic.ACTION_PROMPT_FUZZY_SEARCH);
        this.fuzzySearchCheckBoxWidget.setListener((v) -> this.updateFilteredList());

        label = "malilib.checkbox.action_prompt_screen.search_display_name";
        hoverKey = "malilib.hover.action.prompt_screen.search_display_name";
        this.searchDisplayNameCheckBoxWidget = new CheckBoxWidget(label, hoverKey);
        this.searchDisplayNameCheckBoxWidget.setBooleanStorage(MaLiLibConfigs.Generic.ACTION_PROMPT_SEARCH_DISPLAY_NAME);
        this.searchDisplayNameCheckBoxWidget.setListener((v) -> this.updateFilteredList());

        int screenWidth = 320;
        this.searchTextField = new BaseTextFieldWidget(screenWidth - 12, 16);
        this.searchTextField.setListener(this::updateFilteredList);
        this.searchTextField.setUpdateListenerAlways(true);

        this.setScreenWidthAndHeight(screenWidth, 132);
    }

    @Override
    protected void initScreen()
    {
        this.setPosition(4, this.height - this.screenHeight - 4);

        super.initScreen();

        if (MaLiLibConfigs.Generic.ACTION_PROMPT_REMEMBER_SEARCH.getBooleanValue())
        {
            this.searchTextField.setText(MaLiLibConfigs.Internal.ACTION_PROMPT_SEARCH_TEXT.getStringValue());
        }

        this.searchTextField.setFocused(true);
        this.updateFilteredList();
    }

    @Override
    protected void reAddActiveWidgets()
    {
        super.reAddActiveWidgets();

        this.addWidget(this.dropDownWidget);
        this.addWidget(this.searchTextField);
        this.addWidget(this.rememberSearchCheckBoxWidget);
        this.addWidget(this.fuzzySearchCheckBoxWidget);
        this.addWidget(this.searchDisplayNameCheckBoxWidget);
    }

    @Override
    protected void updateWidgetPositions()
    {
        super.updateWidgetPositions();

        int x = this.x + this.screenWidth - DefaultIcons.CHECKMARK_OFF.getWidth();
        this.rememberSearchCheckBoxWidget.setPosition(x, this.y);
        this.fuzzySearchCheckBoxWidget.setPosition(x, this.y + 11);
        this.searchDisplayNameCheckBoxWidget.setPosition(x, this.y + 22);
        this.closeButton.setX(x - this.closeButton.getWidth() - 2);

        this.dropDownWidget.setPosition(this.x, this.y);
        this.searchTextField.setPosition(this.x, this.y + 16);
    }

    @Override
    protected void onScreenClosed()
    {
        if (MaLiLibConfigs.Generic.ACTION_PROMPT_REMEMBER_SEARCH.getBooleanValue())
        {
            MaLiLibConfigs.Internal.ACTION_PROMPT_SEARCH_TEXT.setValue(this.searchTextField.getText());
        }

        super.onScreenClosed();
    }

    @Override
    public boolean onKeyTyped(int keyCode, int scanCode, int modifiers)
    {
        if (keyCode == Keyboard.KEY_RETURN)
        {
            // Close the screen before running the action, in case the action opens another screen
            this.closeScreen();

            NamedAction action = this.getListWidget().getKeyboardNavigationEntry();

            if (action != null)
            {
                action.execute();
            }
        }
        else if (keyCode == Keyboard.KEY_ESCAPE)
        {
            this.closeScreen();
        }

        return super.onKeyTyped(keyCode, scanCode, modifiers);
    }

    protected void onListSelectionChanged(ActionList list)
    {
        MaLiLibConfigs.Internal.ACTION_PROMPT_SELECTED_LIST.setValue(list.getName());
        this.updateFilteredList();
    }

    protected List<? extends NamedAction> getActions()
    {
        return this.dropDownWidget.getSelectedEntry().getActions();
    }

    protected List<NamedAction> getFilteredActions()
    {
        return this.filteredActions;
    }

    protected boolean shouldUseFuzzySearch()
    {
        return MaLiLibConfigs.Generic.ACTION_PROMPT_FUZZY_SEARCH.getBooleanValue();
    }

    protected boolean stringMatchesSearch(List<String> searchTerms, String text)
    {
        if (this.shouldUseFuzzySearch())
        {
            for (String searchTerm : searchTerms)
            {
                if (StringUtils.containsOrderedCharacters(searchTerm, text))
                {
                    return true;
                }
            }
        }
        else
        {
            for (String searchTerm : searchTerms)
            {
                if (text.contains(searchTerm))
                {
                    return true;
                }
            }
        }

        return false;
    }

    protected boolean actionMatchesSearch(List<String> searchTerms, NamedAction action)
    {
        if (this.stringMatchesSearch(searchTerms, action.getName().toLowerCase(Locale.ROOT)))
        {
            return true;
        }

        return MaLiLibConfigs.Generic.ACTION_PROMPT_SEARCH_DISPLAY_NAME.getBooleanValue() &&
               this.stringMatchesSearch(searchTerms, action.getDisplayName().toLowerCase(Locale.ROOT));
    }

    protected void updateFilteredList()
    {
        this.updateFilteredList(this.searchTextField.getText());
    }

    protected void updateFilteredList(String searchText)
    {
        this.filteredActions.clear();

        if (org.apache.commons.lang3.StringUtils.isBlank(searchText))
        {
            this.filteredActions.addAll(this.getActions());
        }
        else
        {
            searchText = searchText.toLowerCase(Locale.ROOT);
            List<String> searchTerms = Arrays.asList(searchText.split("\\|"));

            for (NamedAction action : this.getActions())
            {
                if (this.actionMatchesSearch(searchTerms, action))
                {
                    this.filteredActions.add(action);
                }
            }
        }

        this.getListWidget().refreshEntries();

        if (this.filteredActions.isEmpty() == false)
        {
            this.getListWidget().getEntrySelectionHandler().setKeyboardNavigationIndex(0);
        }
    }

    @Override
    protected DataListWidget<NamedAction> createListWidget(int listX, int listY, int listWidth, int listHeight)
    {
        DataListWidget<NamedAction> listWidget = new DataListWidget<>(listX, listY, listWidth, listHeight, this::getFilteredActions);
        listWidget.setAllowKeyboardNavigation(true);
        listWidget.setListEntryWidgetFixedHeight(12);
        listWidget.setFetchFromSupplierOnRefresh(true);
        listWidget.getBackgroundRenderer().getNormalSettings().setEnabledAndColor(true, 0xA0000000);
        listWidget.setEntryWidgetFactory(ActionPromptEntryWidget::new);
        return listWidget;
    }

    public static ActionResult openActionPromptScreen(ActionContext ctx)
    {
        BaseScreen.openScreen(new ActionPromptScreen());
        return ActionResult.SUCCESS;
    }
}
