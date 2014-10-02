/*
 * Copyright 2013 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.namelessrom.devicecontrol.wizard.ui;

import android.app.Activity;
import android.app.ListFragment;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.squareup.otto.Subscribe;

import org.namelessrom.devicecontrol.R;
import org.namelessrom.devicecontrol.database.DatabaseHandler;
import org.namelessrom.devicecontrol.database.TaskerItem;
import org.namelessrom.devicecontrol.utils.providers.BusProvider;
import org.namelessrom.devicecontrol.wizard.TaskerWizardModel;
import org.namelessrom.devicecontrol.wizard.events.SaveTaskEvent;
import org.namelessrom.devicecontrol.wizard.model.AbstractWizardModel;
import org.namelessrom.devicecontrol.wizard.model.ModelCallbacks;
import org.namelessrom.devicecontrol.wizard.model.Page;
import org.namelessrom.devicecontrol.wizard.model.ReviewItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class ReviewFragment extends ListFragment implements ModelCallbacks {
    private Callbacks           mCallbacks;
    private AbstractWizardModel mWizardModel;
    private List<ReviewItem>    mCurrentReviewItems;

    private ReviewAdapter mReviewAdapter;

    public ReviewFragment() { }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mReviewAdapter = new ReviewAdapter();
    }

    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater, final ViewGroup container,
            final Bundle savedInstanceState) {
        final View rootView = inflater.inflate(R.layout.wizard_fragment_page, container, false);

        final TextView titleView = (TextView) rootView.findViewById(android.R.id.title);
        titleView.setText(R.string.review);
        titleView.setTextColor(getResources().getColor(R.color.review_green));

        ((ListView) rootView.findViewById(android.R.id.list))
                .setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        setListAdapter(mReviewAdapter);

        return rootView;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        BusProvider.getBus().register(this);

        if (!(activity instanceof Callbacks)) {
            throw new ClassCastException("Activity must implement fragment's callbacks");
        }

        mCallbacks = (Callbacks) activity;

        mWizardModel = mCallbacks.onGetModel();
        if (mWizardModel != null) {
            mWizardModel.registerListener(this);
        }
        onPageTreeChanged();
    }

    @Override
    public void onPageTreeChanged() {
        onPageDataChanged(null);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        BusProvider.getBus().unregister(this);
        mCallbacks = null;

        mWizardModel.unregisterListener(this);
    }

    @Override
    public void onPageDataChanged(Page changedPage) {
        ArrayList<ReviewItem> reviewItems = new ArrayList<ReviewItem>();
        for (Page page : mWizardModel.getCurrentPageSequence()) {
            page.getReviewItems(reviewItems);
        }
        Collections.sort(reviewItems, new Comparator<ReviewItem>() {
            @Override
            public int compare(ReviewItem a, ReviewItem b) {
                return a.getWeight() > b.getWeight() ? +1 : a.getWeight() < b.getWeight() ? -1 : 0;
            }
        });
        mCurrentReviewItems = reviewItems;

        if (mReviewAdapter != null) {
            mReviewAdapter.notifyDataSetInvalidated();
        }
    }

    @Subscribe
    public void onSaveTaskEvent(final SaveTaskEvent event) {
        if (event == null) return;

        String category = "", action = "", value = "";
        String title, displayValue;
        for (final ReviewItem item : mCurrentReviewItems) {
            title = item.getTitle();
            displayValue = item.getDisplayValue();
            if (title.contains("1)")) {
                category = displayValue;
            } else if (title.contains("2)")) {
                action = displayValue;
            } else if (title.contains("3)")) {
                value = displayValue;
            }
        }

        TaskerItem item = null;
        if (mWizardModel instanceof TaskerWizardModel) {
            item = ((TaskerWizardModel) mWizardModel).getItem();
        }
        if (item == null) {
            item = new TaskerItem(category, action, "NONE", value, true);
            DatabaseHandler.getInstance().addTaskerItem(item);
        } else {
            if (category.isEmpty() || action.isEmpty() || value.isEmpty()) {
                DatabaseHandler.getInstance().deleteTaskerItem(item);
            }
            item.setCategory(category);
            item.setName(action);
            item.setValue(value);
            DatabaseHandler.getInstance().updateTaskerItem(item);
        }

        getActivity().finish();
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        mCallbacks.onEditScreenAfterReview(mCurrentReviewItems.get(position).getPageKey());
    }

    public interface Callbacks {
        AbstractWizardModel onGetModel();

        void onEditScreenAfterReview(String pageKey);
    }

    private class ReviewAdapter extends BaseAdapter {

        private final LayoutInflater inflater;

        public ReviewAdapter() {
            super();
            inflater = LayoutInflater.from(getActivity());
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }

        @Override
        public int getItemViewType(int position) {
            return 0;
        }

        @Override
        public int getViewTypeCount() {
            return 1;
        }

        @Override
        public boolean areAllItemsEnabled() {
            return true;
        }

        @Override
        public Object getItem(int position) {
            return mCurrentReviewItems.get(position);
        }

        @Override
        public long getItemId(int position) {
            return mCurrentReviewItems.get(position).hashCode();
        }

        @Override
        public View getView(int position, View view, ViewGroup container) {
            final ViewHolder viewHolder;
            if (view == null) {
                view = inflater.inflate(R.layout.wizard_list_item_review, container, false);

                viewHolder = new ViewHolder(view);
                view.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) view.getTag();
            }

            final ReviewItem reviewItem = mCurrentReviewItems.get(position);

            String value = reviewItem.getDisplayValue();
            if (TextUtils.isEmpty(value)) {
                value = "(None)";
            }

            viewHolder.title.setText(reviewItem.getTitle());
            viewHolder.value.setText(value);

            return view;
        }

        @Override
        public int getCount() {
            return mCurrentReviewItems.size();
        }

        private class ViewHolder {
            private final TextView title;
            private final TextView value;

            public ViewHolder(final View v) {
                title = (TextView) v.findViewById(android.R.id.text1);
                value = (TextView) v.findViewById(android.R.id.text2);
            }
        }
    }
}
