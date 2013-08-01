package com.gawdl3y.android.actionablelistview;

import android.content.Context;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListAdapter;
import android.widget.ListView;

/**
 * A {@code ListView} that automatically handles {@code ActionMode}s for checked items
 * @author Schuyler Cebulskie
 */
public class ActionableListView extends CheckableListView implements AdapterView.OnItemLongClickListener, ActionMode.Callback {
    private SparseArray<ActionItem> mActions = new SparseArray<ActionItem>();
    private ActionMode mActionMode;

    public ActionableListView(Context context) {
        super(context);
        setOnItemLongClickListener(this);
    }

    public ActionableListView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setOnItemLongClickListener(this);
    }

    public ActionableListView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setOnItemLongClickListener(this);
    }

    @Override
    public void setAdapter(ListAdapter adapter) {
        if(!(adapter instanceof ActionableAdapter)) throw new IllegalArgumentException("Adapter must be instance of ActionableAdapter");
        super.setAdapter(adapter);
    }

    @Override
    public boolean performItemClick(View view, int position, long id) {
        boolean sooper = super.performItemClick(view, position, id);
        if(getChoiceMode() == CHOICE_MODE_MULTIPLE && getCheckedItemCount() == 0) mActionMode.finish();
        return sooper;
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        if(getChoiceMode() == CHOICE_MODE_NONE) mActionMode = startActionMode(this);
        performItemClick(view, position, id);
        return true;
    }

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        setChoiceMode(CHOICE_MODE_MULTIPLE);
        return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        menu.clear();
        for(int i = 0; i < mActions.size(); i++) {
            ActionItem item = mActions.valueAt(i);
            menu.add(0, item.getActionType(),item.getPosition(), item.getTitleResource()).setIcon(item.getIconResource());
        }
        return true;
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        if(getAdapter() != null) {
            ActionableAdapter adapter = (ActionableAdapter) getAdapter();
            boolean dataOrderAffected = false;
            for(int i = 0; i < getCheckedItemPositions().size(); i++) {
                if(adapter.performActionOnItem(item.getItemId(), getCheckedItemPositions().keyAt(i))) dataOrderAffected = true;
            }

            if(dataOrderAffected) mode.finish();
        }
        return true;
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
        mActionMode = null;
        setChoiceMode(CHOICE_MODE_NONE);
    }

    /**
     * Adds an {@code ActionItem}
     * @param action The item to add
     */
    public void addAction(ActionItem action) {
        mActions.put(action.getActionType(), action);
        if(mActionMode != null) mActionMode.invalidate();
    }

    /**
     * Removes an {@code ActionItem}
     * @param actionType The action type of the item to remove
     */
    public void removeAction(int actionType) {
        mActions.remove(actionType);
        if(mActionMode != null) mActionMode.invalidate();
    }

    /**
     * @return The {@code ActionItem}s
     */
    public SparseArray<ActionItem> getActions() {
        return mActions;
    }

    /**
     * @return The {@code ActionMode}
     */
    public ActionMode getActionMode() {
        return mActionMode;
    }
}
