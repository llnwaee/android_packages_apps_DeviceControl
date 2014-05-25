package org.namelessrom.devicecontrol.widgets.preferences;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.preference.ListPreference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

import org.namelessrom.devicecontrol.R;

import static butterknife.ButterKnife.findById;

public class CustomListPreference extends ListPreference {

    private String color = "#FFFFFF";

    public CustomListPreference(Context context) {
        super(context);
        setLayoutResource(R.layout.preference);
    }

    public CustomListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setLayoutResource(R.layout.preference);
    }

    public void setTitleColor(final String color) { this.color = color; }

    @Override
    protected void onBindView(final View view) {
        super.onBindView(view);

        final TextView title = findById(view, android.R.id.title);
        title.setTextColor(Color.parseColor(color));
        title.setTypeface(Typeface.create("sans-serif-condensed", Typeface.NORMAL));

        final TextView summary = findById(view, android.R.id.summary);
        summary.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
    }

    @Override
    public boolean isPersistent() { return false; }

    @Override
    protected boolean shouldPersist() { return false; }

}