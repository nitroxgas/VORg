package br.com.casadalagoa.vof;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * {@link BoatAdapter} exposes a list of weather forecasts
 * from a {@link android.database.Cursor} to a {@link android.widget.ListView}.
 */
public class BoatAdapter extends CursorAdapter {

    private static final int VIEW_TYPE_COUNT = 2;
    private static final int VIEW_TYPE_DESC = 0;
    private static final int VIEW_TYPE_FUTURE_DAY = 1;

    // Flag to determine if we want to use a separate view for "today".
    private boolean mUseTodayLayout = true;

    /**
     * Cache of the children views for a forecast list item.
     */
    public static class ViewHolder {
        public final ImageView iconView;
        public final TextView dataView;
        public final TextView nameView;
        public final TextView rankView;
        public final TextView completitionView;
        public final FrameLayout layView;
        public final LinearLayout lay_itemView;

        public ViewHolder(View view) {
            iconView = (ImageView) view.findViewById(R.id.list_item_icon);
            dataView = (TextView) view.findViewById(R.id.list_item_data);
            nameView = (TextView) view.findViewById(R.id.list_item_name);
            rankView = (TextView) view.findViewById(R.id.list_item_rank);
            completitionView = (TextView) view.findViewById(R.id.list_item_leg_comp);
            layView = (FrameLayout) view.findViewById(R.id.list_item_lay_img);
            lay_itemView = (LinearLayout) view.findViewById(R.id.list_item_lay_all);
        }
    }

    public BoatAdapter(Context context, Cursor c, int flags) {
        super(context, c, flags);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        // Choose the layout type
        int viewType = getItemViewType(cursor.getPosition());
        int layoutId = -1;
        switch (viewType) {
            case VIEW_TYPE_DESC: {
                layoutId = R.layout.list_item_description;
                break;
            }
            case VIEW_TYPE_FUTURE_DAY: {
                layoutId = R.layout.list_item;
                break;
            }
        }
        View view = LayoutInflater.from(context).inflate(layoutId, parent, false);
        ViewHolder viewHolder = new ViewHolder(view);
        view.setTag(viewHolder);
        return view;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {

        ViewHolder viewHolder = (ViewHolder) view.getTag();

        //viewHolder.layView.setBackgroundColor(Color.parseColor("#"+cursor.getString(BoatFragment.COL_CODE_COLOR)));
        //viewHolder.lay_itemView.setBackgroundColor(Color.parseColor("#"+cursor.getString(BoatFragment.COL_CODE_COLOR)));

        int viewType = getItemViewType(cursor.getPosition());
        switch (viewType) {
            case VIEW_TYPE_DESC: {
                // Get weather icon
                viewHolder.nameView.setText(R.string.app_description_text);
                break;
            }
            case VIEW_TYPE_FUTURE_DAY: {
                // Get weather icon
                viewHolder.iconView.setImageResource(Utility.getFormattedBoatHeading(this.mContext,
                        cursor.getString(BoatFragment.COL_BOAT_CODE), cursor.getString(BoatFragment.COL_BOAT_BOATHEADINGTRUE)));
                String dataString = "Speed: "+cursor.getString(BoatFragment.COL_BOAT_SPEEDTHROUGWATER)+"kn  HD:"+cursor.getString(BoatFragment.COL_BOAT_BOATHEADINGTRUE)+"º Wind:"+cursor.getString(BoatFragment.COL_BOAT_TRUEWINDSPEEDMAX)+"kn";

                viewHolder.dataView.setText(dataString);

                // Read weather forecast from cursor
                String description = cursor.getString(BoatFragment.COL_CODE_NAME);
                // Find TextView and set weather forecast on it
                viewHolder.nameView.setText(description);

                // For accessibility, add a content description to the icon field
                viewHolder.iconView.setContentDescription(description);

                // Read user preference for metric or imperial temperature units
                // boolean isMetric = Utility.isMetric(context);

                // Rank
                String mRank = cursor.getString(BoatFragment.COL_BOAT_LEGSTANDING);
                viewHolder.rankView.setText(mRank);

                // Leg Progress
                String mProgress = cursor.getString(BoatFragment.COL_BOAT_LEGPROGRESS);
                viewHolder.completitionView.setText(mProgress+"%");
                break;
            }
        }
    }

    public void setUseTodayLayout(boolean useTodayLayout) {
        mUseTodayLayout = useTodayLayout;
    }

    @Override
    public int getItemViewType(int position) {
        return (position == 0 && mUseTodayLayout) ? VIEW_TYPE_DESC : VIEW_TYPE_FUTURE_DAY;
    }

    @Override
    public int getViewTypeCount() {
        return VIEW_TYPE_COUNT;
    }

    @Override
    protected void onContentChanged() {
        super.onContentChanged();
    }
}
